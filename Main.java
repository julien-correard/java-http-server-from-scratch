import java.io.BufferedReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws Exception {

        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("Serveur démarré sur le port 8080...");

            while (true) {

                Socket socket = null;
                


                
                try {
                    socket = serverSocket.accept();
                    System.out.println("Client connecté");


                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                    );

                    HttpRequest request = new HttpRequest(reader);
                    System.out.println("Méthode : " + request.method);
                    System.out.println("Chemin  : " + request.path);

                    System.out.println(request.headers);

                    HttpResponse.send(socket, 200, request.body);
                } catch (BadRequestException e) {
                    try {
                        HttpResponse.send(socket, 400, "{\"error\": \"Requête invalide\"}");
                    } catch (Exception f) {
                        System.out.println("Client injoignable");
                    }
                    System.out.println(e.getMessage());
                } catch (Exception e) {
                    if (socket != null) {
                        try {
                            HttpResponse.send(socket, 500, "{\"error\": \"Erreur du serveur\"}");
                        } catch (Exception f) {
                            System.out.println("Client injoignable");
                        }
                    }
                    e.printStackTrace();
                } finally {
                    if (socket != null) 
                        {try {

                            
                            socket.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            }

        //serverSocket.close();
    }
}
