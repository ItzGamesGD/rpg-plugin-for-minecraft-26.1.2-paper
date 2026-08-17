package com.hyunseo.hyunseorpg.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ShopInventoryHolder implements InventoryHolder {
    private final ShopData shop;
    private final int page;
    private Inventory inventory;

    public ShopInventoryHolder(ShopData shop, int page) {
        this.shop = shop;
        this.page = page;
    }

    public ShopData shop() { return shop; }
    public int page() { return page; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
