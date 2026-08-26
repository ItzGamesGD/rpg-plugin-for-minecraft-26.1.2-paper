package com.hyunseo.hyunseorpg.item;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Delivers rewards to inventory first and drops only the overflow. */
public final class InventoryDeliveryService {
    private BiConsumer<Player, ItemStack> deliveryObserver;
    private Consumer<ItemStack> itemNormalizer = item -> { };
    private PendingRewardService pendingRewards;

    public void setPendingRewardService(PendingRewardService pendingRewards) {
        this.pendingRewards = pendingRewards;
    }

    public void setDeliveryObserver(BiConsumer<Player, ItemStack> deliveryObserver) {
        this.deliveryObserver = deliveryObserver;
    }

    /** Enqueues a completion reward durably under a deterministic idempotency token. */
    public boolean queueItemOnce(Player player, java.util.UUID token, ItemStack item, String cause) {
        if (player == null || token == null || item == null || item.getType().isAir() || item.getAmount() <= 0
                || pendingRewards == null) return false;
        pendingRewards.queueItemOnce(player.getUniqueId(), token, item, cause);
        return true;
    }

    public void setItemNormalizer(Consumer<ItemStack> itemNormalizer) {
        this.itemNormalizer = itemNormalizer == null ? item -> { } : itemNormalizer;
    }

    public void giveOrDrop(Player player, Location fallbackLocation, ItemStack itemStack) {
        if (player == null || itemStack == null || itemStack.getType().isAir() || itemStack.getAmount() <= 0) {
            return;
        }
        itemNormalizer.accept(itemStack);

        Location dropLocation = fallbackLocation == null ? player.getLocation() : fallbackLocation;
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
        notifyDelivered(player, itemStack, leftovers);
        leftovers.values().forEach(leftover -> {
            if (pendingRewards != null) {
                pendingRewards.queueItem(player.getUniqueId(), leftover, "inventory-overflow");
                pendingRewards.notifyPending(player);
                return;
            }
            if (dropLocation.getWorld() != null) {
                dropLocation.getWorld().dropItemNaturally(dropLocation, leftover);
            }
        });
    }

    /** Delivers RPG loot to storage and persists overflow instead of deleting it. */
    public void giveOrDiscard(Player player, ItemStack itemStack) {
        if (player == null || itemStack == null || itemStack.getType().isAir() || itemStack.getAmount() <= 0) {
            return;
        }
        itemNormalizer.accept(itemStack);
        if (pendingRewards != null) {
            pendingRewards.deliverOrQueue(player, itemStack, "inventory-delivery");
            notifyDelivered(player, itemStack, Map.of());
            return;
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
        notifyDelivered(player, itemStack, leftovers);
    }

    /** Main-thread exact delivery used by GUI transactions after a capacity preflight. */
    public boolean giveExactly(Player player, ItemStack itemStack) {
        if (player == null || itemStack == null || itemStack.getType().isAir() || itemStack.getAmount() <= 0) return false;
        itemNormalizer.accept(itemStack);
        ItemStack[] before = cloneContents(player.getInventory().getStorageContents());
        try {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack.clone());
            if (!leftovers.isEmpty()) {
                player.getInventory().setStorageContents(before);
                return false;
            }
            notifyDelivered(player, itemStack, Map.of());
            return true;
        } catch (RuntimeException failure) {
            player.getInventory().setStorageContents(before);
            return false;
        }
    }

    private ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[contents.length];
        for (int index = 0; index < contents.length; index++) {
            copy[index] = contents[index] == null ? null : contents[index].clone();
        }
        return copy;
    }

    private void notifyDelivered(Player player, ItemStack requested, Map<Integer, ItemStack> leftovers) {
        if (deliveryObserver == null) return;
        int remaining = leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
        if (remaining < requested.getAmount()) {
            deliveryObserver.accept(player, requested.clone());
        }
    }
}
