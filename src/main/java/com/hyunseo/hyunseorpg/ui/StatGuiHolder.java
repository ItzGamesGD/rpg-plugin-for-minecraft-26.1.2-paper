package com.hyunseo.hyunseorpg.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class StatGuiHolder implements InventoryHolder {
    private final StatGuiType type;

    public StatGuiHolder(StatGuiType type) {
        this.type = type;
    }

    public StatGuiType type() {
        return type;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
