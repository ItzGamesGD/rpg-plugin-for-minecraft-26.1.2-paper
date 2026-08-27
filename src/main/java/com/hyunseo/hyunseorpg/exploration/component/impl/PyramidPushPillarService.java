package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.integration.ExplorationPorts;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidBlockPosition;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidGridDirection;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidGridPoint;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomCandidate;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidCompletionRetryPolicy;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidUndergroundCompletionState;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidUndergroundCompletionCoordinator;
import com.hyunseo.hyunseorpg.exploration.pyramid.PushPillarBoard;
import com.hyunseo.hyunseorpg.exploration.pyramid.PushPillarDefinition;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import com.hyunseo.hyunseorpg.exploration.persistence.StructureRepository;
import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntimeManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Live contact/display adapter for the Pyramid push-pillar board. */
public final class PyramidPushPillarService {
    private final JavaPlugin plugin;
    private final ExplorationPorts ports;
    private final StructureRepository repository;
    private final Map<UUID, Session> sessions = new LinkedHashMap<>();
    private final Map<UUID, Integer> completionRetryAttempts = new LinkedHashMap<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> completionRetryTasks = new LinkedHashMap<>();

    public PyramidPushPillarService(JavaPlugin plugin, ExplorationPorts ports) {
        this(plugin, ports, null);
    }

    public PyramidPushPillarService(JavaPlugin plugin, ExplorationPorts ports, StructureRepository repository) {
        this.plugin = plugin;
        this.ports = ports;
        this.repository = repository;
    }

    public synchronized void start(ExplorationEventContext context, ExplorationComponentSpec spec,
                                   PyramidRoomCandidate room, PushPillarBoard board,
                                   List<PushPillarDefinition> definitions) {
        stop(context.runtime().structureId());
        Session session = new Session(context.runtime().structureId(), context.runtime(),
                context.world().orElseThrow(), room.origin(), room.orientation(), board, definitions,
                ports,
                Math.max(1.0D, spec.decimal("contact-radius", 1.65D)),
                Math.max(0.25D, spec.decimal("grid-scale", 1.0D)),
                Math.max(0, spec.integer("display-y-offset", 0)));
        try {
            for (PushPillarDefinition definition : definitions) {
                Location location = session.location(board.currentPosition(definition.id()).orElse(definition.initialPosition()));
                Map<String, Object> options = new LinkedHashMap<>(spec.options());
                options.put("material", material(definition.color(), spec.string("display-material", "SANDSTONE")));
                options.put("glowing", true);
                UUID display = ports.displays().spawn("block", location, options);
                if (display != null) session.displays.put(definition.id(), display);
            }
        } catch (RuntimeException exception) {
            cleanupDisplays(session);
            throw exception;
        }
        if (session.displays.size() != definitions.size()) {
            int expected = definitions.size();
            int spawned = session.displays.size();
            cleanupDisplays(session);
            plugin.getLogger().warning("Pyramid pillars failed: structure=" + session.structureId
                    + ", expected=" + expected + ", spawned=" + spawned);
            throw new IllegalStateException("Pyramid pillar display spawn incomplete: expected="
                    + expected + ", spawned=" + spawned);
        }
        sessions.put(session.structureId, session);
        plugin.getLogger().info("Desert Pyramid push-pillar puzzle activated: structure="
                + session.structureId + ", pillars=" + definitions.size());
    }

    public synchronized void onMove(Player player, Location from, Location to, long currentTick) {
        if (player == null || from == null || to == null || from.getWorld() == null || to.getWorld() == null
                || !from.getWorld().getUID().equals(to.getWorld().getUID())) return;
        for (Session session : List.copyOf(sessions.values())) {
            if (!session.runtime.participants().contains(player.getUniqueId())
                    || !session.world.getUID().equals(to.getWorld().getUID())) continue;
            if (session.tryPush(player, from, to, currentTick)) return;
        }
    }

    private void cleanupDisplays(Session session) {
        for (UUID display : session.displays.values()) {
            try { ports.displays().remove(display); }
            catch (RuntimeException ignored) { }
        }
        session.displays.clear();
    }

    /**
     * Durably records the solved intent before the final board mutation.
     * A failed first save leaves the board movable, so restart never loses
     * evidence of an already-solved board.
     */
    private boolean persistSolvedIntentAndLogicalPosition(UUID structureId, String pillarId,
                                                             PyramidGridPoint position) {
        if (repository == null) return true;
        try {
            return PyramidUndergroundCompletionCoordinator.persistSolvedIntentAndLogicalPosition(
                    repository, structureId, pillarId, position);
        } catch (java.io.IOException | RuntimeException failure) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Pyramid final pillar move deferred until solved intent and logical position are durable: structure="
                            + structureId, failure);
            return false;
        }
    }

    private boolean persistLogicalPosition(UUID structureId, String pillarId,
                                           PyramidGridPoint position, boolean solved) {
        if (repository == null || position == null) return repository == null;
        try {
            var latest = repository.get(structureId).orElse(null);
            if (latest == null) return false;
            var next = latest.withMetadata("pyramid-pillar-position-" + pillarId,
                            position.x() + "," + position.z())
                    .withMetadata("pyramid-pillar-solved-" + pillarId, Boolean.toString(solved));
            repository.save(next);
            return true;
        } catch (java.io.IOException | RuntimeException failure) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Pyramid logical pillar state was not persisted: structure=" + structureId, failure);
            return false;
        }
    }

    private synchronized void persistUndergroundCompletion(UUID structureId,
                                                           com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntime runtime) {
        if (repository == null) return;
        try {
            var record = PyramidUndergroundCompletionCoordinator.complete(repository, structureId);
            if (record == null) return;
            completionRetryAttempts.remove(structureId);
            runtime.sequence().setFlag("pyramid.underground.complete");
            org.bukkit.scheduler.BukkitTask task = completionRetryTasks.remove(structureId);
            if (task != null) task.cancel();
            runtime.sequence().clearFlag("pyramid.underground.persistence.retry");
        } catch (java.io.IOException | RuntimeException failure) {
            runtime.sequence().setFlag("pyramid.underground.persistence.retry");
            int attempt = completionRetryAttempts.getOrDefault(structureId, 0) + 1;
            completionRetryAttempts.put(structureId, attempt);
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Pyramid underground completion persistence retryable: structure=" + structureId
                            + ", attempt=" + attempt, failure);
            if (PyramidCompletionRetryPolicy.shouldRetry(attempt) && !completionRetryTasks.containsKey(structureId)) {
                org.bukkit.scheduler.BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> {
                            synchronized (PyramidPushPillarService.this) {
                                completionRetryTasks.remove(structureId);
                                persistUndergroundCompletion(structureId, runtime);
                            }
                        }, PyramidCompletionRetryPolicy.delayTicks(attempt));
                completionRetryTasks.put(structureId, task);
                runtime.tracker().track(task::cancel);
            }
        }
    }

    public synchronized void markRecoveryRequired(ExplorationEventContext context, String reason) {
        if (context == null) return;
        UUID structureId = context.runtime().structureId();
        try {
            if (repository != null) {
                var latest = repository.get(structureId).orElse(context.record());
                repository.save(latest.withMetadata("pyramid-failure-state", "RECOVERY_REQUIRED")
                        .withMetadata("pyramid-failure-reason", reason == null ? "pillar-state-invalid" : reason));
            }
        } catch (Exception ignored) { }
        context.runtime().sequence().cancelPendingTasks();
        stop(structureId);
        plugin.getLogger().severe("Pyramid progression frozen: recovery required for structure=" + structureId
                + " (" + (reason == null ? "pillar-state-invalid" : reason) + ")");
    }

    public synchronized void stop(UUID structureId) {
        Session session = sessions.remove(structureId);
        org.bukkit.scheduler.BukkitTask retry = completionRetryTasks.remove(structureId);
        if (retry != null) retry.cancel();
        completionRetryAttempts.remove(structureId);
        if (session == null) return;
        for (UUID display : session.displays.values()) ports.displays().remove(display);
    }

    public synchronized void stopAll() {
        for (UUID structureId : List.copyOf(sessions.keySet())) stop(structureId);
    }

    private String material(String color, String fallback) {
        return switch (color.toLowerCase(java.util.Locale.ROOT)) {
            case "red", "sun" -> "RED_SANDSTONE";
            case "blue", "moon" -> "BLUE_ICE";
            case "green", "emerald" -> "EMERALD_BLOCK";
            case "purple", "amethyst" -> "PURPLE_WOOL";
            default -> fallback;
        };
    }

    private final class Session {
        private final UUID structureId;
        private final com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntime runtime;
        private final org.bukkit.World world;
        private final PyramidBlockPosition origin;
        private final com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomOrientation orientation;
        private final PushPillarBoard board;
        private final List<PushPillarDefinition> definitions;
        private final double contactRadius;
        private final double gridScale;
        private final int displayYOffset;
        private final Map<String, UUID> displays = new LinkedHashMap<>();
        private final ExplorationPorts ports;

        private Session(UUID structureId, com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntime runtime,
                        org.bukkit.World world, PyramidBlockPosition origin,
                        com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomOrientation orientation,
                        PushPillarBoard board, List<PushPillarDefinition> definitions,
                        ExplorationPorts ports,
                        double contactRadius, double gridScale, int displayYOffset) {
            this.structureId = structureId;
            this.runtime = runtime;
            this.world = world;
            this.origin = origin;
            this.orientation = orientation;
            this.board = board;
            this.definitions = definitions;
            this.ports = ports;
            this.contactRadius = contactRadius;
            this.gridScale = gridScale;
            this.displayYOffset = displayYOffset;
        }

        private boolean tryPush(Player player, Location from, Location to, long tick) {
            double moveX = to.getX() - from.getX();
            double moveZ = to.getZ() - from.getZ();
            if (Math.abs(moveX) + Math.abs(moveZ) < 0.001D) return false;
            for (PushPillarDefinition definition : definitions) {
                PyramidGridPoint current = board.currentPosition(definition.id()).orElse(null);
                if (current == null) continue;
                Location pillar = location(current);
                if (pillar.getWorld() == null || pillar.distanceSquared(to) > contactRadius * contactRadius) continue;
                PyramidGridDirection direction = direction(moveX, moveZ);
                double towardX = pillar.getX() - from.getX();
                double towardZ = pillar.getZ() - from.getZ();
                if (moveX * towardX + moveZ * towardZ <= 0.0D) continue;
                // The final move commits solved intent and its logical position in one
                // durable snapshot before changing the in-memory board. This prevents a
                // pending marker from surviving a later logical-position save failure.
                boolean finalMove = board.wouldCompleteMove(definition.id(), direction, tick);
                PyramidGridPoint previous = board.currentPosition(definition.id()).orElse(null);
                PyramidGridPoint destination = previous == null ? null : previous.translate(direction);
                if (finalMove && !persistSolvedIntentAndLogicalPosition(structureId, definition.id(), destination)) {
                    player.sendMessage(net.kyori.adventure.text.Component.text(
                            "피라미드 장치가 잠시 불안정합니다. 잠시 후 다시 시도하십시오."));
                    return true;
                }
                PushPillarBoard.MoveResult result = board.tryMove(definition.id(), direction, tick);
                if (!result.moved()) continue;
                if (!finalMove && !persistLogicalPosition(structureId, definition.id(), result.position(), result.solvedNow())) {
                    board.rollbackMove(definition.id(), previous);
                    player.sendMessage(net.kyori.adventure.text.Component.text(
                            "피라미드 장치가 잠시 불안정합니다. 이동이 저장되지 않았습니다."));
                    return true;
                }
                Location moved = location(result.position());
                UUID display = displays.get(definition.id());
                try {
                    if (display == null) throw new IllegalStateException("missing pillar display: " + definition.id());
                    portsMove(display, moved);
                } catch (RuntimeException corruption) {
                    failClosedRepresentation(corruption);
                    player.sendMessage(net.kyori.adventure.text.Component.text(
                            "피라미드 장치가 손상되어 진행을 중지했습니다. 관리자에게 초기화를 요청하십시오."));
                    return true;
                }
                player.sendMessage(net.kyori.adventure.text.Component.text(
                        result.solvedNow() ? "피라미드 기둥 하나가 제자리에 놓였습니다." : "피라미드 기둥이 이동했습니다."));
                player.getWorld().spawnParticle(Particle.END_ROD, moved.clone().add(0.0D, 0.8D, 0.0D),
                        8, 0.25D, 0.25D, 0.25D, 0.01D);
                if (result.allSolved()) {
                    runtime.sequence().clearFlag("pyramid.puzzle.active");
                    runtime.sequence().setFlag("pyramid.puzzle.solved");
                    persistUndergroundCompletion(structureId, runtime);
                    player.sendMessage(net.kyori.adventure.text.Component.text("피라미드의 봉인 장치가 해제되었습니다."));
                }
                return true;
            }
            return false;
        }

        private void portsMove(UUID display, Location location) {
            if (!ports.displays().move(display, location)) {
                throw new IllegalStateException("pillar-display-missing-or-invalid");
            }
        }

        private void failClosedRepresentation(RuntimeException failure) {
            try {
                var latest = repository == null ? null : repository.get(structureId).orElse(null);
                if (latest != null) {
                    repository.save(latest.withMetadata("pyramid-failure-state", "RECOVERY_REQUIRED")
                            .withMetadata("pyramid-failure-reason", "pillar-display-missing-or-invalid"));
                }
            } catch (Exception ignored) { }
            runtime.sequence().cancelPendingTasks();
            PyramidPushPillarService.this.stop(structureId);
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Pyramid pillar representation failed closed: structure=" + structureId, failure);
        }

        private Location location(PyramidGridPoint point) {
            PyramidBlockPosition resolved = orientation.resolve(origin, (int) Math.round(point.x() * gridScale),
                    displayYOffset, (int) Math.round(point.z() * gridScale));
            return new Location(world, resolved.x() + 0.5D, resolved.y(), resolved.z() + 0.5D);
        }

        private PyramidGridDirection direction(double x, double z) {
            if (Math.abs(x) >= Math.abs(z)) return x >= 0 ? PyramidGridDirection.EAST : PyramidGridDirection.WEST;
            return z >= 0 ? PyramidGridDirection.SOUTH : PyramidGridDirection.NORTH;
        }
    }
}
