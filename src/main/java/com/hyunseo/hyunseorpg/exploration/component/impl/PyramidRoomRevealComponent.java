package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomService;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;

import java.io.IOException;

/** Applies a preflighted pyramid room after the telegraph delay. */
public final class PyramidRoomRevealComponent implements ExplorationComponent {
    private final PyramidRoomService rooms;

    public PyramidRoomRevealComponent(PyramidRoomService rooms) {
        this.rooms = rooms;
    }

    @Override public String type() { return "pyramid_room_reveal"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.ACTIVATE; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) throws IOException {
        if (!"desert_pyramid".equals(context.record().structureType())) {
            throw new IllegalArgumentException("pyramid_room_reveal requires desert_pyramid");
        }
        rooms.reveal(context, spec);
        context.runtime().sequence().setFlag("pyramid.room.ready");
        context.runtime().tracker().track(() -> rooms.cleanup(context.runtime().structureId()));
    }
}
