package com.hyunseo.hyunseorpg.equipment;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EquipmentDataTest {
    @Test
    void gradeValuesAreLimitedToTheReservedFiveGradeModel() {
        assertEquals(EquipmentGrade.UNSPECIFIED, EquipmentGrade.fromValue(0));
        assertEquals(EquipmentGrade.GRADE_1, EquipmentGrade.fromValue(1));
        assertEquals(EquipmentGrade.GRADE_5, EquipmentGrade.fromValue(5));
        assertEquals(EquipmentGrade.UNSPECIFIED, EquipmentGrade.fromValue(6));
    }

    @Test
    void dataSnapshotNormalizesCollectionsWithoutChangingIndependentFields() {
        EquipmentData data = new EquipmentData(
                "basic_sword",
                EquipmentTierService.Category.WEAPON,
                EquipmentGrade.GRADE_2,
                12,
                "normal-3",
                "normal",
                List.of("blade_throw"),
                7,
                Set.of("hyunseorpg:item_id", "hyunseorpg:enhancement_level"),
                1
        );

        assertEquals("basic_sword", data.itemId());
        assertEquals(EquipmentGrade.GRADE_2, data.grade());
        assertEquals(12, data.upgradeLevel());
        assertEquals("normal-3", data.promotionLevel());
        assertEquals(List.of("blade_throw"), data.enchantData());
        assertEquals(7, data.killCount());
        assertTrue(data.customFlags().contains("hyunseorpg:item_id"));
    }
}
