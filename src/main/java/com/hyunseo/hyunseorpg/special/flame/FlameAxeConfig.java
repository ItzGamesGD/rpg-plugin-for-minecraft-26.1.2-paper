package com.hyunseo.hyunseorpg.special.flame;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

public record FlameAxeConfig(int fullChargeTicks, double heavyDamage, double heavyRange,
                             double heavyAngleCosine, int heavyFireTicks, double heavyKnockback,
                             int maxTargets, double searchRadius, double displayScale,
                             double spinDegrees, double speed, double turnRadians,
                             double contactRadius, double hitRotation, double spinDamage,
                             int lifetimeTicks, double returnDistance) {
    public static FlameAxeConfig from(ConfigService config) {
        String p = "special-equipment.items.flame_axe.abilities.";
        return new FlameAxeConfig(config.getSpecialEquipmentInt(p + "heavy-attack.full-charge-ticks", 20),
                config.getSpecialEquipmentDouble(p + "heavy-attack.damage", 7),
                config.getSpecialEquipmentDouble(p + "heavy-attack.range", 4),
                config.getSpecialEquipmentDouble(p + "heavy-attack.angle-cosine", .55),
                config.getSpecialEquipmentInt(p + "heavy-attack.fire-ticks", 80),
                config.getSpecialEquipmentDouble(p + "heavy-attack.knockback", .65),
                config.getSpecialEquipmentInt(p + "spinning-throw.max-targets", 5),
                config.getSpecialEquipmentDouble(p + "spinning-throw.search-radius", 8),
                config.getSpecialEquipmentDouble(p + "spinning-throw.display-scale", .8),
                config.getSpecialEquipmentDouble(p + "spinning-throw.spin-degrees-per-tick", 30),
                config.getSpecialEquipmentDouble(p + "spinning-throw.travel-speed", .7),
                config.getSpecialEquipmentDouble(p + "spinning-throw.turn-radians-per-tick", .16),
                config.getSpecialEquipmentDouble(p + "spinning-throw.contact-radius", 1.1),
                config.getSpecialEquipmentDouble(p + "spinning-throw.hit-rotation-requirement", 360),
                config.getSpecialEquipmentDouble(p + "spinning-throw.damage", 4),
                config.getSpecialEquipmentInt(p + "spinning-throw.max-lifetime-ticks", 200),
                config.getSpecialEquipmentDouble(p + "spinning-throw.return-distance", 1.2));
    }
}
