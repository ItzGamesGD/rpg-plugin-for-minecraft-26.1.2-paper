package com.hyunseo.hyunseorpg.enchant.nativeapi;

/** Minecraft selection pools are tag membership, independent from weight and enchanting cost. */
public record AcquisitionPolicy(boolean enchantingTable, boolean tradeable, boolean randomLoot,
                                TreasurePolicy treasurePolicy) {
    public AcquisitionPolicy {
        if (treasurePolicy == null) throw new IllegalArgumentException("treasurePolicy is required");
        if (treasurePolicy == TreasurePolicy.TREASURE && enchantingTable) {
            throw new IllegalArgumentException("treasure enchantments cannot be enchanting-table candidates");
        }
    }

    public enum TreasurePolicy { TREASURE, NON_TREASURE }

    public static AcquisitionPolicy common() {
        return new AcquisitionPolicy(true, true, true, TreasurePolicy.NON_TREASURE);
    }

    public static AcquisitionPolicy treasure() {
        return new AcquisitionPolicy(false, true, true, TreasurePolicy.TREASURE);
    }
}
