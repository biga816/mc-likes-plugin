package dev.example.likes.book;

import dev.example.likes.model.BroadcastRankingEntry;
import dev.example.likes.model.LikePlayerStats;
import dev.example.likes.util.PlayerTranslator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Builds the 3-page book for {@code /like ranking}.
 *
 * <ul>
 * <li>Page 1: Top Received — players ranked by received count</li>
 * <li>Page 2: Top Givers — players ranked by sent count</li>
 * <li>Page 3: Popular Likes — broadcasts ranked by reaction count</li>
 * </ul>
 */
public class LikeRankingBookRenderer {

        /** Max player name characters shown on rank list lines. */
        private static final int MAX_NAME_LEN = 10;

        /** Max reason characters shown on popular likes entries. */
        private static final int MAX_REASON_LEN = 17;

        /** Max entries shown on the Popular Likes page (2-line format). */
        private static final int MAX_POPULAR_ENTRIES = 5;

        /**
         * Builds all pages for the ranking book.
         *
         * @param received   players ranked by received count
         * @param sent       players ranked by sent count
         * @param popular    broadcasts ranked by reaction count
         * @param translator locale-bound translator for the viewing player
         * @return list of page components (3 pages)
         */
        public List<Component> buildPages(
                        List<LikePlayerStats> received,
                        List<LikePlayerStats> sent,
                        List<BroadcastRankingEntry> popular,
                        UUID viewerUuid,
                        Set<String> reactedBroadcastIds,
                        PlayerTranslator translator) {
                List<Component> pages = new ArrayList<>();
                pages.add(buildReceivedPage(received, translator));
                pages.add(buildSentPage(sent, translator));
                pages.add(buildPopularPage(popular, viewerUuid, reactedBroadcastIds, translator));
                return pages;
        }

        // ── Page builders ─────────────────────────────────────────────────────────

        private Component buildReceivedPage(List<LikePlayerStats> list, PlayerTranslator tr) {
                TextComponent.Builder b = Component.text();
                b.append(Component.text(tr.translate("likes.book.ranking.title"))
                                .color(NamedTextColor.BLACK)
                                .decorate(TextDecoration.BOLD));
                b.append(Component.newline());
                b.append(Component.newline());
                b.append(Component.text(tr.translate("likes.book.ranking.received"))
                                .color(NamedTextColor.DARK_GRAY)
                                .decorate(TextDecoration.BOLD));

                if (list.isEmpty()) {
                        b.append(Component.newline());
                        b.append(Component.newline());
                        b.append(Component.text(tr.translate("likes.book.ranking.empty"))
                                        .color(NamedTextColor.GRAY));
                } else {
                        for (int i = 0; i < list.size(); i++) {
                                LikePlayerStats s = list.get(i);
                                b.append(Component.newline());
                                b.append(Component.text((i + 1) + ". ")
                                                .color(NamedTextColor.DARK_GRAY));
                                b.append(Component.text(truncate(s.playerName(), MAX_NAME_LEN))
                                                .color(NamedTextColor.BLACK));
                                b.append(Component.text("  ♥" + s.receivedCount())
                                                .color(NamedTextColor.RED));
                        }
                }
                return b.build();
        }

        private Component buildSentPage(List<LikePlayerStats> list, PlayerTranslator tr) {
                TextComponent.Builder b = Component.text();
                b.append(Component.text(tr.translate("likes.book.ranking.sent"))
                                .color(NamedTextColor.DARK_GRAY)
                                .decorate(TextDecoration.BOLD));

                if (list.isEmpty()) {
                        b.append(Component.newline());
                        b.append(Component.newline());
                        b.append(Component.text(tr.translate("likes.book.ranking.empty"))
                                        .color(NamedTextColor.GRAY));
                } else {
                        for (int i = 0; i < list.size(); i++) {
                                LikePlayerStats s = list.get(i);
                                b.append(Component.newline());
                                b.append(Component.text((i + 1) + ". ")
                                                .color(NamedTextColor.DARK_GRAY));
                                b.append(Component.text(truncate(s.playerName(), MAX_NAME_LEN))
                                                .color(NamedTextColor.BLACK));
                                b.append(Component.text("  ♥" + s.sentCount())
                                                .color(NamedTextColor.RED));
                        }
                }
                return b.build();
        }

        private Component buildPopularPage(List<BroadcastRankingEntry> list, UUID viewerUuid,
                        Set<String> reactedBroadcastIds, PlayerTranslator tr) {
                TextComponent.Builder b = Component.text();
                b.append(Component.text(tr.translate("likes.book.ranking.popular"))
                                .color(NamedTextColor.DARK_GRAY)
                                .decorate(TextDecoration.BOLD));

                if (list.isEmpty()) {
                        b.append(Component.newline());
                        b.append(Component.newline());
                        b.append(Component.text(tr.translate("likes.book.ranking.empty"))
                                        .color(NamedTextColor.GRAY));
                } else {
                        int limit = Math.min(list.size(), MAX_POPULAR_ENTRIES);
                        for (int i = 0; i < limit; i++) {
                                BroadcastRankingEntry entry = list.get(i);
                                String senderName = truncate(resolveName(entry.sourceSenderUuid()), 7);
                                String targetName = truncate(resolveName(entry.targetUuid()), 7);
                                String reason = truncate(entry.reasonText(), MAX_REASON_LEN);
                                String code = entry.displayCode();
                                boolean alreadyReacted = reactedBroadcastIds.contains(entry.broadcastId());
                                boolean isViewer = entry.sourceSenderUuid().equals(viewerUuid)
                                                || entry.targetUuid().equals(viewerUuid);

                                NamedTextColor senderColor = entry.sourceSenderUuid().equals(viewerUuid)
                                                ? NamedTextColor.GREEN
                                                : NamedTextColor.BLACK;
                                NamedTextColor targetColor = entry.targetUuid().equals(viewerUuid)
                                                ? NamedTextColor.GREEN
                                                : NamedTextColor.BLACK;

                                b.append(Component.newline());
                                b.append(Component.text((i + 1) + ". ")
                                                .color(NamedTextColor.DARK_GRAY));
                                b.append(Component.text(senderName)
                                                .color(senderColor));
                                b.append(Component.text("→")
                                                .color(NamedTextColor.RED));
                                b.append(Component.text(targetName + " ")
                                                .color(targetColor));
                                b.append(buildClickableHeart(code, entry.reactionCount(), alreadyReacted, isViewer,
                                                tr));
                                b.append(Component.newline());
                                b.append(Component.text("   \"" + reason + "\"")
                                                .color(NamedTextColor.GRAY)
                                                .hoverEvent(HoverEvent.showText(
                                                                Component.text(entry.reasonText() != null
                                                                                ? entry.reasonText()
                                                                                : "")
                                                                                .color(NamedTextColor.GRAY))));
                        }
                }
                return b.build();
        }

        // ── Shared helpers ────────────────────────────────────────────────────────

        /**
         * Builds a clickable heart+count component.
         * Clicking runs {@code /like #<code>}; hovering shows a translated tooltip.
         *
         * @param code  the 4-character display code (without {@code #})
         * @param count current reaction count
         * @param tr    locale-bound translator for the hover tooltip
         * @return the styled, clickable component
         */
        static Component buildClickableHeart(String code, long count, boolean alreadyReacted, boolean isViewer,
                        PlayerTranslator tr) {
                String symbol = alreadyReacted ? "♥" : "♡";
                Component heart = Component.text("[" + symbol + count + "]").color(NamedTextColor.RED);
                if (!alreadyReacted && !isViewer) {
                        heart = heart
                                        .decorate(TextDecoration.UNDERLINED)
                                        .clickEvent(ClickEvent.runCommand("/like #" + code));
                }
                return Component.text("").color(NamedTextColor.RED).append(heart);
        }

        private static String truncate(String text, int max) {
                if (text == null)
                        return "";
                if (text.length() <= max)
                        return text;
                return text.substring(0, Math.max(0, max - 2)) + "..";
        }

        private static String resolveName(UUID uuid) {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null)
                        return online.getName();
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                return name != null ? name : uuid.toString().substring(0, 8);
        }
}
