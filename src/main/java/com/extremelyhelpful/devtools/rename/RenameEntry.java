package com.extremelyhelpful.devtools.rename;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class RenameEntry {
    private final String key;
    private final String rawName;
    private final Component component;
    private final Set<Material> requiredMaterials;
    private final String requiredPermission;
    private final Integer cooldownSeconds;
    private final Integer costExpLevels;

    public RenameEntry(String key, String rawName, Component component, Set<Material> requiredMaterials, String requiredPermission, Integer cooldownSeconds, Integer costExpLevels) {
        this.key = key;
        this.rawName = rawName;
        this.component = component;
        this.requiredMaterials = requiredMaterials != null ? requiredMaterials : Collections.emptySet();
        this.requiredPermission = requiredPermission;
        this.cooldownSeconds = cooldownSeconds;
        this.costExpLevels = costExpLevels;
    }

    public String getKey() {
        return key;
    }

    public String getRawName() {
        return rawName;
    }

    public Component getComponent() {
        return component;
    }

    public Set<Material> getRequiredMaterials() {
        return requiredMaterials;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public Integer getCooldownSeconds() {
        return cooldownSeconds;
    }

    public Integer getCostExpLevels() {
        return costExpLevels;
    }

    public boolean matchesMaterial(Material material) {
        if (requiredMaterials.isEmpty()) {
            return true;
        }
        return requiredMaterials.contains(material);
    }

    public boolean matchesPermission(Player player) {
        if (requiredPermission == null || requiredPermission.isEmpty()) {
            return true;
        }
        return player.hasPermission(requiredPermission);
    }

    public String getFormattedRequiredMaterials() {
        if (requiredMaterials.isEmpty()) {
            return "ANY";
        }
        return requiredMaterials.stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
