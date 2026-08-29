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
        return parse(raw, "pyramid");
    }

    public static List<PushPillarDefinition> parse(Object raw, String context) {
        String source = context == null || context.isBlank() ? "pyramid" : context.trim();
        if (!(raw instanceof List<?> values)) {
            throw new IllegalArgumentException(source + ": field=pillars must be a list");
        }
        List<PushPillarDefinition> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            Object value = values.get(index);
            if (!(value instanceof Map<?, ?> map)) {
                throw invalid(source, index, "<missing>", "pillar", "must be a map");
            }
            String id = text(map.get("id"));
            if (id.isBlank()) throw invalid(source, index, "<missing>", "id", "must not be blank");
            String symbol = text(map.containsKey("symbol-id") ? map.get("symbol-id") : id);
            String color = text(map.containsKey("color") ? map.get("color") : "sand");
            PyramidGridPoint initial = requirePoint(map.get("initial"), source, index, id, "initial");
            PyramidGridPoint target = requirePoint(map.get("target"), source, index, id, "target");
            Set<PyramidGridPoint> allowed = new LinkedHashSet<>();
            Object rawAllowed = map.get("allowed");
            if (!(rawAllowed instanceof List<?> cells)) {
                throw invalid(source, index, id, "allowed", "must be a list");
            }
            for (int cellIndex = 0; cellIndex < cells.size(); cellIndex++) {
                PyramidGridPoint parsed = point(cells.get(cellIndex));
                if (parsed == null) {
                    throw invalid(source, index, id, "allowed[" + cellIndex + "]", "must be an x,z coordinate");
                }
                allowed.add(parsed);
            }
            result.add(new PushPillarDefinition(id, symbol, color, initial, target, allowed));
        }
        return List.copyOf(result);
    }

    private static PyramidGridPoint requirePoint(Object raw, String context, int index, String id, String field) {
        PyramidGridPoint point = point(raw);
        if (point == null) throw invalid(context, index, id, field, "must be an x,z coordinate");
        return point;
    }

    private static IllegalArgumentException invalid(String context, int index, String id,
                                                    String field, String detail) {
        return new IllegalArgumentException(context + ": pillarIndex=" + index + ", pillarId=" + id
                + ", field=" + field + " " + detail);
    }

    private static PyramidGridPoint point(Object raw) {
        if (raw instanceof String value) {
            String[] parts = value.trim().split(",");
            if (parts.length != 2) return null;
            try { return new PyramidGridPoint(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())); }
            catch (NumberFormatException ignored) { return null; }
        }
        if (raw instanceof List<?> values && values.size() == 2) {
            try { return new PyramidGridPoint(Integer.parseInt(String.valueOf(values.get(0))),
                    Integer.parseInt(String.valueOf(values.get(1)))); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
}
