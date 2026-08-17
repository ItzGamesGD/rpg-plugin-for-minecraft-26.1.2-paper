package com.hyunseo.hyunseorpg.farming;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Owns delivery lifecycle and persistence; it never scans or deducts inventory. */
public final class DeliveryService {
    private final DeliveryDataService data;
    private final DeliveryRegistry registry;
    private final DeliveryGenerator generator;

    public DeliveryService(DeliveryDataService data, DeliveryRegistry registry) {
        this.data = Objects.requireNonNull(data, "data");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.generator = new DeliveryGenerator(registry);
    }

    public Optional<DeliverySession> getOrCreate(UUID playerId, DeliveryProvider provider) {
        if (playerId == null || provider == null || provider == DeliveryProvider.ESTATE_RESERVED) return Optional.empty();
        long now = System.currentTimeMillis();
        FarmingDeliveryState existing = data.get(playerId, provider).orElse(null);
        if (existing != null) {
            if (existing.status() == DeliveryStatus.ACTIVE && existing.expiresAt() > now) {
                return toSession(existing);
            }
            if (existing.status() == DeliveryStatus.ACTIVE) {
                if (!data.expire(playerId, provider, existing.deliveryId(), now)) return Optional.empty();
                existing = data.get(playerId, provider).orElse(null);
            }
            if (existing != null && nextAvailableAt(existing) > now) {
                return toSession(existing);
            }
            if (existing != null && !data.clear(playerId, provider)) return Optional.empty();
        }
        Optional<FarmingDeliveryState> generated = generator.generate(provider, now);
        if (generated.isEmpty()) return Optional.empty();
        FarmingDeliveryState state = generated.orElseThrow();
        if (!data.save(playerId, provider, state)) {
            return Optional.empty();
        }
        return toSession(state);
    }

    public Optional<DeliverySession> current(UUID playerId, DeliveryProvider provider) {
        if (playerId == null || provider == null) return Optional.empty();
        FarmingDeliveryState state = data.get(playerId, provider).orElse(null);
        if (state == null || state.status() != DeliveryStatus.ACTIVE) return Optional.empty();
        if (state.expiresAt() <= System.currentTimeMillis()) {
            expire(playerId, provider, state.deliveryId());
            return Optional.empty();
        }
        return toSession(state);
    }

    public boolean complete(UUID playerId, DeliveryProvider provider, String deliveryId) {
        if (playerId == null || provider == null || deliveryId == null) return false;
        return data.completeWithRewards(playerId, provider, deliveryId, 0L, 0L,
                System.currentTimeMillis());
    }

    public boolean complete(UUID playerId, DeliveryProvider provider, String deliveryId,
                            long points, long favorIncrease) {
        if (playerId == null || provider == null || deliveryId == null) return false;
        return data.completeWithRewards(playerId, provider, deliveryId, points, favorIncrease,
                System.currentTimeMillis());
    }

    public boolean expire(UUID playerId, DeliveryProvider provider, String deliveryId) {
        if (playerId == null || provider == null) return false;
        return data.expire(playerId, provider, deliveryId, System.currentTimeMillis());
    }

    /** Admin-only lifecycle operation. It never changes registry definitions. */
    public boolean adminExpire(UUID playerId, DeliveryProvider provider) {
        FarmingDeliveryState state = data.get(playerId, provider).orElse(null);
        return state != null && state.status() == DeliveryStatus.ACTIVE
                && data.expire(playerId, provider, state.deliveryId(), System.currentTimeMillis());
    }

    public boolean adminComplete(UUID playerId, DeliveryProvider provider) {
        FarmingDeliveryState state = data.get(playerId, provider).orElse(null);
        return state != null && state.status() == DeliveryStatus.ACTIVE
                && complete(playerId, provider, state.deliveryId());
    }

    /** Clears and regenerates only the selected player's delivery. */
    public boolean adminReroll(UUID playerId, DeliveryProvider provider) {
        FarmingDeliveryState previous = data.get(playerId, provider).orElse(null);
        if (!data.clear(playerId, provider)) return false;
        if (getOrCreate(playerId, provider).isPresent()) return true;
        if (previous != null) data.save(playerId, provider, previous);
        return false;
    }

    public int completedCount(UUID playerId, DeliveryProvider provider) {
        return data.completedCount(playerId, provider);
    }

    public java.util.List<String> lastRegistryErrors() { return registry.lastErrors(); }

    public long refreshSeconds() { return registry.refreshSeconds(); }

    public long timeLimitSeconds() { return registry.timeLimitSeconds(); }

    private Optional<DeliverySession> toSession(FarmingDeliveryState state) {
        DeliveryDefinition definition = registry.get(state.definitionId()).orElse(null);
        if (definition == null) return Optional.empty();
        return Optional.of(new DeliverySession(state.deliveryId(), definition,
                new DeliveryRequirement(state.itemFamily(), state.requiredAmount(), state.minimumQuality()),
                state.createdAt(), state.expiresAt(), state.status(), nextAvailableAt(state)));
    }

    private long nextAvailableAt(FarmingDeliveryState state) {
        if (state.status() == DeliveryStatus.ACTIVE) return state.expiresAt();
        if (state.statusAt() <= 0L) return 0L;
        long refreshSeconds = Math.max(1L, registry.refreshSeconds());
        if (refreshSeconds > Long.MAX_VALUE / 1000L) return Long.MAX_VALUE;
        long refreshMillis = refreshSeconds * 1000L;
        if (state.statusAt() > Long.MAX_VALUE - refreshMillis) return Long.MAX_VALUE;
        return state.statusAt() + refreshMillis;
    }
}
