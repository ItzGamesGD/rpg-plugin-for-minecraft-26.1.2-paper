package com.hyunseo.hyunseorpg.mob;

public record MobAttributeData(
        double maxHealth,
        double movementSpeed,
        double attackDamage,
        double armor,
        double scale,
        String movementSpeedMode,
        double movementSpeedValue,
        double knockbackResistance
) {
    public MobAttributeData(
            double maxHealth,
            double movementSpeed,
            double attackDamage,
            double armor,
            double scale
    ) {
        this(maxHealth, movementSpeed, attackDamage, armor, scale,
                "ABSOLUTE", movementSpeed, 0.0D);
    }

    public MobAttributeData {
        movementSpeedMode = movementSpeedMode == null || movementSpeedMode.isBlank()
                ? "ABSOLUTE"
                : movementSpeedMode.trim().toUpperCase(java.util.Locale.ROOT);
        movementSpeedValue = movementSpeedValue > 0.0D ? movementSpeedValue : movementSpeed;
    }

    public boolean hasMaxHealth() {
        return maxHealth > 0.0D;
    }

    public boolean hasMovementSpeed() {
        return movementSpeed > 0.0D || movementSpeedValue > 0.0D;
    }

    public boolean multipliesMovementSpeed() {
        return "MULTIPLIER".equals(movementSpeedMode);
    }

    public boolean hasKnockbackResistance() {
        return knockbackResistance > 0.0D;
    }

    public boolean hasAttackDamage() {
        return attackDamage > 0.0D;
    }

    public boolean hasArmor() {
        return armor > 0.0D;
    }

    public boolean hasScale() {
        return scale > 0.0D;
    }
}
