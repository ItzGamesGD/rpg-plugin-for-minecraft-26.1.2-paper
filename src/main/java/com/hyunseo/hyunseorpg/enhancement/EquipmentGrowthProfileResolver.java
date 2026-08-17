package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * Resolves the YAML promotion profile for an equipment item.
 *
 * Enhancement profiles may intentionally group multiple tool materials under
 * one profile, while promotion option pools are tool-family specific. Keeping
 * this resolution in one place prevents registry metadata and live promotion
 * from disagreeing about whether an item can be promoted.
 */
public final class EquipmentGrowthProfileResolver {
    private EquipmentGrowthProfileResolver() {
    }

    public static String resolvePromotionProfile(ConfigService config, RPGItemService itemService,
                                                 ItemStack item, String fallback) {
        if (item == null || item.getType().isAir()) return normalize(fallback);
        String itemId = itemService.getItemId(item).orElse("");
        if (!itemId.isBlank()) {
            String configured = config.getEquipmentGrowthString(
                    "promotion.profile-overrides.custom-item-ids." + itemId, "");
            if (!configured.isBlank()) return normalize(configured);
        }

        Material material = item.getType();
        String configured = config.getEquipmentGrowthString(
                "promotion.profile-overrides.materials." + material.name(), "");
        if (!configured.isBlank()) return normalize(configured);

        String name = material.name();
        if (name.endsWith("_PICKAXE")) return "pickaxe";
        if (name.endsWith("_SHOVEL")) return "shovel";
        if (name.endsWith("_HOE")) return "hoe";
        if (name.endsWith("_AXE")) return "axe";
        if (name.endsWith("_SWORD")) return "sword";
        if (material == Material.TRIDENT) return "spear";
        if (material == Material.CROSSBOW) return "crossbow";
        if (material == Material.BOW) return "bow";
        if (material == Material.SHIELD) return "shield";
        if (material == Material.FISHING_ROD) return "fishing_rod";
        return normalize(fallback);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
