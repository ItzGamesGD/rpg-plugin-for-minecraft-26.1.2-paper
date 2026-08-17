package com.hyunseo.hyunseorpg.player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerDataCache {
    private final Map<UUID, PlayerRPGData> cachedData = new HashMap<>();

    public Optional<PlayerRPGData> get(UUID uuid) {
        return Optional.ofNullable(cachedData.get(uuid));
    }

    public void put(PlayerRPGData data) {
        cachedData.put(data.getUuid(), data);
    }

    public Optional<PlayerRPGData> remove(UUID uuid) {
        return Optional.ofNullable(cachedData.remove(uuid));
    }

    public Collection<PlayerRPGData> values() {
        return Collections.unmodifiableCollection(cachedData.values());
    }

    public void clear() {
        cachedData.clear();
    }
}
