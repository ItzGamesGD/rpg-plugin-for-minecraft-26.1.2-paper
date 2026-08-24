package com.hyunseo.hyunseorpg.exploration.pyramid;

import java.util.Locale;
import java.util.Set;

/**
 * Explicit Pyramid-only variant composition. It is intentionally a small
 * completion policy, not a sequence engine and not a cross-module callback
 * graph.
 */
public enum PyramidVariantModules {
    GUARDIAN_ONLY("guardian_only", Set.of(Module.GUARDIAN)),
    UNDERGROUND_PUZZLE_ONLY("underground_puzzle_only", Set.of(Module.UNDERGROUND)),
    GUARDIAN_AND_PUZZLE("guardian_and_puzzle", Set.of(Module.GUARDIAN, Module.UNDERGROUND));

    private final String id;
    private final Set<Module> requiredModules;

    PyramidVariantModules(String id, Set<Module> requiredModules) {
        this.id = id;
        this.requiredModules = Set.copyOf(requiredModules);
    }

    public String id() {
        return id;
    }

    public Set<Module> requiredModules() {
        return requiredModules;
    }

    public static PyramidVariantModules parse(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (PyramidVariantModules variant : values()) {
            if (variant.id.equals(normalized)) return variant;
        }
        throw new IllegalArgumentException("unknown desert pyramid variant: " + value);
    }

    public enum Module {
        GUARDIAN,
        UNDERGROUND
    }
}
