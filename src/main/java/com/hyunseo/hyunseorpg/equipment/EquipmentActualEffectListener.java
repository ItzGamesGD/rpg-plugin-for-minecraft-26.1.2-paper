package com.hyunseo.hyunseorpg.equipment;

import com.hyunseo.hyunseorpg.activity.ActivityBlockRewardValidator;
import com.hyunseo.hyunseorpg.combat.CombatService;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentEnhancementService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentPromotionService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Applies stored equipment growth to vanilla gameplay events. */
public final class EquipmentActualEffectListener implements Listener {
    private static final UUID TOOL_SPEED_MODIFIER = UUID.fromString("2bd8ac2e-3e0e-4f6a-8d9a-6f80b5aa2c01");
    private static final UUID ARMOR_HEALTH_MODIFIER = UUID.fromString("7b47e3ae-b5c5-4fc3-9c3e-2bcb0d104a02");
    private static final UUID[] ARMOR_HEALTH_ITEM_MODIFIERS = {
            UUID.fromString("7b47e3ae-b5c5-4fc3-9c3e-2bcb0d104a11"),
            UUID.fromString("7b47e3ae-b5c5-4fc3-9c3e-2bcb0d104a12"),
            UUID.fromString("7b47e3ae-b5c5-4fc3-9c3e-2bcb0d104a13"),
            UUID.fromString("7b47e3ae-b5c5-4fc3-9c3e-2bcb0d104a14")
    };
    private static final String TOOL_SPEED_NAME = "hyunseorpg_tool_block_speed";
    private static final String ARMOR_HEALTH_NAME = "hyunseorpg_armor_health";
    private final ConfigService config;
    private final EquipmentTierService tiers;
    private final EquipmentEnhancementService enhancement;
    private final EquipmentPromotionService promotion;
    private final CombatService combat;
    private final RPGItemService itemService;
    private final ActivityBlockRewardValidator blockRewards;
    private final ToolDurabilityService toolDurability;

    public EquipmentActualEffectListener(ConfigService config, EquipmentTierService tiers, EquipmentEnhancementService enhancement,
                                          EquipmentPromotionService promotion, CombatService combat,
                                          RPGItemService itemService,
                                          ActivityBlockRewardValidator blockRewards,
                                          ToolDurabilityService toolDurability) {
        this.config = config;
        this.tiers = tiers;
        this.enhancement = enhancement;
        this.promotion = promotion;
        this.combat = combat;
        this.itemService = itemService;
        this.blockRewards = blockRewards;
        this.toolDurability = toolDurability;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (combat.isInternalDamage()) return;
        Player attacker = directAttacker(event.getDamager());
        if (attacker == null) return;
        ItemStack item = attackItem(event.getDamager(), attacker);
        EquipmentTierService.Category category = tiers.getCategory(item);
        if (category == EquipmentTierService.Category.WEAPON
                && !(event.getDamager() instanceof Projectile)) {
            applyOffensiveGrowth(event, item, false);
        } else if (category == EquipmentTierService.Category.TOOL && isAxe(item)
                && !(event.getDamager() instanceof Projectile)) {
            applyOffensiveGrowth(event, item, false);
        } else if (category == EquipmentTierService.Category.SPEAR
                || category == EquipmentTierService.Category.RANGED) {
            applyOffensiveGrowth(event, item, event.getDamager() instanceof Projectile);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIncomingDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || combat.isInternalDamage()) return;
        refreshArmorHealth(player);
        double reduction = 0.0D;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            reduction += enhancement.getDamageReductionBonus(armor);
            reduction += promotion.getOptionValue(armor, "damage-reduction");
        }
        double cap = Math.max(0.0D, Math.min(1.0D,
                config.getDouble("equipment-effects.damage-reduction-cap", 0.8D)));
        reduction = Math.min(cap, Math.max(0.0D, reduction));
        if (reduction > 0.0D) event.setDamage(event.getDamage() * (1.0D - reduction));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        refreshToolEffect(event.getPlayer(), event.getBlock());
        applyToolBonusDrop(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        refreshToolEffect(event.getPlayer(), event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        refreshToolEffect(event.getPlayer(), null);
        refreshArmorHealth(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        refreshToolEffect(event.getPlayer(), null);
        refreshArmorHealth(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        org.bukkit.Bukkit.getScheduler().runTaskLater(config.getPlugin(), () -> {
            refreshToolEffect(event.getPlayer(), null);
            refreshArmorHealth(event.getPlayer());
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        refreshToolEffect(event.getPlayer(), null);
        refreshArmorHealth(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        org.bukkit.Bukkit.getScheduler().runTask(config.getPlugin(), () -> refreshArmorHealth(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        org.bukkit.Bukkit.getScheduler().runTask(config.getPlugin(), () -> refreshArmorHealth(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageIndicator(EntityDamageEvent event) {
        if (event.getFinalDamage() <= 0.0D || event.getEntity().getWorld() == null) return;
        double radius = Math.max(0.0D, config.getDouble("equipment-effects.damage-indicator-radius", 0.65D));
        double minHeight = config.getDouble("equipment-effects.damage-indicator-min-height", 1.0D);
        double maxHeight = Math.max(minHeight,
                config.getDouble("equipment-effects.damage-indicator-max-height", 1.7D));
        ThreadLocalRandom random = ThreadLocalRandom.current();
        var location = event.getEntity().getLocation().add(
                random.nextDouble(-radius, radius),
                random.nextDouble(minHeight, maxHeight),
                random.nextDouble(-radius, radius));
        TextDisplay display = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
        display.text(Component.text("-" + String.format(Locale.ROOT, "%.1f", event.getFinalDamage()), NamedTextColor.RED));
        display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        org.bukkit.Bukkit.getScheduler().runTaskLater(config.getPlugin(), display::remove, 16L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onCustomTridentInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.TRIDENT) return;
        if (isBlockedCustomTrident(item)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCustomTridentLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (isBlockedCustomTrident(trident.getItemStack())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onToolDurabilityDamage(PlayerItemDamageEvent event) {
        ItemStack tool = event.getItem();
        if (tiers.getCategory(tool) != EquipmentTierService.Category.TOOL) return;
        if (toolDurability != null && toolDurability.shouldPreserveNormal(tool)) {
            event.setCancelled(true);
        }
    }

    private void applyOffensiveGrowth(EntityDamageByEntityEvent event, ItemStack item, boolean projectile) {
        if (item == null) return;
        double flat = enhancement.getAttackBonus(item) + promotion.getWeaponDamageBonus(item);
        if (isAxe(item)) flat += enhancement.getAxeAttackBonus(item);
        if (projectile) flat += promotion.getOptionValue(item, "projectile-damage");
        if (tiers.getCategory(item) == EquipmentTierService.Category.SPEAR) {
            flat += promotion.getOptionValue(item, "thrust-damage");
        }
        double multiplier = 1.0D;
        double criticalChance = promotion.getOptionValue(item, "critical-chance");
        if (criticalChance > 0.0D && ThreadLocalRandom.current().nextDouble() < criticalChance) multiplier *= 1.5D;
        event.setDamage(Math.max(0.0D, (event.getDamage() + flat) * multiplier));
    }

    private void refreshToolEffect(Player player, org.bukkit.block.Block targetBlock) {
        AttributeInstance speed = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (speed != null) {
            speed.getModifiers().stream()
                    .filter(modifier -> TOOL_SPEED_NAME.equals(modifier.getName()))
                    .toList()
                    .forEach(speed::removeModifier);
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        removeManagedItemAttribute(held, Attribute.BLOCK_BREAK_SPEED, TOOL_SPEED_NAME, TOOL_SPEED_MODIFIER);
        if (tiers.getCategory(held) != EquipmentTierService.Category.TOOL) return;
        double bonus = enhancement.getToolEfficiencyBonus(held) + promotionToolSpeed(held, targetBlock);
        double perPoint = Math.max(0.0D, config.getDouble("equipment-effects.tool-block-break-speed-per-point", 0.1D));
        updatePlayerAttribute(player, Attribute.BLOCK_BREAK_SPEED, TOOL_SPEED_NAME, TOOL_SPEED_MODIFIER,
                Math.max(0.0D, bonus) * perPoint);
    }

    private double promotionToolSpeed(ItemStack tool, org.bukkit.block.Block targetBlock) {
        String material = tool.getType().name();
        if (material.endsWith("_PICKAXE")) return promotion.getOptionValue(tool, "mining-speed");
        if (material.endsWith("_AXE")) {
            return targetBlock == null || isLog(targetBlock) ? promotion.getOptionValue(tool, "logging-speed") : 0.0D;
        }
        if (material.endsWith("_SHOVEL")) {
            return targetBlock == null || isExcavatable(targetBlock)
                    ? promotion.getOptionValue(tool, "excavation-speed") : 0.0D;
        }
        return 0.0D;
    }

    private void applyToolBonusDrop(BlockBreakEvent event) {
        String blockType = event.getBlock().getType().name();
        String activity = blockType.contains("LOG") || blockType.contains("WOOD")
                || blockType.contains("STEM") || blockType.contains("HYPHAE")
                ? "LOGGING" : "MINING";
        if (!blockRewards.isValidRewardBreak(event, activity)
                || (blockRewards.isCropBlock(event.getBlock())
                && !blockRewards.isMatureAllowedCrop(event.getBlock()))
                || !blockRewards.claimBonusDrop(event)) return;
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (tiers.getCategory(tool) != EquipmentTierService.Category.TOOL) return;
        double chance = Math.max(0.0D, Math.min(1.0D, promotion.getOptionValue(tool, "bonus-drop-chance")));
        if (chance <= 0.0D || ThreadLocalRandom.current().nextDouble() >= chance) return;
        if (!event.getBlock().isPreferredTool(tool)) return;
        for (ItemStack drop : event.getBlock().getDrops(tool, event.getPlayer())) {
            if (drop != null && !drop.getType().isAir() && drop.getAmount() > 0) {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop.clone());
            }
        }
    }

    private boolean isLog(org.bukkit.block.Block block) {
        String type = block.getType().name();
        return type.endsWith("_LOG") || type.endsWith("_WOOD") || type.endsWith("_STEM") || type.endsWith("_HYPHAE");
    }

    private boolean isExcavatable(org.bukkit.block.Block block) {
        String type = block.getType().name();
        return type.contains("SAND") || type.contains("GRAVEL") || type.contains("DIRT") || type.contains("CLAY")
                || type.contains("SNOW") || type.equals("SOUL_SAND") || type.equals("SOUL_SOIL");
    }

    private void refreshArmorHealth(Player player) {
        removeLegacyArmorHealthModifier(player);
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (int index = 0; index < armorContents.length && index < ARMOR_HEALTH_ITEM_MODIFIERS.length; index++) {
            ItemStack armor = armorContents[index];
            double bonus = promotion.getOptionValue(armor, "max-health-bonus");
            updateItemAttribute(armor, Attribute.MAX_HEALTH, ARMOR_HEALTH_NAME,
                    ARMOR_HEALTH_ITEM_MODIFIERS[index], Math.max(0.0D, bonus));
        }
        player.getInventory().setArmorContents(armorContents);

        AttributeInstance health = player.getAttribute(Attribute.MAX_HEALTH);
        if (health == null) return;
        if (player.getHealth() > health.getValue()) player.setHealth(health.getValue());
    }

    private void removeLegacyArmorHealthModifier(Player player) {
        AttributeInstance health = player.getAttribute(Attribute.MAX_HEALTH);
        if (health == null) return;
        health.getModifiers().stream()
                .filter(modifier -> ARMOR_HEALTH_MODIFIER.equals(modifier.getUniqueId())
                        || ARMOR_HEALTH_NAME.equals(modifier.getName()))
                .toList()
                .forEach(health::removeModifier);
    }

    private void updateItemAttribute(ItemStack item, Attribute attribute, String name, UUID uniqueId, double amount) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        var existing = meta.getAttributeModifiers(attribute);
        if (existing != null) {
            existing.stream()
                    .filter(modifier -> uniqueId.equals(modifier.getUniqueId()) || name.equals(modifier.getName()))
                    .toList()
                    .forEach(modifier -> meta.removeAttributeModifier(attribute, modifier));
        }
        if (amount > 0.0D) {
            meta.addAttributeModifier(attribute, new AttributeModifier(uniqueId, name, amount,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
        item.setItemMeta(meta);
    }

    private void removeManagedItemAttribute(ItemStack item, Attribute attribute, String name, UUID uniqueId) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        var existing = meta.getAttributeModifiers(attribute);
        if (existing == null) return;
        boolean changed = false;
        for (AttributeModifier modifier : existing.stream()
                .filter(modifier -> uniqueId.equals(modifier.getUniqueId()) || name.equals(modifier.getName()))
                .toList()) {
            meta.removeAttributeModifier(attribute, modifier);
            changed = true;
        }
        if (changed) item.setItemMeta(meta);
    }

    private void updatePlayerAttribute(Player player, Attribute attribute, String name, UUID uniqueId, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.getModifiers().stream()
                .filter(modifier -> uniqueId.equals(modifier.getUniqueId()) || name.equals(modifier.getName()))
                .toList()
                .forEach(instance::removeModifier);
        if (amount <= 0.0D) return;
        instance.addModifier(new AttributeModifier(uniqueId, name, amount,
                AttributeModifier.Operation.ADD_NUMBER));
    }

    private boolean isCustomRpgItem(ItemStack item) {
        return item != null && item.getType() == Material.TRIDENT && itemService.getItemId(item).isPresent();
    }

    private boolean isBlockedCustomTrident(ItemStack item) {
        return isCustomRpgItem(item) && !itemService.isItem(item, "poseidon_spear");
    }

    private boolean isAxe(ItemStack item) {
        return item != null && item.getType().name().endsWith("_AXE");
    }

    private Player directAttacker(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    private ItemStack attackItem(org.bukkit.entity.Entity damager, Player attacker) {
        if (damager instanceof Trident trident) return trident.getItemStack();
        return attacker.getInventory().getItemInMainHand();
    }
}
