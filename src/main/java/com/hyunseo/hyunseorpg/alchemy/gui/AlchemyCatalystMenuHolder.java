package com.hyunseo.hyunseorpg.alchemy.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class AlchemyCatalystMenuHolder implements InventoryHolder {
    private final UUID sessionId;
    private Inventory inventory;

    public AlchemyCatalystMenuHolder(UUID sessionId) { this.sessionId = sessionId; }
    public UUID sessionId() { return sessionId; }
    public void inventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
