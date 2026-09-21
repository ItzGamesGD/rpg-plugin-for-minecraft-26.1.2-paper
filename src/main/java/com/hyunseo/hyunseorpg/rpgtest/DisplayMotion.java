package com.hyunseo.hyunseorpg.rpgtest;

import org.bukkit.entity.Display;

/**
 * Client-side smoothing for Displays that are repositioned by an encounter every server tick.
 *
 * <p>Entity collision and combat still run at the server's normal tick rate.  This only asks the
 * client to interpolate the visual teleport/transform over two ticks, which prevents an orbital
 * weapon or an ItemDisplay mirrored from an AI driver looking like a sequence of hard snaps.</p>
 */
public final class DisplayMotion {
    public static final int INTERPOLATION_TICKS = 2;

    private DisplayMotion() { }

    public static void configure(Display display) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTeleportDuration(INTERPOLATION_TICKS);
    }
}
