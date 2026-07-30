package com.extremelyhelpful.devtools.rename;

import com.extremelyhelpful.devtools.ExtremelyHelpfulDeveloperTools;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RenameCommand implements CommandExecutor, TabCompleter {
    private final ExtremelyHelpfulDeveloperTools plugin;

    public RenameCommand(ExtremelyHelpfulDeveloperTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.colorize("&cThis command can only be executed by a player in-game."));
            return true;
        }

        if (!player.hasPermission("rename.use")) {
            player.sendMessage(plugin.getRenameManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player, label);
            return true;
        }

        String sub = args[0];

        if (sub.equalsIgnoreCase("reload")) {
            if (!player.hasPermission("rename.reload")) {
                player.sendMessage(plugin.getRenameManager().getMessage("no-permission"));
                return true;
            }
            plugin.getRenameManager().loadConfig();
            player.sendMessage(plugin.getRenameManager().getMessage("reload-success"));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || item.getAmount() <= 0) {
            player.sendMessage(plugin.getRenameManager().getMessage("no-item"));
            return true;
        }

        RenameEntry entry = plugin.getRenameManager().getEntry(sub);
        if (entry == null) {
            String msg = plugin.getRenameManager().getMessage("entry-not-found")
                    .replace("%entry%", sub);
            player.sendMessage(msg);
            return true;
        }

        // Check conditions (Permission & Material)
        if (!entry.matchesPermission(player)) {
            String msg = plugin.getRenameManager().getMessage("condition-failed-permission")
                    .replace("%entry%", entry.getKey());
            player.sendMessage(msg);
            return true;
        }

        if (!entry.matchesMaterial(item.getType())) {
            String msg = plugin.getRenameManager().getMessage("condition-failed-material")
                    .replace("%required_materials%", entry.getFormattedRequiredMaterials());
            player.sendMessage(msg);
            return true;
        }

        // Check Cooldown
        boolean bypassCooldown = player.hasPermission("rename.bypass.cooldown");
        if (!bypassCooldown) {
            long remainingSeconds = plugin.getRenameManager().getRemainingCooldownSeconds(player, entry);
            if (remainingSeconds > 0) {
                String msg = plugin.getRenameManager().getMessage("cooldown")
                        .replace("%seconds%", String.valueOf(remainingSeconds));
                player.sendMessage(msg);
                return true;
            }
        }

        // Check XP Levels
        boolean bypassExp = player.hasPermission("rename.bypass.exp");
        int costExp = plugin.getRenameManager().getEffectiveCostExpLevels(entry);
        if (!bypassExp && costExp > 0) {
            if (player.getLevel() < costExp) {
                String msg = plugin.getRenameManager().getMessage("not-enough-exp")
                        .replace("%levels%", String.valueOf(costExp));
                player.sendMessage(msg);
                return true;
            }
        }

        // Apply rename
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(entry.getComponent());
            item.setItemMeta(meta);
            player.getInventory().setItemInMainHand(item);

            // Deduct XP levels if configured
            if (!bypassExp && costExp > 0 && plugin.getRenameManager().isConsumeExpLevels()) {
                player.setLevel(player.getLevel() - costExp);
            }

            // Record Cooldown
            if (!bypassCooldown) {
                plugin.getRenameManager().setCooldown(player, entry);
            }

            String msg = plugin.getRenameManager().getMessage("rename-success")
                    .replace("%entry%", entry.getKey());
            player.sendMessage(msg);
        }

        return true;
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(plugin.colorize("&8[&bItemRename&8] &eItem Rename Commands:"));
        player.sendMessage(plugin.colorize("&b/" + label + " <entry> &7- Rename your held item to a predefined name entry."));
        if (player.hasPermission("rename.reload")) {
            player.sendMessage(plugin.colorize("&b/" + label + " reload &7- Reload the renames.yml configuration."));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase(Locale.ROOT);

            for (RenameEntry entry : plugin.getRenameManager().getEntries().values()) {
                String key = entry.getKey();
                if (key.toLowerCase(Locale.ROOT).startsWith(input)) {
                    completions.add(key);
                }
            }

            if (sender.hasPermission("rename.reload") && "reload".startsWith(input)) {
                completions.add("reload");
            }

            return completions;
        }
        return Collections.emptyList();
    }
}
