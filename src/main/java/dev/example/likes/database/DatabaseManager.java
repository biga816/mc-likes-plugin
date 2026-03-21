package dev.example.likes.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Manages the SQLite database connection.
 * Holds a long-lived connection that is initialized on plugin startup and closed on shutdown.
 */
public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    private final String jdbcUrl;
    private Connection connection;

    /**
     * Constructs a DatabaseManager.
     *
     * @param dataFolder the plugin's data folder; likes.db will be created inside it
     * @throws ClassNotFoundException if the SQLite JDBC driver is not found
     */
    public DatabaseManager(File dataFolder) throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        this.jdbcUrl = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + "/likes.db";
    }

    /**
     * Establishes the database connection and initializes tables.
     * Enables WAL mode and creates any missing tables.
     *
     * @throws SQLException if a database operation fails
     */
    public void initialize() throws SQLException {
        connection = DriverManager.getConnection(jdbcUrl);
        LOGGER.info("SQLite database connected: " + jdbcUrl);

        try (Statement stmt = connection.createStatement()) {
            // Enable WAL mode for better write performance
            stmt.execute("PRAGMA journal_mode=WAL");

            // likes_broadcasts table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS likes_broadcasts (
                    broadcast_id TEXT PRIMARY KEY,
                    short_id TEXT UNIQUE NOT NULL,
                    created_at INTEGER NOT NULL,
                    source_type TEXT NOT NULL,
                    source_sender_uuid TEXT NOT NULL,
                    target_uuid TEXT NOT NULL,
                    reason_code TEXT NOT NULL,
                    reason_text TEXT NOT NULL
                )
                """);

            // likes_events table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS likes_events (
                    event_id TEXT PRIMARY KEY,
                    created_at INTEGER NOT NULL,
                    broadcast_id TEXT NOT NULL,
                    sender_uuid TEXT NOT NULL,
                    target_uuid TEXT NOT NULL,
                    FOREIGN KEY(broadcast_id) REFERENCES likes_broadcasts(broadcast_id),
                    UNIQUE(broadcast_id, sender_uuid)
                )
                """);

            // likes_sender_daily table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS likes_sender_daily (
                    date TEXT NOT NULL,
                    sender_uuid TEXT NOT NULL,
                    direct_like_count INTEGER NOT NULL,
                    PRIMARY KEY(date, sender_uuid)
                )
                """);
        }

        LOGGER.info("Database tables initialized successfully.");
    }

    /**
     * Returns the current database connection.
     *
     * @return the SQLite connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Closes the database connection.
     * Must be called when the plugin is disabled.
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                LOGGER.info("Database connection closed.");
            } catch (SQLException e) {
                LOGGER.warning("Failed to close database connection: " + e.getMessage());
            }
        }
    }
}
