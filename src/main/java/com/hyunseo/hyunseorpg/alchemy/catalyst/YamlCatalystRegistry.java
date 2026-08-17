package com.hyunseo.hyunseorpg.alchemy.catalyst;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.Optional;

public final class YamlCatalystRegistry implements CatalystRegistry {
    private final ConfigService config;
    private volatile Map<String, CatalystDefinition> snapshot = Map.of();

    public YamlCatalystRegistry(ConfigService config) { this.config = config; }

    @Override
    public synchronized boolean reload() {
        var section = config.getAlchemyCatalystsSection("catalysts");
        if (section == null) return false;
        Map<String, CatalystDefinition> candidate = new LinkedHashMap<>();
        try {
            for (String rawId : section.getKeys(false)) {
                String id = normalize(rawId);
                String path = "catalysts." + rawId;
                String mode = config.getAlchemyCatalystString(path + ".mode", "");
                if (!id.matches("[a-z0-9]+(_[a-z0-9]+)*") || mode.isBlank()) return false;
                String material = config.getAlchemyCatalystString(path + ".vanilla-material",
                        config.getAlchemyCatalystString(path + ".material", id.toUpperCase(Locale.ROOT)));
                List<String> allowed = config.getAlchemyCatalystStringList(path + ".allowed-potions")
                        .stream().map(YamlCatalystRegistry::normalize).filter(value -> !value.isBlank()).toList();
                Map<String, String> inversion = new LinkedHashMap<>();
                var inversionSection = section.getConfigurationSection(rawId + ".inversion-effect-ids");
                if (inversionSection != null) {
                    for (String potion : inversionSection.getKeys(false)) {
                        String effect = normalize(inversionSection.getString(potion, ""));
                        if (!effect.isBlank()) inversion.put(normalize(potion), effect);
                    }
                }
                candidate.put(id, new CatalystDefinition(id,
                        config.getAlchemyCatalystBoolean(path + ".enabled", false),
                        CatalystDefinition.Mode.valueOf(mode.toUpperCase(Locale.ROOT)), material,
                        allowed, inversion,
                        config.getAlchemyCatalystInt(path + ".duration-multiplier-percent", 100),
                        config.getAlchemyCatalystInt(path + ".amplifier-delta", 0),
                        config.getAlchemyCatalystString(path + ".delivery", "ORIGINAL")));
            }
        } catch (IllegalArgumentException invalid) {
            return false;
        }
        snapshot = Map.copyOf(candidate);
        return true;
    }

    @Override public Optional<CatalystDefinition> find(String catalystId) { return Optional.ofNullable(snapshot.get(normalize(catalystId))); }
    @Override public Map<String, CatalystDefinition> all() { return snapshot; }
    private static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
}
