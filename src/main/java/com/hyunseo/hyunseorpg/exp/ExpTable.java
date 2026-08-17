package com.hyunseo.hyunseorpg.exp;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

public final class ExpTable {
    private final ConfigService configService;

    public ExpTable(ConfigService configService) {
        this.configService = configService;
    }

    public long getRequiredExpForNextBaseLevel(int currentLevel) {
        int safeLevel = Math.max(1, currentLevel);
        long configuredValue = configService.getExpLong("base-exp-required." + safeLevel, -1L);
        if (configuredValue > 0L) {
            return scaled(configuredValue);
        }

        if (safeLevel <= 16) {
            return scaled(2L * safeLevel + 7L);
        }
        if (safeLevel <= 31) {
            return scaled(5L * safeLevel - 38L);
        }
        return scaled(9L * safeLevel - 158L);
    }

    public int getMaxBaseLevel() {
        return Math.max(1, configService.getExpInt("base-level.max-level", 100));
    }

    public int getStatPointsPerBaseLevel() {
        return Math.max(0, configService.getExpInt("base-level.stat-points-per-level", 1));
    }

    public long getRequiredExpForNextClassLevel(int currentLevel) {
        int safeLevel = Math.max(1, currentLevel);
        long configuredValue = configService.getExpLong("class-exp-required." + safeLevel, -1L);
        if (configuredValue > 0L) {
            return configuredValue;
        }
        return 20L + (long) safeLevel * 10L;
    }

    public int getMaxClassLevel() {
        return Math.max(1, configService.getExpInt("class-level.max-level", 100));
    }

    public int getSkillPointsPerClassLevel() {
        return Math.max(0, configService.getExpInt("class-level.skill-points-per-level", 1));
    }

    public int getClassStatPointsPerClassLevel() {
        return Math.max(0, configService.getExpInt("class-level.class-stat-points-per-level", 1));
    }

    public boolean shouldSyncVanillaExpBar() {
        return configService.getExpBoolean("base-level.sync-vanilla-exp-bar", true);
    }

    private long scaled(long value) {
        double multiplier = configService.getExpDouble("base-level.required-exp-multiplier", 1.0D);
        if (!Double.isFinite(multiplier) || multiplier <= 0.0D) multiplier = 1.0D;
        return Math.max(1L, Math.round(value * multiplier));
    }
}
