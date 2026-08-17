package com.hyunseo.hyunseorpg.progression;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemRegistry;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Loads and validates the shared hunt-to-growth item contract. */
public final class ProgressionLoopRegistry {
    private final ConfigService config;
    private final RPGItemRegistry itemRegistry;
    private final Map<String, ProgressionLoopStage> stages = new LinkedHashMap<>();
    private List<String> lastErrors = List.of();

    public ProgressionLoopRegistry(ConfigService config, RPGItemRegistry itemRegistry) {
        this.config = config;
        this.itemRegistry = itemRegistry;
    }

    public boolean loadAndValidate() {
        Map<String, ProgressionLoopStage> previous = new LinkedHashMap<>(stages);
        List<String> previousErrors = lastErrors;
        List<String> errors = new ArrayList<>();
        lastErrors = errors;
        stages.clear();
        ConfigurationSection root = config.getProgressionLoopSection("stages");
        if (root == null) {
            error("progression-loop.yml is missing stages");
            restore(previous, previousErrors);
            return false;
        }
        for (String rawId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(rawId);
            if (section == null) {
                error("Missing stage section: stages." + rawId);
                continue;
            }
            String id = normalize(rawId);
            stages.put(id, new ProgressionLoopStage(
                    id,
                    section.getString("display-name", id),
                    parseResources(section.getMapList("inputs")),
                    parseResources(section.getMapList("outputs")),
                    section.getStringList("next").stream().map(this::normalize).toList(),
                    section.getStringList("recipes").stream().map(this::normalize).toList()
            ));
        }
        boolean valid = validate();
        if (!valid) restore(previous, previousErrors);
        else lastErrors = List.of();
        return valid;
    }

    public List<String> lastErrors() {
        return List.copyOf(lastErrors);
    }

    private void restore(Map<String, ProgressionLoopStage> previous, List<String> previousErrors) {
        stages.clear();
        stages.putAll(previous);
        lastErrors = List.copyOf(previousErrors);
    }

    public List<ProgressionLoopStage> getAll() {
        return List.copyOf(stages.values());
    }

    public boolean validate() {
        boolean valid = true;
        Set<String> producedItems = new java.util.HashSet<>();
        Set<String> sourceItems = new java.util.HashSet<>();
        ConfigurationSection sources = config.getProgressionLoopSection("sources");
        if (sources != null) {
            for (String sourceId : sources.getKeys(false)) {
                sourceItems.addAll(parseResources(
                        sources.getMapList(sourceId + ".outputs")).stream()
                        .filter(ProgressionLoopResource::isItem)
                        .map(ProgressionLoopResource::itemId)
                        .toList());
            }
        }
        for (ProgressionLoopStage stage : stages.values()) {
            producedItems.addAll(stage.outputs().stream()
                    .filter(ProgressionLoopResource::isItem)
                    .map(ProgressionLoopResource::itemId)
                    .toList());
            for (ProgressionLoopResource resource : concat(stage.inputs(), stage.outputs())) {
                if (resource.type().isBlank()) {
                    error("Blank resource type in stage " + stage.id());
                    valid = false;
                }
                if (resource.isItem() && !itemExists(resource.itemId())) {
                    error("Unknown item ID in progression-loop.yml: " + resource.itemId()
                            + " (stage " + stage.id() + ")");
                    valid = false;
                }
            }
            for (String recipeId : stage.recipeIds()) {
                if (!recipeExists(recipeId)) {
                    error("Unknown recipe ID in progression-loop.yml: " + recipeId
                            + " (stage " + stage.id() + ")");
                    valid = false;
                }
            }
            for (String next : stage.nextStages()) {
                if (!stages.containsKey(next)) {
                    error("Unknown next stage '" + next + "' from " + stage.id());
                    valid = false;
                }
            }
        }

        for (ProgressionLoopStage stage : stages.values()) {
            for (ProgressionLoopResource input : stage.inputs()) {
                if (input.isItem() && !producedItems.contains(input.itemId())
                        && !sourceItems.contains(input.itemId())) {
                    error("Unreachable item input in progression-loop.yml: " + input.itemId()
                            + " (stage " + stage.id() + ")");
                    valid = false;
                }
            }
        }

        String enhancementStone = normalize(config.getEquipmentGrowthString(
                "enhancement.required-stone-item-id", "basic_upgrade_stone"));
        String promotionStone = normalize(config.getEquipmentGrowthString(
                "promotion.required-stone-item-id", "basic_promotion_stone"));
        String magicStone = "magic_stone";
        String fragment = normalize(config.getProgressionLoopString(
                "access-materials.fragment-item-id", "basic_upgrade_fragment"));
        valid &= requireItem("enhancement.required-stone-item-id", enhancementStone);
        valid &= requireItem("promotion.required-stone-item-id", promotionStone);
        valid &= requireItem("boss-sessions reward item", magicStone);
        valid &= requireItem("access-materials.fragment-item-id", fragment);

        ConfigurationSection craftingStage = config.getProgressionLoopSection("stages.crafting");
        if (craftingStage == null) {
            error("progression-loop.yml is missing stages.crafting");
            valid = false;
        } else if (craftingStage.getStringList("recipes").isEmpty()) {
            error("progression-loop.yml stages.crafting must declare at least one recipe");
            valid = false;
        }

        Logger logger = config.getPlugin().getLogger();
        if (valid) {
            logger.info("HyunseoRPG progression loop validated: " + stages.size() + " stages.");
        } else {
            logger.severe("HyunseoRPG progression loop validation failed. Check progression-loop.yml and item IDs.");
        }
        return valid;
    }

    private List<ProgressionLoopResource> parseResources(List<Map<?, ?>> rawResources) {
        List<ProgressionLoopResource> result = new ArrayList<>();
        for (Map<?, ?> raw : rawResources) {
            result.add(new ProgressionLoopResource(
                    String.valueOf(raw.containsKey("type") ? raw.get("type") : ""),
                    String.valueOf(raw.containsKey("id") ? raw.get("id") : "")));
        }
        return result;
    }

    private List<ProgressionLoopResource> concat(List<ProgressionLoopResource> first,
                                                  List<ProgressionLoopResource> second) {
        List<ProgressionLoopResource> result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }

    private boolean itemExists(String itemId) {
        return itemId != null && !itemId.isBlank() && itemRegistry.get(itemId).isPresent();
    }

    private boolean requireItem(String path, String itemId) {
        if (itemExists(itemId)) return true;
        error("Missing configured item for " + path + ": " + itemId);
        return false;
    }

    private boolean recipeExists(String recipeId) {
        ConfigurationSection recipes = config.getCraftingSection("crafting-recipes");
        if (recipes == null || recipeId == null || recipeId.isBlank()) return false;
        return recipes.getKeys(false).stream().anyMatch(id -> id.equalsIgnoreCase(recipeId));
    }

    private void error(String message) {
        if (lastErrors instanceof ArrayList<?> rawErrors) {
            @SuppressWarnings("unchecked") ArrayList<String> errors = (ArrayList<String>) rawErrors;
            errors.add(message);
        }
        config.getPlugin().getLogger().severe("[progression-loop] " + message);
    }

    private void warn(String message) {
        config.getPlugin().getLogger().warning("[progression-loop] " + message);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
