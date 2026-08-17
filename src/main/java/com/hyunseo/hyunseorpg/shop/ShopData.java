package com.hyunseo.hyunseorpg.shop;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ShopData(String shopId, String title, List<ShopItemData> items) {
    public ShopData {
        Objects.requireNonNull(shopId, "shopId");
        Objects.requireNonNull(title, "title");
        items = items.stream().sorted(Comparator.comparingInt(ShopItemData::order).thenComparing(ShopItemData::productId)).toList();
    }

    public int pageCount() {
        int lastOrder = items.stream().mapToInt(ShopItemData::order).max().orElse(-1);
        return Math.max(1, lastOrder / 45 + 1);
    }

    public List<ShopItemData> page(int page) {
        int safePage = Math.max(0, Math.min(page, pageCount() - 1));
        int from = safePage * 45;
        return items.stream()
                .filter(item -> item.order() >= from && item.order() < from + 45)
                .toList();
    }

    public Optional<ShopItemData> itemAt(int page, int slot) {
        if (slot < 0 || slot >= 45) {
            return Optional.empty();
        }
        int safePage = Math.max(0, Math.min(page, pageCount() - 1));
        int order = safePage * 45 + slot;
        return items.stream().filter(item -> item.order() == order).findFirst();
    }
}
