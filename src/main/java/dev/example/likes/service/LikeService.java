package dev.example.likes.service;

import dev.example.likes.database.BroadcastRepository;
import dev.example.likes.database.BroadcastStatsRepository;
import dev.example.likes.database.DailyLimitRepository;
import dev.example.likes.database.DatabaseManager;
import dev.example.likes.database.DatabaseWriteExecutor;
import dev.example.likes.database.EventRepository;
import dev.example.likes.database.PlayerStatsRepository;
import dev.example.likes.model.LikesBroadcast;
import dev.example.likes.model.LikesEvent;
import dev.example.likes.util.DisplayCodeGenerator;
import dev.example.likes.util.MessageFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import net.kyori.adventure.audience.Audience;
import org.bukkit.plugin.Plugin;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Service responsible for the business logic of sending likes and reactions.
 * <p>
 * All DB writes are routed through {@link DatabaseWriteExecutor} to guarantee
 * serialized SQLite access. Bukkit/Paper API calls are confined to the server
 * main thread; no Bukkit objects are touched from the write executor thread.
 * </p>
 */
public class LikeService {

    private static final Logger log = Logger.getLogger(LikeService.class.getName());

    private final BroadcastRepository broadcastRepository;
    private final EventRepository eventRepository;
    private final DailyLimitRepository dailyLimitRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final BroadcastStatsRepository broadcastStatsRepository;
    private final DatabaseManager databaseManager;
    private final DatabaseWriteExecutor writeExecutor;
    private final DisplayCodeGenerator displayCodeGenerator;
    private final CooldownService cooldownService;
    private final RecentService recentService;
    private final MessageFactory messageFactory;
    private final FileConfiguration config;
    private final Plugin plugin;

    /**
     * Constructs a LikeService with all required dependencies.
     */
    public LikeService(
            BroadcastRepository broadcastRepository,
            EventRepository eventRepository,
            DailyLimitRepository dailyLimitRepository,
            PlayerStatsRepository playerStatsRepository,
            BroadcastStatsRepository broadcastStatsRepository,
            DatabaseManager databaseManager,
            DatabaseWriteExecutor writeExecutor,
            DisplayCodeGenerator displayCodeGenerator,
            CooldownService cooldownService,
            RecentService recentService,
            MessageFactory messageFactory,
            FileConfiguration config,
            Plugin plugin) {
        this.broadcastRepository = broadcastRepository;
        this.eventRepository = eventRepository;
        this.dailyLimitRepository = dailyLimitRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.broadcastStatsRepository = broadcastStatsRepository;
        this.databaseManager = databaseManager;
        this.writeExecutor = writeExecutor;
        this.displayCodeGenerator = displayCodeGenerator;
        this.cooldownService = cooldownService;
        this.recentService = recentService;
        this.messageFactory = messageFactory;
        this.config = config;
        this.plugin = plugin;
    }

    // ── Main operations ──────────────────────────────────────────────────────

    /**
     * Sends a like from the sender to the target player.
     * <p>
     * Validates inputs and checks limits on the main thread, then submits a
     * single atomic write transaction to the {@link DatabaseWriteExecutor}. On
     * completion the success/failure callback runs back on the server main thread.
     * </p>
     *
     * @param sender the player sending the like
     * @param target the target player
     * @param reason the reason text for the like
     */
    public void sendLike(Player sender, Player target, String reason) {
        // ── 1. Validate inputs ────────────────────────────────────────────────
        if (reason == null || reason.isEmpty()) {
            sender.sendMessage(messageFactory.error("likes.error.reason.empty"));
            return;
        }
        int maxLength = config.getInt("reason.maxLength", 48);
        if (reason.length() > maxLength) {
            sender.sendMessage(messageFactory.error("likes.error.reason.too-long", Component.text(maxLength)));
            return;
        }
        if (reason.contains("\n") || reason.contains("\r")) {
            sender.sendMessage(messageFactory.error("likes.error.reason.newline"));
            return;
        }

        // ── 2. Disallow self-like ─────────────────────────────────────────────
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(messageFactory.error("likes.error.self"));
            return;
        }

        // ── 3. Check cooldown ─────────────────────────────────────────────────
        if (cooldownService.isOnCooldown(sender.getUniqueId(), target.getUniqueId())) {
            long remaining = cooldownService.getRemainingSeconds(sender.getUniqueId(), target.getUniqueId());
            sender.sendMessage(messageFactory.error("likes.error.cooldown", Component.text(remaining)));
            return;
        }

        // ── 4. Check daily limit (DB read, main thread) ───────────────────────
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        int dailyLimit = config.getInt("limits.dailyDirectLikeLimit", 20);
        try {
            int dailyCount = dailyLimitRepository.getDailyCount(today, sender.getUniqueId());
            if (dailyCount >= dailyLimit) {
                sender.sendMessage(messageFactory.error("likes.error.daily-limit", Component.text(dailyLimit)));
                return;
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to get daily count for " + sender.getUniqueId(), e);
            sender.sendMessage(messageFactory.error("likes.error.internal"));
            return;
        }

        // ── 5. Generate display code (DB read, main thread) ───────────────────
        String displayCode;
        try {
            displayCode = displayCodeGenerator.generateUnique();
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to generate unique displayCode", e);
            sender.sendMessage(messageFactory.error("likes.error.internal"));
            return;
        }

        // ── 6. Capture Bukkit values before leaving the main thread ───────────
        UUID senderUuid = sender.getUniqueId();
        String senderName = sender.getName();
        UUID targetUuid = target.getUniqueId();
        String targetName = target.getName();

        String broadcastId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        LikesBroadcast broadcast = new LikesBroadcast(
                broadcastId, displayCode, now, "DIRECT",
                senderUuid, targetUuid, "CUSTOM", reason);
        LikesEvent senderEvent = new LikesEvent(
                UUID.randomUUID().toString(), now, broadcastId, senderUuid, targetUuid);

        // ── 7. Submit atomic write transaction ────────────────────────────────
        writeExecutor.submit(() -> {
            databaseManager.executeInTransaction(conn -> {
                broadcastRepository.save(broadcast);
                eventRepository.save(senderEvent);
                broadcastStatsRepository.insertNew(conn, broadcastId, now);
                playerStatsRepository.upsertSentCount(conn, senderUuid, senderName, now);
                playerStatsRepository.upsertReceivedCount(conn, targetUuid, targetName, now);
                dailyLimitRepository.increment(today, senderUuid);
            });
            return null;
        }).whenComplete((ignored, ex) ->
        // ── 8. Callback on the main thread ────────────────────────────
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (ex != null) {
                log.log(Level.SEVERE, "Failed to persist like from " + senderUuid, unwrap(ex));
                Player senderOnline = Bukkit.getPlayer(senderUuid);
                if (senderOnline != null) {
                    senderOnline.sendMessage(messageFactory.error("likes.error.internal"));
                }
                return;
            }

            // Set in-memory state now that the DB write succeeded
            cooldownService.setCooldown(senderUuid, targetUuid);
            recentService.add(broadcast);
            Bukkit.getOnlinePlayers().forEach(
                    p -> recentService.updateLastSeen(p.getUniqueId(), broadcastId));

            // Send success notification to sender (re-resolve in case they logged back in)
            Player senderOnline = Bukkit.getPlayer(senderUuid);
            if (senderOnline != null) {
                senderOnline.sendMessage(messageFactory.success(
                        "likes.command.sent",
                        Component.text(targetName).color(NamedTextColor.WHITE),
                        Component.text(reason).color(NamedTextColor.GRAY)));
            }

            // Broadcast to all players except sender and target, plus console
            Component senderDisplay = Component.text(senderName).color(NamedTextColor.WHITE);
            Component targetDisplay = Component.text(targetName).color(NamedTextColor.WHITE);
            Audience others = Audience.audience(
                    Stream.concat(
                            Bukkit.getOnlinePlayers().stream()
                                    .filter(p -> !p.getUniqueId().equals(targetUuid)
                                            && !p.getUniqueId().equals(senderUuid)),
                            Stream.of(Bukkit.getConsoleSender()))
                            .collect(java.util.stream.Collectors.toList()));
            others.sendMessage(messageFactory.buildBroadcastMessage(
                    broadcast, senderDisplay, targetDisplay, -1, false, true, true));

            // Send special message to target with "you" label and no react button
            Player targetOnline = Bukkit.getPlayer(targetUuid);
            if (targetOnline != null) {
                Component youDisplay = Component.translatable("likes.broadcast.you")
                        .color(NamedTextColor.GREEN);
                targetOnline.sendMessage(messageFactory.buildBroadcastMessage(
                        broadcast, senderDisplay, youDisplay));
            }
        }));
    }

    /**
     * Sends a reaction to the broadcast identified by the given displayCode.
     *
     * @param sender      the player sending the reaction
     * @param displayCode the 4-character display code (without {@code #} prefix)
     */
    public void react(Player sender, String displayCode) {
        LikesBroadcast broadcast;
        try {
            var optBroadcast = broadcastRepository.findLatestByDisplayCode(displayCode);
            if (optBroadcast.isEmpty()) {
                sender.sendMessage(messageFactory.error("likes.error.not-found",
                        Component.text(displayCode)));
                return;
            }
            broadcast = optBroadcast.get();
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to find broadcast by displayCode: " + displayCode, e);
            sender.sendMessage(messageFactory.error("likes.error.internal"));
            return;
        }

        reactToBroadcast(sender, broadcast);
    }

    /**
     * Sends a reaction to the broadcast the player last saw.
     *
     * @param sender the player sending the reaction
     */
    public void react(Player sender) {
        var optBroadcastId = recentService.getLastSeenBroadcastId(sender.getUniqueId());
        if (optBroadcastId.isEmpty()) {
            sender.sendMessage(messageFactory.error("likes.error.no-recent"));
            return;
        }

        String broadcastId = optBroadcastId.get();
        var optBroadcast = recentService.findById(broadcastId);
        if (optBroadcast.isEmpty()) {
            sender.sendMessage(messageFactory.error("likes.error.no-recent"));
            return;
        }

        reactToBroadcast(sender, optBroadcast.get());
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Core reaction logic for a resolved broadcast.
     * <p>
     * Duplicate and self-react checks happen on the main thread. The write
     * transaction is submitted to {@link DatabaseWriteExecutor}; success/failure
     * messages are sent back on the main thread.
     * </p>
     */
    private void reactToBroadcast(Player sender, LikesBroadcast broadcast) {
        // 1. Disallow self-react
        if (sender.getUniqueId().equals(broadcast.targetUuid())) {
            sender.sendMessage(messageFactory.error("likes.error.self"));
            return;
        }

        String displayCode = broadcast.displayCode();
        Component displayCodeComponent = Component.text("(#" + displayCode + ")")
                .color(NamedTextColor.WHITE);

        // 2. Check for duplicate reaction (DB read, main thread)
        try {
            if (eventRepository.exists(broadcast.broadcastId(), sender.getUniqueId())) {
                sender.sendMessage(messageFactory.error("likes.error.already-reacted", displayCodeComponent));
                return;
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE,
                    "Failed to check event existence for broadcastId: " + broadcast.broadcastId(), e);
            sender.sendMessage(messageFactory.error("likes.error.internal"));
            return;
        }

        // 3. Capture values before leaving the main thread
        UUID senderUuid = sender.getUniqueId();
        String senderName = sender.getName();
        // Resolve target name on main thread (may call Bukkit API)
        String targetName = resolvePlayerName(broadcast.targetUuid());

        String eventId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        LikesEvent event = new LikesEvent(
                eventId, now, broadcast.broadcastId(), senderUuid, broadcast.targetUuid());

        // 4. Submit atomic write transaction
        writeExecutor.submit(() -> {
            databaseManager.executeInTransaction(conn -> {
                eventRepository.save(event);
                broadcastStatsRepository.incrementReactionCount(conn, broadcast.broadcastId(), now);
                playerStatsRepository.upsertReactedCount(conn, senderUuid, senderName, now);
            });
            return null;
        }).whenComplete((ignored, ex) ->
        // 5. Callback on the main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player senderOnline = Bukkit.getPlayer(senderUuid);
            if (ex != null) {
                Throwable cause = unwrap(ex);
                if (cause instanceof SQLIntegrityConstraintViolationException) {
                    // UNIQUE constraint: concurrent duplicate reaction
                    if (senderOnline != null) {
                        senderOnline.sendMessage(messageFactory.error(
                                "likes.error.already-reacted", displayCodeComponent));
                    }
                } else {
                    log.log(Level.SEVERE,
                            "Failed to persist reaction on broadcastId: "
                                    + broadcast.broadcastId(),
                            cause);
                    if (senderOnline != null) {
                        senderOnline.sendMessage(messageFactory.error("likes.error.internal"));
                    }
                }
                return;
            }

            // Send success message
            Component targetNameComponent = Component.text(targetName).color(NamedTextColor.WHITE);
            if (senderOnline != null) {
                senderOnline.sendMessage(messageFactory.success(
                        "likes.likeboost.success", targetNameComponent, displayCodeComponent));
            }

            // Update lastSeen
            recentService.updateLastSeen(senderUuid, broadcast.broadcastId());
        }));
    }

    /**
     * Resolves a player's display name from UUID; must be called on the main
     * thread.
     */
    private String resolvePlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null)
            return online.getName();
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString();
    }

    /** Unwraps a {@link CompletionException} to its root cause. */
    private static Throwable unwrap(Throwable t) {
        while (t instanceof CompletionException && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }
}
