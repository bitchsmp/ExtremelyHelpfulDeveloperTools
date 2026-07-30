package com.extremelyhelpful.devtools;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DPSCommand implements CommandExecutor, TabCompleter {
    private final ExtremelyHelpfulDeveloperTools plugin;

    public DPSCommand(ExtremelyHelpfulDeveloperTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.colorize("&cThis command can only be executed by a player in-game."));
            return true;
        }

        if (!player.hasPermission("dpstracker.use")) {
            sendConfigMessage(player, "messages.no-permission");
            return true;
        }

        String sub = (args.length > 0) ? args[0].toLowerCase(Locale.ROOT) : "track";

        switch (sub) {
            case "track":
                handleTrack(player);
                break;
            case "stop":
                handleStop(player);
                break;
            case "status":
                handleStatus(player);
                break;
            case "reload":
                handleReload(player);
                break;
            case "help":
            default:
                sendHelp(player, label);
                break;
        }

        return true;
    }

    private void handleTrack(Player player) {
        TrackingSession existing = plugin.getTrackingManager().getSessionByPlayer(player);
        if (existing != null) {
            String msg = plugin.getConfigString("messages.already-tracking")
                    .replace("%entity%", existing.getEntityName());
            player.sendMessage(plugin.colorize(plugin.getConfigString("messages.prefix") + msg));
            return;
        }

        int maxDistance = plugin.getConfig().getInt("max-tracking-distance", 50);
        Entity target = player.getTargetEntity(maxDistance);

        if (!(target instanceof LivingEntity livingTarget) || target.equals(player)) {
            String msg = plugin.getConfigString("messages.no-target")
                    .replace("%distance%", String.valueOf(maxDistance));
            player.sendMessage(plugin.colorize(plugin.getConfigString("messages.prefix") + msg));
            return;
        }

        TrackingSession session = plugin.getTrackingManager().startTracking(player, livingTarget);

        String msg = plugin.getConfigString("messages.track-started")
                .replace("%entity%", session.getEntityName())
                .replace("%uuid%", session.getEntityUuid().toString())
                .replace("%health%", session.getFormattedInitialHealth());

        player.sendMessage(plugin.colorize(plugin.getConfigString("messages.prefix") + msg));
    }

    private void handleStop(Player player) {
        TrackingSession stopped = plugin.getTrackingManager().stopTracking(player);
        if (stopped == null) {
            sendConfigMessage(player, "messages.not-tracking");
            return;
        }

        String msg = plugin.getConfigString("messages.track-stopped")
                .replace("%entity%", stopped.getEntityName());
        player.sendMessage(plugin.colorize(plugin.getConfigString("messages.prefix") + msg));
    }

    private void handleStatus(Player player) {
        TrackingSession session = plugin.getTrackingManager().getSessionByPlayer(player);
        if (session == null) {
            sendConfigMessage(player, "messages.not-tracking");
            return;
        }

        double currentHealth = session.getEntity().isValid() ? session.getEntity().getHealth() : 0.0;
        String formattedCurrentHp = TrackingSession.format(currentHealth);

        String msg = plugin.getConfigString("messages.status")
                .replace("%entity%", session.getEntityName())
                .replace("%initial_health%", session.getFormattedInitialHealth())
                .replace("%current_health%", formattedCurrentHp)
                .replace("%hits%", String.valueOf(session.getHitRecords().size()));

        player.sendMessage(plugin.colorize(plugin.getConfigString("messages.prefix") + msg));
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("dpstracker.reload")) {
            sendConfigMessage(player, "messages.no-permission");
            return;
        }

        plugin.reloadPluginConfig();
        sendConfigMessage(player, "messages.reload-success");
    }

    private void sendHelp(Player player, String label) {
        String p = plugin.getConfigString("messages.prefix");
        player.sendMessage(plugin.colorize(p + "&eExtremely Helpful Dev Tools - DPS Tracker Commands:"));
        player.sendMessage(plugin.colorize("&b/" + label + " track &7- Track the target entity you are looking at"));
        player.sendMessage(plugin.colorize("&b/" + label + " stop &7- Stop tracking your current target"));
        player.sendMessage(plugin.colorize("&b/" + label + " status &7- View status of tracked target"));
        player.sendMessage(plugin.colorize("&b/" + label + " reload &7- Reload configuration"));
    }

    private void sendConfigMessage(Player player, String key) {
        String msg = plugin.getConfigString(key);
        player.sendMessage(plugin.colorize(plugin.getConfigString("messages.prefix") + msg));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> options = Arrays.asList("track", "stop", "status", "reload", "help");
            String input = args[0].toLowerCase(Locale.ROOT);
            for (String opt : options) {
                if (opt.startsWith(input)) {
                    completions.add(opt);
                }
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
