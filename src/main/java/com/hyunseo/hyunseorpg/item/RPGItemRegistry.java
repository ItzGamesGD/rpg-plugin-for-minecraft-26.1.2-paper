package com.hyunseo.hyunseorpg.item;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.ui.KoreanDisplay;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;

/** Central registry for all custom drop, crafting, and enhancement items. */
public final class RPGItemRegistry {
    private static final Set<String> RETIRED_SPECIAL_ITEM_IDS = Set.of(
            "fire_sword", "fire_bow", "water_sword", "wind_sword", "wind_bow",
            "earth_sword", "earth_bow", "earth_mace", "ice_sword", "ice_bow",
            "magic_sword", "magic_bow");
    private final ConfigService configService;
    private final Map<String, RPGItemData> itemsById = new ConcurrentHashMap<>();

    public RPGItemRegistry(ConfigService configService) {
        this.configService = configService;
    }

    public void load() {
        itemsById.clear();
        FileConfiguration bundledItems = loadBundledItems();
        for (String rawId : configService.getItemsKeys("items")) {
            String id = normalize(rawId);
            if (RETIRED_SPECIAL_ITEM_IDS.contains(id)) {
                configService.getPlugin().getLogger().info(
                        "Ignoring retired special equipment item definition: " + id);
                continue;
            }
            String path = "items." + rawId;
            Material material = parseMaterial(configService.getItemsString(path + ".material", "STONE"));
            if (material == null || !material.isItem()) {
                continue;
            }
            String configuredDisplayName = configService.getItemsString(path + ".display-name", id);
            String bundledDisplayName = bundledItems == null
                    ? ""
                    : bundledItems.getString("items." + rawId + ".display-name", "");
            String displayName = KoreanDisplay.canonicalItemName(id)
                    .orElseGet(() -> localizedDisplayName(configuredDisplayName, bundledDisplayName, id));
            List<String> configuredLore = configService.getItemsStringList(path + ".lore");
            List<String> bundledLore = bundledItems == null
                    ? List.of()
                    : bundledItems.getStringList("items." + rawId + ".lore");
            List<String> lore = KoreanDisplay.canonicalItemLore(id)
                    .orElseGet(() -> localizedLore(configuredLore, bundledLore));
            String category = configService.getItemsString(path + ".category", "MATERIAL");
            if (isRetiredCookingDefinition(id, category)) {
                configService.getPlugin().getLogger().info(
                        "Ignoring retired cooking item definition: " + id);
                continue;
            }
            if (isLegacyProfessionDefinition(id, category, path)) {
                // Old PDC items remain harmlessly readable by RPGItemService, but
                // no new registry entry, recipe, shop product, or reward is created.
                configService.getPlugin().getLogger().warning(
                        "Ignoring legacy profession item definition: " + id);
                continue;
            }
            itemsById.put(id, new RPGItemData(
                    id,
                    displayName,
                    material,
                    category,
                    configService.getItemsString(path + ".rarity", ""),
                    Math.max(0, configService.getItemsInt(path + ".custom-model-data", 0)),
                    lore,
                    configService.getItemsString(path + ".use-effect", ""),
                    Math.max(0, configService.getItemsInt(path + ".use-duration-seconds", 0)),
                    Math.max(0, configService.getItemsInt(path + ".use-amplifier", 0)),
                     configService.getItemsBoolean(path + ".consume-on-use", false),
                     configService.getItemsStringList(path + ".tags")
            ));
        }
    }

    private FileConfiguration loadBundledItems() {
        try (InputStream stream = configService.getPlugin().getResource("items.yml")) {
            if (stream == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            configService.getPlugin().getLogger().warning(
                    "Unable to load bundled items.yml for display fallback: " + exception.getMessage());
            return null;
        }
    }

    private String localizedDisplayName(String configured, String bundled, String id) {
        if (configured == null || configured.isBlank() || configured.equalsIgnoreCase(id)) {
            return bundled.isBlank() ? fallbackDisplayName(id) : bundled;
        }
        if (containsLatin(configured) && !bundled.isBlank() && !containsLatin(bundled)) {
            return bundled;
        }
        return configured;
    }

    private List<String> localizedLore(List<String> configured, List<String> bundled) {
        if (!bundled.isEmpty() && containsLatin(configured) && !containsLatin(bundled)) {
            return bundled;
        }
        return configured;
    }

    private boolean containsLatin(String value) {
        return value != null && value.matches(".*[A-Za-z].*");
    }

    private boolean containsLatin(List<String> values) {
        return values != null && values.stream().anyMatch(this::containsLatin);
    }

    private boolean isRetiredCookingDefinition(String id, String category) {
        String normalizedCategory = category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
        return normalizedCategory.equals("FARMING_COOKED") || id.startsWith("cooked_");
    }

    public Optional<RPGItemData> get(String itemId) {
        return Optional.ofNullable(itemsById.get(normalize(itemId)));
    }

    public List<RPGItemData> getAll() {
        return itemsById.values().stream().sorted(Comparator.comparing(RPGItemData::itemId)).toList();
    }

    private boolean isLegacyProfessionDefinition(String id, String category, String path) {
        String normalizedCategory = category == null ? "" : category.toUpperCase(Locale.ROOT);
        if (normalizedCategory.startsWith("PROFESSION_") || normalizedCategory.startsWith("JOB_")) return true;
        boolean hasProfessionFields = !configService.getItemsString(path + ".profession-bonus.activity", "").isBlank()
                || !configService.getItemsString(path + ".job-bonus.activity", "").isBlank()
                || !configService.getItemsString(path + ".required-profession", "").isBlank()
                || !configService.getItemsString(path + ".profession", "").isBlank()
                || !configService.getItemsString(path + ".job", "").isBlank()
                || configService.getItemsDouble(path + ".coin-bonus", -1.0D) >= 0.0D
                || configService.getItemsDouble(path + ".extra-drop-chance", -1.0D) >= 0.0D;
        if (hasProfessionFields) return true;

        String normalizedId = id.toLowerCase(Locale.ROOT);
        return normalizedId.contains("lumberjack") || normalizedId.contains("woodcutter")
                || normalizedId.contains("farmer") || normalizedId.contains("farming")
                || normalizedId.contains("hunter") || normalizedId.contains("hunting")
                || normalizedId.contains("profession") || normalizedId.contains("job")
                || normalizedId.equals("miner_grace")
                || normalizedId.equals("miner_blessing")
                || normalizedId.equals("miner_blessing_bundle");
    }

    private Material parseMaterial(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.regionMatches(true, 0, "minecraft:", 0, "minecraft:".length())) {
            normalized = normalized.substring("minecraft:".length());
        }
        return Material.matchMaterial(normalized.toUpperCase(Locale.ROOT));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String fallbackDisplayName(String id) {
        return switch (id) {
            case "magic_stone" -> "마석";
            case "enchant_book_blade_throw" -> "칼날 연쇄 인챈트 북";
            case "enchant_book_light_greatsword" -> "빛의 대검 인챈트 북";
            case "enchant_book_laser_arrow" -> "레이저 화살 인챈트 북";
            case "abundance_essence" -> "풍요의 정수";
            case "vitality_stat_token" -> "생명의 증표";
            case "satiety_stat_token" -> "포만의 증표";
            case "abundance_stat_token" -> "풍요의 증표";
            case "seed_corn" -> "옥수수 씨앗";
            case "seed_onion" -> "양파 씨앗";
            case "seed_chili" -> "고추 씨앗";
            case "seed_garlic" -> "마늘 씨앗";
            case "crop_corn" -> "옥수수";
            case "crop_onion" -> "양파";
            case "crop_chili" -> "고추";
            case "crop_garlic" -> "마늘";
            default -> id;
        };
    }
}
