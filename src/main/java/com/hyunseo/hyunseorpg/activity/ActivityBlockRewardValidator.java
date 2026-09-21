package com.hyunseo.hyunseorpg.activity;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * Single gate for rewards that originate from block destruction.
 * The event cache prevents one BlockBreakEvent from being rewarded twice when
 * several listeners observe it.
 */
public final class ActivityBlockRewardValidator {
    private final ActivityBlockRepository placedBlocks;
    private final Set<BlockBreakEvent> placedEventCache =
            Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<BlockBreakEvent> claimedBonusEvents =
            Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<BlockBreakEvent> syntheticEvents =
            Collections.newSetFromMap(new WeakHashMap<>());

    public ActivityBlockRewardValidator(ActivityBlockRepository placedBlocks, ConfigService config) {
        this.placedBlocks = placedBlocks;
    }

    public boolean isPlayerPlaced(BlockBreakEvent event) {
        if (event == null) return false;
        synchronized (placedEventCache) {
            if (placedEventCache.contains(event)) return true;
            if (!placedBlocks.isRecorded(event.getBlock())) return false;
            placedEventCache.add(event);
            return true;
        }
    }

    /** Removes the ledger entry once, while keeping the event exclusion cached. */
    public boolean consumePlayerPlaced(BlockBreakEvent event) {
        if (!isPlayerPlaced(event)) return false;
        placedBlocks.remove(event.getBlock());
        return true;
    }

    /** Returns true only when the block may produce an activity reward. */
    public boolean isValidRewardBreak(BlockBreakEvent event, String activity) {
        if (event == null || event.isCancelled() || event.getPlayer() == null || isSynthetic(event)) return false;
        if (isPlayerPlaced(event)) return false;
        Player player = event.getPlayer();
        return player.getGameMode() == GameMode.SURVIVAL;
    }

    /** Crop rewards require a configured crop and a mature growth state. */
    public boolean isMatureAllowedCrop(Block block) {
        if (block == null || !isCrop(block.getType())) return false;
        Set<String> configured = Set.of("WHEAT", "CARROTS", "POTATOES", "BEETROOTS", "NETHER_WART",
                "COCOA", "MELON", "PUMPKIN", "SUGAR_CANE", "CACTUS");
        if (!configured.contains(block.getType().name())) return false;
        return !(block.getBlockData() instanceof Ageable ageable)
                || ageable.getAge() >= ageable.getMaximumAge();
    }

    public boolean isCropBlock(Block block) {
        return block != null && isCrop(block.getType());
    }

    /** Claims the promotion bonus once for this event. */
    public boolean claimBonusDrop(BlockBreakEvent event) {
        synchronized (claimedBonusEvents) {
            return claimedBonusEvents.add(event);
        }
    }

    public void markSynthetic(BlockBreakEvent event) {
        if (event == null) return;
        synchronized (syntheticEvents) {
            syntheticEvents.add(event);
        }
    }

    private boolean isSynthetic(BlockBreakEvent event) {
        synchronized (syntheticEvents) {
            return syntheticEvents.contains(event);
        }
    }

    private boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART, COCOA,
                    MELON, PUMPKIN, SUGAR_CANE, CACTUS -> true;
            default -> false;
        };
    }
}
