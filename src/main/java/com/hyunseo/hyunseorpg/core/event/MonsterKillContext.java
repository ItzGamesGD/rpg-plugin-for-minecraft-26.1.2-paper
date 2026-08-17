package com.hyunseo.hyunseorpg.core.event;

import com.hyunseo.hyunseorpg.mob.MobService;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Stable monster identity passed to rewards, quests and future collection systems. */
public record MonsterKillContext(
        String mobId,
        String customMobId,
        int level,
        String spawnSource,
        String dropTableId,
        EntityType vanillaType,
        Set<String> tags,
        boolean customMob,
        boolean boss,
        boolean elite
) {
    public MonsterKillContext {
        mobId = normalize(mobId);
        customMobId = normalize(customMobId);
        level = Math.max(1, level);
        spawnSource = normalize(spawnSource);
        dropTableId = normalize(dropTableId);
        tags = Set.copyOf(tags == null ? Set.of() : new LinkedHashSet<>(tags));
    }

    public static MonsterKillContext from(LivingEntity entity, MobService mobService) {
        return new MonsterKillContext(
                mobService.getMobId(entity),
                mobService.getMobTagService().getCustomMobId(entity),
                mobService.getMobTagService().getMobLevel(entity),
                mobService.getMobTagService().getSpawnSource(entity),
                mobService.getMobTagService().getDropTableId(entity),
                entity.getType(),
                mobService.getMobTagService().getTags(entity),
                mobService.getMobTagService().isCustomMob(entity),
                mobService.isBossMob(entity),
                mobService.isEliteMob(entity)
        );
    }

    public static MonsterKillContext mythic(
            LivingEntity entity,
            String mobId,
            int level,
            boolean boss,
            boolean elite
    ) {
        return new MonsterKillContext(mobId, mobId, level, "MYTHIC", "", entity.getType(),
                Set.of("rpg_mob", "mythic"), true, boss, elite);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
