package dev.example.likes.model;

import java.util.UUID;

public record LikesEvent(
        String eventId,
        String serverId,
        long createdAt,
        String broadcastId,
        UUID senderUuid,
        UUID targetUuid) {
}
