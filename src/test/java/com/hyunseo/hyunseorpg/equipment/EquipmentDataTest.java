package com.hyunseo.hyunseorpg.equipment;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

final class EquipmentDataTest {
    @Test
    void dataSnapshotKeepsEnhancementIndependentOfRetiredPromotionFields() {
        EquipmentData data = new EquipmentData("basic_sword", EquipmentTierService.Category.WEAPON,
                12, List.of("blade_throw"), 7,
                Set.of("hyunseorpg:item_id", "hyunseorpg:enhancement_level"), 1);
        assertEquals("basic_sword", data.itemId());
        assertEquals(12, data.upgradeLevel());
        assertEquals(List.of("blade_throw"), data.enchantData());
        assertEquals(7, data.killCount());
        assertTrue(data.customFlags().contains("hyunseorpg:enhancement_level"));
    }
}
