package com.hyunseo.hyunseorpg.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ShopAdminInventoryHolder implements InventoryHolder {
    private final ShopAdminSession session;
    private Inventory inventory;

    public ShopAdminInventoryHolder(ShopAdminSession session) {
        this.session = session;
    }

    public ShopAdminSession session() { return session; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
