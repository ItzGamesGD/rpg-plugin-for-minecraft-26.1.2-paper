package com.hyunseo.hyunseorpg.alchemy;

import net.kyori.adventure.text.Component;
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
import java.util.Comparator;
import java.util.List;

/** Read-only GUI for the player's currently active production effects. */
public final class EffectListGuiService implements Listener {
    private final JavaPlugin plugin;
    private final EffectService effects;

    public EffectListGuiService(JavaPlugin plugin, EffectService effects) {
        this.plugin = plugin;
        this.effects = effects;
    }

    public void open(Player player) {
        EffectListGuiHolder holder = new EffectListGuiHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54, Component.text("현재 상태 효과"));
        holder.setInventory(inventory);
        List<ActiveEffectInstance> active = new ArrayList<>(effects.getActive(player.getUniqueId()));
        active.sort(Comparator.comparing(instance -> instance.definition().displayName()));
        long now = effects.currentTick();
        for (int i = 0; i < Math.min(active.size(), 45); i++) {
            ActiveEffectInstance instance = active.get(i);
            CustomEffectDefinition definition = instance.definition();
            long remaining = instance.remainingTicks(now);
            ItemStack icon = new ItemStack(Material.POTION);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(Component.text(definition.displayName()));
            meta.lore(List.of(
                    Component.text("효과 ID: " + definition.id()),
                    Component.text("설명: " + definition.description()),
                    Component.text("남은 시간: " + formatTicks(remaining)),
                    Component.text("스택: " + instance.stacks() + "/" + definition.maxStacks()),
                    Component.text("출처: " + instance.source().type().name())));
            icon.setItemMeta(meta);
            inventory.setItem(i, icon);
        }
        if (active.isEmpty()) {
            ItemStack empty = new ItemStack(Material.GLASS_BOTTLE);
            ItemMeta meta = empty.getItemMeta();
            meta.displayName(Component.text("활성 상태 효과 없음"));
            meta.lore(List.of(Component.text("현재 적용 중인 상태 효과가 없습니다.")));
            empty.setItemMeta(meta);
            inventory.setItem(22, empty);
        }
        player.openInventory(inventory);
    }

    private String formatTicks(long ticks) {
        long seconds = Math.max(0L, ticks) / 20L;
        return (seconds / 60L) + "분 " + (seconds % 60L) + "초";
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof EffectListGuiHolder)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof EffectListGuiHolder) event.setCancelled(true);
    }
}
