package dev.example.likes.util;

import dev.example.likes.model.LikesBroadcast;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Factory for building chat message components using the Adventure API.
 * <p>
 * User-facing text is expressed as {@link Component#translatable(String)} keys,
 * which Adventure / Paper resolves to each player's client locale at send time
 * via {@link net.kyori.adventure.translation.GlobalTranslator}.
 * </p>
 */
public class MessageFactory {

    private final String prefix;

    /**
     * Constructs a MessageFactory.
     *
     * @param config plugin configuration; reads the prefix from
     *               {@code broadcast.prefix}.
     *               Defaults to {@code "[LIKE]"} if the key is absent.
     */
    public MessageFactory(FileConfiguration config) {
        this.prefix = config.getString("broadcast.prefix", "[LIKE]");
    }

    /**
     * Builds a server-wide broadcast message for a newly created like.
     * <p>
     * Format: {@code [LIKE] senderName → targetName  "reasonText"  [♡]}
     * </p>
     *
     * @param broadcast  the broadcast data
     * @param senderName the sender's display name
     * @param targetName the target player's display name
     * @return the assembled {@link Component}
     */
    public Component buildBroadcastMessage(LikesBroadcast broadcast, String senderName, String targetName) {
        return buildBroadcastMessage(broadcast, senderName, targetName, -1);
    }

    /**
     * Builds a server-wide broadcast message with a reaction count.
     * <p>
     * Format: {@code [LIKE] senderName → targetName  "reasonText"  [♡N]}
     * Pass {@code -1} for {@code reactionCount} to omit the count.
     * </p>
     *
     * @param broadcast     the broadcast data
     * @param senderName    the sender's display name
     * @param targetName    the target player's display name
     * @param reactionCount total number of reactions, or {@code -1} to show no
     *                      count
     * @return the assembled {@link Component}
     */
    public Component buildBroadcastMessage(LikesBroadcast broadcast, String senderName, String targetName,
            int reactionCount) {
        return buildBroadcastMessage(broadcast, senderName, targetName, reactionCount, false);
    }

    /**
     * Builds a server-wide broadcast message with a reaction count and
     * already-reacted state.
     * <p>
     * When {@code alreadyReacted} is {@code true} the react button shows
     * {@code ♥} instead of {@code ♡} and the {@code UNDERLINED} decoration is
     * omitted.
     * </p>
     *
     * @param broadcast      the broadcast data
     * @param senderName     the sender's display name
     * @param targetName     the target player's display name
     * @param reactionCount  total number of reactions, or {@code -1} to show no
     *                       count
     * @param alreadyReacted whether the viewing player has already reacted
     * @return the assembled {@link Component}
     */
    public Component buildBroadcastMessage(LikesBroadcast broadcast, String senderName, String targetName,
            int reactionCount, boolean alreadyReacted) {
        String shortId = broadcast.shortId();

        String heart = alreadyReacted ? "♥" : "♡";
        String count = reactionCount < 0 ? "" : String.valueOf(reactionCount);
        Component reactLabel = Component.text("[" + heart + count + "]");

        Component reactButton = reactLabel
                .color(NamedTextColor.RED);

        if (!alreadyReacted) {
            reactButton = reactButton
                    .color(NamedTextColor.GRAY)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.runCommand("/like boost " + shortId))
                    .hoverEvent(HoverEvent.showText(
                            Component.translatable("likes.broadcast.react.hover")
                                    .append(Component.text("\nID: "))
                                    .append(Component.text(shortId).color(NamedTextColor.YELLOW))));
        }

        return Component.text(prefix)
                .color(NamedTextColor.AQUA)
                .append(Component.text(" "))
                .append(Component.text(senderName).color(NamedTextColor.WHITE))
                .append(Component.text(" -♥→ ").color(NamedTextColor.RED))
                .append(Component.text(targetName).color(NamedTextColor.WHITE))
                .append(Component.text("  \"" + broadcast.reasonText() + "\"").color(NamedTextColor.GRAY))
                .append(Component.text("  "))
                .append(reactButton);
    }

    /**
     * Builds a personal notification message for the target player.
     * <p>
     * Uses the {@code likes.notification.body} translation key with the sender's
     * name
     * and reason as arguments, allowing per-player locale rendering.
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
                .append(Component.translatable(
                        "likes.notification.body",
                        Component.text(senderName).color(NamedTextColor.WHITE),
                        Component.text("\"" + broadcast.reasonText() + "\"").color(NamedTextColor.GRAY))
                        .color(NamedTextColor.GRAY));
    }

    /**
     * Builds an error message component from a translation key.
     *
     * @param key  the translation key (e.g. {@code "likes.error.self"})
     * @param args optional translation arguments ({@code {0}}, {@code {1}}, …)
     * @return a red {@link Component}
     */
    public Component error(String key, ComponentLike... args) {
        return Component.translatable(key, args).color(NamedTextColor.RED);
    }

    /**
     * Builds an informational message component from a translation key.
     *
     * @param key  the translation key
     * @param args optional translation arguments
     * @return a yellow {@link Component}
     */
    public Component info(String key, ComponentLike... args) {
        return Component.translatable(key, args).color(NamedTextColor.YELLOW);
    }

    /**
     * Builds a success message component from a translation key.
     *
     * @param key  the translation key
     * @param args optional translation arguments
     * @return a green {@link Component}
     */
    public Component success(String key, ComponentLike... args) {
        return Component.translatable(key, args).color(NamedTextColor.GREEN);
    }
}
