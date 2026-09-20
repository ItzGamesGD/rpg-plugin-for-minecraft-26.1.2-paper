package com.hyunseo.hyunseorpg.enchant;

import com.hyunseo.hyunseorpg.enchant.nativeapi.RetiredVanillaEnchantments;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.equipment.EquipmentInstanceService;
import com.hyunseo.hyunseorpg.equipment.EquipmentLoreBuilder;
import com.hyunseo.hyunseorpg.equipment.EquipmentTierService;
import com.hyunseo.hyunseorpg.equipment.trigger.EnchantTriggerBinding;
import com.hyunseo.hyunseorpg.equipment.trigger.TriggerType;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.skill.SkillInputType;
import com.hyunseo.hyunseorpg.weapon.WeaponService;
import com.hyunseo.hyunseorpg.weapon.WeaponType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
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
    private final WeaponService weaponService;
    private final EquipmentTierService tierService;
    private final EquipmentInstanceService equipmentInstances;
    private final NamespacedKey equippedKey;
    private final NamespacedKey generatedLoreKey;
    private final NamespacedKey bookLevelKey;
    private final NamespacedKey bookDataVersionKey;
    private final NamespacedKey bookInputKey;
    private final NamespacedKey bookCategoryKey;
    private final NamespacedKey swiftSneakSyncKey;
    private final NamespacedKey swiftSneakOriginalKey;
    private final NamespacedKey itemIdKey;

    public EnchantService(JavaPlugin plugin, ConfigService config, EnchantRegistry registry, RPGItemService itemService,
                          WeaponService weaponService,
                          EquipmentTierService tierService,
                          EquipmentInstanceService equipmentInstances) {
        this.registry = registry;
        this.config = config;
        this.itemService = itemService;
        this.weaponService = weaponService;
        this.tierService = tierService;
        this.equipmentInstances = equipmentInstances;
        this.equippedKey = new NamespacedKey(plugin, "equipped_enchants");
        this.generatedLoreKey = new NamespacedKey(plugin, "generated_enchant_lore");
        this.bookLevelKey = new NamespacedKey(plugin, "enchant_book_level");
        this.bookDataVersionKey = new NamespacedKey(plugin, "enchant_book_data_version");
        this.bookInputKey = new NamespacedKey(plugin, "enchant_book_input");
        this.bookCategoryKey = new NamespacedKey(plugin, "enchant_book_category");
        this.swiftSneakSyncKey = new NamespacedKey(plugin, "swift_sneak_vanilla_sync");
        this.swiftSneakOriginalKey = new NamespacedKey(plugin, "swift_sneak_original_level");
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
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
            List<EnchantData> custom = allNativeEnchantments(book).keySet().stream()
                    .filter(enchantment -> NATIVE_NAMESPACE.equals(enchantment.getKey().getNamespace()))
                    .map(enchantment -> registry.get(enchantment.getKey().getKey()).orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
            // Compatibility books intentionally have one custom enchant. Reject ambiguous native books
            // instead of selecting whichever map entry happens to iterate first.
            if (custom.size() == 1) return Optional.of(custom.getFirst());
            if (custom.size() > 1) return Optional.empty();
        }
        // Temporary compatibility path for old generated books.
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

    /** Maintenance-only extraction result; normal book acquisition/application remains Minecraft-owned. */
    public Optional<ItemStack> createBook(String enchantId, int level) {
        EnchantData data = registry.get(enchantId).orElse(null);
        if (data == null) return Optional.empty();
        Enchantment nativeEnchant = nativeEnchant(data.enchantId());
        if (nativeEnchant == null) return Optional.empty();
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (!(meta instanceof EnchantmentStorageMeta storage)) return Optional.empty();
        int safeLevel = Math.max(1, Math.min(data.maxLevel(), level));
        storage.addStoredEnchant(nativeEnchant, safeLevel, true);
        book.setItemMeta(meta);
        return Optional.of(book);
    }

    /** Removes exactly the selected enchant and refreshes only generated enchant lore. */
    public boolean removeEnchantForExtraction(ItemStack equipment, String enchantId) {
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

    /** Maintenance-only mutation used by extraction; normal application is exclusively vanilla-anvil owned. */
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
        removeGeneratedLore(meta);
        equipment.setItemMeta(meta);
        return true;
    }

    /** Legacy cleanup/migration hook. Native enchantments use Minecraft's tooltip exclusively. */
    public void refreshEquippedLore(ItemStack equipment) {
        if (equipment == null || !equipment.hasItemMeta()) return;
        migrateLegacyEnchantments(equipment);
        ItemMeta meta = equipment.getItemMeta();
        if (meta == null) return;
        removeGeneratedLore(meta);
        equipment.setItemMeta(meta);
    }

    /** Called by item creation so books and equipped items share the same definition data. */
    public void refreshBookLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        migrateLegacyEnchantments(item);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        removeGeneratedLore(meta);
        item.setItemMeta(meta);
    }

    private void removeGeneratedLore(ItemMeta meta) {
        Set<String> oldGenerated = decodeGenerated(meta.getPersistentDataContainer().get(generatedLoreKey, PersistentDataType.STRING));
        EquipmentLoreBuilder lore = EquipmentLoreBuilder.from(meta);
        lore.removeIf(line -> {
            String plain = PlainTextComponentSerializer.plainText().serialize(line);
            return oldGenerated.contains(plain) || plain.equals(EnchantLoreFormatter.SECTION_TITLE)
                    || plain.startsWith("인챈트: ") || plain.startsWith("Enchant: ")
                    || plain.startsWith("?몄콌?? ");
        });
        meta.lore(lore.build());
        meta.getPersistentDataContainer().remove(generatedLoreKey);
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
        boolean changed = migrateRetiredNativeEnchantments(meta);
        changed |= migrateRetiredCustomBook(equipment, meta);
        String raw = meta.getPersistentDataContainer().get(equippedKey, PersistentDataType.STRING);
        if (raw != null && !raw.isBlank()) {
            List<String> preserved = new ArrayList<>();
            for (String encoded : raw.split(",")) {
                String[] parts = encoded.trim().split("@", 2);
                int level;
                try { level = parts.length == 2 ? Integer.parseInt(parts[1]) : 1; }
                catch (NumberFormatException exception) { preserved.add(encoded.trim()); continue; }
                String legacyId = normalize(parts[0]);
                legacyId = RetiredVanillaEnchantments.LEGACY_ALIASES.getOrDefault(legacyId, legacyId);
                String vanillaId = RetiredVanillaEnchantments.VANILLA_TARGETS.get(legacyId);
                EnchantData active = vanillaId == null ? registry.get(legacyId).orElse(null) : null;
                Enchantment target = vanillaId == null
                        ? (active == null ? null : nativeEnchant(active.enchantId()))
                        : Registry.ENCHANTMENT.get(new NamespacedKey(NamespacedKey.MINECRAFT, vanillaId));
                if (target == null) {
                    preserved.add(encoded.trim());
                    config.getPlugin().getLogger().warning("Preserving unmapped legacy enchant '" + encoded.trim() + "'");
                    continue;
                }
                int maximum = vanillaId == null ? active.maxLevel() : target.getMaxLevel();
                int desired = RetiredVanillaEnchantments.mergedLevel(levelOf(meta, target), level, 0, maximum);
                if (hasMigrationConflict(meta, target, null)) {
                    preserved.add(encoded.trim());
                    config.getPlugin().getLogger().warning("Preserving incompatible legacy enchant '" + encoded.trim() + "'");
                    continue;
                }
                setLevel(meta, target, desired);
                changed = true;
            }
            if (preserved.isEmpty()) meta.getPersistentDataContainer().remove(equippedKey);
            else meta.getPersistentDataContainer().set(equippedKey, PersistentDataType.STRING, String.join(",", preserved));
        }
        if (changed || raw != null) equipment.setItemMeta(meta);
        return changed;
    }

    private boolean migrateRetiredCustomBook(ItemStack item, ItemMeta meta) {
        if (item.getType() != Material.ENCHANTED_BOOK) return false;
        String itemId = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (itemId == null) return false;
        String retiredId = normalize(itemId).replaceFirst("^enchant_book_", "");
        retiredId = RetiredVanillaEnchantments.LEGACY_ALIASES.getOrDefault(retiredId, retiredId);
        String vanillaId = RetiredVanillaEnchantments.VANILLA_TARGETS.get(retiredId);
        if (vanillaId == null) return false;
        Enchantment target = Registry.ENCHANTMENT.get(new NamespacedKey(NamespacedKey.MINECRAFT, vanillaId));
        if (target == null || hasMigrationConflict(meta, target, null)) {
            config.getPlugin().getLogger().warning("Preserving incompatible retired enchant book '" + itemId + "'");
            return false;
        }
        int legacyLevel = meta.getPersistentDataContainer().getOrDefault(bookLevelKey, PersistentDataType.INTEGER, 1);
        setLevel(meta, target, RetiredVanillaEnchantments.mergedLevel(levelOf(meta, target), legacyLevel, 0,
                target.getMaxLevel()));
        cleanRetiredBookPresentation(meta);
        return true;
    }

    private boolean migrateRetiredNativeEnchantments(ItemMeta meta) {
        boolean changed = false;
        boolean convertedBook = false;
        Map<Enchantment, Integer> present = meta instanceof EnchantmentStorageMeta storage
                ? new LinkedHashMap<>(storage.getStoredEnchants()) : new LinkedHashMap<>(meta.getEnchants());
        for (Map.Entry<Enchantment, Integer> entry : present.entrySet()) {
            String namespace = entry.getKey().getKey().getNamespace();
            String retiredId = entry.getKey().getKey().getKey();
            String vanillaId = NATIVE_NAMESPACE.equals(namespace)
                    ? RetiredVanillaEnchantments.VANILLA_TARGETS.get(retiredId) : null;
            if (vanillaId == null) continue;
            Enchantment vanilla = Registry.ENCHANTMENT.get(new NamespacedKey(NamespacedKey.MINECRAFT, vanillaId));
            if (vanilla == null || hasMigrationConflict(meta, vanilla, entry.getKey())) {
                config.getPlugin().getLogger().warning("Preserving incompatible retired native enchant '" + retiredId + "'");
                continue;
            }
            int snapshot = retiredId.equals("swift_sneak")
                    ? meta.getPersistentDataContainer().getOrDefault(swiftSneakOriginalKey, PersistentDataType.INTEGER, 0) : 0;
            int desired = RetiredVanillaEnchantments.mergedLevel(
                    levelOf(meta, vanilla), entry.getValue(), snapshot, vanilla.getMaxLevel());
            setLevel(meta, vanilla, desired);
            removeLevel(meta, entry.getKey());
            if (retiredId.equals("swift_sneak")) {
                meta.getPersistentDataContainer().remove(swiftSneakSyncKey);
                meta.getPersistentDataContainer().remove(swiftSneakOriginalKey);
            }
            changed = true;
            convertedBook |= meta instanceof EnchantmentStorageMeta;
        }
        // A prior bridge snapshot can survive after its retired enchant was already removed.
        Integer snapshot = meta.getPersistentDataContainer().get(swiftSneakOriginalKey, PersistentDataType.INTEGER);
        if (snapshot != null) {
            Enchantment swift = Enchantment.SWIFT_SNEAK;
            setLevel(meta, swift, RetiredVanillaEnchantments.mergedLevel(levelOf(meta, swift), 0,
                    snapshot, swift.getMaxLevel()));
            meta.getPersistentDataContainer().remove(swiftSneakSyncKey);
            meta.getPersistentDataContainer().remove(swiftSneakOriginalKey);
            changed = true;
        }
        if (convertedBook) cleanRetiredBookPresentation(meta);
        return changed;
    }

    @SuppressWarnings("deprecation")
    private void cleanRetiredBookPresentation(ItemMeta meta) {
        meta.displayName(null);
        meta.lore(null);
        if (meta.hasCustomModelData()) meta.setCustomModelData(null);
        var pdc = meta.getPersistentDataContainer();
        pdc.remove(itemIdKey);
        pdc.remove(bookLevelKey);
        pdc.remove(bookDataVersionKey);
        pdc.remove(bookInputKey);
        pdc.remove(bookCategoryKey);
        pdc.remove(generatedLoreKey);
    }

    private boolean hasMigrationConflict(ItemMeta meta, Enchantment candidate, Enchantment ignored) {
        return allEnchantments(meta).keySet().stream().anyMatch(existing -> !existing.equals(candidate)
                && !existing.equals(ignored)
                && !(NATIVE_NAMESPACE.equals(existing.getKey().getNamespace())
                    && RetiredVanillaEnchantments.IDS.contains(existing.getKey().getKey()))
                && (candidate.conflictsWith(existing) || existing.conflictsWith(candidate)));
    }

    private Map<Enchantment, Integer> allEnchantments(ItemMeta meta) {
        return meta instanceof EnchantmentStorageMeta storage ? storage.getStoredEnchants() : meta.getEnchants();
    }

    private int levelOf(ItemMeta meta, Enchantment enchantment) {
        return allEnchantments(meta).getOrDefault(enchantment, 0);
    }

    private void setLevel(ItemMeta meta, Enchantment enchantment, int level) {
        if (level <= 0) return;
        if (meta instanceof EnchantmentStorageMeta storage) storage.addStoredEnchant(enchantment, level, true);
        else meta.addEnchant(enchantment, level, true);
    }

    private void removeLevel(ItemMeta meta, Enchantment enchantment) {
        if (meta instanceof EnchantmentStorageMeta storage) storage.removeStoredEnchant(enchantment);
        else meta.removeEnchant(enchantment);
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

    private Set<String> decodeGenerated(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        Set<String> values = new HashSet<>();
        for (String value : raw.split("\u001F", -1)) if (!value.isBlank()) values.add(value);
        return values;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record EquippedEnchant(String id, int level) { }

    public record AppliedEnchant(String id, int level) { }
}
