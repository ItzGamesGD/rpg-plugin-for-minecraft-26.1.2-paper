package com.hyunseo.hyunseorpg.alchemy.abundance;

import java.util.Map;
import java.util.Optional;

public interface EssenceRegistry {
    Optional<EssenceDefinition> find(String essenceId);
    Map<String, EssenceDefinition> all();
    boolean reload();
}
