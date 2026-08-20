package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CustomEffectRegistry {
    private static final int SUPPORTED_SCHEMA_VERSION = 1;
    private final ConfigService config;
    private Map<String, CustomEffectDefinition> snapshot = Map.of();
    private List<String> lastErrors = List.of();
    private int loadedSchemaVersion = 0;

    public CustomEffectRegistry(ConfigService config) { this.config = config; }

    public boolean load() {
        Map<String, CustomEffectDefinition> candidate = new LinkedHashMap<>();
        java.util.ArrayList<String> errors = new java.util.ArrayList<>();
        int schemaVersion = config.getAlchemyEffectsInt("schema-version", 0);
        if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            errors.add("unsupported schema-version: " + schemaVersion
                    + " (expected " + SUPPORTED_SCHEMA_VERSION + ")");
        }
        if (config.getAlchemyEffectsSection("effects") == null) {
            errors.add("missing effects section");
        }
        Set<String> ids = config.getAlchemyEffectsKeys("effects");
        Set<String> normalizedIds = new java.util.HashSet<>();
        for (String rawId : ids) {
            String id = normalize(rawId);
            if (!id.matches("[a-z0-9_]+")) { errors.add("invalid effect id: " + rawId); continue; }
            if (!normalizedIds.add(id)) { errors.add("duplicate effect id after normalization: " + rawId); continue; }
            ConfigurationSection section = config.getAlchemyEffectsSection("effects." + rawId);
            if (section == null) { errors.add("missing effect section: " + rawId); continue; }
            if (!section.getBoolean("enabled", false)) continue;
            int maxDuration = Math.max(1, config.getAlchemyScalingInt("limits.duration-ticks", 72000));
            int maxAmplifier = Math.max(0, config.getAlchemyScalingInt("limits.amplifier", 10));
            int maxStacks = Math.max(1, config.getAlchemyScalingInt("limits.stacks", 16));
            int duration = section.getInt("duration-ticks", 0);
            int amplifier = section.getInt("amplifier", -1);
            int stacks = section.getInt("max-stacks", 0);
            if (duration < 1 || duration > maxDuration) {
                errors.add(id + ": duration-ticks must be 1.." + maxDuration);
                continue;
            }
            if (amplifier < 0 || amplifier > maxAmplifier) {
                errors.add(id + ": amplifier must be 0.." + maxAmplifier);
                continue;
            }
            if (stacks < 1 || stacks > maxStacks) {
                errors.add(id + ": max-stacks must be 1.." + maxStacks);
                continue;
            }
            EffectStackPolicy stackPolicy;
            EffectTargetPolicy targetPolicy;
            try {
                stackPolicy = EffectStackPolicy.valueOf(section.getString("policies.stack", "REFRESH_DURATION").toUpperCase(Locale.ROOT));
                targetPolicy = EffectTargetPolicy.valueOf(section.getString("target-policy", "ANY").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                errors.add(id + ": invalid policy");
                continue;
            }
            java.util.ArrayList<EffectComponentDefinition> components = new java.util.ArrayList<>();
            for (Map<?, ?> raw : section.getMapList("components")) {
                Object idValue = raw.get("id");
                String componentId = idValue == null ? "" : String.valueOf(idValue).toLowerCase(Locale.ROOT);
                if (!componentId.equals("attribute")) { errors.add(id + ": unsupported component " + componentId); continue; }
                Object attrValue = raw.get("attribute");
                String attribute = attrValue == null ? "" : String.valueOf(attrValue);
                Object operationValue = raw.get("operation");
                String operation = operationValue == null ? "ADD_NUMBER" : String.valueOf(operationValue);
                Object amountValue = raw.get("amount");
                double amount = amountValue instanceof Number number ? number.doubleValue() : 0.0D;
                if (attribute.isBlank() || !Double.isFinite(amount)) { errors.add(id + ": invalid attribute component"); continue; }
                Object enabledValue = raw.get("enabled");
                boolean enabled = enabledValue == null || Boolean.parseBoolean(String.valueOf(enabledValue));
                Object priorityValue = raw.get("priority");
                int priority = priorityValue instanceof Number number ? number.intValue() : 0;
                components.add(new EffectComponentDefinition(componentId, enabled, priority, attribute, operation, amount));
            }
            if (components.isEmpty() && section.getString("handler-id", "").isBlank()) {
                errors.add(id + ": no enabled component or handler");
                continue;
            }
            try {
                candidate.put(id, new CustomEffectDefinition(id, section.getString("display-name", id),
                        section.getString("description", CustomEffectDefinition.defaultDescription(id)), true,
                        section.getInt("priority", 0), duration, amplifier, stacks, targetPolicy, stackPolicy,
                        section.getBoolean("policies.remove-on-death", true),
                        section.getBoolean("policies.persist-on-logout", false),
                        section.getBoolean("policies.persist-on-world-change", false),
                        section.getString("handler-id", ""), components));
            } catch (IllegalArgumentException exception) {
                errors.add(id + ": " + exception.getMessage());
            }
        }
        if (!errors.isEmpty()) { lastErrors = List.copyOf(errors); return false; }
        snapshot = Map.copyOf(candidate);
        lastErrors = List.of();
        loadedSchemaVersion = schemaVersion;
        return true;
    }

    public Optional<CustomEffectDefinition> get(String id) { return Optional.ofNullable(snapshot.get(normalize(id))); }
    public Map<String, CustomEffectDefinition> getAll() { return snapshot; }
    public List<String> lastErrors() { return lastErrors; }
    public int loadedSchemaVersion() { return loadedSchemaVersion; }
    public boolean hasValidSnapshot() { return loadedSchemaVersion == SUPPORTED_SCHEMA_VERSION && lastErrors.isEmpty(); }
    private static String normalize(String id) { return id == null ? "" : id.trim().toLowerCase(Locale.ROOT); }
}
