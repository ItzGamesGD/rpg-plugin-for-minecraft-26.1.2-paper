package com.hyunseo.hyunseorpg.shop;

import com.hyunseo.hyunseorpg.economy.CoinService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.item.VanillaStackingService;
import com.hyunseo.hyunseorpg.equipment.HoeHarvestModifierService;
import com.hyunseo.hyunseorpg.farming.CropQuality;
import com.hyunseo.hyunseorpg.farming.CropQualityService;
import com.hyunseo.hyunseorpg.farming.FarmingProfileService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Owns atomic coin and inventory changes for all shop transactions. */
public final class ShopService {
    private final CoinService coinService;
    private final RPGItemService itemService;
    private final VanillaStackingService vanillaStacking;
    private HoeHarvestModifierService hoeModifiers;
    private CropQualityService cropQuality;
    private FarmingProfileService farmingProfiles;

    public ShopService(CoinService coinService, RPGItemService itemService,
                       VanillaStackingService vanillaStacking) {
        this.coinService = coinService;
        this.itemService = itemService;
        this.vanillaStacking = vanillaStacking;
    }

    public void setFarmingSaleModifier(HoeHarvestModifierService hoeModifiers,
                                       CropQualityService cropQuality) {
        this.hoeModifiers = hoeModifiers;
        this.cropQuality = cropQuality;
    }

    public void setFarmingPurchaseGate(FarmingProfileService farmingProfiles) {
        this.farmingProfiles = farmingProfiles;
    }

    public ShopTransactionResult buy(Player player, ShopItemData product, boolean stack) {
        if (!product.purchasable()) return ShopTransactionResult.failed(ShopTransactionReason.PURCHASE_DISABLED);
        ShopTransactionReason farmingGate = farmingPurchaseGate(player, product);
        if (farmingGate != null) return ShopTransactionResult.failed(farmingGate);
        if (product.buyPrice() <= 0L) return ShopTransactionResult.failed(ShopTransactionReason.INVALID_PRICE);

        int bundleCount = stack ? bulkBundleCount(product) : 1;
        int amount = deliveryAmount(product, bundleCount);
        long total = totalPrice(product.buyPrice(), bundleCount);
        if (amount < 1 || total < 0L) return ShopTransactionResult.failed(ShopTransactionReason.PRICE_OVERFLOW);
        if (!product.currencyItemId().isBlank() && total > Integer.MAX_VALUE) {
            return ShopTransactionResult.failed(ShopTransactionReason.PRICE_OVERFLOW);
        }
        if (product.currencyItemId().isBlank() && coinService.getCoins(player) < total) {
            return ShopTransactionResult.failed(ShopTransactionReason.INSUFFICIENT_COINS);
        }
        if (!product.currencyItemId().isBlank()
                && countCurrency(player, product.currencyItemId()) < total) {
            return ShopTransactionResult.failed(ShopTransactionReason.INSUFFICIENT_COINS);
        }

        ItemStack delivery = product.template();
        if (vanillaStacking != null) vanillaStacking.normalize(delivery);
        delivery.setAmount(amount);
        if (!hasCapacity(player.getInventory(), delivery)) {
            return ShopTransactionResult.failed(ShopTransactionReason.INVENTORY_FULL);
        }

        ItemStack[] before = cloneContents(player.getInventory().getStorageContents());
        if (product.currencyItemId().isBlank()) {
            if (!coinService.takeCoins(player, total)) {
                return ShopTransactionResult.failed(ShopTransactionReason.INSUFFICIENT_COINS);
            }
        } else if (!removeCurrency(player, product.currencyItemId(), (int) total)) {
            return ShopTransactionResult.failed(ShopTransactionReason.INSUFFICIENT_COINS);
        }

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(delivery);
        if (!leftovers.isEmpty()) {
            player.getInventory().setStorageContents(before);
            if (product.currencyItemId().isBlank()) {
                coinService.addCoins(player, total);
            }
            return ShopTransactionResult.failed(ShopTransactionReason.TRANSACTION_FAILED);
        }
        return ShopTransactionResult.success(amount, total);
    }

    private ShopTransactionReason farmingPurchaseGate(Player player, ShopItemData product) {
        String cropId = product == null ? "" : product.requiredFarmingCropId();
        if (cropId == null || cropId.isBlank()) return null;
        if (player == null || farmingProfiles == null || !farmingProfiles.isReady(player)) {
            return ShopTransactionReason.FARMING_PROFILE_NOT_READY;
        }
        return farmingProfiles.isCropUnlocked(player, cropId)
                ? null : ShopTransactionReason.FARMING_CROP_LOCKED;
    }

    public ShopTransactionResult sell(Player player, ShopItemData product, boolean stack) {
        return sell(player, product, stack, null);
    }

    public ShopTransactionResult sell(Player player, ShopItemData product, boolean stack, ItemStack saleHoe) {
        if (!product.sellable()) return ShopTransactionResult.failed(ShopTransactionReason.SALE_DISABLED);
        long unitPrice = effectiveSellPrice(product, saleHoe);
        if (unitPrice <= 0L) return ShopTransactionResult.failed(ShopTransactionReason.INVALID_PRICE);

        int bundleAmount = product.amount();
        int owned = countMatching(player.getInventory(), product);
        int bundleCount = stack ? owned / bundleAmount : (owned >= bundleAmount ? 1 : 0);
        if (bundleCount <= 0) return ShopTransactionResult.failed(ShopTransactionReason.NO_MATCHING_ITEMS);

        int amount = deliveryAmount(product, bundleCount);
        long total = totalPrice(unitPrice, bundleCount);
        if (amount < 1 || total < 0L) return ShopTransactionResult.failed(ShopTransactionReason.PRICE_OVERFLOW);
        if (!coinService.canAddCoins(player, total)) return ShopTransactionResult.failed(ShopTransactionReason.PRICE_OVERFLOW);

        ItemStack[] before = cloneContents(player.getInventory().getStorageContents());
        int removed = removeMatching(player.getInventory(), product, amount);
        if (removed != amount) {
            player.getInventory().setStorageContents(before);
            return ShopTransactionResult.failed(ShopTransactionReason.TRANSACTION_FAILED);
        }
        coinService.addCoins(player, total);
        return ShopTransactionResult.success(removed, total);
    }

    /** Returns the number of complete bundles bought by Shift-click. */
    public int bulkBundleCount(ShopItemData product) {
        int bundleAmount = Math.max(1, product.amount());
        ItemStack template = product.template();
        if (vanillaStacking != null) vanillaStacking.normalize(template);
        int maxStackSize = Math.max(1, vanillaStacking == null
                ? template.getMaxStackSize() : vanillaStacking.effectiveMaxStackSize(template));
        return Math.max(1, maxStackSize / bundleAmount);
    }

    public int bulkDeliveryAmount(ShopItemData product) {
        return deliveryAmount(product, bulkBundleCount(product));
    }

    public long bulkPrice(ShopItemData product, boolean buy) {
        long bundlePrice = buy ? product.buyPrice() : product.sellPrice();
        return totalPrice(bundlePrice, bulkBundleCount(product));
    }

    public long bulkPrice(ShopItemData product, boolean buy, ItemStack saleHoe) {
        long bundlePrice = buy ? product.buyPrice() : effectiveSellPrice(product, saleHoe);
        return totalPrice(bundlePrice, bulkBundleCount(product));
    }

    public long effectiveSellPrice(ShopItemData product, ItemStack saleHoe) {
        // Farming sale bonus is an account equipment effect evaluated at the transaction time.
        // It is not a harvest-provenance bonus and therefore does not modify crop PDC or stacking.
        if (product == null || product.sellPrice() <= 0L || saleHoe == null
                || hoeModifiers == null || cropQuality == null) return product == null ? 0L : product.sellPrice();
        String itemId = itemService.getItemId(product.template()).orElse("");
        CropQuality quality = cropQuality.qualityOfItem(itemId).orElse(CropQuality.NORMAL);
        if (quality == CropQuality.NORMAL) return product.sellPrice();
        double bonus = hoeModifiers.salePriceBonus(saleHoe);
        if (!Double.isFinite(bonus) || bonus <= 0.0D) return product.sellPrice();
        try {
            return BigDecimal.valueOf(product.sellPrice())
                    .multiply(BigDecimal.valueOf(1.0D + bonus))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private int deliveryAmount(ShopItemData product, int bundleCount) {
        try {
            return Math.multiplyExact(Math.max(1, product.amount()), Math.max(1, bundleCount));
        } catch (ArithmeticException ignored) {
            return -1;
        }
    }

    private long totalPrice(long bundlePrice, int bundleCount) {
        try {
            return Math.multiplyExact(bundlePrice, bundleCount);
        } catch (ArithmeticException ignored) {
            return -1L;
        }
    }

    private boolean hasCapacity(Inventory inventory, ItemStack item) {
        int remaining = item.getAmount();
        int maxStack = Math.max(1, item.getMaxStackSize());
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                remaining -= maxStack;
            } else if (slot.isSimilar(item)) {
                remaining -= Math.max(0, Math.min(maxStack, slot.getMaxStackSize()) - slot.getAmount());
            }
            if (remaining <= 0) return true;
        }
        return false;
    }

    private int countMatching(Inventory inventory, ShopItemData product) {
        long total = 0L;
        for (ItemStack item : inventory.getStorageContents()) {
            if (matches(item, product)) total += item.getAmount();
        }
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private long countCurrency(Player player, String itemId) {
        long total = 0L;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (itemService.isItem(item, itemId)) total += item.getAmount();
        }
        return total;
    }

    private boolean removeCurrency(Player player, String itemId, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (!itemService.isItem(item, itemId)) continue;
            int removed = Math.min(remaining, item.getAmount());
            remaining -= removed;
            if (removed >= item.getAmount()) contents[slot] = null;
            else item.setAmount(item.getAmount() - removed);
        }
        if (remaining > 0) return false;
        player.getInventory().setStorageContents(contents);
        return true;
    }

    private int removeMatching(Inventory inventory, ShopItemData product, int requested) {
        int remaining = requested;
        ItemStack[] contents = inventory.getStorageContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (!matches(item, product)) continue;
            int remove = Math.min(remaining, item.getAmount());
            remaining -= remove;
            if (remove == item.getAmount()) contents[slot] = null;
            else item.setAmount(item.getAmount() - remove);
        }
        inventory.setStorageContents(contents);
        return requested - remaining;
    }

    private boolean matches(ItemStack item, ShopItemData product) {
        if (item == null || item.getType().isAir()) return false;
        return switch (product.matchMode()) {
            case MATERIAL -> item.getType() == product.template().getType();
            case CUSTOM_ID -> itemService.getItemId(item).isPresent()
                    && itemService.getItemId(item).equals(itemService.getItemId(product.template()));
            case EXACT -> {
                ItemStack expected = product.template();
                ItemStack candidate = item.clone();
                expected.setAmount(1);
                candidate.setAmount(1);
                yield candidate.isSimilar(expected);
            }
        };
    }

    private ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[contents.length];
        for (int index = 0; index < contents.length; index++) {
            copy[index] = contents[index] == null ? null : contents[index].clone();
        }
        return copy;
    }
}
