package dev.example.likes.command;

import dev.example.likes.model.LikesBroadcast;
import dev.example.likes.service.RecentService;
import dev.example.likes.util.MessageFactory;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handler for the /likes recent command.
 * Displays the 5 most recent likes, each row with a React button.
 * Can only be executed by players.
 */
public class LikesCommand implements CommandExecutor {

    private final RecentService recentService;
    private final MessageFactory messageFactory;

    /**
     * Constructs a LikesCommand.
     *
     * @param recentService  service managing recent broadcasts
     * @param messageFactory message factory
     */
    public LikesCommand(RecentService recentService, MessageFactory messageFactory) {
        this.recentService = recentService;
        this.messageFactory = messageFactory;
    }

    /**
     * Handles the /likes command.
     * <ol>
     *   <li>Rejects execution from the console</li>
     *   <li>Shows usage if the sub-command is not "recent"</li>
     *   <li>Fetches the 5 most recent entries via {@link RecentService#getRecent(int)}</li>
     *   <li>Notifies the player if there are no recent entries</li>
     *   <li>Formats and displays each broadcast using {@link MessageFactory#buildBroadcastMessage}</li>
     *   <li>Updates {@link RecentService#updateLastSeen} with the latest broadcastId after display</li>
     * </ol>
     *
     * @param sender  command sender
     * @param command command object
     * @param label   command label
     * @param args    command arguments
     * @return always {@code true}
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Reject console execution
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageFactory.error("likes.error.console-only"));
            return true;
        }

        // 2. Validate sub-command
        if (args.length < 1 || !args[0].equalsIgnoreCase("recent")) {
            player.sendMessage(messageFactory.info("likes.command.likes.usage"));
            return true;
        }

        // 3. Fetch the 5 most recent broadcasts
        List<LikesBroadcast> recent = recentService.getRecent(5);

        // 4. Check if the list is empty
        if (recent.isEmpty()) {
            player.sendMessage(messageFactory.info("likes.recent.empty"));
            return true;
        }

        // 5. Display title then each broadcast
        player.sendMessage(messageFactory.info("likes.recent.title"));
        for (LikesBroadcast broadcast : recent) {
            String senderName = resolveName(broadcast.sourceSenderUuid());
            String targetName = resolveName(broadcast.targetUuid());
            Component msg = messageFactory.buildBroadcastMessage(broadcast, senderName, targetName);
            player.sendMessage(msg);
        }

        // 6. Update lastSeen with the most recent broadcastId (first element is newest)
        recentService.updateLastSeen(player.getUniqueId(), recent.get(0).broadcastId());

        return true;
    }

    /**
     * Resolves a player display name from a UUID.
     * Prefers {@link Bukkit#getPlayer(java.util.UUID)} for online players,
     * falling back to {@link Bukkit#getOfflinePlayer(java.util.UUID)} for offline players.
     *
     * @param uuid the UUID to resolve
     * @return the player name, or the UUID string if it cannot be resolved
     */
    private String resolveName(java.util.UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString();
    }
}
