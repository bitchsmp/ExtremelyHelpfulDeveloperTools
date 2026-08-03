package com.extremelyhelpful.devtools.spelunking;

import com.extremelyhelpful.devtools.ExtremelyHelpfulDeveloperTools;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpelunkingCommand implements CommandExecutor, TabCompleter {
    private final ExtremelyHelpfulDeveloperTools plugin;
    private final SpelunkingManager manager;
    private static final Pattern TIME_PATTERN = Pattern.compile("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?", Pattern.CASE_INSENSITIVE);

    public SpelunkingCommand(ExtremelyHelpfulDeveloperTools plugin, SpelunkingManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spelunking.use")) {
            String msg = plugin.getConfigString("messages.no-permission");
            sender.sendMessage(plugin.colorize(msg.isEmpty() ? "&cYou do not have permission to execute this command." : msg));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.colorize("&cUsage: /spelunking <player> <duration> | /spelunking stop <player> | /spelunking reload"));
            return true;
        }

        // Subcommand: reload
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("spelunking.reload")) {
                String msg = plugin.getConfigString("messages.no-permission");
                sender.sendMessage(plugin.colorize(msg.isEmpty() ? "&cYou do not have permission to reload." : msg));
                return true;
            }
            plugin.reloadPluginConfig();
            manager.loadConfig();
            String msg = plugin.getConfigString("messages.reload-success");
            sender.sendMessage(plugin.colorize(msg.isEmpty() ? "&aConfiguration reloaded successfully!" : msg));
            return true;
        }

        // Subcommand: stop
        if (args[0].equalsIgnoreCase("stop")) {
            if (args.length < 2) {
                if (sender instanceof Player player) {
                    boolean stopped = manager.stopSession(player);
                    if (stopped) {
                        sender.sendMessage(plugin.colorize("&aSpelunking stopped for &e" + player.getName() + "&a."));
                    } else {
                        sender.sendMessage(plugin.colorize("&cNo active spelunking session found for &e" + player.getName() + "&c."));
                    }
                } else {
                    sender.sendMessage(plugin.colorize("&cUsage: /spelunking stop <player>"));
                }
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                String msg = plugin.getConfigString("messages.player-not-found");
                sender.sendMessage(plugin.colorize(msg.replace("%player%", args[1])));
                return true;
            }

            boolean stopped = manager.stopSession(target);
            if (stopped) {
                sender.sendMessage(plugin.colorize("&aSpelunking stopped for &e" + target.getName() + "&a."));
            } else {
                sender.sendMessage(plugin.colorize("&cNo active spelunking session found for &e" + target.getName() + "&c."));
            }
            return true;
        }

        // Main command: /spelunking <player> <duration>
        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            String msg = plugin.getConfigString("messages.player-not-found");
            sender.sendMessage(plugin.colorize(msg.replace("%player%", args[0])));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /spelunking <player> <duration> (e.g., /spelunking " + targetPlayer.getName() + " 30s)"));
            return true;
        }

        String durationArg = args[1];
        long durationSeconds = parseDurationToSeconds(durationArg);
        if (durationSeconds <= 0) {
            String msg = plugin.getConfigString("messages.invalid-duration");
            sender.sendMessage(plugin.colorize(msg.isEmpty() ? "&cInvalid duration format. Examples: 30, 30s, 1m, 2m30s." : msg));
            return true;
        }

        long durationTicks = durationSeconds * 20L;
        int radius = manager.getDefaultRadius();

        int oresFound = manager.startSession(targetPlayer, durationTicks, radius);

        if (oresFound == 0) {
            String msg = plugin.getConfigString("messages.spelunking-no-ores");
            if (!msg.isEmpty()) {
                sender.sendMessage(plugin.colorize(msg.replace("%radius%", String.valueOf(radius))
                        .replace("%player%", targetPlayer.getName())));
            } else {
                sender.sendMessage(plugin.colorize("&eNo configured ores found within " + radius + " blocks of &a" + targetPlayer.getName() + "&e."));
            }
            return true;
        }

        String formattedDuration = formatDuration(durationSeconds);
        String msg = plugin.getConfigString("messages.spelunking-started");
        if (!msg.isEmpty()) {
            sender.sendMessage(plugin.colorize(msg.replace("%player%", targetPlayer.getName())
                    .replace("%count%", String.valueOf(oresFound))
                    .replace("%radius%", String.valueOf(radius))
                    .replace("%duration%", formattedDuration)));
        } else {
            sender.sendMessage(plugin.colorize("&aSpelunking activated for &e" + targetPlayer.getName() + "&a! Found &b" + oresFound + " &aores in " + radius + " block radius for &e" + formattedDuration + "&a."));
        }

        if (!sender.equals(targetPlayer)) {
            targetPlayer.sendMessage(plugin.colorize("&aSpelunking activated by &e" + sender.getName() + "&a! Found &b" + oresFound + " &aores for &e" + formattedDuration + "&a."));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("spelunking.use")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("stop");
            if (sender.hasPermission("spelunking.reload")) {
                completions.add("reload");
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("stop")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("reload")) {
                return Collections.emptyList();
            }
            List<String> durations = Arrays.asList("10s", "30s", "1m", "2m", "5m", "10m");
            return durations.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public static long parseDurationToSeconds(String input) {
        if (input == null || input.isBlank()) return -1;

        // Pure digits fallback
        try {
            long val = Long.parseLong(input);
            if (val > 0) return val;
        } catch (NumberFormatException ignored) {
        }

        Matcher matcher = TIME_PATTERN.matcher(input);
        if (matcher.matches()) {
            long hours = matcher.group(1) != null ? Long.parseLong(matcher.group(1)) : 0;
            long minutes = matcher.group(2) != null ? Long.parseLong(matcher.group(2)) : 0;
            long seconds = matcher.group(3) != null ? Long.parseLong(matcher.group(3)) : 0;
            long total = hours * 3600 + minutes * 60 + seconds;
            return total > 0 ? total : -1;
        }

        return -1;
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        long remSeconds = seconds % 60;
        if (remSeconds == 0) {
            return minutes + "m";
        }
        return minutes + "m " + remSeconds + "s";
    }
}
