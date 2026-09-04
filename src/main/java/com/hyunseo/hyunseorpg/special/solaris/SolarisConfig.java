package com.hyunseo.hyunseorpg.special.solaris;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

public record SolarisConfig(
        long dawnResetTicks, double dawnRadius, double dawnDamage, int dawnFireTicks,
        double pointRange, int pointTelegraphTicks, int pointFallTicks, double pointDamage, long pointCooldownTicks,
        int wheelHoldTicks, long killWindowMillis, double orbMultiplier, int maximumOrbs, double wheelRadius,
        int maximumHits, double smallSwordDamage, int swordIntervalTicks, int orbitTicks, int smallFallTicks, long wheelCooldownTicks,
        double judgmentRadius, int sunRiseTicks, int showerTicks, int meteorCount, double meteorDirectDamage,
        double impactRadius, double explosionDamage, int judgmentFireTicks, int meteorFallTicks, long judgmentCooldownTicks) {
    public static SolarisConfig from(ConfigService c) {
        String p="special-equipment.items.solaris.abilities.";
        return new SolarisConfig(
          ticks(c,p+"dawn.reset-seconds",20), d(c,p+"dawn.radius",4), d(c,p+"dawn.damage",4), ticksI(c,p+"dawn.fire-seconds",3),
          d(c,p+"one-point.target-range",20), i(c,p+"one-point.telegraph-ticks",12), i(c,p+"one-point.fall-ticks",5), d(c,p+"one-point.damage",16), ticks(c,p+"one-point.cooldown-seconds",8),
          i(c,p+"heavenly-wheel.hold-ticks",40), (long)(d(c,p+"heavenly-wheel.kill-window-seconds",30)*1000), d(c,p+"heavenly-wheel.orb-multiplier",1.5), i(c,p+"heavenly-wheel.maximum-orbs",20), d(c,p+"heavenly-wheel.target-radius",20),
          i(c,p+"heavenly-wheel.maximum-hits-per-target",2), d(c,p+"heavenly-wheel.damage",5), i(c,p+"heavenly-wheel.sword-interval-ticks",2), i(c,p+"heavenly-wheel.orbit-ticks",30), i(c,p+"heavenly-wheel.fall-ticks",3), ticks(c,p+"heavenly-wheel.cooldown-seconds",20),
          d(c,p+"judgment.effect-radius",30), i(c,p+"judgment.sun-rise-ticks",50), i(c,p+"judgment.shower-ticks",160), i(c,p+"judgment.meteor-count",30), d(c,p+"judgment.direct-damage",8),
          d(c,p+"judgment.impact-radius",2.5), d(c,p+"judgment.explosion-damage",5), ticksI(c,p+"judgment.fire-seconds",5), i(c,p+"judgment.meteor-fall-ticks",10), ticks(c,p+"judgment.cooldown-seconds",60));
    }
    private static double d(ConfigService c,String p,double f){return c.getSpecialEquipmentDouble(p,f);} private static int i(ConfigService c,String p,int f){return Math.max(1,c.getSpecialEquipmentInt(p,f));}
    private static long ticks(ConfigService c,String p,double f){return Math.max(1,Math.round(d(c,p,f)*20));} private static int ticksI(ConfigService c,String p,double f){return (int)ticks(c,p,f);}
}
