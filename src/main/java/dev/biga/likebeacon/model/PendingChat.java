package dev.biga.likebeacon.model;

import java.util.UUID;

/** Immutable chat message waiting for its first reaction. */
public record PendingChat(
                String displayCode,
                UUID authorUuid,
                String authorName,
                String bodyText,
                String world,
                Integer x,
                Integer y,
                Integer z,
                long createdAt) {
}
