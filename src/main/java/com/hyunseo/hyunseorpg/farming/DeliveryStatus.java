package com.hyunseo.hyunseorpg.farming;

public enum DeliveryStatus {
    ACTIVE("\uC9C4\uD589 \uC911"),
    COMPLETED("\uC644\uB8CC"),
    FAILED("\uC2E4\uD328"),
    EXPIRED("\uB9CC\uB8CC");

    private final String displayName;

    DeliveryStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
