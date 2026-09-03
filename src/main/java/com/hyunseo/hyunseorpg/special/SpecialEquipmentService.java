package com.hyunseo.hyunseorpg.special;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.crafting.CraftingRecipeData;
import com.hyunseo.hyunseorpg.crafting.CraftingRecipeRegistry;
import com.hyunseo.hyunseorpg.crafting.CraftingTransactionService;
import com.hyunseo.hyunseorpg.economy.CoinService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentEnhancementService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentPromotionService;
import com.hyunseo.hyunseorpg.equipment.EquipmentLoreBuilder;
import com.hyunseo.hyunseorpg.equipment.EquipmentGrade;
import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

/** Owns late-game equipment identity, unlock checks, recipes, and per-item soul progress. */
public final class SpecialEquipmentService {
    private final JavaPlugin plugin;
    private final ConfigService config;
    private final SpecialEquipmentRegistry registry;
    private final RPGItemService itemService;
    private final PlayerDataService playerDataService;
    private final InventoryDeliveryService delivery;
    private final EquipmentEnhancementService enhancement;
    private final EquipmentPromotionService promotion;
    private final NamespacedKey specialIdKey;
    private final NamespacedKey schemaKey;
    private final Set<UUID> unlockBypass = ConcurrentHashMap.newKeySet();
    private CraftingTransactionService craftingTransactions;
    private CraftingRecipeRegistry craftingRecipes;

    public SpecialEquipmentService(JavaPlugin plugin, ConfigService config, SpecialEquipmentRegistry registry,
                                   RPGItemService itemService, PlayerDataService playerDataService,
                                   InventoryDeliveryService delivery, EquipmentEnhancementService enhancement,
                                   EquipmentPromotionService promotion) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
        this.itemService = itemService;
        this.playerDataService = playerDataService;
        this.delivery = delivery;
        this.enhancement = enhancement;
        this.promotion = promotion;
        this.specialIdKey = new NamespacedKey(plugin, "special_equipment_id");
        this.schemaKey = new NamespacedKey(plugin, "special_equipment_schema");
    }

    public SpecialEquipmentRegistry registry() {
        return registry;
    }

    /** Connects special-equipment output tagging to the canonical crafting transaction. */
    public void setCraftingTransactionService(CraftingTransactionService transactions,
                                              CraftingRecipeRegistry recipes) {
        this.craftingTransactions = transactions;
        this.craftingRecipes = recipes;
        transactions.setOutputFactory(this::createCraftingOutput);
    }

    public ItemStack createRecipeOutput(CraftingRecipeData recipe) {
        return createCraftingOutput(null, recipe);
    }

    public ItemStack create(String id, int amount) {
        SpecialEquipmentData data = registry.get(id).orElse(null);
        if (data == null) return null;
        ItemStack item = itemService.create(data.itemId(), Math.max(1, amount)).orElse(null);
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(specialIdKey, PersistentDataType.STRING, data.id());
        meta.getPersistentDataContainer().set(schemaKey, PersistentDataType.INTEGER, 1);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "equipment_grade"),
                PersistentDataType.INTEGER, data.grade());
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "enhancement_level"),
                PersistentDataType.INTEGER, 0);
        if (data.customEnchantSlots() > 0) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "unlocked_enchant_slots"),
                    PersistentDataType.INTEGER, data.customEnchantSlots());
        }
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
                    AttributeModifier.Operation.ADD_NUMBER));
        }
        EquipmentLoreBuilder lore = EquipmentLoreBuilder.from(meta)
                .add(Component.text("Special equipment: " + data.element(), NamedTextColor.LIGHT_PURPLE))
                .add(Component.text("Equipment grade: " + data.grade(), NamedTextColor.LIGHT_PURPLE))
                .add(Component.text("Enhancement: unavailable", NamedTextColor.GRAY))
                .add(Component.text("Promotion: unavailable", NamedTextColor.GRAY))
                .add(Component.text("Custom enchantment: " + (data.allowCustomEnchants() ? "available" : "unavailable"), NamedTextColor.AQUA))
                .add(Component.text("Final equipment material: " + (data.finalGearMaterialAllowed() ? "allowed" : "not allowed"), NamedTextColor.GRAY));
        data.abilities().values().forEach(ability -> lore
                .add(Component.text("Ability: " + ability.displayName(), NamedTextColor.GOLD))
                .add(Component.text("- " + ability.description(), NamedTextColor.GRAY)));
        meta.lore(lore
                .add(Component.text("Growth is controlled by special-equipment.yml", NamedTextColor.DARK_GRAY))
                .build());
        item.setItemMeta(meta);
        return item;
    }

    private boolean isDurabilityEquipment(SpecialEquipmentData data) {
        String type = data.equipmentType().toUpperCase(Locale.ROOT);
        return !type.equals("CONSUMABLE") && !type.equals("BUFF") && !type.equals("UNKNOWN");
    }

    public String getSpecialId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        String marker = item.getItemMeta().getPersistentDataContainer().get(specialIdKey, PersistentDataType.STRING);
        if (marker != null && registry.get(marker).isPresent()) return marker.toLowerCase(Locale.ROOT);
        String itemId = itemService.getItemId(item).orElse("");
        return registry.get(itemId).map(SpecialEquipmentData::id).orElse("");
    }

    public SpecialEquipmentData getData(ItemStack item) {
        String id = getSpecialId(item);
        return id.isBlank() ? null : registry.get(id).orElse(null);
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
            missing.add("equipment " + data.requiredEquipmentId() + " +" + data.minimumEnhancementLevel()
                    + " promotion " + data.minimumPromotionStage());
        }
        return missing;
    }

    public boolean craft(Player player, String id) {
        if (craftingTransactions == null || craftingRecipes == null) return false;
        String recipeId = craftingRecipes.specialRecipeId(id).orElse(null);
        if (recipeId == null) {
            player.sendMessage(Component.text("Special equipment recipe is not available.", NamedTextColor.RED));
            return false;
        }
        CraftingTransactionService.Result result = craftingTransactions.craft(player, recipeId, false);
        if (result.success()) return true;
        player.sendMessage(Component.text(result.status() == CraftingTransactionService.Status.MISSING_INGREDIENTS
                ? "Materials are insufficient." : "Special equipment could not be crafted.", NamedTextColor.RED));
        return false;
    }

    private ItemStack createCraftingOutput(Player player, CraftingRecipeData recipe) {
        String specialId = craftingRecipes.specialEquipmentIdForRecipe(recipe.id()).orElse(null);
        if (specialId != null) return create(specialId, recipe.outputAmount());
        if (recipe.outputId().startsWith("vanilla:")) {
            Material material = Material.matchMaterial(recipe.outputId().substring("vanilla:".length()).toUpperCase(Locale.ROOT));
            return material == null ? null : new ItemStack(material, recipe.outputAmount());
        }
        return itemService.create(recipe.outputId(), recipe.outputAmount()).orElse(null);
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
            if (promotionStageRank(promotion.getStage(item)) < data.minimumPromotionStage()) continue;
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

    private int promotionStageRank(String stage) {
        if (stage == null || stage.isBlank()) return 0;
        String[] parts = stage.split("-", 2);
        if (parts.length != 2) return 0;
        try {
            int gradeOrder = config.getEquipmentGrowthInt("promotion.grades." + parts[0] + ".order", 0);
            int star = Integer.parseInt(parts[1]);
            return gradeOrder <= 0 ? 0 : (gradeOrder - 1) * 5 + star;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
