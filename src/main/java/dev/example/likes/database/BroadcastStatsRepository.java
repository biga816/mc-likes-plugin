package dev.example.likes.database;

import dev.example.likes.model.BroadcastRankingEntry;
import dev.example.likes.model.LikeBroadcastStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository for the {@code like_broadcast_stats} aggregation table.
 * <p>
 * Write methods accept an explicit {@link Connection} to participate in a
 * caller-managed transaction. Read methods use the shared connection from
 * {@link DatabaseManager#getConnection()}.
 * </p>
 */
public class BroadcastStatsRepository {

    private final DatabaseManager databaseManager;

    /**
     * Constructs a BroadcastStatsRepository.
     *
     * @param databaseManager the database connection manager
     */
    public BroadcastStatsRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    // ── Write methods (transactional, called from DatabaseWriteExecutor) ────────

    /**
     * Inserts a new stats row with {@code reaction_count = 1}.
     * Called once when a direct like broadcast is created; the sender's own
     * initial like is counted as the first reaction.
     *
     * @param conn        the connection in the active transaction
     * @param broadcastId the new broadcast's ID
     * @param updatedAt   current timestamp in epoch milliseconds
     * @throws SQLException if a database error occurs
     */
    public void insertNew(Connection conn, String broadcastId, long updatedAt) throws SQLException {
        String sql = """
                INSERT INTO like_broadcast_stats (broadcast_id, reaction_count, updated_at)
                VALUES (?, 1, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, broadcastId);
            ps.setLong(2, updatedAt);
            ps.executeUpdate();
        }
    }

    /**
     * Increments {@code reaction_count} for an existing broadcast, or inserts a
     * new row with count 1 if none exists (upsert).
     * Called when a player reacts to an existing broadcast.
     *
     * @param conn        the connection in the active transaction
     * @param broadcastId the broadcast ID to update
     * @param updatedAt   current timestamp in epoch milliseconds
     * @throws SQLException if a database error occurs
     */
    public void incrementReactionCount(Connection conn, String broadcastId, long updatedAt)
            throws SQLException {
        String sql = """
                INSERT INTO like_broadcast_stats (broadcast_id, reaction_count, updated_at)
                VALUES (?, 1, ?)
                ON CONFLICT(broadcast_id) DO UPDATE SET
                    reaction_count = reaction_count + 1,
                    updated_at     = excluded.updated_at
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, broadcastId);
            ps.setLong(2, updatedAt);
            ps.executeUpdate();
        }
    }

    // ── Read methods (for /like ranking) ─────────────────────────────────────

    /**
     * Returns the top {@code limit} broadcasts by {@code reaction_count}, joined
     * with their broadcast details for display purposes.
     *
     * @param limit maximum number of results
     * @return list of ranking entries ordered by reaction_count DESC
     * @throws SQLException if a database error occurs
     */
    public List<BroadcastRankingEntry> getTopBroadcasts(int limit) throws SQLException {
        String sql = """
                SELECT b.broadcast_id, b.display_code, b.created_at,
                       b.source_sender_uuid, b.target_uuid, b.reason_text,
                       s.reaction_count
                FROM like_broadcast_stats s
                JOIN likes_broadcasts b ON b.broadcast_id = s.broadcast_id
                ORDER BY s.reaction_count DESC
                LIMIT ?
                """;
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<BroadcastRankingEntry> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(new BroadcastRankingEntry(
                            rs.getString("broadcast_id"),
                            rs.getString("display_code"),
                            rs.getLong("created_at"),
                            UUID.fromString(rs.getString("source_sender_uuid")),
                            UUID.fromString(rs.getString("target_uuid")),
                            rs.getString("reason_text"),
                            rs.getLong("reaction_count")));
                }
                return results;
            }
        }
    }

    /**
     * Returns the reaction count for each of the given broadcast IDs in a single
     * query, reading from the pre-computed {@code like_broadcast_stats} table.
     * Broadcast IDs with no stats row are not included in the returned map.
     * Used by {@code /like list} to avoid per-row COUNT queries on likes_events.
     *
     * @param broadcastIds the list of broadcast IDs to look up
     * @return a map of broadcastId → reaction_count
     * @throws SQLException if a database error occurs
     */
    public Map<String, Long> reactionCountByBroadcastIds(List<String> broadcastIds) throws SQLException {
        if (broadcastIds.isEmpty()) return Map.of();
        String placeholders = broadcastIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = "SELECT broadcast_id, reaction_count FROM like_broadcast_stats WHERE broadcast_id IN ("
                + placeholders + ")";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < broadcastIds.size(); i++) {
                ps.setString(i + 1, broadcastIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Long> result = new HashMap<>();
                while (rs.next()) {
                    result.put(rs.getString("broadcast_id"), rs.getLong("reaction_count"));
                }
                return result;
            }
        }
    }

    /**
     * Returns the stats for a single broadcast, or empty if no record exists.
     *
     * @param broadcastId the broadcast ID
     * @return an Optional containing the stats, or empty
     * @throws SQLException if a database error occurs
     */
    public Optional<LikeBroadcastStats> getStats(String broadcastId) throws SQLException {
        String sql = "SELECT * FROM like_broadcast_stats WHERE broadcast_id = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, broadcastId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new LikeBroadcastStats(
                            rs.getString("broadcast_id"),
                            rs.getLong("reaction_count"),
                            rs.getLong("updated_at")));
                }
                return Optional.empty();
            }
        }
    }
}
