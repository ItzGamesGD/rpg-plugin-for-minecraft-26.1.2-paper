package com.hyunseo.hyunseorpg.alchemy.potion;

import java.util.Locale;
import java.util.Objects;

public final class PotionDefinition {
    private final String id, displayName, effectId, outputItemId;
    private final Delivery delivery;
    private final boolean enabled;
    public PotionDefinition(String id, String displayName, Delivery delivery, String effectId, boolean enabled) {
        this(id, displayName, delivery, effectId, id, enabled);
    }
    public PotionDefinition(String id, String displayName, Delivery delivery, String effectId,
                            String outputItemId, boolean enabled) {
        this.id = requireId(id); this.displayName = Objects.requireNonNull(displayName); this.delivery = Objects.requireNonNull(delivery);
        this.effectId = requireId(effectId); this.outputItemId = requireId(outputItemId); this.enabled = enabled;
    }
    public String id() { return id; }
    public String displayName() { return displayName; }
    public String effectId() { return effectId; }
    public Delivery delivery() { return delivery; }
    public boolean enabled() { return enabled; }
    public String outputItemId() { return outputItemId; }
    private static String requireId(String value) { if (value == null || !value.matches("[a-z0-9]+(_[a-z0-9]+)*")) throw new IllegalArgumentException("Invalid potion id: " + value); return value.toLowerCase(Locale.ROOT); }
    public enum Delivery { DRINK, SPLASH, LINGERING }
}
