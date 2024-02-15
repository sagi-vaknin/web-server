<a name="readme-top"></a>

<h3 align="center">Web-Server</h3>

  <p align="center">
   Multi Threaded Java Web Server
    <br />
    <a href="https://github.com/sagi-vaknin/web-server"><strong>Explore the docs »</strong></a>
  </p>
</div>

## About The Project
This Java application implements a multi-threaded web server capable of handling HTTP requests. 
It manages each new connection with a separate ClientHandler thread, allowing the main server thread to continue accepting new connections. 
Server parameters such as port, root directory, default page, and maximum threads can be configured via the config.ini file. 
It includes functionality to parse incoming requests using the HTTPRequest class, extracting relevant information such as requested page, request method, and content length. The server supports dynamic content generation for the params_info.html page (in case the page is missing), which displays submitted parameters in a structured format. Error handling is implemented to provide appropriate error responses for HTTP status codes such as 404 (Not Found), 400 (Bad Request), 501 (Not Implemented), and 500 (Internal Server Error).


<p align="right">(<a href="#readme-top">back to top</a>)</p>

### File Structure
* WebServer.java: Main class implementing the web server functionality.
* HTTPRequest.java: Class for parsing incoming HTTP requests and holding header values and body parameters.
* config.ini: Configuration file specifying server parameters such as port, root, default page, max threads.
* html folder: Includes index.html and its related files.

### Built With
* Java

### Instructions
* Ensure you have Java installed on your system.
* Modify the config.ini file to specify server configuration parameters such as port, root directory, default page, and maximum threads.
* Compile and run the server.

## Contact

Sagi Vaknin - sagivak1@gmail.com <br>
LinkedIn  - https://www.linkedin.com/in/sagi-vaknin-sv <br>
Project Link: [https://github.com/sagi-vaknin/web-server](https://github.com/sagi-vaknin/web-server)


<p align="right">(<a href="#readme-top">back to top</a>)</p>


