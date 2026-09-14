package com.hyunseo.hyunseorpg.equipment;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentData;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentRegistry;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/** Central permission policy for enhancement and repair. Legacy promotion PDC is ignored. */
public final class EquipmentGrowthPolicy {
    private final ConfigService config;
    private final RPGItemService items;
    private final EquipmentTierService tiers;
    private final SpecialEquipmentRegistry specials;

    public EquipmentGrowthPolicy(ConfigService config, RPGItemService items,
                                 EquipmentTierService tiers, SpecialEquipmentRegistry specials) {
        this.config = config;
        this.items = items;
        this.tiers = tiers;
        this.specials = specials;
    }

    public boolean isEndgame(ItemStack item) {
        return config.getEquipmentGrowthBoolean("equipment-registry." + itemId(item) + ".endgame", false);
    }

    public boolean canEnhance(ItemStack item) {
        return tiers.isSupported(item) && tiers.getMaxEnhancement(item) > 0
                && !isEndgame(item) && special(item) == null;
    }

    public boolean canEnchant(ItemStack item) {
        if (!tiers.isSupported(item) || isEndgame(item)) return false;
        SpecialEquipmentData data = special(item);
        return data == null || data.allowCustomEnchants();
    }

    public boolean canRepair(ItemStack item) {
        return tiers.isSupported(item) && config.getBoolean("repair.enabled", true);
    }

    public String growthRestriction(ItemStack item) {
        return isEndgame(item) ? "Endgame equipment cannot be enhanced."
                : "Equipment cannot be enhanced.";
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
