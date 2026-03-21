package dev.example.likes.model;

import java.util.UUID;

/**
 * Data model for a like event.
 * Represents a single like sent by a player in response to a broadcast.
 */
public record LikesEvent(
    String eventId,
    long createdAt,
    String broadcastId,
    UUID senderUuid,
    UUID targetUuid
) {}
