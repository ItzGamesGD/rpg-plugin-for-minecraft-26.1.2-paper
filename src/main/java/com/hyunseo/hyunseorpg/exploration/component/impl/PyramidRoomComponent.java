package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;

/** Preflights the bounded Pyramid underground room after canonical loot extraction. */
public final class PyramidRoomComponent implements ExplorationComponent {
    private final com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomService rooms;

    public PyramidRoomComponent(com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomService rooms) {
        this.rooms = rooms;
    }

    @Override public String type() { return "pyramid_room"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.PYRAMID_LOOT_TRIGGER; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) throws Exception {
        if (!"desert_pyramid".equals(context.record().structureType())) {
            throw new IllegalArgumentException("pyramid_room requires desert_pyramid");
        }
        rooms.prepare(context, spec);
        String actionId = spec.string("action-id", "pyramid_room_reveal");
        long delay = Math.max(0L, spec.integer("reveal-delay-ticks", 140));
        String nextPhase = spec.string("reveal-phase", "pyramid_room_reveal");
        if (!context.sequenceScheduler().schedule(context, actionId, delay, nextPhase)) {
            throw new IllegalStateException("pyramid room reveal already scheduled: " + actionId);
        }
    }
}
