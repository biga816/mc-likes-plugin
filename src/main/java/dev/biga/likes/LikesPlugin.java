package dev.biga.likes;

import dev.biga.likes.book.LikeBookService;
import dev.biga.likes.command.LikeCommand;
import dev.biga.likes.listener.ChatLikeListener;
import dev.biga.likes.database.FeedItemRepository;
import dev.biga.likes.database.ItemStatsRepository;
import dev.biga.likes.database.DailyLimitRepository;
import dev.biga.likes.database.DatabaseManager;
import dev.biga.likes.database.DatabaseWriteExecutor;
import dev.biga.likes.database.ReactionRepository;
import dev.biga.likes.database.PlayerStatsRepository;
import dev.biga.likes.service.CooldownService;
import dev.biga.likes.service.ChatLikeEligibilityService;
import dev.biga.likes.service.LikeEffectService;
import dev.biga.likes.service.LikeService;
import dev.biga.likes.service.RecentService;
import dev.biga.likes.service.PendingChatService;
import dev.biga.likes.util.I18nService;
import dev.biga.likes.util.MessageFactory;
import dev.biga.likes.util.DisplayCodeGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Entry point for the Likes plugin.
 * Initializes i18n, the database, wires dependencies, and registers commands in
 * onEnable(). Closes the database connection and unregisters translations in
 * onDisable().
 */
public class LikesPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private DatabaseWriteExecutor writeExecutor;
    private I18nService i18nService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        // 1. Read server-id from config
        String serverId = getConfig().getString("server-id", "default");
        getLogger().info("Server ID: " + serverId);

        // 2. Initialize i18n translations
        i18nService = new I18nService();
        i18nService.initialize(getClass().getClassLoader());

        // 3. Create data folder and initialize database connection
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

        // 4. Initialize the single-writer executor
        writeExecutor = new DatabaseWriteExecutor();

        // 5. Initialize repositories
        FeedItemRepository itemRepo = new FeedItemRepository(databaseManager);
        ReactionRepository reactionRepo = new ReactionRepository(databaseManager);
        DailyLimitRepository dailyRepo = new DailyLimitRepository(databaseManager);
        PlayerStatsRepository playerStatsRepo = new PlayerStatsRepository(databaseManager);
        ItemStatsRepository itemStatsRepo = new ItemStatsRepository(databaseManager);

        // 6. Initialize services
        CooldownService cooldownService = new CooldownService(getConfig());
        RecentService recentService = new RecentService(getConfig());
        try {
            recentService.loadFromDb(itemRepo, serverId);
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to load recent items on startup", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        DisplayCodeGenerator displayCodeGen = new DisplayCodeGenerator(itemRepo);
        MessageFactory messageFactory = new MessageFactory(getConfig());
        LikeEffectService effectService = new LikeEffectService(getConfig());
        PendingChatService pendingChatService = new PendingChatService(
                getConfig().getInt("chat.pendingBufferSize", 30));

        LikeService likeService = new LikeService(
                itemRepo, reactionRepo, dailyRepo,
                playerStatsRepo, itemStatsRepo,
                databaseManager, writeExecutor,
                displayCodeGen, cooldownService, recentService, pendingChatService, messageFactory,
                effectService, getConfig(), this, serverId);

        // 7. Book UI service
        LikeBookService bookService = new LikeBookService(
                playerStatsRepo, itemStatsRepo, itemRepo, reactionRepo, messageFactory, this, serverId);

        // 8. Register commands
        LikeCommand likeCommand = new LikeCommand(likeService, recentService,
                itemStatsRepo, reactionRepo, messageFactory, bookService);
        getCommand("like").setExecutor(likeCommand);
        getCommand("like").setTabCompleter(likeCommand);

        if (getConfig().getBoolean("chat.enabled", false)) {
            ChatLikeEligibilityService eligibility = new ChatLikeEligibilityService(
                    true, getConfig().getInt("chat.minLength", 4));
            getServer().getPluginManager().registerEvents(new ChatLikeListener(
                    pendingChatService, displayCodeGen, messageFactory, eligibility, serverId,
                    getConfig().getInt("chat.maxStoredLength", 100)), this);
        }

        getLogger().info("Likes enabled!");
    }

    @Override
    public void onDisable() {
        if (writeExecutor != null) {
            writeExecutor.shutdown();
        }
        if (i18nService != null) {
            i18nService.close();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("Likes disabled!");
    }
}
