package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/** Registry for future support features. It intentionally has no execution path while unimplemented. */
public final class FutureEquipmentFeatureRegistry {
    private final ConfigService config;

    public FutureEquipmentFeatureRegistry(ConfigService config) {
        this.config = config;
    }

    public List<Feature> getAll() {
        ConfigurationSection section = config.getEquipmentSupportSection("future-features");
        if (section == null) return List.of();
        List<Feature> result = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection item = section.getConfigurationSection(id);
            if (item == null) continue;
            result.add(new Feature(id, item.getString("display-name", id),
                    item.getString("description", "Future feature"),
                    item.getBoolean("enabled", false), item.getBoolean("implemented", false)));
        }
        return List.copyOf(result);
    }

    public record Feature(String id, String displayName, String description, boolean enabled, boolean implemented) { }
}
