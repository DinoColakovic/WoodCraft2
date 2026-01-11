package unze.ptf.woodcraft.woodcraft.dao;

import unze.ptf.woodcraft.woodcraft.db.Database;
import unze.ptf.woodcraft.woodcraft.model.Material;
import unze.ptf.woodcraft.woodcraft.model.MaterialType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MaterialDao {
    public int create(Material material) {
        String sql = """
            INSERT INTO materials(user_id, name, type, color_hex, sheet_width_cm, sheet_height_cm, sheet_price,
                                  price_per_square_meter, price_per_linear_meter)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, material.getUserId());
            statement.setString(2, material.getName());
            statement.setString(3, material.getType().name());
            statement.setString(4, material.getColorHex());
            statement.setDouble(5, material.getSheetWidthCm());
            statement.setDouble(6, material.getSheetHeightCm());
            statement.setDouble(7, material.getSheetPrice());
            statement.setDouble(8, material.getPricePerSquareMeter());
            statement.setDouble(9, material.getPricePerLinearMeter());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create material", exception);
        }
        return -1;
    }

    public List<Material> findByUser(int userId) {
        String sql = "SELECT * FROM materials WHERE user_id = ? ORDER BY name";
        List<Material> materials = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    materials.add(mapRow(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load materials", exception);
        }
        return materials;
    }

    public Optional<Material> findById(int materialId) {
        String sql = "SELECT * FROM materials WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, materialId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load material", exception);
        }
        return Optional.empty();
    }

    private Material mapRow(ResultSet resultSet) throws SQLException {
        return new Material(
                resultSet.getInt("id"),
                resultSet.getInt("user_id"),
                resultSet.getString("name"),
                MaterialType.valueOf(resultSet.getString("type")),
                resultSet.getString("color_hex"),
                resultSet.getDouble("sheet_width_cm"),
                resultSet.getDouble("sheet_height_cm"),
                resultSet.getDouble("sheet_price"),
                resultSet.getDouble("price_per_square_meter"),
                resultSet.getDouble("price_per_linear_meter")
        );
    }
}
