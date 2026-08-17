package com.hyunseo.hyunseorpg.alchemy;

import java.util.Set;

public final class EffectComponentRegistry {
    private static final Set<String> SUPPORTED = Set.of("attribute");
    public boolean isSupported(String id) { return id != null && SUPPORTED.contains(id.toLowerCase()); }
    public Set<String> supportedIds() { return SUPPORTED; }
}
