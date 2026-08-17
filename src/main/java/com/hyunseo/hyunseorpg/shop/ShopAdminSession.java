package com.hyunseo.hyunseorpg.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Keeps every edited page in memory until the administrator closes the editor. */
public final class ShopAdminSession {
    private final ShopData original;
    private List<ShopItemData> products;
    private int page;
    private boolean navigating;

    public ShopAdminSession(ShopData original) {
        this.original = original;
        this.products = new ArrayList<>(original.items());
    }

    public ShopData original() { return original; }
    public int page() { return page; }
    public void setPage(int page) { this.page = Math.max(0, Math.min(page, pageCount())); }
    public boolean navigating() { return navigating; }
    public void setNavigating(boolean navigating) { this.navigating = navigating; }
    /** Customer pages are based on the last occupied administrator slot. */
    public int pageCount() {
        int lastOrder = products.stream().mapToInt(ShopItemData::order).max().orElse(-1);
        return Math.max(1, lastOrder / 45 + 1);
    }

    public List<ShopItemData> pageItems() {
        int from = page * 45;
        return products.stream()
                .filter(product -> product.order() >= from && product.order() < from + 45)
                .collect(Collectors.toList());
    }

    public void capture(Inventory inventory) {
        int from = page * 45;
        List<ShopItemData> previousPage = products.stream()
                .filter(product -> product.order() >= from && product.order() < from + 45)
                .toList();
        List<ShopItemData> replacement = new ArrayList<>();
        Set<String> consumedProductIds = new HashSet<>();
        for (int slot = 0; slot < 45; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            ShopItemData existing = findExisting(previousPage, consumedProductIds, item);
            if (existing == null) {
                existing = new ShopItemData(nextProductId(replacement), item, Math.max(1, item.getAmount()), 0,
                        0L, 0L, false, false, ShopMatchMode.EXACT);
            }
            consumedProductIds.add(existing.productId());
            replacement.add(existing.withTemplate(item, from + slot));
        }

        List<ShopItemData> next = products.stream()
                .filter(product -> product.order() < from || product.order() >= from + 45)
                .collect(Collectors.toCollection(ArrayList::new));
        next.addAll(replacement);
        products = next;
        setPage(page);
    }

    public ShopData toShopData() {
        return new ShopData(original.shopId(), original.title(), products);
    }

    private ShopItemData findExisting(List<ShopItemData> candidates, Set<String> consumed, ItemStack item) {
        for (ShopItemData candidate : candidates) {
            if (!consumed.contains(candidate.productId()) && candidate.template().isSimilar(normalize(item))) return candidate;
        }
        return null;
    }

    private String nextProductId(List<ShopItemData> replacement) {
        int sequence = 1;
        while (containsProductId("product_" + sequence) || containsProductId(replacement, "product_" + sequence)) sequence++;
        return "product_" + sequence;
    }

    private boolean containsProductId(String productId) {
        return containsProductId(products, productId);
    }

    private boolean containsProductId(List<ShopItemData> values, String productId) {
        for (ShopItemData product : values) {
            if (product.productId().equals(productId)) return true;
        }
        return false;
    }

    private ItemStack normalize(ItemStack item) {
        ItemStack copy = item.clone();
        copy.setAmount(1);
        return copy;
    }
}
