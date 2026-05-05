package dev.example.likes.database;

import dev.example.likes.model.LikesEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    /**
     * Returns recent broadcasts that the given player has reacted to, ordered by
     * event creation time descending. Used for the future {@code /like mine}
     * command.
     *
     * @param senderUuid the reactor's UUID
     * @param limit      maximum number of results
     * @return list of event records ordered by created_at DESC
     * @throws SQLException if a database error occurs
     */
    public List<dev.example.likes.model.LikesEvent> getRecentReactionsBy(UUID senderUuid, int limit)
            throws SQLException {
        String sql = """
                SELECT * FROM likes_events
                WHERE sender_uuid = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, senderUuid.toString());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<dev.example.likes.model.LikesEvent> results = new java.util.ArrayList<>();
                while (rs.next()) {
                    results.add(new dev.example.likes.model.LikesEvent(
                            rs.getString("event_id"),
                            rs.getLong("created_at"),
                            rs.getString("broadcast_id"),
                            UUID.fromString(rs.getString("sender_uuid")),
                            UUID.fromString(rs.getString("target_uuid"))));
                }
                return results;
            }
        }
    }

    /**
     * Returns the set of broadcast IDs (from the given list) that the specified
     * sender has already reacted to.
     *
     * @param broadcastIds the list of broadcast IDs to check
     * @param senderUuid   the sender's UUID
     * @return a set of broadcast IDs the sender has reacted to
     * @throws SQLException if a database operation fails
     */
    public Set<String> reactedBroadcastIds(List<String> broadcastIds, UUID senderUuid) throws SQLException {
        if (broadcastIds.isEmpty())
            return Set.of();
        String placeholders = broadcastIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = "SELECT broadcast_id FROM likes_events WHERE broadcast_id IN (" + placeholders
                + ") AND sender_uuid = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < broadcastIds.size(); i++) {
                ps.setString(i + 1, broadcastIds.get(i));
            }
            ps.setString(broadcastIds.size() + 1, senderUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                Set<String> result = new HashSet<>();
                while (rs.next()) {
                    result.add(rs.getString(1));
                }
                return result;
            }
        }
    }
}
