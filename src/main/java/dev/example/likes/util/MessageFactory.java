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
        return buildBroadcastMessage(broadcast,
                Component.text(senderName).color(NamedTextColor.WHITE),
                Component.text(targetName).color(NamedTextColor.WHITE),
                reactionCount, alreadyReacted, true);
    }

    /**
     * Builds a server-wide broadcast message with full control over sender/target
     * display components and react button visibility.
     * <p>
     * Pass a {@link Component#translatable(String)} as {@code senderDisplay} or
     * {@code targetDisplay} to show a localized label (e.g. "あなた / you").
     * Set {@code showReactButton} to {@code false} to suppress the button
     * (used when sending to the target player themselves).
     * </p>
     *
     * @param broadcast       the broadcast data
     * @param senderDisplay   pre-built component for the sender name slot
     * @param targetDisplay   pre-built component for the target name slot
     * @param reactionCount   total number of reactions, or {@code -1} to show no
     *                        count
     * @param alreadyReacted  whether the viewing player has already reacted
     * @param showReactButton whether to append the react button
     * @return the assembled {@link Component}
     */
    public Component buildBroadcastMessage(LikesBroadcast broadcast, Component senderDisplay, Component targetDisplay,
            int reactionCount, boolean alreadyReacted, boolean showReactButton) {
        String shortId = broadcast.shortId();

        Component message = Component.text(prefix)
                .color(NamedTextColor.AQUA)
                .append(Component.text(" "))
                .append(senderDisplay)
                .append(Component.text(" -♥→ ").color(NamedTextColor.RED))
                .append(targetDisplay)
                .append(Component.text("  \"" + broadcast.reasonText() + "\"").color(NamedTextColor.GRAY));

        if (!showReactButton) {
            return message;
        }

        String heart = alreadyReacted ? "♥" : "♡";
        String count = reactionCount < 0 ? "" : String.valueOf(reactionCount);
        Component reactButton = Component.text("[" + heart + count + "]");

        if (!alreadyReacted) {
            reactButton = reactButton
                    .color(NamedTextColor.GRAY)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.runCommand("/like boost " + shortId))
                    .hoverEvent(HoverEvent.showText(
                            Component.translatable("likes.broadcast.react.hover")
                                    .append(Component.text("\nID: "))
                                    .append(Component.text(shortId).color(NamedTextColor.YELLOW))));
        } else {
            reactButton = reactButton.color(NamedTextColor.RED);
        }

        return message.append(Component.text("  ")).append(reactButton);
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
