package com.hyunseo.hyunseorpg.alchemy.catalyst;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class YamlSpecialCatalystRegistry implements SpecialCatalystRegistry {
    private final ConfigService config;
    private volatile Map<String, SpecialCatalystDefinition> snapshot = Map.of();

    public YamlSpecialCatalystRegistry(ConfigService config) { this.config = config; }

    @Override public synchronized boolean reload() {
        var section = config.getAlchemyCatalystsSection("catalysts");
        if (section == null) return false;
        Map<String, SpecialCatalystDefinition> candidate = new LinkedHashMap<>();
        try {
            for (String rawId : section.getKeys(false)) {
                String id = rawId.trim().toLowerCase(Locale.ROOT);
                String path = "catalysts." + rawId;
                String mode = config.getAlchemyCatalystString(path + ".mode", "");
                if (!"SPECIAL".equalsIgnoreCase(mode)) continue;
                String kind = config.getAlchemyCatalystString("catalysts." + rawId + ".kind", "");
                if (!id.matches("[a-z0-9]+(_[a-z0-9]+)*") || kind.isBlank()) return false;
                if (id.equals("sculk")) config.normalizeAlchemyCatalystMaterial(id, "SCULK_CATALYST");
                String material = config.getAlchemyCatalystString(path + ".vanilla-material",
                        config.getAlchemyCatalystString(path + ".material", id.toUpperCase(Locale.ROOT)));
                candidate.put(id, new SpecialCatalystDefinition(id,
                        config.getAlchemyCatalystBoolean(path + ".enabled", false),
                        SpecialCatalystDefinition.Kind.valueOf(kind.toUpperCase(Locale.ROOT)), material,
                        config.getAlchemyCatalystInt(path + ".max-propagation-count", 8),
                        config.getAlchemyCatalystDouble(path + ".radius", 8.0D),
                        config.getAlchemyCatalystInt(path + ".propagation-delay-ticks",
                                config.getAlchemyCatalystInt(path + ".delay-ticks", 10)),
                        config.getAlchemyCatalystInt(path + ".lifetime-ticks", 40),
                        config.getAlchemyCatalystDouble(path + ".max-distance", 16.0D),
                        config.getAlchemyCatalystDouble(path + ".max-total-distance",
                                config.getAlchemyCatalystDouble(path + ".max-distance", 16.0D) * 8.0D),
                        config.getAlchemyCatalystDouble(path + ".attenuation", 0.5D),
                        config.getAlchemyCatalystDouble(path + ".visual-radius", id.equals("sculk") ? 1.25D :
                                config.getAlchemyCatalystDouble(path + ".radius", 8.0D)),
                        config.getAlchemyCatalystInt(path + ".visual-color.red", id.equals("sculk") ? 10 : 255),
                        config.getAlchemyCatalystInt(path + ".visual-color.green", id.equals("sculk") ? 70 : 255),
                        config.getAlchemyCatalystInt(path + ".visual-color.blue", id.equals("sculk") ? 80 : 255)));
            }
        } catch (IllegalArgumentException invalid) { return false; }
        snapshot = Map.copyOf(candidate);
        return true;
    }
    @Override public Optional<SpecialCatalystDefinition> find(String id) { return Optional.ofNullable(snapshot.get(normalize(id))); }
    @Override public Map<String, SpecialCatalystDefinition> all() { return snapshot; }
    public String diagnostic(String id) {
        SpecialCatalystDefinition definition = find(id).orElse(null);
        return "catalyst=" + normalize(id) + " loaded-material="
                + (definition == null ? "MISSING" : definition.materialId());
    }
    private static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
}
