package com.hyunseo.hyunseorpg.crafting;

import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Preserves legacy item ownership metadata when an existing recipe requires it. */
public final class SoulboundItemService implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey ownerKey;
    private final RPGItemService itemService;
    private final Map<UUID, List<ItemStack>> pendingRestore = new ConcurrentHashMap<>();

    public SoulboundItemService(JavaPlugin plugin, RPGItemService itemService) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "soulbound_owner");
        this.itemService = itemService;
    }

    public void bind(ItemStack item, UUID owner) {
        if (item == null || item.getType().isAir()) return;
        var meta = item.getItemMeta();
        if (meta == null) return;
        // Ownership was removed from the V2 item flow. Remove legacy owner data when an old path touches an item.
        meta.getPersistentDataContainer().remove(ownerKey);
        item.setItemMeta(meta);
    }

    public boolean isSoulbound(ItemStack item) {
        return false;
    }

    public boolean isOwnedBy(ItemStack item, Player player) {
        return true;
    }

    public UUID owner(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public boolean requiresBinding(ItemStack item) {
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isSoulbound(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getItem().getItemStack();
        if (isSoulbound(item) && !isOwnedBy(item, player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent event) {
        if (isSoulbound(event.getItem())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory clicked = event.getClickedInventory();
        boolean top = clicked != null && clicked.equals(event.getView().getTopInventory());
        if (top && (isSoulbound(event.getCurrentItem()) || isSoulbound(event.getCursor()))) {
            if (isAllowedRecipeTransfer(event)) return;
            event.setCancelled(true);
            return;
        }
        if (event.isShiftClick() && isSoulbound(event.getCurrentItem())) event.setCancelled(true);
        if (event.getHotbarButton() >= 0 && top) {
            ItemStack hotbar = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            if (isSoulbound(hotbar)) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!isSoulbound(event.getOldCursor())) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)
                && !(event.getView().getTopInventory().getType() == InventoryType.WORKBENCH
                || event.getView().getTopInventory().getType() == InventoryType.CRAFTING
                || event.getView().getTopInventory().getType() == InventoryType.MERCHANT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof ItemFrame) && !(clicked instanceof ArmorStand)) return;
        if (isSoulbound(event.getPlayer().getInventory().getItemInMainHand())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        List<ItemStack> retained = new ArrayList<>();
        event.getDrops().removeIf(item -> {
            if (!isSoulbound(item) || !isOwnedBy(item, event.getEntity())) return false;
            retained.add(item.clone());
            return true;
        });
        if (!retained.isEmpty()) pendingRestore.computeIfAbsent(event.getEntity().getUniqueId(), ignored -> new ArrayList<>()).addAll(retained);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        List<ItemStack> retained = pendingRestore.remove(event.getPlayer().getUniqueId());
        if (retained == null || retained.isEmpty()) return;
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            for (ItemStack item : retained) {
                event.getPlayer().getInventory().addItem(item).values().forEach(leftover ->
                        event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), leftover));
            }
        });
    }

    private boolean isAllowedRecipeTransfer(InventoryClickEvent event) {
        InventoryType type = event.getView().getTopInventory().getType();
        if (type != InventoryType.WORKBENCH && type != InventoryType.CRAFTING && type != InventoryType.MERCHANT) return false;
        if (event.getWhoClicked() instanceof Player player) {
            return isOwnedBy(event.getCurrentItem(), player) || isOwnedBy(event.getCursor(), player);
        }
        return false;
    }
}
