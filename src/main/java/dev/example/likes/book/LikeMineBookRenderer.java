package dev.example.likes.book;

import dev.example.likes.model.ItemRankingEntry;
import dev.example.likes.model.PlayerStats;
import dev.example.likes.model.FeedItem;
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
 * <li>Page 2: Likes You Received — recent received items</li>
 * <li>Page 3: Likes You Sent — recent sent items</li>
 * </ul>
 */
public class LikeMineBookRenderer {

    private static final int MAX_NAME_LEN = 14;
    private static final int MAX_REASON_LEN = 17;

    /**
     * Builds all pages for the mine book.
     *
     * @param stats             the player's aggregate stats, or {@code null} if
     *                          none
     * @param mostLikedReceived the received items with the most reactions,
     *                          ordered by reaction_count DESC, created_at DESC
     * @param receivedItems     recent items where this player is the target
     * @param sentItems         recent items where this player is the sender
     * @param reactionCounts    map of itemId → reaction count
     * @param translator        locale-bound translator for the viewing player
     * @return list of page components (3 pages)
     */
    public List<Component> buildPages(
            PlayerStats stats,
            List<ItemRankingEntry> mostLikedReceived,
            List<FeedItem> receivedItems,
            List<FeedItem> sentItems,
            Map<String, Long> reactionCounts,
            PlayerTranslator translator) {
        List<Component> pages = new ArrayList<>();
        pages.add(buildSummaryPage(stats, mostLikedReceived, translator));
        pages.add(buildReceivedPage(receivedItems, reactionCounts, translator));
        pages.add(buildSentPage(sentItems, reactionCounts, translator));
        return pages;
    }

    // ── Page builders ─────────────────────────────────────────────────────────

    private Component buildSummaryPage(PlayerStats stats,
            List<ItemRankingEntry> mostLiked,
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

    private Component buildReceivedPage(List<FeedItem> list,
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
                appendItemEntry(b, list.get(i), reactionCounts, /* showSender */ true);
            }
        }
        return b.build();
    }

    private Component buildSentPage(List<FeedItem> list,
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
                appendItemEntry(b, list.get(i), reactionCounts, /* showSender */ false);
            }
        }
        return b.build();
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private void appendItemEntry(TextComponent.Builder b, FeedItem bc,
            Map<String, Long> reactionCounts, boolean showSender) {
        String name = showSender
                ? BookComponents.resolveName(bc.initiatorUuid() != null ? bc.initiatorUuid() : bc.authorUuid())
                : BookComponents.resolveName(bc.authorUuid());
        long count = reactionCounts.getOrDefault(bc.itemId(), 0L);
        appendEntry(b, name, bc.bodyText(), count, bc.createdAt());
    }

    private void appendRankingEntry(TextComponent.Builder b, ItemRankingEntry entry, boolean showSender) {
        String name = showSender
                ? BookComponents.resolveName(
                        entry.initiatorUuid() != null ? entry.initiatorUuid() : entry.authorUuid())
                : BookComponents.resolveName(entry.authorUuid());
        appendEntry(b, name, entry.bodyText(), entry.reactionCount(), entry.createdAt());
    }

    private void appendEntry(TextComponent.Builder b, String name, String bodyText, long count, long createdAt) {
        String reason = BookComponents.truncate(bodyText, MAX_REASON_LEN);

        b.append(Component.text(BookComponents.truncate(name, MAX_NAME_LEN))
                .color(NamedTextColor.BLACK));
        b.append(Component.text("  ♥" + count + " ").color(NamedTextColor.RED));
        b.append(Component.newline());
        b.append(BookComponents.buildReasonLine(bodyText, reason, "   ", createdAt));
    }
}
