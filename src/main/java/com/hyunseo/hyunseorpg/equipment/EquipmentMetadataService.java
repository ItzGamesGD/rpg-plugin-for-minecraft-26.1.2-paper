package com.hyunseo.hyunseorpg.equipment;

import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Common equipment metadata facade over the existing PDC layout.
 * It never removes or rewrites unrelated PDC values.
 */
public final class EquipmentMetadataService {
    public static final int CURRENT_DATA_VERSION = 1;

    private final RPGItemService itemService;
    private final EquipmentTierService tiers;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey enhancementLevelKey;
    private final NamespacedKey equippedEnchantsKey;
    private final NamespacedKey dataVersionKey;
    private final NamespacedKey killCountKey;
    private final NamespacedKey legacyEnhancementSchemaKey;
    private final NamespacedKey specialEquipmentSchemaKey;

    public EquipmentMetadataService(JavaPlugin plugin, RPGItemService itemService, EquipmentTierService tiers) {
        this.itemService = itemService;
        this.tiers = tiers;
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.enhancementLevelKey = new NamespacedKey(plugin, "enhancement_level");
        this.equippedEnchantsKey = new NamespacedKey(plugin, "equipped_enchants");
        this.dataVersionKey = new NamespacedKey(plugin, "equipment_data_version");
        this.killCountKey = new NamespacedKey(plugin, "equipment_kill_count");
        this.legacyEnhancementSchemaKey = new NamespacedKey(plugin, "equipment_schema_version");
        this.specialEquipmentSchemaKey = new NamespacedKey(plugin, "special_equipment_schema");
    }

    public boolean isEquipment(ItemStack item) {
        return tiers.isSupported(item);
    }

    /** Adds only missing common defaults. Existing growth and PDC fields remain untouched. */
    public boolean ensureDataVersion(ItemStack item) {
        if (!isEquipment(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        boolean changed = false;
        if (!pdc.has(dataVersionKey, PersistentDataType.INTEGER)
                || pdc.getOrDefault(dataVersionKey, PersistentDataType.INTEGER, 0) < CURRENT_DATA_VERSION) {
            pdc.set(dataVersionKey, PersistentDataType.INTEGER, CURRENT_DATA_VERSION);
            changed = true;
        }
        if (!pdc.has(killCountKey, PersistentDataType.LONG)) {
            pdc.set(killCountKey, PersistentDataType.LONG, 0L);
            changed = true;
        }
        if (changed) item.setItemMeta(meta);
        return changed;
    }

    public Optional<EquipmentData> read(ItemStack item) {
        if (!isEquipment(item) || !item.hasItemMeta()) return Optional.empty();
        ensureDataVersion(item);
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        String itemId = pdc.get(itemIdKey, PersistentDataType.STRING);
        if (itemId == null || itemId.isBlank()) itemId = "vanilla:" + item.getType().name().toLowerCase(Locale.ROOT);
        return Optional.of(new EquipmentData(
                itemId.toLowerCase(Locale.ROOT),
                tiers.getCategory(item),
                bounded(pdc.getOrDefault(enhancementLevelKey, PersistentDataType.INTEGER, 0)),
                readEnchantData(pdc.get(equippedEnchantsKey, PersistentDataType.STRING)),
                Math.max(0L, pdc.getOrDefault(killCountKey, PersistentDataType.LONG, 0L)),
                readFlags(pdc.getKeys()),
                readDataVersion(meta)
        ));
    }

    private int readDataVersion(ItemMeta meta) {
        var pdc = meta.getPersistentDataContainer();
        int common = pdc.getOrDefault(dataVersionKey, PersistentDataType.INTEGER, 0);
        int enhancement = pdc.getOrDefault(legacyEnhancementSchemaKey, PersistentDataType.INTEGER, 0);
        int special = pdc.getOrDefault(specialEquipmentSchemaKey, PersistentDataType.INTEGER, 0);
        return Math.max(common, Math.max(enhancement, special));
    }

    private List<String> readEnchantData(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private Set<String> readFlags(Set<NamespacedKey> keys) {
        Set<String> flags = new LinkedHashSet<>();
        for (NamespacedKey key : keys) flags.add(key.toString());
        return flags;
    }

    private int bounded(int value) {
        return Math.min(1000, Math.max(0, value));
    }

}
