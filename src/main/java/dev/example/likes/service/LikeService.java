package dev.example.likes.service;

import dev.example.likes.database.BroadcastRepository;
import dev.example.likes.database.DailyLimitRepository;
import dev.example.likes.database.EventRepository;
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

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Service responsible for the business logic of sending likes and reactions.
 */
public class LikeService {
    private static final Logger log = Logger.getLogger(LikeService.class.getName());

    private final BroadcastRepository broadcastRepository;
    private final EventRepository eventRepository;
    private final DailyLimitRepository dailyLimitRepository;
    private final DisplayCodeGenerator displayCodeGenerator;
    private final CooldownService cooldownService;
    private final RecentService recentService;
    private final MessageFactory messageFactory;
    private final FileConfiguration config;
    @SuppressWarnings("unused") // retained as a constructor argument required by the spec
    private final Plugin plugin;

    /**
     * Constructs a LikeService.
     *
     * @param broadcastRepository  broadcast repository
     * @param eventRepository      event repository
     * @param dailyLimitRepository daily limit repository
     * @param displayCodeGenerator display code generator
     * @param cooldownService      cooldown service
     * @param recentService        recent broadcast service
     * @param messageFactory       message factory
     * @param config               plugin configuration
     * @param plugin               plugin instance
     */
    public LikeService(
            BroadcastRepository broadcastRepository,
            EventRepository eventRepository,
            DailyLimitRepository dailyLimitRepository,
            DisplayCodeGenerator displayCodeGenerator,
            CooldownService cooldownService,
            RecentService recentService,
            MessageFactory messageFactory,
            FileConfiguration config,
            Plugin plugin) {
        this.broadcastRepository = broadcastRepository;
        this.eventRepository = eventRepository;
        this.dailyLimitRepository = dailyLimitRepository;
        this.displayCodeGenerator = displayCodeGenerator;
        this.cooldownService = cooldownService;
        this.recentService = recentService;
        this.messageFactory = messageFactory;
        this.config = config;
        this.plugin = plugin;
    }

    /**
     * Sends a like from the sender to the target player.
     * <p>
     * Performs validation, cooldown, and daily limit checks. If all pass,
     * persists the broadcast and sends a server-wide notification plus a personal
     * notification to the target. On failure, sends an error message to the sender
     * and returns early.
     * </p>
     *
     * @param sender the player sending the like
     * @param target the target player
     * @param reason the reason text for the like
     */
    public void sendLike(Player sender, Player target, String reason) {
        // 1. Validate inputs
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

        // 2. Disallow self-like
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(messageFactory.error("likes.error.self"));
            return;
        }

        // 3. Check cooldown
        if (cooldownService.isOnCooldown(sender.getUniqueId(), target.getUniqueId())) {
            long remaining = cooldownService.getRemainingSeconds(sender.getUniqueId(), target.getUniqueId());
            sender.sendMessage(messageFactory.error("likes.error.cooldown", Component.text(remaining)));
            return;
        }

        // 4. Check daily limit
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

        // 5. Generate displayCode
        String displayCode;
        try {
            displayCode = displayCodeGenerator.generateUnique();
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to generate unique displayCode", e);
            sender.sendMessage(messageFactory.error("likes.error.internal"));
            return;
        }

        // 6. Create and persist the broadcast
        String broadcastId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        LikesBroadcast broadcast = new LikesBroadcast(
                broadcastId,
                displayCode,
                now,
                "DIRECT",
                sender.getUniqueId(),
                target.getUniqueId(),
                "CUSTOM",
                reason);
        try {
            broadcastRepository.save(broadcast);
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to save broadcast", e);
            sender.sendMessage(messageFactory.error("likes.error.internal"));
            return;
        }

        // 7. Create and persist the event for the sender
        String eventId = UUID.randomUUID().toString();
        LikesEvent event = new LikesEvent(
                eventId,
                now,
                broadcastId,
                sender.getUniqueId(),
                target.getUniqueId());
        try {
            eventRepository.save(event);
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to save event", e);
            sender.sendMessage(messageFactory.error("likes.error.internal"));
            return;
        }

        // 8. Increment the daily like count
        try {
            dailyLimitRepository.increment(today, sender.getUniqueId());
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to increment daily count for " + sender.getUniqueId(), e);
            sender.sendMessage(messageFactory.error("likes.error.internal"));
            return;
        }

        // 9. Set cooldown
        cooldownService.setCooldown(sender.getUniqueId(), target.getUniqueId());

        // 10. Update in-memory buffer and lastSeen for all online players
        recentService.add(broadcast);
        Bukkit.getOnlinePlayers().forEach(p -> recentService.updateLastSeen(p.getUniqueId(), broadcast.broadcastId()));

        // 11. Send success notification to the sender
        String targetName = target.getName();
        sender.sendMessage(messageFactory.success(
                "likes.command.sent",
                Component.text(targetName).color(NamedTextColor.WHITE),
                Component.text(reason).color(NamedTextColor.GRAY)));

        // 12. Broadcast to all players except sender and target, plus console
        Component senderDisplay = Component.text(sender.getName()).color(NamedTextColor.WHITE);
        Component targetDisplay = Component.text(targetName).color(NamedTextColor.WHITE);
        Audience others = Audience.audience(
                Stream.concat(
                        Bukkit.getOnlinePlayers().stream()
                                .filter(p -> !p.getUniqueId().equals(target.getUniqueId())
                                        && !p.getUniqueId().equals(sender.getUniqueId())),
                        Stream.of(Bukkit.getConsoleSender())).collect(java.util.stream.Collectors.toList()));
        others.sendMessage(messageFactory.buildBroadcastMessage(broadcast, senderDisplay, targetDisplay, -1, false, true, true));

        // 13. Send broadcast to the target with "you" label and no react button
        Component youDisplay = Component.translatable("likes.broadcast.you").color(NamedTextColor.GREEN);
        target.sendMessage(messageFactory.buildBroadcastMessage(broadcast, senderDisplay, youDisplay));
    }

    /**
     * Sends a reaction to the broadcast identified by the given displayCode.
     * <p>
     * Resolves to the most recent broadcast matching the code. Sends an error
     * message and returns early if no matching broadcast exists.
     * </p>
     *
     * @param sender      the player sending the reaction
     * @param displayCode the 4-character display code of the target broadcast
     */
    public void react(Player sender, String displayCode) {
        LikesBroadcast broadcast;
        try {
            var optBroadcast = broadcastRepository.findLatestByDisplayCode(displayCode);
            if (optBroadcast.isEmpty()) {
                sender.sendMessage(messageFactory.error("likes.error.not-found", Component.text(displayCode)));
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
     * <p>
     * Retrieves the broadcast directly from the in-memory recent buffer via
     * {@link RecentService#getLastSeenBroadcastId(UUID)}.
     * Sends an error message and returns early if no lastSeen entry exists.
     * </p>
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

    /**
     * Performs the reaction logic for a resolved broadcast.
     *
     * @param sender    the player sending the reaction
     * @param broadcast the target broadcast
     */
    private void reactToBroadcast(Player sender, LikesBroadcast broadcast) {
        // 1. Disallow self-react
        if (sender.getUniqueId().equals(broadcast.targetUuid())) {
            sender.sendMessage(messageFactory.error("likes.error.self"));
            return;
        }

        // 2. Check for duplicate reaction
        try {
            if (eventRepository.exists(broadcast.broadcastId(), sender.getUniqueId())) {
                sender.sendMessage(messageFactory.error("likes.error.already-reacted"));
                return;
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to check event existence for broadcastId: " + broadcast.broadcastId(), e);
            sender.sendMessage(messageFactory.error("likes.error.internal"));
            return;
        }

        // 3. Create and persist the reaction event
        String eventId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        LikesEvent event = new LikesEvent(
                eventId,
                now,
                broadcast.broadcastId(),
                sender.getUniqueId(),
                broadcast.targetUuid());
        try {
            eventRepository.save(event);
        } catch (SQLIntegrityConstraintViolationException e) {
            sender.sendMessage(messageFactory.error("likes.error.already-reacted"));
            return;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to save react event for broadcastId: " + broadcast.broadcastId(), e);
            sender.sendMessage(messageFactory.error("likes.error.internal"));
            return;
        }

        // 4. Send success message
        sender.sendMessage(messageFactory.success("likes.likeboost.success"));

        // 5. Update lastSeen
        recentService.updateLastSeen(sender.getUniqueId(), broadcast.broadcastId());
    }
}
