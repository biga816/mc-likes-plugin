package dev.example.likes.book;

import dev.example.likes.model.BroadcastRankingEntry;
import dev.example.likes.model.LikePlayerStats;
import dev.example.likes.util.PlayerTranslator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToLongFunction;

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
        private static final int MAX_POPULAR_ENTRIES = 6;

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
                b.append(Component.text("⏷" + tr.translate("likes.book.ranking.received"))
                                .color(NamedTextColor.DARK_GRAY)
                                .decorate(TextDecoration.BOLD));
                appendPlayerStatsList(b, list, LikePlayerStats::receivedCount, tr);
                return b.build();
        }

        private Component buildSentPage(List<LikePlayerStats> list, PlayerTranslator tr) {
                TextComponent.Builder b = Component.text();
                b.append(Component.text("⏷" + tr.translate("likes.book.ranking.sent"))
                                .color(NamedTextColor.DARK_GRAY)
                                .decorate(TextDecoration.BOLD));
                appendPlayerStatsList(b, list, LikePlayerStats::sentCount, tr);
                return b.build();
        }

        private Component buildPopularPage(List<BroadcastRankingEntry> list, UUID viewerUuid,
                        Set<String> reactedBroadcastIds, PlayerTranslator tr) {
                TextComponent.Builder b = Component.text();
                b.append(Component.text("⏷" + tr.translate("likes.book.ranking.popular"))
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
                                String senderName = BookComponents
                                                .truncate(BookComponents.resolveName(entry.sourceSenderUuid()), 7);
                                String targetName = BookComponents
                                                .truncate(BookComponents.resolveName(entry.targetUuid()), 7);
                                String reason = BookComponents.truncate(entry.reasonText(), MAX_REASON_LEN);
                                String code = entry.displayCode();
                                boolean alreadyReacted = reactedBroadcastIds.contains(entry.broadcastId());
                                boolean isViewer = entry.sourceSenderUuid().equals(viewerUuid)
                                                || entry.targetUuid().equals(viewerUuid);

                                b.append(Component.newline());
                                b.append(Component.text((i + 1) + ". ")
                                                .color(NamedTextColor.DARK_GRAY));
                                b.append(BookComponents.buildSenderArrowTarget(
                                                senderName,
                                                BookComponents.nameColor(entry.sourceSenderUuid(), viewerUuid),
                                                targetName, BookComponents.nameColor(entry.targetUuid(), viewerUuid)));
                                b.append(BookComponents.buildClickableHeart(code, entry.reactionCount(), alreadyReacted,
                                                isViewer, tr));
                                b.append(Component.newline());
                                b.append(BookComponents.buildReasonLine(entry.reasonText(), reason, "   ",
                                                entry.createdAt()));
                        }
                }
                return b.build();
        }

        // ── Shared helpers ────────────────────────────────────────────────────────

        private void appendPlayerStatsList(TextComponent.Builder b, List<LikePlayerStats> list,
                        ToLongFunction<LikePlayerStats> countExtractor, PlayerTranslator tr) {
                if (list.isEmpty()) {
                        b.append(Component.newline());
                        b.append(Component.newline());
                        b.append(Component.text(tr.translate("likes.book.ranking.empty"))
                                        .color(NamedTextColor.GRAY));
                } else {
                        for (int i = 0; i < list.size(); i++) {
                                LikePlayerStats s = list.get(i);
                                b.append(Component.newline());
                                b.append(Component.text((i + 1) + ". ").color(NamedTextColor.DARK_GRAY));
                                b.append(Component.text(BookComponents.truncate(s.playerName(), MAX_NAME_LEN))
                                                .color(NamedTextColor.BLACK));
                                b.append(Component.text("  ♥" + countExtractor.applyAsLong(s))
                                                .color(NamedTextColor.RED));
                        }
                }
        }
}
