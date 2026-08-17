package com.hyunseo.hyunseorpg.ui;

import com.hyunseo.hyunseorpg.classsystem.ClassStatInvestmentResult;
import com.hyunseo.hyunseorpg.classsystem.ClassStatService;
import com.hyunseo.hyunseorpg.skill.SkillStatInvestmentResult;
import com.hyunseo.hyunseorpg.skill.SkillStatService;
import com.hyunseo.hyunseorpg.skill.SkillStatType;
import com.hyunseo.hyunseorpg.skill.SkillUpgradeResult;
import com.hyunseo.hyunseorpg.stat.StatInvestmentResult;
import com.hyunseo.hyunseorpg.stat.StatService;
import com.hyunseo.hyunseorpg.stat.StatType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class StatGuiListener implements Listener {
    private final JavaPlugin plugin;
    private final StatGuiService statGuiService;
    private final StatService statService;
    private final SkillStatService skillStatService;
    private final ClassStatService classStatService;

    public StatGuiListener(JavaPlugin plugin, StatGuiService statGuiService, StatService statService, SkillStatService skillStatService, ClassStatService classStatService) {
        this.plugin = plugin;
        this.statGuiService = statGuiService;
        this.statService = statService;
        this.skillStatService = skillStatService;
        this.classStatService = classStatService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof StatGuiHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        if (event.getClick() != ClickType.LEFT) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        if (holder.type() == StatGuiType.STAT) {
            handleStatClick(player, clickedItem);
            return;
        }
        if (holder.type() == StatGuiType.SKILL_STAT) {
            handleSkillStatClick(player, clickedItem);
            return;
        }
        if (holder.type() == StatGuiType.CLASS_STAT) {
            handleClassStatClick(player, clickedItem);
        }
    }

    private void handleStatClick(Player player, ItemStack clickedItem) {
        ItemMeta meta = clickedItem.getItemMeta();
        String statId = meta.getPersistentDataContainer().get(statGuiService.statTypeKey(), PersistentDataType.STRING);
        if (statId == null) {
            return;
        }

        StatType statType;
        try {
            statType = StatType.valueOf(statId.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return;
        }

        StatInvestmentResult result = statService.investStatPoint(player, statType);
        player.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
        Bukkit.getScheduler().runTask(plugin, () -> statGuiService.openStatGui(player));
    }

    private void handleSkillStatClick(Player player, ItemStack clickedItem) {
        ItemMeta meta = clickedItem.getItemMeta();
        String skillId = meta.getPersistentDataContainer().get(statGuiService.skillUpgradeKey(), PersistentDataType.STRING);
        if (skillId != null) {
            SkillUpgradeResult result = skillStatService.upgradeSkill(player, skillId);
            player.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
            Bukkit.getScheduler().runTask(plugin, () -> statGuiService.openSkillStatGui(player));
            return;
        }

        String skillStatId = meta.getPersistentDataContainer().get(statGuiService.skillStatTypeKey(), PersistentDataType.STRING);
        if (skillStatId == null) {
            return;
        }

        SkillStatType skillStatType;
        try {
            skillStatType = SkillStatType.valueOf(skillStatId.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return;
        }

        SkillStatInvestmentResult result = skillStatService.investSkillStatPoint(player, skillStatType);
        player.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
        Bukkit.getScheduler().runTask(plugin, () -> statGuiService.openSkillStatGui(player));
    }

    private void handleClassStatClick(Player player, ItemStack clickedItem) {
        ItemMeta meta = clickedItem.getItemMeta();
        String classStatId = meta.getPersistentDataContainer().get(statGuiService.classStatTypeKey(), PersistentDataType.STRING);
        if (classStatId == null) {
            return;
        }

        ClassStatInvestmentResult result = classStatService.investClassStatPoint(player, classStatId);
        player.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
        Bukkit.getScheduler().runTask(plugin, () -> statGuiService.openClassStatGui(player));
    }
}
