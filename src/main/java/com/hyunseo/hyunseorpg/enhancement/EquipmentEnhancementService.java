package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.equipment.EquipmentLoreBuilder;
import com.hyunseo.hyunseorpg.equipment.EquipmentGrowthPolicy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

/** Stores equipment growth causes on PDC and recalculates values from current YAML rules. */
public final class EquipmentEnhancementService {
    private static final int DATA_SCHEMA_VERSION = 1;
    private static final String LORE_PREFIX = "강화: ";
    private final RPGItemService itemService;
    private final EnhancementRegistry registry;
    private final NamespacedKey levelKey;
    private final NamespacedKey failCountKey;
    private final NamespacedKey schemaKey;
    private EquipmentPromotionService promotionService;
    private EquipmentGrowthPolicy growthPolicy;

    public EquipmentEnhancementService(JavaPlugin plugin, RPGItemService itemService, EnhancementRegistry registry) {
        this.itemService = itemService;
        this.registry = registry;
        this.levelKey = new NamespacedKey(plugin, "enhancement_level");
        this.failCountKey = new NamespacedKey(plugin, "enhancement_fail_count");
        this.schemaKey = new NamespacedKey(plugin, "equipment_schema_version");
    }

    public void setPromotionService(EquipmentPromotionService promotionService) {
        this.promotionService = promotionService;
    }

    public void setGrowthPolicy(EquipmentGrowthPolicy growthPolicy) {
        this.growthPolicy = growthPolicy;
    }

    public Optional<String> getProfileId(ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        return registry.findProfile(itemService.getItemId(item).orElse(null), item.getType());
    }

    public Optional<String> getEnhanceableItemId(ItemStack item) {
        return getProfileId(item).map(profile -> itemService.getItemId(item).orElse("vanilla:" + item.getType().name().toLowerCase()));
    }

    public int getLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer level = item.getItemMeta().getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        int maximum = Math.max(1, registry.getConfiguredMaximumLevel());
        return level == null ? 0 : Math.min(maximum, Math.max(0, level));
    }

    public int getFailCount(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer count = item.getItemMeta().getPersistentDataContainer().get(failCountKey, PersistentDataType.INTEGER);
        return count == null ? 0 : Math.min(1000, Math.max(0, count));
    }

    public int getDataSchemaVersion(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(schemaKey, PersistentDataType.INTEGER, 0);
    }

    public int getMaximumLevel(ItemStack item) {
        return promotionService == null
                ? registry.getConfiguredMaximumLevel()
                : promotionService.getEnhancementMaxLevel(item);
    }

    public Optional<EnhancementLevelData> getNextLevel(ItemStack item) {
        if (growthPolicy != null && !growthPolicy.canEnhance(item)) return Optional.empty();
        return getProfileId(item).flatMap(profile -> registry.getNextLevel(profile, getLevel(item), getMaximumLevel(item)));
    }

    public double getSuccessChance(ItemStack item, EnhancementLevelData nextLevel) {
        int maximum = Math.max(1, getMaximumLevel(item));
        return registry.getCurrentSuccessChance((double) nextLevel.level() / maximum, getFailCount(item));
    }

    public long getCoinCost(ItemStack item, EnhancementLevelData nextLevel) {
        if (nextLevel == null) return 0L;
        int maximum = Math.max(1, getMaximumLevel(item));
        return Math.max(0L, registry.getCoinCost((double) nextLevel.level() / maximum));
    }

    public double getFailureBonus() {
        return registry.getFailureBonus();
    }

    public boolean isRequiredStone(ItemStack item) {
        return itemService.isItem(item, registry.getRequiredStoneItemId());
    }

    public double getAttackBonus(ItemStack item) {
        if (growthPolicy != null && !growthPolicy.canEnhance(item)) return 0.0D;
        return getProfileId(item)
                .filter(profile -> "ATTACK_DAMAGE".equalsIgnoreCase(registry.getTarget(profile)))
                .map(profile -> registry.getEffectValue(profile, getLevel(item)))
                .orElse(0.0D);
    }

    public double getAxeAttackBonus(ItemStack item) {
        if (item == null || !item.getType().name().endsWith("_AXE")) return 0.0D;
        if (growthPolicy != null && !growthPolicy.canEnhance(item)) return 0.0D;
        return registry.getAxeCombatEffectValue(getLevel(item));
    }

    public double getToolEfficiencyBonus(ItemStack item) {
        if (growthPolicy != null && !growthPolicy.canEnhance(item)) return 0.0D;
        return getProfileId(item)
                .filter(profile -> "TOOL_EFFICIENCY".equalsIgnoreCase(registry.getTarget(profile)))
                .map(profile -> registry.getEffectValue(profile, getLevel(item)))
                .orElse(0.0D);
    }

    /** Legacy armor hook retained for CombatService compatibility. */
    public double getDamageReductionBonus(ItemStack item) {
        if (growthPolicy != null && !growthPolicy.canEnhance(item)) return 0.0D;
        return getProfileId(item)
                .filter(profile -> "damage-reduction".equalsIgnoreCase(registry.getEffectType(profile)))
                .map(profile -> registry.getEffectValue(profile, getLevel(item)))
                .orElse(0.0D);
    }

    public void recordFailure(ItemStack item) {
        if (growthPolicy != null && !growthPolicy.canEnhance(item)) return;
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return;
        ensureSchema(meta, item);
        int next = Math.min(1000, getFailCount(item) + 1);
        meta.getPersistentDataContainer().set(failCountKey, PersistentDataType.INTEGER, next);
        item.setItemMeta(meta);
    }

    public void applySuccessfulEnhancement(ItemStack item, EnhancementLevelData nextLevel) {
        if (growthPolicy != null && !growthPolicy.canEnhance(item)) return;
        String profile = getProfileId(item).orElseThrow();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        ensureSchema(meta, item);
        meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, nextLevel.level());
        meta.getPersistentDataContainer().set(failCountKey, PersistentDataType.INTEGER, 0);
        meta.lore(EquipmentLoreBuilder.from(meta)
                .removePlainPrefix(LORE_PREFIX)
                .add(Component.text(LORE_PREFIX + "+" + nextLevel.level() + " | " + registry.getEffectName(profile)
                                + " +" + formatValue(nextLevel.effectValue()), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false))
                .build());
        item.setItemMeta(meta);
    }

    /**
     * Admin/test helper for live verification. It uses the same lore and PDC
     * write path as a normal successful enhancement, but skips material, coin,
     * and chance checks.
     */
    public int forceEnhancementLevel(ItemStack item, int requestedLevel) {
        if (item == null || item.getType().isAir()) return 0;
        if (growthPolicy != null && !growthPolicy.canEnhance(item)) return getLevel(item);
        String profile = getProfileId(item).orElse(null);
        if (profile == null) return getLevel(item);
        int target = Math.max(0, Math.min(getMaximumLevel(item), requestedLevel));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return getLevel(item);
        ensureSchema(meta, item);
        meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, target);
        meta.getPersistentDataContainer().set(failCountKey, PersistentDataType.INTEGER, 0);
        meta.lore(EquipmentLoreBuilder.from(meta)
                .removePlainPrefix(LORE_PREFIX)
                .add(Component.text(LORE_PREFIX + "+" + target + " | " + registry.getEffectName(profile)
                                + " +" + formatValue(registry.getEffectValue(profile, target)), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false))
                .build());
        item.setItemMeta(meta);
        return target;
    }

    private void ensureSchema(ItemMeta meta, ItemStack item) {
        int current = meta.getPersistentDataContainer().getOrDefault(schemaKey, PersistentDataType.INTEGER, 0);
        if (current < DATA_SCHEMA_VERSION) {
            int oldLevel = meta.getPersistentDataContainer().getOrDefault(levelKey, PersistentDataType.INTEGER, 0);
            int newMaximum = Math.max(1, registry.getConfiguredMaximumLevel());
            // Existing numeric progress is retained. Only the read path clamps
            // invalid values to the current configured maximum.
            if (oldLevel < 0 || oldLevel > newMaximum) {
                meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER,
                        Math.max(0, Math.min(newMaximum, oldLevel)));
            }
            meta.getPersistentDataContainer().set(failCountKey, PersistentDataType.INTEGER,
                    meta.getPersistentDataContainer().getOrDefault(failCountKey, PersistentDataType.INTEGER, 0));
            meta.getPersistentDataContainer().set(schemaKey, PersistentDataType.INTEGER, DATA_SCHEMA_VERSION);
        }
    }

    private String formatValue(double value) {
        return Math.round(value * 1000.0D) / 1000.0D + (value > 0.0D && value < 1.0D ? "%" : "");
    }
}
