package com.hyunseo.hyunseorpg.special;

import com.hyunseo.hyunseorpg.special.flame.FlameAxeListener;
import com.hyunseo.hyunseorpg.special.thanatos.ThanatosMaceListener;
import com.hyunseo.hyunseorpg.special.thunder.ThunderAxeListener;
import com.hyunseo.hyunseorpg.special.water.WaterTridentListener;

import java.util.Set;

/** Canonical union of weapons whose listeners exclusively own their input events. */
public final class DedicatedWeaponIds {
    private static final Set<String> IDS = Set.of(
            FlameAxeListener.ID,
            WaterTridentListener.ID,
            ThanatosMaceListener.ID,
            ThunderAxeListener.ID,
            com.hyunseo.hyunseorpg.special.solaris.SolarisListener.ID,
            com.hyunseo.hyunseorpg.special.moonlit.MoonlitAfterglowListener.ID);

    private DedicatedWeaponIds() { }

    public static boolean owns(String id) {
        return IDS.contains(id);
    }

    static Set<String> all() {
        return IDS;
    }
}
