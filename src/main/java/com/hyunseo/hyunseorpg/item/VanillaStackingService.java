package com.hyunseo.hyunseorpg.item;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Arrays;

/** Applies the configured maximum stack size only to approved vanilla items. */
public final class VanillaStackingService implements Listener {
    private static final int DEFAULT_MAX_STACK_SIZE = 16;
    private static final int MAX_ALLOWED_STACK_SIZE = 99;

    private final ConfigService configService;
    private final RPGItemService itemService;

    public VanillaStackingService(ConfigService configService, RPGItemService itemService) {
        this.configService = configService;
        this.itemService = itemService;
    }

    /** Mutates only the max-stack component; item identity and all other meta remain untouched. */
    public ItemStack normalize(ItemStack item) {
        if (!isEligible(item)) return item;
        int max = configuredMax(item.getType());
        if (max <= item.getType().getMaxStackSize()) return item;
        var meta = item.getItemMeta();
        if (meta == null) return item;
        if (!meta.hasMaxStackSize() || meta.getMaxStackSize() != max) {
            meta.setMaxStackSize(max);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Returns the effective capacity used by plugin inventory transactions. */
    public int effectiveMaxStackSize(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;
        if (!isEligible(item)) return Math.max(1, item.getMaxStackSize());
        normalize(item);
        return Math.max(1, Math.min(MAX_ALLOWED_STACK_SIZE, configuredMax(item.getType())));
    }

    public boolean isEligible(ItemStack item) {
        if (!configService.getBoolean("vanilla-stacking.enabled", true)
                || item == null || item.getType().isAir()) return false;
        VanillaStackingPolicy.Group group = VanillaStackingPolicy.group(item.getType());
        if (!groupEnabled(group)) return false;
        if (itemService.getItemId(item).isPresent()) return false;
        if (!isPlainVanillaMeta(item)) return false;
        return true;
    }

    public List<Material> allowedMaterials() {
        return Arrays.stream(Material.values()).filter(VanillaStackingPolicy::isAllowed).toList();
    }

    private boolean isPlainVanillaMeta(ItemStack item) {
        if (!item.hasItemMeta()) return true;
        var meta = item.getItemMeta();
        if (meta == null) return true;
        // Any PDC means another system owns item state. Do not alter or stack it.
        if (!meta.getPersistentDataContainer().getKeys().isEmpty()) return false;
        // Enchanted potion/vehicle variants are not simple vanilla items for this policy.
        if (meta.hasEnchants()) return false;
        return true;
    }

    private boolean groupEnabled(VanillaStackingPolicy.Group group) {
        return switch (group) {
            case POTION -> configService.getBoolean("vanilla-stacking.potion.enabled", true);
            case VEHICLES -> configService.getBoolean("vanilla-stacking.vehicles.enabled", true);
            case UTILITY -> configService.getBoolean("vanilla-stacking.utility.enabled", true)
                    && configuredUtilityMaterial(Material.SADDLE);
            case NONE -> false;
        };
    }

    private boolean configuredUtilityMaterial(Material material) {
        List<String> configured = configService.getStringList("vanilla-stacking.utility.materials");
        if (configured.isEmpty()) return material == Material.SADDLE;
        return configured.stream().map(value -> value.toUpperCase(Locale.ROOT))
                .anyMatch(value -> value.equals(material.name()));
    }

    private int configuredMax(Material material) {
        String overridePath = "vanilla-stacking.overrides." + material.name();
        int override = configService.getPlugin().getConfig().getInt(overridePath, -1);
        int configured;
        if (override > 0) {
            configured = override;
        } else {
            configured = switch (VanillaStackingPolicy.group(material)) {
                case POTION -> configService.getPlugin().getConfig()
                        .getInt("vanilla-stacking.potion.max-stack-size", DEFAULT_MAX_STACK_SIZE);
                case VEHICLES -> configService.getPlugin().getConfig()
                        .getInt("vanilla-stacking.vehicles.max-stack-size", DEFAULT_MAX_STACK_SIZE);
                case UTILITY -> configService.getPlugin().getConfig()
                        .getInt("vanilla-stacking.utility.max-stack-size", DEFAULT_MAX_STACK_SIZE);
                case NONE -> material.getMaxStackSize();
            };
        }
        return Math.max(1, Math.min(MAX_ALLOWED_STACK_SIZE, configured));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        normalizeAndMergeInventory(event.getPlayer().getInventory());
        // The player inventory can receive its persisted contents after the join callback.
        // Re-run once on the next tick so reconnect normalization cannot race that load.
        configService.getPlugin().getServer().getScheduler().runTask(
                configService.getPlugin(),
                () -> normalizeAndMergeInventory(event.getPlayer().getInventory()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        normalizeAndMergeInventory(event.getInventory());
        configService.getPlugin().getServer().getScheduler().runTask(
                configService.getPlugin(),
                () -> normalizeAndMergeInventory(event.getInventory()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        normalize(event.getCurrentItem());
        normalize(event.getCursor());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        normalize(event.getOldCursor());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent event) {
        normalize(event.getItem());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(ItemSpawnEvent event) {
        normalize(event.getEntity().getItemStack());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        normalize(event.getItem().getItemStack());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftPrepare(PrepareItemCraftEvent event) {
        normalize(event.getInventory().getResult());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        event.getResults().forEach(this::normalize);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        normalize(event.getItem());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage().trim();
        if (raw.startsWith("/")) raw = raw.substring(1);
        String root = raw.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (root.contains(":")) root = root.substring(root.indexOf(':') + 1);
        if (!root.equals("give") && !root.equals("item")) return;
        Player player = event.getPlayer();
        configService.getPlugin().getServer().getScheduler().runTask(configService.getPlugin(),
                () -> normalizeAndMergeInventory(player.getInventory()));
    }

    /** Normalizes inventory entries without merging plugin-owned stacks. */
    public void normalizeAndMergeInventory(Inventory inventory) {
        if (inventory == null) return;
        for (ItemStack item : inventory.getContents()) normalize(item);
    }
}
