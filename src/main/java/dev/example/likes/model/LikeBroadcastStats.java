package dev.example.likes.model;

/**
 * Immutable snapshot of a broadcast's like statistics.
 *
 * @param broadcastId   the broadcast's unique identifier
 * @param reactionCount total number of likes (including the sender's initial
 *                      like)
 * @param updatedAt     last update timestamp in epoch milliseconds
 */
public record LikeBroadcastStats(
                String broadcastId,
                long reactionCount,
                long updatedAt) {
}
