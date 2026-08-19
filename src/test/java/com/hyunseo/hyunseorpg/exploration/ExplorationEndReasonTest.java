package com.hyunseo.hyunseorpg.exploration;

import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationEndReason;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplorationEndReasonTest {
    @Test
    void exposesDistinctOperationalLifecycleReasons() {
        assertEquals(6, ExplorationEndReason.values().length);
        assertTrue(EnumSet.of(
                ExplorationEndReason.FINAL_CLEAR,
                ExplorationEndReason.ABANDON_GRACE,
                ExplorationEndReason.ACTIVATION_FAILURE,
                ExplorationEndReason.COMPLETION_FAILURE,
                ExplorationEndReason.STATE_INVALID,
                ExplorationEndReason.PLUGIN_DISABLE
        ).containsAll(EnumSet.allOf(ExplorationEndReason.class)));
    }
}
