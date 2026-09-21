package com.hyunseo.hyunseorpg.special;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentEnhancementService;
import com.hyunseo.hyunseorpg.equipment.EquipmentLoreBuilder;
import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Owns late-game equipment identity, unlock checks, and per-item soul progress. */
public final class SpecialEquipmentService {
    public static final String POSEIDON_ID = "poseidon_spear";
    private static final String LEGACY_POSEIDON_ID = "poseidons_spear";
    private final JavaPlugin plugin;
    private final ConfigService config;
    private final SpecialEquipmentRegistry registry;
    private final RPGItemService itemService;
    private final PlayerDataService playerDataService;
    private final InventoryDeliveryService delivery;
    private final EquipmentEnhancementService enhancement;
    private final NamespacedKey specialIdKey;
    private final NamespacedKey schemaKey;
    private final Set<UUID> unlockBypass = ConcurrentHashMap.newKeySet();

    public SpecialEquipmentService(JavaPlugin plugin, ConfigService config, SpecialEquipmentRegistry registry,
                                   RPGItemService itemService, PlayerDataService playerDataService,
                                   InventoryDeliveryService delivery, EquipmentEnhancementService enhancement) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
        this.itemService = itemService;
        this.playerDataService = playerDataService;
        this.delivery = delivery;
        this.enhancement = enhancement;
        this.specialIdKey = new NamespacedKey(plugin, "special_equipment_id");
        this.schemaKey = new NamespacedKey(plugin, "special_equipment_schema");
    }

    public SpecialEquipmentRegistry registry() {
        return registry;
    }

    public ItemStack create(String id, int amount) {
        SpecialEquipmentData data = registry.get(id).orElse(null);
        if (data == null) return null;
        ItemStack item = itemService.create(data.itemId(), Math.max(1, amount)).orElse(null);
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(specialIdKey, PersistentDataType.STRING, canonicalId(data));
        meta.getPersistentDataContainer().set(schemaKey, PersistentDataType.INTEGER, 1);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "enhancement_level"),
                PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "special_equipment"),
                PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "equipment_element"),
                PersistentDataType.STRING, data.element());
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "final_gear_material_allowed"),
                PersistentDataType.BYTE, (byte) (data.finalGearMaterialAllowed() ? 1 : 0));
        if (data.unbreakable() && isDurabilityEquipment(data)) {
            // Special weapons and equipment are intentionally not part of the durability economy.
            meta.setUnbreakable(true);
        }
        if (data.itemId().equals(POSEIDON_ID)) {
            // Poseidon's physical throw/retrieval contract is vanilla Trident + Loyalty.
            meta.addEnchant(Enchantment.LOYALTY, 3, true);
        }
        if (data.id().equals("flowing_water_sword")) {
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                    UUID.fromString("9d4b44ca-51b4-4b6a-b7f0-0e9c3e6c2a10"),
                    "hyunseorpg_flowing_water_speed", 0.75D,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
        if (data.id().equals("moonlit_afterglow")) {
            double attackSpeed = Math.max(0.1D, config.getSpecialEquipmentDouble(
                    "special-equipment.items.moonlit_afterglow.abilities.base-attack-speed", 6.4D));
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                    UUID.fromString("b99bb286-d005-4baa-9364-caa975b299ae"),
                    // Swords contribute -2.4 to the player's 4.0 base (effective 1.6).
                    "hyunseorpg_moonlit_afterglow_speed", attackSpeed - 1.6D,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        }
        int enhancementCap = enhancement.getMaximumLevel(item);
        EquipmentLoreBuilder lore = EquipmentLoreBuilder.from(meta)
                .add(Component.text("Special equipment: " + data.element(), NamedTextColor.LIGHT_PURPLE))
                .add(Component.text(enhancementCapabilityText(enhancementCap), NamedTextColor.GRAY))
                .add(Component.text("Custom enchantment: " + (data.allowCustomEnchants() ? "available" : "unavailable"), NamedTextColor.AQUA))
                .add(Component.text("Final equipment material: " + (data.finalGearMaterialAllowed() ? "allowed" : "not allowed"), NamedTextColor.GRAY));
        data.abilities().values().forEach(ability -> lore
                .add(Component.text("Ability: " + ability.displayName(), NamedTextColor.GOLD))
                .add(Component.text("- " + ability.description(), NamedTextColor.GRAY)));
        meta.lore(lore
                .add(Component.text("Growth is controlled by special-equipment.yml", NamedTextColor.DARK_GRAY))
                .build());
        item.setItemMeta(meta);
        ensureRuntimeComponents(item);
        return item;
    }

    static String enhancementCapabilityText(int maximumLevel) {
        return maximumLevel > 0
                ? "Enhancement: available (max +" + maximumLevel + ")"
                : "Enhancement: unavailable";
    }

    /** Normalizes legacy identities and use components on both new and pre-existing special items. */
    public void ensureRuntimeComponents(ItemStack item) {
        String id = getSpecialId(item);
        if (id.equals(POSEIDON_ID)) {
            normalizePoseidon(item);
            return;
        }
    }

    private boolean isDurabilityEquipment(SpecialEquipmentData data) {
        String type = data.equipmentType().toUpperCase(Locale.ROOT);
        return !type.equals("CONSUMABLE") && !type.equals("BUFF") && !type.equals("UNKNOWN");
    }

    public String getSpecialId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        String marker = item.getItemMeta().getPersistentDataContainer().get(specialIdKey, PersistentDataType.STRING);
        String resolved;
        if (marker != null && (registry.get(marker).isPresent() || canonicalId(marker).equals(POSEIDON_ID))) {
            resolved = canonicalId(marker);
        } else {
            String itemId = itemService.getItemId(item).orElse("");
            resolved = registry.get(itemId).map(this::canonicalId).orElseGet(() -> registry.getAll().stream()
                    .filter(data -> data.itemId().equalsIgnoreCase(itemId))
                    .map(this::canonicalId)
                    .findFirst().orElse(""));
        }
        if (resolved.equals(POSEIDON_ID)) normalizePoseidon(item);
        return resolved;
    }

    private void normalizePoseidon(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String marker = meta.getPersistentDataContainer().get(specialIdKey, PersistentDataType.STRING);
        boolean changed = !POSEIDON_ID.equals(marker);
        if (changed) meta.getPersistentDataContainer().set(specialIdKey, PersistentDataType.STRING, POSEIDON_ID);
        if (meta.getEnchantLevel(Enchantment.LOYALTY) != 3) {
            meta.addEnchant(Enchantment.LOYALTY, 3, true);
            changed = true;
        }
        if (changed) item.setItemMeta(meta);
    }

    private String canonicalId(SpecialEquipmentData data) {
        return canonicalId(data.itemId().equalsIgnoreCase(POSEIDON_ID) ? POSEIDON_ID : data.id());
    }

    static String canonicalId(String id) {
        String normalized = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        return normalized.equals(LEGACY_POSEIDON_ID) ? POSEIDON_ID : normalized;
    }

    public SpecialEquipmentData getData(ItemStack item) {
        String id = getSpecialId(item);
        if (id.isBlank()) return null;
        return registry.get(id).orElseGet(() -> registry.getAll().stream()
                .filter(data -> canonicalId(data).equals(id))
                .findFirst().orElse(null));
    }

    public boolean isSpecial(ItemStack item) {
        return !getSpecialId(item).isBlank();
    }

    public boolean isUnlocked(Player player, String id) {
        SpecialEquipmentData data = registry.get(id).orElse(null);
        if (data == null || !data.enabled()) return false;
        if (unlockBypass.contains(player.getUniqueId())) return true;
        return missingRequirements(player, data).isEmpty();
    }

    public List<String> missingRequirements(Player player, String id) {
        SpecialEquipmentData data = registry.get(id).orElse(null);
        return data == null ? List.of("unknown equipment") : missingRequirements(player, data);
    }

    public List<String> missingRequirements(Player player, SpecialEquipmentData data) {
        List<String> missing = new ArrayList<>();
        PlayerRPGData progress = playerDataService.getOrLoad(player);
        if (progress.getBaseLevel() < data.requiredRpgLevel()) {
            missing.add("RPG level " + data.requiredRpgLevel() + " (current " + progress.getBaseLevel() + ")");
        }
        for (String world : data.requiredWorlds()) {
            if (!progress.hasVisitedWorld(world)) {
                missing.add("visit world " + world);
            }
        }
        for (Map.Entry<String, Long> entry : data.requiredMobKills().entrySet()) {
            long current = progress.getCustomMobKillCount(entry.getKey());
            if (current < entry.getValue()) missing.add(entry.getKey() + " kills " + entry.getValue() + " (current " + current + ")");
        }
        for (Map.Entry<String, Long> entry : data.requiredBossKills().entrySet()) {
            long current = progress.getBossKillCount(entry.getKey());
            if (current < entry.getValue()) missing.add(entry.getKey() + " boss kills " + entry.getValue() + " (current " + current + ")");
        }
        for (Map.Entry<String, Integer> entry : data.requiredItems().entrySet()) {
            int current = countIngredient(player.getInventory(), entry.getKey());
            if (current < entry.getValue()) missing.add(entry.getKey() + " x" + entry.getValue() + " (current " + current + ")");
        }
        if (!data.requiredEquipmentId().isBlank() && !hasRequiredEquipment(player, data)) {
            missing.add("equipment " + data.requiredEquipmentId() + " +" + data.minimumEnhancementLevel());
        }
        return missing;
    }

    public long getSoul(ItemStack item, String mobId) {
        if (!isSpecial(item)) return 0L;
        NamespacedKey key = soulKey(mobId);
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(key, PersistentDataType.LONG, 0L);
    }

    public boolean addSoul(ItemStack item, String mobId, long amount) {
        if (!isSpecial(item) || amount <= 0L) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        long current = getSoul(item, mobId);
        long next = Long.MAX_VALUE - current < amount ? Long.MAX_VALUE : current + amount;
        meta.getPersistentDataContainer().set(soulKey(mobId), PersistentDataType.LONG, next);
        item.setItemMeta(meta);
        return true;
    }

    public void setUnlockBypass(Player player, boolean enabled) {
        if (enabled) unlockBypass.add(player.getUniqueId());
        else unlockBypass.remove(player.getUniqueId());
    }

    public boolean hasUnlockBypass(Player player) {
        return unlockBypass.contains(player.getUniqueId());
    }

    private boolean hasRequiredEquipment(Player player, SpecialEquipmentData data) {
        for (ItemStack item : allItems(player)) {
            if (item == null || item.getType().isAir()) continue;
            boolean idMatch = itemService.isItem(item, data.requiredEquipmentId());
            boolean materialMatch = item.getType().name().equalsIgnoreCase(data.requiredEquipmentId());
            if (!idMatch && !materialMatch) continue;
            if (enhancement.getLevel(item) < data.minimumEnhancementLevel()) continue;
            return true;
        }
        return false;
    }

    private List<ItemStack> allItems(Player player) {
        List<ItemStack> items = new ArrayList<>(Arrays.asList(player.getInventory().getContents()));
        items.addAll(Arrays.asList(player.getInventory().getArmorContents()));
        items.add(player.getInventory().getItemInOffHand());
        return items;
    }

    private boolean hasIngredients(Player player, Map<String, Integer> inputs) {
        for (Map.Entry<String, Integer> input : inputs.entrySet()) {
            if (countIngredient(player.getInventory(), input.getKey()) < input.getValue()) return false;
        }
        return true;
    }

    private void consumeIngredients(Player player, Map<String, Integer> inputs) {
        for (Map.Entry<String, Integer> input : inputs.entrySet()) {
            int remaining = input.getValue();
            for (int slot = 0; slot < player.getInventory().getSize() && remaining > 0; slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (!matchesIngredient(item, input.getKey())) continue;
                int taken = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - taken);
                remaining -= taken;
                if (item.getAmount() <= 0) player.getInventory().setItem(slot, null);
            }
        }
    }

    private int countIngredient(PlayerInventory inventory, String ingredient) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) if (matchesIngredient(item, ingredient)) total += item.getAmount();
        return total;
    }

    private boolean matchesIngredient(ItemStack item, String rawIngredient) {
        if (item == null || item.getType().isAir()) return false;
        String ingredient = rawIngredient == null ? "" : rawIngredient.trim();
        if (ingredient.regionMatches(true, 0, "vanilla:", 0, 8)) {
            return item.getType().name().equalsIgnoreCase(ingredient.substring(8));
        }
        return itemService.isItem(item, ingredient);
    }

    private NamespacedKey soulKey(String mobId) {
        String safe = mobId == null ? "unknown" : mobId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return new NamespacedKey(plugin, "special_soul_" + safe);
    }


}
