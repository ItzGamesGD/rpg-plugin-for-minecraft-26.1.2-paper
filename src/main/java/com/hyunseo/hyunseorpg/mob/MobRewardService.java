package com.hyunseo.hyunseorpg.mob;

import com.hyunseo.hyunseorpg.economy.CoinService;
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
    private final CoinService coinService;
    private final MobDropService mobDropService;
    private final MythicMobIntegrationService mythicService;

    public MobRewardService(MobService mobService, ExpService expService, CoinService coinService, MobDropService mobDropService) {
        this(mobService, expService, coinService, mobDropService, null);
    }

    public MobRewardService(MobService mobService, ExpService expService, CoinService coinService,
                            MobDropService mobDropService, MythicMobIntegrationService mythicService) {
        this.mobService = mobService;
        this.expService = expService;
        this.coinService = coinService;
        this.mobDropService = mobDropService;
        this.mythicService = mythicService;
    }

    public void rewardMythic(Player player, LivingEntity defeated, MythicMobData data) {
        long exp = data.expReward();
        long coins = data.coinReward();
        long classExp = data.classExpReward();
        if (exp > 0L) expService.giveBaseExp(player, exp);
        if (classExp > 0L) expService.giveClassExp(player, classExp);
        if (coins > 0L) {
            coinService.addCoins(player, coins);
            player.sendMessage(Component.text("코인 +" + coins, NamedTextColor.GOLD));
        }
        if (mythicService != null) mythicService.dropRewards(player, data);
        player.sendMessage(Component.text(
                "MythicMobs 처치: " + data.displayName() + " (" + data.mobId() + ")"
                        + ", 경험치=" + exp + ", 코인=" + coins,
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

        long coins = mobService.getCoinReward(defeated);
        if (coins > 0L) {
            coinService.addCoins(player, coins);
            player.sendMessage(Component.text("코인 +" + coins, NamedTextColor.GOLD));
        }

        mobDropService.dropRewards(player, defeated);

        if (!mobService.isRpgMob(defeated)) {
            return;
        }

        long classExp = mobService.getClassExpReward(defeated);
        if (classExp > 0L) {
            expService.giveClassExp(player, classExp);
        }

        player.sendMessage(Component.text(
                "RPG 몹 처치: ID=" + mobService.getMobId(defeated)
                        + ", 레벨=" + mobService.getMobTagService().getMobLevel(defeated)
                        + ", 기본 경험치=" + baseExp
                        + ", 전문화 경험치=" + classExp
                        + ", 코인=" + coins,
                NamedTextColor.GRAY
        ));
    }
}
