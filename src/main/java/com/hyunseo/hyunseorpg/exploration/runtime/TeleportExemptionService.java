package com.hyunseo.hyunseorpg.exploration.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Short-lived exemption used by ForcedRelocation and external system teleports. */
public final class TeleportExemptionService {
    private final Map<Key, Long> untilTick = new HashMap<>();

    public synchronized void exempt(UUID structureId, UUID playerId, long currentTick, long durationTicks) {
        untilTick.put(new Key(structureId, playerId), currentTick + Math.max(1L, durationTicks));
    }

    public synchronized boolean isExempt(UUID structureId, UUID playerId, long currentTick) {
        Long until = untilTick.get(new Key(structureId, playerId));
        if (until == null) return false;
        if (currentTick <= until) return true;
        untilTick.remove(new Key(structureId, playerId));
        return false;
    }

    public synchronized void clear(UUID structureId) {
        untilTick.keySet().removeIf(key -> key.structureId.equals(structureId));
    }

    private record Key(UUID structureId, UUID playerId) { }
}
