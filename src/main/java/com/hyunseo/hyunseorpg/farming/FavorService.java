package com.hyunseo.hyunseorpg.farming;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Reads provider-specific favor rules without owning player persistence. */
public final class FavorService {
    private final ConfigService config;
    private Map<DeliveryProvider, FavorDefinition> definitions = Map.of();
    private boolean enabled;

    public FavorService(ConfigService config) {
        this.config = config;
    }

    public void load() {
        enabled = config.getFarmingFavorBoolean("enabled", false);
        Map<DeliveryProvider, FavorDefinition> candidate = new LinkedHashMap<>();
        for (String rawProvider : config.getFarmingFavorKeys("providers")) {
            DeliveryProvider provider = DeliveryProvider.fromInput(rawProvider).orElse(null);
            if (provider == null || provider == DeliveryProvider.ESTATE_RESERVED) continue;
            String root = "providers." + rawProvider;
            long maxFavor = Math.max(0L, config.getFarmingFavorLong(root + ".max-favor", 0L));
            double baseMultiplier = finiteNonNegative(
                    config.getFarmingFavorDouble(root + ".base-multiplier", 1.0D), 1.0D);
            double maxMultiplier = finiteNonNegative(
                    config.getFarmingFavorDouble(root + ".max-multiplier", baseMultiplier), baseMultiplier);
            maxMultiplier = Math.max(baseMultiplier, maxMultiplier);

            List<FavorBand> bands = new ArrayList<>();
            for (String rawBand : config.getFarmingFavorKeys(root + ".bands")) {
                String bandRoot = root + ".bands." + rawBand;
                double minimum = finiteNonNegative(config.getFarmingFavorDouble(
                        bandRoot + ".min-score", 0.0D), 0.0D);
                double maximum = finiteNonNegative(config.getFarmingFavorDouble(
                        bandRoot + ".max-score", 100.0D), 100.0D);
                long increase = Math.max(0L, config.getFarmingFavorLong(
                        bandRoot + ".favor-increase", 0L));
                if (maximum < minimum) continue;
                bands.add(new FavorBand(rawBand.trim().toLowerCase(Locale.ROOT), minimum, maximum, increase));
            }
            bands.sort(Comparator.comparingDouble(FavorBand::minimumScore));

            List<FavorLevel> levels = new ArrayList<>();
            for (String rawLevel : config.getFarmingFavorKeys(root + ".levels")) {
                String levelRoot = root + ".levels." + rawLevel;
                long minimum = Math.max(0L, config.getFarmingFavorLong(levelRoot + ".minimum-favor", 0L));
                double multiplier = finiteNonNegative(config.getFarmingFavorDouble(
                        levelRoot + ".multiplier", baseMultiplier), baseMultiplier);
                levels.add(new FavorLevel(rawLevel, minimum, Math.min(maxMultiplier, Math.max(baseMultiplier, multiplier))));
            }
            levels.sort(Comparator.comparingLong(FavorLevel::minimumFavor));
            candidate.put(provider, new FavorDefinition(maxFavor, baseMultiplier, maxMultiplier,
                    List.copyOf(bands), List.copyOf(levels)));
        }
        definitions = Map.copyOf(candidate);
    }

    public String band(DeliveryProvider provider, double score) {
        FavorDefinition definition = definitions.get(provider);
        if (definition == null || definition.bands().isEmpty()) return "unconfigured";
        double safeScore = Double.isFinite(score) ? Math.max(0.0D, score) : 0.0D;
        FavorBand selected = definition.bands().get(0);
        for (FavorBand band : definition.bands()) {
            if (safeScore >= band.minimumScore() && safeScore <= band.maximumScore()) return band.id();
            if (safeScore >= band.minimumScore()) selected = band;
        }
        return selected.id();
    }

    public long favorIncrease(DeliveryProvider provider, double score, long currentFavor) {
        FavorDefinition definition = definitions.get(provider);
        if (!enabled || definition == null || definition.maxFavor() <= 0L) return 0L;
        FavorBand selected = findBand(definition, score);
        long current = clampFavor(definition, currentFavor);
        if (selected == null || current >= definition.maxFavor()) return 0L;
        return Math.min(selected.favorIncrease(), definition.maxFavor() - current);
    }

    public double multiplier(DeliveryProvider provider, long currentFavor) {
        FavorDefinition definition = definitions.get(provider);
        if (!enabled || definition == null) return 1.0D;
        long current = clampFavor(definition, currentFavor);
        double selected = definition.baseMultiplier();
        for (FavorLevel level : definition.levels()) {
            if (current >= level.minimumFavor()) selected = level.multiplier();
            else break;
        }
        return Math.min(definition.maxMultiplier(), Math.max(definition.baseMultiplier(), selected));
    }

    public long maxFavor(DeliveryProvider provider) {
        FavorDefinition definition = definitions.get(provider);
        return definition == null ? 0L : definition.maxFavor();
    }

    public boolean enabled() {
        return config.getFarmingFavorBoolean("enabled", false);
    }

    private FavorBand findBand(FavorDefinition definition, double score) {
        double safeScore = Double.isFinite(score) ? Math.max(0.0D, score) : 0.0D;
        for (FavorBand band : definition.bands()) {
            if (safeScore >= band.minimumScore() && safeScore <= band.maximumScore()) return band;
        }
        return null;
    }

    private long clampFavor(FavorDefinition definition, long value) {
        return Math.max(0L, Math.min(definition.maxFavor(), value));
    }

    private double finiteNonNegative(double value, double fallback) {
        return Double.isFinite(value) && value >= 0.0D ? value : fallback;
    }

    public record FavorBand(String id, double minimumScore, double maximumScore, long favorIncrease) { }

    public record FavorLevel(String id, long minimumFavor, double multiplier) { }

    public record FavorDefinition(long maxFavor, double baseMultiplier, double maxMultiplier,
                                  List<FavorBand> bands, List<FavorLevel> levels) { }
}
