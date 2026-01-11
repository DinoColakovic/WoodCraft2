package unze.ptf.woodcraft.woodcraft.dao;

import unze.ptf.woodcraft.woodcraft.db.Database;
import unze.ptf.woodcraft.woodcraft.model.Document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class DocumentDao {
    public int createDocument(int userId, String name) {
        String sql = "INSERT INTO documents(user_id, name) VALUES (?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setString(2, name);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create document", exception);
        }
        return -1;
    }

    public Optional<Document> findFirstByUser(int userId) {
        String sql = "SELECT id, user_id, name FROM documents WHERE user_id = ? ORDER BY id LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new Document(
                            resultSet.getInt("id"),
                            resultSet.getInt("user_id"),
                            resultSet.getString("name")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read document", exception);
        }
        return Optional.empty();
    }
}
