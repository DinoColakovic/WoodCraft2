package unze.ptf.woodcraft.woodcraft.dao;

import unze.ptf.woodcraft.woodcraft.db.Database;
import unze.ptf.woodcraft.woodcraft.model.Role;
import unze.ptf.woodcraft.woodcraft.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    public int createUser(String username, String passwordHash, Role role) {
        String sql = "INSERT INTO users(username, password_hash, role) VALUES (?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, role.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create user", exception);
        }
        return -1;
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE username = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read user", exception);
        }
        return Optional.empty();
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read user", exception);
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        String sql = "SELECT id, username, password_hash, role FROM users ORDER BY username";
        List<User> users = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(mapRow(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list users", exception);
        }
        return users;
    }

    public long countUsers() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to count users", exception);
        }
        return 0;
    }

    public void updatePassword(int userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update password", exception);
        }
    }

    public void updateRole(int userId, Role role) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role.name());
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update role", exception);
        }
    }

    private User mapRow(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getInt("id"),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
                Role.valueOf(resultSet.getString("role"))
        );
    }
}
