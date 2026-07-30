package com.extremelyhelpful.devtools;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;

public class EntityDamageListener implements Listener {
    private final ExtremelyHelpfulDeveloperTools plugin;

    public EntityDamageListener(ExtremelyHelpfulDeveloperTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        Set<TrackingSession> sessions = plugin.getTrackingManager().getSessionsForEntity(entity);
        if (sessions.isEmpty()) {
            return;
        }

        double rawDamage = event.getDamage();
        double finalDamage = event.getFinalDamage();
        double healthBefore = livingEntity.getHealth();
        double healthAfter = Math.max(0.0, healthBefore - finalDamage);

        for (TrackingSession session : sessions) {
            Player player = session.getPlayer();
            if (!player.isOnline()) {
                continue;
            }

            HitRecord hit = session.recordHit(rawDamage, finalDamage, healthBefore, healthAfter);
            sendHitNotification(player, session, hit);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Set<TrackingSession> sessions = plugin.getTrackingManager().getSessionsForEntity(entity);
        if (sessions.isEmpty()) {
            return;
        }

        long deathTime = System.currentTimeMillis();

        for (TrackingSession session : sessions) {
            Player player = session.getPlayer();
            if (player.isOnline()) {
                sendDeathSummaryReport(player, session, deathTime);
            }
            plugin.getTrackingManager().removeSession(session);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getTrackingManager().stopTracking(event.getPlayer());
    }

    private void sendHitNotification(Player player, TrackingSession session, HitRecord hit) {
        String msg = plugin.getConfigString("messages.hit-logged")
                .replace("%hit_index%", String.valueOf(hit.getHitIndex()))
                .replace("%raw_damage%", hit.getFormattedRawDamage())
                .replace("%final_damage%", hit.getFormattedFinalDamage())
                .replace("%health_before%", hit.getFormattedHealthBefore())
                .replace("%health_after%", hit.getFormattedHealthAfter())
                .replace("%entity%", session.getEntityName());

        player.sendMessage(plugin.colorize(plugin.getConfigString("messages.prefix") + msg));
    }

    private void sendDeathSummaryReport(Player player, TrackingSession session, long deathTime) {
        String prefix = plugin.getConfigString("messages.prefix");

        player.sendMessage(plugin.colorize(plugin.getConfigString("messages.summary-header")));

        player.sendMessage(plugin.colorize(prefix + plugin.getConfigString("messages.summary-target")
                .replace("%entity%", session.getEntityName())
                .replace("%uuid%", session.getEntityUuid().toString())));

        player.sendMessage(plugin.colorize(prefix + plugin.getConfigString("messages.summary-initial-hp")
                .replace("%initial_health%", session.getFormattedInitialHealth())));

        player.sendMessage(plugin.colorize(prefix + plugin.getConfigString("messages.summary-final-hp")
                .replace("%final_health%", "0.0")));

        player.sendMessage(plugin.colorize(prefix + plugin.getConfigString("messages.summary-total-hits")
                .replace("%total_hits%", String.valueOf(session.getHitRecords().size()))));

        player.sendMessage(plugin.colorize(prefix + plugin.getConfigString("messages.summary-duration")
                .replace("%time_seconds%", session.getFormattedTimePassedSeconds(deathTime))));

        player.sendMessage(plugin.colorize(prefix + plugin.getConfigString("messages.summary-dps")
                .replace("%dps%", session.getFormattedDPS(deathTime))));

        player.sendMessage(plugin.colorize(plugin.getConfigString("messages.summary-footer")));
    }
}
