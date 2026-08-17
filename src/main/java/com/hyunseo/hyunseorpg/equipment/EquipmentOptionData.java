package com.hyunseo.hyunseorpg.equipment;

public record EquipmentOptionData(
        String id,
        String displayName,
        EquipmentOptionType type,
        double value,
        int slotCost,
        String requiredMaterialId
) {
}
