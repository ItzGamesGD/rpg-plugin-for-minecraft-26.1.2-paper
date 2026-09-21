package com.hyunseo.hyunseorpg.item;

import com.hyunseo.hyunseorpg.enchant.EnchantRegistry;
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
    private static final Map<String, String> LEGACY_ITEM_ALIASES = Map.of();
    private static final Set<String> RETIRED_SPECIAL_ITEM_IDS = Set.of(
            "fire_sword", "fire_bow", "water_sword", "wind_sword", "wind_bow",
            "earth_sword", "earth_bow", "earth_mace", "ice_sword", "ice_bow",
            "magic_sword", "magic_bow");
    private final RPGItemRegistry registry;
    private final NamespacedKey itemIdKey;
    private Consumer<ItemStack> itemNormalizer = item -> { };

    public RPGItemService(JavaPlugin plugin, RPGItemRegistry registry) {
        this.registry = registry;
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
    }

    public Optional<ItemStack> create(String itemId, int amount) {
        return createInternal(itemId, amount, true);
    }

    private Optional<ItemStack> createInternal(String itemId, int amount, boolean normalize) {
        String normalizedId = itemId == null ? "" : itemId.trim().toLowerCase(Locale.ROOT);
        if (EnchantRegistry.isRetiredBookItemId(normalizedId)
                || RETIRED_SPECIAL_ITEM_IDS.contains(normalizedId)) {
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
