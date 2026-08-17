package com.hyunseo.hyunseorpg.alchemy.gui;

public interface InventoryEventAdapter<E> {
    ClickAction classify(E event);
    boolean isAlchemyInventory(E event);
    boolean isCraftButton(E event);
    boolean isOutputSlot(E event);
    boolean isInputSlot(E event);
    boolean isPlayerInventory(E event);
    enum ClickAction { NORMAL, SHIFT_CLICK, NUMBER_KEY, SWAP_OFFHAND, DRAG, DOUBLE_CLICK, COLLECT_TO_CURSOR, HOTBAR_SWAP, HOPPER_TRANSFER, CLOSE, UNKNOWN }
}
