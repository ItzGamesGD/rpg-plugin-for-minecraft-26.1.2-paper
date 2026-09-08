package com.hyunseo.hyunseorpg.prototype.thousandeyes;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Pure, canonical animation calculations used by the runtime rig and unit tests. */
public final class ThousandEyesMath {
    private ThousandEyesMath() { }

    public static Vector3f orbit(Vector3f center, Vector3f forward, double angle, double radius, double height) {
        Vector3f f = horizontal(forward);
        Vector3f right = new Vector3f(-f.z, 0.0F, f.x);
        return new Vector3f(center).add(new Vector3f(right).mul((float) (Math.cos(angle) * radius)))
                .add(new Vector3f(f).mul((float) (Math.sin(angle) * radius))).add(0.0F, (float) height, 0.0F);
    }

    public static Vector3f horizontal(Vector3f direction) {
        Vector3f value = new Vector3f(direction.x, 0.0F, direction.z);
        return value.lengthSquared() < 1.0E-6F ? new Vector3f(0, 0, 1) : value.normalize();
    }

    /** Rotates the display's local +Z normal to the requested direction without Euler composition. */
    public static Quaternionf facing(Vector3f direction, boolean verticalSlab) {
        Vector3f normal = new Vector3f(direction);
        if (normal.lengthSquared() < 1.0E-6F) normal.set(0, 0, 1); else normal.normalize();
        Quaternionf look = new Quaternionf().rotationTo(new Vector3f(0, 0, 1), normal);
        return verticalSlab ? look.rotateLocalX((float) (Math.PI / 2.0D)) : look;
    }

    public static double lerp(double from, double to, double progress) {
        double p = Math.max(0.0D, Math.min(1.0D, progress));
        return from + (to - from) * (p * p * (3.0D - 2.0D * p));
    }

    public static List<Integer> shuffledOrder(long seed, int size) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < size; i++) order.add(i);
        Collections.shuffle(order, new Random(seed));
        return List.copyOf(order);
    }
}
