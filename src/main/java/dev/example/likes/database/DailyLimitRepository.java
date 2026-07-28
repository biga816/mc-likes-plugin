package dev.example.likes.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Repository for accessing the sender_daily table.
 * Tracks the number of direct likes sent per player per server per day.
 */
public class DailyLimitRepository {

    private final DatabaseManager databaseManager;

    /**
     * Constructs a DailyLimitRepository.
     *
     * @param databaseManager the database connection manager
     */
    public DailyLimitRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Returns the number of likes sent by the given player on the specified date
     * for the given server. Returns 0 if no record exists.
     *
     * @param serverId   the server ID to scope the lookup
     * @param date       the target date in "yyyy-MM-dd" format
     * @param senderUuid the sender's UUID
     * @return the like count for that day (0 if no record exists)
     * @throws SQLException if a database operation fails
     */
    public int getDailyCount(String serverId, String date, UUID senderUuid) throws SQLException {
        String sql = """
                SELECT count FROM sender_daily
                WHERE date = ? AND server_id = ? AND player_uuid = ? AND action_type = 'DIRECT'
                """;
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            ps.setString(2, serverId);
            ps.setString(3, senderUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
                return 0;
            }
        }
    }

    /**
     * Increments the like count for the given player on the specified date and
     * server by 1. Creates a new record (count=1) if none exists.
     *
     * @param serverId   the server ID to scope the increment
     * @param date       the target date in "yyyy-MM-dd" format
     * @param senderUuid the sender's UUID
     * @throws SQLException if a database operation fails
     */
    public void increment(String serverId, String date, UUID senderUuid) throws SQLException {
        String sql = """
                INSERT INTO sender_daily (date, server_id, player_uuid, action_type, count)
                VALUES (?, ?, ?, 'DIRECT', 1)
                ON CONFLICT(date, server_id, player_uuid, action_type) DO UPDATE SET
                    count = count + 1
                """;
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            ps.setString(2, serverId);
            ps.setString(3, senderUuid.toString());
            ps.executeUpdate();
        }
    }
}
