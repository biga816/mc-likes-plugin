package dev.example.likes.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Repository for accessing the likes_sender_daily table.
 * Tracks the number of direct likes sent per player per day.
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
     * Returns the number of likes sent by the given player on the specified date.
     * Returns 0 if no record exists.
     *
     * @param date       the target date in "yyyy-MM-dd" format
     * @param senderUuid the sender's UUID
     * @return the like count for that day (0 if no record exists)
     * @throws SQLException if a database operation fails
     */
    public int getDailyCount(String date, UUID senderUuid) throws SQLException {
        String sql = "SELECT direct_like_count FROM likes_sender_daily WHERE date = ? AND sender_uuid = ?";
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            ps.setString(2, senderUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("direct_like_count");
                }
                return 0;
            }
        }
    }

    /**
     * Increments the like count for the given player on the specified date by 1.
     * Creates a new record (count=1) if none exists, otherwise increments the
     * existing one.
     *
     * @param date       the target date in "yyyy-MM-dd" format
     * @param senderUuid the sender's UUID
     * @throws SQLException if a database operation fails
     */
    public void increment(String date, UUID senderUuid) throws SQLException {
        String sql = """
                INSERT INTO likes_sender_daily (date, sender_uuid, direct_like_count)
                VALUES (?, ?, 1)
                ON CONFLICT(date, sender_uuid) DO UPDATE SET
                    direct_like_count = direct_like_count + 1
                """;
        Connection conn = databaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            ps.setString(2, senderUuid.toString());
            ps.executeUpdate();
        }
    }
}
