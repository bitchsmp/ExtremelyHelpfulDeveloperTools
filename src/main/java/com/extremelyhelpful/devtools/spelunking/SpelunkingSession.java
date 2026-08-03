package com.extremelyhelpful.devtools.spelunking;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SpelunkingSession {
    private final UUID playerUuid;
    private final List<Integer> entityIds;
    private final Set<String> teamNames;
    private BukkitTask expiryTask;

    public SpelunkingSession(UUID playerUuid, List<Integer> entityIds, Set<String> teamNames) {
        this.playerUuid = playerUuid;
        this.entityIds = entityIds != null ? entityIds : new ArrayList<>();
        this.teamNames = teamNames != null ? teamNames : new HashSet<>();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public List<Integer> getEntityIds() {
        return entityIds;
    }

    public Set<String> getTeamNames() {
        return teamNames;
    }

    public BukkitTask getExpiryTask() {
        return expiryTask;
    }

    public void setExpiryTask(BukkitTask expiryTask) {
        this.expiryTask = expiryTask;
    }

    public void cleanup(Player player) {
        if (expiryTask != null && !expiryTask.isCancelled()) {
            expiryTask.cancel();
        }

        if (player != null && player.isOnline()) {
            // Destroy spawned fake glowing entities
            if (!entityIds.isEmpty()) {
                int[] ids = entityIds.stream().mapToInt(Integer::intValue).toArray();
                WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(ids);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyPacket);
            }

            // Remove glow teams
            for (String teamName : teamNames) {
                try {
                    WrapperPlayServerTeams removeTeamPacket = new WrapperPlayServerTeams(
                            teamName,
                            WrapperPlayServerTeams.TeamMode.REMOVE,
                            (WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
                            List.of()
                    );
                    PacketEvents.getAPI().getPlayerManager().sendPacket(player, removeTeamPacket);
                } catch (Exception ignored) {
                }
            }
        }

        entityIds.clear();
        teamNames.clear();
    }
}
