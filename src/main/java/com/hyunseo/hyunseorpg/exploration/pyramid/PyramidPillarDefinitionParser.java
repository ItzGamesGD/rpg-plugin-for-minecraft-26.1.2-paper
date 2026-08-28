package com.hyunseo.hyunseorpg.exploration.pyramid;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Single parser used by startup validation and the already-validated runtime component. */
public final class PyramidPillarDefinitionParser {
    private PyramidPillarDefinitionParser() { }

    public static List<PushPillarDefinition> parse(Object raw) {
        if (!(raw instanceof List<?> values)) return List.of();
        List<PushPillarDefinition> result = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof Map<?, ?> map)) continue;
            String id = text(map.get("id"));
            String symbol = text(map.containsKey("symbol-id") ? map.get("symbol-id") : id);
            String color = text(map.containsKey("color") ? map.get("color") : "sand");
            PyramidGridPoint initial = point(map.get("initial"));
            PyramidGridPoint target = point(map.get("target"));
            Set<PyramidGridPoint> allowed = new LinkedHashSet<>();
            if (map.get("allowed") instanceof List<?> cells) {
                for (Object cell : cells) {
                    PyramidGridPoint parsed = point(cell);
                    if (parsed != null) allowed.add(parsed);
                }
            }
            if (!id.isBlank() && initial != null && target != null) {
                result.add(new PushPillarDefinition(id, symbol, color, initial, target, allowed));
            }
        }
        return List.copyOf(result);
    }

    private static PyramidGridPoint point(Object raw) {
        if (raw instanceof String value) {
            String[] parts = value.trim().split(",");
            if (parts.length != 2) return null;
            try { return new PyramidGridPoint(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())); }
            catch (NumberFormatException ignored) { return null; }
        }
        if (raw instanceof List<?> values && values.size() >= 2) {
            try { return new PyramidGridPoint(Integer.parseInt(String.valueOf(values.get(0))),
                    Integer.parseInt(String.valueOf(values.get(1)))); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
}
