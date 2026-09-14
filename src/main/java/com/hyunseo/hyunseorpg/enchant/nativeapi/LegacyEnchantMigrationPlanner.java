package com.hyunseo.hyunseorpg.enchant.nativeapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Pure planning step: unresolved entries are always preserved verbatim. */
public final class LegacyEnchantMigrationPlanner {
    private LegacyEnchantMigrationPlanner() { }

    public static Plan plan(String encoded, Function<String, Target> resolver) {
        Map<String, Integer> additions = new LinkedHashMap<>();
        List<Failure> preserved = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) return new Plan(Map.of(), List.of());
        for (String raw : encoded.split(",")) {
            String entry = raw.trim();
            if (entry.isBlank()) continue;
            String[] split = entry.split("@", 2);
            Target target = resolver.apply(split[0]);
            if (target == null) {
                preserved.add(new Failure(entry, FailureReason.UNKNOWN_RUNTIME_ID));
                continue;
            }
            if (!target.nativeAvailable()) {
                preserved.add(new Failure(entry, FailureReason.MISSING_NATIVE_TARGET));
                continue;
            }
            int requested = 1;
            if (split.length == 2) try { requested = Integer.parseInt(split[1]); } catch (NumberFormatException ignored) { }
            int level = Math.max(1, Math.min(target.maxLevel(), requested));
            if (target.existingLevel() < level) additions.merge(target.canonicalId(), level, Math::max);
        }
        return new Plan(Map.copyOf(additions), List.copyOf(preserved));
    }

    public record Target(String canonicalId, int maxLevel, boolean nativeAvailable, int existingLevel) { }
    public record Failure(String encodedEntry, FailureReason reason) { }
    public enum FailureReason { UNKNOWN_RUNTIME_ID, MISSING_NATIVE_TARGET }
    public record Plan(Map<String, Integer> additions, List<Failure> preserved) {
        public String preservedEncoding() {
            return preserved.stream().map(Failure::encodedEntry).reduce((a, b) -> a + "," + b).orElse("");
        }
    }
}
