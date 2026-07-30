package com.extremelyhelpful.devtools;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrackingManager {
    private final Map<UUID, TrackingSession> playerSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Set<TrackingSession>> entitySessions = new ConcurrentHashMap<>();

    public synchronized TrackingSession startTracking(Player player, LivingEntity entity) {
        stopTracking(player);

        TrackingSession session = new TrackingSession(player, entity);
        playerSessions.put(player.getUniqueId(), session);

        entitySessions.computeIfAbsent(entity.getUniqueId(), k -> Collections.synchronizedSet(new HashSet<>()))
                .add(session);

        return session;
    }

    public synchronized TrackingSession stopTracking(Player player) {
        TrackingSession session = playerSessions.remove(player.getUniqueId());
        if (session != null) {
            Set<TrackingSession> sessions = entitySessions.get(session.getEntityUuid());
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    entitySessions.remove(session.getEntityUuid());
                }
            }
        }
        return session;
    }

    public TrackingSession getSessionByPlayer(Player player) {
        return playerSessions.get(player.getUniqueId());
    }

    public Set<TrackingSession> getSessionsForEntity(Entity entity) {
        Set<TrackingSession> sessions = entitySessions.get(entity.getUniqueId());
        if (sessions == null) {
            return Collections.emptySet();
        }
        synchronized (sessions) {
            return new HashSet<>(sessions);
        }
    }

    public synchronized void removeSession(TrackingSession session) {
        if (session == null) return;
        playerSessions.remove(session.getPlayer().getUniqueId(), session);
        Set<TrackingSession> sessions = entitySessions.get(session.getEntityUuid());
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                entitySessions.remove(session.getEntityUuid());
            }
        }
    }

    public synchronized void clearAll() {
        playerSessions.clear();
        entitySessions.clear();
    }
}
