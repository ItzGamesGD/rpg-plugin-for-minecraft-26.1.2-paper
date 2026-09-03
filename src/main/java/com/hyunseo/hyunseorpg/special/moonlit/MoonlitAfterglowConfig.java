package com.hyunseo.hyunseorpg.special.moonlit;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

public record MoonlitAfterglowConfig(double baseAttackSpeed, double passiveDamage, double damageReduction,
        int mitigationTicks, double yugwangCooldownSeconds, double yugwangDistance, double yugwangVelocity,
        double moonFlashDamage, double moonFlashDistance, double moonFlashCooldownSeconds, double slashSpeed,
        double moonShadowCooldownSeconds, int teleportCount, int cadenceTicks, double minimumRadius,
        double maximumRadius, int candidateRetries, double shadowSlashDamage, int finalTimeoutTicks) {
    public static MoonlitAfterglowConfig from(ConfigService config) {
        String p = "special-equipment.items.moonlit_afterglow.abilities.";
        return new MoonlitAfterglowConfig(
                config.getSpecialEquipmentDouble(p + "base-attack-speed", 6.4),
                config.getSpecialEquipmentDouble(p + "passive.additional-damage", 1.5),
                config.getSpecialEquipmentDouble(p + "passive.damage-reduction", .30),
                Math.max(1, config.getSpecialEquipmentInt(p + "passive.mitigation-duration-ticks", 3)),
                config.getSpecialEquipmentDouble(p + "yugwang.cooldown-seconds", 3),
                config.getSpecialEquipmentDouble(p + "yugwang.distance", 2.4),
                config.getSpecialEquipmentDouble(p + "yugwang.velocity", 1.2),
                config.getSpecialEquipmentDouble(p + "moon-flash.damage", 3),
                config.getSpecialEquipmentDouble(p + "moon-flash.distance", 8),
                config.getSpecialEquipmentDouble(p + "moon-flash.cooldown-seconds", 8),
                config.getSpecialEquipmentDouble(p + "moon-flash.trailing-slash-speed", 1),
                config.getSpecialEquipmentDouble(p + "moon-shadow.cooldown-seconds", 24),
                Math.max(1, config.getSpecialEquipmentInt(p + "moon-shadow.teleport-count", 10)),
                Math.max(1, config.getSpecialEquipmentInt(p + "moon-shadow.cadence-ticks", 2)),
                config.getSpecialEquipmentDouble(p + "moon-shadow.minimum-radius", 2),
                config.getSpecialEquipmentDouble(p + "moon-shadow.maximum-radius", 12),
                Math.max(1, config.getSpecialEquipmentInt(p + "moon-shadow.candidate-retries", 8)),
                config.getSpecialEquipmentDouble(p + "moon-shadow.slash-damage", 2),
                Math.max(1, config.getSpecialEquipmentInt(p + "moon-shadow.final-trigger-timeout-ticks", 200)));
    }
}
