package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import com.hyunseo.hyunseorpg.exploration.persistence.StructureRepository;
import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntimeManager;

import java.io.IOException;
import java.util.UUID;

/** The sole durable transition authority for Desert Pyramid underground completion. */
public final class PyramidUndergroundCompletionCoordinator {
    public enum CompletionStatus {
        COMPLETED, ALREADY_COMPLETED, BLOCKED_RECOVERY_REQUIRED, NOT_ELIGIBLE, PERSISTENCE_FAILED
    }

    public record CompletionResult(CompletionStatus status, StructureRecord record) {
        public boolean completed() {
            return status == CompletionStatus.COMPLETED || status == CompletionStatus.ALREADY_COMPLETED;
        }
    }
    private PyramidUndergroundCompletionCoordinator() { }

    public static boolean persistSolvedIntentAndLogicalPosition(StructureRepository repository, UUID structureId,
                                                                   String pillarId, PyramidGridPoint position) throws IOException {
        StructureRecord record = repository.get(structureId).orElse(null);
        if (record == null || pillarId == null || position == null) return false;
        if ("RECOVERY_REQUIRED".equals(record.activationMetadata().get("pyramid-failure-state"))) return false;
        if (Boolean.parseBoolean(record.activationMetadata().getOrDefault("pyramid-underground-complete", "false"))) return true;
        StructureRecord next = record.withMetadata("pyramid-underground-completion-state",
                PyramidUndergroundCompletionState.COMPLETION_PENDING.value())
                .withMetadata("pyramid-pillar-position-" + pillarId,
                        position.x() + "," + position.z())
                .withMetadata("pyramid-pillar-solved-" + pillarId, "true");
        if (next != record) repository.save(next);
        return true;
    }

    public static boolean persistSolvedIntent(StructureRepository repository, UUID structureId) throws IOException {
        StructureRecord record = repository.get(structureId).orElse(null);
        if (record == null) return false;
        if ("RECOVERY_REQUIRED".equals(record.activationMetadata().get("pyramid-failure-state"))) return false;
        if (Boolean.parseBoolean(record.activationMetadata().getOrDefault("pyramid-underground-complete", "false"))) return true;
        if (PyramidUndergroundCompletionState.parse(record.activationMetadata()
                .get("pyramid-underground-completion-state")) == PyramidUndergroundCompletionState.COMPLETION_PENDING) return true;
        repository.save(record.withMetadata("pyramid-underground-completion-state",
                PyramidUndergroundCompletionState.COMPLETION_PENDING.value()));
        return true;
    }

    /** Idempotently converges UNSOLVED/PENDING/legacy contradictions to durable COMPLETE. */
    public static CompletionResult complete(StructureRepository repository, UUID structureId) {
        StructureRecord record = repository.get(structureId).orElse(null);
        if (record == null) return new CompletionResult(CompletionStatus.NOT_ELIGIBLE, null);
        if ("RECOVERY_REQUIRED".equals(record.activationMetadata().get("pyramid-failure-state"))) {
            return new CompletionResult(CompletionStatus.BLOCKED_RECOVERY_REQUIRED, record);
        }
        boolean complete = Boolean.parseBoolean(record.activationMetadata()
                .getOrDefault("pyramid-underground-complete", "false"));
        PyramidUndergroundCompletionState state = PyramidUndergroundCompletionState.reconcile(complete,
                record.activationMetadata().get("pyramid-underground-completion-state"));
        if (complete && state == PyramidUndergroundCompletionState.COMPLETE) {
            return new CompletionResult(CompletionStatus.ALREADY_COMPLETED, record);
        }
        // Completion is legal only after the same durable pending intent written before
        // the irreversible final pillar move (or while normalizing a legacy true flag).
        // This intentionally refuses arbitrary callers from completing an unsolved module.
        if (!complete && state != PyramidUndergroundCompletionState.COMPLETION_PENDING) {
            return new CompletionResult(CompletionStatus.NOT_ELIGIBLE, record);
        }
        StructureRecord resolved = record.withMetadata("pyramid-underground-complete", "true")
                .withMetadata("pyramid-underground-completion-state", PyramidUndergroundCompletionState.COMPLETE.value())
                .withMetadata("pyramid-content-version",
                        Integer.toString(ExplorationRuntimeManager.CURRENT_PYRAMID_CONTENT_VERSION));
        try {
            repository.save(resolved);
            return new CompletionResult(CompletionStatus.COMPLETED, resolved);
        } catch (IOException | RuntimeException failure) {
            return new CompletionResult(CompletionStatus.PERSISTENCE_FAILED, record);
        }
    }
}
