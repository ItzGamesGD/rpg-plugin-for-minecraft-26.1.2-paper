package com.hyunseo.hyunseorpg.farming;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/** Holder for the cooking recipe list and its multi-input workstation. */
public final class CookingMenuHolder implements InventoryHolder {
    private final UUID playerId;
    private final String recipeId;
    private final boolean recipeList;
    private Inventory inventory;

    public CookingMenuHolder(UUID playerId, String recipeId, boolean recipeList) {
        this.playerId = playerId;
        this.recipeId = recipeId == null ? "" : recipeId;
        this.recipeList = recipeList;
    }

    public UUID playerId() { return playerId; }
    public String recipeId() { return recipeId; }
    public boolean recipeList() { return recipeList; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
