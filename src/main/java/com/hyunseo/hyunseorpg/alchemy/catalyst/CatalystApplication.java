package com.hyunseo.hyunseorpg.alchemy.catalyst;

import java.util.Objects;

public record CatalystApplication(String potionId, String catalystId, int durationMultiplierPercent,
                                  int amplifierDelta, Delivery delivery, boolean inverted,
                                  String effectOverrideId) {
    public CatalystApplication(String potionId, String catalystId, int durationMultiplierPercent,
                               int amplifierDelta, Delivery delivery, boolean inverted) {
        this(potionId, catalystId, durationMultiplierPercent, amplifierDelta, delivery, inverted, "");
    }
    public CatalystApplication {
        Objects.requireNonNull(potionId, "potionId");
        Objects.requireNonNull(catalystId, "catalystId");
        Objects.requireNonNull(delivery, "delivery");
        effectOverrideId = effectOverrideId == null ? "" : effectOverrideId.trim();
        if (durationMultiplierPercent < 0) throw new IllegalArgumentException("durationMultiplierPercent must not be negative");
    }

    public enum Delivery { ORIGINAL, SPLASH, LINGERING }
}
