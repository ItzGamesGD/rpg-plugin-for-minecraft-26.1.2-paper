package com.hyunseo.hyunseorpg.exploration.registry;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Immutable data-only component invocation loaded from exploration YAML. */
public record ExplorationComponentSpec(String type, Map<String, Object> options) {
    public ExplorationComponentSpec {
        if (type == null || type.isBlank()) throw new IllegalArgumentException("component type is blank");
        type = type.trim().toLowerCase(Locale.ROOT);
        options = Map.copyOf(options == null ? Map.of() : new LinkedHashMap<>(options));
    }

    public String string(String key, String fallback) {
        Object value = options.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    public int integer(String key, int fallback) {
        Object value = options.get(key);
        if (value instanceof Number number) return number.intValue();
        try { return value == null ? fallback : Integer.parseInt(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    public double decimal(String key, double fallback) {
        Object value = options.get(key);
        if (value instanceof Number number) return number.doubleValue();
        try { return value == null ? fallback : Double.parseDouble(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    public boolean bool(String key, boolean fallback) {
        Object value = options.get(key);
        if (value instanceof Boolean bool) return bool;
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }
}
