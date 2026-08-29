package com.hyunseo.hyunseorpg.exploration.component.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChoicePromptComponentTest {
    @Test
    void pyramidNeverUsesOutpostFallbackPrompt() {
        ExplorationComponentSpec spec = new ExplorationComponentSpec("choice_prompt", Map.of());
        assertEquals("피라미드의 수수께끼가 길을 막습니다.",
                ChoicePromptComponent.defaultPromptText("desert_pyramid", spec));
    }

    @Test
    void configuredPromptStillWins() {
        ExplorationComponentSpec spec = new ExplorationComponentSpec("choice_prompt",
                Map.of("prompt-text", "사용자 지정 문구"));
        assertEquals("사용자 지정 문구", ChoicePromptComponent.defaultPromptText("desert_pyramid", spec));
    }

    @Test
    void defaultsAreOwnedByTheActiveStructure() {
        assertEquals(Set.of("tier1", "tier2", "tier3", "flee"),
                ChoicePromptComponent.defaultChoices("pillager_outpost"));
        assertEquals(Set.of("answer_a", "answer_b", "answer_c"),
                ChoicePromptComponent.defaultChoices("desert_pyramid"));
        assertTrue(ChoicePromptComponent.validChoices("desert_pyramid", Set.of("answer_a", "answer_b")));
        assertFalse(ChoicePromptComponent.validChoices("desert_pyramid", Set.of("tier1", "flee")));
    }

    @Test
    void unknownStructureCannotInheritOutpostContract() {
        assertThrows(IllegalStateException.class,
                () -> ChoicePromptComponent.defaultChoices("unknown_structure"));
        assertEquals("탐험 선택이 준비되지 않았습니다.",
                ChoicePromptComponent.defaultPromptText("unknown_structure",
                        new ExplorationComponentSpec("choice_prompt", Map.of())));
    }
}
