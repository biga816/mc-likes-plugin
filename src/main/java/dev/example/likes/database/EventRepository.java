package dev.example.likes.database;

import dev.example.likes.model.LikesEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Repository for accessing the likes_events table.
 */
public class EventRepository {

    private final DatabaseManager databaseManager;

    /**
     * Constructs an EventRepository.
     *
     * @param databaseManager the database connection manager
     */
    public EventRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Saves a like event to the database.
     *
     * @param event the event to save
     * @throws SQLException if a database operation fails
     */
    public void save(LikesEvent event) throws SQLException {
        String sql = """
            INSERT INTO likes_events
                (event_id, created_at, broadcast_id, sender_uuid, target_uuid)
            VALUES (?, ?, ?, ?, ?)
            """;
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, event.eventId());
            ps.setLong(2, event.createdAt());
            ps.setString(3, event.broadcastId());
            ps.setString(4, event.senderUuid().toString());
            ps.setString(5, event.targetUuid().toString());
            ps.executeUpdate();
        }
    }

    /**
     * Checks whether an event exists for the given broadcast ID and sender UUID.
     * Used to prevent duplicate likes on the same broadcast.
     *
     * @param broadcastId the broadcast ID to check
     * @param senderUuid  the sender's UUID to check
     * @return true if such an event already exists
     * @throws SQLException if a database operation fails
     */
    public boolean exists(String broadcastId, UUID senderUuid) throws SQLException {
        String sql = "SELECT 1 FROM likes_events WHERE broadcast_id = ? AND sender_uuid = ? LIMIT 1";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, broadcastId);
            ps.setString(2, senderUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
