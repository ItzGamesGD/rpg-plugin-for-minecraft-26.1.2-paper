package com.hyunseo.hyunseorpg.alchemy.recipe;

import com.hyunseo.hyunseorpg.alchemy.potion.PotionRegistry;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.Material;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Disabled or unresolved recipes remain out of the active snapshot. */
public final class YamlAlchemyRecipeRegistry implements AlchemyRecipeRegistry {
    private final ConfigService config; private final PotionRegistry potions; private final RPGItemService items;
    private Map<String, AlchemyRecipeDefinition> snapshot = Map.of();
    public YamlAlchemyRecipeRegistry(ConfigService config, PotionRegistry potions, RPGItemService items) { this.config = config; this.potions = potions; this.items = items; }
    @Override public synchronized boolean reload() {
        Map<String, AlchemyRecipeDefinition> candidate = new LinkedHashMap<>();
        for (String raw : config.getAlchemyRecipeKeys("recipes")) {
            String path = "recipes." + raw; String id = normalize(raw);
            String result = normalize(config.getAlchemyRecipeString(path + ".result-potion-id", ""));
            boolean enabled = config.getAlchemyRecipeBoolean(path + ".enabled", false);
            if (!enabled) continue;
            if (result.isBlank() || potions.find(result).isEmpty()) return false;
            for (String ingredient : config.getAlchemyRecipeStringList(path + ".ingredients")) {
                String normalized = normalize(ingredient);
                if (!isKnownIngredient(normalized)) return false;
            }
            candidate.put(id, new AlchemyRecipeDefinition(id, result,
                    config.getAlchemyRecipeStringList(path + ".ingredients").stream().map(this::normalize).toList(), true));
        }
        snapshot = Map.copyOf(candidate); return true;
    }
    @Override public synchronized java.util.Optional<AlchemyRecipeDefinition> findByResult(String potionId) { return snapshot.values().stream().filter(r -> r.resultPotionId().equals(normalize(potionId))).findFirst(); }
    @Override public synchronized Map<String, AlchemyRecipeDefinition> all() { return snapshot; }
    private boolean isKnownIngredient(String id) {
        if (id.startsWith("vanilla:")) {
            String materialId = id.substring("vanilla:".length()).trim().toUpperCase(Locale.ROOT);
            return !materialId.isBlank() && Material.matchMaterial(materialId) != null;
        }
        return items.getData(id).isPresent() || id.equals("abundance_essence");
    }
    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
}
