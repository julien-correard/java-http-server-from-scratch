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

    private record ServerResponse(int status, String body) {}

    public static void main(String[] args) throws Exception {

        JdbcClientRepository clientRepository = new JdbcClientRepository("jdbc:postgresql://localhost:5433/clients_db", "postgres", "secret");

        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Serveur démarré sur le port 8080...");

            while (true) {

                Socket socket = null;

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

                    ServerResponse response = route(request, clientRepository);

                    HttpResponse.send(socket, response.status(), response.body());
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

    private static ServerResponse route(HttpRequest request, ClientRepository repo)
            throws BadRequestException {
        switch (request.method) {
            case "POST":
                return handlePost(request, repo);
            case "GET":
                return handleGet(request, repo);
            case "DELETE":
                return handleDelete(request, repo);
            case "PATCH":
                return handlePatch(request, repo);
            default:
                return error(405, "Méthode non autorisée");
        }
    }

    private static ServerResponse handlePost(HttpRequest request, ClientRepository repo)
            throws BadRequestException {
        Map<String, String> fields = new JsonParser(request.body).parseObject();

        Client created = repo.createNewClient(fields.get("name"), fields.get("phone"));

        return new ServerResponse(201, clientToJson(created));
    }

    private static ServerResponse handleGet(HttpRequest request, ClientRepository repo) {
        if (CLIENTS_PATH.equals(request.path)) {
            return new ServerResponse(200, clientsToJson(repo.findAll()));
        }

        if (!request.path.startsWith(CLIENTS_PATH + "/")) {
            return error(404, "Ressource introuvable");
        }

        Integer id = parseId(request.path);
        if (id == null) {
            return error(400, "Numéro de client invalide");
        }

        Client client = repo.findById(id);
        if (client == null) {
            return error(404, "Le client n'existe pas");
        }

        return new ServerResponse(200, clientToJson(client));
    }

    private static ServerResponse handleDelete(HttpRequest request, ClientRepository repo) {
        Integer id = parseId(request.path);
        if (id == null) {
            return error(400, "Client non spécifié");
        }

        if (repo.deleteById(id) == null) {
            return error(404, "Le client n'existe pas");
        }

        return new ServerResponse(204, "");
    }

    private static ServerResponse handlePatch(HttpRequest request, ClientRepository repo)
            throws BadRequestException {
        Integer id = parseId(request.path);
        if (id == null) {
            return error(400, "Client non spécifié");
        }

        Map<String, String> fields = new JsonParser(request.body).parseObject();

        Client updated = repo.updateClient(id, fields.get("name"), fields.get("phone"));
        if (updated == null) {
            return error(404, "Le client n'existe pas");
        }

        return new ServerResponse(200, clientToJson(updated));
    }

    private static Integer parseId(String path) {
        if (!path.startsWith(CLIENTS_PATH + "/")) {
            return null;
        }

        String number = path.substring(CLIENTS_PATH.length() + 1);
        try {
            return Integer.valueOf(number);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static ServerResponse error(int status, String message) {
        return new ServerResponse(status, "{\"error\":\"" + escape(message) + "\"}");
    }

    private static String clientToJson(Client client) {
        String json = "{\"id\":" + client.getId()
                + ",\"name\":\"" + escape(client.getName())
                + "\",\"phone\":\"" + escape(client.getPhone()) + "\"}";
        return json;
    }

    private static String clientsToJson(List<Client> clients) {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < clients.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(clientToJson(clients.get(i)));
        }

        return sb.append("]").toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

}
