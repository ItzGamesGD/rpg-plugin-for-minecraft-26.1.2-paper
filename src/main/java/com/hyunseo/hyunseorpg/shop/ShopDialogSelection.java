package com.hyunseo.hyunseorpg.shop;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Converts untrusted dialog slider values into validated product quantities. */
final class ShopDialogSelection {
    static final int HARD_CAP = 64;

    private ShopDialogSelection() {
    }

    static Map<Integer, Integer> read(List<Integer> bundleSizes,
                                      Function<String, Float> input,
                                      Function<Integer, Integer> currentMaximum) {
        Map<Integer, Integer> selected = new LinkedHashMap<>();
        for (int index = 0; index < bundleSizes.size(); index++) {
            Float raw = input.apply(inputKey(index));
            if (raw == null || !Float.isFinite(raw) || raw <= 0.0F || raw != Math.rint(raw)) continue;
            int quantity = raw.intValue();
            int maximum = Math.min(HARD_CAP, Math.max(0, currentMaximum.apply(index)));
            if (quantity > maximum || quantity % bundleSizes.get(index) != 0) continue;
            selected.put(index, quantity);
        }
        return selected;
    }

    static String inputKey(int index) {
        return "quantity_" + index;
    }
}
