package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Optional;

/**
 * Read-only boundary for future brewing and alchemy providers.
 *
 * This class resolves canonical ItemRegistry/PDC metadata only. It does not
 * create recipes, consume items, or calculate brewing effects.
 */
public final class FarmingItemBridge {
    private final RPGItemService items;
    private final CropQualityService quality;
    private final FarmingEssenceService essence;

    public FarmingItemBridge(RPGItemService items, CropQualityService quality,
                             FarmingEssenceService essence) {
        this.items = items;
        this.quality = quality;
        this.essence = essence;
    }

    public Optional<String> getFarmingItemId(ItemStack item) {
        return items.getItemId(item)
                .filter(id -> items.getData(id).isPresent())
                .filter(items::isFarmingItemId);
    }

    /** Seeds are farming items, but are not ingredients unless explicitly tagged. */
    public boolean isFarmingIngredient(ItemStack item) {
        String itemId = getFarmingItemId(item).orElse("");
        if (itemId.isBlank()) return false;
        if (itemId.startsWith("seed_") && !hasFarmingTag(item, "farming-ingredient")) return false;
        return itemId.startsWith("crop_")
                || itemId.startsWith("processed_")
                || hasFarmingTag(item, "farming-ingredient")
                || hasFarmingTag(item, "farming-processed");
    }

    public Optional<String> getCropId(ItemStack item) {
        if (!isFarmingIngredient(item)) return Optional.empty();
        Optional<String> stored = items.getFarmingCropId(item);
        if (stored.isPresent()) return stored;
        return getFarmingItemId(item).flatMap(quality::cropOfItem);
    }

    public Optional<String> getProcessingItemId(ItemStack item) {
        String itemId = getFarmingItemId(item).orElse("");
        if (itemId.startsWith("processed_") || hasFarmingTag(item, "farming-processed")) {
            return itemId.isBlank() ? Optional.empty() : Optional.of(itemId);
        }
        return Optional.empty();
    }

    public Optional<CropQuality> getQuality(ItemStack item) {
        if (!isFarmingIngredient(item)) return Optional.empty();
        Optional<CropQuality> stored = items.getFarmingQuality(item);
        if (stored.isPresent()) return stored;
        return getFarmingItemId(item).flatMap(quality::qualityOfItem);
    }

    public double getQualityScore(ItemStack item) {
        return getQuality(item).map(quality::qualityScore).orElse(0.0D);
    }

    public int getFarmingDataVersion(ItemStack item) {
        return items.getFarmingDataVersion(item);
    }

    public boolean hasFarmingTag(ItemStack item, String tag) {
        return getFarmingItemId(item).map(id -> items.hasTag(id, normalize(tag))).orElse(false);
    }

    public boolean isAbundanceEssence(ItemStack item) {
        return getFarmingItemId(item).map(id -> id.equals(essence.resultItemId())).orElse(false);
    }

    /**
     * Usage tags are the essence policy contract from farming/essence.yml.
     * They are intentionally separate from ItemRegistry tags such as alchemy-catalyst.
     */
    public boolean isAbundanceEssenceUsableFor(ItemStack item, String usageTag) {
        return isAbundanceEssence(item) && essence.hasUsageTag(usageTag);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
