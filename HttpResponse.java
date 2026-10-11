import java.io.PrintWriter;
import java.net.Socket;

public class HttpResponse {
    public static void send(Socket socket, int statusCode, String body) throws Exception {
    
    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
    
    String status;
    switch (statusCode) {
      case 200 : status = "OK";
        break;
      case 400 : status = "Bad Request";
        break;
      case 404 : status = "Not Found";
        break;
      case 500 : status = "Internal Server Error";
        break;
      default : status = "Unknown";
    }
    String statusLine = "HTTP/1.1 " +
                        String.valueOf(statusCode) + " " +
                        status + "\r\n";

    String contentTypeLine = "Content-Type: application/json\r\n";

    int contentLength = body.getBytes("UTF-8").length;
    String contentLine = "Content-Length: " +
                        String.valueOf(contentLength) +
                        "\r\n";
                        
    String blankLine = "\r\n";

    writer.print(statusLine);
    writer.print(contentTypeLine);
    writer.print(contentLine);
    writer.print(blankLine);
    writer.print(body);
    writer.flush();
    }
}
