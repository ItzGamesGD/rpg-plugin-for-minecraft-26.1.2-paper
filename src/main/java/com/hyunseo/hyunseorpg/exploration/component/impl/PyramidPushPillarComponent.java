package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidGridPoint;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidPillarRecoveryPolicy;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomCandidate;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomService;
import com.hyunseo.hyunseorpg.exploration.pyramid.PushPillarBoard;
import com.hyunseo.hyunseorpg.exploration.pyramid.PushPillarDefinition;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Connects the Pyramid-specific logical pillar board to the live runtime. */
public final class PyramidPushPillarComponent implements ExplorationComponent {
    private static final String RETRY_ATTEMPTS = "pyramid.pillar.restore.retry.attempts";
    private static final String RETRY_SCHEDULED = "pyramid.puzzle.restore.retry.scheduled";
    private static final String RETRY_EXHAUSTED = "pyramid.puzzle.restore.retry.exhausted";

    private final PyramidPushPillarService service;
    private final PyramidRoomService rooms;

    public PyramidPushPillarComponent(PyramidPushPillarService service, PyramidRoomService rooms) {
        this.service = service;
        this.rooms = rooms;
    }

    @Override public String type() { return "pyramid_push_pillars"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.PYRAMID_ROOM_REVEAL; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        if (!"desert_pyramid".equals(context.record().structureType())) {
            throw new IllegalArgumentException("pyramid_push_pillars requires desert_pyramid");
        }
        if (Boolean.parseBoolean(context.record().activationMetadata()
                .getOrDefault("pyramid-underground-complete", "false"))) {
            // Persistent module completion is authoritative across restart.
            return;
        }
        if (context.runtime().sequence().flag("pyramid.room.reveal.in_progress")) return;

        // From this point onward a committed room is owned by the pillar runtime. Mark that
        // ownership before any world/session/display lookup so a technical failure cannot make
        // heartbeat route an already-carved room back through pyramid_room_reveal.
        context.runtime().sequence().setFlag("pyramid.puzzle.started");
        context.runtime().sequence().clearFlag(RETRY_SCHEDULED);
        try {
            context.world().orElseThrow(() -> new IllegalStateException("pyramid world is not loaded"));
            PyramidRoomCandidate room = rooms.room(context.runtime().structureId()).orElse(null);
            if (room == null && Boolean.parseBoolean(context.record().activationMetadata()
                    .getOrDefault("pyramid-room-created", "false"))) {
                // prepare() restores a validated committed room session without carving/revealing it.
                room = rooms.prepare(context, spec);
            }
            if (room == null) throw new IllegalStateException("no safe Desert Pyramid puzzle room");

            List<PushPillarDefinition> pillars = parsePillars(spec.options().get("pillars"));
            if (pillars.isEmpty()) throw new IllegalArgumentException("pyramid_push_pillars requires pillars");
            PushPillarBoard board = new PushPillarBoard(pillars, Math.max(0L, spec.integer("cooldown-ticks", 8)),
                    readLogicalPositions(context, pillars));
            service.start(context, spec, room, board, pillars);
        } catch (Exception failure) {
            // The started marker represents an active pillar session, not a failed
            // reservation. Clear it before scheduling recovery so a reload/heartbeat
            // can re-enter the pillar-only restore path if the delayed task is lost.
            context.runtime().sequence().clearFlag("pyramid.puzzle.started");
            int attempt = context.runtime().sequence().incrementCounter(RETRY_ATTEMPTS);
            PyramidPillarRecoveryPolicy.Decision decision = PyramidPillarRecoveryPolicy.afterFailure(attempt);
            if (decision.retry()) {
                String actionId = "pyramid_pillar_restore_retry_" + attempt;
                boolean scheduled = context.sequenceScheduler().schedule(
                        context, actionId, decision.delayTicks(), decision.phase());
                if (scheduled || context.runtime().sequence().actionInFlight(actionId)) {
                    context.runtime().sequence().setFlag(RETRY_SCHEDULED);
                    context.plugin().getLogger().log(java.util.logging.Level.WARNING,
                            "Pyramid pillar restore deferred: structure=" + context.record().structureId()
                                    + ", attempt=" + attempt, failure);
                    return;
                }
            }
            context.runtime().sequence().setFlag(RETRY_EXHAUSTED);
            context.plugin().getLogger().log(java.util.logging.Level.SEVERE,
                    "Pyramid pillar restore exhausted without replaying room reveal: structure="
                            + context.record().structureId() + ", attempt=" + attempt, failure);
            return;
        }

        context.runtime().sequence().clearFlag(RETRY_SCHEDULED);
        context.runtime().sequence().clearFlag(RETRY_EXHAUSTED);
        context.runtime().sequence().setFlag("pyramid.room.ready");
        context.runtime().sequence().setFlag("pyramid.puzzle.ready");
        context.runtime().sequence().setFlag("pyramid.puzzle.active");
        context.runtime().tracker().track(() -> service.stop(context.runtime().structureId()));
    }

    private List<PushPillarDefinition> parsePillars(Object raw) {
        if (!(raw instanceof List<?> values)) return List.of();
        List<PushPillarDefinition> result = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof Map<?, ?> map)) continue;
            String id = text(map.get("id"));
            String symbol = text(map.containsKey("symbol-id") ? map.get("symbol-id") : id);
            String color = text(map.containsKey("color") ? map.get("color") : "sand");
            PyramidGridPoint initial = point(map.get("initial"));
            PyramidGridPoint target = point(map.get("target"));
            Set<PyramidGridPoint> allowed = new LinkedHashSet<>();
            Object rawAllowed = map.get("allowed");
            if (rawAllowed instanceof List<?> cells) {
                for (Object cell : cells) {
                    PyramidGridPoint parsed = point(cell);
                    if (parsed != null) allowed.add(parsed);
                }
            }
            if (!id.isBlank() && initial != null && target != null) {
                result.add(new PushPillarDefinition(id, symbol, color, initial, target, allowed));
            }
        }
        return List.copyOf(result);
    }

    private PyramidGridPoint point(Object raw) {
        if (raw instanceof String text) {
            String[] parts = text.trim().split(",");
            if (parts.length != 2) return null;
            try { return new PyramidGridPoint(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())); }
            catch (NumberFormatException ignored) { return null; }
        }
        if (raw instanceof List<?> values && values.size() >= 2) {
            try { return new PyramidGridPoint(Integer.parseInt(String.valueOf(values.get(0))),
                    Integer.parseInt(String.valueOf(values.get(1)))); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private Map<String, PyramidGridPoint> readLogicalPositions(ExplorationEventContext context,
                                                                 List<PushPillarDefinition> pillars) {
        Map<String, PyramidGridPoint> restored = new java.util.LinkedHashMap<>();
        for (PushPillarDefinition pillar : pillars) {
            String raw = context.record().activationMetadata().get("pyramid-pillar-position-" + pillar.id());
            if (raw == null || raw.isBlank()) continue;
            String[] parts = raw.split(",");
            if (parts.length != 2) throw new IllegalStateException("invalid durable pillar position: " + pillar.id());
            try {
                restored.put(pillar.id(), new PyramidGridPoint(Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim())));
            } catch (NumberFormatException failure) {
                throw new IllegalStateException("invalid durable pillar position: " + pillar.id(), failure);
            }
        }
        return Map.copyOf(restored);
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
}
