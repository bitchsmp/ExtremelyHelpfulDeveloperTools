package com.extremelyhelpful.devtools.endcrystal;

import com.extremelyhelpful.devtools.ExtremelyHelpfulDeveloperTools;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class EndCrystalManager {
    private final ExtremelyHelpfulDeveloperTools plugin;
    private String defaultMode = "killed";
    private float explosionPower = 6.0f;
    private boolean setFire = false;
    private boolean breakBlocks = true;
    private final Map<String, String> dimensionModes = new HashMap<>();

    public EndCrystalManager(ExtremelyHelpfulDeveloperTools plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        dimensionModes.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("end-crystal");
        if (section == null) {
            defaultMode = "killed";
            explosionPower = 6.0f;
            setFire = false;
            breakBlocks = true;
            return;
        }

        defaultMode = section.getString("default-mode", "killed").toLowerCase(Locale.ROOT);
        explosionPower = (float) section.getDouble("explosion-power", 6.0);
        setFire = section.getBoolean("set-fire", false);
        breakBlocks = section.getBoolean("break-blocks", true);

        ConfigurationSection dimSection = section.getConfigurationSection("dimensions");
        if (dimSection != null) {
            for (String key : dimSection.getKeys(false)) {
                String val = dimSection.getString(key, defaultMode);
                if (val != null) {
                    dimensionModes.put(key.toLowerCase(Locale.ROOT), val.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    public boolean shouldExplodeOnExplosionDamage(World world) {
        if (world == null) {
            return defaultMode.equals("explode");
        }

        String mode = getModeForWorld(world);
        return mode.equalsIgnoreCase("explode");
    }

    public String getModeForWorld(World world) {
        if (world == null) {
            return defaultMode;
        }

        // 1. Check exact world name (e.g., "world", "world_nether", "world_the_end")
        String worldName = world.getName().toLowerCase(Locale.ROOT);
        if (dimensionModes.containsKey(worldName)) {
            return dimensionModes.get(worldName);
        }

        // 2. Check world environment name (e.g., "normal", "nether", "the_end")
        String envName = world.getEnvironment().name().toLowerCase(Locale.ROOT);
        if (dimensionModes.containsKey(envName)) {
            return dimensionModes.get(envName);
        }

        // 3. Check NamespacedKey full string (e.g., "minecraft:overworld")
        String keyStr = world.getKey().toString().toLowerCase(Locale.ROOT);
        if (dimensionModes.containsKey(keyStr)) {
            return dimensionModes.get(keyStr);
        }

        // 4. Check NamespacedKey key part (e.g., "overworld")
        String keyPart = world.getKey().getKey().toLowerCase(Locale.ROOT);
        if (dimensionModes.containsKey(keyPart)) {
            return dimensionModes.get(keyPart);
        }

        return defaultMode;
    }

    public float getExplosionPower() {
        return explosionPower;
    }

    public boolean isSetFire() {
        return setFire;
    }

    public boolean isBreakBlocks() {
        return breakBlocks;
    }
}
