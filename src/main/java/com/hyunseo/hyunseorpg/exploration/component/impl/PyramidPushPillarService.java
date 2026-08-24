package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.integration.ExplorationPorts;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidBlockPosition;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidGridDirection;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidGridPoint;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomCandidate;
import com.hyunseo.hyunseorpg.exploration.pyramid.PushPillarBoard;
import com.hyunseo.hyunseorpg.exploration.pyramid.PushPillarDefinition;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
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
    private final Map<UUID, Session> sessions = new LinkedHashMap<>();

    public PyramidPushPillarService(JavaPlugin plugin, ExplorationPorts ports) {
        this.plugin = plugin;
        this.ports = ports;
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
                Location location = session.location(definition.initialPosition());
                Map<String, Object> options = new LinkedHashMap<>(spec.options());
                options.put("material", material(definition.color(), spec.string("display-material", "SANDSTONE")));
                options.put("glowing", true);
                UUID display = ports.displays().spawn("block", location, options);
                if (display != null) session.displays.put(definition.id(), display);
            }
        } catch (RuntimeException exception) {
            for (UUID display : session.displays.values()) ports.displays().remove(display);
            throw exception;
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

    public synchronized void stop(UUID structureId) {
        Session session = sessions.remove(structureId);
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

    private static final class Session {
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
                PushPillarBoard.MoveResult result = board.tryMove(definition.id(), direction, tick);
                if (!result.moved()) continue;
                Location moved = location(result.position());
                UUID display = displays.get(definition.id());
                if (display != null) {
                    portsMove(display, moved);
                }
                player.sendMessage(net.kyori.adventure.text.Component.text(
                        result.solvedNow() ? "피라미드 기둥 하나가 제자리에 놓였습니다." : "피라미드 기둥이 이동했습니다."));
                player.getWorld().spawnParticle(Particle.END_ROD, moved.clone().add(0.0D, 0.8D, 0.0D),
                        8, 0.25D, 0.25D, 0.25D, 0.01D);
                if (result.allSolved()) {
                    runtime.sequence().clearFlag("pyramid.puzzle.active");
                    runtime.sequence().setFlag("pyramid.puzzle.solved");
                    player.sendMessage(net.kyori.adventure.text.Component.text("피라미드의 봉인 장치가 해제되었습니다."));
                }
                return true;
            }
            return false;
        }

        private void portsMove(UUID display, Location location) {
            ports.displays().move(display, location);
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
