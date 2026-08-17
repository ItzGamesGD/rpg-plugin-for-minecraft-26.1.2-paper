package com.hyunseo.hyunseorpg.crafting;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Holds explicit recipe slots so empty spaces and page placement survive saving. */
public final class CraftingAdminSession {
    private final String type;
    private final Map<String, Integer> positions;
    private int page;
    private int pageLimit;
    private boolean navigating;

    public CraftingAdminSession(String type, List<String> recipeIds) {
        this.type = type;
        this.positions = new LinkedHashMap<>();
        for (int index = 0; index < recipeIds.size(); index++) positions.put(recipeIds.get(index), index);
        this.pageLimit = 1;
    }

    public CraftingAdminSession(String type, Map<String, Integer> positions) {
        this.type = type;
        this.positions = new LinkedHashMap<>(positions);
        this.pageLimit = 1;
    }

    public String type() { return type; }
    public int page() { return page; }
    public void setPage(int page) {
        this.page = Math.max(0, Math.min(page, Math.max(pageCount(), pageLimit + 1) - 1));
        this.pageLimit = Math.max(pageLimit, this.page + 1);
    }
    public boolean navigating() { return navigating; }
    public void setNavigating(boolean navigating) { this.navigating = navigating; }
    public int pageCount() {
        int last = positions.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return Math.max(1, last / 45 + 1);
    }
    public int editorPageCount() { return Math.max(pageCount(), pageLimit + 1); }
    public List<String> pageItems() {
        int from = page * 45;
        return positions.entrySet().stream()
                .filter(entry -> entry.getValue() >= from && entry.getValue() < from + 45)
                .sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
    }
    public List<String> recipeIds() {
        return positions.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
    }
    public Map<String, Integer> positions() { return Map.copyOf(positions); }

    public void capture(Inventory inventory, java.util.function.Function<ItemStack, String> resolver) {
        int from = page * 45;
        positions.entrySet().removeIf(entry -> entry.getValue() >= from && entry.getValue() < from + 45);
        for (int slot = 0; slot < 45; slot++) {
            String recipeId = resolver.apply(inventory.getItem(slot));
            if (recipeId != null) positions.put(recipeId, from + slot);
        }
        setPage(page);
    }
}
