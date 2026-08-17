package com.hyunseo.hyunseorpg.shop;

public enum ShopMatchMode {
    MATERIAL,
    CUSTOM_ID,
    EXACT;

    public static ShopMatchMode fromConfig(String value) {
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return MATERIAL;
        }
    }
}
