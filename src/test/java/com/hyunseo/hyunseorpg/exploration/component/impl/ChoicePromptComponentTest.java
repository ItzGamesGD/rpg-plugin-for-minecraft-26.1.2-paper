package com.hyunseo.hyunseorpg.exploration.component.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import java.util.Map;
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
}
