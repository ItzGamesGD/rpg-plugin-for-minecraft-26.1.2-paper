package com.hyunseo.hyunseorpg.weapon;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/** Resolves both normal Minecraft weapons and legacy HyunseoRPG weapons. */
public final class WeaponService {
    private final NamespacedKey weaponTypeKey;
    private final NamespacedKey legacyWeaponClassKey;

    public WeaponService(JavaPlugin plugin) {
        this.weaponTypeKey = new NamespacedKey(plugin, "weapon_type");
        this.legacyWeaponClassKey = new NamespacedKey(plugin, "weapon_class");
    }

    public Optional<WeaponType> getWeaponType(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return Optional.empty();
        }

        if (itemStack.hasItemMeta()) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                String storedType = meta.getPersistentDataContainer().get(weaponTypeKey, PersistentDataType.STRING);
                Optional<WeaponType> explicitType = WeaponType.fromInput(storedType);
                if (explicitType.isPresent()) {
                    return explicitType;
                }

                String legacyClass = meta.getPersistentDataContainer().get(legacyWeaponClassKey, PersistentDataType.STRING);
                Optional<WeaponType> migrated = legacyWeaponType(legacyClass);
                if (migrated.isPresent()) return migrated;
            }
        }

        for (WeaponType weaponType : WeaponType.values()) {
            if (weaponType.matchesMaterial(itemStack.getType())) {
                return Optional.of(weaponType);
            }
        }
        return Optional.empty();
    }

    public boolean isWeapon(ItemStack itemStack) {
        return getWeaponType(itemStack).isPresent();
    }

    public boolean isWeaponType(ItemStack itemStack, WeaponType weaponType) {
        return getWeaponType(itemStack).filter(weaponType::equals).isPresent();
    }

    public void markWeaponType(ItemMeta meta, WeaponType weaponType) {
        meta.getPersistentDataContainer().set(weaponTypeKey, PersistentDataType.STRING, weaponType.id());
    }

    /** Read-only item migration: no player class or class restriction is restored. */
    private Optional<WeaponType> legacyWeaponType(String value) {
        if (value == null) return Optional.empty();
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "swordmaster", "assassin" -> Optional.of(WeaponType.SWORD);
            case "bowmaster" -> Optional.of(WeaponType.BOW);
            case "lancer" -> Optional.of(WeaponType.SPEAR);
            case "wizard" -> Optional.of(WeaponType.MAGIC);
            default -> Optional.empty();
        };
    }
}
