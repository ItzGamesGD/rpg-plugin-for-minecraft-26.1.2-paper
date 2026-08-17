package com.hyunseo.hyunseorpg.alchemy.potion;

public interface PotionPdcContract<I> {
    String POTION_ID_KEY = "potion_id";
    String DATA_VERSION_KEY = "data_version";
    String CATALYST_KEY = "catalyst_id";
    String CATALYST_DELIVERY_KEY = "catalyst_delivery";
    String CATALYST_DURATION_KEY = "catalyst_duration_percent";
    String CATALYST_AMPLIFIER_KEY = "catalyst_amplifier_delta";
    String CATALYST_INVERTED_KEY = "catalyst_inverted";
    String CATALYST_EFFECT_KEY = "catalyst_effect_id";
    void write(I item, PotionDefinition definition, String catalystId, int dataVersion);
    String readPotionId(I item);
    String readCatalystId(I item);
    int readDataVersion(I item);
    default void writeTransformation(I item, String delivery, int durationPercent,
                                     int amplifierDelta, boolean inverted, String effectId) { }
    default String readDelivery(I item) { return "ORIGINAL"; }
    default int readDurationPercent(I item) { return 100; }
    default int readAmplifierDelta(I item) { return 0; }
    default boolean readInverted(I item) { return false; }
    default String readEffectOverride(I item) { return ""; }
}
