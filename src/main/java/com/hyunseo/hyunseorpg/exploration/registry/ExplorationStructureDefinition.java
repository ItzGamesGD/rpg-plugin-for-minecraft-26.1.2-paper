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
        double lootTriggerRadius,
        long lootTriggerGraceTicks,
        double combatAbandonRadius,
        long combatAbandonGraceTicks,
        List<StructureVariantDefinition> variants,
        double entryBoundaryPadding
) {
    public ExplorationStructureDefinition {
        id = normalize(id);
        minecraftKey = normalize(minecraftKey);
        if (!Double.isFinite(selectionChance) || selectionChance < 0.0D || selectionChance > 1.0D) {
            throw new IllegalArgumentException("selectionChance must be 0..1");
        }
        if (!Double.isFinite(triggerRadius) || triggerRadius <= 0.0D) throw new IllegalArgumentException("triggerRadius <= 0");
        if (!Double.isFinite(lootTriggerRadius) || lootTriggerRadius <= 0.0D) {
            throw new IllegalArgumentException("lootTriggerRadius <= 0");
        }
        if (lootTriggerGraceTicks < 1L) throw new IllegalArgumentException("lootTriggerGraceTicks < 1");
        if (!Double.isFinite(combatAbandonRadius) || combatAbandonRadius < triggerRadius) {
            throw new IllegalArgumentException("combatAbandonRadius < triggerRadius");
        }
        if (combatAbandonGraceTicks < 1L) throw new IllegalArgumentException("combatAbandonGraceTicks < 1");
        if (!Double.isFinite(entryBoundaryPadding) || entryBoundaryPadding < 0.0D) {
            throw new IllegalArgumentException("entryBoundaryPadding < 0");
        }
        variants = List.copyOf(variants == null ? List.of() : variants);
        if (enabled && selectionChance > 0.0D && variants.stream().noneMatch(StructureVariantDefinition::enabled)) {
            throw new IllegalArgumentException("enabled RPG structure requires at least one enabled variant");
        }
    }

    /** Compatibility constructor for callers using the pre-padding canonical shape. */
    public ExplorationStructureDefinition(String id, String minecraftKey, boolean enabled,
                                          double selectionChance, double triggerRadius,
                                          double lootTriggerRadius, long lootTriggerGraceTicks,
                                          double combatAbandonRadius, long combatAbandonGraceTicks,
                                          List<StructureVariantDefinition> variants) {
        this(id, minecraftKey, enabled, selectionChance, triggerRadius, lootTriggerRadius,
                lootTriggerGraceTicks, combatAbandonRadius, combatAbandonGraceTicks, variants, 4.0D);
    }

    /** Compatibility constructor for pre-loot-exit definitions. */
    public ExplorationStructureDefinition(String id, String minecraftKey, boolean enabled,
                                          double selectionChance, double triggerRadius,
                                          double abandonRadius, long abandonGraceTicks,
                                          List<StructureVariantDefinition> variants) {
        this(id, minecraftKey, enabled, selectionChance, triggerRadius, triggerRadius,
                abandonGraceTicks, abandonRadius, abandonGraceTicks, variants);
    }

    public double abandonRadius() { return combatAbandonRadius; }
    public long abandonGraceTicks() { return combatAbandonGraceTicks; }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("blank id");
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
