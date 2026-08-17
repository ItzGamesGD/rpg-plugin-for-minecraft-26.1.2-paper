package com.hyunseo.hyunseorpg.shop;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/** A bundle-priced product. The template never contains shop display lore. */
public record ShopItemData(
        String productId,
        ItemStack template,
        int amount,
        int order,
        long buyPrice,
        long sellPrice,
        boolean purchasable,
        boolean sellable,
        ShopMatchMode matchMode,
        String currencyItemId,
        String requiredFarmingCropId
) {
    public ShopItemData {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(matchMode, "matchMode");
        amount = Math.max(1, amount);
        currencyItemId = currencyItemId == null ? "" : currencyItemId.trim().toLowerCase(java.util.Locale.ROOT);
        requiredFarmingCropId = requiredFarmingCropId == null ? "" : requiredFarmingCropId.trim().toLowerCase(java.util.Locale.ROOT);
        template = template.clone();
        template.setAmount(1);
    }

    public ShopItemData(String productId, ItemStack template, int amount, int order, long buyPrice, long sellPrice,
                        boolean purchasable, boolean sellable, ShopMatchMode matchMode) {
        this(productId, template, amount, order, buyPrice, sellPrice, purchasable, sellable, matchMode, "", "");
    }

    public ShopItemData(String productId, ItemStack template, int order, long buyPrice, long sellPrice,
                        boolean purchasable, boolean sellable, ShopMatchMode matchMode, String currencyItemId) {
        this(productId, template, 1, order, buyPrice, sellPrice, purchasable, sellable, matchMode, currencyItemId, "");
    }

    @Override
    public ItemStack template() {
        return template.clone();
    }

    public ShopItemData withTemplate(ItemStack nextTemplate, int nextOrder) {
        return new ShopItemData(productId, nextTemplate, amount, nextOrder, buyPrice, sellPrice,
                purchasable, sellable, matchMode, currencyItemId, requiredFarmingCropId);
    }
}
