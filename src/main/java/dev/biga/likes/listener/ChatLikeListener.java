package dev.biga.likes.listener;

import dev.biga.likes.model.PendingChat;
import dev.biga.likes.service.ChatLikeEligibilityService;
import dev.biga.likes.service.PendingChatService;
import dev.biga.likes.util.DisplayCodeGenerator;
import dev.biga.likes.util.MessageFactory;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Adds a per-viewer reaction control to eligible public chat messages. */
public class ChatLikeListener implements Listener {
    private static final Logger log = Logger.getLogger(ChatLikeListener.class.getName());
    private final PendingChatService pendingChatService;
    private final DisplayCodeGenerator displayCodeGenerator;
    private final MessageFactory messageFactory;
    private final ChatLikeEligibilityService eligibilityService;
    private final String serverId;
    private final int maxStoredLength;

    public ChatLikeListener(PendingChatService pendingChatService, DisplayCodeGenerator displayCodeGenerator,
            MessageFactory messageFactory, ChatLikeEligibilityService eligibilityService,
            String serverId, int maxStoredLength) {
        this.pendingChatService = pendingChatService;
        this.displayCodeGenerator = displayCodeGenerator;
        this.messageFactory = messageFactory;
        this.eligibilityService = eligibilityService;
        this.serverId = serverId;
        this.maxStoredLength = Math.max(1, maxStoredLength);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String plainText = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (!eligibilityService.isEligible(plainText))
            return;

        PendingChat pending;
        try {
            pending = pendingChatService.putGenerated(displayCodeGenerator, serverId,
                    displayCode -> new PendingChat(displayCode, event.getPlayer().getUniqueId(),
                            event.getPlayer().getName(), truncate(plainText, maxStoredLength),
                            null, null, null, null, System.currentTimeMillis()));
        } catch (SQLException e) {
            log.log(Level.WARNING, "Failed to allocate a display code for chat", e);
            return;
        }

        Component suffix = messageFactory.buildChatLikeSuffix(pending.displayCode());
        ChatRenderer previous = event.renderer();
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            try {
                Component rendered = previous.render(source, sourceDisplayName, message, viewer);
                return viewer.equals(source) ? rendered : rendered.append(suffix);
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "Existing chat renderer failed; omitting chat like control", e);
                try {
                    return ChatRenderer.defaultRenderer().render(source, sourceDisplayName, message, viewer);
                } catch (RuntimeException fallbackFailure) {
                    return message;
                }
            }
        });
    }

    private static String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text
                : text.substring(0, Math.max(0, maxLength - 1)) + "…";
    }
}
