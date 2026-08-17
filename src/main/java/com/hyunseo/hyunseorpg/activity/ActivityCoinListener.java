package com.hyunseo.hyunseorpg.activity;

import com.hyunseo.hyunseorpg.core.event.RPGMobKillEvent;
import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.mob.MobTagService;
import com.hyunseo.hyunseorpg.activity.ActivityBlockRepository;
import com.hyunseo.hyunseorpg.progression.ProgressionAccessRewardService;
import com.hyunseo.hyunseorpg.farming.CropHarvestValidator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.Locale;

/** Converts ordinary vanilla activity events into the shared coin reward path. */
public final class ActivityCoinListener implements Listener {
    private final ActivityCoinRewardService rewards;
    private final ProgressionAccessRewardService accessRewards;
    private final ActivityBlockRepository placedBlocks;
    private final ActivityBlockRewardValidator blockRewards;
    private final CropHarvestValidator customCrops;
    private final ConfigService config;

    public ActivityCoinListener(ActivityCoinRewardService rewards,
                                ProgressionAccessRewardService accessRewards,
                                ActivityBlockRepository placedBlocks,
                                ConfigService config,
                                ActivityBlockRewardValidator blockRewards,
                                CropHarvestValidator customCrops) {
        this.rewards = rewards;
        this.accessRewards = accessRewards;
        this.placedBlocks = placedBlocks;
        this.config = config;
        this.blockRewards = blockRewards;
        this.customCrops = customCrops;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (customCrops.isRegisteredCrop(block)) return;
        if (!isLog(block.getType()) && !isCrop(block.getType())) return;
        if (blockRewards.isPlayerPlaced(event)) {
            blockRewards.consumePlayerPlaced(event);
            return;
        }
        if (isCrop(block.getType()) && !blockRewards.isMatureAllowedCrop(block)) return;
        ActivityType activity = isLog(block.getType()) ? ActivityType.LOGGING : ActivityType.FARMING;
        if (!blockRewards.isValidRewardBreak(event, activity.name())) return;
        accessRewards.roll(event.getPlayer(), activity, block.getLocation());
        rewards.reward(event.getPlayer(), activity, block.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (isLog(block.getType()) || isCrop(block.getType())) placedBlocks.record(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Location location = event.getPlayer().getLocation();
        accessRewards.roll(event.getPlayer(), ActivityType.FISHING, location);
        rewards.rewardVariant(event.getPlayer(), ActivityType.FISHING, location, fishingTier(event));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player) || !(event.getEntity() instanceof Animals)) return;
        accessRewards.roll(player, ActivityType.HUSBANDRY, player.getLocation());
        rewards.rewardVariant(player, ActivityType.HUSBANDRY, player.getLocation(), "BREED");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPassiveAnimalDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Animals animal)) return;
        if (animal.getScoreboardTags().contains(MobTagService.RPG_MOB_TAG)) return;
        Player killer = animal.getKiller();
        if (killer == null) return;
        accessRewards.roll(killer, ActivityType.HUSBANDRY, animal.getLocation());
        rewards.rewardVariant(killer, ActivityType.HUSBANDRY, animal.getLocation(), "SLAUGHTER");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRpgMobKill(RPGMobKillEvent event) {
        // MobRewardService already pays the mob-specific coinReward. Do not add
        // the generic HUNTING amount here, or custom mob values are doubled.
    }

    private boolean isLog(Material material) {
        return material.name().endsWith("_LOG") || material.name().endsWith("_STEM")
                || material == Material.CRIMSON_HYPHAE || material == Material.WARPED_HYPHAE;
    }

    private boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART, COCOA,
                    MELON, PUMPKIN, SUGAR_CANE, CACTUS -> true;
            default -> material.name().toLowerCase(Locale.ROOT).endsWith("_BUSH");
        };
    }

    private String fishingTier(PlayerFishEvent event) {
        if (!(event.getCaught() instanceof Item item)) return "FISH";
        Material material = item.getItemStack().getType();
        if (material == Material.ENCHANTED_BOOK || material == Material.NAME_TAG
                || material == Material.SADDLE || material == Material.NAUTILUS_SHELL
                || material == Material.BOW || material == Material.FISHING_ROD) {
            return "TREASURE";
        }
        if (material == Material.COD || material == Material.SALMON
                || material == Material.TROPICAL_FISH || material == Material.PUFFERFISH) {
            return "FISH";
        }
        return "JUNK";
    }
}
