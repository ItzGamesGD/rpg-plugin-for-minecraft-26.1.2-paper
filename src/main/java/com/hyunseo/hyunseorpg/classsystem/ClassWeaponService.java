package com.hyunseo.hyunseorpg.classsystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import com.hyunseo.hyunseorpg.weapon.WeaponService;
import com.hyunseo.hyunseorpg.weapon.WeaponType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ClassWeaponService {
    private final NamespacedKey weaponIdKey;
    private final NamespacedKey weaponClassKey;
    private final WeaponService weaponService;

    public ClassWeaponService(JavaPlugin plugin) {
        this(plugin, new WeaponService(plugin));
    }

    public ClassWeaponService(JavaPlugin plugin, WeaponService weaponService) {
        this.weaponIdKey = new NamespacedKey(plugin, "weapon_id");
        this.weaponClassKey = new NamespacedKey(plugin, "weapon_class");
        this.weaponService = weaponService;
    }

    public ItemStack createWeapon(RPGClass rpgClass) {
        ItemStack weapon = new ItemStack(rpgClass.weaponMaterial());
        ItemMeta meta = weapon.getItemMeta();
        if (meta == null) {
            return weapon;
        }

        meta.displayName(Component.text(rpgClass.weaponDisplayName(), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(createLore(rpgClass));
        // Class weapons also use normal durability; repair is handled by HyunseoRPG.
        meta.setUnbreakable(false);
        if (rpgClass.customModelData() > 0) {
            meta.setCustomModelData(rpgClass.customModelData());
        }
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(weaponIdKey, PersistentDataType.STRING, rpgClass.weaponId());
        meta.getPersistentDataContainer().set(weaponClassKey, PersistentDataType.STRING, rpgClass.id());
        weaponService.markWeaponType(meta, WeaponType.fromLegacyClass(rpgClass));
        weapon.setItemMeta(meta);
        return weapon;
    }

    public void giveWeapon(Player player, RPGClass rpgClass) {
        giveItem(player, createWeapon(rpgClass));
        player.sendMessage(Component.text(rpgClass.weaponDisplayName() + "을(를) 지급했습니다.", NamedTextColor.GREEN));
    }

    public void giveAllWeapons(Player player) {
        for (RPGClass rpgClass : RPGClass.values()) {
            giveItem(player, createWeapon(rpgClass));
        }
        player.sendMessage(Component.text("모든 직업 무기를 지급했습니다.", NamedTextColor.GREEN));
    }

    public boolean isClassWeapon(ItemStack itemStack, RPGClass rpgClass) {
        return getClassFromWeapon(itemStack)
                .map(rpgClass::equals)
                .orElse(false);
    }

    public Optional<RPGClass> getClassFromWeapon(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return Optional.empty();
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }

        String weaponClass = meta.getPersistentDataContainer().get(weaponClassKey, PersistentDataType.STRING);
        if (weaponClass != null && !weaponClass.isBlank()) {
            return RPGClass.fromInput(weaponClass);
        }

        String weaponId = meta.getPersistentDataContainer().get(weaponIdKey, PersistentDataType.STRING);
        if (weaponId == null || weaponId.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(RPGClass.values())
                .filter(rpgClass -> rpgClass.weaponId().equals(weaponId))
                .findFirst();
    }

    private List<Component> createLore(RPGClass rpgClass) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("HyunseoRPG 직업 전용 무기", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("직업: " + rpgClass.koreanName(), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        if (rpgClass.weaponMaterial() == Material.BLAZE_ROD) {
            lore.add(Component.text("입력 안정화를 위한 더미 무기", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text("입력 방식", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        for (String line : rpgClass.weaponLore()) {
            lore.add(Component.text("- " + line, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text("판정 ID: " + rpgClass.weaponId(), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        if (rpgClass.customModelData() > 0) {
            lore.add(Component.text("CustomModelData: " + rpgClass.customModelData(), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        return lore;
    }

    private void giveItem(Player player, ItemStack itemStack) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}
