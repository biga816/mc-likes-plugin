package dev.biga.likebeacon.model;

import java.util.UUID;

/**
 * Immutable snapshot of a player's all-time like statistics.
 *
 * @param playerUuid    the player's UUID
 * @param playerName    the player's last-known display name
 * @param receivedCount reactions on items authored by this player (DIRECT and
 *                      CHAT)
 * @param sentCount     DIRECT items initiated by this player
 * @param reactedCount  reactions made by this player, including the initial
 *                      reaction created with a DIRECT item
 * @param updatedAt     last update timestamp in epoch milliseconds
 */
public record PlayerStats(
    UUID playerUuid,
    String playerName,
    long receivedCount,
    long sentCount,
    long reactedCount,
    long updatedAt) {
}
