package com.hyunseo.hyunseorpg.enhancement;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class PromotionInventoryHolder implements InventoryHolder {
    private Inventory inventory;
    private UUID playerId;
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }
    public UUID playerId() { return playerId; }
    @Override public Inventory getInventory() { return inventory; }
}
