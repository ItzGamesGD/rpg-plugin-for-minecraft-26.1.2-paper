package com.hyunseo.hyunseorpg.player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerDataListener implements Listener {
    private final PlayerDataService playerDataService;

    public PlayerDataListener(PlayerDataService playerDataService) {
        this.playerDataService = playerDataService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerDataService.loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataService.saveAndUnload(event.getPlayer());
    }
}
