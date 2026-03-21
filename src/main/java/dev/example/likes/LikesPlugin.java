package dev.example.likes;

import dev.example.likes.command.LikeBoostCommand;
import dev.example.likes.command.LikeCommand;
import dev.example.likes.command.LikesCommand;
import dev.example.likes.database.BroadcastRepository;
import dev.example.likes.database.DailyLimitRepository;
import dev.example.likes.database.DatabaseManager;
import dev.example.likes.database.EventRepository;
import dev.example.likes.service.CooldownService;
import dev.example.likes.service.LikeService;
import dev.example.likes.service.RecentService;
import dev.example.likes.util.MessageFactory;
import dev.example.likes.util.ShortIdGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Entry point for the Likes plugin.
 * Initializes the database, wires dependencies, and registers commands in onEnable().
 * Closes the database connection in onDisable().
 */
public class LikesPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // 1. Create data folder and initialize database connection
        getDataFolder().mkdirs();
        try {
            databaseManager = new DatabaseManager(getDataFolder());
            databaseManager.initialize();
        } catch (ClassNotFoundException e) {
            getLogger().log(Level.SEVERE, "SQLite JDBC driver not found", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to initialize the database", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Initialize repositories
        BroadcastRepository broadcastRepo = new BroadcastRepository(databaseManager);
        EventRepository eventRepo = new EventRepository(databaseManager);
        DailyLimitRepository dailyRepo = new DailyLimitRepository(databaseManager);

        // 3. Initialize services
        CooldownService cooldownService = new CooldownService(getConfig());
        RecentService recentService = new RecentService(getConfig());
        try {
            recentService.loadFromDb(broadcastRepo);
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to load recent broadcasts on startup", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        ShortIdGenerator shortIdGen = new ShortIdGenerator(broadcastRepo);
        MessageFactory messageFactory = new MessageFactory(getConfig());

        LikeService likeService = new LikeService(
            broadcastRepo, eventRepo, dailyRepo,
            shortIdGen, cooldownService, recentService, messageFactory,
            getConfig(), this
        );

        // 4. Register commands
        LikeCommand likeCommand = new LikeCommand(likeService, messageFactory);
        getCommand("like").setExecutor(likeCommand);
        getCommand("like").setTabCompleter(likeCommand);
        getCommand("likeboost").setExecutor(new LikeBoostCommand(likeService, messageFactory));
        getCommand("likes").setExecutor(new LikesCommand(recentService, messageFactory));

        getLogger().info("Likes enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("Likes disabled!");
    }
}
