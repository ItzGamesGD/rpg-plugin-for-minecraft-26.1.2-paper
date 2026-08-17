package com.hyunseo.hyunseorpg.mob;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class MobService {
    private final ConfigService configService;
    private final MobTagService mobTagService;
    private final MobRegistry mobRegistry;
    private final MobAbilityRegistry mobAbilityRegistry;
    private final MobLevelScalingService mobLevelScalingService;

    public MobService(
            ConfigService configService,
            MobTagService mobTagService,
            MobRegistry mobRegistry,
            MobAbilityRegistry mobAbilityRegistry,
            MobLevelScalingService mobLevelScalingService
    ) {
        this.configService = configService;
        this.mobTagService = mobTagService;
        this.mobRegistry = mobRegistry;
        this.mobAbilityRegistry = mobAbilityRegistry;
        this.mobLevelScalingService = mobLevelScalingService;
    }

    public boolean isRpgMob(LivingEntity entity) {
        return mobTagService.isRpgMob(entity);
    }

    public boolean hasTag(LivingEntity entity, String tag) {
        return mobTagService.hasTag(entity, tag);
    }

    public LivingEntity spawnTaggedMob(Location location, EntityType entityType, String categoryTag) {
        return spawnTaggedMob(location, entityType, categoryTag, 1);
    }

    public LivingEntity spawnTaggedMob(Location location, EntityType entityType, String categoryTag, int level) {
        World world = location.getWorld();
        if (world == null || !entityType.isAlive()) {
            throw new IllegalArgumentException("Cannot spawn living mob type: " + entityType);
        }

        Entity spawned = world.spawnEntity(location, entityType);
        if (!(spawned instanceof LivingEntity livingEntity)) {
            spawned.remove();
            throw new IllegalArgumentException("Entity type is not living: " + entityType);
        }

        markAsRpgMob(
                livingEntity,
                Math.max(1, level),
                entityType.name().toLowerCase() + "_" + normalizeCategory(categoryTag),
                toDisplayName(entityType.name()),
                List.of(categoryTag == null || categoryTag.isBlank() ? "rpg" : categoryTag)
        );
        return livingEntity;
    }

    public Optional<LivingEntity> spawnCustomMob(Location location, String mobId) {
        return spawnCustomMob(location, mobId, null, "PLUGIN");
    }

    public Optional<LivingEntity> spawnCustomMob(Location location, String mobId, Integer levelOverride) {
        return spawnCustomMob(location, mobId, levelOverride, "PLUGIN");
    }

    public Optional<LivingEntity> spawnCustomMob(
            Location location,
            String mobId,
            Integer levelOverride,
            String spawnSource
    ) {
        Optional<MobData> mobData = mobRegistry.get(mobId);
        if (mobData.isEmpty()) {
            return Optional.empty();
        }

        MobData data = mobData.get();
        LivingEntity entity = spawnTaggedMob(location, data.vanillaType(), "rpg");
        int level = levelOverride == null ? data.level() : Math.max(1, levelOverride);
        markAsRpgMob(entity, level, data.mobId(), data.displayName(), data.tags());
        mobTagService.markCustomMob(entity, data.mobId(), spawnSource, data.dropTableId());
        applyCustomAttributes(entity, data.attributes());
        return Optional.of(entity);
    }

    public void markAsNaturalRpgMob(LivingEntity entity, int level) {
        int safeLevel = Math.max(1, level);
        markAsRpgMob(
                entity,
                safeLevel,
                entity.getType().name().toLowerCase() + "_natural",
                toDisplayName(entity.getType().name()),
                List.of(MobTagService.RPG_MOB_TAG)
        );
        mobLevelScalingService.applyNaturalScaling(entity, safeLevel);
    }

    public void markAsZoneRpgMob(LivingEntity entity, MobSpawnZone zone) {
        int safeLevel = Math.max(1, zone.mobLevel());
        markAsRpgMob(
                entity,
                safeLevel,
                zone.mobIdPrefix() + "_" + entity.getType().name().toLowerCase(),
                toDisplayName(entity.getType().name()),
                zone.tags().isEmpty() ? List.of(MobTagService.RPG_MOB_TAG) : zone.tags()
        );
        mobLevelScalingService.applyNaturalScaling(entity, safeLevel);
    }

    public void markAsRpgMob(LivingEntity entity, int level, String mobId, String displayName, Collection<String> tags) {
        mobTagService.addTag(entity, MobTagService.RPG_MOB_TAG);
        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                mobTagService.addTag(entity, tag);
            }
        }
        mobTagService.setMobLevel(entity, level);
        mobTagService.setMobId(entity, mobId);
        mobTagService.setMobDisplayName(entity, displayName);
        mobTagService.syncScoreboardTags(entity);
    }

    public Optional<LivingEntity> findLookedAtOrNearestTaggedMob(Location origin, double radius) {
        return getNearbyLivingEntities(origin, radius).stream()
                .filter(entity -> mobTagService.isRpgMob(entity) || !mobTagService.getTags(entity).isEmpty())
                .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(origin)));
    }

    public MobTagService getMobTagService() {
        return mobTagService;
    }

    public MobRegistry getMobRegistry() {
        return mobRegistry;
    }

    public MobAbilityRegistry getMobAbilityRegistry() {
        return mobAbilityRegistry;
    }

    public Optional<MobData> getMobData(LivingEntity entity) {
        String mobId = mobTagService.getMobId(entity);
        if (mobId.isBlank()) {
            return Optional.empty();
        }
        return mobRegistry.get(mobId);
    }

    public String getMobId(LivingEntity entity) {
        return mobTagService.getMobId(entity);
    }

    public boolean isBossMob(LivingEntity entity) {
        return getMobData(entity).map(MobData::boss).orElseGet(() -> mobTagService.hasTag(entity, "boss"));
    }

    public boolean isEliteMob(LivingEntity entity) {
        return getMobData(entity).map(MobData::elite).orElseGet(() -> mobTagService.hasTag(entity, "elite"));
    }

    public Optional<MobAbilityProfile> getAbilityProfile(LivingEntity entity) {
        return getMobData(entity)
                .map(MobData::abilityProfileId)
                .flatMap(mobAbilityRegistry::get);
    }

    public long getBaseExpReward(LivingEntity entity) {
        Optional<MobData> mobData = getMobData(entity);
        if (mobData.isPresent() && mobData.get().baseExp() > 0L) {
            return mobData.get().baseExp();
        }

        int level = Math.max(1, mobTagService.getMobLevel(entity));
        if (isRpgMob(entity)) {
            return Math.max(0L, (long) configService.getMobsInt("reward.rpg-base-exp", 5) * level);
        }
        return Math.max(0L, configService.getMobsInt("reward.normal-base-exp", 1));
    }

    public long getClassExpReward(LivingEntity entity) {
        if (!isRpgMob(entity)) {
            return 0L;
        }

        Optional<MobData> mobData = getMobData(entity);
        if (mobData.isPresent() && mobData.get().classExp() > 0L) {
            return mobData.get().classExp();
        }

        int level = Math.max(1, mobTagService.getMobLevel(entity));
        return Math.max(0L, (long) configService.getMobsInt("reward.rpg-class-exp", 3) * level);
    }

    public long getCoinReward(LivingEntity entity) {
        Optional<MobData> mobData = getMobData(entity);
        if (mobData.isPresent() && mobData.get().coinReward() > 0L) {
            return mobData.get().coinReward();
        }
        if (!isRpgMob(entity)) {
            return Math.max(0L, configService.getMobsInt("reward.normal-coins", 0));
        }

        int level = Math.max(1, mobTagService.getMobLevel(entity));
        long baseCoins = Math.max(0L, configService.getMobsInt("reward.rpg-coin-base", 1));
        double coinsPerLevel = Math.max(0.0D, configService.getMobsDouble("reward.rpg-coin-per-level", 0.5D));
        return Math.max(0L, baseCoins + Math.round(coinsPerLevel * level));
    }

    public boolean isRewardableMob(LivingEntity entity) {
        return isManageableRpgCandidate(entity);
    }

    public boolean isManageableRpgCandidate(LivingEntity entity) {
        if (!(entity instanceof Mob) || entity instanceof Animals) {
            return false;
        }
        return isManageableRpgCandidate(entity.getType());
    }

    public boolean isManageableRpgCandidate(EntityType entityType) {
        return entityType != EntityType.PLAYER
                && entityType != EntityType.VILLAGER
                && entityType != EntityType.WANDERING_TRADER
                && !isPassiveAnimal(entityType);
    }

    private boolean isPassiveAnimal(EntityType entityType) {
        return switch (entityType) {
            case COW, MOOSHROOM, PIG, SHEEP, CHICKEN, RABBIT, GOAT, HORSE,
                    DONKEY, MULE, CAMEL, LLAMA, TRADER_LLAMA, WOLF, CAT,
                    OCELOT, PARROT, TURTLE, BEE, FOX, AXOLOTL, FROG,
                    SNIFFER, ARMADILLO -> true;
            default -> false;
        };
    }

    private List<LivingEntity> getNearbyLivingEntities(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) {
            return List.of();
        }

        return world.getNearbyEntities(center, radius, radius, radius).stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .toList();
    }

    private void ensureDefaults(LivingEntity entity) {
        if (mobTagService.getMobLevel(entity) < 1) {
            mobTagService.setMobLevel(entity, 1);
        }
        if (mobTagService.getMobId(entity).isBlank()) {
            mobTagService.setMobId(entity, entity.getType().name().toLowerCase() + "_rpg");
        }
        mobTagService.setMobDisplayName(entity, toDisplayName(entity.getType().name()));
    }

    private void applyCustomAttributes(LivingEntity entity, MobAttributeData attributes) {
        if (attributes == null) {
            return;
        }

        if (attributes.hasMaxHealth()) {
            AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(attributes.maxHealth());
                entity.setHealth(Math.min(attributes.maxHealth(), maxHealth.getValue()));
            }
        }
        if (attributes.hasMovementSpeed()) {
            AttributeInstance movementSpeed = entity.getAttribute(Attribute.MOVEMENT_SPEED);
            if (movementSpeed != null) {
                double value = attributes.multipliesMovementSpeed()
                        ? movementSpeed.getBaseValue() * attributes.movementSpeedValue()
                        : attributes.movementSpeedValue();
                if (value > 0.0D) {
                    movementSpeed.setBaseValue(value);
                }
            }
        }
        if (attributes.hasAttackDamage()) {
            setAttribute(entity, Attribute.ATTACK_DAMAGE, attributes.attackDamage());
        }
        if (attributes.hasArmor()) {
            setAttribute(entity, Attribute.ARMOR, attributes.armor());
        }
        if (attributes.hasScale()) {
            setAttribute(entity, Attribute.SCALE, attributes.scale());
        }
        if (attributes.hasKnockbackResistance()) {
            setAttribute(entity, Attribute.KNOCKBACK_RESISTANCE, attributes.knockbackResistance());
        }
    }

    private void setAttribute(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance != null) {
            attributeInstance.setBaseValue(value);
        }
    }

    private String normalizeCategory(String categoryTag) {
        if (categoryTag == null || categoryTag.isBlank()) {
            return "rpg";
        }
        return categoryTag.trim().toLowerCase();
    }

    private String toDisplayName(String entityTypeName) {
        String lower = entityTypeName.toLowerCase().replace("_", " ");
        String[] parts = lower.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
