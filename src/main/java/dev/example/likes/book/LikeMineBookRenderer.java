package dev.example.likes.book;

import dev.example.likes.model.BroadcastRankingEntry;
import dev.example.likes.model.LikePlayerStats;
import dev.example.likes.model.LikesBroadcast;
import dev.example.likes.util.PlayerTranslator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the 3-page book for {@code /like mine}.
 *
 * <ul>
 * <li>Page 1: Summary — stat counts + most liked events received</li>
 * <li>Page 2: Likes You Received — recent received broadcasts</li>
 * <li>Page 3: Likes You Sent — recent sent broadcasts</li>
 * </ul>
 */
public class LikeMineBookRenderer {

    private static final int MAX_NAME_LEN = 14;
    private static final int MAX_REASON_LEN = 17;

    /**
     * Builds all pages for the mine book.
     *
     * @param stats              the player's aggregate stats, or {@code null} if
     *                           none
     * @param mostLikedReceived  the received broadcasts with the most reactions,
     *                           ordered by reaction_count DESC, created_at DESC
     * @param receivedBroadcasts recent broadcasts where this player is the target
     * @param sentBroadcasts     recent broadcasts where this player is the sender
     * @param reactionCounts     map of broadcastId → reaction count
     * @param translator         locale-bound translator for the viewing player
     * @return list of page components (3 pages)
     */
    public List<Component> buildPages(
            LikePlayerStats stats,
            List<BroadcastRankingEntry> mostLikedReceived,
            List<LikesBroadcast> receivedBroadcasts,
            List<LikesBroadcast> sentBroadcasts,
            Map<String, Long> reactionCounts,
            PlayerTranslator translator) {
        List<Component> pages = new ArrayList<>();
        pages.add(buildSummaryPage(stats, mostLikedReceived, translator));
        pages.add(buildReceivedPage(receivedBroadcasts, reactionCounts, translator));
        pages.add(buildSentPage(sentBroadcasts, reactionCounts, translator));
        return pages;
    }

    // ── Page builders ─────────────────────────────────────────────────────────

    private Component buildSummaryPage(LikePlayerStats stats,
            List<BroadcastRankingEntry> mostLiked,
            PlayerTranslator tr) {
        TextComponent.Builder b = Component.text();
        b.append(Component.text(tr.translate("likes.book.mine.title"))
                .color(NamedTextColor.BLACK)
                .decorate(TextDecoration.BOLD));
        b.append(Component.newline());
        b.append(Component.newline());

        // ── Stats section ──────────────────────────────────────────────────
        b.append(Component.text("⏷" + tr.translate("likes.book.mine.summary"))
                .color(NamedTextColor.DARK_GRAY)
                .decorate(TextDecoration.BOLD));
        b.append(Component.newline());

        long received = stats != null ? stats.receivedCount() : 0;
        long sent = stats != null ? stats.sentCount() : 0;
        long reacted = stats != null ? stats.reactedCount() : 0;

        b.append(Component.text(tr.translate("likes.book.mine.received") + ": ").color(NamedTextColor.DARK_GRAY));
        b.append(Component.text("♥" + received).color(NamedTextColor.RED));
        b.append(Component.newline());
        b.append(Component.text(tr.translate("likes.book.mine.sent") + ": ").color(NamedTextColor.DARK_GRAY));
        b.append(Component.text("♥" + sent).color(NamedTextColor.RED));
        b.append(Component.newline());
        b.append(Component.text(tr.translate("likes.book.mine.reacted") + ": ").color(NamedTextColor.DARK_GRAY));
        b.append(Component.text("♥" + reacted).color(NamedTextColor.RED));

        // ── Most liked event section ───────────────────────────────────────
        b.append(Component.newline());
        b.append(Component.newline());
        b.append(Component.text("⏷" + tr.translate("likes.book.mine.most_liked_event"))
                .color(NamedTextColor.DARK_GRAY)
                .decorate(TextDecoration.BOLD));

        if (mostLiked.isEmpty()) {
            b.append(Component.newline());
            b.append(Component.newline());
            b.append(Component.text(tr.translate("likes.book.empty")).color(NamedTextColor.GRAY));
        } else {
            for (int i = 0; i < mostLiked.size(); i++) {
                b.append(Component.newline());
                b.append(Component.text((i + 1) + ". ").color(NamedTextColor.DARK_GRAY));
                appendRankingEntry(b, mostLiked.get(i), /* showSender */ true);
            }
        }
        return b.build();
    }

    private Component buildReceivedPage(List<LikesBroadcast> list,
            Map<String, Long> reactionCounts,
            PlayerTranslator tr) {
        TextComponent.Builder b = Component.text();
        b.append(Component.text("⏷" + tr.translate("likes.book.mine.received_page"))
                .color(NamedTextColor.DARK_GRAY)
                .decorate(TextDecoration.BOLD));

        if (list.isEmpty()) {
            b.append(Component.newline());
            b.append(Component.newline());
            b.append(Component.text(tr.translate("likes.book.empty")).color(NamedTextColor.GRAY));
        } else {
            for (int i = 0; i < list.size(); i++) {
                b.append(Component.newline());
                b.append(Component.text((i + 1) + ". ").color(NamedTextColor.DARK_GRAY));
                appendBroadcastEntry(b, list.get(i), reactionCounts, /* showSender */ true);
            }
        }
        return b.build();
    }

    private Component buildSentPage(List<LikesBroadcast> list,
            Map<String, Long> reactionCounts,
            PlayerTranslator tr) {
        TextComponent.Builder b = Component.text();
        b.append(Component.text("⏷" + tr.translate("likes.book.mine.sent_page"))
                .color(NamedTextColor.DARK_GRAY)
                .decorate(TextDecoration.BOLD));

        if (list.isEmpty()) {
            b.append(Component.newline());
            b.append(Component.newline());
            b.append(Component.text(tr.translate("likes.book.empty")).color(NamedTextColor.GRAY));
        } else {
            for (int i = 0; i < list.size(); i++) {
                b.append(Component.newline());
                b.append(Component.text((i + 1) + ". ").color(NamedTextColor.DARK_GRAY));
                appendBroadcastEntry(b, list.get(i), reactionCounts, /* showSender */ false);
            }
        }
        return b.build();
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private void appendBroadcastEntry(TextComponent.Builder b, LikesBroadcast bc,
            Map<String, Long> reactionCounts, boolean showSender) {
        String name = showSender
                ? BookComponents.resolveName(bc.sourceSenderUuid())
                : BookComponents.resolveName(bc.targetUuid());
        long count = reactionCounts.getOrDefault(bc.broadcastId(), 0L);
        appendEntry(b, name, bc.reasonText(), count, bc.createdAt());
    }

    private void appendRankingEntry(TextComponent.Builder b, BroadcastRankingEntry entry, boolean showSender) {
        String name = showSender
                ? BookComponents.resolveName(entry.sourceSenderUuid())
                : BookComponents.resolveName(entry.targetUuid());
        appendEntry(b, name, entry.reasonText(), entry.reactionCount(), entry.createdAt());
    }

    private void appendEntry(TextComponent.Builder b, String name, String reasonText, long count, long createdAt) {
        String reason = BookComponents.truncate(reasonText, MAX_REASON_LEN);

        b.append(Component.text(BookComponents.truncate(name, MAX_NAME_LEN))
                .color(NamedTextColor.BLACK));
        b.append(Component.text("  ♥" + count + " ").color(NamedTextColor.RED));
        b.append(Component.newline());
        b.append(BookComponents.buildReasonLine(reasonText, reason, "   ", createdAt));
    }
}
