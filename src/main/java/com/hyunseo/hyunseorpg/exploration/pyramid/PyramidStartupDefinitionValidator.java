package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.registry.ExplorationStructureDefinition;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.List;

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
        Set<String> seenOfficialComponents = new HashSet<>();
        for (var spec : guardianTrial.components()) {
            String type = spec.type().trim().toLowerCase(Locale.ROOT);
            String phase = spec.string("phase", "").trim().toLowerCase(Locale.ROOT);
            if ("raid_wave_spawn".equals(type)
                    || Set.of("loot_exit", "choice_tier_1", "choice_tier_2", "choice_tier_3", "next_wave")
                    .contains(phase)) {
                throw new IllegalArgumentException("structure=desert_pyramid, variant=guardian_trial: Outpost component/phase is forbidden: "
                        + type + " / " + phase);
            }
            if ("choice_prompt".equals(type)) validatePyramidChoiceContract(spec);
            // Sequence-state entries are repeatable extension steps. Only the
            // canonical guardian-trial components have one-entry semantics.
            if (!REQUIRED_GUARDIAN_TRIAL_PHASES.containsKey(type)) {
                continue;
            }
            if (!spec.bool("enabled", true)) {
                throw new IllegalArgumentException("structure=desert_pyramid, variant=guardian_trial: required component is disabled: "
                        + type);
            }
            if (!seenOfficialComponents.add(type)) {
                throw new IllegalArgumentException("structure=desert_pyramid, variant=guardian_trial: duplicate component="
                        + type);
            }
            phases.put(type, spec.string("phase", "").trim().toLowerCase(Locale.ROOT));
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

    private static void validatePyramidChoiceContract(com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec spec) {
        String promptId = spec.string("prompt-id", "").trim().toLowerCase(Locale.ROOT);
        if (promptId.isBlank() || "outpost_raid_difficulty".equals(promptId)) {
            throw new IllegalArgumentException("structure=desert_pyramid, component=choice_prompt: Pyramid prompt-id is missing or Outpost-owned");
        }
        List<String> choices = spec.stringList("choices");
        if (choices.isEmpty() || choices.stream().map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> Set.of("tier1", "tier2", "tier3", "flee").contains(value)
                        || !value.startsWith("answer_"))) {
            throw new IllegalArgumentException("structure=desert_pyramid, component=choice_prompt: Pyramid answer choices are invalid");
        }
        String fallback = spec.string("default-choice", "").trim().toLowerCase(Locale.ROOT);
        if (!choices.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList().contains(fallback)) {
            throw new IllegalArgumentException("structure=desert_pyramid, component=choice_prompt: default-choice is not an answer choice");
        }
        String promptText = spec.string("prompt-text", "").toLowerCase(Locale.ROOT);
        if (promptText.contains("outpost") || promptText.contains("약탈자") || promptText.contains("습격")) {
            throw new IllegalArgumentException("structure=desert_pyramid, component=choice_prompt: Outpost prompt text is forbidden");
        }
    }
}
