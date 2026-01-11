package unze.ptf.woodcraft.woodcraft.dao;

import unze.ptf.woodcraft.woodcraft.db.Database;
import unze.ptf.woodcraft.woodcraft.model.Guide;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GuideDao {
    public Guide create(int documentId, Guide.Orientation orientation, double positionCm) {
        String sql = "INSERT INTO guides(document_id, orientation, position_cm) VALUES (?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, documentId);
            statement.setString(2, orientation.name());
            statement.setDouble(3, positionCm);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Guide(keys.getInt(1), documentId, orientation, positionCm);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create guide", exception);
        }
        return new Guide(-1, documentId, orientation, positionCm);
    }

    public List<Guide> findByDocument(int documentId) {
        String sql = "SELECT id, document_id, orientation, position_cm FROM guides WHERE document_id = ?";
        List<Guide> guides = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, documentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    guides.add(new Guide(
                            resultSet.getInt("id"),
                            resultSet.getInt("document_id"),
                            Guide.Orientation.valueOf(resultSet.getString("orientation")),
                            resultSet.getDouble("position_cm")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load guides", exception);
        }
        return guides;
    }
}
