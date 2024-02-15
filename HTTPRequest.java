import java.util.HashMap;
import java.util.Set;

public class HTTPRequest {
    private static final Set<String> HTTP_METHODS = Set.of("CONNECT", "DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT", "TRACE");
    private String request;
    private String type;
    private String requestedPage;
    private boolean isImage;
    private int contentLength;
    private String referer;
    private String userAgent;
    private boolean isChunked;
    private HashMap<String, String> parameters;

    public HTTPRequest(String requestHeader, String requestBody) {
        // Parse the HTTP request header
        parseRequestHeader(requestHeader);
        if (requestBody != "") {
            // extract params from body
            parseParameters(requestBody);
        }
    }

    private void parseRequestHeader(String requestHeader) throws IllegalArgumentException {
        parameters = new HashMap<>();
        // Split the request header into lines
        String[] lines = requestHeader.split("\r\n");
        // Extract the first line to get the request type and requested page
        String firstLine = lines[0];
        String[] firstLineParts = firstLine.split("\\s+");
        request = firstLine;
        type = firstLineParts[0];
        if(firstLineParts.length > 1 && firstLineParts[1].contains("?"))
        {
            // if URL contains params
            String[] pageAndParams = firstLineParts[1].split("\\?");
            requestedPage = pageAndParams[0];
            parseParameters(pageAndParams[1]);
        }
        else
        {
            requestedPage = firstLineParts[1];
        }
        // Check if the requested page has an image extension
        isImage = requestedPage.matches(".*\\.(jpg|bmp|gif|png)$");

        // Parse other headers for content length, referer, user agent, and parameters
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("Content-Length:") ) {
                contentLength = Integer.parseInt(line.split(":")[1].strip());
            } else if (line.startsWith("Referer:")) {
                referer = line.substring("Referer:".strip().length());
            } else if (line.startsWith("User-Agent:")) {
                userAgent = line.substring("User-Agent:".strip().length());
            } else if (line.startsWith("chunked: yes") ||line.startsWith("chunked:yes") ) {
                isChunked = true;
            } 
        }
        // if method is POST and also has params in URL (extracting params from referer because firstLine == "params_info.html" at this point)
        if (type.equals("POST") && referer != null && referer.contains("?")) {
            String[] prms = referer.split("\\?");
            parseParameters(prms[1]);
        }
        checkValidity(this.toString(), firstLine);
    }
    public String getRequest() {
        return request;
    }

    public String getType() {
        return type;
    }

    public String getRequestedPage() {
        return requestedPage;
    }

    public boolean isImage() {
        return isImage;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getReferer() {
        return referer;
    }

    public boolean isChunked() {
        return isChunked;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public HashMap<String, String> getParameters() {
        return parameters;
    }
    // not needed can be deleted
    public void setParameters(HashMap<String, String> parameters) {
        this.parameters.putAll(parameters);
    } 

    public void parseParameters(String line)
    {
        // Parsing parameters from query string or form data
        String[] paramParts = line.split("&");
        for (String paramPart : paramParts) {
            String[] keyValue = paramPart.split("=");
            String key = keyValue[0];
            String value = keyValue.length > 1 ? keyValue[1] : "";
            parameters.put(key, value);
        }
    }

    private void checkValidity(String httpRequest, String httpHeader)
    {
        String[] firstLineParts = httpHeader.split("\\s+");
        
        if(HTTP_METHODS.contains(firstLineParts[0]) == false){
            throw new IllegalArgumentException(httpRequest);
        }

        if(firstLineParts.length == 2 && firstLineParts[1].startsWith("/")){
            return;
        }
        
        else if(firstLineParts.length == 3 ){
            if(!(firstLineParts[2].equals("HTTP/1.0") || firstLineParts[2].equals("HTTP/1.1")) || !(firstLineParts[1].startsWith("/")))
            {
                throw new IllegalArgumentException(httpRequest);
            }
        }

        else{
            throw new IllegalArgumentException(httpRequest);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP Request:\n");
        sb.append(request).append("\n");
        sb.append("Type: ").append(type).append("\n");
        sb.append("Requested Page: ").append(requestedPage).append("\n");
        sb.append("Is Image: ").append(isImage).append("\n");
        sb.append("Content Length: ").append(contentLength).append("\n");
        sb.append("Referer: ").append(referer).append("\n");
        sb.append("User Agent: ").append(userAgent).append("\n");
        sb.append("Parameters:\n");
        for (String key : parameters.keySet()) {
            sb.append("  ").append(key).append(": ").append(parameters.get(key)).append("\n");
        }
        return sb.toString();
    }
}
