package com.hyunseo.hyunseorpg.player;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Opaque persistence payload for retired quest keys.
 *
 * <p>This type deliberately has no quest-domain interpretation or mutation API. It exists only so
 * loading and subsequently saving a legacy player file does not discard data an administrator may
 * still need for archival or downgrade purposes.</p>
 */
final class LegacyQuestCompatibilityData {
    private static final List<String> ROOTS = List.of("questStates", "questProgress", "quests");

    private final Map<String, Object> roots;

    private LegacyQuestCompatibilityData(Map<String, Object> roots) {
        this.roots = Map.copyOf(roots);
    }

    static LegacyQuestCompatibilityData empty() {
        return new LegacyQuestCompatibilityData(Map.of());
    }

    static LegacyQuestCompatibilityData capture(YamlConfiguration yaml) {
        Map<String, Object> roots = new LinkedHashMap<>();
        for (String root : ROOTS) {
            if (yaml.contains(root)) {
                roots.put(root, copyValue(yaml.get(root)));
            }
        }
        return new LegacyQuestCompatibilityData(roots);
    }

    void writeTo(YamlConfiguration yaml) {
        roots.forEach((path, value) -> yaml.set(path, copyValue(value)));
    }

    private static Object copyValue(Object value) {
        if (value instanceof ConfigurationSection section) {
            Map<String, Object> copied = new LinkedHashMap<>();
            section.getValues(false).forEach((key, child) -> copied.put(key, copyValue(child)));
            return copied;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copied = new LinkedHashMap<>();
            map.forEach((key, child) -> copied.put(String.valueOf(key), copyValue(child)));
            return copied;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(LegacyQuestCompatibilityData::copyValue).toList();
        }
        return value;
    }
}
