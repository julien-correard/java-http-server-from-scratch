import java.io.BufferedReader;
import java.util.Map;
import java.util.HashMap;


public class HttpRequest {
  public String method;
  public String path;
  public String version;
  public Map<String, String> headers = new HashMap<>();
  public int contentLength;
  public String body;
  private static final int MAX_BODY = 1_000_000;
  
  public HttpRequest(BufferedReader reader) throws Exception {
    


    headerReader (reader);

    bodyReader (reader);
      
    }
    
  

  private void headerReader (BufferedReader reader) throws Exception {
  
    String requestLine = reader.readLine();

    if (requestLine == null) throw new BadRequestException("Requête vide");

    String[] parts = requestLine.split(" ");

    if (parts.length < 3) throw new BadRequestException("Requête invalide");
    
    method = parts[0];
    path = parts[1];
    version = parts[2];

    String line;
    
    while ((line = reader.readLine()) != null && !line.isEmpty()) {
      int pos = line.indexOf(':');
        // position du premier ":"
      if (pos == -1) throw new BadRequestException ("Pas de ':' dans l'en tête");
      String key = line.substring(0, pos).trim().toLowerCase();
      String data = line.substring(pos +1).trim();     

      headers.put(key,data);
    }

    String contentLengthStr = headers.get("content-length");
    if (contentLengthStr == null) body = "";
    else {
      contentLength = 0;
      try {
        contentLength = Integer.parseInt(contentLengthStr);
        if (contentLength < 0) throw new BadRequestException("Content length négatif");
        if (contentLength > MAX_BODY) throw new BadRequestException ("Content length trop grand");
      } catch (NumberFormatException e){ 
        throw new BadRequestException("En tête invalide");
      }
    }
  }

  private void bodyReader (BufferedReader reader) throws Exception {
  int bytesRead = 0;
      StringBuilder sb = new StringBuilder();

      while (bytesRead < contentLength) {
        int c = reader.read();
        if (c == -1) throw new BadRequestException("Connexion perdue");
        char ch = (char) c;
        sb.append(ch);
        bytesRead += String.valueOf(ch).getBytes("UTF-8").length;
      }

      body = sb.toString();

  }
}