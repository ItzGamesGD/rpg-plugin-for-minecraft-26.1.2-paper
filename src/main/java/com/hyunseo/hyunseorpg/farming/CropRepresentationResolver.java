package com.hyunseo.hyunseorpg.farming;

import java.util.Optional;
import java.util.function.Function;

/** Resolves a block representation to a canonical crop without putting registry knowledge in CropIndex. */
public final class CropRepresentationResolver {
    private final CropIndex index;
    private final Function<String, Optional<CropDefinition>> definitionLookup;

    public CropRepresentationResolver(CropIndex index,
                                      Function<String, Optional<CropDefinition>> definitionLookup) {
        this.index = index;
        this.definitionLookup = definitionLookup;
    }

    public Optional<CropInstance> find(CropPosition position) {
        Optional<CropInstance> direct = index.findByBlock(position);
        if (direct.isPresent()) return direct;
        if (position.y() == Integer.MIN_VALUE) return Optional.empty();
        CropInstance lower = index.findByBlock(new CropPosition(
                position.worldId(), position.x(), position.y() - 1, position.z())).orElse(null);
        if (lower == null) return Optional.empty();
        return definitionLookup.apply(lower.cropId())
                .filter(CropDefinition::twoBlock)
                .map(ignored -> lower);
    }
}
