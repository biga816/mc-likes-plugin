package dev.example.likes.database;

import dev.example.likes.model.LikesBroadcast;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for accessing the likes_broadcasts table.
 */
public class BroadcastRepository {

    private final DatabaseManager databaseManager;

    /**
     * Constructs a BroadcastRepository.
     *
     * @param databaseManager the database connection manager
     */
    public BroadcastRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Saves a broadcast to the database.
     *
     * @param broadcast the broadcast to save
     * @throws SQLException if a database operation fails
     */
    public void save(LikesBroadcast broadcast) throws SQLException {
        String sql = """
                INSERT INTO likes_broadcasts
                    (broadcast_id, display_code, created_at, source_type, source_sender_uuid, target_uuid, reason_code, reason_text)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, broadcast.broadcastId());
            ps.setString(2, broadcast.displayCode());
            ps.setLong(3, broadcast.createdAt());
            ps.setString(4, broadcast.sourceType());
            ps.setString(5, broadcast.sourceSenderUuid().toString());
            ps.setString(6, broadcast.targetUuid().toString());
            ps.setString(7, broadcast.reasonCode());
            ps.setString(8, broadcast.reasonText());
            ps.executeUpdate();
        }
    }

    /**
     * Finds the most recent broadcast matching the given displayCode.
     *
     * @param displayCode the display code to search for
     * @return an Optional containing the most recent matching broadcast, or empty if not found
     * @throws SQLException if a database operation fails
     */
    public Optional<LikesBroadcast> findLatestByDisplayCode(String displayCode) throws SQLException {
        String sql = "SELECT * FROM likes_broadcasts WHERE display_code = ? ORDER BY created_at DESC LIMIT 1";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, displayCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Retrieves recent broadcasts ordered by creation time descending.
     *
     * @param limit maximum number of results to return
     * @return list of broadcasts ordered by created_at DESC
     * @throws SQLException if a database operation fails
     */
    public List<LikesBroadcast> findRecent(int limit) throws SQLException {
        String sql = "SELECT * FROM likes_broadcasts ORDER BY created_at DESC LIMIT ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<LikesBroadcast> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
                return results;
            }
        }
    }

    /**
     * Checks whether a broadcast with the given displayCode exists within the most
     * recent {@code recentWindow} broadcasts. Used for collision detection during code
     * generation.
     *
     * @param displayCode  the display code to check
     * @param recentWindow number of most recent broadcasts to search within
     * @return true if a matching broadcast exists in the recent window
     * @throws SQLException if a database operation fails
     */
    public boolean existsInRecentByDisplayCode(String displayCode, int recentWindow) throws SQLException {
        String sql = """
                SELECT 1 FROM likes_broadcasts
                WHERE display_code = ?
                AND broadcast_id IN (
                    SELECT broadcast_id FROM likes_broadcasts ORDER BY created_at DESC LIMIT ?
                )
                LIMIT 1
                """;
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, displayCode);
            ps.setInt(2, recentWindow);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Maps the current row of a ResultSet to a LikesBroadcast.
     *
     * @param rs the ResultSet to map from
     * @return the mapped LikesBroadcast
     * @throws SQLException if reading the ResultSet fails
     */
    private LikesBroadcast mapRow(ResultSet rs) throws SQLException {
        return new LikesBroadcast(
                rs.getString("broadcast_id"),
                rs.getString("display_code"),
                rs.getLong("created_at"),
                rs.getString("source_type"),
                UUID.fromString(rs.getString("source_sender_uuid")),
                UUID.fromString(rs.getString("target_uuid")),
                rs.getString("reason_code"),
                rs.getString("reason_text"));
    }
}
