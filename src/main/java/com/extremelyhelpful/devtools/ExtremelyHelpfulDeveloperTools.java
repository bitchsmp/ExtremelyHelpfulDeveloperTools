package com.extremelyhelpful.devtools;

import com.extremelyhelpful.devtools.endcrystal.EndCrystalListener;
import com.extremelyhelpful.devtools.endcrystal.EndCrystalManager;
import com.extremelyhelpful.devtools.rename.RenameCommand;
import com.extremelyhelpful.devtools.rename.RenameManager;
import com.extremelyhelpful.devtools.spelunking.SpelunkingCommand;
import com.extremelyhelpful.devtools.spelunking.SpelunkingListener;
import com.extremelyhelpful.devtools.spelunking.SpelunkingManager;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class ExtremelyHelpfulDeveloperTools extends JavaPlugin {
    private TrackingManager trackingManager;
    private RenameManager renameManager;
    private SpelunkingManager spelunkingManager;
    private EndCrystalManager endCrystalManager;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().checkForUpdates(false).bStats(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PacketEvents.getAPI().init();

        this.trackingManager = new TrackingManager();
        this.renameManager = new RenameManager(this);
        this.spelunkingManager = new SpelunkingManager(this);
        this.endCrystalManager = new EndCrystalManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new EntityDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new SpelunkingListener(spelunkingManager), this);
        getServer().getPluginManager().registerEvents(new EndCrystalListener(endCrystalManager), this);

        // Register commands
        DPSCommand dpsCommand = new DPSCommand(this);
        PluginCommand command = getCommand("dps");
        if (command != null) {
            command.setExecutor(dpsCommand);
            command.setTabCompleter(dpsCommand);
        }

        RenameCommand renameCmd = new RenameCommand(this);
        PluginCommand renameCommand = getCommand("rename");
        if (renameCommand != null) {
            renameCommand.setExecutor(renameCmd);
            renameCommand.setTabCompleter(renameCmd);
        }

        SpelunkingCommand spelunkingCmd = new SpelunkingCommand(this, spelunkingManager);
        PluginCommand spelunkingCommand = getCommand("spelunking");
        if (spelunkingCommand != null) {
            spelunkingCommand.setExecutor(spelunkingCmd);
            spelunkingCommand.setTabCompleter(spelunkingCmd);
        }

        getLogger().info("ExtremelyHelpfulDeveloperTools Plugin v" + getDescription().getVersion() + " for Minecraft 1.21.11 enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (spelunkingManager != null) {
            spelunkingManager.clearAll();
        }
        if (trackingManager != null) {
            trackingManager.clearAll();
        }
        PacketEvents.getAPI().terminate();
        getLogger().info("ExtremelyHelpfulDeveloperTools Plugin disabled.");
    }

    public TrackingManager getTrackingManager() {
        return trackingManager;
    }

    public RenameManager getRenameManager() {
        return renameManager;
    }

    public SpelunkingManager getSpelunkingManager() {
        return spelunkingManager;
    }

    public EndCrystalManager getEndCrystalManager() {
        return endCrystalManager;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        if (renameManager != null) {
            renameManager.loadConfig();
        }
        if (spelunkingManager != null) {
            spelunkingManager.loadConfig();
        }
        if (endCrystalManager != null) {
            endCrystalManager.loadConfig();
        }
    }

    public String getConfigString(String path) {
        String val = getConfig().getString(path);
        return val != null ? val : "";
    }

    public String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
