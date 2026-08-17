package com.hyunseo.hyunseorpg.crafting;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

public final class CraftingMenuHolder implements InventoryHolder {
    private final String type;
    private final int page;
    private final boolean admin;
    private final CraftingAdminSession session;
    private final List<String> visibleRecipeIds;
    private final Consumer<Player> backOpener;
    private Inventory inventory;

    public CraftingMenuHolder(String type, int page, boolean admin, CraftingAdminSession session) {
        this(type, page, admin, session, List.of(), null);
    }

    public CraftingMenuHolder(String type, int page, boolean admin, CraftingAdminSession session,
                              List<String> visibleRecipeIds, Consumer<Player> backOpener) {
        this.type = type;
        this.page = page;
        this.admin = admin;
        this.session = session;
        this.visibleRecipeIds = List.copyOf(visibleRecipeIds == null ? List.of() : visibleRecipeIds);
        this.backOpener = backOpener;
    }

    public String type() { return type; }
    public int page() { return page; }
    public boolean admin() { return admin; }
    public boolean navigating() { return session != null && session.navigating(); }
    public CraftingAdminSession session() { return session; }
    public boolean filtered() { return !visibleRecipeIds.isEmpty(); }
    public List<String> visibleRecipeIds() { return visibleRecipeIds; }
    public Consumer<Player> backOpener() { return backOpener; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
