package com.hyunseo.hyunseorpg.mob;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class MobLevelScalingService {
    private static final Map<String, ScalingOption> OPTIONS = createOptions();

    private final ConfigService configService;
    private final PlayerDataService playerDataService;

    public MobLevelScalingService(ConfigService configService, PlayerDataService playerDataService) {
        this.configService = configService;
        this.playerDataService = playerDataService;
    }

    public int calculateNaturalSpawnLevel(Collection<? extends Player> onlinePlayers) {
        if (!isEnabled()) {
            return Math.max(1, configService.getMobsInt("defaults.mob-level", 1));
        }

        LevelSample sample = samplePlayerLevels(onlinePlayers);
        if (sample.playerCount() == 0) {
            return Math.max(1, configService.getMobsInt("defaults.mob-level", 1));
        }

        double lowLevelChance = getDouble("low-level-bias-chance");
        if (ThreadLocalRandom.current().nextDouble() < lowLevelChance) {
            return sample.minLevel();
        }

        int variation = (int) Math.round(getDouble("random-level-variation"));
        int offset = variation <= 0 ? 0 : ThreadLocalRandom.current().nextInt(-variation, variation + 1);
        int candidate = sample.averageLevel() + offset;
        return clamp(candidate, 1, (int) Math.round(getDouble("max-level")));
    }

    public LevelSample samplePlayerLevels(Collection<? extends Player> onlinePlayers) {
        int playerCount = 0;
        int totalLevel = 0;
        int minLevel = Integer.MAX_VALUE;

        for (Player player : onlinePlayers) {
            int level = playerDataService.getOrLoad(player).getBaseLevel();
            playerCount++;
            totalLevel += level;
            minLevel = Math.min(minLevel, level);
        }

        if (playerCount == 0) {
            return new LevelSample(0, 1, 1);
        }
        return new LevelSample(playerCount, minLevel, Math.max(1, Math.round((float) totalLevel / (float) playerCount)));
    }

    public int calculatePlayerAverageLevel(Player player) {
        if (player == null) return Math.max(1, configService.getMobsInt("defaults.mob-level", 1));
        return samplePlayerLevels(java.util.List.of(player)).averageLevel();
    }

    public int calculateNearbyAverageLevel(Location location, double radius) {
        if (location == null || location.getWorld() == null) return Math.max(1, configService.getMobsInt("defaults.mob-level", 1));
        double safeRadius = Math.max(1.0D, radius);
        List<Player> nearby = location.getWorld().getNearbyEntities(location, safeRadius, safeRadius, safeRadius).stream()
                .filter(Player.class::isInstance).map(Player.class::cast)
                .filter(player -> player.getGameMode() != org.bukkit.GameMode.SPECTATOR)
                .filter(player -> !player.hasPermission("hyunseorpg.admin.invisible"))
                .filter(Player::isOnline).toList();
        return calculateNaturalSpawnLevel(nearby);
    }

    public void applyNaturalScaling(LivingEntity entity, int level) {
        if (!isEnabled()) {
            return;
        }

        int safeLevel = Math.max(1, level);
        double maxHealth = getDouble("health-base") + Math.max(0, safeLevel - 1) * getDouble("health-per-level");
        double attackDamage = getDouble("attack-base") + Math.max(0, safeLevel - 1) * getDouble("attack-per-level");

        if (maxHealth > 0.0D) {
            AttributeInstance maxHealthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttribute != null) {
                maxHealthAttribute.setBaseValue(maxHealth);
                entity.setHealth(Math.min(maxHealth, maxHealthAttribute.getValue()));
            }
        }

        if (attackDamage > 0.0D) {
            AttributeInstance attackAttribute = entity.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attackAttribute != null) {
                attackAttribute.setBaseValue(attackDamage);
            }
        }
    }

    public Map<String, Double> getCurrentSettings() {
        Map<String, Double> settings = new LinkedHashMap<>();
        OPTIONS.forEach((key, option) -> settings.put(key, getDouble(key)));
        return settings;
    }

    public Optional<String> setScalingValue(String key, double value) {
        String normalizedKey = normalizeKey(key);
        ScalingOption option = OPTIONS.get(normalizedKey);
        if (option == null) {
            return Optional.of("알 수 없는 스케일링 항목입니다: " + key);
        }
        if (value < option.minValue() || value > option.maxValue()) {
            return Optional.of(normalizedKey + " 값은 " + option.minValue() + " ~ " + option.maxValue() + " 사이여야 합니다.");
        }

        Object storedValue = option.integerValue() ? (int) Math.round(value) : value;
        configService.setMobsValue(option.path(), storedValue);
        configService.saveMobsConfig();
        return Optional.empty();
    }

    public boolean isScalingKey(String key) {
        return OPTIONS.containsKey(normalizeKey(key));
    }

    public Collection<String> getScalingKeys() {
        return OPTIONS.keySet();
    }

    private boolean isEnabled() {
        return configService.getMobsBoolean("level-scaling.enabled", true);
    }

    private double getDouble(String key) {
        ScalingOption option = OPTIONS.get(key);
        if (option == null) {
            throw new IllegalArgumentException("Unknown scaling key: " + key);
        }
        return configService.getMobsDouble(option.path(), option.defaultValue());
    }

    private int clamp(int value, int minimum, int maximum) {
        int safeMaximum = Math.max(minimum, maximum);
        return Math.max(minimum, Math.min(safeMaximum, value));
    }

    private String normalizeKey(String key) {
        return key.trim().toLowerCase(Locale.ROOT).replace("_", "-");
    }

    private static Map<String, ScalingOption> createOptions() {
        Map<String, ScalingOption> options = new LinkedHashMap<>();
        options.put("health-base", new ScalingOption("level-scaling.health-base", 20.0D, 1.0D, 500.0D, false));
        options.put("health-per-level", new ScalingOption("level-scaling.health-per-level", 1.5D, 0.0D, 50.0D, false));
        options.put("attack-base", new ScalingOption("level-scaling.attack-base", 3.0D, 0.0D, 100.0D, false));
        options.put("attack-per-level", new ScalingOption("level-scaling.attack-per-level", 0.25D, 0.0D, 20.0D, false));
        options.put("low-level-bias-chance", new ScalingOption("level-scaling.low-level-bias-chance", 0.35D, 0.0D, 0.75D, false));
        options.put("random-level-variation", new ScalingOption("level-scaling.random-level-variation", 15.0D, 0.0D, 30.0D, true));
        options.put("max-level", new ScalingOption("level-scaling.max-level", 100.0D, 1.0D, 500.0D, true));
        return Map.copyOf(options);
    }

    private record ScalingOption(String path, double defaultValue, double minValue, double maxValue, boolean integerValue) {
    }

    public record LevelSample(int playerCount, int minLevel, int averageLevel) {
    }
}
