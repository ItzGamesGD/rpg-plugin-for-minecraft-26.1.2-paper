package com.hyunseo.hyunseorpg.special;

/** Declarative ability metadata shared by special-equipment UI and runtime handlers. */
public record SpecialEquipmentAbilityDefinition(
        String id,
        String displayName,
        String trigger,
        String description,
        String configPath
) {
    public SpecialEquipmentAbilityDefinition {
        id = id == null ? "" : id;
        displayName = displayName == null ? id : displayName;
        trigger = trigger == null ? "" : trigger;
        description = description == null ? "" : description;
        configPath = configPath == null ? "" : configPath;
    }
}
