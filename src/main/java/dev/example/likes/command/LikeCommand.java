package dev.example.likes.command;

import dev.example.likes.database.EventRepository;
import dev.example.likes.model.LikesBroadcast;
import dev.example.likes.service.LikeService;
import dev.example.likes.service.RecentService;
import dev.example.likes.util.MessageFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unified handler for the /like command.
 *
 * <ul>
 * <li>{@code /like <player> <reason...>} — send a like to a player</li>
 * <li>{@code /like #<displayCode>} — react to a broadcast by display code</li>
 * <li>{@code /like list} — show the 5 most recent likes</li>
 * </ul>
 *
 * <p>
 * Argument routing:
 * </p>
 * <ol>
 * <li>If the first argument starts with {@code #}, it is treated as a display
 * code.</li>
 * <li>If the first argument is {@code list} (case-insensitive), the list is
 * shown.</li>
 * <li>Otherwise, the first argument is treated as a player name.</li>
 * </ol>
 */
public class LikeCommand implements CommandExecutor, TabCompleter {

    private static final Logger log = Logger.getLogger(LikeCommand.class.getName());

    private final LikeService likeService;
    private final RecentService recentService;
    private final EventRepository eventRepository;
    private final MessageFactory messageFactory;

    public LikeCommand(LikeService likeService, RecentService recentService,
            EventRepository eventRepository, MessageFactory messageFactory) {
        this.likeService = likeService;
        this.recentService = recentService;
        this.eventRepository = eventRepository;
        this.messageFactory = messageFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageFactory.error("likes.error.console-only"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(messageFactory.usageInfo("like", "displaycode", "list"));
            return true;
        }

        String first = args[0];

        // /like list
        if (first.equalsIgnoreCase("list")) {
            handleList(player);
            return true;
        }

        // /like #<displayCode> — react by display code (strip the # prefix)
        if (first.startsWith("#")) {
            String displayCode = first.substring(1);
            if (displayCode.isEmpty()) {
                player.sendMessage(messageFactory.usageInfo("displaycode"));
                return true;
            }
            likeService.react(player, displayCode);
            return true;
        }

        // /like <player> <reason...>
        if (args.length < 2) {
            player.sendMessage(messageFactory.usageInfo("like"));
            return true;
        }

        Player target = Bukkit.getPlayer(first);
        if (target == null) {
            player.sendMessage(messageFactory.error("likes.error.player-not-found", Component.text(first)));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        likeService.sendLike(player, target, reason);
        return true;
    }

    private void handleList(Player player) {
        List<LikesBroadcast> recent = recentService.getRecent(5);

        if (recent.isEmpty()) {
            player.sendMessage(messageFactory.info("likes.recent.empty"));
            return;
        }

        List<String> broadcastIds = recent.stream().map(LikesBroadcast::broadcastId).toList();
        Map<String, Integer> countMap = new HashMap<>();
        Set<String> reactedIds = new HashSet<>();
        try {
            countMap = eventRepository.countByBroadcastIds(broadcastIds);
            reactedIds = eventRepository.reactedBroadcastIds(broadcastIds, player.getUniqueId());
        } catch (SQLException e) {
            log.log(Level.WARNING, "Failed to get reaction data for recent broadcasts", e);
        }

        player.sendMessage(messageFactory.info("likes.recent.title"));
        for (LikesBroadcast broadcast : recent) {
            boolean isOwnSend = broadcast.sourceSenderUuid().equals(player.getUniqueId());
            Component senderDisplay = isOwnSend
                    ? Component.translatable("likes.broadcast.you").color(NamedTextColor.GREEN)
                    : Component.text(resolveName(broadcast.sourceSenderUuid())).color(NamedTextColor.WHITE);
            boolean isOwnLike = broadcast.targetUuid().equals(player.getUniqueId());
            Component targetDisplay = isOwnLike
                    ? Component.translatable("likes.broadcast.you").color(NamedTextColor.GREEN)
                    : Component.text(resolveName(broadcast.targetUuid())).color(NamedTextColor.WHITE);
            int count = countMap.getOrDefault(broadcast.broadcastId(), 0);
            boolean alreadyReacted = reactedIds.contains(broadcast.broadcastId());
            Component msg = messageFactory.buildBroadcastMessage(broadcast, senderDisplay, targetDisplay, count,
                    alreadyReacted, true, !isOwnLike);
            player.sendMessage(msg);
        }

        recentService.updateLastSeen(player.getUniqueId(), recent.get(0).broadcastId());
    }

    private String resolveName(java.util.UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();

            // "list" subcommand
            if ("list".startsWith(partial)) {
                suggestions.add("list");
            }

            // Recent display codes with # prefix
            recentService.getRecentDisplayCodes(5).stream()
                    .map(code -> "#" + code)
                    .filter(s -> s.toLowerCase().startsWith(partial))
                    .forEach(suggestions::add);

            // Online player names
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(sender))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .forEach(suggestions::add);

            return suggestions;
        }
        return List.of();
    }
}
