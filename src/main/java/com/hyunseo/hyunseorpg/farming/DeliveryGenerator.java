package com.hyunseo.hyunseorpg.farming;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Rolls one immutable requirement from a provider snapshot. */
public final class DeliveryGenerator {
    private final DeliveryRegistry registry;

    public DeliveryGenerator(DeliveryRegistry registry) {
        this.registry = registry;
    }

    public Optional<FarmingDeliveryState> generate(DeliveryProvider provider, long now) {
        List<DeliveryDefinition> definitions = registry.forProvider(provider).stream()
                .filter(definition -> !definition.itemFamilies().isEmpty())
                .toList();
        if (definitions.isEmpty()) return Optional.empty();
        DeliveryDefinition definition = weightedDefinition(definitions);
        List<String> families = definition.itemFamilies();
        String family = families.get(ThreadLocalRandom.current().nextInt(families.size()));
        int amount = definition.amountMin();
        if (definition.amountMax() > definition.amountMin()) {
            amount += ThreadLocalRandom.current().nextInt(definition.amountMax() - definition.amountMin() + 1);
        }
        long expiresAt = addSeconds(now, registry.timeLimitSeconds());
        return Optional.of(FarmingDeliveryState.active(UUID.randomUUID().toString(), definition.id(), family,
                amount, CropQuality.NORMAL, now, expiresAt));
    }

    static long addSeconds(long timestamp, long seconds) {
        long safeSeconds = Math.max(1L, seconds);
        if (safeSeconds > Long.MAX_VALUE / 1000L) return Long.MAX_VALUE;
        long millis = safeSeconds * 1000L;
        return timestamp > Long.MAX_VALUE - millis ? Long.MAX_VALUE : timestamp + millis;
    }

    static DeliveryDefinition weightedDefinition(List<DeliveryDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) return null;
        int totalWeight = definitions.stream().mapToInt(definition -> Math.max(0, definition.weight())).sum();
        if (totalWeight <= 0) return definitions.get(ThreadLocalRandom.current().nextInt(definitions.size()));
        int pick = ThreadLocalRandom.current().nextInt(totalWeight);
        for (DeliveryDefinition definition : definitions) {
            pick -= Math.max(0, definition.weight());
            if (pick < 0) return definition;
        }
        return definitions.getLast();
    }
}
