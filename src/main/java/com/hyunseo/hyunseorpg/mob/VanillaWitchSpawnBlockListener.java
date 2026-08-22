package com.hyunseo.hyunseorpg.mob;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Blocks vanilla witch creation while keeping plugin-created custom witches available.
 *
 * <p>Custom RPG witches receive their PDC identity only after Bukkit creates the entity,
 * so the spawn event cannot use that identity as its exemption. Bukkit reports those
 * plugin-controlled spawns as {@code CUSTOM}; every vanilla-controlled spawn reason is
 * cancelled here instead.</p>
 */
public final class VanillaWitchSpawnBlockListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (shouldBlock(event.getEntityType(), event.getSpawnReason())) {
            event.setCancelled(true);
        }
    }

    static boolean shouldBlock(EntityType entityType, CreatureSpawnEvent.SpawnReason spawnReason) {
        return entityType == EntityType.WITCH
                && spawnReason != CreatureSpawnEvent.SpawnReason.CUSTOM;
    }
}
