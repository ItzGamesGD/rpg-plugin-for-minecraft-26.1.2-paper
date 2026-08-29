package com.hyunseo.hyunseorpg.exploration.runtime;

import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression boundary: Pyramid phases must never inherit the Outpost encounter contract. */
final class ExplorationPyramidIsolationTest {
    @Test
    void pyramidCannotExecuteOutpostPhasesOrRaidWaves() {
        assertTrue(leak("loot_exit", "sequence_state", Map.of()));
        assertTrue(leak("pyramid_quiz", "raid_wave_spawn", Map.of()));
        assertTrue(leak("pyramid_quiz", "choice_prompt",
                Map.of("prompt-id", "outpost_raid_difficulty", "choices", List.of("tier1", "flee"))));
        assertFalse(leak("pyramid_quiz", "choice_prompt",
                Map.of("prompt-id", "pyramid_entry_quiz", "choices", List.of("answer_a", "answer_b"))));
    }

    @Test
    void outpostContractRemainsValidForOutpostOnly() {
        assertFalse(ExplorationRuntimeManager.isPyramidOutpostLeak(
                "pillager_outpost", "loot_exit",
                new ExplorationComponentSpec("choice_prompt",
                        Map.of("prompt-id", "outpost_raid_difficulty", "choices", List.of("tier1")))));
    }

    private boolean leak(String phase, String type, Map<String, Object> options) {
        return ExplorationRuntimeManager.isPyramidOutpostLeak(
                "desert_pyramid", phase, new ExplorationComponentSpec(type, options));
    }
}
