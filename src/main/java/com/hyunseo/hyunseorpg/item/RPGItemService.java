package com.hyunseo.hyunseorpg.item;

import com.hyunseo.hyunseorpg.enchant.EnchantRegistry;
import com.hyunseo.hyunseorpg.farming.CropQuality;
import com.hyunseo.hyunseorpg.ui.KoreanDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** PDC item identity is authoritative; Material is only the visual base. */
public final class RPGItemService {
    private static final Map<String, String> LEGACY_ITEM_ALIASES = Map.of(
            "upgrade_stone_fragment", "basic_upgrade_fragment");
    private static final Set<String> RETIRED_SPECIAL_ITEM_IDS = Set.of(
            "fire_sword", "fire_bow", "water_sword", "wind_sword", "wind_bow",
            "earth_sword", "earth_bow", "earth_mace", "ice_sword", "ice_bow",
            "magic_sword", "magic_bow");
    private final RPGItemRegistry registry;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey farmingCropIdKey;
    private final NamespacedKey farmingQualityKey;
    private final NamespacedKey farmingDataVersionKey;
    private Consumer<ItemStack> itemNormalizer = item -> { };

    public RPGItemService(JavaPlugin plugin, RPGItemRegistry registry) {
        this.registry = registry;
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
        this.farmingCropIdKey = new NamespacedKey(plugin, "farming_crop_id");
        this.farmingQualityKey = new NamespacedKey(plugin, "farming_quality");
        this.farmingDataVersionKey = new NamespacedKey(plugin, "farming_crop_data_version");
    }

    public Optional<ItemStack> create(String itemId, int amount) {
        return createInternal(itemId, amount, true);
    }

    private Optional<ItemStack> createInternal(String itemId, int amount, boolean normalize) {
        String normalizedId = itemId == null ? "" : itemId.trim().toLowerCase(Locale.ROOT);
        if (EnchantRegistry.isRetiredBookItemId(normalizedId)
                || RETIRED_SPECIAL_ITEM_IDS.contains(normalizedId)
                || normalizedId.startsWith("cooked_")) {
            return Optional.empty();
        }
        RPGItemData data = registry.get(normalizedId).orElse(null);
        if (data == null || amount <= 0) return Optional.empty();
        ItemStack itemStack = new ItemStack(data.material(), amount);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return Optional.empty();
        meta.displayName(Component.text(data.displayName(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        if (!data.rarity().isBlank()) {
            List<Component> lore = new java.util.ArrayList<>();
            lore.add(Component.text("\uB4F1\uAE09: " + KoreanDisplay.rarity(data.rarity()), rarityColor(data.rarity()))
                    .decoration(TextDecoration.ITALIC, false));
            lore.addAll(data.lore().stream()
                    .map(line -> Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .toList());
            meta.lore(lore);
        } else if (!data.lore().isEmpty()) {
            meta.lore(data.lore().stream()
                    .map(line -> Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .toList());
        }
        if (data.customModelData() > 0) meta.setCustomModelData(data.customModelData());
        meta.setUnbreakable(false);
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, data.itemId());
        itemStack.setItemMeta(meta);
        if (normalize) itemNormalizer.accept(itemStack);
        return Optional.of(itemStack);
    }

    /** Rebuilds farming items from their registry definition and removes foreign metadata. */
    public void normalizeFarmingItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        String itemId = getItemId(item).orElse("").toLowerCase(Locale.ROOT);
        FarmingIdentity identity = farmingIdentity(itemId).orElse(null);
        if (identity == null) return;
        Optional<ItemStack> canonical = createInternal(itemId, item.getAmount(), false);
        if (canonical.isEmpty()) return;
        ItemStack replacement = canonical.orElseThrow();
        int amount = item.getAmount();
        ItemMeta canonicalMeta = replacement.getItemMeta();
        if (canonicalMeta == null) return;
        var pdc = canonicalMeta.getPersistentDataContainer();
        pdc.set(farmingCropIdKey, PersistentDataType.STRING, identity.cropId());
        pdc.set(farmingQualityKey, PersistentDataType.STRING, identity.qualityId());
        pdc.set(farmingDataVersionKey, PersistentDataType.INTEGER, 1);
        item.setType(replacement.getType());
        item.setItemMeta(canonicalMeta);
        item.setAmount(amount);
    }

    /** Reads the canonical crop family stored on a farming item. */
    public Optional<String> getFarmingCropId(ItemStack item) {
        String stored = readFarmingString(item, farmingCropIdKey);
        if (!stored.isBlank()) return Optional.of(stored);
        return getItemId(item).flatMap(id -> farmingIdentity(id).map(FarmingIdentity::cropId));
    }

    /** Reads the canonical quality stored on a farming item. */
    public Optional<CropQuality> getFarmingQuality(ItemStack item) {
        String stored = readFarmingString(item, farmingQualityKey);
        if (!stored.isBlank() && !stored.equals("none")) {
            Optional<CropQuality> parsed = CropQuality.fromId(stored);
            if (parsed.isPresent()) return parsed;
        }
        return getItemId(item).flatMap(id -> farmingIdentity(id)
                .flatMap(identity -> CropQuality.fromId(identity.qualityId())));
    }

    /** Returns the farming item metadata version, or zero for an unnormalized item. */
    public int getFarmingDataVersion(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer version = item.getItemMeta().getPersistentDataContainer()
                .get(farmingDataVersionKey, PersistentDataType.INTEGER);
        return version == null ? 0 : Math.max(0, version);
    }

    private String readFarmingString(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return "";
        String value = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isFarmingItemId(String itemId) {
        return farmingIdentity(itemId).isPresent();
    }

    static Optional<FarmingIdentity> farmingIdentity(String rawItemId) {
        String itemId = rawItemId == null ? "" : rawItemId.trim().toLowerCase(Locale.ROOT);
        if (itemId.startsWith("seed_")) {
            String crop = itemId.substring("seed_".length());
            return crop.isBlank() ? Optional.empty() : Optional.of(new FarmingIdentity(itemId, crop, "none"));
        }
        if (itemId.startsWith("crop_")) {
            String tail = itemId.substring("crop_".length());
            int marker = tail.indexOf("_quality_");
            if (marker < 0) return tail.isBlank()
                    ? Optional.empty() : Optional.of(new FarmingIdentity(itemId, tail, CropQuality.NORMAL.id()));
            String crop = tail.substring(0, marker);
            String quality = tail.substring(marker + "_quality_".length());
            return crop.isBlank() || CropQuality.fromId(quality).isEmpty()
                    ? Optional.empty() : Optional.of(new FarmingIdentity(itemId, crop, quality));
        }
        if (itemId.startsWith("processed_")) {
            String tail = itemId.substring("processed_".length());
            int lastSeparator = tail.lastIndexOf('_');
            String quality = lastSeparator > 0 ? tail.substring(lastSeparator + 1) : "normal";
            if (CropQuality.fromId(quality).isEmpty()) quality = CropQuality.NORMAL.id();
            String product = lastSeparator > 0 ? tail.substring(0, lastSeparator) : tail;
            int cropSeparator = product.indexOf('_');
            String crop = cropSeparator > 0 ? product.substring(0, cropSeparator) : product;
            return crop.isBlank() ? Optional.empty() : Optional.of(new FarmingIdentity(itemId, crop, quality));
        }
        return Optional.empty();
    }

    record FarmingIdentity(String itemId, String cropId, String qualityId) { }

    static Set<String> stackableFarmingMetadataKeys() {
        return Set.of("item_id", "farming_crop_id", "farming_quality", "farming_crop_data_version");
    }

    public java.util.Set<String> registeredItemIds() {
        return registry.getAll().stream().map(RPGItemData::itemId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public void setItemNormalizer(Consumer<ItemStack> itemNormalizer) {
        this.itemNormalizer = itemNormalizer == null ? item -> { } : itemNormalizer;
    }

    private NamedTextColor rarityColor(String rarity) {
        return switch (rarity.toUpperCase(Locale.ROOT)) {
            case "COMMON" -> NamedTextColor.WHITE;
            case "UNCOMMON" -> NamedTextColor.GREEN;
            case "RARE" -> NamedTextColor.BLUE;
            case "EPIC" -> NamedTextColor.LIGHT_PURPLE;
            case "LEGENDARY" -> NamedTextColor.GOLD;
            default -> NamedTextColor.GRAY;
        };
    }

    public Optional<RPGItemData> getData(String itemId) {
        return registry.get(itemId);
    }

    public List<RPGItemData> getAllData() {
        return registry.getAll();
    }

    public Optional<String> getItemId(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return Optional.empty();
        ItemMeta meta = itemStack.getItemMeta();
        String itemId = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (itemId == null || itemId.isBlank()) return Optional.empty();
        String normalized = itemId.toLowerCase(Locale.ROOT);
        String canonical = LEGACY_ITEM_ALIASES.get(normalized);
        if (canonical != null) {
            meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, canonical);
            itemStack.setItemMeta(meta);
            normalized = canonical;
        }
        return Optional.of(normalized);
    }

    public boolean isItem(ItemStack itemStack, String itemId) {
        return getItemId(itemStack).map(id -> id.equalsIgnoreCase(itemId)).orElse(false);
    }

    public boolean hasTag(String itemId, String tag) {
        String normalized = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
        return registry.get(itemId).map(data -> data.tags().contains(normalized)).orElse(false);
    }

    public void markItemId(ItemMeta meta, String itemId) {
        if (meta == null || itemId == null || itemId.isBlank()) return;
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, itemId.trim().toLowerCase(Locale.ROOT));
    }
}
