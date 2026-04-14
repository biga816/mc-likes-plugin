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
     * Builds the simplest broadcast message: no reaction count, no react button.
     * <p>
     * Use
     * {@link #buildBroadcastMessage(LikesBroadcast, Component, Component, int, boolean, boolean, boolean)}
     * for full control over reaction count and button behavior.
     * </p>
     *
     * @param broadcast     the broadcast data
     * @param senderDisplay pre-built component for the sender name slot
     * @param targetDisplay pre-built component for the target name slot
     * @return the assembled {@link Component}
     */
    public Component buildBroadcastMessage(LikesBroadcast broadcast, Component senderDisplay, Component targetDisplay) {
        return buildBroadcastMessage(broadcast, senderDisplay, targetDisplay, -1, false, false, false);
    }

    /**
     * Builds a server-wide broadcast message with full control over sender/target
     * display components, react button visibility, and click interactivity.
     * <p>
     * When {@code clickable} is {@code false} and the player has not yet reacted,
     * the react button is shown as {@code ♡} without underline or click event
     * (used when the viewing player is the like target and cannot react).
     * </p>
     *
     * @param broadcast       the broadcast data
     * @param senderDisplay   pre-built component for the sender name slot
     * @param targetDisplay   pre-built component for the target name slot
     * @param reactionCount   total number of reactions, or {@code -1} to show no
     *                        count
     * @param alreadyReacted  whether the viewing player has already reacted
     * @param showReactButton whether to append the react button
     * @param clickable       whether the react button should have a click event and
     *                        underline decoration
     * @return the assembled {@link Component}
     */
    public Component buildBroadcastMessage(LikesBroadcast broadcast, Component senderDisplay, Component targetDisplay,
            int reactionCount, boolean alreadyReacted, boolean showReactButton, boolean clickable) {
        String displayCode = broadcast.displayCode();

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
        Component reactButton = Component.text("[" + heart + count + "]").color(NamedTextColor.GRAY);
        Component codeLabel = Component.text("(" + displayCode + ")").color(NamedTextColor.DARK_GRAY)
                .decorate(TextDecoration.ITALIC);

        if (alreadyReacted) {
            reactButton = reactButton.color(NamedTextColor.RED);
        } else if (clickable) {
            reactButton = reactButton
                    .color(NamedTextColor.GRAY)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.runCommand("/like boost " + displayCode))
                    .hoverEvent(HoverEvent.showText(
                            Component.translatable("likes.broadcast.react.hover")
                                    .append(Component.text("\nID: "))
                                    .append(Component.text(displayCode).color(NamedTextColor.WHITE))));
            codeLabel = codeLabel.color(NamedTextColor.WHITE);
        }

        return message.append(Component.text("  ")).append(reactButton).append(Component.text("  ")).append(codeLabel);
    }

    /**
     * Builds a usage message component by combining the usage prefix with one or
     * more usage keys, separated by " | ".
     *
     * @param usageKeys the translation keys for each usage entry
     * @return a yellow {@link Component}
     */
    public Component usageInfo(String... usageKeys) {
        Component msg = Component.translatable("likes.command.like.usage.prefix").color(NamedTextColor.YELLOW);
        for (int i = 0; i < usageKeys.length; i++) {
            msg = msg.append(Component.text(i == 0 ? " " : " | ").color(NamedTextColor.YELLOW))
                    .append(Component.translatable("likes.command.like.usage." + usageKeys[i])
                            .color(NamedTextColor.YELLOW));
        }
        return msg;
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
