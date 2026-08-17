package com.hyunseo.hyunseorpg.special;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/** Read-only special equipment recipe menu with live unlock feedback. */
public final class SpecialEquipmentMenuService implements Listener {
    private static final int PAGE_SIZE = 45;
    private final JavaPlugin plugin;
    private final SpecialEquipmentService service;

    public SpecialEquipmentMenuService(JavaPlugin plugin, SpecialEquipmentService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void open(Player player) { open(player, 0); }

    public void open(Player player, int page) {
        List<SpecialEquipmentData> entries = service.registry().getAll();
        int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, pages - 1));
        SpecialEquipmentMenuHolder holder = new SpecialEquipmentMenuHolder(safePage);
        Inventory inventory = Bukkit.createInventory(holder, 54, Component.text("특수 장비 제작"));
        holder.setInventory(inventory);
        int from = safePage * PAGE_SIZE;
        for (int index = from; index < Math.min(entries.size(), from + PAGE_SIZE); index++) {
            SpecialEquipmentData data = entries.get(index);
            inventory.setItem(index - from, display(player, data));
        }
        inventory.setItem(45, button(safePage > 0 ? Material.ARROW : Material.BARRIER, "이전 페이지"));
        inventory.setItem(49, button(Material.BARRIER, "닫기"));
        inventory.setItem(53, button(safePage + 1 < pages ? Material.ARROW : Material.BARRIER, "다음 페이지"));
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SpecialEquipmentMenuHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == 45) open(player, holder.page() - 1);
        else if (slot == 53) open(player, holder.page() + 1);
        else if (slot >= 0 && slot < PAGE_SIZE) {
            int index = holder.page() * PAGE_SIZE + slot;
            List<SpecialEquipmentData> entries = service.registry().getAll();
            if (index < entries.size()) service.craft(player, entries.get(index).id());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof SpecialEquipmentMenuHolder) event.setCancelled(true);
    }

    private ItemStack display(Player player, SpecialEquipmentData data) {
        ItemStack item = service.create(data.id(), 1);
        if (item == null) item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(Component.text("재료: " + data.recipeInputs(), NamedTextColor.GRAY));
        List<String> missing = service.missingRequirements(player, data);
        if (missing.isEmpty()) lore.add(Component.text("제작 가능: 클릭", NamedTextColor.GREEN));
        else {
            lore.add(Component.text("잠김: 조건 부족", NamedTextColor.RED));
            missing.stream().limit(6).forEach(value -> lore.add(Component.text("- " + value, NamedTextColor.GRAY)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack button(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}
