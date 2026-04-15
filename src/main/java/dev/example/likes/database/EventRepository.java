package dev.example.likes.database;

import dev.example.likes.model.LikesEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
     * Returns the total number of events (reactions) for the given broadcast ID.
     *
     * @param broadcastId the broadcast ID to count
     * @return the number of events
     * @throws SQLException if a database operation fails
     */
    public int countByBroadcastId(String broadcastId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM likes_events WHERE broadcast_id = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, broadcastId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
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
     * Returns the reaction count for each of the given broadcast IDs in a single query.
     * Broadcast IDs with no reactions are not included in the returned map.
     *
     * @param broadcastIds the list of broadcast IDs to count
     * @return a map of broadcastId → reaction count
     * @throws SQLException if a database operation fails
     */
    public Map<String, Integer> countByBroadcastIds(List<String> broadcastIds) throws SQLException {
        if (broadcastIds.isEmpty()) return Map.of();
        String placeholders = broadcastIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = "SELECT broadcast_id, COUNT(*) FROM likes_events WHERE broadcast_id IN (" + placeholders + ") GROUP BY broadcast_id";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < broadcastIds.size(); i++) {
                ps.setString(i + 1, broadcastIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Integer> result = new HashMap<>();
                while (rs.next()) {
                    result.put(rs.getString(1), rs.getInt(2));
                }
                return result;
            }
        }
    }

    /**
     * Returns the set of broadcast IDs (from the given list) that the specified sender has already reacted to.
     *
     * @param broadcastIds the list of broadcast IDs to check
     * @param senderUuid   the sender's UUID
     * @return a set of broadcast IDs the sender has reacted to
     * @throws SQLException if a database operation fails
     */
    public Set<String> reactedBroadcastIds(List<String> broadcastIds, UUID senderUuid) throws SQLException {
        if (broadcastIds.isEmpty()) return Set.of();
        String placeholders = broadcastIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = "SELECT broadcast_id FROM likes_events WHERE broadcast_id IN (" + placeholders + ") AND sender_uuid = ?";
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
