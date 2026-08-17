package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public final class VanillaEnchantBlockListener implements Listener {
    private final ConfigService configService;

    public VanillaEnchantBlockListener(ConfigService configService) {
        this.configService = configService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        if (isBlockingEnabled() && configService.getBoolean("vanilla-enchants.block-enchanting-table", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        if (isBlockingEnabled() && configService.getBoolean("vanilla-enchants.block-enchanting-table", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!isBlockingEnabled() || !configService.getBoolean("vanilla-enchants.block-anvil-enchants", true)
                || event.getResult() == null) {
            return;
        }

        Inventory inventory = event.getInventory();
        ItemStack base = inventory.getItem(0);
        ItemStack result = event.getResult();
        if (hasNewOrIncreasedEnchantments(base, result)) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isBlockingEnabled() || !configService.getBoolean("vanilla-enchants.block-anvil-enchants", true)) return;
        if (event.getView().getTopInventory().getType() == org.bukkit.event.inventory.InventoryType.ANVIL
                && (isVanillaEnchanted(event.getCurrentItem())
                || isVanillaEnchanted(event.getCursor())
                || isVanillaEnchanted(event.getView().getTopInventory().getItem(0))
                || isVanillaEnchanted(event.getView().getTopInventory().getItem(1)))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobDrop(EntityDeathEvent event) {
        if (!isBlockingEnabled() || !configService.getBoolean("vanilla-enchants.strip-mob-drop-enchants", true)) return;
        event.getDrops().removeIf(this::isVanillaEnchanted);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLoot(LootGenerateEvent event) {
        if (!isBlockingEnabled() || !configService.getBoolean("vanilla-enchants.strip-loot-enchants", true)) return;
        event.getLoot().removeIf(this::isVanillaEnchanted);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!isBlockingEnabled() || !configService.getBoolean("vanilla-enchants.strip-fishing-enchants", true)) return;
        if (event.getCaught() instanceof Item item && isVanillaEnchanted(item.getItemStack())) {
            item.setItemStack(stripVanillaEnchantments(item.getItemStack()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!isBlockingEnabled()) {
            return;
        }

        String command = event.getMessage().trim().toLowerCase();
        String commandName = command.split("\\s+", 2)[0];
        if (commandName.equals("/enchant") || commandName.equals("/minecraft:enchant")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("HyunseoRPG 강화 시스템 준비를 위해 바닐라 인챈트 명령어는 사용할 수 없습니다.");
        }
    }

    private boolean hasNewOrIncreasedEnchantments(ItemStack base, ItemStack result) {
        Map<Enchantment, Integer> baseEnchantments = getAllEnchantments(base);
        Map<Enchantment, Integer> resultEnchantments = getAllEnchantments(result);

        for (Map.Entry<Enchantment, Integer> entry : resultEnchantments.entrySet()) {
            int baseLevel = baseEnchantments.getOrDefault(entry.getKey(), 0);
            if (entry.getValue() > baseLevel) {
                return true;
            }
        }
        return false;
    }

    private Map<Enchantment, Integer> getAllEnchantments(ItemStack itemStack) {
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return enchantments;
        }

        ItemMeta meta = itemStack.getItemMeta();
        enchantments.putAll(meta.getEnchants());
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            enchantments.putAll(storageMeta.getStoredEnchants());
        }
        return enchantments;
    }

    private boolean isVanillaEnchanted(ItemStack item) {
        return !getAllEnchantments(item).isEmpty();
    }

    private ItemStack stripVanillaEnchantments(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        for (Enchantment enchantment : meta.getEnchants().keySet()) meta.removeEnchant(enchantment);
        if (meta instanceof EnchantmentStorageMeta storage) {
            for (Enchantment enchantment : storage.getStoredEnchants().keySet()) storage.removeStoredEnchant(enchantment);
        }
        copy.setItemMeta(meta);
        return copy;
    }

    private boolean isBlockingEnabled() {
        if (configService.getBoolean("vanilla-enchants.enabled", false)) return false;
        return configService.getBoolean("enhancement.block-vanilla-enchanting", true);
    }
}
