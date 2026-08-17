package com.hyunseo.hyunseorpg.equipment;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentEnhancementService;
import com.hyunseo.hyunseorpg.farming.FarmingStage;
import org.bukkit.inventory.ItemStack;

/** Provides cached farming-only modifiers for direct custom-crop harvests. */
public final class HoeHarvestModifierService {
    private final ConfigService config;
    private final EquipmentTierService tiers;
    private final EquipmentEnhancementService enhancement;

    public HoeHarvestModifierService(ConfigService config, EquipmentTierService tiers,
                                     EquipmentEnhancementService enhancement) {
        this.config = config;
        this.tiers = tiers;
        this.enhancement = enhancement;
    }

    public HoeHarvestModifiers resolve(ItemStack hoe, FarmingStage stage) {
        if (!isHoe(hoe)) return HoeHarvestModifiers.neutral();
        int level = Math.max(0, enhancement.getLevel(hoe));
        double perLevel = levelPoint("durability-save-chance", level,
                config.getFarmingProgressionDouble("hoe.durability-save.enhancement-per-level", 0.0D));
        double cap = clamp(config.getFarmingHoeEnhancementDouble(
                "limits.maximum-durability-save-chance", config.getFarmingProgressionDouble(
                        "hoe.durability-save.maximum-chance", 1.0D)));
        double saleBonus = levelPoint("quality-sale-bonus", level, 0.0D);
        double saleCap = clamp(config.getFarmingHoeEnhancementDouble(
                "limits.maximum-quality-sale-bonus", 0.0D));
        // Quality density belongs to item-local farming promotion. Enhancement
        // only protects durability and changes the later sale calculation.
        return new HoeHarvestModifiers(durabilitySaveChance(perLevel, cap),
                Math.min(saleCap, Math.max(0.0D, saleBonus)));
    }

    public double salePriceBonus(ItemStack hoe) {
        // This is intentionally evaluated at sale time from the currently held hoe.
        // Harvested items do not carry hoe provenance, so no stack-fragmenting PDC is needed.
        return resolve(hoe, null).salePriceBonus();
    }

    private double levelPoint(String field, int level, double fallback) {
        java.util.List<Integer> points = config.getFarmingHoeEnhancementKeys("levels").stream()
                .map(this::parseLevel)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .toList();
        if (points.isEmpty()) return Math.max(0.0D, finite(fallback));
        int clamped = Math.max(points.get(0), Math.min(points.get(points.size() - 1), level));
        int lower = points.get(0);
        int upper = points.get(points.size() - 1);
        for (int point : points) {
            if (point <= clamped) lower = point;
            if (point >= clamped) {
                upper = point;
                break;
            }
        }
        double lowerValue = finite(config.getFarmingHoeEnhancementDouble(
                "levels." + lower + "." + field, fallback));
        double upperValue = finite(config.getFarmingHoeEnhancementDouble(
                "levels." + upper + "." + field, lowerValue));
        if (lower == upper) return lowerValue;
        double ratio = (clamped - lower) / (double) (upper - lower);
        return lowerValue + (upperValue - lowerValue) * ratio;
    }

    private Integer parseLevel(String raw) {
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public boolean isHoe(ItemStack item) {
        return item != null && !item.getType().isAir()
                && tiers.getCategory(item) == EquipmentTierService.Category.TOOL
                && item.getType().name().endsWith("_HOE");
    }

    private double finite(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }

    private double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(1.0D, value)) : 1.0D;
    }

    public static double durabilitySaveChance(double rawChance, double cap) {
        if (!Double.isFinite(rawChance) || !Double.isFinite(cap)) return 0.0D;
        return Math.min(Math.max(0.0D, cap), Math.max(0.0D, rawChance));
    }

    public record HoeHarvestModifiers(double durabilitySaveChance, double salePriceBonus) {
        public static HoeHarvestModifiers neutral() {
            return new HoeHarvestModifiers(0.0D, 0.0D);
        }
    }
}
