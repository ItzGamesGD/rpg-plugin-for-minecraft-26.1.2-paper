package com.hyunseo.hyunseorpg.equipment;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.enhancement.EnhancementRegistry;
import com.hyunseo.hyunseorpg.item.RPGItemData;
import com.hyunseo.hyunseorpg.item.RPGItemRegistry;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentData;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentRegistry;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Canonical metadata registry for equipment definitions.
 * It is a read model over existing item, enhancement, tier and special registries.
 */
public final class EquipmentRegistry {
    private final ConfigService config;
    private final RPGItemRegistry items;
    private final RPGItemService itemService;
    private final EnhancementRegistry enhancement;
    private final EquipmentTierService tiers;
    private final SpecialEquipmentRegistry specialEquipment;
    private final Map<String, EquipmentDefinition> definitions = new LinkedHashMap<>();

    public EquipmentRegistry(ConfigService config, RPGItemRegistry items, RPGItemService itemService,
                             EnhancementRegistry enhancement, EquipmentTierService tiers,
                             SpecialEquipmentRegistry specialEquipment) {
        this.config = config;
        this.items = items;
        this.itemService = itemService;
        this.enhancement = enhancement;
        this.tiers = tiers;
        this.specialEquipment = specialEquipment;
    }

    public void load() {
        definitions.clear();
        for (Material material : Material.values()) {
            if (!material.isItem() || material.isAir()) continue;
            ItemStack item = new ItemStack(material);
            if (tiers.isSupported(item)) {
                register("vanilla:" + material.name().toLowerCase(Locale.ROOT), item, false, null);
            }
        }

        for (RPGItemData data : items.getAll()) {
            ItemStack item = new ItemStack(data.material());
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            itemService.markItemId(meta, data.itemId());
            item.setItemMeta(meta);
            if (tiers.isSupported(item)) register(data.itemId(), item, true, data);
        }

        // Starter weapons are created by WeaponItemService and therefore do not
        // have to be present in items.yml to be visible in the registry.
        for (String rawId : config.getWeaponsKeys("weapons")) {
            String id = normalize("basic_" + rawId);
            Material material = Material.matchMaterial(config.getWeaponsString("weapons." + rawId + ".material", ""));
            if (material == null || !material.isItem()) continue;
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            itemService.markItemId(meta, id);
            item.setItemMeta(meta);
            if (tiers.isSupported(item)) register(id, item, true, null);
        }
    }

    public Optional<EquipmentDefinition> get(String equipmentId) {
        return Optional.ofNullable(definitions.get(normalize(equipmentId)));
    }

    public Optional<EquipmentDefinition> find(ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        String id = itemService.getItemId(item).orElse("vanilla:" + item.getType().name().toLowerCase(Locale.ROOT));
        return get(id);
    }

    public List<EquipmentDefinition> getAll() {
        return definitions.values().stream()
                .sorted(Comparator.comparing(EquipmentDefinition::equipmentId))
                .toList();
    }

    private void register(String id, ItemStack item, boolean custom, RPGItemData itemData) {
        String normalizedId = normalize(id);
        var profile = enhancement.findProfile(custom ? normalizedId : null, item.getType());
        boolean upgradeAllowed = profile.isPresent();
        boolean special = false;
        SpecialEquipmentData specialData = specialEquipment.get(normalizedId).orElse(null);
        if (specialData != null) {
            special = true;
            upgradeAllowed = false;
        }
        if (itemData != null && "SPECIAL_EQUIPMENT".equalsIgnoreCase(itemData.category())) special = true;

        boolean endgame = config.getEquipmentGrowthBoolean("equipment-registry." + normalizedId + ".endgame", false);
        if (endgame) {
            upgradeAllowed = false;
        }
        definitions.put(normalizedId, new EquipmentDefinition(
                normalizedId,
                tiers.getCategory(item),
                upgradeAllowed,
                special,
                endgame
        ));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
