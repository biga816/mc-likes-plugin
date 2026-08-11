package dev.biga.likes.model;

import java.util.UUID;

public record Reaction(
                String reactionId,
                String serverId,
                long createdAt,
                String itemId,
                UUID reactorUuid,
                UUID authorUuid,
                String reactionType) {
}
