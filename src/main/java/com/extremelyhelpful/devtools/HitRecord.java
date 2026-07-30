package com.extremelyhelpful.devtools;

import java.util.Locale;

public class HitRecord {
    private final int hitIndex;
    private final long timestamp;
    private final double rawDamage;
    private final double finalDamage;
    private final double healthBefore;
    private final double healthAfter;

    public HitRecord(int hitIndex, long timestamp, double rawDamage, double finalDamage, double healthBefore, double healthAfter) {
        this.hitIndex = hitIndex;
        this.timestamp = timestamp;
        this.rawDamage = rawDamage;
        this.finalDamage = finalDamage;
        this.healthBefore = healthBefore;
        this.healthAfter = healthAfter;
    }

    public int getHitIndex() {
        return hitIndex;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getRawDamage() {
        return rawDamage;
    }

    public double getFinalDamage() {
        return finalDamage;
    }

    public double getHealthBefore() {
        return healthBefore;
    }

    public double getHealthAfter() {
        return healthAfter;
    }

    public String getFormattedRawDamage() {
        return format(rawDamage);
    }

    public String getFormattedFinalDamage() {
        return format(finalDamage);
    }

    public String getFormattedHealthBefore() {
        return format(healthBefore);
    }

    public String getFormattedHealthAfter() {
        return format(healthAfter);
    }

    private static String format(double val) {
        return String.format(Locale.US, "%.1f", val);
    }
}
