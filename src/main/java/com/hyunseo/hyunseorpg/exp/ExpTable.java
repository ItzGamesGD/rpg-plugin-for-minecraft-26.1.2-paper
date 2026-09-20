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






    private long scaled(long value) {
        double multiplier = configService.getExpDouble("base-level.required-exp-multiplier", 1.0D);
        if (!Double.isFinite(multiplier) || multiplier <= 0.0D) multiplier = 1.0D;
        return Math.max(1L, Math.round(value * multiplier));
    }
}
