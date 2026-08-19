package com.hyunseo.hyunseorpg.exploration.runtime;

import com.hyunseo.hyunseorpg.exploration.model.StructureEventState;
import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationStructureDefinition;

import java.util.UUID;

/** Pure trigger gate for proximity activation; runtime creation remains elsewhere. */
public final class ExplorationTriggerPolicy {
    public boolean isEligible(StructureRecord record, ExplorationStructureDefinition definition,
                               UUID playerWorldId, double x, double y, double z) {
        if (record == null || definition == null || playerWorldId == null) return false;
        if (!definition.enabled() || !record.worldId().equals(playerWorldId)) return false;
        if (record.state() != StructureEventState.UNDISCOVERED
                && record.state() != StructureEventState.ACTIVE) return false;

        double radius = definition.triggerRadius();
        if (!Double.isFinite(radius) || radius <= 0.0D) return false;
        return record.bounds().distanceSquaredTo(x, y, z) <= radius * radius;
    }
}
