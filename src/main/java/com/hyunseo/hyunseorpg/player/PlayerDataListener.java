package com.hyunseo.hyunseorpg.player;

import com.hyunseo.hyunseorpg.stat.StatService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerDataListener implements Listener {
    private final PlayerDataService playerDataService;
    private final StatService statService;

    public PlayerDataListener(PlayerDataService playerDataService, StatService statService) {
        this.playerDataService = playerDataService;
        this.statService = statService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerDataService.loadPlayer(event.getPlayer());
        statService.refreshPlayerStats(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataService.saveAndUnload(event.getPlayer());
    }
}
