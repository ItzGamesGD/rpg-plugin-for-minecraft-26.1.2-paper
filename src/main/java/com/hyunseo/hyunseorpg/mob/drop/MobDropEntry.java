package com.hyunseo.hyunseorpg.mob.drop;

import org.bukkit.Material;

public record MobDropEntry(
        String itemId,
        Material material,
        String displayName,
        double chance,
        int minAmount,
        int maxAmount,
        boolean bossOnly
) {
    public MobDropEntry {
        chance = Math.max(0.0D, Math.min(1.0D, chance));
        minAmount = Math.max(1, minAmount);
        maxAmount = Math.max(minAmount, maxAmount);
    }
}
