package com.hyunseo.hyunseorpg.progression;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.activity.ActivityBlockRewardValidator;
import com.hyunseo.hyunseorpg.farming.CropHarvestValidator;
import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Rolls the early access material from ordinary vanilla activities. */
public final class MagicStoneFragmentService implements Listener {
    private final ConfigService config;
    private final RPGItemService itemService;
    private final InventoryDeliveryService delivery;
    private final ActivityBlockRewardValidator blockRewards;
    private final CropHarvestValidator customCrops;

    public MagicStoneFragmentService(ConfigService config, RPGItemService itemService,
                                     InventoryDeliveryService delivery,
                                     ActivityBlockRewardValidator blockRewards,
                                     CropHarvestValidator customCrops) {
        this.config = config;
        this.itemService = itemService;
        this.delivery = delivery;
        this.blockRewards = blockRewards;
        this.customCrops = customCrops;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (customCrops.isRegisteredCrop(event.getBlock())) return;
        Material material = event.getBlock().getType();
        if ((isOre(material) || isStone(material))
                && blockRewards.isValidRewardBreak(event, "MINING")) {
            roll(event.getPlayer(), "MINING", event.getBlock().getLocation());
        } else if (isLog(material)
                && blockRewards.isValidRewardBreak(event, "LOGGING")) {
            roll(event.getPlayer(), "LOGGING", event.getBlock().getLocation());
        } else if (isCrop(material)
                && blockRewards.isMatureAllowedCrop(event.getBlock())
                && blockRewards.isValidRewardBreak(event, "FARMING")) {
            roll(event.getPlayer(), "FARMING", event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            roll(event.getPlayer(), "FISHING", event.getPlayer().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null || entity instanceof Wither || entity instanceof EnderDragon) return;
        roll(killer, "HUNTING", entity.getLocation());
    }

    public void roll(Player player, String source, Location location) {
        if (player == null || source == null || !config.getProgressionLoopBoolean("magic-stone-fragments.enabled", true)) return;
        String key = source.toUpperCase(Locale.ROOT);
        double chance = Math.max(0.0D, Math.min(1.0D,
                config.getProgressionLoopDouble("magic-stone-fragments.sources." + key + ".chance", 0.05D)));
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;
        int min = Math.max(1, config.getProgressionLoopInt("magic-stone-fragments.sources." + key + ".min-amount", 1));
        int max = Math.max(min, config.getProgressionLoopInt("magic-stone-fragments.sources." + key + ".max-amount", min));
        int amount = ThreadLocalRandom.current().nextInt(min, max + 1);
        String itemId = config.getProgressionLoopString("magic-stone-fragments.item-id", "magic_stone_fragment");
        ItemStack item = itemService.create(itemId, amount).orElse(null);
        if (item == null) {
            config.getPlugin().getLogger().warning("Magic stone fragment item is not registered: " + itemId);
            return;
        }
        delivery.giveOrDiscard(player, item);
        player.sendActionBar(Component.text("마석 파편 +" + amount, NamedTextColor.LIGHT_PURPLE));
    }

    private boolean isOre(Material material) { return material.name().endsWith("_ORE") || material == Material.ANCIENT_DEBRIS; }
    private boolean isStone(Material material) { return switch (material) {
        case STONE, TUFF, ANDESITE, DIORITE, GRANITE, DEEPSLATE, CALCITE, DRIPSTONE_BLOCK,
             SMOOTH_BASALT, COBBLESTONE, MOSSY_COBBLESTONE -> true;
        default -> false;
    }; }
    private boolean isLog(Material material) { return material.name().endsWith("_LOG") || material.name().endsWith("_STEM"); }
    private boolean isCrop(Material material) { return switch (material) {
        case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART, COCOA, MELON, PUMPKIN,
             SUGAR_CANE, CACTUS -> true;
        default -> material.name().endsWith("_BUSH");
    }; }
}
