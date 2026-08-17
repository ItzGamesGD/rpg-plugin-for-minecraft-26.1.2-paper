package com.hyunseo.hyunseorpg.alchemy.abundance;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class YamlEssenceRegistry implements EssenceRegistry {
    private final ConfigService config;
    private Map<String, EssenceDefinition> snapshot = Map.of();
    public YamlEssenceRegistry(ConfigService config) { this.config = config; }
    @Override public synchronized boolean reload() {
        Set<String> ids = config.getAlchemyAbundanceKeys("essences");
        Map<String, EssenceDefinition> candidate = new LinkedHashMap<>();
        for (String raw : ids) {
            String id = normalize(raw);
            if (!id.matches("[a-z0-9]+(_[a-z0-9]+)*")) return false;
            candidate.put(id, new EssenceDefinition(id,
                    config.getAlchemyAbundanceBoolean("essences." + raw + ".enabled", false),
                    config.getAlchemyAbundanceString("essences." + raw + ".balance", "BALANCE_PENDING")));
        }
        snapshot = Map.copyOf(candidate);
        return true;
    }
    @Override public synchronized java.util.Optional<EssenceDefinition> find(String id) { return java.util.Optional.ofNullable(snapshot.get(normalize(id))); }
    @Override public synchronized Map<String, EssenceDefinition> all() { return snapshot; }
    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
}
