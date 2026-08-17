package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/** Validates canonical PDC item identity and quality, never display names. */
public final class DeliveryItemValidator {
    private final RPGItemService items;
    private final CropQualityService qualityService;

    public DeliveryItemValidator(RPGItemService items, CropQualityService qualityService) {
        this.items = items;
        this.qualityService = qualityService;
    }

    public Optional<CropQuality> qualityFor(ItemStack item, DeliveryRequirement requirement) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) return Optional.empty();
        String itemId = items.getItemId(item).orElse("").trim().toLowerCase(java.util.Locale.ROOT);
        if (itemId.isBlank() || !matchesFamily(itemId, requirement.itemFamily())) return Optional.empty();
        CropQuality quality = qualityService.qualityOfItem(itemId).orElseGet(() ->
                itemId.equals(requirement.itemFamily()) ? CropQuality.NORMAL : CropQuality.NORMAL);
        return quality.ordinal() >= requirement.minimumQuality().ordinal() ? Optional.of(quality) : Optional.empty();
    }

    private boolean matchesFamily(String itemId, String family) {
        String normalized = family.trim().toLowerCase(java.util.Locale.ROOT);
        if (itemId.equals(normalized)) return true;
        if (normalized.startsWith("crop_")) return itemId.equals(normalized + "_quality_" + qualitySuffix(itemId));
        if (normalized.startsWith("processed_")) return itemId.startsWith(normalized + "_");
        return false;
    }

    private String qualitySuffix(String itemId) {
        int marker = itemId.lastIndexOf("_quality_");
        if (marker >= 0) return itemId.substring(marker + "_quality_".length());
        return "normal";
    }
}
