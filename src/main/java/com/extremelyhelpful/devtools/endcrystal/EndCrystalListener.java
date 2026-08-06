package com.extremelyhelpful.devtools.endcrystal;

import org.bukkit.Location;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class EndCrystalListener implements Listener {
    private final EndCrystalManager manager;

    public EndCrystalListener(EndCrystalManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION && cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            return;
        }

        if (crystal.isDead() || !crystal.isValid()) {
            return;
        }

        if (!manager.shouldExplodeOnExplosionDamage(crystal.getWorld())) {
            // "killed" mode: allow vanilla default behavior (crystal destroyed quietly without exploding)
            return;
        }

        // "explode" mode: cancel damage, remove crystal, and detonate explosion
        event.setCancelled(true);
        Location loc = crystal.getLocation();
        crystal.remove();

        crystal.getWorld().createExplosion(
                loc,
                manager.getExplosionPower(),
                manager.isSetFire(),
                manager.isBreakBlocks()
        );
    }
}
