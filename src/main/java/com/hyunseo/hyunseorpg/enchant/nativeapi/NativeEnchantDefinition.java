package com.hyunseo.hyunseorpg.enchant.nativeapi;

/** Bootstrap-safe metadata for a Hyunseo enchantment registry entry. */
public record NativeEnchantDefinition(String id, String displayName, int maxLevel, int weight,
                                      int minimumBaseCost, int minimumPerLevelCost,
                                      int maximumBaseCost, int maximumPerLevelCost,
                                      int anvilCost, String supportedItemTag, String exclusiveSetTag,
                                      AcquisitionPolicy acquisition) {
    public NativeEnchantDefinition {
        if (acquisition == null) throw new IllegalArgumentException("Explicit acquisition policy required: " + id);
    }
}
