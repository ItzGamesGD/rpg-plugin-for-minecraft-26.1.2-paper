package com.hyunseo.hyunseorpg.core.event;

import com.hyunseo.hyunseorpg.mob.MobData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Optional;

public final class RPGMobKillEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player killer;
    private final LivingEntity defeated;
    private final String mobId;
    private final int mobLevel;
    private final boolean boss;
    private final boolean elite;
    private final long baseExpReward;
    private final long classExpReward;
    private final MobData mobData;
    private final MonsterKillContext context;

    public RPGMobKillEvent(
            Player killer,
            LivingEntity defeated,
            String mobId,
            int mobLevel,
            boolean boss,
            boolean elite,
            long baseExpReward,
            long classExpReward,
            MobData mobData
    ) {
        this(killer, defeated, mobId, mobLevel, boss, elite, baseExpReward, classExpReward,
                mobData, new MonsterKillContext(
                        mobId, "", mobLevel, "", mobData == null ? "" : mobData.dropTableId(),
                        defeated.getType(), java.util.Set.of(), false, boss, elite));
    }

    public RPGMobKillEvent(
            Player killer,
            LivingEntity defeated,
            String mobId,
            int mobLevel,
            boolean boss,
            boolean elite,
            long baseExpReward,
            long classExpReward,
            MobData mobData,
            MonsterKillContext context
    ) {
        this.killer = killer;
        this.defeated = defeated;
        this.mobId = mobId;
        this.mobLevel = mobLevel;
        this.boss = boss;
        this.elite = elite;
        this.baseExpReward = baseExpReward;
        this.classExpReward = classExpReward;
        this.mobData = mobData;
        this.context = context;
    }

    public Player getKiller() {
        return killer;
    }

    public LivingEntity getDefeated() {
        return defeated;
    }

    public String getMobId() {
        return mobId;
    }

    public int getMobLevel() {
        return mobLevel;
    }

    public boolean isBoss() {
        return boss;
    }

    public boolean isElite() {
        return elite;
    }

    public long getBaseExpReward() {
        return baseExpReward;
    }

    public long getClassExpReward() {
        return classExpReward;
    }


    public Optional<MobData> getMobData() {
        return Optional.ofNullable(mobData);
    }

    public MonsterKillContext getContext() {
        return context;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
