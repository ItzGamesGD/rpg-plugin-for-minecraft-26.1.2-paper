package com.hyunseo.hyunseorpg.alchemy.catalyst;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable catalyst policy. Item matching is explicit and never inferred from lore. */
public final class CatalystDefinition {
    private final String catalystId;
    private final String itemId;
    private final boolean enabled;
    private final Mode mode;
    private final String materialId;
    private final List<String> allowedPotions;
    private final Map<String, String> inversionEffectIds;
    private final int durationMultiplierPercent;
    private final int amplifierDelta;
    private final String delivery;

    public CatalystDefinition(String catalystId, boolean enabled, Mode mode) {
        this(catalystId, enabled, mode, catalystId.toUpperCase(), List.of(), Map.of(),
                100, 0, "ORIGINAL");
    }

    public CatalystDefinition(String catalystId, boolean enabled, Mode mode, String materialId,
                               List<String> allowedPotions, Map<String, String> inversionEffectIds,
                               int durationMultiplierPercent, int amplifierDelta, String delivery) {
        this(catalystId, enabled, mode, materialId, "", allowedPotions, inversionEffectIds,
                durationMultiplierPercent, amplifierDelta, delivery);
    }

    public CatalystDefinition(String catalystId, boolean enabled, Mode mode, String materialId,
                               String itemId, List<String> allowedPotions, Map<String, String> inversionEffectIds,
                               int durationMultiplierPercent, int amplifierDelta, String delivery) {
        this.catalystId = Objects.requireNonNull(catalystId, "catalystId");
        this.itemId = itemId == null ? "" : itemId.trim().toLowerCase();
        this.enabled = enabled;
        this.mode = Objects.requireNonNull(mode, "mode");
        this.materialId = materialId == null ? "" : materialId.trim().toUpperCase();
        this.allowedPotions = List.copyOf(allowedPotions == null ? List.of() : allowedPotions);
        this.inversionEffectIds = Map.copyOf(inversionEffectIds == null ? Map.of() : inversionEffectIds);
        this.durationMultiplierPercent = Math.max(1, durationMultiplierPercent);
        this.amplifierDelta = amplifierDelta;
        this.delivery = delivery == null ? "ORIGINAL" : delivery.trim().toUpperCase();
    }

    public String catalystId() { return catalystId; }
    public String itemId() { return itemId; }
    public boolean enabled() { return enabled; }
    public Mode mode() { return mode; }
    public String materialId() { return materialId; }
    public List<String> allowedPotions() { return allowedPotions; }
    public Map<String, String> inversionEffectIds() { return inversionEffectIds; }
    public int durationMultiplierPercent() { return durationMultiplierPercent; }
    public int amplifierDelta() { return amplifierDelta; }
    public String delivery() { return delivery; }

    public boolean allowsPotion(String potionId) {
        return allowedPotions.isEmpty() || allowedPotions.contains(potionId);
    }

    public enum Mode { DURATION, AMPLIFIER, DELIVERY, INVERSION, SPECIAL }
}
