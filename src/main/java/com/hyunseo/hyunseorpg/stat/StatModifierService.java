package com.hyunseo.hyunseorpg.stat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StatModifierService {
    private final Map<UUID, Map<String, StatModifier>> modifiersByPlayer = new HashMap<>();

    public void putModifier(UUID playerId, StatModifier modifier) {
        modifiersByPlayer
                .computeIfAbsent(playerId, ignored -> new HashMap<>())
                .put(modifier.sourceId(), modifier);
    }

    public boolean removeModifier(UUID playerId, String sourceId) {
        Map<String, StatModifier> modifiers = modifiersByPlayer.get(playerId);
        if (modifiers == null) {
            return false;
        }

        boolean removed = modifiers.remove(normalizeSourceId(sourceId)) != null;
        if (modifiers.isEmpty()) {
            modifiersByPlayer.remove(playerId);
        }
        return removed;
    }

    public Collection<StatModifier> getModifiers(UUID playerId) {
        Map<String, StatModifier> modifiers = modifiersByPlayer.get(playerId);
        if (modifiers == null) {
            return ListBackedEmptyCollection.INSTANCE;
        }
        return Collections.unmodifiableCollection(modifiers.values());
    }

    public void clearModifiers(UUID playerId) {
        modifiersByPlayer.remove(playerId);
    }

    public void clearAll() {
        modifiersByPlayer.clear();
    }

    private String normalizeSourceId(String value) {
        return value.trim().toLowerCase();
    }

    private static final class ListBackedEmptyCollection {
        private static final Collection<StatModifier> INSTANCE = Collections.emptyList();
    }
}
