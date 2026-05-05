package dev.example.likes.model;

import java.util.UUID;

/**
 * DTO for a ranking result that joins broadcast details with its reaction
 * count.
 * Used for the future {@code /like ranking} command.
 *
 * @param broadcastId      the broadcast's unique identifier
 * @param displayCode      the 4-character display code (e.g. {@code "A7K2"})
 * @param createdAt        broadcast creation timestamp in epoch milliseconds
 * @param sourceSenderUuid UUID of the player who sent the original like
 * @param targetUuid       UUID of the player who received the like
 * @param reasonText       the reason text provided by the sender
 * @param reactionCount    total number of reactions including the initial like
 */
public record BroadcastRankingEntry(
                String broadcastId,
                String displayCode,
                long createdAt,
                UUID sourceSenderUuid,
                UUID targetUuid,
                String reasonText,
                long reactionCount) {
}
