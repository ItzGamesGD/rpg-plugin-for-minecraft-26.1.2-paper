package com.hyunseo.hyunseorpg.progression;

import java.util.Locale;

public record ProgressionLoopResource(String type, String itemId) {
    public ProgressionLoopResource {
        type = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        itemId = itemId == null ? "" : itemId.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isItem() {
        return "ITEM".equals(type);
    }
}
