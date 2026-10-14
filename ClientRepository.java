import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClientRepository {

    private Map <Integer, Client> clients = new HashMap<>();
    private int clientIndex = 1;

    public Client findById(int id){

        Client client = clients.get(id);

        return client;
    }

    public List<Client> findAll(){

        List<Client> allClients = new ArrayList<>(clients.values());

        return allClients;
    }

    public Client createNewClient(String name, String phone){
        
        Client newClient = new Client (clientIndex, name, phone);
        
        clients.put(clientIndex, newClient);
        
        clientIndex++;

        return newClient;

    }
}
