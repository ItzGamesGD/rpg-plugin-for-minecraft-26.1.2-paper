package com.hyunseo.hyunseorpg.alchemy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps active effects independent by target UUID and normalized effect ID.
 * Stack/reapply policy remains an EffectService concern.
 */
public final class ActiveEffectStore {
    private final Map<UUID, Map<String, ActiveEffectInstance>> byTarget = new HashMap<>();

    public ActiveEffectInstance get(UUID targetId, String effectId) {
        Map<String, ActiveEffectInstance> effects = byTarget.get(targetId);
        return effects == null ? null : effects.get(normalize(effectId));
    }

    public void put(UUID targetId, ActiveEffectInstance instance) {
        if (targetId == null || instance == null || instance.definition() == null) {
            throw new IllegalArgumentException("target and effect instance are required");
        }
        byTarget.computeIfAbsent(targetId, ignored -> new HashMap<>())
                .put(normalize(instance.definition().id()), instance);
    }

    public ActiveEffectInstance remove(UUID targetId, String effectId) {
        Map<String, ActiveEffectInstance> effects = byTarget.get(targetId);
        if (effects == null) return null;
        ActiveEffectInstance removed = effects.remove(normalize(effectId));
        if (effects.isEmpty()) byTarget.remove(targetId);
        return removed;
    }

    public List<ActiveEffectInstance> getActive(UUID targetId) {
        Map<String, ActiveEffectInstance> effects = byTarget.get(targetId);
        return effects == null ? List.of() : List.copyOf(effects.values());
    }

    public List<ActiveEffectInstance> removeAll(UUID targetId) {
        Map<String, ActiveEffectInstance> effects = byTarget.remove(targetId);
        return effects == null ? List.of() : List.copyOf(effects.values());
    }

    public boolean contains(UUID targetId, String effectId) {
        return get(targetId, effectId) != null;
    }

    public List<UUID> targetIds() {
        return List.copyOf(new ArrayList<>(byTarget.keySet()));
    }

    public int size(UUID targetId) {
        Map<String, ActiveEffectInstance> effects = byTarget.get(targetId);
        return effects == null ? 0 : effects.size();
    }

    public void clear() {
        byTarget.clear();
    }

    private static String normalize(String effectId) {
        return effectId == null ? "" : effectId.trim().toLowerCase(Locale.ROOT);
    }
}
