package com.hyunseo.hyunseorpg.activity;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Handles mining safety and routes valid mining to the shared activity reward loop. */
public final class MiningActivityService {
    private static final Set<Material> TARGET_ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS);
    private static final Set<Material> DEFAULT_STONE_BLOCKS = EnumSet.of(
            Material.STONE, Material.TUFF, Material.ANDESITE, Material.DIORITE,
            Material.GRANITE, Material.DEEPSLATE, Material.CALCITE,
            Material.DRIPSTONE_BLOCK, Material.SMOOTH_BASALT,
            Material.COBBLESTONE, Material.MOSSY_COBBLESTONE,
            Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS,
            Material.CRACKED_STONE_BRICKS, Material.CHISELED_STONE_BRICKS);

    private final ConfigService config;
    private final ActivityBlockRepository placedBlocks;
    private final ActivityBlockRewardValidator blockRewards;

    public MiningActivityService(ConfigService config, ActivityBlockRepository placedBlocks,
                                 ActivityBlockRewardValidator blockRewards) {
        this.config = config;
        this.placedBlocks = placedBlocks;
        this.blockRewards = blockRewards;
    }

    public void handleBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (blockRewards.isPlayerPlaced(event)) {
            blockRewards.consumePlayerPlaced(event);
            return;
        }
        if (!blockRewards.isValidRewardBreak(event, "MINING")) return;
        if (!isEligible(player, block)) return;
    }

    /** Compatibility entry point for non-event callers. */
    public void handleBreak(Player player, Block block) {
        if (placedBlocks.isRecorded(block)) {
            placedBlocks.remove(block);
            return;
        }
        if (!isEligible(player, block)) return;
    }

    public void recordPlacement(Block block) {
        if (isMiningBlock(block)) placedBlocks.record(block);
    }

    public boolean isTargetOre(Material material) {
        return TARGET_ORES.contains(material);
    }

    public boolean isMiningBlock(Block block) {
        return block != null && (isTargetOre(block.getType()) || isStoneBlock(block.getType()));
    }

    public boolean isEligible(Player player, Block block) {
        if (player == null || block == null || !isMiningBlock(block)) return false;
        if (isStoneBlock(block.getType()) && block.getWorld().getEnvironment() != World.Environment.NORMAL) return false;
        if (player.getGameMode() != GameMode.SURVIVAL) return false;
        if (!allowedWorld(player.getWorld().getName())) return false;
        return isPickaxe(player.getInventory().getItemInMainHand());
    }

    private boolean allowedWorld(String worldName) { return true; }

    private boolean isStoneBlock(Material material) { return DEFAULT_STONE_BLOCKS.contains(material); }

    private boolean isPickaxe(ItemStack item) {
        return item != null && item.getType().name().endsWith("_PICKAXE");
    }
}
