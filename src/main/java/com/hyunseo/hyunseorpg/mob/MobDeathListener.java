package com.hyunseo.hyunseorpg.mob;

import com.hyunseo.hyunseorpg.core.event.RPGMobKillEvent;
import com.hyunseo.hyunseorpg.core.event.MonsterKillContext;
import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.mythic.MythicMobData;
import com.hyunseo.hyunseorpg.mythic.MythicMobIntegrationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class MobDeathListener implements Listener {
    private final MobRewardService mobRewardService;
    private final MobService mobService;
    private final InventoryDeliveryService inventoryDeliveryService;
    private final MythicMobIntegrationService mythicService;

    public MobDeathListener(MobRewardService mobRewardService, MobService mobService,
                            InventoryDeliveryService inventoryDeliveryService) {
        this(mobRewardService, mobService, inventoryDeliveryService, null);
    }

    public MobDeathListener(MobRewardService mobRewardService, MobService mobService,
                            InventoryDeliveryService inventoryDeliveryService,
                            MythicMobIntegrationService mythicService) {
        this.mobRewardService = mobRewardService;
        this.mobService = mobService;
        this.inventoryDeliveryService = inventoryDeliveryService;
        this.mythicService = mythicService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity defeated = event.getEntity();
        if (defeated instanceof Player) {
            return;
        }

        Player killer = defeated.getKiller();
        if (killer == null) {
            return;
        }

        if (mythicService != null) {
            MythicMobData mythicData = mythicService.findMobId(defeated)
                    .flatMap(mythicService.registry()::get).orElse(null);
            if (mythicData != null) {
                if (!mythicServiceRegistryAllowsNativeDrops(mythicService)) {
                    event.getDrops().clear();
                }
                Bukkit.getPluginManager().callEvent(new RPGMobKillEvent(
                        killer, defeated, mythicData.mobId(), mythicData.level(), mythicData.boss(), mythicData.elite(),
                        mythicData.expReward(), mythicData.classExpReward(), null,
                        MonsterKillContext.mythic(defeated, mythicData.mobId(), mythicData.level(),
                                mythicData.boss(), mythicData.elite())));
                mobRewardService.rewardMythic(killer, defeated, mythicData);
                return;
            }
        }

        if (mobService.isManageableRpgCandidate(defeated)) {
            event.getDrops().forEach(drop -> inventoryDeliveryService.giveOrDiscard(killer, drop));
            event.getDrops().clear();
        }

        if (mobService.isRpgMob(defeated) || !mobService.getMobId(defeated).isBlank()) {
            Bukkit.getPluginManager().callEvent(new RPGMobKillEvent(
                    killer,
                    defeated,
                    mobService.getMobId(defeated),
                    mobService.getMobTagService().getMobLevel(defeated),
                    mobService.isBossMob(defeated),
                    mobService.isEliteMob(defeated),
                    mobService.getBaseExpReward(defeated),
                    mobService.getClassExpReward(defeated),
                    mobService.getMobData(defeated).orElse(null),
                    MonsterKillContext.from(defeated, mobService)
            ));
        }

        mobRewardService.reward(killer, defeated);
    }

    private boolean mythicServiceRegistryAllowsNativeDrops(MythicMobIntegrationService service) {
        return service.nativeDropsAllowed();
    }
}
