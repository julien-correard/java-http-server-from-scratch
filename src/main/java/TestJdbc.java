import java.sql.Connection;
import java.sql.DriverManager;

public class TestJdbc {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://localhost:5433/clients_db";
        try (Connection c = DriverManager.getConnection(url, "postgres", "secret")) {
            System.out.println("Connecté : " + !c.isClosed());
        }
    }
}