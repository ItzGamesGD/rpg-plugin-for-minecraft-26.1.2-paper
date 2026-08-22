package com.hyunseo.hyunseorpg.exploration.registry;

import java.util.List;
import java.util.Locale;

/** Structure-level policy. Numeric values stay YAML-controlled and BALANCE_PENDING. */
public record ExplorationStructureDefinition(
        String id,
        String minecraftKey,
        boolean enabled,
        double selectionChance,
        double triggerRadius,
        double abandonRadius,
        long abandonGraceTicks,
        List<StructureVariantDefinition> variants
) {
    public ExplorationStructureDefinition {
        id = normalize(id);
        minecraftKey = normalize(minecraftKey);
        if (!Double.isFinite(selectionChance) || selectionChance < 0.0D || selectionChance > 1.0D) {
            throw new IllegalArgumentException("selectionChance must be 0..1");
        }
        if (!Double.isFinite(triggerRadius) || triggerRadius <= 0.0D) throw new IllegalArgumentException("triggerRadius <= 0");
        if (!Double.isFinite(abandonRadius) || abandonRadius < triggerRadius) throw new IllegalArgumentException("abandonRadius < triggerRadius");
        if (abandonGraceTicks < 1L) throw new IllegalArgumentException("abandonGraceTicks < 1");
        variants = List.copyOf(variants == null ? List.of() : variants);
        if (enabled && selectionChance > 0.0D && variants.stream().noneMatch(StructureVariantDefinition::enabled)) {
            throw new IllegalArgumentException("enabled RPG structure requires at least one enabled variant");
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("blank id");
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
