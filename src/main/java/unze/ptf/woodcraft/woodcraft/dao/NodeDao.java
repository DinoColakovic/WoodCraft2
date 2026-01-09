package unze.ptf.woodcraft.woodcraft.dao;

import unze.ptf.woodcraft.woodcraft.db.Database;
import unze.ptf.woodcraft.woodcraft.model.NodePoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NodeDao {
    public NodePoint create(int documentId, double xCm, double yCm) {
        String sql = "INSERT INTO nodes(document_id, x_cm, y_cm) VALUES (?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, documentId);
            statement.setDouble(2, xCm);
            statement.setDouble(3, yCm);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new NodePoint(keys.getInt(1), documentId, xCm, yCm);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create node", exception);
        }
        return new NodePoint(-1, documentId, xCm, yCm);
    }

    public List<NodePoint> findByDocument(int documentId) {
        String sql = "SELECT id, document_id, x_cm, y_cm FROM nodes WHERE document_id = ?";
        List<NodePoint> nodes = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, documentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    nodes.add(new NodePoint(
                            resultSet.getInt("id"),
                            resultSet.getInt("document_id"),
                            resultSet.getDouble("x_cm"),
                            resultSet.getDouble("y_cm")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load nodes", exception);
        }
        return nodes;
    }
}
