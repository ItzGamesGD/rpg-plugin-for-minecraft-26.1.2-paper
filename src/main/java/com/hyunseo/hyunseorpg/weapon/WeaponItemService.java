package com.hyunseo.hyunseorpg.weapon;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.equipment.EquipmentMetadataService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;

/** Creates non-exclusive starter weapons that are usable by every player. */
public final class WeaponItemService {
    private final ConfigService configService;
    private final WeaponService weaponService;
    private final RPGItemService itemService;
    private EquipmentMetadataService equipmentMetadataService;

    public WeaponItemService(ConfigService configService, WeaponService weaponService, RPGItemService itemService) {
        this.configService = configService;
        this.weaponService = weaponService;
        this.itemService = itemService;
    }

    public void setEquipmentMetadataService(EquipmentMetadataService equipmentMetadataService) {
        this.equipmentMetadataService = equipmentMetadataService;
    }

    public ItemStack create(WeaponType weaponType) {
        String path = "weapons." + weaponType.id();
        Material material = parseMaterial(configService.getWeaponsString(path + ".material", defaultMaterial(weaponType).name()));
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }
        meta.displayName(Component.text(configService.getWeaponsString(path + ".display-name", weaponType.displayName()), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(Component.text("HyunseoRPG 기본 무기", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        // RPG weapons use normal durability so the repair menu can be tested.
        meta.setUnbreakable(false);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        int customModelData = configService.getWeaponsInt(path + ".custom-model-data", 0);
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        weaponService.markWeaponType(meta, weaponType);
        itemService.markItemId(meta, "basic_" + weaponType.id());
        itemStack.setItemMeta(meta);
        if (equipmentMetadataService != null) equipmentMetadataService.ensureDataVersion(itemStack);
        return itemStack;
    }

    private Material defaultMaterial(WeaponType weaponType) {
        return switch (weaponType) {
            case SWORD -> Material.IRON_SWORD;
            case BOW -> Material.BOW;
            case SPEAR -> Material.TRIDENT;
            case AXE -> Material.IRON_AXE;
            case MAGIC -> Material.BLAZE_ROD;
        };
    }

    private Material parseMaterial(String value) {
        Material material = Material.matchMaterial(value.trim().toUpperCase(Locale.ROOT));
        return material == null || !material.isItem() ? Material.STICK : material;
    }
}
