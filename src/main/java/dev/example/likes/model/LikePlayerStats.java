package dev.example.likes.model;

import java.util.UUID;

/**
 * Immutable snapshot of a player's all-time like statistics.
 *
 * @param playerUuid    the player's UUID
 * @param playerName    the player's last-known display name
 * @param receivedCount number of direct likes received by this player
 * @param sentCount     number of direct likes sent by this player
 * @param reactedCount  number of times this player reacted to others'
 *                      broadcasts
 * @param updatedAt     last update timestamp in epoch milliseconds
 */
public record LikePlayerStats(
                UUID playerUuid,
                String playerName,
                long receivedCount,
                long sentCount,
                long reactedCount,
                long updatedAt) {
}
