package com.hyunseo.hyunseorpg.alchemy.potion;

import com.hyunseo.hyunseorpg.alchemy.EffectService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class YamlPotionRegistry implements PotionRegistry {
    private final ConfigService config; private final EffectService effects;
    private Map<String, PotionDefinition> snapshot = Map.of();
    public YamlPotionRegistry(ConfigService config, EffectService effects) { this.config = config; this.effects = effects; }
    @Override public synchronized boolean reload() {
        Map<String, PotionDefinition> candidate = new LinkedHashMap<>();
        for (String raw : config.getAlchemyPotionKeys("potions")) {
            String path = "potions." + raw;
            String id = normalize(raw); String effectId = normalize(config.getAlchemyPotionString(path + ".effect-id", ""));
            try {
                PotionDefinition.Delivery delivery = PotionDefinition.Delivery.valueOf(config.getAlchemyPotionString(path + ".delivery", "DRINK").toUpperCase(Locale.ROOT));
                boolean enabled = config.getAlchemyPotionBoolean(path + ".enabled", false);
                if (effectId.isBlank()) return false;
                if (enabled && effects.registry().get(effectId).isEmpty()) return false;
                String outputItemId = normalize(config.getAlchemyPotionString(path + ".output-item-id", id));
                if (outputItemId.isBlank()) return false;
                candidate.put(id, new PotionDefinition(id, config.getAlchemyPotionString(path + ".display-name", id), delivery, effectId, outputItemId, enabled));
            } catch (IllegalArgumentException invalid) { return false; }
        }
        snapshot = Map.copyOf(candidate); return true;
    }
    @Override public synchronized java.util.Optional<PotionDefinition> find(String id) { return java.util.Optional.ofNullable(snapshot.get(normalize(id))); }
    @Override public synchronized Map<String, PotionDefinition> all() { return snapshot; }
    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
}
