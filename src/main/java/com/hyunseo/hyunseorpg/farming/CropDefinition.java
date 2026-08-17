package com.hyunseo.hyunseorpg.farming;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Configuration-backed definition of a crop. */
public record CropDefinition(
        String id,
        String seedItemId,
        String cropItemId,
        Material displayBlock,
        int stages,
        boolean twoBlock,
        Set<Material> soil,
        long secondsPerStage,
        int dataVersion,
        boolean enabled
) {
    public CropDefinition {
        id = normalize(id);
        seedItemId = normalize(seedItemId);
        cropItemId = normalize(cropItemId);
        displayBlock = Objects.requireNonNull(displayBlock, "displayBlock");
        soil = Set.copyOf(Objects.requireNonNull(soil, "soil"));
        stages = Math.max(2, stages);
        secondsPerStage = Math.max(1L, secondsPerStage);
        dataVersion = Math.max(1, dataVersion);
        if (id.isBlank() || seedItemId.isBlank() || cropItemId.isBlank()) {
            throw new IllegalArgumentException("Crop IDs must not be blank");
        }
    }

    public int maxStage() {
        return stages - 1;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
