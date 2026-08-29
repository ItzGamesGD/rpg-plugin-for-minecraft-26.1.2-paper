package com.hyunseo.hyunseorpg.exploration.pyramid;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Minimal logical state for composing independent Pyramid modules.
 *
 * <p>Only completed/unavailable module identity belongs in persistent metadata.
 * Delays, pillar cells, displays and pending resurrection callbacks remain
 * runtime-only under the Exploration lifecycle policy.</p>
 */
public final class PyramidModuleProgress {
    private final PyramidVariantModules variant;
    private final EnumSet<PyramidVariantModules.Module> completed =
        EnumSet.noneOf(PyramidVariantModules.Module.class);
    private final EnumSet<PyramidVariantModules.Module> unavailable =
        EnumSet.noneOf(PyramidVariantModules.Module.class);

    public PyramidModuleProgress(PyramidVariantModules variant) {
        this.variant = Objects.requireNonNull(variant, "variant");
    }

    /** Returns true only for the first completion of a required module. */
    public boolean complete(PyramidVariantModules.Module module) {
        Objects.requireNonNull(module, "module");
        return variant.requiredModules().contains(module)
            && !unavailable.contains(module)
            && completed.add(module);
    }

    /** Preflight failure may skip only a required underground module. */
    public boolean markUnavailable(PyramidVariantModules.Module module) {
        Objects.requireNonNull(module, "module");
        if (module != PyramidVariantModules.Module.UNDERGROUND
            || !variant.requiredModules().contains(module)
            || completed.contains(module)) {
            return false;
        }
        return unavailable.add(module);
    }

    public boolean isCompleted(PyramidVariantModules.Module module) {
        return completed.contains(module);
    }

    public boolean isUnavailable(PyramidVariantModules.Module module) {
        return unavailable.contains(module);
    }

    /**
     * A combined variant treats a preflight-unavailable underground module as
     * skipped, so Guardian remains independently completable. An
     * underground-only variant instead becomes a no-reward terminal outcome.
     */
    public boolean structureComplete() {
        return variant.requiredModules().stream()
            .allMatch(module -> completed.contains(module) || unavailable.contains(module));
    }

    public boolean terminalWithoutReward() {
        return variant == PyramidVariantModules.UNDERGROUND_PUZZLE_ONLY
            && unavailable.contains(PyramidVariantModules.Module.UNDERGROUND);
    }

    public Set<PyramidVariantModules.Module> completedModules() {
        return Set.copyOf(completed);
    }

    public Set<PyramidVariantModules.Module> unavailableModules() {
        return Set.copyOf(unavailable);
    }
}
