package dev.biga.likes.book;

import dev.biga.likes.model.ItemRankingEntry;
import dev.biga.likes.model.PlayerStats;
import dev.biga.likes.model.FeedItem;
import dev.biga.likes.util.PlayerTranslator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
     * @param viewerUuid        UUID of the player viewing the book
     * @param translator        locale-bound translator for the viewing player
     * @return list of page components (3 pages)
     */
    public List<Component> buildPages(
            PlayerStats stats,
            List<ItemRankingEntry> mostLikedReceived,
            List<FeedItem> receivedItems,
            List<FeedItem> sentItems,
            Map<String, Long> reactionCounts,
            UUID viewerUuid,
            PlayerTranslator translator) {
        List<Component> pages = new ArrayList<>();
        pages.add(buildSummaryPage(stats, mostLikedReceived, viewerUuid, translator));
        pages.add(buildReceivedPage(receivedItems, reactionCounts, viewerUuid, translator));
        pages.add(buildSentPage(sentItems, reactionCounts, viewerUuid, translator));
        return pages;
    }

    // ── Page builders ─────────────────────────────────────────────────────────

    private Component buildSummaryPage(PlayerStats stats,
            List<ItemRankingEntry> mostLiked,
            UUID viewerUuid,
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
                appendRankingEntry(b, mostLiked.get(i), viewerUuid);
            }
        }
        return b.build();
    }

    private Component buildReceivedPage(List<FeedItem> list,
            Map<String, Long> reactionCounts,
            UUID viewerUuid,
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
                appendItemEntry(b, list.get(i), reactionCounts, viewerUuid);
            }
        }
        return b.build();
    }

    private Component buildSentPage(List<FeedItem> list,
            Map<String, Long> reactionCounts,
            UUID viewerUuid,
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
                appendItemEntry(b, list.get(i), reactionCounts, viewerUuid);
            }
        }
        return b.build();
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private void appendItemEntry(TextComponent.Builder b, FeedItem bc,
            Map<String, Long> reactionCounts, UUID viewerUuid) {
        long count = reactionCounts.getOrDefault(bc.itemId(), 0L);
        appendEntry(b, bc.itemType(), bc.initiatorUuid(), bc.authorUuid(), bc.bodyText(), count,
                bc.createdAt(), viewerUuid);
    }

    private void appendRankingEntry(TextComponent.Builder b, ItemRankingEntry entry, UUID viewerUuid) {
        appendEntry(b, entry.itemType(), entry.initiatorUuid(), entry.authorUuid(), entry.bodyText(),
                entry.reactionCount(), entry.createdAt(), viewerUuid);
    }

    private void appendEntry(TextComponent.Builder b, String itemType, UUID initiatorUuid, UUID authorUuid,
            String bodyText, long count, long createdAt, UUID viewerUuid) {
        String reason = BookComponents.truncateReason(bodyText);

        b.append(BookComponents.buildItemParticipants(
                itemType, initiatorUuid, authorUuid, viewerUuid,
                BookComponents.participantLayoutForNumberedItem(count)));
        b.append(Component.text("♥" + BookComponents.formatReactionCount(count) + " ")
                .color(NamedTextColor.RED));
        b.append(Component.newline());
        b.append(BookComponents.buildReasonLine(bodyText, reason, "   ", createdAt, count));
    }
}
