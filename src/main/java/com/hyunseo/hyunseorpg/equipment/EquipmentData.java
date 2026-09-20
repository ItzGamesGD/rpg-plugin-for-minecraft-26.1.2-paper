package com.hyunseo.hyunseorpg.equipment;

import java.util.List;
import java.util.Set;

/**
 * Read-only snapshot of the common equipment instance data.
 * It is a view over existing PDC fields; it is not used to recalculate effects.
 */
public record EquipmentData(
        String itemId,
        EquipmentTierService.Category equipmentType,
        int upgradeLevel,
        List<String> enchantData,
        long killCount,
        Set<String> customFlags,
        int dataVersion
) {
    public EquipmentData {
        itemId = itemId == null ? "" : itemId;
        equipmentType = equipmentType == null ? EquipmentTierService.Category.UNSUPPORTED : equipmentType;
        upgradeLevel = Math.max(0, upgradeLevel);
        enchantData = List.copyOf(enchantData == null ? List.of() : enchantData);
        killCount = Math.max(0L, killCount);
        customFlags = Set.copyOf(customFlags == null ? Set.of() : customFlags);
        dataVersion = Math.max(0, dataVersion);
    }
}
