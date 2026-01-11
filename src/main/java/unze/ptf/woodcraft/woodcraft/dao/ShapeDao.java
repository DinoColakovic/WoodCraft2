package unze.ptf.woodcraft.woodcraft.dao;

import unze.ptf.woodcraft.woodcraft.db.Database;
import unze.ptf.woodcraft.woodcraft.model.ShapePolygon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ShapeDao {
    public ShapePolygon createShape(ShapePolygon shape) {
        String insertSql = """
            INSERT INTO shapes(document_id, material_id, quantity, node_ids, area_cm2, perimeter_cm)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = Database.getConnection();
             PreparedStatement insert = connection.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            insert.setInt(1, shape.getDocumentId());
            if (shape.getMaterialId() == null) {
                insert.setNull(2, java.sql.Types.INTEGER);
            } else {
                insert.setInt(2, shape.getMaterialId());
            }
            insert.setInt(3, shape.getQuantity());
            insert.setString(4, serializeNodeIds(shape));
            insert.setDouble(5, shape.getAreaCm2());
            insert.setDouble(6, shape.getPerimeterCm());
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return new ShapePolygon(
                            keys.getInt(1),
                            shape.getDocumentId(),
                            shape.getMaterialId(),
                            shape.getQuantity(),
                            shape.getNodeIds(),
                            shape.getNodes(),
                            shape.getAreaCm2(),
                            shape.getPerimeterCm()
                    );
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create shape", exception);
        }
        return shape;
    }

    public void deleteByDocument(int documentId) {
        String deleteSql = "DELETE FROM shapes WHERE document_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement delete = connection.prepareStatement(deleteSql)) {
            delete.setInt(1, documentId);
            delete.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete shapes", exception);
        }
    }

    public void replaceShapes(int documentId, List<ShapePolygon> shapes) {
        deleteByDocument(documentId);
        for (ShapePolygon shape : shapes) {
            createShape(shape);
        }
    }

    public List<ShapePolygon> findByDocument(int documentId) {
        String sql = "SELECT id, document_id, material_id, quantity, node_ids, area_cm2, perimeter_cm FROM shapes WHERE document_id = ?";
        List<ShapePolygon> shapes = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, documentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    List<Integer> nodeIds = parseNodeIds(resultSet.getString("node_ids"));
                    shapes.add(new ShapePolygon(
                            resultSet.getInt("id"),
                            resultSet.getInt("document_id"),
                            (Integer) resultSet.getObject("material_id"),
                            resultSet.getInt("quantity"),
                            nodeIds,
                            List.of(),
                            resultSet.getDouble("area_cm2"),
                            resultSet.getDouble("perimeter_cm")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load shapes", exception);
        }
        return shapes;
    }

    public void updateMaterial(int shapeId, Integer materialId) {
        String sql = "UPDATE shapes SET material_id = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (materialId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, materialId);
            }
            statement.setInt(2, shapeId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update shape material", exception);
        }
    }

    private String serializeNodeIds(ShapePolygon shape) {
        StringBuilder builder = new StringBuilder();
        List<Integer> nodeIds = shape.getNodeIds();
        if (nodeIds == null || nodeIds.isEmpty()) {
            nodeIds = new ArrayList<>();
            for (int index = 0; index < shape.getNodes().size(); index++) {
                nodeIds.add(shape.getNodes().get(index).getId());
            }
        }
        for (int index = 0; index < nodeIds.size(); index++) {
            builder.append(nodeIds.get(index));
            if (index < nodeIds.size() - 1) {
                builder.append(',');
            }
        }
        return builder.toString();
    }

    private List<Integer> parseNodeIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] parts = value.split(",");
        List<Integer> nodeIds = new ArrayList<>();
        for (String part : parts) {
            try {
                nodeIds.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                // ignore invalid entries
            }
        }
        return nodeIds;
    }
}
