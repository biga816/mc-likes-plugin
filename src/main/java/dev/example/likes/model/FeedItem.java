package dev.example.likes.model;

import java.util.UUID;

public record FeedItem(
                String itemId,
                String serverId,
                String displayCode,
                long createdAt,
                String itemType,
                UUID authorUuid,
                UUID initiatorUuid,
                String bodyText,
                String world,
                Integer x,
                Integer y,
                Integer z) {
}
