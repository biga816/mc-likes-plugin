package dev.example.likes.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Manages the SQLite database connection and schema.
 * <p>
 * Holds a long-lived connection that is initialized on plugin startup and
 * closed on shutdown. All write access must go through
 * {@link DatabaseWriteExecutor}; this class provides a
 * {@link #executeInTransaction} helper for transactional write tasks.
 * </p>
 */
public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    private final String jdbcUrl;
    private Connection connection;

    /**
     * Constructs a DatabaseManager.
     *
     * @param dataFolder the plugin's data folder; likes.db will be created inside
     *                   it
     * @throws ClassNotFoundException if the SQLite JDBC driver is not found
     */
    public DatabaseManager(File dataFolder) throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        this.jdbcUrl = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + "/likes.db";
    }

    /**
     * Establishes the database connection and initializes the schema.
     * Applies required PRAGMAs and creates all tables and indexes.
     *
     * @throws SQLException if a database operation fails
     */
    public void initialize() throws SQLException {
        connection = DriverManager.getConnection(jdbcUrl);
        LOGGER.info("SQLite database connected: " + jdbcUrl);

        try (Statement stmt = connection.createStatement()) {

            // ── PRAGMAs ────────────────────────────────────────────────────────
            // WAL mode: allows concurrent reads while a single writer is active.
            stmt.execute("PRAGMA journal_mode=WAL");
            // Busy timeout: secondary safeguard in case of unexpected contention.
            stmt.execute("PRAGMA busy_timeout=5000");
            // Enforce foreign-key constraints.
            stmt.execute("PRAGMA foreign_keys=ON");

            // ── Core tables ────────────────────────────────────────────────────

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS likes_broadcasts (
                        broadcast_id        TEXT PRIMARY KEY,
                        server_id           TEXT NOT NULL,
                        display_code        TEXT NOT NULL,
                        created_at          INTEGER NOT NULL,
                        source_type         TEXT NOT NULL,
                        source_sender_uuid  TEXT NOT NULL,
                        target_uuid         TEXT NOT NULL,
                        reason_code         TEXT NOT NULL,
                        reason_text         TEXT NOT NULL
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS likes_events (
                        event_id     TEXT PRIMARY KEY,
                        server_id    TEXT NOT NULL,
                        created_at   INTEGER NOT NULL,
                        broadcast_id TEXT NOT NULL,
                        sender_uuid  TEXT NOT NULL,
                        target_uuid  TEXT NOT NULL,
                        FOREIGN KEY(broadcast_id) REFERENCES likes_broadcasts(broadcast_id),
                        UNIQUE(broadcast_id, sender_uuid)
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS likes_sender_daily (
                        date              TEXT NOT NULL,
                        server_id         TEXT NOT NULL,
                        sender_uuid       TEXT NOT NULL,
                        direct_like_count INTEGER NOT NULL,
                        PRIMARY KEY(date, server_id, sender_uuid)
                    )
                    """);

            // ── Aggregation tables ─────────────────────────────────────────────

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS like_player_stats (
                        server_id      TEXT NOT NULL,
                        player_uuid    TEXT NOT NULL,
                        player_name    TEXT NOT NULL,
                        received_count INTEGER NOT NULL DEFAULT 0,
                        sent_count     INTEGER NOT NULL DEFAULT 0,
                        reacted_count  INTEGER NOT NULL DEFAULT 0,
                        updated_at     INTEGER NOT NULL,
                        PRIMARY KEY(server_id, player_uuid)
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS like_broadcast_stats (
                        broadcast_id   TEXT PRIMARY KEY,
                        server_id      TEXT NOT NULL,
                        reaction_count INTEGER NOT NULL DEFAULT 0,
                        updated_at     INTEGER NOT NULL,
                        FOREIGN KEY(broadcast_id)
                            REFERENCES likes_broadcasts(broadcast_id)
                            ON DELETE CASCADE
                    )
                    """);

            // ── Indexes on core tables ─────────────────────────────────────────

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_likes_broadcasts_server_created_at
                    ON likes_broadcasts(server_id, created_at DESC)
                    """);

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_likes_broadcasts_server_display_code_created_at
                    ON likes_broadcasts(server_id, display_code, created_at DESC)
                    """);

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_likes_broadcasts_server_target
                    ON likes_broadcasts(server_id, target_uuid, created_at DESC)
                    """);

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_likes_events_server_broadcast_sender
                    ON likes_events(server_id, broadcast_id, sender_uuid)
                    """);

            // ── Indexes on aggregation tables ──────────────────────────────────

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_like_player_stats_server_received
                    ON like_player_stats(server_id, received_count DESC)
                    """);

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_like_player_stats_server_sent
                    ON like_player_stats(server_id, sent_count DESC)
                    """);

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_like_player_stats_server_reacted
                    ON like_player_stats(server_id, reacted_count DESC)
                    """);

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_like_broadcast_stats_server_reaction
                    ON like_broadcast_stats(server_id, reaction_count DESC)
                    """);
        }

        LOGGER.info("Database schema initialized successfully.");
    }

    /**
     * Returns the current database connection.
     * <p>
     * Write operations must be invoked from within a task submitted to
     * {@link DatabaseWriteExecutor} to guarantee serialization.
     * </p>
     *
     * @return the SQLite connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Executes a transactional write task on the current connection.
     * <p>
     * Sets {@code autoCommit=false}, runs the task, then commits. If the task
     * throws, the transaction is rolled back and the exception is re-thrown.
     * This method must be called from within a task submitted to
     * {@link DatabaseWriteExecutor}.
     * </p>
     *
     * @param task the transactional task to execute
     * @throws SQLException if a database error occurs or the task throws
     */
    public void executeInTransaction(TransactionTask task) throws SQLException {
        connection.setAutoCommit(false);
        try {
            task.execute(connection);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
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

    /** Functional interface for a transactional SQL task. */
    @FunctionalInterface
    public interface TransactionTask {
        void execute(Connection conn) throws SQLException;
    }
}
