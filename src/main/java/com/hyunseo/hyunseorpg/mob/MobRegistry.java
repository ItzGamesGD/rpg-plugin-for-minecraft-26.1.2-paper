package com.hyunseo.hyunseorpg.mob;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.entity.EntityType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MobRegistry {
    private final ConfigService configService;
    private final Map<String, MobData> mobDataById = new LinkedHashMap<>();

    public MobRegistry(ConfigService configService) {
        this.configService = configService;
    }

    public void load() {
        mobDataById.clear();
        loadSection("custom-mobs");
        loadSection("mob-definitions");
        configService.getPlugin().getLogger().info(
                "Loaded " + mobDataById.size() + " registered RPG mob definitions from mobs.yml.");
    }

    private void loadSection(String sectionPath) {
        Set<String> mobIds = configService.getMobsKeys(sectionPath);
        for (String mobId : mobIds) {
            parseMobData(sectionPath, mobId).ifPresent(mobData -> mobDataById.put(mobData.mobId(), mobData));
        }
    }

    public Optional<MobData> get(String mobId) {
        return Optional.ofNullable(mobDataById.get(normalizeId(mobId)));
    }

    public List<String> getMobIds() {
        return List.copyOf(mobDataById.keySet());
    }

    public List<MobData> getAll() {
        return List.copyOf(mobDataById.values());
    }

    public Optional<ConfigurationSection> getSection(String mobId) {
        if (mobId == null || mobId.isBlank()) return Optional.empty();
        for (String root : List.of("custom-mobs", "mob-definitions")) {
            ConfigurationSection section = configService.getMobsSection(root + "." + normalizeId(mobId));
            if (section != null) return Optional.of(section);
        }
        return Optional.empty();
    }

    public List<String> getBehaviorMobIds() {
        return mobDataById.keySet().stream()
                .filter(id -> getSection(id).map(section -> section.isString("behavior-id") || section.isConfigurationSection("behavior")).orElse(false))
                .toList();
    }

    private Optional<MobData> parseMobData(String sectionPath, String rawMobId) {
        String mobId = normalizeId(rawMobId);
        String path = sectionPath + "." + rawMobId + ".";
        Optional<EntityType> entityType = parseEntityType(configService.getMobsString(path + "vanilla-type", ""));
        if (entityType.isEmpty() || !entityType.get().isAlive()) {
            return Optional.empty();
        }

        List<String> tags = configService.getMobsStringList(path + "tags");
        boolean boss = configService.getMobsBoolean(path + "boss", containsIgnoreCase(tags, "boss"));
        boolean elite = configService.getMobsBoolean(path + "elite", containsIgnoreCase(tags, "elite"));

        return Optional.of(new MobData(
                mobId,
                configService.getMobsString(path + "display-name", entityType.get().name()),
                entityType.get(),
                Math.max(1, configService.getMobsInt(path + "level", 1)),
                tags,
                normalizeId(configService.getMobsString(path + "ability-profile", "")),
                parseAttributes(path),
                Math.max(0L, getInt(path, "exp", "base-exp", configService.getMobsInt(path + "expReward", 0))),
                Math.max(0L, configService.getMobsInt(path + "class-exp", 0)),
                Math.max(0L, getInt(path, "coin", "money-reward", configService.getMobsInt(path + "moneyReward", 0))),
                configService.getMobsString(path + "drop-table", configService.getMobsString(path + "dropTable", "")),
                configService.getMobsString(path + "region", ""),
                boss,
                elite
        ));
    }

    private MobAttributeData parseAttributes(String path) {
        double maxHealth = getDouble(path, "hp", "attributes.max-health", 0.0D);
        double attackDamage = getDouble(path, "damage", "attributes.attack-damage", 0.0D);
        double armor = configService.getMobsDouble(path + "attributes.armor", 0.0D);
        double scale = configService.getMobsDouble(path + "attributes.scale", 0.0D);
        double knockback = configService.getMobsDouble(path + "attributes.knockback-resistance", 0.0D);

        ConfigurationSection speedSection = configService.getMobsSection(path + "attributes.movement-speed");
        if (speedSection != null) {
            String mode = speedSection.getString("mode", "ABSOLUTE");
            double value = speedSection.getDouble("value", 0.0D);
            return new MobAttributeData(maxHealth, 0.0D, attackDamage, armor, scale,
                    mode, value, knockback);
        }

        String mode = configService.getMobsString(path + "attributes.movement-speed-mode", "ABSOLUTE");
        double legacySpeed = configService.getMobsDouble(path + "attributes.movement-speed", 0.0D);
        double configuredValue = configService.getMobsDouble(path + "attributes.movement-speed-value", legacySpeed);
        double multiplier = configService.getMobsDouble(path + "attributes.movement-speed-multiplier", 0.0D);
        if (multiplier > 0.0D) {
            mode = "MULTIPLIER";
            configuredValue = multiplier;
        }
        return new MobAttributeData(maxHealth, legacySpeed, attackDamage, armor, scale,
                mode, configuredValue, knockback);
    }

    private int getInt(String path, String primaryKey, String fallbackKey, int defaultValue) {
        int fallback = configService.getMobsInt(path + fallbackKey, defaultValue);
        return configService.getMobsInt(path + primaryKey, fallback);
    }

    private double getDouble(String path, String primaryKey, String fallbackKey, double defaultValue) {
        double fallback = configService.getMobsDouble(path + fallbackKey, defaultValue);
        return configService.getMobsDouble(path + primaryKey, fallback);
    }

    private boolean containsIgnoreCase(List<String> values, String target) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(target));
    }

    private Optional<EntityType> parseEntityType(String input) {
        String normalized = input.toUpperCase(Locale.ROOT).replace("-", "_");
        return Arrays.stream(EntityType.values())
                .filter(type -> type.name().equals(normalized))
                .findFirst();
    }

    private String normalizeId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
