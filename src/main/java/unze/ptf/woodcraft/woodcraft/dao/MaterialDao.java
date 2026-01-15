package unze.ptf.woodcraft.woodcraft.dao;

import unze.ptf.woodcraft.woodcraft.db.Database;
import unze.ptf.woodcraft.woodcraft.model.Material;
import unze.ptf.woodcraft.woodcraft.model.GrainDirection;
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
            INSERT INTO materials(user_id, name, type, sheet_width_cm, sheet_height_cm, sheet_price,
                                  price_per_square_meter, price_per_linear_meter, image_path,
                                  grain_direction, edge_banding_cost_per_meter)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, material.getUserId());
            statement.setString(2, material.getName());
            statement.setString(3, material.getType().name());
            statement.setDouble(4, material.getSheetWidthCm());
            statement.setDouble(5, material.getSheetHeightCm());
            statement.setDouble(6, material.getSheetPrice());
            statement.setDouble(7, material.getPricePerSquareMeter());
            statement.setDouble(8, material.getPricePerLinearMeter());
            statement.setString(9, material.getImagePath());
            statement.setString(10, material.getGrainDirection().name());
            statement.setDouble(11, material.getEdgeBandingCostPerMeter());
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

    public void update(Material material) {
        String sql = """
            UPDATE materials
            SET name = ?, type = ?, sheet_width_cm = ?, sheet_height_cm = ?, sheet_price = ?,
                price_per_square_meter = ?, price_per_linear_meter = ?, image_path = ?,
                grain_direction = ?, edge_banding_cost_per_meter = ?
            WHERE id = ? AND user_id = ?
            """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, material.getName());
            statement.setString(2, material.getType().name());
            statement.setDouble(3, material.getSheetWidthCm());
            statement.setDouble(4, material.getSheetHeightCm());
            statement.setDouble(5, material.getSheetPrice());
            statement.setDouble(6, material.getPricePerSquareMeter());
            statement.setDouble(7, material.getPricePerLinearMeter());
            statement.setString(8, material.getImagePath());
            statement.setString(9, material.getGrainDirection().name());
            statement.setDouble(10, material.getEdgeBandingCostPerMeter());
            statement.setInt(11, material.getId());
            statement.setInt(12, material.getUserId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update material", exception);
        }
    }

    private Material mapRow(ResultSet resultSet) throws SQLException {
        return new Material(
                resultSet.getInt("id"),
                resultSet.getInt("user_id"),
                resultSet.getString("name"),
                MaterialType.valueOf(resultSet.getString("type")),
                resultSet.getDouble("sheet_width_cm"),
                resultSet.getDouble("sheet_height_cm"),
                resultSet.getDouble("sheet_price"),
                resultSet.getDouble("price_per_square_meter"),
                resultSet.getDouble("price_per_linear_meter"),
                resultSet.getString("image_path"),
                GrainDirection.valueOf(resultSet.getString("grain_direction")),
                resultSet.getDouble("edge_banding_cost_per_meter")
        );
    }
}
