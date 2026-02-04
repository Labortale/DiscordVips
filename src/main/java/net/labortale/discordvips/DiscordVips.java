package net.labortale.discordvips;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import net.labortale.discordvips.data.link.LinkDatabase;
import net.labortale.discordvips.data.pendinglink.PendingLinkDatabase;
import net.labortale.discordvips.data.pendinglink.InMemoryPendingLinkDatabase;
import net.labortale.discordvips.data.link.OrmLiteLinkDatabase;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DiscordVips extends JavaPlugin {

    Path dataDirectory;

    ScheduledExecutorService purgeCodeScheduler;

    public DiscordVips(@NotNull JavaPluginInit init) {
        super(init);
        dataDirectory = init.getDataDirectory();
        instance = this;
        configManager = this.withConfig("config", PluginConfig.CODEC);
        purgeCodeScheduler = Executors.newSingleThreadScheduledExecutor();
        purgeCodeScheduler.scheduleAtFixedRate(() -> DiscordVips.getPendingLinkDb().purgeExpired(System.currentTimeMillis()), 1, 10, TimeUnit.MINUTES);
    }

    private Config<PluginConfig> configManager;
    private static PluginConfig CONFIG;
    public static PluginConfig getConfig(){
        return CONFIG;
    }

    private ConnectionSource connSrc;
    private DiscordBot discordBot;

    private static DiscordVips instance;
    public static DiscordVips getInstance() {
        return instance;
    }

    private static PendingLinkDatabase pendingLinkDb;
    public static PendingLinkDatabase getPendingLinkDb() {
        return pendingLinkDb;
    }

    private static LinkDatabase linkDb;
    public static LinkDatabase getLinkDb() {
        return linkDb;
    }
    @Override
    public void setup(){
        configManager.load().join();
        configManager.save().join();
        CONFIG = configManager.get();
        // Registers the configuration with the filename "ExamplePlugin"
        if(CONFIG.getDiscordToken().isEmpty()){
            getLogger().atSevere().log("Cannot start plugin: Discord token missing!");
            return;
        }
        discordBot = new DiscordBot(CONFIG.getDiscordToken());
        discordBot.startFullSyncTask(3600, 10);
        try {
            Path dbPath = dataDirectory.resolve("database.db");
            connSrc = new JdbcConnectionSource("jdbc:sqlite:" + dbPath.toAbsolutePath());
            linkDb = new OrmLiteLinkDatabase(connSrc);
            pendingLinkDb = new InMemoryPendingLinkDatabase();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        discordBot.shutdown();
        connSrc.closeQuietly();
        try {
            if (!purgeCodeScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                purgeCodeScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            purgeCodeScheduler.shutdownNow();
            DiscordVips.getInstance().getLogger().atWarning().log("Cannot cleanly shutdown purgeCodeScheduler! ", e);
        }
    }

}
