package dev.example.likes.book;

import dev.example.likes.util.PlayerTranslator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Shared component-building helpers for book renderers.
 */
class BookComponents {

    private BookComponents() {}

    /**
     * Truncates {@code text} 
    to at most {@code max} characters,
     * appending {@code ".."} when truncated.
     */
    static String truncate(String text, int max) {
        if (text == null)
            return "";
        if (text.length() <= max)
            return text;
        return text.substring(0, Math.max(0, max - 2)) + "..";
    }

    /**
     * Resolves a player's display name from their UUID.
     * Falls back to the first 8 characters of the UUID string if unknown.
     */
    static String resolveName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null)
            return online.getName();
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    /**
     * Returns {@link NamedTextColor#GREEN} if {@code uuid} matches
     * {@code viewerUuid}, otherwise {@link NamedTextColor#BLACK}.
     */
    static NamedTextColor nameColor(UUID uuid, UUID viewerUuid) {
        return uuid.equals(viewerUuid) ? NamedTextColor.GREEN : NamedTextColor.BLACK;
    }

    /**
     * Builds a heart+count component that is clickable when the viewer
     * has not yet reacted and is not a participant.
     *
     * @param code           the 4-character display code (without {@code #})
     * @param count          current reaction count
     * @param alreadyReacted whether the viewer has already reacted
     * @param isViewer       whether the viewer is sender or target
     * @param tr             locale-bound translator (reserved for future tooltip use)
     * @return the styled component
     */
    static Component buildClickableHeart(String code, long count, boolean alreadyReacted,
            boolean isViewer, PlayerTranslator tr) {
        String symbol = alreadyReacted ? "♥" : "♡";
        Component heart = Component.text("[" + symbol + count + "]").color(NamedTextColor.RED);
        if (!alreadyReacted && !isViewer) {
            heart = heart
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.runCommand("/like #" + code));
        }
        return Component.text("").color(NamedTextColor.RED).append(heart);
    }

    /**
     * Builds a {@code [indent]sender→target [♡count]} line.
     *
     * @param senderName  truncated sender name
     * @param senderColor color for the sender name
     * @param targetName  truncated target name
     * @param targetColor color for the target name
     * @return the assembled component (trailing space included; no newline)
     */
    static Component buildSenderArrowTarget(
            String senderName, NamedTextColor senderColor,
            String targetName, NamedTextColor targetColor) {
        return Component.text(senderName).color(senderColor)
                .append(Component.text("→").color(NamedTextColor.RED))
                .append(Component.text(targetName + " ").color(targetColor));
    }


    private static final DateTimeFormatter REASON_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Builds a reason line that shows truncated text inline and the full
     * text with creation date on hover.
     *
     * @param fullText  the full reason text shown on hover
     * @param truncated the truncated text shown inline
     * @param indent    leading whitespace prefix (e.g. {@code "  "} or {@code "   "})
     * @param createdAt broadcast creation timestamp in epoch milliseconds
     * @return the styled component
     */
    static Component buildReasonLine(String fullText, String truncated, String indent, long createdAt) {
        String dateLabel = "[" + REASON_DATE_FORMAT.format(
                Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault())) + "]";
        return Component.text(indent + "\"" + truncated + "\"")
                .color(NamedTextColor.GRAY)
                .hoverEvent(HoverEvent.showText(
                        Component.text(dateLabel).color(NamedTextColor.DARK_GRAY)
                                .append(Component.newline())
                                .append(Component.text(fullText != null ? fullText : "").color(NamedTextColor.GRAY))));
    }
}
