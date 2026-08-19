package com.hyunseo.hyunseorpg.exploration.registry;

import java.util.List;
import java.util.Locale;

public record StructureVariantDefinition(
        String id,
        double weight,
        boolean enabled,
        List<ExplorationComponentSpec> components
) {
    public StructureVariantDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("variant id is blank");
        id = id.trim().toLowerCase(Locale.ROOT);
        if (!Double.isFinite(weight) || weight < 0.0D) throw new IllegalArgumentException("invalid weight");
        components = List.copyOf(components == null ? List.of() : components);
    }
}
