package com.extremelyhelpful.devtools.spelunking;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class SpelunkingListener implements Listener {
    private final SpelunkingManager manager;

    public SpelunkingListener(SpelunkingManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.stopSession(event.getPlayer());
    }
}
