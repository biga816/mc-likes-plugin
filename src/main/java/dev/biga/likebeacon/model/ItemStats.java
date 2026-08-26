package dev.biga.likebeacon.model;

/**
 * Immutable snapshot of a item's like statistics.
 *
 * @param itemId        the item's unique identifier
 * @param reactionCount total number of likes (including the sender's initial
 *                      like)
 * @param updatedAt     last update timestamp in epoch milliseconds
 */
public record ItemStats(
    String itemId,
    long reactionCount,
    long updatedAt) {
}
