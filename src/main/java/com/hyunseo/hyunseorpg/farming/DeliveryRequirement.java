package com.hyunseo.hyunseorpg.farming;

/** A requirement is an item family, not a display-name or a single stack identity. */
public record DeliveryRequirement(String itemFamily, int amount, CropQuality minimumQuality) {
    public DeliveryRequirement {
        if (itemFamily == null || itemFamily.isBlank()) throw new IllegalArgumentException("itemFamily must not be blank");
        itemFamily = itemFamily.trim().toLowerCase(java.util.Locale.ROOT);
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        minimumQuality = minimumQuality == null ? CropQuality.NORMAL : minimumQuality;
    }
}
