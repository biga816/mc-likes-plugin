package dev.example.likes.book;

import dev.example.likes.model.LikesBroadcast;
import dev.example.likes.util.PlayerTranslator;
import dev.example.likes.util.RelativeTimeFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Builds a multi-page book for {@code /like feed}.
 *
 * <p>
 * Each page shows {@code itemsPerPage} like events in reverse-chronological
 * order. Per entry:
 * </p>
 * <ul>
 * <li>Line 1: relative time (e.g. "2m ago" / "2分前")</li>
 * <li>Line 2: {@code sender→target  [♡count]} — heart is clickable if the
 * viewer has not yet reacted and is not a participant</li>
 * <li>Line 3: {@code  "reason"} — truncated, hover shows full text</li>
 * </ul>
 *
 * <p>
 * Display codes are intentionally omitted from this view.
 * </p>
 */
public class LikeFeedBookRenderer {

    /** Max characters for sender/target names on feed entries. */
    private static final int MAX_NAME_LEN = 7;

    /** Max reason characters shown inline (hover reveals the full text). */
    private static final int MAX_REASON_LEN = 18;

    /**
     * Builds all pages for the feed book.
     *
     * @param broadcasts        recent broadcasts, newest first
     * @param reactionCounts    map of broadcastId → reaction count
     * @param reactedBroadcasts broadcast IDs the viewer has already reacted to
     * @param viewerUuid        UUID of the player opening the book
     * @param itemsPerPage      number of entries per book page
     * @param tr                locale-bound translator for the viewing player
     * @return ordered list of page components
     */
    public List<Component> buildPages(
            List<LikesBroadcast> broadcasts,
            Map<String, Long> reactionCounts,
            Set<String> reactedBroadcasts,
            UUID viewerUuid,
            int itemsPerPage,
            PlayerTranslator tr) {

        List<Component> pages = new ArrayList<>();

        if (broadcasts.isEmpty()) {
            TextComponent.Builder b = Component.text();
            b.append(Component.text(tr.translate("likes.command.feed.title"))
                    .color(NamedTextColor.BLACK)
                    .decorate(TextDecoration.BOLD));
            b.append(Component.newline());
            b.append(Component.newline());
            b.append(Component.text(tr.translate("likes.command.feed.empty"))
                    .color(NamedTextColor.GRAY));
            pages.add(b.build());
            return pages;
        }

        for (int pageStart = 0; pageStart < broadcasts.size(); pageStart += itemsPerPage) {
            int pageEnd = Math.min(pageStart + itemsPerPage, broadcasts.size());
            List<LikesBroadcast> pageItems = broadcasts.subList(pageStart, pageEnd);

            TextComponent.Builder b = Component.text();

            // Title only on the first page
            if (pageStart == 0) {
                b.append(Component.text(tr.translate("likes.command.feed.title"))
                        .color(NamedTextColor.BLACK)
                        .decorate(TextDecoration.BOLD));
                b.append(Component.newline());
                b.append(Component.newline());
            }

            for (int i = 0; i < pageItems.size(); i++) {
                LikesBroadcast bc = pageItems.get(i);

                String timeStr = RelativeTimeFormatter.format(bc.createdAt(), tr);
                String senderName = BookComponents.truncate(BookComponents.resolveName(bc.sourceSenderUuid()), MAX_NAME_LEN);
                String targetName = BookComponents.truncate(BookComponents.resolveName(bc.targetUuid()), MAX_NAME_LEN);
                String reason = BookComponents.truncate(bc.reasonText(), MAX_REASON_LEN);
                long count = reactionCounts.getOrDefault(bc.broadcastId(), 0L);
                String code = bc.displayCode();
                boolean alreadyReacted = reactedBroadcasts.contains(bc.broadcastId());
                boolean isViewer = bc.sourceSenderUuid().equals(viewerUuid)
                        || bc.targetUuid().equals(viewerUuid);

                // Line 1: relative time
                b.append(Component.text(timeStr).color(NamedTextColor.DARK_GRAY)
                        .decorate(TextDecoration.ITALIC));
                b.append(Component.newline());

                // Line 2: sender→target [♡count]
                b.append(Component.text("  "));
                b.append(BookComponents.buildSenderArrowTarget(
                        senderName, BookComponents.nameColor(bc.sourceSenderUuid(), viewerUuid),
                        targetName, BookComponents.nameColor(bc.targetUuid(), viewerUuid)));
                b.append(BookComponents.buildClickableHeart(code, count, alreadyReacted, isViewer, tr));
                b.append(Component.newline());

                // Line 3: reason (truncated inline, full text on hover)
                b.append(BookComponents.buildReasonLine(bc.reasonText(), reason, "  "));

                // Blank separator between entries (not after the last one on the page)
                if (i < pageItems.size() - 1) {
                    b.append(Component.newline());
                }
            }

            pages.add(b.build());
        }

        return pages;
    }
}
