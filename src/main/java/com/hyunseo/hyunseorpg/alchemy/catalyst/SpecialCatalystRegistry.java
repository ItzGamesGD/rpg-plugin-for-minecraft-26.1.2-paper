package com.hyunseo.hyunseorpg.alchemy.catalyst;

import java.util.Map;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;

public interface SpecialCatalystRegistry {
    Optional<SpecialCatalystDefinition> find(String catalystId);
    Map<String, SpecialCatalystDefinition> all();
    boolean reload();

    default Optional<SpecialCatalystDefinition> findByItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        String material = item.getType().name();
        return all().values().stream()
                .filter(definition -> definition.materialId().equalsIgnoreCase(material))
                .findFirst();
    }
}
