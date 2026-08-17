package com.hyunseo.hyunseorpg.ui;

import com.hyunseo.hyunseorpg.classsystem.ClassStatData;
import com.hyunseo.hyunseorpg.classsystem.ClassStatService;
import com.hyunseo.hyunseorpg.skill.SkillData;
import com.hyunseo.hyunseorpg.skill.SkillStatService;
import com.hyunseo.hyunseorpg.skill.SkillStatType;
import com.hyunseo.hyunseorpg.stat.StatService;
import com.hyunseo.hyunseorpg.stat.StatType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/** Investment GUIs. Skill entries are deliberately shown for every weapon. */
public final class StatGuiService {
    public static final String STAT_TITLE = "스탯 창";
    public static final String SKILL_STAT_TITLE = "스킬 스탯 창";
    public static final String CLASS_STAT_TITLE = "무기 전문화 창";

    private final NamespacedKey statTypeKey;
    private final NamespacedKey skillStatTypeKey;
    private final NamespacedKey skillUpgradeKey;
    private final NamespacedKey classStatTypeKey;
    private final StatService statService;
    private final SkillStatService skillStatService;
    private final ClassStatService classStatService;

    public StatGuiService(JavaPlugin plugin, StatService statService, SkillStatService skillStatService, ClassStatService classStatService) {
        this.statTypeKey = new NamespacedKey(plugin, "stat_gui_stat_type");
        this.skillStatTypeKey = new NamespacedKey(plugin, "stat_gui_skill_stat_type");
        this.skillUpgradeKey = new NamespacedKey(plugin, "stat_gui_skill_upgrade");
        this.classStatTypeKey = new NamespacedKey(plugin, "stat_gui_class_stat_type");
        this.statService = statService;
        this.skillStatService = skillStatService;
        this.classStatService = classStatService;
    }

    public void openStatGui(Player player) {
        Inventory inventory = Bukkit.createInventory(new StatGuiHolder(StatGuiType.STAT), 27, Component.text(STAT_TITLE));
        int slot = 10;
        for (StatType statType : StatType.values()) {
            inventory.setItem(slot++, createStatItem(player, statType));
            if (slot == 17) {
                slot = 19;
            }
        }
        player.openInventory(inventory);
    }

    public void openSkillStatGui(Player player) {
        Inventory inventory = Bukkit.createInventory(new StatGuiHolder(StatGuiType.SKILL_STAT), 54, Component.text(SKILL_STAT_TITLE));
        int slot = 10;
        for (SkillStatType skillStatType : SkillStatType.values()) {
            inventory.setItem(slot++, createSkillStatItem(player, skillStatType));
            if (slot == 17) {
                slot = 19;
            }
        }
        slot = 37;
        for (SkillData skillData : skillStatService.skillRegistry().getAll()) {
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot++, createSkillUpgradeItem(player, skillData));
        }
        player.openInventory(inventory);
    }

    public void openClassStatGui(Player player) {
        Inventory inventory = Bukkit.createInventory(new StatGuiHolder(StatGuiType.CLASS_STAT), 54, Component.text(CLASS_STAT_TITLE));
        int slot = 10;
        for (ClassStatData classStatData : classStatService.getAvailableStats(player)) {
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot++, createClassStatItem(player, classStatData));
            if (slot == 17) {
                slot = 19;
            }
        }
        player.openInventory(inventory);
    }

    public NamespacedKey statTypeKey() { return statTypeKey; }
    public NamespacedKey skillStatTypeKey() { return skillStatTypeKey; }
    public NamespacedKey skillUpgradeKey() { return skillUpgradeKey; }
    public NamespacedKey classStatTypeKey() { return classStatTypeKey; }

    private ItemStack createStatItem(Player player, StatType statType) {
        ItemStack itemStack = new ItemStack(iconFor(statType));
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text(statType.name(), NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("레벨: " + statService.getStatLevel(player, statType) + "/" + statService.getMaxBaseStat(), NamedTextColor.GRAY),
                Component.text("현재 값: " + format(statService.getEffectiveStat(player, statType)), NamedTextColor.AQUA),
                Component.text("스탯 포인트: " + statService.getStatPoints(player), NamedTextColor.YELLOW),
                Component.text("좌클릭: 포인트 1개 투자", NamedTextColor.GREEN)
        ));
        meta.getPersistentDataContainer().set(statTypeKey, PersistentDataType.STRING, statType.name());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private ItemStack createSkillStatItem(Player player, SkillStatType skillStatType) {
        ItemStack itemStack = new ItemStack(skillStatType.iconMaterial());
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text(skillStatType.displayName(), NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(
                Component.text("레벨: " + skillStatService.getSkillStatLevel(player, skillStatType) + "/" + skillStatService.getMaxSkillStat(), NamedTextColor.GRAY),
                Component.text("스킬 포인트: " + skillStatService.getSkillPoints(player), NamedTextColor.YELLOW),
                Component.text("좌클릭: 포인트 1개 투자", NamedTextColor.GREEN)
        ));
        meta.getPersistentDataContainer().set(skillStatTypeKey, PersistentDataType.STRING, skillStatType.name());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private ItemStack createSkillUpgradeItem(Player player, SkillData skillData) {
        ItemStack itemStack = new ItemStack(skillData.iconMaterial());
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text(skillData.displayName(), NamedTextColor.AQUA));
        int level = skillStatService.getSkillLevel(player, skillData);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(level <= 0 ? "상태: 잠김" : "상태: 해금됨", level <= 0 ? NamedTextColor.RED : NamedTextColor.GREEN));
        lore.add(Component.text("레벨: " + level + "/" + skillData.maxLevel(), NamedTextColor.GRAY));
        lore.add(Component.text("필요 무기: " + skillData.weaponType().displayName(), NamedTextColor.AQUA));
        lore.add(Component.text("필요 숙련도: Lv." + skillData.requiredProficiencyLevel(), NamedTextColor.AQUA));
        lore.add(Component.text("마나 비용: " + format(skillData.manaCost()), NamedTextColor.AQUA));
        lore.add(Component.text("스킬 포인트: " + skillStatService.getSkillPoints(player), NamedTextColor.YELLOW));
        lore.add(Component.text("좌클릭: 해금 또는 강화", NamedTextColor.GREEN));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(skillUpgradeKey, PersistentDataType.STRING, skillData.skillId());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private ItemStack createClassStatItem(Player player, ClassStatData classStatData) {
        ItemStack itemStack = new ItemStack(classStatData.iconMaterial());
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text(classStatData.displayName(), NamedTextColor.GREEN));
        int level = classStatService.getClassStatLevel(player, classStatData);
        meta.lore(List.of(
                Component.text("레벨: " + level + "/" + classStatData.maxLevel(), NamedTextColor.GRAY),
                Component.text("대상 스킬: " + classStatData.skillId(), NamedTextColor.AQUA),
                Component.text("레벨당 증가량: " + format(classStatData.bonusPerLevel()), NamedTextColor.AQUA),
                Component.text("무기 전문화 포인트: " + classStatService.getClassStatPoints(player), NamedTextColor.YELLOW),
                Component.text("좌클릭: 포인트 " + classStatData.pointCost() + "개 투자", NamedTextColor.GREEN)
        ));
        meta.getPersistentDataContainer().set(classStatTypeKey, PersistentDataType.STRING, classStatData.fullId());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private Material iconFor(StatType statType) {
        return switch (statType) {
            case MAX_HEALTH -> Material.RED_DYE;
            case MAX_MANA -> Material.LAPIS_LAZULI;
            case MANA_REGEN -> Material.GLOWSTONE_DUST;
            case ATTACK -> Material.IRON_SWORD;
            case ATTACK_SPEED -> Material.FEATHER;
            case MOVE_SPEED -> Material.SUGAR;
            case DAMAGE_REDUCTION -> Material.SHIELD;
        };
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
