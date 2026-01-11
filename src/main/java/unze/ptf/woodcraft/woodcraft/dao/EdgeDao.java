package unze.ptf.woodcraft.woodcraft.dao;

import unze.ptf.woodcraft.woodcraft.db.Database;
import unze.ptf.woodcraft.woodcraft.model.Edge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EdgeDao {
    public Edge create(int documentId, int startNodeId, int endNodeId) {
        String sql = "INSERT INTO edges(document_id, start_node_id, end_node_id) VALUES (?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, documentId);
            statement.setInt(2, startNodeId);
            statement.setInt(3, endNodeId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Edge(keys.getInt(1), documentId, startNodeId, endNodeId);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create edge", exception);
        }
        return new Edge(-1, documentId, startNodeId, endNodeId);
    }

    public void deleteByNode(int nodeId) {
        String sql = "DELETE FROM edges WHERE start_node_id = ? OR end_node_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, nodeId);
            statement.setInt(2, nodeId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete edges", exception);
        }
    }

    public List<Edge> findByDocument(int documentId) {
        String sql = "SELECT id, document_id, start_node_id, end_node_id FROM edges WHERE document_id = ?";
        List<Edge> edges = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, documentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    edges.add(new Edge(
                            resultSet.getInt("id"),
                            resultSet.getInt("document_id"),
                            resultSet.getInt("start_node_id"),
                            resultSet.getInt("end_node_id")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load edges", exception);
        }
        return edges;
    }
}
