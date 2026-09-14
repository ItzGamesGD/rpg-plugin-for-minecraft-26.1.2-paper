package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.equipment.EquipmentGrowthPolicy;
import com.hyunseo.hyunseorpg.equipment.EquipmentTierService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentRegistry;
import com.hyunseo.hyunseorpg.special.DedicatedWeaponIds;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/** Classifies an item through existing item, equipment, endgame and special-equipment registries. */
public final class EnhancementClassificationService {
    private final ConfigService config;
    private final RPGItemService items;
    private final EquipmentTierService tiers;
    private final EquipmentGrowthPolicy growth;
    private final SpecialEquipmentRegistry specials;

    public EnhancementClassificationService(ConfigService config, RPGItemService items, EquipmentTierService tiers,
                                            EquipmentGrowthPolicy growth, SpecialEquipmentRegistry specials) {
        this.config = config;
        this.items = items;
        this.tiers = tiers;
        this.growth = growth;
        this.specials = specials;
    }

    public EnhancementClass classify(ItemStack item) {
        if (item == null || item.getType().isAir()) return EnhancementClass.UNSUPPORTED;
        boolean supported = tiers.isSupported(item);
        boolean endgame = supported && growth.isEndgame(item);
        String id = items.getItemId(item).orElse("").toLowerCase(Locale.ROOT);
        boolean registeredSpecial = !id.isBlank() && specials.get(id).isPresent();
        var configuredElementals = config.getEquipmentGrowthStringList("enhancement.elemental-item-ids");
        boolean elemental = registeredSpecial && (configuredElementals.isEmpty()
                ? !DedicatedWeaponIds.owns(id)
                : configuredElementals.stream().map(value -> value.toLowerCase(Locale.ROOT)).anyMatch(id::equals));
        return classify(supported, endgame, registeredSpecial, elemental);
    }

    static EnhancementClass classify(boolean supported, boolean endgame,
                                     boolean registeredSpecial, boolean configuredElemental) {
        if (!supported) return EnhancementClass.UNSUPPORTED;
        if (endgame) return EnhancementClass.ENDGAME;
        if (configuredElemental) return EnhancementClass.ELEMENTAL;
        return registeredSpecial ? EnhancementClass.SPECIAL : EnhancementClass.VANILLA;
    }

    public int maximumLevel(ItemStack item) {
        EnhancementClass type = classify(item);
        return type.enhanceable()
                ? Math.max(0, config.getEquipmentGrowthInt(type.capConfigPath(), type.defaultCap()))
                : 0;
    }
}
