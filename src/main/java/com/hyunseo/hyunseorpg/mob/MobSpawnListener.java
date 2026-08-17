package com.hyunseo.hyunseorpg.mob;

import com.hyunseo.hyunseorpg.mob.variant.ZombieVariantService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class MobSpawnListener implements Listener {
    private final MobService mobService;
    private final MobLevelScalingService mobLevelScalingService;
    private final ZombieVariantService zombieVariantService;

    public MobSpawnListener(MobService mobService, MobLevelScalingService mobLevelScalingService,
                            ZombieVariantService zombieVariantService) {
        this.mobService = mobService;
        this.mobLevelScalingService = mobLevelScalingService;
        this.zombieVariantService = zombieVariantService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }
        if (!mobService.isManageableRpgCandidate(livingEntity)) {
            return;
        }
        int level = mobLevelScalingService.calculateNaturalSpawnLevel(org.bukkit.Bukkit.getOnlinePlayers());
        mobService.markAsNaturalRpgMob(livingEntity, level);
        zombieVariantService.initializeNaturalSpawn(event);
    }
}
