package com.hyunseo.hyunseorpg.special.thanatos;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

/** Immutable runtime snapshot of the YAML-owned Thanatos values. */
public record ThanatosConfig(
        int mortalDelayTicks,
        int mortalFallTicks,
        double mortalDamage,
        int mortalModel,
        int oppressionCooldownTicks,
        double oppressionRadius,
        int oppressionDurationTicks,
        double oppressionDamage,
        int sentenceCooldownTicks,
        double sentenceRange,
        double sentenceCosine,
        int sentenceFallTicks,
        double groundRadius,
        int groundDurationTicks,
        int witherDurationTicks,
        int witherAmplifier,
        int ultimatumCooldownTicks,
        int chargeTicks,
        double launchVelocity,
        double impactRadius,
        double impactDamage) {

    public static ThanatosConfig from(ConfigService config) {
        String path = "special-equipment.items.thanatos_mace.abilities.";
        return new ThanatosConfig(
                positive(config.getSpecialEquipmentInt(path + "mortal.delay-ticks", 100)),
                positive(config.getSpecialEquipmentInt(path + "mortal.fall-ticks", 8)),
                nonNegative(config.getSpecialEquipmentDouble(path + "mortal.damage", 6.0D)),
                Math.max(0, config.getSpecialEquipmentInt(path + "mortal.sword-custom-model-data", 23601)),
                positive(config.getSpecialEquipmentInt(path + "deaths-oppression.cooldown-ticks", 200)),
                nonNegative(config.getSpecialEquipmentDouble(path + "deaths-oppression.radius", 5.0D)),
                positive(config.getSpecialEquipmentInt(path + "deaths-oppression.duration-ticks", 40)),
                nonNegative(config.getSpecialEquipmentDouble(path + "deaths-oppression.damage", 4.0D)),
                positive(config.getSpecialEquipmentInt(path + "death-sentence.cooldown-ticks", 300)),
                nonNegative(config.getSpecialEquipmentDouble(path + "death-sentence.target-range", 24.0D)),
                clamp(config.getSpecialEquipmentDouble(path + "death-sentence.minimum-facing-cosine", 0.35D), -1.0D, 1.0D),
                positive(config.getSpecialEquipmentInt(path + "death-sentence.telegraph-ticks", 24)),
                nonNegative(config.getSpecialEquipmentDouble(path + "death-sentence.ground-radius", 5.0D)),
                positive(config.getSpecialEquipmentInt(path + "death-sentence.ground-duration-ticks", 120)),
                positive(config.getSpecialEquipmentInt(path + "death-sentence.wither-duration-ticks", 100)),
                Math.max(0, config.getSpecialEquipmentInt(path + "death-sentence.wither-amplifier", 1)),
                positive(config.getSpecialEquipmentInt(path + "ultimatum.cooldown-ticks", 600)),
                positive(config.getSpecialEquipmentInt(path + "ultimatum.charge-ticks", 15)),
                nonNegative(config.getSpecialEquipmentDouble(path + "ultimatum.launch-velocity", 1.55D)),
                nonNegative(config.getSpecialEquipmentDouble(path + "ultimatum.impact-radius", 10.0D)),
                nonNegative(config.getSpecialEquipmentDouble(path + "ultimatum.damage", 12.0D)));
    }

    private static int positive(int value) { return Math.max(1, value); }
    private static double nonNegative(double value) { return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D; }
    private static double clamp(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
}
