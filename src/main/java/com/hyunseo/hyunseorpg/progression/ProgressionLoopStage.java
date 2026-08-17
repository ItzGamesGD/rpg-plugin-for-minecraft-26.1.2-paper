package com.hyunseo.hyunseorpg.progression;

import java.util.List;

public record ProgressionLoopStage(
        String id,
        String displayName,
        List<ProgressionLoopResource> inputs,
        List<ProgressionLoopResource> outputs,
        List<String> nextStages,
        List<String> recipeIds
) {
    public ProgressionLoopStage {
        id = id == null ? "" : id;
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        inputs = List.copyOf(inputs == null ? List.of() : inputs);
        outputs = List.copyOf(outputs == null ? List.of() : outputs);
        nextStages = List.copyOf(nextStages == null ? List.of() : nextStages);
        recipeIds = List.copyOf(recipeIds == null ? List.of() : recipeIds);
    }
}
