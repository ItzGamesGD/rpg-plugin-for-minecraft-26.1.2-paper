package com.hyunseo.hyunseorpg.stat;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class StatModifierCleanupListener implements Listener {
    private final StatModifierService statModifierService;

    public StatModifierCleanupListener(StatModifierService statModifierService) {
        this.statModifierService = statModifierService;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        statModifierService.clearModifiers(event.getPlayer().getUniqueId());
    }
}
