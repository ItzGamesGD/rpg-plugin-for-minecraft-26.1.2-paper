package com.hyunseo.hyunseorpg.classsystem;

public record ClassSelectionResult(Status status, RPGClass selectedClass) {
    public static ClassSelectionResult selected(RPGClass selectedClass) {
        return new ClassSelectionResult(Status.SELECTED, selectedClass);
    }

    public static ClassSelectionResult alreadySelected(RPGClass selectedClass) {
        return new ClassSelectionResult(Status.ALREADY_SELECTED, selectedClass);
    }

    public enum Status {
        SELECTED,
        ALREADY_SELECTED
    }
}
