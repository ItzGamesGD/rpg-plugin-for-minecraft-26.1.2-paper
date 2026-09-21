package com.hyunseo.hyunseorpg.equipment;

import org.bukkit.inventory.ItemStack;

/** Shared durability-preservation rules for normal tool damage. */
public final class ToolDurabilityService {
    private final EquipmentTierService tiers;

    public ToolDurabilityService(EquipmentTierService tiers) {
        this.tiers = tiers;
    }

    public boolean isHoe(ItemStack item) {
        return item != null && !item.getType().isAir()
                && tiers.getCategory(item) == EquipmentTierService.Category.TOOL
                && item.getType().name().endsWith("_HOE");
    }

    /** Preserves the existing PlayerItemDamageEvent behavior for normal tools. */
    public boolean shouldPreserveNormal(ItemStack tool) {
        return tool != null && tool.getItemMeta() != null && tool.getItemMeta().isUnbreakable();
    }

}
