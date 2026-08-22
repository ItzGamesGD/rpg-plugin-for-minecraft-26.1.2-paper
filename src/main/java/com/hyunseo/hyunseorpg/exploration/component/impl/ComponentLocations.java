package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import org.bukkit.Location;

final class ComponentLocations {
    private ComponentLocations() { }

    static Location relative(ExplorationEventContext context, ExplorationComponentSpec spec) {
        Location anchor = context.anchorLocation().orElseThrow(() -> new IllegalStateException("world is not loaded"));
        return anchor.clone().add(spec.decimal("dx", 0.0D), spec.decimal("dy", 0.0D), spec.decimal("dz", 0.0D));
    }
}
