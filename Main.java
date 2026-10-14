import java.io.BufferedReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;

public class Main {

    private static final int SOCKET_TIME_OUT = 10000;
    private static final String CLIENTS_PATH = "/clients";

    public static void main(String[] args) throws Exception {

        ClientRepository clientRepository = new ClientRepository();

        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Serveur démarré sur le port 8080...");

            while (true) {

                Socket socket = null;

                int statusCode = 0;
                String responseBody = "";

                try {
                    socket = serverSocket.accept();

                    System.out.println("Client connecté");

                    socket.setSoTimeout(SOCKET_TIME_OUT);

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                    HttpRequest request = new HttpRequest(reader);
                    System.out.println("Méthode : " + request.method);
                    System.out.println("Chemin  : " + request.path);

                    System.out.println(request.headers);

                    if ("POST".equals(request.method)) {
                        JsonParser parser = new JsonParser(request.body);

                        Map<String, String> newClient = parser.parseObject();

                        String name = newClient.get("name");
                        String phone = newClient.get("phone");

                        Client created = clientRepository.createNewClient(name, phone);

                        statusCode = 201;

                        responseBody = clientToJson(created);
                    } else if ("GET".equals(request.method)) {
                        if (CLIENTS_PATH.equals(request.path)) {
                            List<Client> clients = clientRepository.findAll();

                            statusCode = 200;

                            StringBuilder sb = new StringBuilder();

                            sb.append("[");

                            for (int i = 0; i < clients.size(); i++) {
                                sb.append(clientToJson(clients.get(i)));
                                if (i < clients.size() - 1)
                                    sb.append(",");
                            }

                            sb.append("]");

                            responseBody = sb.toString();
                        } else if (request.path.startsWith(CLIENTS_PATH + "/")) {

                            String numberStr = request.path.substring(CLIENTS_PATH.length() + 1);
                            try {
                                int clientNumber = Integer.parseInt(numberStr);

                                Client client = clientRepository.findById(clientNumber);
                                if (client == null) {
                                    statusCode = 404;
                                    responseBody = "{\"error\":\"Le client n'existe pas\"}";
                                } else {
                                    statusCode = 200;
                                    responseBody = clientToJson(client);
                                }
                            } catch (NumberFormatException e) {
                                statusCode = 400;
                                responseBody = "{\"error\":\"Numéro de client invalide\"}";
                            }
                        } else {
                            statusCode = 404;
                            responseBody = "{\"error\":\"Ressource introuvable\"}";
                        }

                    } else if ("DELETE".equals(request.method)) {
                        if (request.path.startsWith(CLIENTS_PATH + "/")) {
                            String numberStr = request.path.substring(CLIENTS_PATH.length() + 1);
                            try {
                                int clientNumber = Integer.parseInt(numberStr);

                                Client deleted = clientRepository.deleteById(clientNumber);

                                if (deleted == null) {
                                    statusCode = 404;
                                    responseBody = "{\"error\":\"Le client n'existe pas\"}";
                                } else {
                                    statusCode = 204;
                                    responseBody = "";
                                }

                            } catch (NumberFormatException e) {
                                statusCode = 404;
                                responseBody = "{\"error\":\"Ressource introuvable\"}";
                            }

                        } else {
                            statusCode = 400;
                            responseBody = "{\"error\":\"méthode non reconnue\"}";
                        }

                    }

                    HttpResponse.send(socket, statusCode, responseBody);
                } catch (BadRequestException e) {
                    try {
                        HttpResponse.send(socket, 400, "{\"error\": \"Requête invalide\"}");
                    } catch (Exception f) {
                        System.out.println("Client injoignable");
                    }
                    System.out.println(e.getMessage());

                } catch (SocketTimeoutException e) {
                    try {
                        HttpResponse.send(socket, 408, "{\"error\": \"Requête time out\"}");
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
                    if (socket != null) {
                        try {

                            socket.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }

    }

    private static String clientToJson(Client client) {

        int id = client.getId();
        String name = client.getName();
        String phone = client.getPhone();
        String json = "{\"id\":" + id + ",\"name\":\"" + name + "\",\"phone\":\"" + phone + "\"}";
        return json;

    }

}
