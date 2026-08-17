package com.hyunseo.hyunseorpg.alchemy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory attribution boundary for future direct/DoT/area/reflected/chain damage.
 * It is intentionally not wired to loot or boss rewards until those source paths
 * are verified in a later unit.
 */
public final class CombatEffectAttributionService {
    private final Map<Key, Application> applications = new HashMap<>();
    private final Map<Key, List<Contribution>> contributions = new HashMap<>();

    public synchronized void recordApplication(UUID sourceId, UUID targetId, String effectId, long appliedAtTick) {
        if (sourceId == null || targetId == null || effectId == null || effectId.isBlank()) return;
        applications.put(new Key(sourceId, targetId, normalize(effectId)),
                new Application(sourceId, targetId, normalize(effectId), appliedAtTick));
    }

    public synchronized void recordContribution(UUID sourceId, UUID targetId, String effectId,
                                                 CombatEffectHandler.DamageKind kind, double amount) {
        if (sourceId == null || targetId == null || effectId == null || kind == null || amount <= 0.0D) return;
        Key key = new Key(sourceId, targetId, normalize(effectId));
        contributions.computeIfAbsent(key, ignored -> new ArrayList<>())
                .add(new Contribution(sourceId, targetId, normalize(effectId), kind, amount));
    }

    public synchronized List<Contribution> contributions(UUID sourceId, UUID targetId, String effectId) {
        return Collections.unmodifiableList(new ArrayList<>(contributions.getOrDefault(
                new Key(sourceId, targetId, normalize(effectId)), List.of())));
    }

    public synchronized void clear(UUID sourceId, UUID targetId, String effectId) {
        Key key = new Key(sourceId, targetId, normalize(effectId));
        applications.remove(key);
        contributions.remove(key);
    }

    public synchronized void clearAll() {
        applications.clear();
        contributions.clear();
    }

    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT); }

    private record Key(UUID sourceId, UUID targetId, String effectId) { }
    public record Application(UUID sourceId, UUID targetId, String effectId, long appliedAtTick) { }
    public record Contribution(UUID sourceId, UUID targetId, String effectId,
                               CombatEffectHandler.DamageKind kind, double amount) { }
}
