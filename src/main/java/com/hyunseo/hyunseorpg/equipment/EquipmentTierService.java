package com.hyunseo.hyunseorpg.equipment;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/** Resolves the equipment family and tier used by all growth calculations. */
public final class EquipmentTierService {
    public enum Category { WEAPON, SPEAR, RANGED, TOOL, ARMOR, SHIELD, FISHING_ROD, ELYTRA, UNSUPPORTED }

    private final ConfigService config;
    private final RPGItemService itemService;

    public EquipmentTierService(ConfigService config, RPGItemService itemService) {
        this.config = config;
        this.itemService = itemService;
    }

    public Category getCategory(ItemStack item) {
        if (item == null || item.getType().isAir()) return Category.UNSUPPORTED;
        Material material = item.getType();
        if (material == Material.TRIDENT) return Category.SPEAR;
        if (material == Material.BOW || material == Material.CROSSBOW) return Category.RANGED;
        if (material == Material.SHIELD) return Category.SHIELD;
        if (material == Material.FISHING_ROD) return Category.FISHING_ROD;
        if (material == Material.ELYTRA) return Category.ELYTRA;
        if (isArmor(material)) return Category.ARMOR;
        if (isTool(material)) return Category.TOOL;
        if (isWeapon(material)) return Category.WEAPON;
        return Category.UNSUPPORTED;
    }

    public boolean isSupported(ItemStack item) {
        return getCategory(item) != Category.UNSUPPORTED;
    }

    public String getTier(ItemStack item) {
        if (item == null) return "iron";
        String itemId = itemService.getItemId(item).orElse("").toLowerCase(Locale.ROOT);
        if (!itemId.isBlank()) {
            String configured = config.getEquipmentGrowthString("tiers.custom-item-tiers." + itemId, "");
            if (!configured.isBlank()) return configured.toLowerCase(Locale.ROOT);
        }
        String configured = config.getEquipmentGrowthString("tiers.material-tiers." + item.getType().name(), "");
        if (!configured.isBlank()) return configured.toLowerCase(Locale.ROOT);
        return materialTier(item.getType());
    }

    public int getMaxEnhancement(ItemStack item) {
        return Math.max(0, config.getEquipmentGrowthInt(
                "tiers.definitions." + getTier(item) + ".max-enhancement", 10));
    }

    public int getMaxPromotionStage(ItemStack item) {
        return Math.max(0, config.getEquipmentGrowthInt(
                "tiers.definitions." + getTier(item) + ".max-promotion-stage", 1));
    }

    private boolean isWeapon(Material material) {
        return material.name().endsWith("_SWORD") || material == Material.MACE;
    }

    private boolean isTool(Material material) {
        String name = material.name();
        return name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL")
                || name.endsWith("_AXE") || name.endsWith("_HOE");
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    private String materialTier(Material material) {
        String name = material.name();
        if (name.startsWith("WOODEN_")) return "wooden";
        if (name.startsWith("STONE_")) return "stone";
        if (name.startsWith("GOLDEN_")) return "gold";
        if (name.startsWith("IRON_")) return "iron";
        if (name.startsWith("DIAMOND_")) return "diamond";
        if (name.startsWith("NETHERITE_")) return "netherite";
        if (material == Material.ELYTRA) return "elytra";
        return "iron";
    }
}
