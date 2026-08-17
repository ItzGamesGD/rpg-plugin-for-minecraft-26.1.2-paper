package com.hyunseo.hyunseorpg.farming;

import org.bukkit.block.Block;

/** Separates custom crop reward exclusion from the generic activity-block ledger. */
public final class CropHarvestValidator {
    private final CropRepresentationResolver resolver;

    public CropHarvestValidator(CropRepresentationResolver resolver) {
        this.resolver = resolver;
    }

    public boolean isRegisteredCrop(Block block) {
        return block != null && resolver.find(CropPosition.of(block)).isPresent();
    }
}
