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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unified handler for the /like command.
 *
 * <ul>
 * <li>{@code /like <player> <reason...>} — send a like</li>
 * <li>{@code /like boost [displayCode]} — react to a broadcast</li>
 * <li>{@code /like recent} — show the 5 most recent likes</li>
 * </ul>
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
            player.sendMessage(messageFactory.usageInfo("like", "boost", "recent"));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("boost")) {
            if (args.length >= 2) {
                likeService.react(player, args[1]);
            } else {
                likeService.react(player);
            }
            return true;
        }

        if (sub.equals("recent")) {
            handleRecent(player);
            return true;
        }

        // /like <player> <reason...>
        if (args.length < 2) {
            player.sendMessage(messageFactory.usageInfo("like"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(messageFactory.error("likes.error.player-not-found", Component.text(args[0])));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        likeService.sendLike(player, target, reason);
        return true;
    }

    private void handleRecent(Player player) {
        List<LikesBroadcast> recent = recentService.getRecent(5);

        if (recent.isEmpty()) {
            player.sendMessage(messageFactory.info("likes.recent.empty"));
            return;
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
            int count = 0;
            boolean alreadyReacted = false;
            try {
                count = eventRepository.countByBroadcastId(broadcast.broadcastId());
                alreadyReacted = eventRepository.exists(broadcast.broadcastId(), player.getUniqueId());
            } catch (SQLException e) {
                log.log(Level.WARNING, "Failed to get reaction count for broadcastId: " + broadcast.broadcastId(), e);
            }
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
            List.of("boost", "recent").stream()
                    .filter(s -> s.startsWith(partial))
                    .forEach(suggestions::add);
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
