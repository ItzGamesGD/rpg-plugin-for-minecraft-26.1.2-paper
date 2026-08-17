package com.hyunseo.hyunseorpg.exp;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class LevelPlayerListener implements Listener {
    private final JavaPlugin plugin;
    private final LevelService levelService;

    public LevelPlayerListener(JavaPlugin plugin, LevelService levelService) {
        this.plugin = plugin;
        this.levelService = levelService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        refreshNextTick(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setDroppedExp(0);
        event.setKeepLevel(true);
        event.setNewLevel(event.getPlayer().getLevel());
        event.setNewExp(Math.round(event.getPlayer().getExp()));
        event.setNewTotalExp(event.getPlayer().getTotalExperience());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        refreshNextTick(event.getPlayer());
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        if (event.getAmount() == 0) {
            return;
        }

        event.setAmount(0);
        refreshNextTick(event.getPlayer());
    }

    private void refreshNextTick(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                levelService.refreshVanillaExpBar(player);
            }
        });
    }
}
