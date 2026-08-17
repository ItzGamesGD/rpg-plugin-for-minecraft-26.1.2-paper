package com.hyunseo.hyunseorpg.alchemy.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class AlchemyMenuHolder implements InventoryHolder {
    public enum View { HUB }
    private final UUID sessionId;
    private final View view;
    private Inventory inventory;
    public AlchemyMenuHolder(UUID sessionId) { this(sessionId, View.HUB); }
    public AlchemyMenuHolder(UUID sessionId, View view) { this.sessionId = sessionId; this.view = view == null ? View.HUB : view; }
    public UUID sessionId() { return sessionId; }
    public View view() { return view; }
    public void inventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
