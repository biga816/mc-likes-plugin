package dev.example.likes.command;

import dev.example.likes.service.LikeService;
import dev.example.likes.util.MessageFactory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handler for the /likeboost [shortId] command.
 * If a shortId is given, reacts to the specified broadcast.
 * If omitted, reacts to the last seen broadcast.
 * Can only be executed by players.
 */
public class LikeBoostCommand implements CommandExecutor {

    private final LikeService likeService;

    /**
     * Constructs a LikeBoostCommand.
     *
     * @param likeService    service handling like sending and reactions
     * @param messageFactory message factory (reserved for future use, currently unused)
     */
    @SuppressWarnings("unused")
    public LikeBoostCommand(LikeService likeService, MessageFactory messageFactory) {
        this.likeService = likeService;
    }

    /**
     * Handles the /likeboost command.
     * <ol>
     *   <li>Rejects execution from the console</li>
     *   <li>If a shortId is provided, calls {@link LikeService#react(Player, String)}</li>
     *   <li>If shortId is omitted, calls {@link LikeService#react(Player)}</li>
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
        // Reject console execution
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ使用できます");
            return true;
        }

        if (args.length >= 1) {
            // React to the broadcast with the specified shortId
            likeService.react(player, args[0]);
        } else {
            // React to the last seen broadcast
            likeService.react(player);
        }
        return true;
    }
}
