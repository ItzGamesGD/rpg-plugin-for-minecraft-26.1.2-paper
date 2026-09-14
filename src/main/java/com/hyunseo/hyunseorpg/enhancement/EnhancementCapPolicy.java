package com.hyunseo.hyunseorpg.enhancement;

import java.util.OptionalInt;

/** Pure category-cap transition used by previews, clicks and tests. */
public final class EnhancementCapPolicy {
    private EnhancementCapPolicy() { }

    public static OptionalInt nextLevel(int currentLevel, int maximumLevel) {
        int current = Math.max(0, currentLevel);
        if (maximumLevel <= 0 || current >= maximumLevel) return OptionalInt.empty();
        return OptionalInt.of(current + 1);
    }
}
