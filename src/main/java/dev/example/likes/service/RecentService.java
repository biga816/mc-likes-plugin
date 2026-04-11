package dev.example.likes.service;

import dev.example.likes.database.BroadcastRepository;
import dev.example.likes.model.LikesBroadcast;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service that manages an in-memory buffer of recent broadcasts
 * and tracks each player's last seen broadcast ID.
 */
public class RecentService {
    private static final Logger log = Logger.getLogger(RecentService.class.getName());

    private final int bufferSize;
    // Stored in ascending order; getRecent() reverses to return newest first
    private final ArrayDeque<LikesBroadcast> recentBuffer;
    // The broadcast ID last seen (or eligible for reaction) per player
    private final Map<UUID, String> lastSeenBroadcastId = new ConcurrentHashMap<>();

    /**
     * Constructs a RecentService.
     *
     * @param config plugin configuration; reads buffer size from
     *               {@code recent.bufferSize}.
     *               Defaults to 100 entries.
     */
    public RecentService(FileConfiguration config) {
        this.bufferSize = config.getInt("recent.bufferSize", 100);
        this.recentBuffer = new ArrayDeque<>(bufferSize);
        log.fine("RecentService initialized: bufferSize=" + bufferSize);
    }

    /**
     * Loads recent broadcasts from the database into the buffer on startup.
     * Data returned by the DB (created_at DESC) is reversed to ascending order
     * before storing.
     *
     * @param repo the broadcast repository
     * @throws SQLException if a database operation fails
     */
    public void loadFromDb(BroadcastRepository repo) throws SQLException {
        List<LikesBroadcast> recent = repo.findRecent(bufferSize);
        // findRecent returns DESC order; reverse to ascending for the buffer
        List<LikesBroadcast> ascending = new ArrayList<>(recent);
        Collections.reverse(ascending);
        synchronized (this) {
            recentBuffer.clear();
            for (LikesBroadcast broadcast : ascending) {
                recentBuffer.addLast(broadcast);
            }
        }
        log.fine("RecentService loaded " + ascending.size() + " broadcasts from DB");
    }

    /**
     * Adds a new broadcast to the buffer.
     * If the buffer is full, the oldest entry is evicted.
     *
     * @param broadcast the broadcast to add
     */
    public synchronized void add(LikesBroadcast broadcast) {
        if (recentBuffer.size() >= bufferSize) {
            recentBuffer.pollFirst();
        }
        recentBuffer.addLast(broadcast);
    }

    /**
     * Returns up to N recent broadcasts in created_at ASC order (oldest first).
     *
     * @param limit maximum number of entries to return
     * @return list of broadcasts, oldest first
     */
    public synchronized List<LikesBroadcast> getRecent(int limit) {
        List<LikesBroadcast> result = new ArrayList<>(limit);
        Iterator<LikesBroadcast> it = recentBuffer.descendingIterator();
        for (int i = 0; i < limit && it.hasNext(); i++) {
            result.add(it.next());
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * Updates the last seen broadcast ID for the given player.
     *
     * @param playerUuid  the player's UUID
     * @param broadcastId the broadcast ID last seen by the player
     */
    public void updateLastSeen(UUID playerUuid, String broadcastId) {
        lastSeenBroadcastId.put(playerUuid, broadcastId);
    }

    /**
     * Returns the last seen broadcast ID for the given player.
     *
     * @param playerUuid the player's UUID
     * @return an Optional containing the last seen broadcast ID, or empty if not
     *         set
     */
    public Optional<String> getLastSeenBroadcastId(UUID playerUuid) {
        return Optional.ofNullable(lastSeenBroadcastId.get(playerUuid));
    }

    /**
     * Returns the broadcast with the given ID from the in-memory buffer.
     *
     * @param broadcastId the broadcast ID to look up
     * @return an Optional containing the matching broadcast, or empty if not found
     */
    public synchronized Optional<LikesBroadcast> findById(String broadcastId) {
        return recentBuffer.stream()
                .filter(b -> b.broadcastId().equals(broadcastId))
                .findFirst();
    }
}
