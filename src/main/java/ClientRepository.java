import java.util.List;

public interface ClientRepository {
    Client findById(int id);
    List<Client> findAll();
    Client createNewClient(String name, String phone);
    Client deleteById(int id);
    Client updateClient(int id, String name, String phone);
}