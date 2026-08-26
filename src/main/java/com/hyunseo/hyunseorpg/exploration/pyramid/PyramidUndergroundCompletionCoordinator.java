package com.hyunseo.hyunseorpg.exploration.pyramid;

import com.hyunseo.hyunseorpg.exploration.model.StructureRecord;
import com.hyunseo.hyunseorpg.exploration.persistence.StructureRepository;
import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntimeManager;

import java.io.IOException;
import java.util.UUID;

/** The sole durable transition authority for Desert Pyramid underground completion. */
public final class PyramidUndergroundCompletionCoordinator {
    private PyramidUndergroundCompletionCoordinator() { }

    public static boolean persistSolvedIntent(StructureRepository repository, UUID structureId) throws IOException {
        StructureRecord record = repository.get(structureId).orElse(null);
        if (record == null) return false;
        if (Boolean.parseBoolean(record.activationMetadata().getOrDefault("pyramid-underground-complete", "false"))) return true;
        if (PyramidUndergroundCompletionState.parse(record.activationMetadata()
                .get("pyramid-underground-completion-state")) == PyramidUndergroundCompletionState.COMPLETION_PENDING) return true;
        repository.save(record.withMetadata("pyramid-underground-completion-state",
                PyramidUndergroundCompletionState.COMPLETION_PENDING.value()));
        return true;
    }

    /** Idempotently converges UNSOLVED/PENDING/legacy contradictions to durable COMPLETE. */
    public static StructureRecord complete(StructureRepository repository, UUID structureId) throws IOException {
        StructureRecord record = repository.get(structureId).orElse(null);
        if (record == null) return null;
        boolean complete = Boolean.parseBoolean(record.activationMetadata()
                .getOrDefault("pyramid-underground-complete", "false"));
        PyramidUndergroundCompletionState state = PyramidUndergroundCompletionState.reconcile(complete,
                record.activationMetadata().get("pyramid-underground-completion-state"));
        if (complete && state == PyramidUndergroundCompletionState.COMPLETE) return record;
        if (!complete && state != PyramidUndergroundCompletionState.COMPLETION_PENDING) {
            if (!persistSolvedIntent(repository, structureId)) return null;
            record = repository.get(structureId).orElse(record);
        }
        StructureRecord resolved = record.withMetadata("pyramid-underground-complete", "true")
                .withMetadata("pyramid-underground-completion-state", PyramidUndergroundCompletionState.COMPLETE.value())
                .withMetadata("pyramid-content-version",
                        Integer.toString(ExplorationRuntimeManager.CURRENT_PYRAMID_CONTENT_VERSION));
        repository.save(resolved);
        return resolved;
    }
}
