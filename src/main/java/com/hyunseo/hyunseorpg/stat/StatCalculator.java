package com.hyunseo.hyunseorpg.stat;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;

import java.util.Collection;

/** Common stats no longer inherit combat values from a selected old class. */
public final class StatCalculator {
    private final ConfigService configService;

    public StatCalculator(ConfigService configService) {
        this.configService = configService;
    }

    public double calculateStatValue(PlayerRPGData data, StatType statType, Collection<StatModifier> modifiers) {
        if (statType == StatType.MAX_MANA) {
            return calculateMaxMana(data, modifiers);
        }
        if (statType == StatType.MANA_REGEN) {
            return calculateManaRegenPerSecond(data, modifiers);
        }
        double explicitValue = data.getStat(statType);
        double baseValue = explicitValue > 0.0D ? explicitValue : getDefaultStatValue(statType);
        return applyModifiers(baseValue + getInvestedStatBonus(data, statType), statType, modifiers);
    }

    public double calculateMaxMana(PlayerRPGData data, Collection<StatModifier> modifiers) {
        double explicitMaxMana = data.getStat(StatType.MAX_MANA);
        double baseMaxMana = explicitMaxMana > 0.0D ? explicitMaxMana : getDefaultStatValue(StatType.MAX_MANA);
        return applyModifiers(baseMaxMana + getInvestedStatBonus(data, StatType.MAX_MANA), StatType.MAX_MANA, modifiers);
    }

    public double calculateManaRegenPerSecond(PlayerRPGData data, Collection<StatModifier> modifiers) {
        double explicitRegen = data.getStat(StatType.MANA_REGEN);
        double baseRegen = explicitRegen > 0.0D ? explicitRegen : getDefaultStatValue(StatType.MANA_REGEN);
        return applyModifiers(baseRegen + getInvestedStatBonus(data, StatType.MANA_REGEN), StatType.MANA_REGEN, modifiers);
    }

    public int getMaxBaseStat() {
        return configService.getStatsInt("max-base-stat", 8);
    }

    private double applyModifiers(double baseValue, StatType statType, Collection<StatModifier> modifiers) {
        double value = baseValue;
        double multiplyAmount = 0.0D;
        for (StatModifier modifier : modifiers) {
            if (modifier.statType() != statType) {
                continue;
            }
            if (modifier.operation() == StatModifierOperation.ADD) {
                value += modifier.amount();
            } else if (modifier.operation() == StatModifierOperation.MULTIPLY) {
                multiplyAmount += modifier.amount();
            }
        }
        return Math.max(0.0D, value * Math.max(0.0D, 1.0D + multiplyAmount));
    }

    private double getInvestedStatBonus(PlayerRPGData data, StatType statType) {
        return data.getStatLevel(statType) * getStatPointBonus(statType);
    }

    private double getStatPointBonus(StatType statType) {
        return switch (statType) {
            case MAX_HEALTH -> configService.getStatsDouble("stat-point-bonuses.MAX_HEALTH", 2.0D);
            case MAX_MANA -> configService.getStatsDouble("stat-point-bonuses.MAX_MANA", 10.0D);
            case MANA_REGEN -> configService.getStatsDouble("stat-point-bonuses.MANA_REGEN", 0.5D);
            case ATTACK -> configService.getStatsDouble("stat-point-bonuses.ATTACK", 1.0D);
            case ATTACK_SPEED -> configService.getStatsDouble("stat-point-bonuses.ATTACK_SPEED", 0.1D);
            case MOVE_SPEED -> configService.getStatsDouble("stat-point-bonuses.MOVE_SPEED", 0.005D);
            case DAMAGE_REDUCTION -> configService.getStatsDouble("stat-point-bonuses.DAMAGE_REDUCTION", 0.01D);
        };
    }

    private double getDefaultStatValue(StatType statType) {
        return switch (statType) {
            case MAX_HEALTH -> configService.getStatsDouble("stat-defaults.MAX_HEALTH", 20.0D);
            case MAX_MANA -> getDefaultMaxMana();
            case MANA_REGEN -> configService.getStatsDouble("stat-defaults.MANA_REGEN", 1.0D);
            case ATTACK -> configService.getStatsDouble("stat-defaults.ATTACK", 1.0D);
            case ATTACK_SPEED -> configService.getStatsDouble("stat-defaults.ATTACK_SPEED", 4.0D);
            case MOVE_SPEED -> configService.getStatsDouble("stat-defaults.MOVE_SPEED", 0.1D);
            case DAMAGE_REDUCTION -> configService.getStatsDouble("stat-defaults.DAMAGE_REDUCTION", 0.0D);
        };
    }

    private double getDefaultMaxMana() {
        double configured = configService.getStatsDouble("stat-defaults.MAX_MANA", 0.0D);
        return configured > 0.0D ? configured : configService.getDouble("mana.base-max-mana", 100.0D);
    }
}
