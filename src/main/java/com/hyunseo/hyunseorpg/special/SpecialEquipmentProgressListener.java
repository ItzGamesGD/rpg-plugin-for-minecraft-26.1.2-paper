package com.hyunseo.hyunseorpg.special;

import com.hyunseo.hyunseorpg.core.event.RPGMobKillEvent;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/** Persists the progression counters used by special-equipment unlock requirements. */
public final class SpecialEquipmentProgressListener implements Listener {
    private final PlayerDataService playerDataService;

    public SpecialEquipmentProgressListener(PlayerDataService playerDataService) {
        this.playerDataService = playerDataService;
    }

    @EventHandler
    public void onMobKill(RPGMobKillEvent event) {
        if (event.getKiller() == null) return;
        PlayerRPGData data = playerDataService.getOrLoad(event.getKiller());
        String customMobId = event.getContext().customMobId();
        if (!customMobId.isBlank()) data.incrementCustomMobKillCount(customMobId);
        if (event.isBoss()) data.incrementBossKillCount(event.getMobId());
        playerDataService.savePlayer(event.getKiller());
    }

    @EventHandler
    public void onVanillaBossDeath(EntityDeathEvent event) {
        String bossId = switch (event.getEntity().getType()) {
            case WITHER -> "wither";
            case ENDER_DRAGON -> "ender_dragon";
            default -> "";
        };
        if (bossId.isBlank() || event.getEntity().getKiller() == null) return;
        PlayerRPGData data = playerDataService.getOrLoad(event.getEntity().getKiller());
        data.incrementBossKillCount(bossId);
        playerDataService.savePlayer(event.getEntity().getKiller());
    }
}
