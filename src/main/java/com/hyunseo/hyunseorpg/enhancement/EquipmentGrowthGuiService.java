package com.hyunseo.hyunseorpg.enhancement;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** Navigation shell for the Minecraft-native enhancement and enchantment paths. */
public final class EquipmentGrowthGuiService {
    public static final int CLOSE_SLOT = 26;

    public EquipmentGrowthGuiService(org.bukkit.plugin.java.JavaPlugin plugin,
                                     EquipmentEnhancementService enhancementService) { }

    public void openMain(Player player) {
        EquipmentGrowthMenuHolder holder = new EquipmentGrowthMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("장비 성장"));
        holder.setInventory(inventory);
        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
        inventory.setItem(11, icon(Material.ANVIL, "강화", List.of("장비 + 강화석 + XP를 바닐라 모루에서 사용합니다.")));
        inventory.setItem(15, icon(Material.ENCHANTED_BOOK, "인챈트", List.of("인챈팅 테이블, 주민 거래, 전리품과 바닐라 모루를 사용합니다.")));
        inventory.setItem(CLOSE_SLOT, icon(Material.BARRIER, "닫기", List.of()));
        player.openInventory(inventory);
    }

    private ItemStack icon(Material material, String name, List<String> lines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(lines.stream().map(line -> Component.text(line, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }
}
