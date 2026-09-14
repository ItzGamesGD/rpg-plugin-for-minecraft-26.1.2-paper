package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.equipment.EquipmentLoreBuilder;
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
    private final NamespacedKey schemaKey;
    private EnhancementClassificationService classification;

    public EquipmentEnhancementService(JavaPlugin plugin, RPGItemService itemService, EnhancementRegistry registry) {
        this.itemService = itemService;
        this.registry = registry;
        this.levelKey = new NamespacedKey(plugin, "enhancement_level");
        this.schemaKey = new NamespacedKey(plugin, "equipment_schema_version");
    }

    public void setClassificationService(EnhancementClassificationService classification) {
        this.classification = classification;
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
        int maximum = Math.max(0, getMaximumLevel(item));
        return level == null ? 0 : Math.min(maximum, Math.max(0, level));
    }

    public int getDataSchemaVersion(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(schemaKey, PersistentDataType.INTEGER, 0);
    }

    public int getMaximumLevel(ItemStack item) {
        return classification == null ? registry.getConfiguredMaximumLevel() : classification.maximumLevel(item);
    }

    public Optional<EnhancementLevelData> getNextLevel(ItemStack item) {
        if (classification != null && !classification.classify(item).enhanceable()) return Optional.empty();
        return getProfileId(item).flatMap(profile -> registry.getNextLevel(profile, getLevel(item), getMaximumLevel(item)));
    }

    public int getXpLevelCost(int targetLevel) { return registry.getXpLevelCost(targetLevel); }

    public boolean isRequiredStone(ItemStack item) {
        return itemService.isItem(item, registry.getRequiredStoneItemId());
    }

    public double getAttackBonus(ItemStack item) {
        if (classification != null && !classification.classify(item).enhanceable()) return 0.0D;
        return getProfileId(item)
                .filter(profile -> "ATTACK_DAMAGE".equalsIgnoreCase(registry.getTarget(profile)))
                .map(profile -> registry.getEffectValue(profile, getLevel(item)))
                .orElse(0.0D);
    }

    public double getAxeAttackBonus(ItemStack item) {
        if (item == null || !item.getType().name().endsWith("_AXE")) return 0.0D;
        if (classification != null && !classification.classify(item).enhanceable()) return 0.0D;
        return registry.getAxeCombatEffectValue(getLevel(item));
    }

    public double getToolEfficiencyBonus(ItemStack item) {
        if (classification != null && !classification.classify(item).enhanceable()) return 0.0D;
        return getProfileId(item)
                .filter(profile -> "TOOL_EFFICIENCY".equalsIgnoreCase(registry.getTarget(profile)))
                .map(profile -> registry.getEffectValue(profile, getLevel(item)))
                .orElse(0.0D);
    }

    /** Legacy armor hook retained for CombatService compatibility. */
    public double getDamageReductionBonus(ItemStack item) {
        if (classification != null && !classification.classify(item).enhanceable()) return 0.0D;
        return getProfileId(item)
                .filter(profile -> "damage-reduction".equalsIgnoreCase(registry.getEffectType(profile)))
                .map(profile -> registry.getEffectValue(profile, getLevel(item)))
                .orElse(0.0D);
    }

    public void applySuccessfulEnhancement(ItemStack item, EnhancementLevelData nextLevel) {
        if (classification != null && !classification.classify(item).enhanceable()) return;
        String profile = getProfileId(item).orElseThrow();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        ensureSchema(meta, item);
        meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, nextLevel.level());
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
        if (classification != null && !classification.classify(item).enhanceable()) return getLevel(item);
        String profile = getProfileId(item).orElse(null);
        if (profile == null) return getLevel(item);
        int target = Math.max(0, Math.min(getMaximumLevel(item), requestedLevel));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return getLevel(item);
        ensureSchema(meta, item);
        meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, target);
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
            meta.getPersistentDataContainer().set(schemaKey, PersistentDataType.INTEGER, DATA_SCHEMA_VERSION);
        }
    }

    private String formatValue(double value) {
        return Math.round(value * 1000.0D) / 1000.0D + (value > 0.0D && value < 1.0D ? "%" : "");
    }
}
