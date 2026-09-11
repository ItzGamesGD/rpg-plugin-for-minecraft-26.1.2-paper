package com.hyunseo.hyunseorpg.prototype.thousandeyes;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Pure, canonical animation calculations used by the runtime rig and unit tests. */
public final class ThousandEyesMath {
    public enum VisualElement {
        ITEM_EYE(new Vector3f(0, 0, -1)),
        BLOCK_FACE(new Vector3f(0, 0, 1)),
        HORIZONTAL_SLAB(new Vector3f(0, 1, 0));

        private final Vector3f localForward;

        VisualElement(Vector3f localForward) {
            this.localForward = localForward;
        }

        public Vector3f localForward() {
            return new Vector3f(localForward);
        }
    }

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

    /** Applies an element-specific local-axis correction before aiming the visual. */
    public static Quaternionf visualFacing(Vector3f direction, VisualElement element) {
        Vector3f aim = normalizedOr(direction, new Vector3f(0, 0, 1));
        return new Quaternionf().rotationTo(element.localForward(), aim);
    }

    /** Full 3D attack direction; a zero-length snapshot gets a stable horizontal fallback. */
    public static Vector3f attackAim(Vector3f origin, Vector3f target) {
        return normalizedOr(new Vector3f(target).sub(origin), new Vector3f(0, 0, 1));
    }

    /** Returns an independent 3D movement vector, preserving vertical displacement. */
    public static Vector3f movementDelta(Vector3f start, Vector3f destination) {
        return new Vector3f(destination).sub(start);
    }

    public static Vector3f dashPosition(Vector3f start, Vector3f destination, double progress) {
        float p = (float) Math.max(0.0D, Math.min(1.0D, progress));
        return new Vector3f(start).add(movementDelta(start, destination).mul(p));
    }

    private static Vector3f normalizedOr(Vector3f value, Vector3f fallback) {
        Vector3f copy = new Vector3f(value);
        return copy.lengthSquared() < 1.0E-6F ? new Vector3f(fallback) : copy.normalize();
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
