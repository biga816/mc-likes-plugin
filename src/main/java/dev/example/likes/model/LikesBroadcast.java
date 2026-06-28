package dev.example.likes.model;

import java.util.UUID;

public record LikesBroadcast(
        String broadcastId,
        String serverId,
        String displayCode,
        long createdAt,
        String sourceType,
        UUID sourceSenderUuid,
        UUID targetUuid,
        String reasonCode,
        String reasonText) {
}
