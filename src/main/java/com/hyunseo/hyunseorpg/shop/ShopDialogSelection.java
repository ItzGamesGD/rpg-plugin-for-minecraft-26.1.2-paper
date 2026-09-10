package com.hyunseo.hyunseorpg.shop;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Strictly validates the complete, untrusted dialog response. Only zero means skip. */
final class ShopDialogSelection {
    static final int HARD_CAP = 64;

    record Result(Map<Integer, Integer> quantities, String error) {
        boolean valid() {
            return error == null;
        }
    }

    private ShopDialogSelection() {
    }

    static Result read(List<Integer> bundleSizes, Function<String, Float> input) {
        Map<Integer, Integer> selected = new LinkedHashMap<>();
        for (int index = 0; index < bundleSizes.size(); index++) {
            Float raw = input.apply(inputKey(index));
            if (raw == null) return invalid("상품 수량 응답이 누락되었습니다.");
            if (!Float.isFinite(raw)) return invalid("상품 수량은 유한한 숫자여야 합니다.");
            if (raw < 0.0F) return invalid("상품 수량은 음수일 수 없습니다.");
            if (raw != Math.rint(raw)) return invalid("상품 수량은 정수여야 합니다.");
            int quantity = raw.intValue();
            if (quantity > HARD_CAP) return invalid("상품 수량은 64개를 초과할 수 없습니다.");
            if (quantity == 0) continue;
            if (quantity % bundleSizes.get(index) != 0) {
                return invalid("상품 수량이 판매 묶음 단위와 맞지 않습니다.");
            }
            selected.put(index, quantity);
        }
        return new Result(Map.copyOf(selected), null);
    }

    private static Result invalid(String error) {
        return new Result(Map.of(), error);
    }

    static int clampSellQuantity(int requested, int owned, int bundleSize) {
        int clamped = Math.min(Math.min(requested, owned), HARD_CAP);
        return Math.max(0, clamped - clamped % Math.max(1, bundleSize));
    }

    static String inputKey(int index) {
        return "quantity_" + index;
    }
}
