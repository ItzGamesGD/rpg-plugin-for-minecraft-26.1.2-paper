package com.hyunseo.hyunseorpg.enhancement;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class RepairInventoryHolder implements InventoryHolder {
    private Inventory inventory;
    private ItemStack previewItem;
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public ItemStack getPreviewItem() { return previewItem; }
    public void setPreviewItem(ItemStack previewItem) { this.previewItem = previewItem == null ? null : previewItem.clone(); }
    @Override public Inventory getInventory() { return inventory; }
}
