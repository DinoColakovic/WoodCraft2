package unze.ptf.woodcraft.woodcraft.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private static final String DB_FILE = "woodcraft.db";
    private static final String JDBC_PREFIX = "jdbc:sqlite:";

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        ensureDirectory();
        String dbPath = getDatabasePath().toString();
        return DriverManager.getConnection(JDBC_PREFIX + dbPath);
    }

    public static Path getDatabasePath() {
        return Path.of(System.getProperty("user.home"), ".woodcraft", DB_FILE);
    }

    private static void ensureDirectory() {
        try {
            Path dir = Path.of(System.getProperty("user.home"), ".woodcraft");
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (Exception ignored) {
            // Directory creation failure will surface when connection is attempted.
        }
    }
}
