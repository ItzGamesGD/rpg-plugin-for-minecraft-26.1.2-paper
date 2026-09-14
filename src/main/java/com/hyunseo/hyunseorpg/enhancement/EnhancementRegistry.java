package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.Optional;

/** Resolves data-driven equipment growth rules from equipment-growth.yml. */
public final class EnhancementRegistry {
    private final ConfigService configService;

    public EnhancementRegistry(ConfigService configService) {
        this.configService = configService;
    }

    public Optional<String> findProfile(String itemId, Material material) {
        for (String rawId : configService.getEquipmentGrowthKeys("enhancement.profiles")) {
            String id = normalize(rawId);
            String path = "enhancement.profiles." + rawId;
            if (itemId != null && configService.getEquipmentGrowthStringList(path + ".custom-item-ids")
                    .stream().anyMatch(value -> value.equalsIgnoreCase(itemId))) {
                return Optional.of(id);
            }
            if (material != null && configService.getEquipmentGrowthStringList(path + ".materials")
                    .stream().anyMatch(value -> value.equalsIgnoreCase(material.name()))) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    public Optional<EnhancementLevelData> getNextLevel(String profileId, int currentLevel, int maxLevel) {
        int configuredMaximum = configService.getEquipmentGrowthInt("enhancement.max-level", 0);
        if (configuredMaximum > 0) maxLevel = Math.min(maxLevel, configuredMaximum);
        var transition = EnhancementCapPolicy.nextLevel(currentLevel, maxLevel);
        if (transition.isEmpty()) return Optional.empty();
        int next = transition.getAsInt();
        if (!hasGrowthProfile(profileId)) return Optional.empty();
        return Optional.of(new EnhancementLevelData(
                next,
                1,
                1.0D,
                getEffectValue(profileId, next)
        ));
    }

    public int getXpLevelCost(int targetLevel) {
        int cost = Math.max(1, configService.getEquipmentGrowthInt("enhancement.xp-level-cost.default", 1));
        ConfigurationSection levels = configService.getEquipmentGrowthSection("enhancement.xp-level-cost.levels");
        if (levels == null) return cost;
        int selectedThreshold = Integer.MIN_VALUE;
        for (String key : levels.getKeys(false)) {
            int threshold;
            try { threshold = Integer.parseInt(key); } catch (NumberFormatException ignored) { continue; }
            if (threshold <= targetLevel && threshold > selectedThreshold) {
                cost = Math.max(1, levels.getInt(key, cost));
                selectedThreshold = threshold;
            }
        }
        return cost;
    }

    public int getConfiguredMaximumLevel() {
        return Math.max(1, configService.getEquipmentGrowthInt("enhancement.max-level", 50));
    }

    public double getEffectValue(String profileId, int level) {
        return Math.max(0.0D, configService.getEquipmentGrowthDouble("enhancement.profiles." + profileId + ".per-level", 0.0D))
                * Math.max(0, level);
    }

    public double getAxeCombatEffectValue(int level) {
        double perLevel = configService.getEquipmentGrowthDouble("enhancement.axe-combat.per-level",
                configService.getEquipmentGrowthDouble("enhancement.profiles.melee.per-level", 1.0D));
        return Math.max(0.0D, perLevel) * Math.max(0, level);
    }

    public String getEffectName(String profileId) {
        return configService.getEquipmentGrowthString("enhancement.profiles." + profileId + ".target", profileId);
    }

    public String getEffectType(String profileId) {
        String target = normalize(configService.getEquipmentGrowthString("enhancement.profiles." + profileId + ".target", ""));
        return switch (target) {
            case "attack_damage" -> "attack-bonus";
            case "tool_efficiency" -> "gathering-speed";
            case "damage_reduction" -> "damage-reduction";
            default -> "none";
        };
    }

    public String getTarget(String profileId) {
        return configService.getEquipmentGrowthString("enhancement.profiles." + profileId + ".target", "");
    }

    /** Stage-2 compatibility methods: anvil enhancement is deterministic and coin-free. */
    public double getBaseSuccessChance(double normalizedProgress) { return 1.0D; }
    public double getCurrentSuccessChance(double normalizedProgress, int failCount) { return 1.0D; }
    public double getFailureBonus() { return 0.0D; }
    public int getCoinCost(double normalizedProgress) { return 0; }

    public String getRequiredStoneItemId() {
        return normalize(configService.getEquipmentGrowthString("enhancement.required-stone-item-id", "basic_upgrade_stone"));
    }

    private boolean hasGrowthProfile(String profileId) {
        return configService.getEquipmentGrowthKeys("enhancement.profiles").contains(profileId);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

}
