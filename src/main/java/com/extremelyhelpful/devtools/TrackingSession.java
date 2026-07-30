package com.extremelyhelpful.devtools;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TrackingSession {
    private final Player player;
    private final LivingEntity entity;
    private final UUID entityUuid;
    private final String entityName;
    private final double initialHealth;
    private final long trackingStartTimestamp;
    private long firstHitTimestamp = -1L;
    private final List<HitRecord> hitRecords = new ArrayList<>();

    public TrackingSession(Player player, LivingEntity entity) {
        this.player = player;
        this.entity = entity;
        this.entityUuid = entity.getUniqueId();
        this.entityName = entity.getCustomName() != null ? entity.getCustomName() : entity.getType().name();
        this.initialHealth = entity.getHealth();
        this.trackingStartTimestamp = System.currentTimeMillis();
    }

    public Player getPlayer() {
        return player;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public String getEntityName() {
        return entityName;
    }

    public double getInitialHealth() {
        return initialHealth;
    }

    public String getFormattedInitialHealth() {
        return format(initialHealth);
    }

    public long getTrackingStartTimestamp() {
        return trackingStartTimestamp;
    }

    public long getFirstHitTimestamp() {
        return firstHitTimestamp;
    }

    public List<HitRecord> getHitRecords() {
        return Collections.unmodifiableList(hitRecords);
    }

    public synchronized HitRecord recordHit(double rawDamage, double finalDamage, double healthBefore, double healthAfter) {
        long now = System.currentTimeMillis();
        if (firstHitTimestamp <= 0L) {
            firstHitTimestamp = now;
        }
        int hitIndex = hitRecords.size() + 1;
        HitRecord hit = new HitRecord(hitIndex, now, rawDamage, finalDamage, healthBefore, healthAfter);
        hitRecords.add(hit);
        return hit;
    }

    public double getTimePassedSinceFirstHitSeconds(long deathTimestamp) {
        long start = (firstHitTimestamp > 0L) ? firstHitTimestamp : trackingStartTimestamp;
        long durationMs = deathTimestamp - start;
        if (durationMs < 50) {
            durationMs = 50;
        }
        return durationMs / 1000.0;
    }

    public double calculateDPS(long deathTimestamp) {
        double seconds = getTimePassedSinceFirstHitSeconds(deathTimestamp);
        return initialHealth / seconds;
    }

    public String getFormattedDPS(long deathTimestamp) {
        return format(calculateDPS(deathTimestamp));
    }

    public String getFormattedTimePassedSeconds(long deathTimestamp) {
        return format(getTimePassedSinceFirstHitSeconds(deathTimestamp));
    }

    public static String format(double val) {
        return String.format(Locale.US, "%.1f", val);
    }
}
