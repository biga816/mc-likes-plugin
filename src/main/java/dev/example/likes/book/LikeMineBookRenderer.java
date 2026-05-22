package dev.example.likes.book;

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
 * <li>Page 1: Summary — stat counts + recent received likes</li>
 * <li>Page 2: Likes You Received — recent received broadcasts</li>
 * <li>Page 3: Likes You Sent — recent sent broadcasts</li>
 * </ul>
 */
public class LikeMineBookRenderer {

    private static final int MAX_NAME_LEN = 14;
    private static final int MAX_REASON_LEN = 17;

    /** Number of recent received entries shown on the summary page. */
    private static final int SUMMARY_RECENT_LIMIT = 3;

    /**
     * Builds all pages for the mine book.
     *
     * @param stats              the player's aggregate stats, or {@code null} if
     *                           none
     * @param receivedBroadcasts recent broadcasts where this player is the target
     * @param sentBroadcasts     recent broadcasts where this player is the sender
     * @param reactionCounts     map of broadcastId → reaction count
     * @param translator         locale-bound translator for the viewing player
     * @return list of page components (3 pages)
     */
    public List<Component> buildPages(
            LikePlayerStats stats,
            List<LikesBroadcast> receivedBroadcasts,
            List<LikesBroadcast> sentBroadcasts,
            Map<String, Long> reactionCounts,
            PlayerTranslator translator) {
        List<Component> pages = new ArrayList<>();
        pages.add(buildSummaryPage(stats, receivedBroadcasts, reactionCounts, translator));
        pages.add(buildReceivedPage(receivedBroadcasts, reactionCounts, translator));
        pages.add(buildSentPage(sentBroadcasts, reactionCounts, translator));
        return pages;
    }

    // ── Page builders ─────────────────────────────────────────────────────────

    private Component buildSummaryPage(LikePlayerStats stats,
            List<LikesBroadcast> recent,
            Map<String, Long> reactionCounts,
            PlayerTranslator tr) {
        TextComponent.Builder b = Component.text();
        b.append(Component.text(tr.translate("likes.book.mine.title"))
                .color(NamedTextColor.BLACK)
                .decorate(TextDecoration.BOLD));
        b.append(Component.newline());
        b.append(Component.newline());

        // ── Stats section ──────────────────────────────────────────────────
        b.append(Component.text(tr.translate("likes.book.mine.summary"))
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

        // ── Recent received section ────────────────────────────────────────
        b.append(Component.newline());
        b.append(Component.newline());
        b.append(Component.text(tr.translate("likes.book.mine.recent_received"))
                .color(NamedTextColor.DARK_GRAY)
                .decorate(TextDecoration.BOLD));

        if (recent.isEmpty()) {
            b.append(Component.newline());
            b.append(Component.text(tr.translate("likes.book.empty")).color(NamedTextColor.GRAY));
        } else {
            int limit = Math.min(recent.size(), SUMMARY_RECENT_LIMIT);
            for (int i = 0; i < limit; i++) {
                b.append(Component.newline());
                b.append(Component.text((i + 1) + ". ")
                        .color(NamedTextColor.DARK_GRAY));
                appendBroadcastEntry(b, recent.get(i), reactionCounts, /* sender */ true);
            }
        }
        return b.build();
    }

    private Component buildReceivedPage(List<LikesBroadcast> list,
            Map<String, Long> reactionCounts,
            PlayerTranslator tr) {
        TextComponent.Builder b = Component.text();
        b.append(Component.text(tr.translate("likes.book.mine.received_page"))
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
        b.append(Component.text(tr.translate("likes.book.mine.sent_page"))
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
        String reason = BookComponents.truncate(bc.reasonText(), MAX_REASON_LEN);
        long count = reactionCounts.getOrDefault(bc.broadcastId(), 0L);

        b.append(Component.text(BookComponents.truncate(name, MAX_NAME_LEN))
                .color(NamedTextColor.BLACK));
        b.append(Component.text("  ♥" + count + " ").color(NamedTextColor.RED));
        b.append(Component.newline());
        b.append(BookComponents.buildReasonLine(bc.reasonText(), reason, "   "));
    }
}
