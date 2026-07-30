package com.extremelyhelpful.devtools;

import com.extremelyhelpful.devtools.rename.RenameCommand;
import com.extremelyhelpful.devtools.rename.RenameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class ExtremelyHelpfulDeveloperTools extends JavaPlugin {
    private TrackingManager trackingManager;
    private RenameManager renameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.trackingManager = new TrackingManager();
        this.renameManager = new RenameManager(this);

        // Register listener
        getServer().getPluginManager().registerEvents(new EntityDamageListener(this), this);

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

        getLogger().info("ExtremelyHelpfulDeveloperTools Plugin v" + getDescription().getVersion() + " for Minecraft 1.21.11 enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (trackingManager != null) {
            trackingManager.clearAll();
        }
        getLogger().info("ExtremelyHelpfulDeveloperTools Plugin disabled.");
    }

    public TrackingManager getTrackingManager() {
        return trackingManager;
    }

    public RenameManager getRenameManager() {
        return renameManager;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        if (renameManager != null) {
            renameManager.loadConfig();
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
