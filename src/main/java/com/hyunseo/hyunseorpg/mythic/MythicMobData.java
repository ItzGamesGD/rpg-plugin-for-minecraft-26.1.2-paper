package com.hyunseo.hyunseorpg.mythic;

import com.hyunseo.hyunseorpg.mob.drop.MobDropEntry;

import java.util.List;

public record MythicMobData(
        String mobId,
        String displayName,
        int level,
        String category,
        long coinReward,
        long expReward,
        long classExpReward,
        boolean boss,
        boolean elite,
        List<MobDropEntry> drops
) {
    public MythicMobData {
        mobId = mobId == null ? "" : mobId.trim().toLowerCase();
        displayName = displayName == null || displayName.isBlank() ? mobId : displayName;
        level = Math.max(1, level);
        category = category == null ? "" : category.trim().toLowerCase();
        coinReward = Math.max(0L, coinReward);
        expReward = Math.max(0L, expReward);
        classExpReward = Math.max(0L, classExpReward);
        drops = List.copyOf(drops == null ? List.of() : drops);
    }
}
