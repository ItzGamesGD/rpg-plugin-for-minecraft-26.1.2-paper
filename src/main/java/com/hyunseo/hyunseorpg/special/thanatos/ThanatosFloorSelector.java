package com.hyunseo.hyunseorpg.special.thanatos;

import java.util.List;
import java.util.function.Predicate;

/** Chooses the nearest safe surface in a bounded band around an impact, never a whole world column. */
public final class ThanatosFloorSelector {
    private ThanatosFloorSelector() { }

    public static <T> T nearestLocalFloor(List<T> topToBottom, Predicate<T> replaceableFloor,
                                          Predicate<T> clearAbove) {
        for (int index = 0; index < topToBottom.size(); index++) {
            T candidate = topToBottom.get(index);
            if (replaceableFloor.test(candidate) && (index == 0 || clearAbove.test(topToBottom.get(index - 1)))) {
                return candidate;
            }
        }
        return null;
    }
}
