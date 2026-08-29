package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.registry.ExplorationStructureDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

/** Authoritative startup gate executed after registry loading and before module activation. */
public final class PyramidStartupDefinitionValidator {
    private static final Map<String, String> REQUIRED_GUARDIAN_TRIAL_PHASES = Map.of(
            "pyramid_room", "pyramid_loot_trigger",
            "pyramid_room_reveal", "pyramid_room_reveal",
            "pyramid_repel", "pyramid_entry",
            "choice_prompt", "pyramid_quiz",
            "pyramid_guardian", "pyramid_guardian_spawn",
            "pyramid_push_pillars", "pyramid_pillar_restore",
            "reward_drop", "clear");

    private PyramidStartupDefinitionValidator() { }

    public static void requireValid(ExplorationStructureDefinition definition) {
        if (definition == null) return;
        if (!"desert_pyramid".equals(definition.id())) {
            throw new IllegalArgumentException("Pyramid startup validator requires desert_pyramid definition");
        }
        var guardianTrial = definition.variants().stream()
                .filter(variant -> "guardian_trial".equals(variant.id())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "structure=desert_pyramid: missing variant=guardian_trial"));

        Map<String, String> phases = new LinkedHashMap<>();
        for (var spec : guardianTrial.components()) {
            if (phases.put(spec.type(), spec.string("phase", "").trim().toLowerCase(java.util.Locale.ROOT)) != null) {
                throw new IllegalArgumentException("structure=desert_pyramid, variant=guardian_trial: duplicate component="
                        + spec.type());
            }
        }
        for (var required : REQUIRED_GUARDIAN_TRIAL_PHASES.entrySet()) {
            String actual = phases.get(required.getKey());
            if (!required.getValue().equals(actual)) {
                throw new IllegalArgumentException("structure=desert_pyramid, variant=guardian_trial, component="
                        + required.getKey() + ": expected phase=" + required.getValue() + ", actual=" + actual);
            }
        }

        for (var variant : definition.variants()) {
            int componentIndex = 0;
            for (var spec : variant.components()) {
                if (!"pyramid_push_pillars".equals(spec.type())) continue;
                String context = "structure=desert_pyramid, variant=" + variant.id()
                        + ", pillarComponent=" + componentIndex++;
                var pillars = PyramidPillarDefinitionParser.parse(spec.options().get("pillars"), context);
                PyramidPillarConfigurationValidator.requireValid(pillars);
            }
        }
    }
}
