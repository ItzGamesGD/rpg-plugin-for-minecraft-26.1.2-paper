package com.hyunseo.hyunseorpg.alchemy;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record CustomEffectDefinition(String id, String displayName, boolean enabled, int priority,
                                     int durationTicks, int amplifier, int maxStacks,
                                     EffectTargetPolicy targetPolicy, EffectStackPolicy stackPolicy,
                                     boolean removeOnDeath, boolean persistOnLogout,
                                     boolean persistOnWorldChange, String handlerId,
                                     List<EffectComponentDefinition> components) {
    public CustomEffectDefinition {
        id = normalizeId(id);
        displayName = displayName == null || displayName.isBlank() ? id : displayName.trim();
        if (!id.matches("[a-z0-9]+(_[a-z0-9]+)*")) {
            throw new IllegalArgumentException("invalid effect id: " + id);
        }
        if (durationTicks < 1) throw new IllegalArgumentException("durationTicks must be positive");
        if (amplifier < 0) throw new IllegalArgumentException("amplifier must be non-negative");
        if (maxStacks < 1) throw new IllegalArgumentException("maxStacks must be positive");
        targetPolicy = Objects.requireNonNull(targetPolicy, "targetPolicy");
        stackPolicy = Objects.requireNonNull(stackPolicy, "stackPolicy");
        handlerId = handlerId == null ? "" : handlerId.trim().toLowerCase(Locale.ROOT);
        components = List.copyOf(components == null ? List.of() : components);
    }

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
