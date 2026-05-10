package dev.example.likes.book;

import dev.example.likes.database.BroadcastRepository;
import dev.example.likes.database.BroadcastStatsRepository;
import dev.example.likes.database.PlayerStatsRepository;
import dev.example.likes.model.BroadcastRankingEntry;
import dev.example.likes.model.LikePlayerStats;
import dev.example.likes.model.LikesBroadcast;
import dev.example.likes.util.MessageFactory;
import dev.example.likes.util.PlayerTranslator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates async DB reads and book-UI opening for
 * {@code /like ranking} and {@code /like mine}.
 *
 * <p>
 * Pattern: obtain a {@link PlayerTranslator} for the viewer, fetch DB data on
 * an async thread, then switch back to the main thread to build components and
 * call {@link Player#openBook(ItemStack)}.
 * </p>
 */
public class LikeBookService {

    private static final Logger log = Logger.getLogger(LikeBookService.class.getName());

    /** Number of players shown in the received / sent ranking pages. */
    private static final int RANKING_LIMIT = 10;

    /**
     * Number of broadcasts fetched for the Popular Likes page.
     * The renderer caps the display at 5 due to the 2-line-per-entry format.
     */
    private static final int POPULAR_LIMIT = 10;

    /** Number of broadcasts shown on each mine received/sent page. */
    private static final int MINE_LIMIT = 5;

    private final PlayerStatsRepository playerStatsRepo;
    private final BroadcastStatsRepository broadcastStatsRepo;
    private final BroadcastRepository broadcastRepo;
    private final MessageFactory messageFactory;
    private final JavaPlugin plugin;
    private final LikeRankingBookRenderer rankingRenderer;
    private final LikeMineBookRenderer mineRenderer;

    /**
     * Constructs the service with all required dependencies.
     *
     * @param playerStatsRepo    repository for per-player aggregation data
     * @param broadcastStatsRepo repository for per-broadcast aggregation data
     * @param broadcastRepo      repository for raw broadcast records
     * @param messageFactory     factory used to obtain per-player translators
     * @param plugin             the plugin instance (for scheduler access)
     */
    public LikeBookService(
            PlayerStatsRepository playerStatsRepo,
            BroadcastStatsRepository broadcastStatsRepo,
            BroadcastRepository broadcastRepo,
            MessageFactory messageFactory,
            JavaPlugin plugin) {
        this.playerStatsRepo = playerStatsRepo;
        this.broadcastStatsRepo = broadcastStatsRepo;
        this.broadcastRepo = broadcastRepo;
        this.messageFactory = messageFactory;
        this.plugin = plugin;
        this.rankingRenderer = new LikeRankingBookRenderer();
        this.mineRenderer = new LikeMineBookRenderer();
    }

    /**
     * Fetches ranking data asynchronously and opens the 3-page ranking book
     * on the main thread when done.
     *
     * @param player the player to open the book for
     */
    public void openRankingBook(Player player) {
        PlayerTranslator tr = messageFactory.translatorFor(player);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<LikePlayerStats> received = playerStatsRepo.getTopReceivedPlayers(RANKING_LIMIT);
                List<LikePlayerStats> sent = playerStatsRepo.getTopSentPlayers(RANKING_LIMIT);
                List<BroadcastRankingEntry> popular = broadcastStatsRepo.getTopBroadcasts(POPULAR_LIMIT);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    List<Component> pages = rankingRenderer.buildPages(received, sent, popular, tr);
                    openBook(player, tr.translate("likes.book.ranking.title"), pages);
                });
            } catch (SQLException e) {
                log.log(Level.WARNING, "Failed to fetch ranking data for " + player.getName(), e);
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> player.sendMessage(Component.text(tr.translate("likes.error.internal"))
                                .color(NamedTextColor.RED)));
            }
        });
    }

    /**
     * Fetches the player's like data asynchronously and opens the 3-page mine
     * book on the main thread when done.
     *
     * @param player the player to open the book for
     */
    public void openMineBook(Player player) {
        PlayerTranslator tr = messageFactory.translatorFor(player);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Optional<LikePlayerStats> statsOpt = playerStatsRepo.getPlayerStats(player.getUniqueId());
                List<LikesBroadcast> received = broadcastRepo.getRecentBroadcastsReceivedBy(player.getUniqueId(),
                        MINE_LIMIT);
                List<LikesBroadcast> sent = broadcastRepo.getRecentBroadcastsSentBy(player.getUniqueId(), MINE_LIMIT);

                List<String> allIds = new ArrayList<>();
                received.stream().map(LikesBroadcast::broadcastId).forEach(allIds::add);
                sent.stream().map(LikesBroadcast::broadcastId).forEach(allIds::add);
                Map<String, Long> reactionCounts = allIds.isEmpty()
                        ? Map.of()
                        : broadcastStatsRepo.reactionCountByBroadcastIds(allIds);

                LikePlayerStats stats = statsOpt.orElse(null);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    List<Component> pages = mineRenderer.buildPages(stats, received, sent, reactionCounts, tr);
                    openBook(player, tr.translate("likes.book.mine.title"), pages);
                });
            } catch (SQLException e) {
                log.log(Level.WARNING, "Failed to fetch mine data for " + player.getName(), e);
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> player.sendMessage(Component.text(tr.translate("likes.error.internal"))
                                .color(NamedTextColor.RED)));
            }
        });
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Creates a written book item with the given pages and opens it for the player.
     * Must be called on the main thread.
     *
     * @param player the player to show the book to
     * @param title  the book title (shown in the book UI header)
     * @param pages  ordered list of page components
     */
    private void openBook(Player player, String title, List<Component> pages) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null)
            return;
        meta.setTitle(title);
        meta.setAuthor(plugin.getName());
        meta.pages(pages);
        book.setItemMeta(meta);
        player.openBook(book);
    }
}
