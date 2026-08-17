package com.hyunseo.hyunseorpg.alchemy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class HandlerRegistry {
    private final Map<String, Object> handlers = new ConcurrentHashMap<>();

    public boolean contains(String id) { return !normalize(id).isBlank() && handlers.containsKey(normalize(id)); }

    public void register(String id, Object handler) {
        String normalized = normalize(id);
        if (normalized.isBlank() || handler == null) {
            throw new IllegalArgumentException("handler id and handler are required");
        }
        handlers.put(normalized, handler);
    }

    public Optional<Object> get(String id) {
        String normalized = normalize(id);
        return normalized.isBlank() ? Optional.empty() : Optional.ofNullable(handlers.get(normalized));
    }

    public void unregister(String id) {
        String normalized = normalize(id);
        if (!normalized.isBlank()) handlers.remove(normalized);
    }

    public Map<String, Object> all() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(handlers));
    }

    public void clear() { handlers.clear(); }

    private String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
