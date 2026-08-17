package com.hyunseo.hyunseorpg.alchemy.potion;

import java.util.Map;
import java.util.Optional;

public interface PotionRegistry {
    Optional<PotionDefinition> find(String id);
    Map<String, PotionDefinition> all();
    boolean reload();
}
