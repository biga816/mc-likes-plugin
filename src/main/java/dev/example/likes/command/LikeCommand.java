package dev.example.likes.command;

import dev.example.likes.service.LikeService;
import dev.example.likes.util.MessageFactory;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Handler for the /like &lt;player&gt; &lt;reason...&gt; command.
 * Can only be executed by players.
 * Tab completion returns online player names as suggestions (excluding the
 * sender).
 */
public class LikeCommand implements CommandExecutor, TabCompleter {

    private final LikeService likeService;
    private final MessageFactory messageFactory;

    /**
     * Constructs a LikeCommand.
     *
     * @param likeService    service handling like sending and reactions
     * @param messageFactory message factory
     */
    public LikeCommand(LikeService likeService, MessageFactory messageFactory) {
        this.likeService = likeService;
        this.messageFactory = messageFactory;
    }

    /**
     * Handles the /like command.
     * <ol>
     * <li>Rejects execution from the console</li>
     * <li>Shows usage if arguments are insufficient</li>
     * <li>Shows an error if the target player is not online</li>
     * <li>Joins arguments into a reason string and calls
     * {@link LikeService#sendLike}</li>
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
        // 2. Validate arguments
        if (args.length < 2) {
            player.sendMessage(messageFactory.info("likes.command.like.usage"));
            return true;
        }
        // 3. Resolve target player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(messageFactory.error("likes.error.player-not-found", Component.text(args[0])));
            return true;
        }
        // 4. Join remaining arguments into the reason string
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        // 5. Delegate to service
        likeService.sendLike(player, target, reason);
        return true;
    }

    /**
     * Handles tab completion for the /like command.
     * For the first argument, returns online player names (excluding the sender) as
     * suggestions.
     *
     * @param sender  command sender
     * @param command command object
     * @param label   command label
     * @param args    current arguments
     * @return list of completion suggestions
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            // Complete with online player names, excluding the sender
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(sender))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .toList();
        }
        return List.of();
    }
}
