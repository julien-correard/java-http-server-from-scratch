import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InMemoryClientRepository implements ClientRepository {

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

    public Client deleteById(int id){

        Client deleted = clients.remove(id);

        return deleted;
    }

    public Client updateClient(int id,String name,String phone){
    
        Client clientToUpdate = clients.get(id);

        if (clientToUpdate == null) return null;

        if (name != null) clientToUpdate.setName(name);
        if (phone != null) clientToUpdate.setPhone(phone);

        return clientToUpdate;
    }
}
