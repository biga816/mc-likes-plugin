package dev.example.likes;

import dev.example.likes.command.LikeCommand;
import dev.example.likes.database.BroadcastRepository;
import dev.example.likes.database.DailyLimitRepository;
import dev.example.likes.database.DatabaseManager;
import dev.example.likes.database.EventRepository;
import dev.example.likes.service.CooldownService;
import dev.example.likes.service.LikeService;
import dev.example.likes.service.RecentService;
import dev.example.likes.util.I18nService;
import dev.example.likes.util.MessageFactory;
import dev.example.likes.util.DisplayCodeGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Entry point for the Likes plugin.
 * Initializes i18n, the database, wires dependencies, and registers commands in
 * onEnable().
 * Closes the database connection and unregisters translations in onDisable().
 */
public class LikesPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private I18nService i18nService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // 1. Initialize i18n translations
        i18nService = new I18nService();
        i18nService.initialize(getClass().getClassLoader());

        // 2. Create data folder and initialize database connection
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

        // 3. Initialize repositories
        BroadcastRepository broadcastRepo = new BroadcastRepository(databaseManager);
        EventRepository eventRepo = new EventRepository(databaseManager);
        DailyLimitRepository dailyRepo = new DailyLimitRepository(databaseManager);

        // 4. Initialize services
        CooldownService cooldownService = new CooldownService(getConfig());
        RecentService recentService = new RecentService(getConfig());
        try {
            recentService.loadFromDb(broadcastRepo);
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to load recent broadcasts on startup", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        DisplayCodeGenerator displayCodeGen = new DisplayCodeGenerator(broadcastRepo);
        MessageFactory messageFactory = new MessageFactory(getConfig());

        LikeService likeService = new LikeService(
                broadcastRepo, eventRepo, dailyRepo,
                displayCodeGen, cooldownService, recentService, messageFactory,
                getConfig(), this);

        // 5. Register commands
        LikeCommand likeCommand = new LikeCommand(likeService, recentService, eventRepo, messageFactory);
        getCommand("like").setExecutor(likeCommand);
        getCommand("like").setTabCompleter(likeCommand);

        getLogger().info("Likes enabled!");
    }

    @Override
    public void onDisable() {
        if (i18nService != null) {
            i18nService.close();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("Likes disabled!");
    }
}
