package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Atomic, YAML-backed delivery definitions. No player state is stored here. */
public final class DeliveryRegistry {
    private final ConfigService config;
    private final RPGItemService items;
    private final Map<String, DeliveryDefinition> definitions = new LinkedHashMap<>();
    private final Map<DeliveryProvider, List<DeliveryDefinition>> byProvider = new EnumMap<>(DeliveryProvider.class);
    private List<String> lastErrors = List.of();
    private long refreshSeconds = 1200L;
    private long timeLimitSeconds = 1200L;
    private boolean enabled;

    public DeliveryRegistry(ConfigService config, RPGItemService items) {
        this.config = config;
        this.items = items;
    }

    public boolean load() {
        Map<String, DeliveryDefinition> candidate = new LinkedHashMap<>();
        Map<DeliveryProvider, List<DeliveryDefinition>> candidateByProvider = new EnumMap<>(DeliveryProvider.class);
        List<String> errors = new ArrayList<>();
        enabled = config.getFarmingDeliveriesBoolean("enabled", true);
        refreshSeconds = Math.max(1L, config.getFarmingDeliveriesLong("refresh-seconds", 1200L));
        timeLimitSeconds = Math.max(1L, config.getFarmingDeliveriesLong("time-limit-seconds", 1200L));
        if (!enabled) {
            definitions.clear();
            byProvider.clear();
            lastErrors = List.of();
            return true;
        }
        for (String rawId : config.getFarmingDeliveriesKeys("definitions")) {
            ConfigurationSection section = config.getFarmingDeliveriesSection("definitions." + rawId);
            if (section == null) continue;
            try {
                String id = rawId.trim().toLowerCase(java.util.Locale.ROOT);
                DeliveryProvider provider = DeliveryProvider.fromInput(section.getString("provider", "")).orElseThrow();
                List<String> families = section.getStringList("item-families");
                if (families.isEmpty() && provider != DeliveryProvider.ESTATE_RESERVED) {
                    throw new IllegalArgumentException("item-families is empty");
                }
                for (String family : families) {
                    if (!isKnownFamily(family)) throw new IllegalArgumentException("unknown item family: " + family);
                }
                DeliveryDefinition definition = new DeliveryDefinition(id, provider,
                        section.getString("display-name", id), families,
                        Math.max(1, section.getInt("amount-min", 1)),
                        Math.max(1, section.getInt("amount-max", 1)),
                        Math.max(0L, section.getLong("preview-base-points", 0L)),
                        section.getDouble("reward-multiplier", 1.0D),
                        Math.max(0, section.getInt("weight", 1)));
                if (candidate.putIfAbsent(id, definition) != null) {
                    throw new IllegalArgumentException("duplicate definition id");
                }
                candidateByProvider.computeIfAbsent(provider, ignored -> new ArrayList<>()).add(definition);
            } catch (RuntimeException exception) {
                errors.add("farming/deliveries.yml definitions." + rawId + ": " + exception.getMessage());
            }
        }
        if (candidate.isEmpty()) errors.add("farming/deliveries.yml: no valid delivery definitions");
        if (!errors.isEmpty()) {
            lastErrors = List.copyOf(errors);
            return false;
        }
        definitions.clear();
        definitions.putAll(candidate);
        byProvider.clear();
        candidateByProvider.forEach((provider, values) -> byProvider.put(provider, List.copyOf(values)));
        for (DeliveryProvider provider : DeliveryProvider.values()) byProvider.putIfAbsent(provider, List.of());
        lastErrors = List.of();
        return true;
    }

    public Optional<DeliveryDefinition> get(String id) {
        return Optional.ofNullable(definitions.get(id == null ? "" : id.trim().toLowerCase(java.util.Locale.ROOT)));
    }

    public List<DeliveryDefinition> forProvider(DeliveryProvider provider) {
        return List.copyOf(byProvider.getOrDefault(provider, List.of()));
    }

    public List<DeliveryDefinition> getAll() { return List.copyOf(definitions.values()); }
    public List<String> lastErrors() { return lastErrors; }
    public long refreshSeconds() { return refreshSeconds; }
    public long timeLimitSeconds() { return timeLimitSeconds; }
    public boolean enabled() { return enabled; }

    private boolean isKnownFamily(String raw) {
        String family = raw == null ? "" : raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (family.startsWith("crop_")) {
            String crop = family.substring("crop_".length());
            return List.of("corn", "onion", "chili", "garlic").contains(crop);
        }
        if (family.startsWith("processed_")) {
            String crop = family.substring("processed_".length());
            int marker = crop.indexOf('_');
            if (marker > 0) crop = crop.substring(0, marker);
            if (!List.of("corn", "onion", "chili", "garlic").contains(crop)) return false;
            String cropFamily = crop;
            return items.getAllData().stream().anyMatch(data ->
                    data.itemId().startsWith("processed_" + cropFamily + "_")
                            && items.isFarmingItemId(data.itemId()));
        }
        return items.getData(family).isPresent() && items.isFarmingItemId(family);
    }
}
