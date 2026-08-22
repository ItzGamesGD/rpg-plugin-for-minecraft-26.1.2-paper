package com.hyunseo.hyunseorpg.mob;

/** Small pure policy boundary for the first outpost-raider behaviors. */
public final class OutpostRaiderPolicy {
    private OutpostRaiderPolicy() { }

    public static double guardDamageMultiplier(double configured) {
        return Math.max(0.4D, Math.min(0.6D, configured));
    }

    public static boolean blocksEvokerSpell(String spellName) {
        return spellName != null && spellName.toUpperCase(java.util.Locale.ROOT).contains("VEX");
    }

    public static boolean isMountedHeavyUnit(String mobId) {
        return "ravager_rider".equals(mobId == null ? "" : mobId.trim().toLowerCase(java.util.Locale.ROOT));
    }
}
