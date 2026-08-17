package com.hyunseo.hyunseorpg.equipment;

/**
 * Numeric equipment grade reserved for the equipment data model.
 * Existing named promotion grades are intentionally kept separate.
 */
public enum EquipmentGrade {
    UNSPECIFIED(0),
    GRADE_1(1),
    GRADE_2(2),
    GRADE_3(3),
    GRADE_4(4),
    GRADE_5(5);

    private final int value;

    EquipmentGrade(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public boolean isSpecified() {
        return this != UNSPECIFIED;
    }

    public static EquipmentGrade fromValue(int value) {
        return switch (value) {
            case 1 -> GRADE_1;
            case 2 -> GRADE_2;
            case 3 -> GRADE_3;
            case 4 -> GRADE_4;
            case 5 -> GRADE_5;
            default -> UNSPECIFIED;
        };
    }
}
