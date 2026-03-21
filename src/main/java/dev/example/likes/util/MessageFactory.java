package dev.example.likes.util;

import dev.example.likes.model.LikesBroadcast;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Factory for building chat message components using the Adventure API.
 * <p>
 * Reads configuration values such as the prefix from {@link FileConfiguration}
 * and assembles broadcast, notification, and general-purpose messages.
 * </p>
 */
public class MessageFactory {

    private final String prefix;

    /**
     * Constructs a MessageFactory.
     *
     * @param config plugin configuration; reads the prefix from {@code broadcast.prefix}.
     *               Defaults to {@code "[LIKE]"} if the key is absent.
     */
    public MessageFactory(FileConfiguration config) {
        this.prefix = config.getString("broadcast.prefix", "[LIKE]");
    }

    /**
     * Builds a server-wide broadcast message.
     * <p>
     * Format: {@code [LIKE] senderName → targetName  "reasonText"  [👍 React]}
     * </p>
     * <p>
     * The {@code [👍 React]} button is given a clickEvent that runs
     * {@code /likeboost <shortId>} and a hoverEvent showing the short ID.
     * </p>
     *
     * @param broadcast  the broadcast data
     * @param senderName the sender's display name
     * @param targetName the target player's display name
     * @return the assembled {@link Component}
     */
    public Component buildBroadcastMessage(LikesBroadcast broadcast, String senderName, String targetName) {
        String shortId = broadcast.shortId();

        Component reactButton = Component.text("[👍 React]")
            .color(NamedTextColor.GREEN)
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/likeboost " + shortId))
            .hoverEvent(HoverEvent.showText(
                Component.text("Click to react\nID: ")
                    .append(Component.text(shortId).color(NamedTextColor.YELLOW))
            ));

        return Component.text(prefix)
            .color(NamedTextColor.AQUA)
            .append(Component.text(" "))
            .append(Component.text(senderName).color(NamedTextColor.WHITE))
            .append(Component.text(" → ").color(NamedTextColor.GRAY))
            .append(Component.text(targetName).color(NamedTextColor.WHITE))
            .append(Component.text("  \"" + broadcast.reasonText() + "\"").color(NamedTextColor.GRAY))
            .append(Component.text("  "))
            .append(reactButton);
    }

    /**
     * Builds a personal notification message for the target player.
     * <p>
     * Format: {@code [LIKE] senderName liked you: "reasonText"}
     * </p>
     *
     * @param broadcast  the broadcast data
     * @param senderName the sender's display name
     * @return the assembled {@link Component}
     */
    public Component buildTargetNotification(LikesBroadcast broadcast, String senderName) {
        return Component.text(prefix)
            .color(NamedTextColor.AQUA)
            .append(Component.text(" "))
            .append(Component.text(senderName).color(NamedTextColor.WHITE))
            .append(Component.text(" があなたにいいねしました: ").color(NamedTextColor.GRAY))
            .append(Component.text("\"" + broadcast.reasonText() + "\"").color(NamedTextColor.GRAY));
    }

    /**
     * Builds an error message component.
     *
     * @param message the error message text
     * @return a red {@link Component}
     */
    public Component error(String message) {
        return Component.text(message).color(NamedTextColor.RED);
    }

    /**
     * Builds an informational message component.
     *
     * @param message the informational message text
     * @return a yellow {@link Component}
     */
    public Component info(String message) {
        return Component.text(message).color(NamedTextColor.YELLOW);
    }

    /**
     * Builds a success message component.
     *
     * @param message the success message text
     * @return a green {@link Component}
     */
    public Component success(String message) {
        return Component.text(message).color(NamedTextColor.GREEN);
    }
}
