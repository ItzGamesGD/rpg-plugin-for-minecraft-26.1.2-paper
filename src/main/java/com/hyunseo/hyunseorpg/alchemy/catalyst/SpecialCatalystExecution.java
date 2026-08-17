package com.hyunseo.hyunseorpg.alchemy.catalyst;

import java.util.Set;
import java.util.UUID;

public interface SpecialCatalystExecution {
    Result execute(Request request);
    void cancel(UUID executionId, CancelReason reason);
    void cancelWorld(UUID worldId, CancelReason reason);

    record Request(UUID executionId, String potionId, String catalystId, UUID worldId,
                   Set<UUID> visitedTargets, UUID sourceId) {
        public Request(UUID executionId, String potionId, String catalystId, UUID worldId, Set<UUID> visitedTargets) {
            this(executionId, potionId, catalystId, worldId, visitedTargets, null);
        }
        public Request { visitedTargets = Set.copyOf(visitedTargets == null ? Set.of() : visitedTargets); }
    }
    enum Result { STARTED, REJECTED_NOT_REGISTERED, REJECTED_DUPLICATE, REJECTED_POLICY, REJECTED_INVALID_PDC, REJECTED_LIMIT, CANCELLED }
    enum CancelReason { CHUNK_UNLOAD, WORLD_UNLOAD, ENTITY_REMOVED, SERVER_RESTART, TIMEOUT, ADMIN_CANCEL }
}
