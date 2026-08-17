package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.equipment.EquipmentTierService;
import com.hyunseo.hyunseorpg.equipment.EquipmentGrowthPolicy;
import com.hyunseo.hyunseorpg.equipment.EquipmentLoreBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/** Handles grade/star progression and persists aggregated option state on equipment. */
public final class EquipmentPromotionService {
    private static final Set<String> LEGACY_NON_PROMOTION_OPTIONS = Set.of(
            "piercing-chance", "knockback-resistance", "blade-throw-fire", "skill-damage-bonus-percent",
            "fire-damage-bonus-percent", "cooldown-reduction-percent", "burn-duration-bonus-ticks",
            "radius-bonus", "damage-multiplier", "weapon-damage");
    private static final String PROMOTION_LORE_PREFIX = "승급: ";
    private static final String OPTION_LORE_PREFIX = "옵션: ";
    private static final String ENCHANT_SLOT_LORE_PREFIX = "인챈트 슬롯: ";
    private final ConfigService config;
    private final JavaPlugin plugin;
    private final RPGItemService itemService;
    private final EquipmentEnhancementService enhancementService;
    private final EquipmentTierService tierService;
    private final NamespacedKey stageKey;
    private final NamespacedKey gradeKey;
    private final NamespacedKey starKey;
    private final NamespacedKey legacyOptionsKey;
    private final NamespacedKey generalOptionsKey;
    private final NamespacedKey specialOptionsKey;
    private final NamespacedKey enchantSlotsKey;
    private EquipmentGrowthPolicy growthPolicy;

    public EquipmentPromotionService(JavaPlugin plugin, ConfigService config, RPGItemService itemService,
                                     EquipmentEnhancementService enhancementService, EquipmentTierService tierService) {
        this.config = config;
        this.plugin = plugin;
        this.itemService = itemService;
        this.enhancementService = enhancementService;
        this.tierService = tierService;
        this.stageKey = new NamespacedKey(plugin, "promotion_stage");
        this.gradeKey = new NamespacedKey(plugin, "promotion_grade");
        this.starKey = new NamespacedKey(plugin, "promotion_star");
        this.legacyOptionsKey = new NamespacedKey(plugin, "promotion_options");
        this.generalOptionsKey = new NamespacedKey(plugin, "general_option_values");
        this.specialOptionsKey = new NamespacedKey(plugin, "special_option_data");
        this.enchantSlotsKey = new NamespacedKey(plugin, "unlocked_enchant_slots");
    }

    public void setGrowthPolicy(EquipmentGrowthPolicy growthPolicy) {
        this.growthPolicy = growthPolicy;
    }

    public Optional<PromotionPreview> preview(ItemStack item) {
        if (growthPolicy != null && !growthPolicy.canPromote(item)) return Optional.empty();
        if (tierService.getMaxPromotionStage(item) <= 0) return Optional.empty();
        String profile = resolvePromotionProfile(item, enhancementService.getProfileId(item).orElse(""));
        String maxStage = config.getEquipmentGrowthString("promotion.profiles." + profile + ".max-stage", "");
        if (maxStage.isBlank()) return Optional.empty();

        boolean special = isSpecialEquipment(item);
        if (special) {
            String configuredMax = specialValue(item, "promotion.max-stage", "");
            if (!configuredMax.isBlank()) maxStage = configuredMax;
        }

        String current = getStage(item);
        int requiredEnhancement = getPromotionEnhancementRequirement(item);
        if (!special && enhancementService.getLevel(item) < requiredEnhancement) return Optional.empty();
        int currentOrder = orderOf(current);
        int tierMaxStage = tierService.getMaxPromotionStage(item);
        String finalMaxStage = special && allStageIds().contains(maxStage)
                ? maxStage
                : allStageIds().stream()
                        .filter(stage -> orderOf(stage) <= tierMaxStage * 100 + 99)
                        .max(Comparator.comparingInt(this::orderOf))
                        .orElse(maxStage);
        String next = allStageIds().stream()
                .filter(stage -> orderOf(stage) > currentOrder && orderOf(stage) <= orderOf(finalMaxStage))
                .min(Comparator.comparingInt(this::orderOf)).orElse(null);
        if (next == null) return Optional.empty();
        if (!specialSoulRequirementsMet(item, next)) return Optional.empty();

        String path = stagePath(next);
        return Optional.of(new PromotionPreview(
                next,
                getString(path + ".display-name", next),
                specialStoneCost(item, path),
                getInt(path + ".general-option-count", getInt(path + ".normal-option-count", 0)),
                getInt(path + ".special-option-count", 0),
                getConfiguredEnchantSlots(next, getInt(path + ".unlock-enchant-slots", getInt(path + ".total-enchant-slots", 0))),
                profile,
                special,
                special ? specialValue(item, "promotion.success-chance", 1.0D) : 1.0D,
                requiredEnhancement
        ));
    }

    public boolean isPromotionStone(ItemStack item) {
        String id = config.getEquipmentGrowthString("promotion.required-stone-item-id", "basic_promotion_stone");
        return itemService.isItem(item, id);
    }

    public String getStage(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        ItemMeta meta = item.getItemMeta();
        String stage = meta.getPersistentDataContainer().get(stageKey, PersistentDataType.STRING);
        if (stage != null && allStageIds().contains(stage)) return stage;
        String grade = meta.getPersistentDataContainer().get(gradeKey, PersistentDataType.STRING);
        Integer star = meta.getPersistentDataContainer().get(starKey, PersistentDataType.INTEGER);
        if (grade != null && star != null && allStageIds().contains(grade + "-" + star)) return grade + "-" + star;
        String migrated = migrateLegacyStage(stage);
        return allStageIds().contains(migrated) ? migrated : "";
    }

    public int getEnhancementMaxLevel(ItemStack item) {
        int global = Math.max(1, config.getEquipmentGrowthInt("enhancement.max-level", 50));
        return Math.max(0, Math.min(global, tierService.getMaxEnhancement(item)));
    }

    /**
     * Returns the required enhancement level before the next promotion can be
     * attempted. Older configurations used max-enhancement for this purpose,
     * so it remains a backwards-compatible fallback rather than a growth cap.
     */
    public int getPromotionEnhancementRequirement(ItemStack item) {
        String stage = getStage(item);
        String grade = stage.isBlank() ? "normal" : gradeOf(stage);
        return getPromotionEnhancementRequirementForGrade(grade);
    }

    public int getPromotionEnhancementRequirementForStage(String stage) {
        return getPromotionEnhancementRequirementForGrade(gradeOf(stage));
    }

    private int getPromotionEnhancementRequirementForGrade(String grade) {
        int legacyFallback = getGradeMaxEnhancement(grade, 10);
        int configured = config.getEquipmentGrowthInt(
                "promotion.grades." + grade + ".required-enhancement", legacyFallback);
        int global = Math.max(1, config.getEquipmentGrowthInt("enhancement.max-level", 50));
        return Math.max(0, Math.min(global, configured));
    }

    /** @deprecated Promotion requirements are no longer enhancement caps. */
    @Deprecated(forRemoval = false)
    public int getEnhancementMaxLevelForStage(String stage) {
        String grade = gradeOf(stage);
        return getPromotionEnhancementRequirementForGrade(grade);
    }

    public int getUnlockedEnchantSlots(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return Math.max(0, item.getItemMeta().getPersistentDataContainer().getOrDefault(enchantSlotsKey, PersistentDataType.INTEGER, 0));
    }

    public Map<String, Double> getGeneralOptions(ItemStack item) {
        return readOptions(item, generalOptionsKey);
    }

    public Map<String, Double> getSpecialOptions(ItemStack item) {
        return readOptions(item, specialOptionsKey);
    }

    public Map<String, Double> getOptions(ItemStack item) {
        Map<String, Double> result = new LinkedHashMap<>(getGeneralOptions(item));
        getSpecialOptions(item).forEach((id, value) -> result.merge(id, value, Double::sum));
        if (!result.isEmpty()) return Map.copyOf(result);
        return readOptions(item, legacyOptionsKey);
    }

    public double getWeaponDamageBonus(ItemStack item) {
        return getOptions(item).getOrDefault("attack-damage", 0.0D);
    }

    public double getSkillDamageMultiplier(ItemStack item) {
        Map<String, Double> values = getOptions(item);
        return 1.0D + values.getOrDefault("skill-damage", values.getOrDefault("weapon-skill-damage", 0.0D));
    }

    public double getDamageReduction(ItemStack item) {
        return getOptions(item).getOrDefault("damage-reduction", 0.0D);
    }

    public double getOptionValue(ItemStack item, String optionId) {
        if (optionId == null || optionId.isBlank()) return 0.0D;
        String canonical = normalizeOptionId(optionId);
        if (!isPromotionEligible(canonical)) return 0.0D;
        return getOptions(item).getOrDefault(canonical, 0.0D);
    }

    /**
     * Reads an option retained only for compatibility with an older
     * feature-specific system. These values are never eligible for promotion
     * rolls or promotion rerolls.
     */
    public double getLegacyOptionValue(ItemStack item, String optionId) {
        if (optionId == null || optionId.isBlank()) return 0.0D;
        String canonical = normalizeOptionId(optionId);
        if (isPromotionEligible(canonical)) return 0.0D;
        return getOptions(item).getOrDefault(canonical, 0.0D);
    }

    public List<String> getOptionIds(ItemStack item) {
        return getOptions(item).keySet().stream()
                .filter(this::isPromotionEligible)
                .sorted().toList();
    }

    public String getOptionDisplayName(String optionId) {
        return getOptionDefinition(optionId).map(OptionDefinition::displayName)
                .orElse(optionId == null ? "" : optionId);
    }

    public String getOptionUnit(String optionId) {
        return getOptionDefinition(optionId).map(OptionDefinition::unit).orElse("FLAT");
    }

    public OptionBounds getOptionBounds(String optionId) {
        return getOptionDefinition(optionId)
                .map(definition -> new OptionBounds(definition.minimum(), definition.maximum(), definition.step()))
                .orElse(new OptionBounds(0.0D, 0.0D, 0.001D));
    }

    /** The single definition used by both first promotion rolls and rerolls. */
    public Optional<OptionDefinition> getOptionDefinition(String optionId) {
        String canonical = normalizeOptionId(optionId);
        if (canonical.isBlank() || !config.getEquipmentGrowthKeys("promotion.option-definitions").stream()
                .anyMatch(id -> id.equalsIgnoreCase(canonical))) return Optional.empty();
        String path = "promotion.option-definitions." + canonical;
        double configuredMin = finite(config.getEquipmentGrowthDouble(path + ".value.min", 0.0D), 0.0D);
        double configuredMax = finite(config.getEquipmentGrowthDouble(path + ".value.max", configuredMin), configuredMin);
        double min = Math.max(0.0D, Math.min(configuredMin, configuredMax));
        double max = Math.max(min, configuredMax);
        double step = Math.max(0.0001D, finite(config.getEquipmentGrowthDouble(path + ".value.precision", 0.001D), 0.001D));
        String display = config.getEquipmentGrowthString(path + ".display-name", canonical);
        String unit = config.getEquipmentGrowthString(path + ".unit", "FLAT");
        boolean eligible = config.getEquipmentGrowthBoolean(path + ".promotion-eligible",
                !LEGACY_NON_PROMOTION_OPTIONS.contains(canonical));
        return Optional.of(new OptionDefinition(canonical, display.isBlank() ? canonical : display, unit,
                min, max, step,
                Math.max(1, config.getEquipmentGrowthInt(path + ".weight", 1)),
                config.getEquipmentGrowthBoolean(path + ".allow-duplicate", false), eligible,
                config.getEquipmentGrowthBoolean(path + ".legacy-only", !eligible)));
    }

    public boolean isPromotionEligible(String optionId) {
        return getOptionDefinition(optionId).map(OptionDefinition::promotionEligible).orElse(false);
    }

    /** Returns a weighted, step-aligned value on the shared range, never below current. */
    public OptionalDouble rerollOptionValue(String optionId, double currentValue) {
        OptionDefinition definition = getOptionDefinition(optionId).orElse(null);
        if (definition == null || !definition.promotionEligible()) return OptionalDouble.empty();
        double lower = Math.max(definition.minimum(), currentValue);
        if (lower >= definition.maximum() - 0.0000001D) return OptionalDouble.empty();
        return OptionRollService.roll(definition.minimum(), definition.maximum(), definition.step(), lower,
                optionRollSettings(optionId), ThreadLocalRandom.current());
    }

    /** Admin-only simulation hook; it never changes an item or consumes resources. */
    public OptionalDouble simulateFirstOptionRoll(String optionId, RandomGenerator random) {
        OptionDefinition definition = getOptionDefinition(optionId).orElse(null);
        if (definition == null || !definition.promotionEligible()) return OptionalDouble.empty();
        return OptionRollService.roll(definition.minimum(), definition.maximum(), definition.step(),
                definition.minimum(), optionRollSettings(optionId), random);
    }

    /** Replaces only one aggregate promotion option and rebuilds the shared lore. */
    public boolean replaceOption(ItemStack item, String optionId, double value) {
        if (item == null || !item.hasItemMeta() || optionId == null || optionId.isBlank()
                || !Double.isFinite(value) || value < 0.0D) return false;
        String canonical = normalizeOptionId(optionId);
        OptionDefinition definition = getOptionDefinition(canonical).orElse(null);
        if (definition == null || !definition.promotionEligible()
                || value < definition.minimum() - 0.0000001D
                || value > definition.maximum() + 0.0000001D
                || !isStepValue(value, definition)) return false;
        Map<String, Double> general = new LinkedHashMap<>(getGeneralOptions(item));
        Map<String, Double> special = new LinkedHashMap<>(getSpecialOptions(item));
        boolean inGeneral = general.containsKey(canonical);
        boolean inSpecial = special.containsKey(canonical);
        if (!inGeneral && !inSpecial) return false;
        general.remove(canonical);
        special.remove(canonical);
        if (inGeneral) general.put(canonical, value);
        else special.put(canonical, value);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        meta.getPersistentDataContainer().set(generalOptionsKey, PersistentDataType.STRING, encode(general));
        meta.getPersistentDataContainer().set(specialOptionsKey, PersistentDataType.STRING, encode(special));
        meta.getPersistentDataContainer().set(legacyOptionsKey, PersistentDataType.STRING, encode(combine(general, special)));
        refreshOptionsLore(meta, combine(general, special));
        item.setItemMeta(meta);
        return true;
    }

    private void refreshOptionsLore(ItemMeta meta, Map<String, Double> options) {
        EquipmentLoreBuilder lore = EquipmentLoreBuilder.from(meta).removePlainPrefix(OPTION_LORE_PREFIX);
        options.forEach((id, optionValue) -> {
            if (!isPromotionEligible(id)) return;
            lore.add(Component.text(OPTION_LORE_PREFIX + optionDisplay(id) + " +"
                    + formatOptionValue(id, optionValue), NamedTextColor.GRAY));
        });
        meta.lore(lore.build());
    }

    public void apply(ItemStack item, PromotionPreview preview) {
        if (growthPolicy != null && !growthPolicy.canPromote(item)) return;
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return;
        if (!specialSoulRequirementsMet(item, preview.stageId())) return;

        Map<String, Double> general = new LinkedHashMap<>(getGeneralOptions(item));
        Map<String, Double> special = new LinkedHashMap<>(getSpecialOptions(item));
        if (general.isEmpty() && special.isEmpty()) {
            Map<String, Double> legacy = readOptions(item, legacyOptionsKey);
            general.putAll(legacy);
        }
        if (tierService.getCategory(item) == EquipmentTierService.Category.TOOL) {
            // Remove legacy combat options before applying the resolved gathering profile.
            Set<String> allowed = new HashSet<>();
            allowed.addAll(config.getEquipmentGrowthStringList("promotion.profiles." + preview.profileId() + ".general-option-pool"));
            allowed.addAll(config.getEquipmentGrowthStringList("promotion.profiles." + preview.profileId() + ".special-option-pool"));
            allowed = allowed.stream()
                    .map(value -> value.toLowerCase(java.util.Locale.ROOT))
                    .collect(java.util.stream.Collectors.toSet());
            Set<String> allowedOptions = allowed;
            general.keySet().removeIf(id -> !allowedOptions.contains(id.toLowerCase(java.util.Locale.ROOT)));
            special.keySet().removeIf(id -> !allowedOptions.contains(id.toLowerCase(java.util.Locale.ROOT)));
        }
        if (tierService.getCategory(item) == EquipmentTierService.Category.TOOL) {
            meta.getPersistentDataContainer().remove(legacyOptionsKey);
        }
        Map<String, Double> totalOptions = combine(general, special);
        select(item, preview.profileId(), preview.stageId(), "general", preview.normalCount(), general,
                totalOptions, false);
        select(item, preview.profileId(), preview.stageId(), "special", preview.specialCount(), special,
                totalOptions, true);

        String grade = gradeOf(preview.stageId());
        int star = starOf(preview.stageId());
        meta.getPersistentDataContainer().set(stageKey, PersistentDataType.STRING, preview.stageId());
        meta.getPersistentDataContainer().set(gradeKey, PersistentDataType.STRING, grade);
        meta.getPersistentDataContainer().set(starKey, PersistentDataType.INTEGER, star);
        meta.getPersistentDataContainer().set(generalOptionsKey, PersistentDataType.STRING, encode(general));
        meta.getPersistentDataContainer().set(specialOptionsKey, PersistentDataType.STRING, encode(special));
        meta.getPersistentDataContainer().set(legacyOptionsKey, PersistentDataType.STRING, encode(combine(general, special)));
        int unlocked = Math.max(getUnlockedEnchantSlots(item), preview.enchantSlots());
        meta.getPersistentDataContainer().set(enchantSlotsKey, PersistentDataType.INTEGER, unlocked);
        consumeSpecialSouls(meta, item, preview.stageId());

        EquipmentLoreBuilder lore = EquipmentLoreBuilder.from(meta)
                .removePlainPrefix(PROMOTION_LORE_PREFIX)
                .removePlainPrefix(OPTION_LORE_PREFIX)
                .removePlainPrefix(ENCHANT_SLOT_LORE_PREFIX)
                .add(Component.text(PROMOTION_LORE_PREFIX + preview.displayName(), NamedTextColor.LIGHT_PURPLE));
        combine(general, special).forEach((id, value) -> {
            if (!isPromotionEligible(id)) return;
            lore.add(Component.text(OPTION_LORE_PREFIX + optionDisplay(id) + " +"
                    + formatOptionValue(id, value), NamedTextColor.GRAY));
        });
        meta.lore(lore.add(Component.text(ENCHANT_SLOT_LORE_PREFIX + unlocked, NamedTextColor.AQUA)).build());
        item.setItemMeta(meta);
    }

    public double promotionProgress(ItemStack item) {
        List<String> ordered = allStageIds().stream().sorted(Comparator.comparingInt(this::orderOf)).toList();
        if (ordered.isEmpty()) return 0.0D;
        String current = getStage(item);
        int currentIndex = ordered.indexOf(current);
        return currentIndex < 0 ? 0.0D : (double) currentIndex / Math.max(1, ordered.size() - 1);
    }

    public boolean isSpecialEquipment(ItemStack item) {
        String itemId = itemService.getItemId(item).orElse("");
        return !itemId.isBlank() && config.getSpecialEquipmentSection("special-equipment.items") != null
                && config.getSpecialEquipmentSection("special-equipment.items").getKeys(false).stream().anyMatch(rawId -> {
                    ConfigurationSection section = config.getSpecialEquipmentSection("special-equipment.items." + rawId);
                    return section != null && itemId.equalsIgnoreCase(section.getString("item-id", rawId));
                });
    }

    public double getSuccessChance(ItemStack item, PromotionPreview preview) {
        return Math.max(0.0D, Math.min(1.0D, preview.successChance()));
    }

    private void select(ItemStack item, String profile, String stage, String category, int count,
                         Map<String, Double> result, Map<String, Double> totalOptions, boolean weighted) {
        String poolKey = category.equals("general") ? "general-option-pool" : "special-option-pool";
        List<String> pool = isSpecialEquipment(item)
                ? new ArrayList<>(specialList(item, "promotion.options." + poolKey))
                : new ArrayList<>(config.getEquipmentGrowthStringList("promotion.profiles." + profile + "." + poolKey));
        if (pool.isEmpty()) pool = new ArrayList<>(config.getEquipmentGrowthStringList(stagePath(stage) + "." + poolKey));
        pool = pool.stream()
                .map(this::normalizeOptionId)
                .filter(this::isPromotionEligible)
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        // A non-duplicate option already present on the item must not be rolled again.
        pool.removeIf(id -> !allowDuplicate(id) && totalOptions.containsKey(id));
        for (int i = 0; i < count && !pool.isEmpty();) {
            String id = weighted ? weightedPick(pool) : pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            OptionalDouble rolled = rollValue(id, totalOptions.getOrDefault(id, 0.0D));
            if (rolled.isEmpty()) {
                // No valid precision step remains below the aggregate cap.
                pool.remove(id);
                continue;
            }
            result.merge(id, rolled.getAsDouble(), Double::sum);
            totalOptions.merge(id, rolled.getAsDouble(), Double::sum);
            i++;
            if (!allowDuplicate(id)) pool.remove(id);
        }
    }

    /**
     * Promotion option pools are intentionally more specific than base
     * enhancement profiles. This keeps the axe in the gathering path while
     * allowing its own YAML-owned option pool.
     */
    private String resolvePromotionProfile(ItemStack item, String fallback) {
        return EquipmentGrowthProfileResolver.resolvePromotionProfile(config, itemService, item, fallback);
    }

    private boolean allowDuplicate(String id) {
        return getOptionDefinition(id).map(OptionDefinition::allowDuplicate).orElse(false);
    }

    private OptionalDouble rollValue(String id, double currentTotal) {
        OptionDefinition definition = getOptionDefinition(id).orElse(null);
        if (definition == null || !definition.promotionEligible()) return OptionalDouble.empty();
        double remaining = definition.maximum() - Math.max(0.0D, currentTotal);
        if (remaining < definition.minimum() - 0.0000001D) return OptionalDouble.empty();
        // Promotion duplicates are increments. The available upper bound is
        // reduced by the aggregate already stored on the item.
        return OptionRollService.roll(definition.minimum(), remaining, definition.step(), definition.minimum(),
                optionRollSettings(id), ThreadLocalRandom.current());
    }

    private OptionRollService.Settings optionRollSettings(String optionId) {
        String path = "option-roll";
        String override = path + ".overrides." + normalizeOptionId(optionId);
        boolean enabled = config.getEquipmentGrowthBoolean(path + ".enabled", true);
        String distribution = config.getEquipmentGrowthString(path + ".distribution", "weighted-halves");
        String withinRange = config.getEquipmentGrowthString(path + ".within-range-distribution", "uniform");
        double lowerWeight = config.getEquipmentGrowthDouble(override + ".lower-half-weight",
                config.getEquipmentGrowthDouble(path + ".lower-half-weight", 3.0D));
        double upperWeight = config.getEquipmentGrowthDouble(override + ".upper-half-weight",
                config.getEquipmentGrowthDouble(path + ".upper-half-weight", 1.0D));
        boolean preserveMaximum = config.getEquipmentGrowthBoolean(override + ".preserve-maximum-roll",
                config.getEquipmentGrowthBoolean(path + ".preserve-maximum-roll", true));
        return new OptionRollService.Settings(enabled, distribution, lowerWeight, upperWeight,
                withinRange, preserveMaximum);
    }

    private List<String> allStageIds() {
        List<String> ids = new ArrayList<>();
        for (String grade : config.getEquipmentGrowthKeys("promotion.grades")) {
            int stars = Math.max(0, config.getEquipmentGrowthInt("promotion.grades." + grade + ".stars", 0));
            for (int star = 1; star <= stars; star++) ids.add(grade + "-" + star);
        }
        ids.sort(Comparator.comparingInt(this::orderOf));
        return ids;
    }

    private boolean isNewStage(String stage) {
        return allStageIds().contains(stage);
    }

    private String stagePath(String stage) {
        if (!isNewStage(stage)) return "promotions.stages." + stage;
        return "promotion.grades." + gradeOf(stage) + ".stars-data." + starOf(stage);
    }

    private int orderOf(String stage) {
        if (stage == null || stage.isBlank()) return 0;
        if (isNewStageWithoutRecursion(stage)) {
            return config.getEquipmentGrowthInt("promotion.grades." + gradeOf(stage) + ".order", 0) * 100 + starOf(stage);
        }
        return 0;
    }

    private boolean isNewStageWithoutRecursion(String stage) {
        int split = stage.lastIndexOf('-');
        if (split <= 0) return false;
        String grade = stage.substring(0, split);
        try {
            int star = Integer.parseInt(stage.substring(split + 1));
            return config.getEquipmentGrowthKeys("promotion.grades").contains(grade)
                    && star >= 1 && star <= config.getEquipmentGrowthInt("promotion.grades." + grade + ".stars", 0);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private String gradeOf(String stage) {
        int split = stage == null ? -1 : stage.lastIndexOf('-');
        return split <= 0 ? "normal" : stage.substring(0, split);
    }

    private int starOf(String stage) {
        try { return Integer.parseInt(stage.substring(stage.lastIndexOf('-') + 1)); } catch (Exception ignored) { return 0; }
    }

    private String migrateLegacyStage(String stage) {
        if (stage == null || stage.isBlank()) return "";
        if (!stage.startsWith("normal-")) return stage;
        try {
            int oldStar = Integer.parseInt(stage.substring("normal-".length()));
            if (oldStar > 5 && oldStar <= 10) return "advanced-" + (oldStar - 5);
        } catch (NumberFormatException ignored) { }
        return stage;
    }

    private int getGradeMaxEnhancement(String grade, int fallback) {
        return Math.max(0, config.getEquipmentGrowthInt("promotion.grades." + grade + ".max-enhancement", fallback));
    }

    private int getConfiguredEnchantSlots(String stage, int fallback) {
        int configured = config.getEquipmentGrowthInt("promotion.slot-unlocks." + stage, -1);
        return configured < 0 ? fallback : configured;
    }

    private String getString(String path, String fallback) {
        String value = config.getEquipmentGrowthString(path, "");
        return value.isBlank() ? fallback : value;
    }

    private int getInt(String path, int fallback) {
        return config.getEquipmentGrowthInt(path, fallback);
    }

    private Map<String, Double> readOptions(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return Map.of();
        String raw = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return Map.of();
        Map<String, Double> values = new LinkedHashMap<>();
        for (String part : raw.split(";")) {
            String[] split = part.split("=", 2);
            if (split.length != 2) continue;
            try {
                double value = Double.parseDouble(split[1]);
                String id = normalizeOptionId(split[0]);
                if (!id.isBlank() && Double.isFinite(value)) {
                    values.merge(id, Math.max(0.0D, value), Double::sum);
                }
            } catch (NumberFormatException ignored) { }
        }
        values.replaceAll((id, value) -> capStoredOptionValue(id, value));
        return Map.copyOf(values);
    }

    private double capStoredOptionValue(String id, double value) {
        OptionDefinition definition = getOptionDefinition(id).orElse(null);
        return capAggregateOptionValue(value, definition);
    }

    private Map<String, Double> combine(Map<String, Double> first, Map<String, Double> second) {
        Map<String, Double> result = new LinkedHashMap<>(first);
        second.forEach((id, value) -> result.merge(id, value, Double::sum));
        result.replaceAll(this::capCombinedOptionValue);
        return result;
    }

    private double capCombinedOptionValue(String id, double value) {
        OptionDefinition definition = getOptionDefinition(id).orElse(null);
        return capAggregateOptionValue(value, definition);
    }

    static double capAggregateOptionValue(double value, OptionDefinition definition) {
        if (!Double.isFinite(value) || definition == null || !definition.promotionEligible()) return value;
        return Math.min(definition.maximum(), Math.max(0.0D, value));
    }

    private String encode(Map<String, Double> values) {
        return values.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue()).reduce((a, b) -> a + ";" + b).orElse("");
    }

    private String optionDisplay(String id) {
        return getOptionDefinition(id).map(OptionDefinition::displayName).orElse(id);
    }

    private boolean specialSoulRequirementsMet(ItemStack item, String nextStage) {
        String itemId = itemService.getItemId(item).orElse("");
        if (itemId.isBlank()) return true;
        ConfigurationSection root = config.getSpecialEquipmentSection("special-equipment.items");
        if (root == null) return true;
        for (String rawId : root.getKeys(false)) {
            ConfigurationSection entry = root.getConfigurationSection(rawId);
            if (entry == null || !itemId.equalsIgnoreCase(entry.getString("item-id", ""))) continue;
            ConfigurationSection requirements = entry.getConfigurationSection("promotion.soul-requirements." + nextStage);
            if (requirements == null) return true;
            if (!item.hasItemMeta()) return false;
            for (String mobId : requirements.getKeys(false)) {
                long required = Math.max(0L, requirements.getLong(mobId, 0L));
                long current = item.getItemMeta().getPersistentDataContainer()
                        .getOrDefault(soulKey(mobId), PersistentDataType.LONG, 0L);
                if (current < required) return false;
            }
            return true;
        }
        return true;
    }

    private void consumeSpecialSouls(ItemMeta meta, ItemStack item, String stage) {
        String itemId = itemService.getItemId(item).orElse("");
        ConfigurationSection root = config.getSpecialEquipmentSection("special-equipment.items");
        if (itemId.isBlank() || root == null) return;
        for (String rawId : root.getKeys(false)) {
            ConfigurationSection entry = root.getConfigurationSection(rawId);
            if (entry == null || !itemId.equalsIgnoreCase(entry.getString("item-id", ""))) continue;
            ConfigurationSection requirements = entry.getConfigurationSection("promotion.soul-requirements." + stage);
            if (requirements == null) return;
            for (String mobId : requirements.getKeys(false)) {
                long required = Math.max(0L, requirements.getLong(mobId, 0L));
                long current = meta.getPersistentDataContainer().getOrDefault(soulKey(mobId), PersistentDataType.LONG, 0L);
                meta.getPersistentDataContainer().set(soulKey(mobId), PersistentDataType.LONG, Math.max(0L, current - required));
            }
            return;
        }
    }

    private NamespacedKey soulKey(String mobId) {
        String safe = mobId == null ? "unknown" : mobId.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return new NamespacedKey(plugin, "special_soul_" + safe);
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private int specialStoneCost(ItemStack item, String path) {
        int base = getInt(path + ".stones", 1);
        double multiplier = Math.max(1.0D, specialValue(item, "promotion.cost-multiplier", 1.0D));
        return Math.max(1, (int) Math.ceil(base * multiplier));
    }

    private double specialValue(ItemStack item, String suffix, double fallback) {
        String rawId = specialRawId(item);
        return rawId.isBlank() ? fallback
                : config.getSpecialEquipmentDouble("special-equipment.items." + rawId + "." + suffix, fallback);
    }

    private String specialValue(ItemStack item, String suffix, String fallback) {
        String rawId = specialRawId(item);
        return rawId.isBlank() ? fallback
                : config.getSpecialEquipmentString("special-equipment.items." + rawId + "." + suffix, fallback);
    }

    private List<String> specialList(ItemStack item, String suffix) {
        String rawId = specialRawId(item);
        return rawId.isBlank() ? List.of()
                : config.getSpecialEquipmentStringList("special-equipment.items." + rawId + "." + suffix);
    }

    private String specialRawId(ItemStack item) {
        String itemId = itemService.getItemId(item).orElse("");
        ConfigurationSection root = config.getSpecialEquipmentSection("special-equipment.items");
        if (root == null || itemId.isBlank()) return "";
        for (String rawId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(rawId);
            if (section != null && itemId.equalsIgnoreCase(section.getString("item-id", rawId))) return rawId;
        }
        return "";
    }

    private String formatOptionValue(String id, double value) {
        String unit = getOptionDefinition(id).map(OptionDefinition::unit).orElse("");
        if (unit.equalsIgnoreCase("PERCENT") || unit.equalsIgnoreCase("PERCENT_POINT")) {
            return String.format(java.util.Locale.ROOT, "%.0f%%", value * 100.0D);
        }
        return format(value);
    }

    private String weightedPick(List<String> pool) {
        int total = pool.stream().mapToInt(id -> getOptionDefinition(id).map(OptionDefinition::weight).orElse(1)).sum();
        int pick = ThreadLocalRandom.current().nextInt(Math.max(1, total));
        for (String id : pool) {
            pick -= getOptionDefinition(id).map(OptionDefinition::weight).orElse(1);
            if (pick < 0) return id;
        }
        return pool.get(0);
    }

    private String normalizeOptionId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private double finite(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private boolean isStepValue(double value, OptionDefinition definition) {
        double steps = value / definition.step();
        return Math.abs(steps - Math.rint(steps)) <= 0.000001D;
    }

    public record OptionDefinition(String id, String displayName, String unit,
                                   double minimum, double maximum, double step,
                                   int weight, boolean allowDuplicate,
                                   boolean promotionEligible, boolean legacyOnly) { }

    public record OptionBounds(double minimum, double maximum, double precision) { }

    public record PromotionPreview(String stageId, String displayName, int stoneCost, int normalCount,
                                   int specialCount, int enchantSlots, String profileId,
                                   boolean specialEquipment, double successChance,
                                   int requiredEnhancement) { }
}
