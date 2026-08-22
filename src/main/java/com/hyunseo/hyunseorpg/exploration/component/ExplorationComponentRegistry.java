package com.hyunseo.hyunseorpg.exploration.component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ExplorationComponentRegistry {
    private final Map<String, ExplorationComponent> components = new LinkedHashMap<>();

    public ExplorationComponentRegistry register(ExplorationComponent component) {
        if (component == null) return this;
        String key = normalize(component.type());
        if (components.putIfAbsent(key, component) != null) throw new IllegalArgumentException("duplicate component: " + key);
        return this;
    }

    public Optional<ExplorationComponent> get(String type) { return Optional.ofNullable(components.get(normalize(type))); }
    public java.util.Set<String> ids() { return java.util.Set.copyOf(components.keySet()); }

    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
}
