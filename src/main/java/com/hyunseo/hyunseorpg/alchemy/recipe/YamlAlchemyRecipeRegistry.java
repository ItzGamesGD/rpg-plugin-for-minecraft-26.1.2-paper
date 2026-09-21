package com.hyunseo.hyunseorpg.alchemy.recipe;

import com.hyunseo.hyunseorpg.alchemy.catalyst.CatalystRegistry;
import com.hyunseo.hyunseorpg.alchemy.catalyst.SpecialCatalystRegistry;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionRegistry;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Builds the canonical snapshot of staged Brewing Stand transitions. */
public final class YamlAlchemyRecipeRegistry implements AlchemyRecipeRegistry {
    private final ConfigService config;
    private final PotionRegistry potions;
    private final RPGItemService items;
    private final CatalystRegistry catalysts;
    private final SpecialCatalystRegistry specialCatalysts;
    private volatile Map<String, AlchemyRecipeDefinition> snapshot = Map.of();

    public YamlAlchemyRecipeRegistry(ConfigService config, PotionRegistry potions, RPGItemService items,
                                     CatalystRegistry catalysts, SpecialCatalystRegistry specialCatalysts) {
        this.config = config;
        this.potions = potions;
        this.items = items;
        this.catalysts = catalysts;
        this.specialCatalysts = specialCatalysts;
    }

    @Override
    public synchronized boolean reload() {
        Map<String, AlchemyRecipeDefinition> candidate = new LinkedHashMap<>();
        for (String raw : config.getAlchemyRecipeKeys("recipes")) {
            String path = "recipes." + raw;
            if (!config.getAlchemyRecipeBoolean(path + ".enabled", false)) continue;
            String id = normalize(raw);
            String base = normalize(config.getAlchemyRecipeString(path + ".base-input-id", ""));
            String ingredient = normalize(config.getAlchemyRecipeString(path + ".ingredient-id", ""));
            String result = normalize(config.getAlchemyRecipeString(path + ".result-potion-id", ""));
            if (!isKnownBase(base) || !isKnownIngredient(ingredient) || potions.find(result).isEmpty()) return false;
            AlchemyRecipeDefinition recipe = new AlchemyRecipeDefinition(id, base, ingredient, result, true);
            boolean duplicate = candidate.values().stream().anyMatch(existing ->
                    existing.baseInputId().equals(base) && existing.ingredientId().equals(ingredient));
            if (duplicate) return false;
            candidate.put(id, recipe);
        }
        snapshot = Map.copyOf(candidate);
        return true;
    }

    @Override public Optional<AlchemyRecipeDefinition> findByResult(String potionId) {
        String id = normalize(potionId);
        return snapshot.values().stream().filter(recipe -> recipe.resultPotionId().equals(id)).findFirst();
    }

    @Override public Optional<AlchemyRecipeDefinition> findTransition(String baseInputId, String ingredientId) {
        String base = normalize(baseInputId);
        String ingredient = normalize(ingredientId);
        return snapshot.values().stream().filter(recipe -> recipe.enabled()
                && recipe.baseInputId().equals(base) && recipe.ingredientId().equals(ingredient)).findFirst();
    }

    @Override public Map<String, AlchemyRecipeDefinition> all() { return snapshot; }

    private boolean isKnownBase(String id) {
        return potions.find(id).isPresent() || isKnownIngredient(id);
    }

    private boolean isKnownIngredient(String id) {
        if (id.startsWith("vanilla:")) {
            return Material.matchMaterial(id.substring("vanilla:".length()).toUpperCase(Locale.ROOT)) != null;
        }
        if (id.startsWith("catalyst:")) {
            String catalystId = id.substring("catalyst:".length());
            return catalysts.find(catalystId).filter(definition -> definition.enabled()).isPresent()
                    || specialCatalysts.find(catalystId).filter(definition -> definition.enabled()).isPresent();
        }
        return items.getData(id).isPresent() || potions.find(id).isPresent();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
