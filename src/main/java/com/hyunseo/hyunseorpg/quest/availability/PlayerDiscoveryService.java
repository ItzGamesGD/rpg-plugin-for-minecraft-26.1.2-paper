package com.hyunseo.hyunseorpg.quest.availability;

import com.hyunseo.hyunseorpg.core.event.RPGMobKillEvent;
import com.hyunseo.hyunseorpg.mob.MobTagService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Canonical custom-monster discovery tracking; item discovery remains NaturalDiscoveryService's responsibility. */
public final class PlayerDiscoveryService implements Listener {
    private static final double DISCOVERY_RADIUS = 24.0D;
    private static final long SCAN_INTERVAL_MILLIS = 1_000L;

    private final PlayerDataService playerData;
    private final MobTagService tags;
    private final Map<UUID, Long> lastScan = new HashMap<>();

    public PlayerDiscoveryService(PlayerDataService playerData, MobTagService tags) {
        this.playerData = playerData;
        this.tags = tags;
    }

    public boolean hasDiscovered(Player player, String customMobId) {
        return player != null && playerData.getOrLoad(player).hasDiscoveredCustomMonster(customMobId);
    }

    public void discover(Player player, String customMobId) {
        if (player == null || customMobId == null || customMobId.isBlank()) return;
        PlayerRPGData data = playerData.getOrLoad(player);
        if (data.hasDiscoveredCustomMonster(customMobId)) return;
        data.markCustomMonsterSeen(customMobId, System.currentTimeMillis());
        playerData.savePlayer(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ())) return;
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        if (now - lastScan.getOrDefault(player.getUniqueId(), 0L) < SCAN_INTERVAL_MILLIS) return;
        lastScan.put(player.getUniqueId(), now);
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(DISCOVERY_RADIUS, DISCOVERY_RADIUS, DISCOVERY_RADIUS)) {
            if (!(entity instanceof LivingEntity living) || !tags.isCustomMob(living) || !player.hasLineOfSight(living)) continue;
            String id = tags.getCustomMobId(living);
            if (!id.isBlank()) discover(player, id);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(RPGMobKillEvent event) {
        if (event.getKiller() == null || !event.getContext().customMob()) return;
        String id = event.getContext().customMobId();
        if (id.isBlank()) return;
        PlayerRPGData data = playerData.getOrLoad(event.getKiller());
        data.markCustomMonsterKilled(id, System.currentTimeMillis());
        playerData.savePlayer(event.getKiller());
    }
}
