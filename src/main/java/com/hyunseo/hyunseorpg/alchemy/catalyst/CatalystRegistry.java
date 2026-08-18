package com.hyunseo.hyunseorpg.alchemy.catalyst;

import java.util.Map;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;
import com.hyunseo.hyunseorpg.item.RPGItemService;

public interface CatalystRegistry {
    Optional<CatalystDefinition> find(String catalystId);
    Map<String, CatalystDefinition> all();
    boolean reload();

    default Optional<CatalystDefinition> findByItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        String material = item.getType().name();
        return all().values().stream()
                .filter(definition -> definition.materialId().equalsIgnoreCase(material))
                .findFirst();
    }

    default Optional<CatalystDefinition> findByItem(ItemStack item, RPGItemService items) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        if (items != null) {
            Optional<String> itemId = items.getItemId(item);
            if (itemId.isPresent()) {
                Optional<CatalystDefinition> custom = all().values().stream()
                        .filter(definition -> !definition.itemId().isBlank()
                                && definition.itemId().equalsIgnoreCase(itemId.get()))
                        .findFirst();
                if (custom.isPresent()) return custom;
            }
        }
        return findByItem(item);
    }
}
