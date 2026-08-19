package com.hyunseo.hyunseorpg.exploration.integration;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Boundary to the rest of HyunseoRPG. The exploration core never imports farming/alchemy/mob/reward
 * implementations directly. Latest-source integration replaces only these ports.
 */
public record ExplorationPorts(
        MobSpawnPort mobs,
        DisplayPort displays,
        InteractionPort interactions,
        WorldMutationPort worldMutations,
        TeleportPort teleports,
        RewardPort rewards,
        PuzzlePort puzzles
) {
    public ExplorationPorts {
        mobs = mobs == null ? MobSpawnPort.NOOP : mobs;
        displays = displays == null ? DisplayPort.NOOP : displays;
        interactions = interactions == null ? InteractionPort.NOOP : interactions;
        worldMutations = worldMutations == null ? WorldMutationPort.NOOP : worldMutations;
        teleports = teleports == null ? TeleportPort.NOOP : teleports;
        rewards = rewards == null ? RewardPort.NOOP : rewards;
        puzzles = puzzles == null ? PuzzlePort.NOOP : puzzles;
    }

    public static ExplorationPorts noOp() { return new ExplorationPorts(null, null, null, null, null, null, null); }

    @FunctionalInterface
    public interface MobSpawnPort {
        MobSpawnPort NOOP = (mobId, location, count, options) -> List.of();
        Collection<UUID> spawn(String mobId, Location location, int count, Map<String, Object> options);
    }

    @FunctionalInterface
    public interface DisplayPort {
        DisplayPort NOOP = (kind, location, options) -> null;
        UUID spawn(String kind, Location location, Map<String, Object> options);
    }

    @FunctionalInterface
    public interface InteractionPort {
        InteractionPort NOOP = (interactionId, location, options) -> null;
        UUID spawn(String interactionId, Location location, Map<String, Object> options);
    }

    @FunctionalInterface
    public interface WorldMutationPort {
        WorldMutationPort NOOP = (location, options) -> () -> { };
        Runnable applyTemporary(Location location, Map<String, Object> options);
    }

    @FunctionalInterface
    public interface TeleportPort {
        TeleportPort NOOP = (player, location, options) -> false;
        boolean teleport(Player player, Location location, Map<String, Object> options);
    }

    @FunctionalInterface
    public interface RewardPort {
        RewardPort NOOP = (player, rewardId, amount, fallback, options) -> false;
        boolean grant(Player player, String rewardId, int amount, Location fallback, Map<String, Object> options);
    }

    @FunctionalInterface
    public interface PuzzlePort {
        PuzzlePort NOOP = (puzzleId, location, options) -> () -> { };
        Runnable start(String puzzleId, Location location, Map<String, Object> options);
    }
}
