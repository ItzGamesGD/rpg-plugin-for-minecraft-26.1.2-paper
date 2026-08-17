package com.hyunseo.hyunseorpg.equipment;

/** Registry metadata only. No gameplay values are stored here. */
public record EquipmentDefinition(
        String equipmentId,
        EquipmentTierService.Category equipmentType,
        EquipmentGrade grade,
        boolean upgradeAllowed,
        boolean promotionAllowed,
        boolean specialEquipment,
        boolean endgameEquipment
) {
    public EquipmentDefinition {
        equipmentId = equipmentId == null ? "" : equipmentId;
        equipmentType = equipmentType == null ? EquipmentTierService.Category.UNSUPPORTED : equipmentType;
        grade = grade == null ? EquipmentGrade.UNSPECIFIED : grade;
    }
}
