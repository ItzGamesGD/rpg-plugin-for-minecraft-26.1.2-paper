package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;

public final class PuzzleComponent implements ExplorationComponent {
    @Override public String type() { return "puzzle"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.ACTIVATE; }
    @Override public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        String puzzleId = spec.string("puzzle-id", "");
        if (puzzleId.isBlank()) throw new IllegalArgumentException("puzzle requires puzzle-id");
        Runnable cleanup = context.ports().puzzles().start(puzzleId, ComponentLocations.relative(context, spec), spec.options());
        context.runtime().tracker().track(cleanup);
    }
}
