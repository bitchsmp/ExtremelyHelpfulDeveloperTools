package com.extremelyhelpful.devtools.rename;

import com.extremelyhelpful.devtools.ExtremelyHelpfulDeveloperTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class RenameManager {
    private final ExtremelyHelpfulDeveloperTools plugin;
    private final File configFile;
    private FileConfiguration config;

    private int globalCooldownSeconds = 0;
    private int globalCostExpLevels = 0;
    private boolean consumeExpLevels = true;

    private final Map<String, RenameEntry> entries = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public RenameManager(ExtremelyHelpfulDeveloperTools plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "renames.yml");
        loadConfig();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("renames.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Merge defaults if missing keys
        InputStream defaultStream = plugin.getResource("renames.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }

        // Load global settings
        ConfigurationSection settingsSec = config.getConfigurationSection("settings");
        if (settingsSec != null) {
            globalCooldownSeconds = settingsSec.getInt("cooldown-seconds", 0);
            globalCostExpLevels = settingsSec.getInt("cost-exp-levels", 0);
            consumeExpLevels = settingsSec.getBoolean("consume-exp-levels", true);
        } else {
            globalCooldownSeconds = 0;
            globalCostExpLevels = 0;
            consumeExpLevels = true;
        }

        entries.clear();
        ConfigurationSection entriesSection = config.getConfigurationSection("entries");
        if (entriesSection != null) {
            for (String key : entriesSection.getKeys(false)) {
                ConfigurationSection entrySec = entriesSection.getConfigurationSection(key);
                if (entrySec == null) continue;

                String rawName = entrySec.getString("name");
                if (rawName == null) continue;

                Component component = parseComponent(rawName);

                Set<Material> materials = new HashSet<>();
                ConfigurationSection condSec = entrySec.getConfigurationSection("conditions");
                String permission = null;
                Integer cooldownSeconds = null;
                Integer costExpLevels = null;

                if (condSec != null) {
                    permission = condSec.getString("permission");
                    if (condSec.contains("cooldown-seconds")) {
                        cooldownSeconds = condSec.getInt("cooldown-seconds");
                    }
                    if (condSec.contains("cost-exp-levels")) {
                        costExpLevels = condSec.getInt("cost-exp-levels");
                    }

                    List<String> matStrings = new ArrayList<>();
                    if (condSec.isList("materials")) {
                        matStrings.addAll(condSec.getStringList("materials"));
                    } else if (condSec.isString("material")) {
                        matStrings.add(condSec.getString("material"));
                    } else if (condSec.isString("materials")) {
                        matStrings.add(condSec.getString("materials"));
                    }

                    for (String matStr : matStrings) {
                        Material mat = Material.matchMaterial(matStr.trim());
                        if (mat != null) {
                            materials.add(mat);
                        } else {
                            plugin.getLogger().warning("Invalid material '" + matStr + "' in rename entry: " + key);
                        }
                    }
                }

                entries.put(key.toLowerCase(Locale.ROOT), new RenameEntry(key, rawName, component, materials, permission, cooldownSeconds, costExpLevels));
            }
        }
    }

    public Map<String, RenameEntry> getEntries() {
        return Collections.unmodifiableMap(entries);
    }

    public RenameEntry getEntry(String key) {
        if (key == null) return null;
        return entries.get(key.toLowerCase(Locale.ROOT));
    }

    public int getEffectiveCooldownSeconds(RenameEntry entry) {
        if (entry != null && entry.getCooldownSeconds() != null) {
            return entry.getCooldownSeconds();
        }
        return globalCooldownSeconds;
    }

    public int getEffectiveCostExpLevels(RenameEntry entry) {
        if (entry != null && entry.getCostExpLevels() != null) {
            return entry.getCostExpLevels();
        }
        return globalCostExpLevels;
    }

    public boolean isConsumeExpLevels() {
        return consumeExpLevels;
    }

    public long getRemainingCooldownSeconds(Player player, RenameEntry entry) {
        int cdSec = getEffectiveCooldownSeconds(entry);
        if (cdSec <= 0) return 0;

        Map<String, Long> playerCds = cooldowns.get(player.getUniqueId());
        if (playerCds == null) return 0;

        Long expiry = playerCds.get(entry.getKey().toLowerCase(Locale.ROOT));
        if (expiry == null) return 0;

        long diff = expiry - System.currentTimeMillis();
        if (diff <= 0) {
            playerCds.remove(entry.getKey().toLowerCase(Locale.ROOT));
            return 0;
        }

        return (diff + 999) / 1000;
    }

    public void setCooldown(Player player, RenameEntry entry) {
        int cdSec = getEffectiveCooldownSeconds(entry);
        if (cdSec <= 0) return;

        long expiry = System.currentTimeMillis() + (cdSec * 1000L);
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(entry.getKey().toLowerCase(Locale.ROOT), expiry);
    }

    public void clearCooldowns() {
        cooldowns.clear();
    }

    public String getMessage(String path) {
        String prefix = config.getString("messages.prefix", "&8[&bItemRename&8] &r");
        String msg = config.getString("messages." + path, "");
        if (msg == null || msg.isEmpty()) {
            return "";
        }
        return colorize(prefix + msg);
    }

    public String getRawMessage(String path) {
        return config.getString("messages." + path, "");
    }

    private String colorize(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public Component parseComponent(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        String trimmed = input.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                String sanitizedJson = sanitizeJson(trimmed);
                return GsonComponentSerializer.gson().deserialize(sanitizedJson);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to parse JSON text component: " + input + ". Falling back to legacy parsing.", e);
            }
        }

        try {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(trimmed);
        } catch (Exception e) {
            return Component.text(trimmed);
        }
    }

    public static String sanitizeJson(String input) {
        if (input == null) return "";
        String sanitized = input.trim();
        // Replace SNBT booleans 0b and 1b
        sanitized = sanitized.replaceAll(":\\s*0b\\b", ": false");
        sanitized = sanitized.replaceAll(":\\s*1b\\b", ": true");
        // Quote unquoted object keys (e.g. {italic: -> {"italic":)
        sanitized = sanitized.replaceAll("([{,])\\s*([a-zA-Z0-9_-]+)\\s*:", "$1\"$2\":");
        return sanitized;
    }
}
