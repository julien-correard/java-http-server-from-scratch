import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcClientRepository implements ClientRepository {

    private final String url;
    private final String user;
    private final String password;

    public JdbcClientRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public List<Client> findAll() {
        List<Client> result = new ArrayList<>();
        String sql = "SELECT id, name, phone FROM clients";
        try (Connection c = connect();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Client(rs.getInt("id"), rs.getString("name"), rs.getString("phone")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lecture clients", e);
        }
        return result;
    }

    @Override
    public Client findById(int id) {
        String sql = "SELECT id, name, phone FROM clients WHERE id = ?";
        try (Connection c = connect();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Client(rs.getInt("id"), rs.getString("name"), rs.getString("phone"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lecture client " + id, e);
        }
    }

    @Override
    public Client createNewClient(String name, String phone) {
        String sql = """
                INSERT INTO clients (name, phone)
                VALUES (?, ?)
                RETURNING id
                """;
        try (Connection c = connect();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, phone);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next(); // il y a forcément une ligne
                int id = rs.getInt("id");
                return new Client(id, name, phone);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur création client", e);
        }
    }

    @Override
    public Client deleteById(int id) {
        String sql = """
                DELETE FROM clients
                WHERE id = ?
                RETURNING id, name, phone
                """;
        try (Connection c = connect();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Client(rs.getInt("id"), rs.getString("name"), rs.getString("phone"));
                }
                return null; // aucun client avec cet id
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression client " + id, e);
        }
    }

    @Override
    public Client updateClient(int id, String name, String phone) {
        try (Connection c = connect()) {
            String oldName, oldPhone;

            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT name, phone FROM clients WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next())
                        return null; // inexistant : on s'arrête
                    oldName = rs.getString("name");
                    oldPhone = rs.getString("phone");
                }
            }

            String newName = (name != null) ? name : oldName;
            String newPhone = (phone != null) ? phone : oldPhone;

            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE clients SET name = ?, phone = ? WHERE id = ?")) {
                ps.setString(1, newName);
                ps.setString(2, newPhone);
                ps.setInt(3, id);
                ps.executeUpdate();
            }
            return new Client(id, newName, newPhone);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur modification client " + id, e);
        }
    }

}