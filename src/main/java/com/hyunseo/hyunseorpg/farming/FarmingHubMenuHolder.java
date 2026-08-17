package com.hyunseo.hyunseorpg.farming;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/** Holder-based identity for the farming hub and its read-only subviews. */
public final class FarmingHubMenuHolder implements InventoryHolder {
    public enum View { HUB, DELIVERY, INFO }

    private final UUID playerId;
    private final View view;
    private Inventory inventory;

    public FarmingHubMenuHolder(UUID playerId, View view) {
        this.playerId = playerId;
        this.view = view;
    }

    public UUID playerId() { return playerId; }
    public View view() { return view; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
