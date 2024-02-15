import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class WebServer {
    private static int PORT;
    private static String ROOT_DIRECTORY;
    private static String DEFAULT_PAGE;
    private static int MAX_THREADS;
    private static HashMap<String, String> serverParams;


    public static void main(String[] args) {
        readConfig();
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREADS);
        serverParams = new HashMap<>();

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server listening on port " + PORT+"\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (Exception e) {
            System.out.println("Unexpected error occurred, shutting down...");
            System.exit(1);
        } finally {
            threadPool.shutdown();
        }
    }
    private static void readConfig() {
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream("config.ini"));
            PORT = Integer.parseInt(properties.getProperty("port"));
            ROOT_DIRECTORY = properties.getProperty("root");
            if(!ROOT_DIRECTORY.endsWith("/"))
            {
                ROOT_DIRECTORY = ROOT_DIRECTORY + "/";
            }
            
            DEFAULT_PAGE = properties.getProperty("defaultPage");
            MAX_THREADS = Integer.parseInt(properties.getProperty("maxThreads"));

            if (ROOT_DIRECTORY == null || DEFAULT_PAGE == null || MAX_THREADS <= 0 || PORT < 0) {
                throw new Exception();
            }
        } catch (Exception e) {
            System.err.println("Error with config.ini file");
            System.exit(1);
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            BufferedReader in = null;
            OutputStream out = null;

            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = clientSocket.getOutputStream();

                StringBuilder requestHeaderBuilder = new StringBuilder();
                String line;
                int contentLength = -1; // Default value for content length
                // Read the HTTP request
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    requestHeaderBuilder.append(line).append("\r\n");
                    if (line.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(line.split(":")[1].strip());
                    }
                }
                // Read the HTTP request body
                StringBuilder requestBodyBuilder = new StringBuilder();
                
                if (contentLength >= 0) {
                    char[] buffer = new char[contentLength];
                    in.read(buffer, 0, contentLength);
                    requestBodyBuilder.append(buffer);
                }
                
                // Create an instance of HTTPRequest using the parsed header and body
                HTTPRequest httpRequest = new HTTPRequest(requestHeaderBuilder.toString(), requestBodyBuilder.toString());
                // Parse the HTTP request header using HTTPRequest class
                System.out.println("printing the request:\n" + requestHeaderBuilder.toString()+"\n");
                System.out.println("printing http request object:\n" + httpRequest.toString()+"\n");
                // Get the requested page
                String requestedPage = httpRequest.getRequestedPage();

                // If the requested page is empty or null, serve the default page]
                if (requestedPage == null || requestedPage.isEmpty() || requestedPage.equals("/")) {
                    requestedPage = DEFAULT_PAGE;
                }
                
                if(requestedPage.startsWith("/"))
                {
                    requestedPage = requestedPage.substring(1);
                }
                
                String path = normalizePath(ROOT_DIRECTORY + requestedPage);
                // Read the requested file (if it exists) and generate the proper response
                File file = new File(path);
                String method = httpRequest.getType();
                if (method.equals("POST") || method.equals("GET") || method.equals("TRACE") || method.equals("HEAD"))
                {
                    if (method.equals("POST") || method.equals("GET") || method.equals("HEAD"))  {
                        if (httpRequest.getParameters() != null && !method.equals("HEAD")) {
                            handleParamsInfo(httpRequest);
                        }
                        if (file.exists()) {
                            byte[] fileContent = readFile(file);
                            String contentType = retrieveContentType(file);

                            if (method.equals("POST") && requestedPage.equals("params_info.html")) {
                                String page = generateParamsInfoPage(serverParams);
                                String responseHeader = generateResponseHeader(200, null, page.getBytes(StandardCharsets.UTF_8), "text/html", httpRequest.isChunked());
                                out.write(responseHeader.getBytes());
                                out.flush();
                                System.out.println("printing response header:\n" + responseHeader);               
                                sendContent(page.getBytes(StandardCharsets.UTF_8), httpRequest.isChunked());
                                
                            }
                            else {
                                String responseHeader = generateResponseHeader(200, requestedPage, fileContent, contentType, httpRequest.isChunked());
                                System.out.println("printing response header:\n" + responseHeader);
                                out.write(responseHeader.getBytes());
                                out.flush();

                                if(fileContent != null && (method.equals("GET")  || method.equals("POST")) ) {
                                    sendContent(fileContent, httpRequest.isChunked());
                                }
                            }
                        }
                        else {
                            if (requestedPage.equals("params_info.html") && method.equals("POST")) {
                                String page = generateParamsInfoPage(serverParams);
                                String responseHeader = generateResponseHeader(200, null, page.getBytes(StandardCharsets.UTF_8), "text/html", httpRequest.isChunked());
                                out.write(responseHeader.getBytes());
                                out.flush();
                                System.out.println("printing response header:\n" + responseHeader);               
                                sendContent(page.getBytes(StandardCharsets.UTF_8), httpRequest.isChunked());
                            }
                            else {
                                String responseHeader = generateResponseHeader(404, requestedPage, null, null, false);
                                System.out.println("printing response header:\n" + responseHeader);
                                out.write(responseHeader.getBytes());
                                out.flush();
                            }   
                        }
                    }
                    else if(method.equals("TRACE")) {
                        String responseHeader = generateResponseHeader(200, null, null, "application/octet-stream", false);
                        System.out.println("printing response header:\n" + responseHeader);
                        out.write(responseHeader.getBytes());
                        out.write(requestHeaderBuilder.toString().getBytes());
                        out.flush();
                    }
                }
                else {
                    String responseHeader = generateResponseHeader(501, requestedPage, null, null, false);
                    System.out.println("printing response header:\n" + responseHeader);
                    out.write(responseHeader.getBytes());
                    out.flush();
                }
            } 
            catch (FileNotFoundException e) {
                String responseHeader = generateResponseHeader(404, null, null, null, false);
                System.out.println("printing response header:\n" + responseHeader);
                try {   
                    out.write(responseHeader.getBytes());
                    out.flush();
                }
                catch (IOException e1) {
                    System.out.println("Something went wrong with the input/output");
                }
            }
            catch (IOException e) {
                System.out.println("Something went wrong with the input/output");
            } catch (IllegalArgumentException e) {
                try {
                    if (out != null) {
                        System.out.println("printing request:\n" + e.getMessage());
                        String responseHeader = generateResponseHeader(400, null, null, null, false);
                        System.out.println("printing response header:\n" + responseHeader);
                        out.write(responseHeader.getBytes());
                        out.flush();
                    }
                } catch (IOException ex) {
                    System.out.println("Something went wrong with the input/output");
                }
            }
            catch (Exception e) {
                String responseHeader = generateResponseHeader(500, null, null, null, false);
                System.out.println("printing response header:\n" + responseHeader);
                try {
                    out.write(responseHeader.getBytes());
                    out.flush();
                }
                catch(IOException e1) {
                    System.out.println("Something went wrong with the input/output");
                }
            }
            finally{
                try {
                    if (clientSocket != null) {
                        clientSocket.close();
                    } 
                } catch (IOException e) {
                    System.out.println("Something went wrong with the input/output");
                }
            }
    }

        private byte[] readFile(File file) throws IOException {
            //given code in lab file
            FileInputStream fis = new FileInputStream(file); 
            byte[] bFile = new byte[(int)file.length()];
            // read until the end of the stream. 
            while(fis.available() != 0) {
                fis.read(bFile, 0, bFile.length); 
            }
            return bFile;
        }

        // chunked version
        private void sendContent(byte[] content, boolean isChunked) {
            try {
                    OutputStream os = clientSocket.getOutputStream();

                    if(isChunked) {
                        int offset = 0;
                        while (offset < content.length) {
                            int chunkSize = Math.min(1000, content.length - offset);

                            os.write(String.valueOf(chunkSize).getBytes(StandardCharsets.UTF_8));
                            os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                            os.write(content, offset, chunkSize);
                            os.write("\r\n".getBytes(StandardCharsets.UTF_8));
                            offset += chunkSize;
                        }

                        os.write("0\r\n\r\n".getBytes(StandardCharsets.UTF_8)); // Last chunk
                    } else {
                        os.write(content);
                    }
                    os.flush();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }

        private void handleParamsInfo(HTTPRequest httpRequest) {
            // Specify the path of the file you want to check
            String filePath = ROOT_DIRECTORY + "params_info.html";
            try
            {
                File file = new File(filePath);
                file.createNewFile();
                serverParams.putAll(httpRequest.getParameters());
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                    writer.write(generateParamsInfoPage(serverParams));
                } catch (IOException e) {
                    System.out.println("An error occurred while writing data to params_info.html");
                }
            }
            catch (IOException e) {
                System.out.println("An error occurred while creating params_info.html");
            }
        }

        private String normalizePath(String path) {
            String normalizedPath = path;
            if(path.contains("../")) {
                normalizedPath = normalizedPath.replaceAll("\\.\\./", "");
            }
            return normalizedPath;
        }

        private String generateResponseHeader(int statusCode, String filePath, byte[] content, String contentType, boolean isChunked) { 
            String statusMessage = switch (statusCode) {
                case 200 -> "OK";
                case 404 -> "Not Found";
                case 501 -> "Not Implemented";
                case 400 -> "Bad Request";
                case 500 -> "Internal Server Error";
                default -> "";
            };

            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage).append("\r\n");
            
            if (contentType != null) {
                responseBuilder.append("Content-Type: ").append(contentType).append("\r\n");
            }
            
            if (content != null) {
                responseBuilder.append("Content-Length: ").append(content.length).append("\r\n");
            }

            if (isChunked) {
                responseBuilder.append("Transfer-Encoding: chunked\r\n");
            }
            
            responseBuilder.append("\r\n");
            return responseBuilder.toString();
        }
    
        
        private String retrieveContentType(File file) {
            String filePath = file.getAbsolutePath();

            if (filePath.endsWith(".html")) {
                return "text/html";
            } else if (filePath.endsWith(".jpg") || filePath.endsWith(".gif") || filePath.endsWith(".png") ||filePath.endsWith(".bmp") ) {
                return "image";
            } else if (filePath.endsWith(".ico")) {
                return "icon";
            } else {
                return "application/octet-stream";
            }
        }
        

        private String generateParamsInfoPage(HashMap<String, String> params) {

            // Convert the HashMap to TreeMap to sort by keys
            TreeMap<String, String> sortedMap = new TreeMap<>(params);
            
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<!DOCTYPE html>\n");
            htmlBuilder.append("<html>\n");
            htmlBuilder.append("<head>\n");
            htmlBuilder.append("<title>Submitted Parameters</title>\n");
            htmlBuilder.append("</head>\n");
            htmlBuilder.append("<body>\n");
            htmlBuilder.append("<h1>Submitted Parameters</h1>\n");
            htmlBuilder.append("<ul>\n");
            for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
                htmlBuilder.append("<li>").append(entry.getKey()).append(": ").append(entry.getValue()).append("</li>\n"); }
            htmlBuilder.append("</ul>\n");
            htmlBuilder.append("</body>\n");
            htmlBuilder.append("</html>\n");
            return htmlBuilder.toString();
        }
    }
}