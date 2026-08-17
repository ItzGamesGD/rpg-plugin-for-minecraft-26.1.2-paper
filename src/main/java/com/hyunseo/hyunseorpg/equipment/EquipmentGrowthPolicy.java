package com.hyunseo.hyunseorpg.equipment;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentData;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

/** Central permission policy for enhancement, promotion, enchantment and repair. */
public final class EquipmentGrowthPolicy {
    private final ConfigService config;
    private final RPGItemService items;
    private final EquipmentTierService tiers;
    private final SpecialEquipmentRegistry specials;
    private final NamespacedKey gradeKey;

    public EquipmentGrowthPolicy(ConfigService config, RPGItemService items,
                                 EquipmentTierService tiers, SpecialEquipmentRegistry specials) {
        this.config = config;
        this.items = items;
        this.tiers = tiers;
        this.specials = specials;
        this.gradeKey = new NamespacedKey(config.getPlugin(), "equipment_grade");
    }

    public EquipmentGrade grade(ItemStack item) {
        if (item == null || item.getType().isAir()) return EquipmentGrade.UNSPECIFIED;
        if (item.hasItemMeta()) {
            Integer stored = item.getItemMeta().getPersistentDataContainer().get(gradeKey, PersistentDataType.INTEGER);
            EquipmentGrade resolved = EquipmentGrade.fromValue(stored == null ? 0 : stored);
            if (resolved.isSpecified()) return resolved;
        }
        String id = itemId(item);
        EquipmentGrade configured = EquipmentGrade.fromValue(
                config.getEquipmentGrowthInt("equipment-registry." + id + ".grade", 0));
        if (configured.isSpecified()) return configured;
        if (specials.get(id).isPresent()) return EquipmentGrade.GRADE_4;
        return EquipmentGrade.UNSPECIFIED;
    }

    public boolean isEndgame(ItemStack item) {
        return grade(item) == EquipmentGrade.GRADE_5
                || config.getEquipmentGrowthBoolean("equipment-registry." + itemId(item) + ".endgame", false);
    }

    public boolean canEnhance(ItemStack item) {
        return tiers.isSupported(item) && tiers.getMaxEnhancement(item) > 0
                && !isEndgame(item) && special(item) == null
                && grade(item) != EquipmentGrade.GRADE_4;
    }

    public boolean canPromote(ItemStack item) {
        return tiers.isSupported(item) && tiers.getMaxPromotionStage(item) > 0
                && !isEndgame(item) && special(item) == null
                && grade(item) != EquipmentGrade.GRADE_4;
    }

    public boolean canEnchant(ItemStack item) {
        if (!tiers.isSupported(item) || isEndgame(item)) return false;
        SpecialEquipmentData data = special(item);
        return data == null || data.allowCustomEnchants();
    }

    /**
     * Equipment with no ordinary enhancement or promotion path receives its
     * first custom-enchant slot directly, unless its own policy forbids it.
     */
    public boolean canEnchantDirectly(ItemStack item) {
        return canEnchant(item) && !canEnhance(item) && !canPromote(item);
    }

    public boolean canRepair(ItemStack item) {
        return tiers.isSupported(item) && config.getBoolean("repair.enabled", true);
    }

    public String growthRestriction(ItemStack item) {
        return switch (grade(item)) {
            case GRADE_4 -> "Grade 4 special equipment cannot be enhanced or promoted.";
            case GRADE_5 -> "Grade 5 endgame equipment has completed its growth.";
            default -> "Equipment cannot be enhanced or promoted.";
        };
    }

    public String itemId(ItemStack item) {
        if (item == null || item.getType().isAir()) return "";
        return items.getItemId(item).orElse("vanilla:" + item.getType().name().toLowerCase(Locale.ROOT));
    }

    public SpecialEquipmentData special(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        return specials.get(itemId(item)).orElse(null);
    }
}
