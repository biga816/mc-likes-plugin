package dev.example.likes.model;

import java.util.UUID;

/**
 * Data model for a like broadcast.
 * Represents a single broadcast event (a like solicitation).
 */
public record LikesBroadcast(
        String broadcastId,
        String displayCode,
        long createdAt,
        String sourceType,
        UUID sourceSenderUuid,
        UUID targetUuid,
        String reasonCode,
        String reasonText) {
}
