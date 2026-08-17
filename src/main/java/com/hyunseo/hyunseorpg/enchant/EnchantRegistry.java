package com.hyunseo.hyunseorpg.enchant;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.equipment.trigger.DuplicatePolicy;
import com.hyunseo.hyunseorpg.equipment.trigger.EnchantTriggerBinding;
import com.hyunseo.hyunseorpg.equipment.trigger.SourceScope;
import com.hyunseo.hyunseorpg.equipment.trigger.TriggerPhase;
import com.hyunseo.hyunseorpg.equipment.trigger.TriggerType;
import com.hyunseo.hyunseorpg.skill.SkillInputType;
import com.hyunseo.hyunseorpg.weapon.WeaponType;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public final class EnchantRegistry {
    private static final Set<String> RETIRED_ENCHANTS = Set.of(
            "spear_charge", "spear_throw", "lancer", "paladins_blessing",
            "holy_counter", "homing_arrow", "hunters_mark");
    private static final Map<String, String> RETIRED_REASONS = Map.of(
            "spear_charge", "temporarily disabled: spear input implementation pending",
            "spear_throw", "temporarily disabled: spear input implementation pending",
            "lancer", "temporarily disabled: spear input implementation pending",
            "paladins_blessing", "temporarily disabled: shield input implementation pending",
            "holy_counter", "temporarily disabled: shield input implementation pending",
            "homing_arrow", "temporarily disabled: bow input implementation pending",
            "hunters_mark", "temporarily disabled: bow input implementation pending");
    private static final Set<String> LEGACY_DEFINITION_IDS = Set.of(
            "blade_throw", "area_mining_pickaxe", "durability_save_pickaxe");
    private final ConfigService config;
    private final java.util.Map<String, EnchantData> enchants = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, String> legacyAliases = new java.util.concurrent.ConcurrentHashMap<>();

    public EnchantRegistry(ConfigService config) {
        this.config = config;
    }

    public void load() {
        enchants.clear();
        legacyAliases.clear();
        for (String rawId : config.getEnchantsKeys("enchants")) {
            String path = "enchants." + rawId;
            String normalizedRawId = normalize(rawId);
            if (RETIRED_ENCHANTS.contains(normalizedRawId)) continue;
            if (LEGACY_DEFINITION_IDS.contains(normalizedRawId)) {
                config.getPlugin().getLogger().warning(
                        "Ignoring legacy enchant definition " + rawId + "; use the canonical alias definition.");
                continue;
            }
            if (!config.getEnchantsBoolean(path + ".enabled", true)) continue;
            WeaponType weapon = WeaponType.fromInput(config.getEnchantsString(path + ".weapon-type", "")).orElse(null);
            SkillInputType input = parseInput(config.getEnchantsString(path + ".input", ""));
            List<EnchantTriggerBinding> triggers = parseTriggers(path, input);
            String category = normalize(config.getEnchantsString(path + ".equipment-category", ""));
            if (triggers.isEmpty() || (weapon == null && category.isBlank()
                    && triggers.stream().anyMatch(binding -> binding.type() == TriggerType.INPUT))) {
                config.getPlugin().getLogger().warning("Invalid enchant definition: " + path);
                continue;
            }
            String id = normalize(rawId);
            SourceScope sourceScope = parseSourceScope(config.getEnchantsString(path + ".source-scope", "MAIN_HAND"), path);
            DuplicatePolicy duplicatePolicy = parseDuplicatePolicy(config.getEnchantsString(path + ".duplicate-policy", "HIGHEST_LEVEL"), path);
            double cooldown = Math.max(0.0D, config.getEnchantsDouble(path + ".cooldown-seconds", 0.0D));
            EnchantData data = new EnchantData(
                    id,
                    config.getEnchantsString(path + ".display-name", id),
                    normalize(config.getEnchantsString(path + ".book-item-id", "")),
                    normalize(config.getEnchantsString(path + ".executor-id", id)),
                    weapon,
                    input,
                    normalize(config.getEnchantsString(path + ".handler-id", "skill")),
                    sourceScope,
                    duplicatePolicy,
                    config.getEnchantsInt(path + ".priority", 0),
                    cooldown,
                    config.getEnchantsBoolean(path + ".cooldown-on-success-only", true),
                    config.getEnchantsBoolean(path + ".show-cooldown-message", true),
                    category,
                    config.getEnchantsStringList(path + ".material-patterns").stream()
                            .map(this::normalize).filter(value -> !value.isBlank()).toList(),
                    Math.max(1, config.getEnchantsInt(path + ".max-level", 1)),
                    normalize(config.getEnchantsString(path + ".input-conflict-group", "")),
                    triggers,
                    loreList(path, id, "description"),
                    loreList(path, id, "input-description"),
                    normalize(loreValue(path, id, "status", "ACTIVE"))
            );
            enchants.put(id, data);
            for (String rawAlias : config.getEnchantsStringList(path + ".legacy-aliases")) {
                String alias = normalize(rawAlias);
                if (!alias.isBlank() && !alias.equals(id)) legacyAliases.put(alias, id);
            }
        }
    }

    public Optional<EnchantData> get(String id) {
        String normalized = normalize(id);
        return Optional.ofNullable(enchants.get(legacyAliases.getOrDefault(normalized, normalized)));
    }

    public String canonicalId(String id) {
        return get(id).map(EnchantData::enchantId).orElse(normalize(id));
    }

    public List<EnchantData> getAll() {
        return enchants.values().stream().sorted(Comparator.comparing(EnchantData::enchantId)).toList();
    }

    public long countConfiguredRetiredEnchants() {
        return config.getEnchantsKeys("enchants").stream()
                .map(this::normalize)
                .filter(RETIRED_ENCHANTS::contains)
                .count();
    }

    public static boolean isRetiredId(String id) {
        return id != null && RETIRED_ENCHANTS.contains(id.trim().toLowerCase(Locale.ROOT));
    }

    public static String retiredReason(String id) {
        if (id == null) return "temporarily disabled";
        return RETIRED_REASONS.getOrDefault(id.trim().toLowerCase(Locale.ROOT), "temporarily disabled");
    }

    public static boolean isRetiredBookItemId(String itemId) {
        if (itemId == null) return false;
        String normalized = itemId.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("enchant_book_")
                && isRetiredId(normalized.substring("enchant_book_".length()));
    }

    /** Returns structural problems without silently dropping broken definitions. */
    public List<String> validateIntegrity(Set<String> registeredHandlers, Set<String> itemIds) {
        List<String> problems = new ArrayList<>();
        Set<String> books = new HashSet<>();
        for (EnchantData enchant : getAll()) {
            String path = "enchants." + enchant.enchantId();
            if (!enchant.bookItemId().isBlank() && !itemIds.contains(enchant.bookItemId())) {
                problems.add(path + ".book-item-id: missing item " + enchant.bookItemId());
            }
            String handler = normalize(enchant.handlerId());
            if (!registeredHandlers.contains(handler) && !"skill".equals(handler)) {
                problems.add(path + ".handler-id: unregistered handler " + handler);
            }
            if (!enchant.bookItemId().isBlank() && !books.add(enchant.bookItemId())) {
                problems.add(path + ".book-item-id: duplicate book " + enchant.bookItemId());
            }
            for (String alias : config.getEnchantsStringList(path + ".legacy-aliases")) {
                String normalizedAlias = normalize(alias);
                if (enchants.containsKey(normalizedAlias)) {
                    problems.add(path + ".legacy-aliases: alias is also a canonical definition " + normalizedAlias);
                }
            }
        }
        return List.copyOf(problems);
    }

    private SkillInputType parseInput(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return SkillInputType.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private List<EnchantTriggerBinding> parseTriggers(String path, SkillInputType legacyInput) {
        List<EnchantTriggerBinding> bindings = new ArrayList<>();
        var section = config.getEnchantsSection(path);
        if (section != null) {
            for (Map<?, ?> raw : section.getMapList("triggers")) {
                Object rawType = raw.get("type");
                if (rawType == null) continue;
                try {
                    TriggerType type = TriggerType.parse(String.valueOf(rawType));
                    Object rawPhase = raw.containsKey("phase") ? raw.get("phase") : "CONFIRMED";
                    TriggerPhase phase = TriggerPhase.parse(String.valueOf(rawPhase));
                    Object rawInput = raw.containsKey("input") ? raw.get("input")
                            : (legacyInput == null ? "" : legacyInput.name());
                    SkillInputType input = type == TriggerType.INPUT
                            ? parseInput(String.valueOf(rawInput))
                            : null;
                    if (type != TriggerType.INPUT || input != null) bindings.add(new EnchantTriggerBinding(type, input, phase));
                } catch (IllegalArgumentException exception) {
                    config.getPlugin().getLogger().warning("Invalid enchant trigger: " + path + ".triggers");
                }
            }
        }
        if (bindings.isEmpty() && legacyInput != null) {
            bindings.add(new EnchantTriggerBinding(TriggerType.INPUT, legacyInput, TriggerPhase.CONFIRMED));
        }
        return List.copyOf(bindings);
    }

    private SourceScope parseSourceScope(String raw, String path) {
        try { return SourceScope.parse(raw); }
        catch (IllegalArgumentException exception) {
            config.getPlugin().getLogger().warning("Invalid source-scope at " + path);
            return SourceScope.MAIN_HAND;
        }
    }

    private DuplicatePolicy parseDuplicatePolicy(String raw, String path) {
        try { return DuplicatePolicy.parse(raw); }
        catch (IllegalArgumentException exception) {
            config.getPlugin().getLogger().warning("Invalid duplicate-policy at " + path);
            return DuplicatePolicy.HIGHEST_LEVEL;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> loreList(String enchantPath, String enchantId, String key) {
        List<String> inline = config.getEnchantsStringList(enchantPath + ".lore." + key);
        return inline.isEmpty() ? config.getEnchantsStringList("enchant-lore." + enchantId + "." + key) : inline;
    }

    private String loreValue(String enchantPath, String enchantId, String key, String fallback) {
        String inline = config.getEnchantsString(enchantPath + ".lore." + key, "");
        return inline.isBlank() ? config.getEnchantsString("enchant-lore." + enchantId + "." + key, fallback) : inline;
    }
}
