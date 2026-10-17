public class TestClientRepo {
    public static void main(String[] args){

        InMemoryClientRepository clientRepository = new InMemoryClientRepository();

        Client client1 = clientRepository.createNewClient("Dupont", "0675654512");
        Client client2 = clientRepository.createNewClient("Michel", "0465542312");

        System.out.println(clientRepository.findById(client1.getId()));
        System.out.println(clientRepository.findById(client2.getId()));
    }
}
