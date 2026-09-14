package com.hyunseo.hyunseorpg.enchant;

import com.hyunseo.hyunseorpg.enhancement.EquipmentPromotionService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.equipment.EquipmentGrowthPolicy;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import com.hyunseo.hyunseorpg.equipment.EquipmentLoreBuilder;
import com.hyunseo.hyunseorpg.equipment.EquipmentTierService;
import com.hyunseo.hyunseorpg.equipment.trigger.EnchantTriggerBinding;
import com.hyunseo.hyunseorpg.equipment.trigger.TriggerType;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.skill.SkillInputType;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentData;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentRegistry;
import com.hyunseo.hyunseorpg.weapon.WeaponService;
import com.hyunseo.hyunseorpg.weapon.WeaponType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

/** Persists equipped custom enchant IDs, levels and deterministic input conflicts. */
public final class EnchantService {
    private static final String NATIVE_NAMESPACE = "hyunseorpg";
    private final EnchantRegistry registry;
    private final ConfigService config;
    private final RPGItemService itemService;
    private final EquipmentPromotionService promotionService;
    private final WeaponService weaponService;
    private final EquipmentTierService tierService;
    private final SpecialEquipmentRegistry specialEquipment;
    private final EquipmentInstanceService equipmentInstances;
    private final NamespacedKey equippedKey;
    private final NamespacedKey generatedLoreKey;
    private final NamespacedKey bookLevelKey;
    private final NamespacedKey bookDataVersionKey;
    private final NamespacedKey bookInputKey;
    private final NamespacedKey bookCategoryKey;
    private EquipmentGrowthPolicy growthPolicy;

    public EnchantService(JavaPlugin plugin, ConfigService config, EnchantRegistry registry, RPGItemService itemService,
                          EquipmentPromotionService promotionService, WeaponService weaponService,
                          EquipmentTierService tierService, SpecialEquipmentRegistry specialEquipment,
                          EquipmentInstanceService equipmentInstances) {
        this.registry = registry;
        this.config = config;
        this.itemService = itemService;
        this.promotionService = promotionService;
        this.weaponService = weaponService;
        this.tierService = tierService;
        this.specialEquipment = specialEquipment;
        this.equipmentInstances = equipmentInstances;
        this.equippedKey = new NamespacedKey(plugin, "equipped_enchants");
        this.generatedLoreKey = new NamespacedKey(plugin, "generated_enchant_lore");
        this.bookLevelKey = new NamespacedKey(plugin, "enchant_book_level");
        this.bookDataVersionKey = new NamespacedKey(plugin, "enchant_book_data_version");
        this.bookInputKey = new NamespacedKey(plugin, "enchant_book_input");
        this.bookCategoryKey = new NamespacedKey(plugin, "enchant_book_category");
    }

    public void setGrowthPolicy(EquipmentGrowthPolicy growthPolicy) {
        this.growthPolicy = growthPolicy;
    }

    public Optional<EnchantData> findForInput(ItemStack equipment, SkillInputType inputType) {
        if (equipment == null || equipment.getType().isAir()) return Optional.empty();
        WeaponType weapon = weaponService.getWeaponType(equipment).orElse(null);
        for (String id : getActiveEquipped(equipment)) {
            EnchantData data = registry.get(id).orElse(null);
            if (data != null && matchesEquipment(data, equipment, weapon) && hasInput(data, inputType)) {
                return Optional.of(data);
            }
        }
        return Optional.empty();
    }

    public Optional<EnchantData> getEnchantFromBook(ItemStack book) {
        if (book != null && book.hasItemMeta()) {
            Map<Enchantment, Integer> stored = allNativeEnchantments(book);
            for (Enchantment enchantment : stored.keySet()) {
                if (NATIVE_NAMESPACE.equals(enchantment.getKey().getNamespace())) {
                    Optional<EnchantData> resolved = registry.get(enchantment.getKey().getKey());
                    if (resolved.isPresent()) return resolved;
                }
            }
        }
        // Temporary compatibility path for old shop/crafting books.
        return registry.getAll().stream().filter(data -> itemService.isItem(book, data.bookItemId())).findFirst();
    }

    public List<AppliedEnchant> getAppliedEnchants(ItemStack equipment) {
        return getEquippedEntries(equipment).stream()
                .map(entry -> new AppliedEnchant(entry.id(), entry.level()))
                .toList();
    }

    public int getBookLevel(ItemStack book) {
        if (book == null || !book.hasItemMeta()) return 1;
        Optional<EnchantData> data = getEnchantFromBook(book);
        if (data.isPresent()) {
            Enchantment enchantment = nativeEnchant(data.get().enchantId());
            Integer level = enchantment == null ? null : allNativeEnchantments(book).get(enchantment);
            if (level != null) return Math.max(1, level);
        }
        return Math.max(1, book.getItemMeta().getPersistentDataContainer()
                .getOrDefault(bookLevelKey, PersistentDataType.INTEGER, 1));
    }

    /** Creates an extraction result while preserving the current enchant definition metadata. */
    public Optional<ItemStack> createBook(String enchantId, int level) {
        EnchantData data = registry.get(enchantId).orElse(null);
        if (data == null || data.bookItemId().isBlank()) return Optional.empty();
        ItemStack book = itemService.create(data.bookItemId(), 1).orElse(null);
        if (book == null) return Optional.empty();
        ItemMeta meta = book.getItemMeta();
        if (meta == null) return Optional.empty();
        int safeLevel = Math.max(1, Math.min(data.maxLevel(), level));
        var pdc = meta.getPersistentDataContainer();
        pdc.set(bookLevelKey, PersistentDataType.INTEGER, safeLevel);
        pdc.set(bookDataVersionKey, PersistentDataType.INTEGER, 1);
        pdc.set(bookInputKey, PersistentDataType.STRING, data.triggers().stream()
                .filter(binding -> binding.type() == TriggerType.INPUT && binding.input() != null)
                .map(binding -> binding.input().name()).findFirst().orElse("PASSIVE"));
        pdc.set(bookCategoryKey, PersistentDataType.STRING, data.equipmentCategory());
        Enchantment nativeEnchant = nativeEnchant(data.enchantId());
        if (nativeEnchant == null || !(meta instanceof EnchantmentStorageMeta storage)) return Optional.empty();
        storage.addStoredEnchant(nativeEnchant, safeLevel, true);
        book.setItemMeta(meta);
        refreshBookLore(book);
        return Optional.of(book);
    }

    /** Removes exactly the selected enchant and refreshes only generated enchant lore. */
    public boolean removeEnchant(ItemStack equipment, String enchantId) {
        String canonical = registry.canonicalId(enchantId);
        List<EquippedEnchant> equipped = new ArrayList<>(getEquippedEntries(equipment));
        boolean removed = equipped.removeIf(entry -> entry.id().equals(canonical));
        return removed && persistEquipped(equipment, equipped);
    }

    public boolean hasEquippedExecutor(ItemStack equipment, String executorId) {
        if (executorId == null || executorId.isBlank()) return false;
        return getEquipped(equipment).stream()
                .map(registry::get)
                .flatMap(Optional::stream)
                .anyMatch(data -> data.executorId().equalsIgnoreCase(executorId));
    }

    public List<String> getEquipped(ItemStack equipment) {
        return getEquippedEntries(equipment).stream().map(EquippedEnchant::id).toList();
    }

    public List<String> getActiveEquipped(ItemStack equipment) {
        // Native enchantments are governed by their registry definition and vanilla application rules.
        // Legacy promotion slots remain for growth UI compatibility but no longer suppress runtime effects.
        return getEquipped(equipment);
    }

    public int getEnchantLevel(ItemStack equipment, String enchantId) {
        String canonical = registry.canonicalId(enchantId);
        migrateLegacyEnchantments(equipment);
        Enchantment enchantment = nativeEnchant(canonical);
        return enchantment == null || equipment == null ? 0 : equipment.getEnchantmentLevel(enchantment);
    }

    public boolean hasEquipped(ItemStack equipment, String enchantId) {
        return getEnchantLevel(equipment, enchantId) > 0;
    }

    public boolean hasActiveEquipped(ItemStack equipment, String enchantId) {
        String canonical = registry.canonicalId(enchantId);
        return getActiveEquipped(equipment).stream().anyMatch(canonical::equals);
    }

    public boolean hasInputBinding(ItemStack equipment) {
        return getActiveEquipped(equipment).stream()
                .map(registry::get)
                .flatMap(Optional::stream)
                .anyMatch(data -> data.triggers().stream().anyMatch(binding -> binding.type() == TriggerType.INPUT));
    }

    /** Uses active slot data to select and cancel the matching vanilla input. */
    public boolean hasActiveInputBinding(ItemStack equipment, SkillInputType inputType) {
        if (equipment == null || equipment.getType().isAir() || inputType == null) return false;
        WeaponType weapon = weaponService.getWeaponType(equipment).orElse(null);
        for (String id : getActiveEquipped(equipment)) {
            EnchantData data = registry.get(id).orElse(null);
            if (data != null && matchesEquipment(data, equipment, weapon) && hasInput(data, inputType)) return true;
        }
        return false;
    }

    public boolean equip(ItemStack equipment, ItemStack book) {
        Optional<EnchantData> data = getEnchantFromBook(book);
        if (data.isEmpty() || equipment == null || equipment.getType().isAir()) return false;
        if (growthPolicy != null && !growthPolicy.canEnchant(equipment)) return false;

        EnchantData enchant = data.get();
        WeaponType weapon = weaponService.getWeaponType(equipment).orElse(null);
        if (!matchesEquipment(enchant, equipment, weapon)) return false;

        List<EquippedEnchant> equipped = new ArrayList<>(getEquippedEntries(equipment));
        int bookLevel = Math.max(1, getBookLevel(book));
        for (int index = 0; index < equipped.size(); index++) {
            EquippedEnchant current = equipped.get(index);
            if (!current.id().equals(enchant.enchantId())) continue;
            int nextLevel = current.level() + bookLevel;
            if (nextLevel > enchant.maxLevel()) return false;
            equipped.set(index, new EquippedEnchant(current.id(), nextLevel));
            return persistEquipped(equipment, equipped);
        }

        // Native enchantments are not constrained by the legacy promotion-slot count. Input
        // conflicts remain a runtime concern until registry exclusivity sets replace them.
        if (hasInputConflict(enchant, equipped)) return false;

        equipped.add(new EquippedEnchant(enchant.enchantId(), Math.min(enchant.maxLevel(), bookLevel)));
        return persistEquipped(equipment, equipped);
    }

    private boolean persistEquipped(ItemStack equipment, List<EquippedEnchant> equipped) {
        equipmentInstances.ensure(equipment);
        ItemMeta meta = equipment.getItemMeta();
        if (meta == null) return false;
        for (Enchantment enchantment : new ArrayList<>(meta.getEnchants().keySet())) {
            if (NATIVE_NAMESPACE.equals(enchantment.getKey().getNamespace())) meta.removeEnchant(enchantment);
        }
        for (EquippedEnchant entry : equipped) {
            Enchantment enchantment = nativeEnchant(entry.id());
            if (enchantment != null) meta.addEnchant(enchantment, entry.level(), true);
        }
        meta.getPersistentDataContainer().remove(equippedKey);
        rebuildEquippedLore(meta, equipped);
        equipment.setItemMeta(meta);
        return true;
    }

    /** Rebuilds only the previously generated enchant section; all other item lore remains untouched. */
    public void refreshEquippedLore(ItemStack equipment) {
        if (equipment == null || !equipment.hasItemMeta()) return;
        ItemMeta meta = equipment.getItemMeta();
        if (meta == null) return;
        List<EquippedEnchant> equipped = getEquippedEntries(equipment);
        // Registry aliases are resolved while legacy PDC converges to native enchantments.
        rebuildEquippedLore(meta, equipped);
        equipment.setItemMeta(meta);
    }

    /** Called by item creation so books and equipped items share the same definition data. */
    public void refreshBookLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        EnchantData data = getEnchantFromBook(item).orElse(null);
        if (data == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.displayName(Component.text(data.displayName() + " 인챈트 북", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(EnchantLoreFormatter.bookLines(data));
        item.setItemMeta(meta);
    }

    private void rebuildEquippedLore(ItemMeta meta, List<EquippedEnchant> equipped) {
        Set<String> oldGenerated = decodeGenerated(meta.getPersistentDataContainer().get(generatedLoreKey, PersistentDataType.STRING));
        EquipmentLoreBuilder lore = EquipmentLoreBuilder.from(meta);
        lore.removeIf(line -> {
            String plain = PlainTextComponentSerializer.plainText().serialize(line);
            return oldGenerated.contains(plain) || plain.equals(EnchantLoreFormatter.SECTION_TITLE)
                    || plain.startsWith("인챈트: ") || plain.startsWith("Enchant: ")
                    || plain.startsWith("?몄콌?? ");
        });
        List<Component> generated = new ArrayList<>();
        if (!equipped.isEmpty()) {
            generated.add(Component.text(EnchantLoreFormatter.SECTION_TITLE, NamedTextColor.DARK_AQUA));
            equipped.stream().sorted(java.util.Comparator.comparing(EquippedEnchant::id)).forEach(entry ->
                    registry.get(entry.id()).ifPresent(data -> generated.addAll(EnchantLoreFormatter.equipmentLines(data, entry.level()))));
        }
        lore.addAll(generated);
        meta.lore(lore.build());
        meta.getPersistentDataContainer().set(generatedLoreKey, PersistentDataType.STRING, encodeGenerated(generated));
    }

    public String formatEquipped(ItemStack equipment) {
        return getEquippedEntries(equipment).stream()
                .map(entry -> registry.get(entry.id())
                        .map(data -> data.displayName() + (entry.level() > 1 ? " " + entry.level() : ""))
                        .orElse(entry.id()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("없음");
    }

    private List<EquippedEnchant> getEquippedEntries(ItemStack equipment) {
        if (equipment == null || !equipment.hasItemMeta()) return List.of();
        migrateLegacyEnchantments(equipment);
        return allNativeEnchantments(equipment).entrySet().stream()
                .filter(entry -> NATIVE_NAMESPACE.equals(entry.getKey().getKey().getNamespace()))
                .map(entry -> new EquippedEnchant(entry.getKey().getKey().getKey(), entry.getValue()))
                .filter(entry -> registry.get(entry.id()).isPresent())
                .sorted(java.util.Comparator.comparing(EquippedEnchant::id))
                .toList();
    }

    /** Lazily upgrades old equipped_enchants PDC without touching unrelated PDC or vanilla enchants. */
    public boolean migrateLegacyEnchantments(ItemStack equipment) {
        if (equipment == null || !equipment.hasItemMeta()) return false;
        ItemMeta meta = equipment.getItemMeta();
        String raw = meta.getPersistentDataContainer().get(equippedKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return false;
        boolean changed = false;
        List<String> unknown = new ArrayList<>();
        for (String part : raw.split(",")) {
            String[] split = part.trim().split("@", 2);
            EnchantData data = registry.get(split[0]).orElse(null);
            if (data == null) {
                config.getPlugin().getLogger().warning("Preserving unknown legacy enchant '" + split[0] + "'");
                unknown.add(part.trim());
                continue;
            }
            int level = 1;
            if (split.length == 2) try { level = Integer.parseInt(split[1]); } catch (NumberFormatException ignored) { }
            Enchantment enchantment = nativeEnchant(data.enchantId());
            if (enchantment == null) continue;
            int safeLevel = Math.max(1, Math.min(data.maxLevel(), level));
            if (meta.getEnchantLevel(enchantment) < safeLevel) {
                meta.addEnchant(enchantment, safeLevel, true);
                changed = true;
            }
        }
        // Known entries have been copied; unknown values remain recoverable for a future definition.
        if (unknown.isEmpty()) meta.getPersistentDataContainer().remove(equippedKey);
        else meta.getPersistentDataContainer().set(equippedKey, PersistentDataType.STRING, String.join(",", unknown));
        equipment.setItemMeta(meta);
        return changed;
    }

    private Enchantment nativeEnchant(String enchantId) {
        return Registry.ENCHANTMENT.get(new NamespacedKey(NATIVE_NAMESPACE, registry.canonicalId(enchantId)));
    }

    private Map<Enchantment, Integer> allNativeEnchantments(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Map.of();
        ItemMeta meta = item.getItemMeta();
        return meta instanceof EnchantmentStorageMeta storage ? storage.getStoredEnchants() : meta.getEnchants();
    }

    private boolean matchesEquipment(EnchantData data, ItemStack equipment, WeaponType weapon) {
        if (data.enchantId().equals("unbreaking") && equipment.getType().getMaxDurability() <= 0) return false;
        if (data.weaponType() != null && data.weaponType() != weapon) return false;
        if (!data.equipmentCategory().isBlank()
                && !tierService.getCategory(equipment).name().equalsIgnoreCase(data.equipmentCategory())) {
            return false;
        }
        return data.materialPatterns().isEmpty() || data.materialPatterns().stream()
                .anyMatch(pattern -> matchesMaterialPattern(equipment.getType().name(), pattern));
    }

    private boolean matchesMaterialPattern(String material, String rawPattern) {
        String pattern = normalize(rawPattern).toUpperCase(Locale.ROOT);
        String value = material == null ? "" : material.toUpperCase(Locale.ROOT);
        if (pattern.isBlank()) return false;
        if (!pattern.contains("*")) return value.equals(pattern);
        String[] parts = pattern.split("\\*", -1);
        String prefix = parts.length == 0 ? "" : parts[0];
        String suffix = parts.length < 2 ? "" : parts[parts.length - 1];
        return value.startsWith(prefix) && value.endsWith(suffix) && value.length() >= prefix.length() + suffix.length();
    }

    private boolean hasInput(EnchantData data, SkillInputType input) {
        return data.triggers().stream().anyMatch(binding -> binding.type() == TriggerType.INPUT && binding.input() == input);
    }

    private boolean hasInputConflict(EnchantData candidate, List<EquippedEnchant> equipped) {
        List<SkillInputType> candidateInputs = candidate.triggers().stream()
                .filter(binding -> binding.type() == TriggerType.INPUT)
                .map(EnchantTriggerBinding::input)
                .filter(Objects::nonNull)
                .toList();
        if (candidateInputs.isEmpty()) return false;
        for (EquippedEnchant entry : equipped) {
            EnchantData existing = registry.get(entry.id()).orElse(null);
            if (existing == null) continue;
            boolean groupConflict = !candidate.conflictGroup().isBlank()
                    && candidate.conflictGroup().equalsIgnoreCase(existing.conflictGroup());
            boolean inputConflict = existing.triggers().stream().anyMatch(binding -> binding.type() == TriggerType.INPUT
                    && candidateInputs.contains(binding.input()));
            if (groupConflict || inputConflict) return true;
        }
        return false;
    }

    private String encode(List<EquippedEnchant> values) {
        return values.stream().map(entry -> entry.id() + "@" + entry.level())
                .reduce((left, right) -> left + "," + right).orElse("");
    }

    private String encodeGenerated(List<Component> lines) {
        return lines.stream().map(line -> PlainTextComponentSerializer.plainText().serialize(line))
                .collect(java.util.stream.Collectors.joining("\u001F"));
    }

    private Set<String> decodeGenerated(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        Set<String> values = new HashSet<>();
        for (String value : raw.split("\u001F", -1)) if (!value.isBlank()) values.add(value);
        return values;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public int getEnchantSlotLimit(ItemStack equipment) {
        int existing = promotionService.getUnlockedEnchantSlots(equipment);
        int baseline = Math.max(0, config.getEnchantsInt("enchant-slots.baseline."
                + tierService.getCategory(equipment).name(), 0));
        int direct = growthPolicy != null && growthPolicy.canEnchantDirectly(equipment) ? 1 : 0;
        if (growthPolicy == null || growthPolicy.grade(equipment) != com.hyunseo.hyunseorpg.equipment.EquipmentGrade.GRADE_4) {
            return Math.max(direct, Math.max(existing, baseline));
        }
        String id = itemService.getItemId(equipment).orElse("");
        SpecialEquipmentData data = specialEquipment.get(id).orElse(null);
        return data == null ? Math.max(direct, Math.max(existing, baseline))
                : Math.max(Math.max(direct, Math.max(existing, baseline)), data.customEnchantSlots());
    }

    private record EquippedEnchant(String id, int level) { }

    public record AppliedEnchant(String id, int level) { }
}
