package com.extremelyhelpful.devtools.spelunking;

import com.extremelyhelpful.devtools.ExtremelyHelpfulDeveloperTools;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SpelunkingManager {
    private final ExtremelyHelpfulDeveloperTools plugin;
    private final Map<UUID, SpelunkingSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<Material, String> oreGlowColors = new HashMap<>();
    private final AtomicInteger entityIdCounter = new AtomicInteger(-2000000);

    private int defaultRadius = 16;
    private int maxRadius = 64;

    public SpelunkingManager(ExtremelyHelpfulDeveloperTools plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        oreGlowColors.clear();
        defaultRadius = plugin.getConfig().getInt("spelunking.radius", 16);
        maxRadius = plugin.getConfig().getInt("spelunking.max-radius", 64);

        ConfigurationSection oresSection = plugin.getConfig().getConfigurationSection("spelunking.ores");
        if (oresSection != null) {
            for (String key : oresSection.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase(Locale.ROOT));
                    String colorName = oresSection.getString(key, "WHITE");
                    oreGlowColors.put(mat, colorName);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[Spelunking] Invalid material in config: " + key);
                }
            }
        } else {
            addDefaultOres();
        }
    }

    private void addDefaultOres() {
        oreGlowColors.put(Material.DIAMOND_ORE, "AQUA");
        oreGlowColors.put(Material.DEEPSLATE_DIAMOND_ORE, "AQUA");
        oreGlowColors.put(Material.EMERALD_ORE, "GREEN");
        oreGlowColors.put(Material.DEEPSLATE_EMERALD_ORE, "GREEN");
        oreGlowColors.put(Material.GOLD_ORE, "GOLD");
        oreGlowColors.put(Material.DEEPSLATE_GOLD_ORE, "GOLD");
        oreGlowColors.put(Material.RAW_GOLD_BLOCK, "GOLD");
        oreGlowColors.put(Material.IRON_ORE, "WHITE");
        oreGlowColors.put(Material.DEEPSLATE_IRON_ORE, "WHITE");
        oreGlowColors.put(Material.ANCIENT_DEBRIS, "RED");
        oreGlowColors.put(Material.LAPIS_ORE, "BLUE");
        oreGlowColors.put(Material.DEEPSLATE_LAPIS_ORE, "BLUE");
        oreGlowColors.put(Material.NETHER_GOLD_ORE, "GOLD");
        oreGlowColors.put(Material.NETHER_QUARTZ_ORE, "WHITE");
        oreGlowColors.put(Material.COAL_ORE, "DARK_GRAY");
        oreGlowColors.put(Material.DEEPSLATE_COAL_ORE, "DARK_GRAY");
        oreGlowColors.put(Material.COPPER_ORE, "LIGHT_PURPLE");
        oreGlowColors.put(Material.DEEPSLATE_COPPER_ORE, "LIGHT_PURPLE");
    }

    public int startSession(Player player, long durationTicks, int radius) {
        stopSession(player);

        int searchRadius = Math.min(Math.max(1, radius > 0 ? radius : defaultRadius), maxRadius);
        Location center = player.getLocation();

        Map<String, List<String>> colorEntityUuids = new HashMap<>();
        List<Integer> spawnedEntityIds = new ArrayList<>();
        Set<String> teamNames = new HashSet<>();

        int px = center.getBlockX();
        int py = center.getBlockY();
        int pz = center.getBlockZ();

        int oresFound = 0;

        for (int x = px - searchRadius; x <= px + searchRadius; x++) {
            for (int y = Math.max(center.getWorld().getMinHeight(), py - searchRadius); y <= Math.min(center.getWorld().getMaxHeight(), py + searchRadius); y++) {
                for (int z = pz - searchRadius; z <= pz + searchRadius; z++) {
                    double distSq = (x - px) * (x - px) + (y - py) * (y - py) + (z - pz) * (z - pz);
                    if (distSq > searchRadius * searchRadius) continue;

                    Block block = center.getWorld().getBlockAt(x, y, z);
                    Material mat = block.getType();
                    if (oreGlowColors.containsKey(mat)) {
                        String colorName = oreGlowColors.get(mat);
                        int entityId = entityIdCounter.decrementAndGet();
                        UUID entityUuid = UUID.randomUUID();

                        spawnedEntityIds.add(entityId);
                        colorEntityUuids.computeIfAbsent(colorName, k -> new ArrayList<>()).add(entityUuid.toString());

                        // Spawn Shulker packet at block coordinate
                        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                                entityId,
                                Optional.of(entityUuid),
                                EntityTypes.SHULKER,
                                new Vector3d(x + 0.5, y, z + 0.5),
                                0f,
                                0f,
                                0f,
                                0,
                                Optional.empty()
                        );
                        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);

                        // Metadata: Bit 0x20 (Invisible) + 0x40 (Glowing) = 0x60
                        List<EntityData<?>> metadataList = new ArrayList<>();
                        metadataList.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x60));
                        WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(entityId, metadataList);
                        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadataPacket);

                        oresFound++;
                    }
                }
            }
        }

        if (oresFound == 0) {
            return 0;
        }

        // Create Teams for glow colors
        int teamCounter = 0;
        for (Map.Entry<String, List<String>> entry : colorEntityUuids.entrySet()) {
            String colorName = entry.getKey();
            List<String> entityUuidStrings = entry.getValue();

            String teamName = "spk_" + player.getEntityId() + "_" + (teamCounter++);
            teamNames.add(teamName);

            NamedTextColor color = parseNamedTextColor(colorName);
            WrapperPlayServerTeams.ScoreBoardTeamInfo teamInfo = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                    Component.text(teamName),
                    Component.empty(),
                    Component.empty(),
                    WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
                    WrapperPlayServerTeams.CollisionRule.ALWAYS,
                    color,
                    WrapperPlayServerTeams.OptionData.NONE
            );

            WrapperPlayServerTeams createTeamPacket = new WrapperPlayServerTeams(
                    teamName,
                    WrapperPlayServerTeams.TeamMode.CREATE,
                    teamInfo,
                    entityUuidStrings
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, createTeamPacket);
        }

        SpelunkingSession session = new SpelunkingSession(player.getUniqueId(), spawnedEntityIds, teamNames);

        // Schedule expiration task
        session.setExpiryTask(new BukkitRunnable() {
            @Override
            public void run() {
                stopSession(player);
                String msg = plugin.getConfigString("messages.spelunking-stopped");
                if (!msg.isEmpty()) {
                    player.sendMessage(plugin.colorize(msg.replace("%player%", player.getName())));
                }
            }
        }.runTaskLater(plugin, durationTicks));

        activeSessions.put(player.getUniqueId(), session);
        return oresFound;
    }

    public boolean stopSession(Player player) {
        if (player == null) return false;
        SpelunkingSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.cleanup(player);
            return true;
        }
        return false;
    }

    public void clearAll() {
        for (Map.Entry<UUID, SpelunkingSession> entry : activeSessions.entrySet()) {
            Player p = plugin.getServer().getPlayer(entry.getKey());
            entry.getValue().cleanup(p);
        }
        activeSessions.clear();
    }

    public boolean isSessionActive(Player player) {
        return player != null && activeSessions.containsKey(player.getUniqueId());
    }

    public int getDefaultRadius() {
        return defaultRadius;
    }

    public int getMaxRadius() {
        return maxRadius;
    }

    private NamedTextColor parseNamedTextColor(String name) {
        if (name == null) return NamedTextColor.WHITE;
        switch (name.toUpperCase(Locale.ROOT)) {
            case "AQUA": return NamedTextColor.AQUA;
            case "BLACK": return NamedTextColor.BLACK;
            case "BLUE": return NamedTextColor.BLUE;
            case "DARK_AQUA": return NamedTextColor.DARK_AQUA;
            case "DARK_BLUE": return NamedTextColor.DARK_BLUE;
            case "DARK_GRAY": return NamedTextColor.DARK_GRAY;
            case "DARK_GREEN": return NamedTextColor.DARK_GREEN;
            case "DARK_PURPLE": return NamedTextColor.DARK_PURPLE;
            case "DARK_RED": return NamedTextColor.DARK_RED;
            case "GOLD": return NamedTextColor.GOLD;
            case "GRAY": return NamedTextColor.GRAY;
            case "GREEN": return NamedTextColor.GREEN;
            case "LIGHT_PURPLE": return NamedTextColor.LIGHT_PURPLE;
            case "RED": return NamedTextColor.RED;
            case "WHITE": return NamedTextColor.WHITE;
            case "YELLOW": return NamedTextColor.YELLOW;
            default: return NamedTextColor.WHITE;
        }
    }
}
