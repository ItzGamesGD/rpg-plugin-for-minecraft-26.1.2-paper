package com.hyunseo.hyunseorpg.equipment;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Stores attached option IDs on equipment. This service intentionally only
 * manages attachment data; each effect is added through Combat/Stat services
 * in a later balance project.
 */
public final class EquipmentOptionService {
    private static final String SEPARATOR = ",";

    private final ConfigService configService;
    private final EquipmentOptionRegistry registry;
    private final NamespacedKey optionIdsKey;

    public EquipmentOptionService(JavaPlugin plugin, ConfigService configService, EquipmentOptionRegistry registry) {
        this.configService = configService;
        this.registry = registry;
        this.optionIdsKey = new NamespacedKey(plugin, "equipment_option_ids");
    }

    public List<EquipmentOptionData> getOptions(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return List.of();
        }
        String raw = itemStack.getItemMeta().getPersistentDataContainer().get(optionIdsKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<EquipmentOptionData> options = new ArrayList<>();
        for (String optionId : raw.split(SEPARATOR)) {
            registry.get(optionId).ifPresent(options::add);
        }
        return List.copyOf(options);
    }

    public EquipmentOptionApplyResult applyOption(ItemStack itemStack, String optionId) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return new EquipmentOptionApplyResult(false, "No equipment item selected.");
        }
        EquipmentOptionData option = registry.get(optionId).orElse(null);
        if (option == null) {
            return new EquipmentOptionApplyResult(false, "Unknown equipment option.");
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return new EquipmentOptionApplyResult(false, "Item cannot hold options.");
        }

        Set<String> optionIds = new LinkedHashSet<>();
        String raw = meta.getPersistentDataContainer().get(optionIdsKey, PersistentDataType.STRING);
        if (raw != null && !raw.isBlank()) {
            for (String storedId : raw.split(SEPARATOR)) {
                if (!storedId.isBlank()) {
                    optionIds.add(storedId.trim().toLowerCase());
                }
            }
        }
        if (optionIds.contains(option.id())) {
            return new EquipmentOptionApplyResult(false, "Option already attached.");
        }
        int maxSlots = Math.max(1, configService.getEquipmentOptionsKeys("settings").isEmpty()
                ? 3
                : (int) Math.round(configService.getEquipmentOptionsDouble("settings.default-option-slots", 3.0D)));
        if (optionIds.size() + option.slotCost() > maxSlots) {
            return new EquipmentOptionApplyResult(false, "Not enough option slots.");
        }

        optionIds.add(option.id());
        meta.getPersistentDataContainer().set(optionIdsKey, PersistentDataType.STRING, String.join(SEPARATOR, optionIds));
        itemStack.setItemMeta(meta);
        return new EquipmentOptionApplyResult(true, "Option attached: " + option.displayName());
    }
}
