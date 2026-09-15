package com.hyunseo.hyunseorpg.mob;

import com.hyunseo.hyunseorpg.exp.ExpService;
import com.hyunseo.hyunseorpg.mob.drop.MobDropService;
import com.hyunseo.hyunseorpg.mythic.MythicMobData;
import com.hyunseo.hyunseorpg.mythic.MythicMobIntegrationService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class MobRewardService {
    private final MobService mobService;
    private final ExpService expService;
    private final MobDropService mobDropService;
    private final MythicMobIntegrationService mythicService;

    public MobRewardService(MobService mobService, ExpService expService,
                            MobDropService mobDropService, MythicMobIntegrationService mythicService) {
        this.mobService = mobService;
        this.expService = expService;
        this.mobDropService = mobDropService;
        this.mythicService = mythicService;
    }

    public void rewardMythic(Player player, LivingEntity defeated, MythicMobData data) {
        long exp = data.expReward();
        if (exp > 0L) expService.giveBaseExp(player, exp);
        if (mythicService != null) mythicService.dropRewards(player, data);
        player.sendMessage(Component.text(
                "MythicMobs 처치: " + data.displayName() + " (" + data.mobId() + ")"
                        + ", 경험치=" + exp,
                NamedTextColor.GRAY));
    }

    public void reward(Player player, LivingEntity defeated) {
        if (!mobService.isRewardableMob(defeated)) {
            return;
        }

        long baseExp = mobService.getBaseExpReward(defeated);
        if (baseExp > 0L) {
            expService.giveBaseExp(player, baseExp);
        }

        mobDropService.dropRewards(player, defeated);

        if (!mobService.isRpgMob(defeated)) {
            return;
        }

        player.sendMessage(Component.text(
                "RPG 몹 처치: ID=" + mobService.getMobId(defeated)
                        + ", 레벨=" + mobService.getMobTagService().getMobLevel(defeated)
                        + ", 기본 경험치=" + baseExp,
                NamedTextColor.GRAY
        ));
    }
}
