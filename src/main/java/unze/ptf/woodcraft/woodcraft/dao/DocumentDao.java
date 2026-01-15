package unze.ptf.woodcraft.woodcraft.dao;

import unze.ptf.woodcraft.woodcraft.db.Database;
import unze.ptf.woodcraft.woodcraft.model.Document;
import unze.ptf.woodcraft.woodcraft.model.UnitSystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class DocumentDao {
    public int createDocument(int userId, String name) {
        return createDocument(userId, name, 244, 122, 3.0, UnitSystem.CM);
    }

    public int createDocument(int userId, String name, double widthCm, double heightCm, double kerfMm, UnitSystem unitSystem) {
        String sql = """
            INSERT INTO documents(user_id, name, width_cm, height_cm, kerf_mm, unit_system)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setString(2, name);
            statement.setDouble(3, widthCm);
            statement.setDouble(4, heightCm);
            statement.setDouble(5, kerfMm);
            statement.setString(6, unitSystem.name());
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
        String sql = "SELECT id, user_id, name, width_cm, height_cm, kerf_mm, unit_system FROM documents WHERE user_id = ? ORDER BY id LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read document", exception);
        }
        return Optional.empty();
    }

    public Optional<Document> findById(int documentId, int userId) {
        String sql = "SELECT id, user_id, name, width_cm, height_cm, kerf_mm, unit_system FROM documents WHERE id = ? AND user_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, documentId);
            statement.setInt(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read document", exception);
        }
        return Optional.empty();
    }

    public java.util.List<Document> findByUser(int userId) {
        String sql = "SELECT id, user_id, name, width_cm, height_cm, kerf_mm, unit_system FROM documents WHERE user_id = ? ORDER BY id DESC";
        java.util.List<Document> documents = new java.util.ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    documents.add(mapRow(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list documents", exception);
        }
        return documents;
    }

    public void updateSettings(int documentId, double widthCm, double heightCm, double kerfMm, UnitSystem unitSystem) {
        String sql = "UPDATE documents SET width_cm = ?, height_cm = ?, kerf_mm = ?, unit_system = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, widthCm);
            statement.setDouble(2, heightCm);
            statement.setDouble(3, kerfMm);
            statement.setString(4, unitSystem.name());
            statement.setInt(5, documentId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update document settings", exception);
        }
    }

    private Document mapRow(ResultSet resultSet) throws SQLException {
        String unitValue = resultSet.getString("unit_system");
        UnitSystem unitSystem = unitValue == null ? UnitSystem.CM : UnitSystem.valueOf(unitValue);
        return new Document(
                resultSet.getInt("id"),
                resultSet.getInt("user_id"),
                resultSet.getString("name"),
                resultSet.getDouble("width_cm"),
                resultSet.getDouble("height_cm"),
                resultSet.getDouble("kerf_mm"),
                unitSystem
        );
    }
}
