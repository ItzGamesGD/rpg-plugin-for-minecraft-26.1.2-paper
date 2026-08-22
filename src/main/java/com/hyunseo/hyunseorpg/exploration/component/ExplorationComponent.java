package com.hyunseo.hyunseorpg.exploration.component;

import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;

public interface ExplorationComponent {
    String type();
    ExplorationComponentPhase defaultPhase();
    void execute(ExplorationEventContext context, ExplorationComponentSpec spec) throws Exception;
}
