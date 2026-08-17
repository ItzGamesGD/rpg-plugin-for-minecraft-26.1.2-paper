package com.hyunseo.hyunseorpg.mana;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ManaPlayerListener implements Listener {
    private final JavaPlugin plugin;
    private final ManaService manaService;
    private final ManaBossBarService bossBarService;

    public ManaPlayerListener(JavaPlugin plugin, ManaService manaService, ManaBossBarService bossBarService) {
        this.plugin = plugin;
        this.manaService = manaService;
        this.bossBarService = bossBarService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                manaService.refresh(player);
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        bossBarService.remove(event.getPlayer());
    }
}
