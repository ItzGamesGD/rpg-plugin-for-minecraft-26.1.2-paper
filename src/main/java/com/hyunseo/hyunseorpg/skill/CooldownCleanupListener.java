package com.hyunseo.hyunseorpg.skill;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class CooldownCleanupListener implements Listener {
    private final CooldownService cooldownService;

    public CooldownCleanupListener(CooldownService cooldownService) {
        this.cooldownService = cooldownService;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldownService.clearPlayer(event.getPlayer().getUniqueId());
    }
}
