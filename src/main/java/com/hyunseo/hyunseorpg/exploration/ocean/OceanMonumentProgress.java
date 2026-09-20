package com.hyunseo.hyunseorpg.exploration.ocean;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Pure logical state for the dormant Ocean Monument design. */
public final class OceanMonumentProgress {
    public static final int REQUIRED_SEAL_COUNT = 3;

    private final Set<String> sealIds;
    private final Set<String> objectiveIds;
    private final Set<String> brokenSealIds = new LinkedHashSet<>();
    private final Set<String> completedObjectiveIds = new LinkedHashSet<>();
    private MonumentPhase phase = MonumentPhase.UNDISCOVERED;
    private boolean finalSealWarningAcknowledged;
    private boolean bossDefeated;
    private boolean finalRewardClaimed;

    public OceanMonumentProgress(Set<String> sealIds, Set<String> objectiveIds) {
        this.sealIds = normalizedIds(sealIds, "sealIds");
        this.objectiveIds = normalizedIds(objectiveIds, "objectiveIds");
        if (this.sealIds.size() != REQUIRED_SEAL_COUNT) {
            throw new IllegalArgumentException("Ocean Monument requires exactly three seal IDs");
        }
    }

    public MonumentPhase phase() {
        return phase;
    }

    public Set<String> sealIds() {
        return sealIds;
    }

    public Set<String> brokenSealIds() {
        return Set.copyOf(brokenSealIds);
    }

    public Set<String> objectiveIds() {
        return objectiveIds;
    }

    public Set<String> completedObjectiveIds() {
        return Set.copyOf(completedObjectiveIds);
    }

    public boolean finalSealWarningAcknowledged() {
        return finalSealWarningAcknowledged;
    }

    public boolean bossEligible() {
        return phase == MonumentPhase.ENCOUNTER
                && completedObjectiveIds.containsAll(objectiveIds)
                && !bossDefeated;
    }

    public boolean clearEligible() {
        return phase == MonumentPhase.ENCOUNTER && bossDefeated;
    }

    public boolean finalRewardClaimed() {
        return finalRewardClaimed;
    }

    public MonumentActionResult discover() {
        if (phase.terminal()) return MonumentActionResult.TERMINAL;
        if (phase != MonumentPhase.UNDISCOVERED) return MonumentActionResult.DUPLICATE;
        phase = MonumentPhase.SEALED;
        return MonumentActionResult.APPLIED;
    }

    public MonumentActionResult acknowledgeFinalSealWarning() {
        if (phase.terminal()) return MonumentActionResult.TERMINAL;
        if (phase != MonumentPhase.SEALED || brokenSealIds.size() != REQUIRED_SEAL_COUNT - 1) {
            return MonumentActionResult.INVALID_PHASE;
        }
        if (finalSealWarningAcknowledged) return MonumentActionResult.DUPLICATE;
        finalSealWarningAcknowledged = true;
        return MonumentActionResult.APPLIED;
    }

    public MonumentActionResult breakSeal(String sealId) {
        if (phase.terminal()) return MonumentActionResult.TERMINAL;
        if (phase != MonumentPhase.SEALED) return MonumentActionResult.INVALID_PHASE;
        if (!sealIds.contains(sealId)) return MonumentActionResult.UNKNOWN_ID;
        if (brokenSealIds.contains(sealId)) return MonumentActionResult.DUPLICATE;
        if (brokenSealIds.size() == REQUIRED_SEAL_COUNT - 1 && !finalSealWarningAcknowledged) {
            return MonumentActionResult.ACKNOWLEDGEMENT_REQUIRED;
        }
        brokenSealIds.add(sealId);
        if (brokenSealIds.size() == REQUIRED_SEAL_COUNT) phase = MonumentPhase.TRANSITION;
        return MonumentActionResult.APPLIED;
    }

    public MonumentActionResult beginEncounter() {
        if (phase.terminal()) return MonumentActionResult.TERMINAL;
        if (phase != MonumentPhase.TRANSITION) return MonumentActionResult.INVALID_PHASE;
        phase = MonumentPhase.ENCOUNTER;
        return MonumentActionResult.APPLIED;
    }

    public MonumentActionResult completeObjective(String objectiveId) {
        if (phase.terminal()) return MonumentActionResult.TERMINAL;
        if (phase != MonumentPhase.ENCOUNTER) return MonumentActionResult.INVALID_PHASE;
        if (!objectiveIds.contains(objectiveId)) return MonumentActionResult.UNKNOWN_ID;
        return completedObjectiveIds.add(objectiveId)
                ? MonumentActionResult.APPLIED : MonumentActionResult.DUPLICATE;
    }

    public MonumentActionResult defeatBoss() {
        if (phase.terminal()) return MonumentActionResult.TERMINAL;
        if (bossDefeated) return MonumentActionResult.DUPLICATE;
        if (!bossEligible()) return MonumentActionResult.NOT_ELIGIBLE;
        bossDefeated = true;
        return MonumentActionResult.APPLIED;
    }

    public MonumentActionResult clear() {
        if (phase.terminal()) return MonumentActionResult.TERMINAL;
        if (!clearEligible()) return MonumentActionResult.NOT_ELIGIBLE;
        phase = MonumentPhase.CLEARED;
        return MonumentActionResult.APPLIED;
    }

    public MonumentActionResult claimFinalReward() {
        if (phase == MonumentPhase.ABANDONED) return MonumentActionResult.TERMINAL;
        if (phase != MonumentPhase.CLEARED) return MonumentActionResult.INVALID_PHASE;
        if (finalRewardClaimed) return MonumentActionResult.DUPLICATE;
        finalRewardClaimed = true;
        return MonumentActionResult.APPLIED;
    }

    public MonumentActionResult abandon() {
        if (phase.terminal()) return MonumentActionResult.TERMINAL;
        phase = MonumentPhase.ABANDONED;
        return MonumentActionResult.APPLIED;
    }

    private static Set<String> normalizedIds(Set<String> ids, String name) {
        Objects.requireNonNull(ids, name);
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException(name + " contains a blank ID");
            if (!normalized.add(id)) throw new IllegalArgumentException(name + " contains a duplicate ID: " + id);
        }
        return Set.copyOf(normalized);
    }
}
