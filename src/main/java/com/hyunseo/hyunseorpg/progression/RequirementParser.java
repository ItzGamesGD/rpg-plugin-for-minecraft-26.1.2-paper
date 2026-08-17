package com.hyunseo.hyunseorpg.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class RequirementParser {
    private RequirementParser() {
    }

    public static List<Requirement> parseList(List<Map<?, ?>> rawRequirements) {
        List<Requirement> requirements = new ArrayList<>();
        for (Map<?, ?> rawRequirement : rawRequirements) {
            parse(rawRequirement).ifPresent(requirements::add);
        }
        return requirements;
    }

    private static Optional<Requirement> parse(Map<?, ?> rawRequirement) {
        String rawType = stringValue(rawRequirement, "type", "");
        if (rawType.isBlank()) {
            return Optional.empty();
        }

        try {
            RequirementType type = RequirementType.valueOf(rawType.toUpperCase(Locale.ROOT).replace("-", "_"));
            String target = stringValue(rawRequirement, "target", stringValue(rawRequirement, "value", ""));
            long amount = longValue(rawRequirement, "amount", longValue(rawRequirement, "value", 1L));
            return Optional.of(new Requirement(type, target, amount));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static String stringValue(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static long longValue(Map<?, ?> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
