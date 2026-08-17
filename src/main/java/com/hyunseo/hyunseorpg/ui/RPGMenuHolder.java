package com.hyunseo.hyunseorpg.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public final class RPGMenuHolder implements InventoryHolder {
    public enum View { MAIN, SHOP_LIST, BOSS, GROWTH, QUESTS, ENCHANT_SUPPORT }

    private final View view;
    private final Map<Integer, String> actions = new HashMap<>();
    private Inventory inventory;

    public RPGMenuHolder(View view) {
        this.view = view;
    }

    public View view() { return view; }
    public Map<Integer, String> actions() { return actions; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
