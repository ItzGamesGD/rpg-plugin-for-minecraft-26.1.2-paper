package com.hyunseo.hyunseorpg.exploration.ocean;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Ocean Monument's deterministic, runtime-independent content state.
 *
 * <p>The confirmed content flow is discovery, three seals, a pre-final-seal
 * warning, transition, encounter objectives, the Deep-Sea Tidecaster eligibility,
 * clear eligibility, and one logical final-reward claim. It deliberately stores
 * only logical identifiers: no Bukkit types, entity UUIDs, tasks, or locations.</p>
 */
public final class OceanMonumentProgress {
    public static final int REQUIRED_SEAL_COUNT = 3;

    private final Set<String> requiredSealIds;
    private final Set<String> requiredObjectiveIds;
    private final boolean bossRequired;
    private final Set<String> completedSealIds = new LinkedHashSet<>();
    private final Set<String> completedObjectiveIds = new LinkedHashSet<>();

    private MonumentPhase phase = MonumentPhase.DISCOVERY;
    private boolean rewardClaimed;

    public OceanMonumentProgress(
            Set<String> requiredSealIds,
            Set<String> requiredObjectiveIds,
            boolean bossRequired
    ) {
        this.requiredSealIds = immutableIds(requiredSealIds, "requiredSealIds");
        if (this.requiredSealIds.size() != REQUIRED_SEAL_COUNT) {
            throw new IllegalArgumentException("Ocean Monument requires exactly "
                    + REQUIRED_SEAL_COUNT + " seals");
        }
        this.requiredObjectiveIds = immutableIds(requiredObjectiveIds, "requiredObjectiveIds");
        this.bossRequired = bossRequired;
    }

    public MonumentPhase phase() {
        return phase;
    }

    public int completedSealCount() {
        return completedSealIds.size();
    }

    public int remainingSealCount() {
        return requiredSealIds.size() - completedSealIds.size();
    }

    public int completedObjectiveCount() {
        return completedObjectiveIds.size();
    }

    public int remainingObjectiveCount() {
        return requiredObjectiveIds.size() - completedObjectiveIds.size();
    }

    public Set<String> completedSealIds() {
        return Set.copyOf(completedSealIds);
    }

    public Set<String> completedObjectiveIds() {
        return Set.copyOf(completedObjectiveIds);
    }

    public boolean finalSealWarningRequired() {
        return phase == MonumentPhase.SEAL_OBJECTIVES && remainingSealCount() == 1;
    }

    public boolean bossRequired() {
        return bossRequired;
    }

    public boolean bossEligible() {
        return phase == MonumentPhase.BOSS_ELIGIBLE;
    }

    public boolean clearEligible() {
        return phase == MonumentPhase.CLEAR_ELIGIBLE;
    }

    public boolean rewardEligible() {
        return phase == MonumentPhase.CLEARED && !rewardClaimed;
    }

    public boolean rewardClaimed() {
        return rewardClaimed;
    }

    public MonumentActionResult discover() {
        if (phase.terminal()) return terminal();
        if (phase != MonumentPhase.DISCOVERY) return invalidPhase();
        phase = MonumentPhase.SEAL_OBJECTIVES;
        return applied();
    }

    /**
     * Completes one seal exactly once. The third seal is gated by a separate,
     * explicit final-seal warning acknowledgement.
     */
    public MonumentActionResult completeSeal(String sealId) {
        if (!requiredSealIds.contains(sealId)) return unknown();
        if (completedSealIds.contains(sealId)) return duplicate();
        if (phase.terminal()) return terminal();
        if (phase == MonumentPhase.SEAL_OBJECTIVES && finalSealWarningRequired()) {
            return reject(MonumentActionResult.Reason.FINAL_SEAL_WARNING_REQUIRED);
        }
        if (phase != MonumentPhase.SEAL_OBJECTIVES && phase != MonumentPhase.FINAL_SEAL_READY) {
            return invalidPhase();
        }
        if (phase == MonumentPhase.FINAL_SEAL_READY && remainingSealCount() != 1) {
            return invalidPhase();
        }

        completedSealIds.add(sealId);
        if (completedSealIds.size() == requiredSealIds.size()) {
            phase = MonumentPhase.TRANSITION_PENDING;
        }
        return applied();
    }

    /**
     * Records that the runtime has displayed the warning before the final seal.
     * Presentation itself remains a later runtime-adapter concern.
     */
    public MonumentActionResult acknowledgeFinalSealWarning() {
        if (phase.terminal()) return terminal();
        if (!finalSealWarningRequired()) return invalidPhase();
        phase = MonumentPhase.FINAL_SEAL_READY;
        return applied();
    }

    public MonumentActionResult beginEncounter() {
        if (phase.terminal()) return terminal();
        if (phase != MonumentPhase.TRANSITION_PENDING) return invalidPhase();
        phase = MonumentPhase.ENCOUNTER_ACTIVE;
        return applied();
    }

    /**
     * Completes an already-defined logical encounter objective exactly once.
     */
    public MonumentActionResult completeObjective(String objectiveId) {
        if (!requiredObjectiveIds.contains(objectiveId)) return unknown();
        if (completedObjectiveIds.contains(objectiveId)) return duplicate();
        if (phase.terminal()) return terminal();
        if (phase != MonumentPhase.ENCOUNTER_ACTIVE) return invalidPhase();

        completedObjectiveIds.add(objectiveId);
        if (completedObjectiveIds.size() == requiredObjectiveIds.size()) {
            phase = bossRequired ? MonumentPhase.BOSS_ELIGIBLE : MonumentPhase.CLEAR_ELIGIBLE;
        }
        return applied();
    }

    /**
     * Marks the logical boss objective complete. It never represents an entity
     * death event; a future Paper adapter will decide when to call it.
     */
    public MonumentActionResult completeBoss() {
        if (phase.terminal()) return terminal();
        if (phase != MonumentPhase.BOSS_ELIGIBLE) return invalidPhase();
        phase = MonumentPhase.CLEAR_ELIGIBLE;
        return applied();
    }

    /**
     * Separates content clear eligibility from StructureRecord persistence,
     * reward delivery and Bukkit cleanup.
     */
    public MonumentActionResult clear() {
        if (phase == MonumentPhase.CLEARED) return duplicate();
        if (phase == MonumentPhase.ABANDONED) return terminal();
        if (phase != MonumentPhase.CLEAR_ELIGIBLE) return invalidPhase();
        phase = MonumentPhase.CLEARED;
        return applied();
    }

    /**
     * Pure duplicate-suppression for a future reward adapter; no reward is given
     * by this class.
     */
    public MonumentActionResult claimFinalReward() {
        if (phase == MonumentPhase.ABANDONED) return terminal();
        if (phase != MonumentPhase.CLEARED) return invalidPhase();
        if (rewardClaimed) return reject(MonumentActionResult.Reason.REWARD_ALREADY_CLAIMED);
        rewardClaimed = true;
        return applied();
    }

    public MonumentActionResult abandon() {
        if (phase.terminal()) return terminal();
        phase = MonumentPhase.ABANDONED;
        return applied();
    }

    private MonumentActionResult applied() {
        return MonumentActionResult.applied(phase);
    }

    private MonumentActionResult unknown() {
        return reject(MonumentActionResult.Reason.UNKNOWN_ID);
    }

    private MonumentActionResult duplicate() {
        return reject(MonumentActionResult.Reason.DUPLICATE);
    }

    private MonumentActionResult invalidPhase() {
        return reject(MonumentActionResult.Reason.INVALID_PHASE);
    }

    private MonumentActionResult terminal() {
        return reject(MonumentActionResult.Reason.TERMINAL);
    }

    private MonumentActionResult reject(MonumentActionResult.Reason reason) {
        return MonumentActionResult.rejected(reason, phase);
    }

    private static Set<String> immutableIds(Set<String> values, String name) {
        Objects.requireNonNull(values, name);
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must contain non-blank ids");
            }
            normalized.add(value);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return Set.copyOf(normalized);
    }
}
