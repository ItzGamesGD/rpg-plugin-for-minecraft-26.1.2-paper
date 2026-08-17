package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        int next = currentLevel + 1;
        int configuredMaximum = configService.getEquipmentGrowthInt("enhancement.max-level", 0);
        if (configuredMaximum > 0) maxLevel = Math.min(maxLevel, configuredMaximum);
        if (next > maxLevel) return Optional.empty();

        if (!hasGrowthProfile(profileId)) return Optional.empty();
        double progress = normalizedProgress(next, maxLevel);
        return Optional.of(new EnhancementLevelData(
                next,
                getStoneCost(progress),
                getBaseSuccessChance(progress),
                getEffectValue(profileId, next)
        ));
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

    public double getBaseSuccessChance(double normalizedProgress) {
        if (configService.getEquipmentGrowthKeys("enhancement.profiles").isEmpty()) return 1.0D;
        double start = clamp(configService.getEquipmentGrowthDouble("enhancement.start-chance", 1.0D));
        double end = clamp(configService.getEquipmentGrowthDouble("enhancement.final-chance", 0.20D));
        double progress = clamp(normalizedProgress);
        return clamp(start - ((start - end) * progress));
    }

    public double getCurrentSuccessChance(double normalizedProgress, int failCount) {
        double bonus = Math.max(0.0D, configService.getEquipmentGrowthDouble("enhancement.fail-bonus", 0.04D));
        double maximum = clamp(configService.getEquipmentGrowthDouble("enhancement.maximum-chance", 1.0D));
        return Math.min(maximum, getBaseSuccessChance(normalizedProgress) + Math.max(0, failCount) * bonus);
    }

    public double getFailureBonus() {
        return Math.max(0.0D, configService.getEquipmentGrowthDouble("enhancement.fail-bonus", 0.04D));
    }

    /** Returns the coin fee for an enhancement attempt at the supplied progress. */
    public int getCoinCost(double normalizedProgress) {
        int initial = Math.max(0, configService.getEquipmentGrowthInt("enhancement.initial-coin-cost", 0));
        ConfigurationSection curve = configService.getEquipmentGrowthSection("enhancement.coin-cost-curve");
        if (curve == null) return initial;
        List<CostPoint> points = costPoints(curve);
        if (points.isEmpty()) return initial;
        int cost = points.stream()
                .filter(point -> point.amount() >= 0)
                .filter(point -> point.progress() + 0.000001D >= clamp(normalizedProgress))
                .min(Comparator.comparingDouble(CostPoint::progress))
                .map(CostPoint::amount)
                .orElseGet(() -> points.stream().mapToInt(CostPoint::amount).max().orElse(initial));
        return Math.max(0, cost);
    }

    public String getRequiredStoneItemId() {
        return normalize(configService.getEquipmentGrowthString("enhancement.required-stone-item-id", "basic_upgrade_stone"));
    }

    private boolean hasGrowthProfile(String profileId) {
        return configService.getEquipmentGrowthKeys("enhancement.profiles").contains(profileId);
    }

    private int getStoneCost(double progress) {
        int initialCost = Math.max(1, configService.getEquipmentGrowthInt("enhancement.initial-cost", 5));
        ConfigurationSection curve = configService.getEquipmentGrowthSection("enhancement.cost-curve");
        if (curve == null) return initialCost;
        List<CostPoint> points = costPoints(curve);
        if (points.isEmpty()) return initialCost;
        int cost = points.stream()
                .filter(point -> point.amount() > 0)
                .filter(point -> point.progress() + 0.000001D >= progress)
                .min(Comparator.comparingDouble(CostPoint::progress))
                .map(CostPoint::amount)
                .orElseGet(() -> points.stream().mapToInt(CostPoint::amount).max().orElse(0));
        return cost > 0 ? cost : initialCost;
    }

    /**
     * Old external YAML files occasionally parsed unquoted decimal keys such
     * as 0.166667 as a nested 0 -> 166667 section. Read that shape safely so
     * the configured curve is not silently replaced by the initial cost.
     */
    private List<CostPoint> costPoints(ConfigurationSection curve) {
        List<CostPoint> points = new ArrayList<>();
        for (String key : curve.getKeys(false)) {
            Object raw = curve.getValues(false).get(key);
            if (raw instanceof ConfigurationSection nested) {
                for (String child : nested.getKeys(false)) {
                    int amount = readCurveAmount(nested, child);
                    if (amount > 0) points.add(new CostPoint(parseDouble(key + "." + child), amount));
                }
                continue;
            }
            int amount = readCurveAmount(curve, key);
            if (amount > 0) points.add(new CostPoint(parseDouble(key), amount));
        }
        return points;
    }

    private int readCurveAmount(ConfigurationSection curve, String key) {
        Object raw = curve.getValues(false).get(key);
        if (raw instanceof Number number) return Math.max(0, number.intValue());
        try { return Math.max(0, Integer.parseInt(String.valueOf(raw))); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private double normalizedProgress(int level, int maxLevel) {
        return maxLevel <= 0 ? 1.0D : (double) level / (double) maxLevel;
    }

    private double parseDouble(String value) {
        try { return Double.parseDouble(value); } catch (NumberFormatException ignored) { return 1.0D; }
    }

    private double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record CostPoint(double progress, int amount) { }
}
