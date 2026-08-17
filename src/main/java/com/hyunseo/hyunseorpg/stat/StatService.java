package com.hyunseo.hyunseorpg.stat;

import com.hyunseo.hyunseorpg.mana.ManaService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class StatService {
    private final PlayerDataService playerDataService;
    private final StatCalculator statCalculator;
    private final StatModifierService statModifierService;
    private final ManaService manaService;

    public StatService(
            PlayerDataService playerDataService,
            StatCalculator statCalculator,
            StatModifierService statModifierService,
            ManaService manaService
    ) {
        this.playerDataService = playerDataService;
        this.statCalculator = statCalculator;
        this.statModifierService = statModifierService;
        this.manaService = manaService;
    }

    public double getBaseStat(Player player, StatType statType) {
        return playerDataService.getOrLoad(player).getStat(statType);
    }

    public double getEffectiveStat(Player player, StatType statType) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        return statCalculator.calculateStatValue(data, statType, getModifiers(player));
    }

    public int getStatLevel(Player player, StatType statType) {
        return playerDataService.getOrLoad(player).getStatLevel(statType);
    }

    public int getStatPoints(Player player) {
        return playerDataService.getOrLoad(player).getStatPoints();
    }

    public StatInvestmentResult investStatPoint(Player player, StatType statType) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        int maxLevel = getMaxBaseStat();
        int currentLevel = data.getStatLevel(statType);

        if (currentLevel >= maxLevel) {
            return StatInvestmentResult.failure(statType.name() + "은 이미 최대치입니다: " + currentLevel + "/" + maxLevel, currentLevel, maxLevel, data.getStatPoints());
        }
        if (data.getStatPoints() <= 0) {
            return StatInvestmentResult.failure("사용 가능한 스탯포인트가 없습니다.", currentLevel, maxLevel, data.getStatPoints());
        }

        data.setStatPoints(data.getStatPoints() - 1);
        data.setStatLevel(statType, currentLevel + 1);
        playerDataService.savePlayer(player);
        refreshPlayerStats(player);
        return StatInvestmentResult.success(statType, currentLevel + 1, maxLevel, data.getStatPoints());
    }

    public void adminSetBaseStat(Player player, StatType statType, double value) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        data.setStat(statType, Math.max(0.0D, value));
        playerDataService.savePlayer(player);
        manaService.refresh(player);
        refreshPlayerAttributes(player);
    }

    public void adminAddBaseStat(Player player, StatType statType, double amount) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        data.setStat(statType, Math.max(0.0D, data.getStat(statType) + amount));
        playerDataService.savePlayer(player);
        manaService.refresh(player);
        refreshPlayerAttributes(player);
    }

    public void resetBaseStats(Player player) {
        PlayerRPGData data = playerDataService.getOrLoad(player);
        for (StatType statType : StatType.values()) {
            data.setStat(statType, 0.0D);
        }
        playerDataService.savePlayer(player);
        refreshPlayerStats(player);
    }

    public void putModifier(Player player, String sourceId, StatType statType, StatModifierOperation operation, double amount) {
        statModifierService.putModifier(player.getUniqueId(), new StatModifier(sourceId, statType, operation, amount));
        manaService.refresh(player);
    }

    public boolean removeModifier(Player player, String sourceId) {
        boolean removed = statModifierService.removeModifier(player.getUniqueId(), sourceId);
        if (removed) {
            manaService.refresh(player);
        }
        return removed;
    }

    public Collection<StatModifier> getModifiers(Player player) {
        return statModifierService.getModifiers(player.getUniqueId());
    }

    public void clearRuntimeModifiers(Player player) {
        statModifierService.clearModifiers(player.getUniqueId());
        refreshPlayerStats(player);
    }

    public int getMaxBaseStat() {
        return statCalculator.getMaxBaseStat();
    }

    public void refreshPlayerStats(Player player) {
        manaService.refresh(player);
        refreshPlayerAttributes(player);
    }

    private void refreshPlayerAttributes(Player player) {
        setAttribute(player, Attribute.MAX_HEALTH, getEffectiveStat(player, StatType.MAX_HEALTH));
        setAttribute(player, Attribute.MOVEMENT_SPEED, getEffectiveStat(player, StatType.MOVE_SPEED));
        setAttribute(player, Attribute.ATTACK_SPEED, getEffectiveStat(player, StatType.ATTACK_SPEED));
        setAttribute(player, Attribute.ATTACK_DAMAGE, getEffectiveStat(player, StatType.ATTACK));

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null && player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }

    private void setAttribute(Player player, Attribute attribute, double value) {
        AttributeInstance attributeInstance = player.getAttribute(attribute);
        if (attributeInstance != null) {
            attributeInstance.setBaseValue(Math.max(0.0D, value));
        }
    }
}
