package com.hyunseo.hyunseorpg.quest.availability;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.mob.MobData;
import com.hyunseo.hyunseorpg.mob.MobRegistry;
import com.hyunseo.hyunseorpg.mob.MonsterSpawnData;
import com.hyunseo.hyunseorpg.mob.MonsterSpawnRegistry;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Shared eligibility boundary between natural spawning, discovery, and generated quest targets. */
public final class MonsterEligibilityService {
    private static final Set<EntityType> VANILLA_BOSS_EXCLUSIONS = EnumSet.of(
            EntityType.MOOSHROOM, EntityType.WITHER, EntityType.ENDER_DRAGON,
            EntityType.WARDEN, EntityType.ELDER_GUARDIAN);
    private static final Set<String> EXCLUDED_TAGS = Set.of(
            "boss", "mini_boss", "elite", "rare", "event_only", "summon_only",
            "admin_only", "npc", "tamed_only", "quest_excluded");

    private final ConfigService config;
    private final PlayerDataService playerData;
    private final MobRegistry mobs;
    private final MonsterSpawnRegistry spawns;
    private final PlayerDiscoveryService discovery;

    public MonsterEligibilityService(ConfigService config, PlayerDataService playerData, MobRegistry mobs,
                                     MonsterSpawnRegistry spawns, PlayerDiscoveryService discovery) {
        this.config = config;
        this.playerData = playerData;
        this.mobs = mobs;
        this.spawns = spawns;
        this.discovery = discovery;
    }

    public MonsterEligibilityResult checkCustom(Player player, String rawId, MonsterEligibilityContext context) {
        String id = normalize(rawId);
        MobData mob = mobs.get(id).orElse(null);
        if (mob == null) {
            return MonsterEligibilityResult.denied(AvailabilityReason.MISSING_DEFINITION, id, id,
                    QuestTargetSource.HYUNSEORPG_CUSTOM_MOB, "mobs.yml definition is missing");
        }
        if (mob.boss()) return deny(AvailabilityReason.BOSS, mob, "boss mob");
        if (mob.elite()) return deny(AvailabilityReason.MINI_BOSS, mob, "elite mob");
        for (String rawTag : mob.tags()) {
            String tag = normalize(rawTag);
            if (!EXCLUDED_TAGS.contains(tag)) continue;
            AvailabilityReason reason = switch (tag) {
                case "rare" -> AvailabilityReason.RARE;
                case "event_only" -> AvailabilityReason.EVENT_ONLY;
                case "summon_only" -> AvailabilityReason.SUMMON_ONLY;
                case "admin_only" -> AvailabilityReason.ADMIN_ONLY;
                case "npc" -> AvailabilityReason.NPC;
                case "tamed_only" -> AvailabilityReason.TAMED_ONLY;
                case "quest_excluded" -> AvailabilityReason.QUEST_EXCLUDED;
                case "mini_boss", "elite" -> AvailabilityReason.MINI_BOSS;
                default -> AvailabilityReason.BOSS;
            };
            return deny(reason, mob, "tag=" + tag);
        }
        if (mobs.getSection(id).map(section -> !section.getBoolean("quest-eligible", true)).orElse(false)) {
            return deny(AvailabilityReason.QUEST_EXCLUDED, mob, "quest-eligible=false");
        }

        MonsterSpawnData spawn = spawns.get(id).orElse(null);
        if (spawn == null || !spawn.spawnEnabled() || (!spawn.isReplacement() && !spawn.isAdditive())) {
            return deny(AvailabilityReason.NOT_NATURALLY_SPAWNABLE, mob, "no enabled natural spawn rule");
        }
        if (player != null) {
            int level = Math.max(1, playerData.getOrLoad(player).getBaseLevel());
            if (!spawn.acceptsLevel(level)) {
                return deny(AvailabilityReason.LEVEL_LOCKED, mob, "required=" + spawn.minLevel() + "-" + spawn.maxLevel());
            }
            if (!spawn.acceptsWorld(player.getWorld().getName())) {
                return deny(AvailabilityReason.WORLD_UNAVAILABLE, mob, "world=" + player.getWorld().getName());
            }
            boolean requireDiscovery = config.getQuestsBoolean("auto.eligibility.custom-monsters.require-discovery", true);
            if (context == MonsterEligibilityContext.QUEST_TARGET && requireDiscovery && !discovery.hasDiscovered(player, id)) {
                return deny(AvailabilityReason.NOT_DISCOVERED, mob, "discovery required");
            }
        }
        return MonsterEligibilityResult.allowed(id, mob.displayName(), QuestTargetSource.HYUNSEORPG_CUSTOM_MOB,
                spawn.mode() + " level=" + spawn.minLevel() + "-" + spawn.maxLevel());
    }

    public MonsterEligibilityResult checkVanilla(Player player, EntityType type, MonsterEligibilityContext context) {
        if (type == null || !type.isAlive()) {
            return MonsterEligibilityResult.denied(AvailabilityReason.MISSING_DEFINITION, "", "",
                    QuestTargetSource.VANILLA_MOB, "not a living entity type");
        }
        String id = type.name().toLowerCase(Locale.ROOT);
        if (VANILLA_BOSS_EXCLUSIONS.contains(type) || configuredVanillaExclusion(type)) {
            return MonsterEligibilityResult.denied(type == EntityType.WITHER || type == EntityType.ENDER_DRAGON
                    ? AvailabilityReason.BOSS : AvailabilityReason.QUEST_EXCLUDED, id, display(type),
                    QuestTargetSource.VANILLA_MOB, "vanilla exclusion");
        }
        if (context == MonsterEligibilityContext.QUEST_TARGET && !isConfiguredVanillaCandidate(type)) {
            return MonsterEligibilityResult.denied(AvailabilityReason.NOT_NATURALLY_SPAWNABLE, id, display(type),
                    QuestTargetSource.VANILLA_MOB, "not in auto.eligibility.vanilla-mobs");
        }
        return MonsterEligibilityResult.allowed(id, display(type), QuestTargetSource.VANILLA_MOB, "vanilla natural target");
    }

    public List<QuestTargetCandidate> eligibleHuntTargets(Player player) {
        List<QuestTargetCandidate> candidates = new ArrayList<>();
        for (MobData mob : mobs.getAll()) {
            MonsterEligibilityResult result = checkCustom(player, mob.mobId(), MonsterEligibilityContext.QUEST_TARGET);
            if (result.eligible()) candidates.add(new QuestTargetCandidate(result.monsterId(), result.displayName(),
                    result.source(), Math.max(1, mob.level()), result.detail()));
        }
        for (String rawType : config.getQuestsStringList("auto.eligibility.vanilla-mobs")) {
            try {
                EntityType type = EntityType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
                MonsterEligibilityResult result = checkVanilla(player, type, MonsterEligibilityContext.QUEST_TARGET);
                if (result.eligible()) candidates.add(new QuestTargetCandidate(result.monsterId(), result.displayName(),
                        result.source(), 1, result.detail()));
            } catch (IllegalArgumentException ignored) {
                // Doctor reports invalid configured entity names; generation never accepts them.
            }
        }
        return List.copyOf(candidates);
    }

    private boolean configuredVanillaExclusion(EntityType type) {
        return config.getQuestsStringList("auto.exclusions.vanilla-mobs").stream()
                .anyMatch(value -> value.equalsIgnoreCase(type.name()));
    }

    private boolean isConfiguredVanillaCandidate(EntityType type) {
        return config.getQuestsStringList("auto.eligibility.vanilla-mobs").stream()
                .anyMatch(value -> value.equalsIgnoreCase(type.name()));
    }

    private MonsterEligibilityResult deny(AvailabilityReason reason, MobData mob, String detail) {
        return MonsterEligibilityResult.denied(reason, mob.mobId(), mob.displayName(),
                QuestTargetSource.HYUNSEORPG_CUSTOM_MOB, detail);
    }

    private String display(EntityType type) {
        return type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
