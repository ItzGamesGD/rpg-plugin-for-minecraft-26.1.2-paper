package com.hyunseo.hyunseorpg.alchemy.catalyst;

import java.util.Map;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;

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
}
