package com.hyunseo.hyunseorpg.alchemy.gui;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Maps Bukkit click semantics without using title text as an identity key. */
public final class PaperInventoryEventAdapter implements InventoryEventAdapter<InventoryClickEvent> {
    private final int craftButtonSlot;
    private final int outputSlot;
    private final java.util.Set<Integer> inputSlots;

    public PaperInventoryEventAdapter(int craftButtonSlot, int outputSlot, java.util.Set<Integer> inputSlots) {
        this.craftButtonSlot = craftButtonSlot;
        this.outputSlot = outputSlot;
        this.inputSlots = java.util.Set.copyOf(inputSlots);
    }

    @Override public ClickAction classify(InventoryClickEvent event) {
        if (event == null) return ClickAction.UNKNOWN;
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) return ClickAction.SHIFT_CLICK;
        if (event.getClick() == ClickType.NUMBER_KEY) return ClickAction.NUMBER_KEY;
        if (event.getClick() == ClickType.SWAP_OFFHAND) return ClickAction.SWAP_OFFHAND;
        if (event.getClick() == ClickType.DOUBLE_CLICK) return ClickAction.DOUBLE_CLICK;
        if (event.getClick() == ClickType.WINDOW_BORDER_LEFT || event.getClick() == ClickType.WINDOW_BORDER_RIGHT) return ClickAction.UNKNOWN;
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) return ClickAction.COLLECT_TO_CURSOR;
        if (event.getAction() == InventoryAction.HOTBAR_SWAP) return ClickAction.HOTBAR_SWAP;
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) return ClickAction.HOPPER_TRANSFER;
        return ClickAction.NORMAL;
    }

    @Override public boolean isAlchemyInventory(InventoryClickEvent event) {
        return event != null && event.getView().getTopInventory().getHolder() instanceof AlchemyMenuHolder;
    }
    @Override public boolean isCraftButton(InventoryClickEvent event) { return event != null && event.getRawSlot() == craftButtonSlot; }
    @Override public boolean isOutputSlot(InventoryClickEvent event) { return event != null && event.getRawSlot() == outputSlot; }
    @Override public boolean isInputSlot(InventoryClickEvent event) { return event != null && inputSlots.contains(event.getRawSlot()); }
    @Override public boolean isPlayerInventory(InventoryClickEvent event) {
        return event != null && event.getRawSlot() >= event.getView().getTopInventory().getSize();
    }
}
