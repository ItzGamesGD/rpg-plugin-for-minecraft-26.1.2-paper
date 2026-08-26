package com.hyunseo.hyunseorpg.exploration.integration;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import com.hyunseo.hyunseorpg.exploration.model.StructureBounds;

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
        PuzzlePort puzzles,
        StructureEntityCleanupPort entityCleanup
) {
    public ExplorationPorts {
        mobs = mobs == null ? MobSpawnPort.NOOP : mobs;
        displays = displays == null ? DisplayPort.NOOP : displays;
        interactions = interactions == null ? InteractionPort.NOOP : interactions;
        worldMutations = worldMutations == null ? WorldMutationPort.NOOP : worldMutations;
        teleports = teleports == null ? TeleportPort.NOOP : teleports;
        rewards = rewards == null ? RewardPort.NOOP : rewards;
        puzzles = puzzles == null ? PuzzlePort.NOOP : puzzles;
        entityCleanup = entityCleanup == null ? StructureEntityCleanupPort.NOOP : entityCleanup;
    }

    public ExplorationPorts(MobSpawnPort mobs, DisplayPort displays, InteractionPort interactions,
                            WorldMutationPort worldMutations, TeleportPort teleports,
                            RewardPort rewards, PuzzlePort puzzles) {
        this(mobs, displays, interactions, worldMutations, teleports, rewards, puzzles, null);
    }

    public static ExplorationPorts noOp() { return new ExplorationPorts(null, null, null, null, null, null, null); }

    @FunctionalInterface
    public interface MobSpawnPort {
        MobSpawnPort NOOP = (mobId, location, count, options) -> List.of();
        Collection<UUID> spawn(String mobId, Location location, int count, Map<String, Object> options);

        default SpawnDetails describe(UUID entityId) {
            return new SpawnDetails(entityId, "UNKNOWN", "", "", "");
        }

        record SpawnDetails(UUID entityId, String entityType, String rpgMobId,
                            String customMobId, String spawnSource) { }
    }

    @FunctionalInterface
    public interface DisplayPort {
        DisplayPort NOOP = (kind, location, options) -> null;
        UUID spawn(String kind, Location location, Map<String, Object> options);
        default boolean move(UUID entityId, Location location) { return false; }
        default boolean remove(UUID entityId) { return false; }
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

        /** Durable idempotent delivery hook used by completion transactions. */
        default boolean enqueueDurable(Player player, String rewardId, int amount, Location fallback,
                                       Map<String, Object> options, String idempotencyToken) {
            return grant(player, rewardId, amount, fallback, options);
        }
    }

    @FunctionalInterface
    public interface PuzzlePort {
        PuzzlePort NOOP = (puzzleId, location, options) -> () -> { };
        Runnable start(String puzzleId, Location location, Map<String, Object> options);
    }

    @FunctionalInterface
    public interface StructureEntityCleanupPort {
        StructureEntityCleanupPort NOOP = (world, bounds, entityType) -> 0;
        int removeUnmanaged(World world, StructureBounds bounds, EntityType entityType);
    }
}
