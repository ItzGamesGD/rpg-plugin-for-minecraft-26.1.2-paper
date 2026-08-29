package com.hyunseo.hyunseorpg.exploration.listener;

import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntimeManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/** Records actual item removal from an outpost container without blocking vanilla inventory behavior. */
public final class ExplorationChestLootListener implements Listener {
    private static final Set<InventoryAction> REMOVAL_ACTIONS = Set.of(
            InventoryAction.PICKUP_ALL,
            InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_ONE,
            InventoryAction.PICKUP_SOME,
            InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.HOTBAR_MOVE_AND_READD,
            InventoryAction.HOTBAR_SWAP,
            InventoryAction.SWAP_WITH_CURSOR,
            InventoryAction.DROP_ALL_SLOT,
            InventoryAction.DROP_ONE_SLOT,
            InventoryAction.COLLECT_TO_CURSOR);

    private final ExplorationRuntimeManager runtimes;
    private final AtomicLong tickCounter;

    public ExplorationChestLootListener(ExplorationRuntimeManager runtimes, AtomicLong tickCounter) {
        this.runtimes = runtimes;
        this.tickCounter = tickCounter;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isLootRemovalAction(event.getAction())) return;
        Inventory top = event.getView().getTopInventory();
        if (event.getClickedInventory() != top) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
        Location location = resolveLocation(top);
        if (!isSupportedLootContainer(location)) return;
        runtimes.markLootTaken(location, player, tickCounter.get());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!isDragExtraction(top.getSize(), event.getRawSlots(),
                event.getOldCursor() != null && !event.getOldCursor().getType().isAir(), event.isCancelled())) return;
        Location location = resolveLocation(top);
        if (!isSupportedLootContainer(location)) return;
        // markLootTaken owns exact Pyramid chest identity and its durable duplicate
        // suppression; click and drag deliberately converge on that single path.
        runtimes.markLootTaken(location, player, tickCounter.get());
    }

    static boolean isLootRemovalAction(InventoryAction action) {
        return action != null && REMOVAL_ACTIONS.contains(action);
    }

    static boolean isDragExtraction(int topSize, Set<Integer> rawSlots,
                                    boolean cursorHasItem, boolean cancelled) {
        if (cancelled || !cursorHasItem || topSize < 0 || rawSlots == null || rawSlots.isEmpty()) return false;
        // A drag entirely into the top inventory is a deposit. A slot at or after
        // topSize is in the player's inventory and can finish distributing an item
        // previously extracted from the viewed chest.
        return rawSlots.stream().anyMatch(slot -> slot != null && slot >= topSize);
    }

    private Location resolveLocation(Inventory inventory) {
        if (inventory == null) return null;
        Location location = inventory.getLocation();
        if (location != null) return location;
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof BlockState state) return state.getLocation();
        if (holder instanceof DoubleChest doubleChest) return doubleChest.getLocation();
        return null;
    }

    private boolean isSupportedLootContainer(Location location) {
        if (location == null || location.getWorld() == null) return false;
        Block block = location.getBlock();
        Material type = block.getType();
        return type == Material.CHEST
                || type == Material.TRAPPED_CHEST
                || type == Material.BARREL
                || type == Material.SHULKER_BOX;
    }
}
