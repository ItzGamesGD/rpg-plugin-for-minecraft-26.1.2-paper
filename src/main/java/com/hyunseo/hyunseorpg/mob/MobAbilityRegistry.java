package com.hyunseo.hyunseorpg.mob;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MobAbilityRegistry {
    private final ConfigService configService;
    private final Map<String, MobAbilityProfile> profilesById = new LinkedHashMap<>();

    public MobAbilityRegistry(ConfigService configService) {
        this.configService = configService;
    }

    public void load() {
        profilesById.clear();
        Set<String> profileIds = configService.getMobsKeys("ability-profiles");
        for (String profileId : profileIds) {
            parseProfile(profileId).ifPresent(profile -> profilesById.put(profile.profileId(), profile));
        }
    }

    public Optional<MobAbilityProfile> get(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(profilesById.get(normalizeId(profileId)));
    }

    public List<String> getProfileIds() {
        return List.copyOf(profilesById.keySet());
    }

    private Optional<MobAbilityProfile> parseProfile(String rawProfileId) {
        String profileId = normalizeId(rawProfileId);
        List<MobAbilityData> abilities = new ArrayList<>();
        for (String abilityId : configService.getMobsKeys("ability-profiles." + rawProfileId + ".abilities")) {
            abilities.add(parseAbility(rawProfileId, abilityId));
        }
        return Optional.of(new MobAbilityProfile(profileId, abilities));
    }

    private MobAbilityData parseAbility(String profileId, String rawAbilityId) {
        String path = "ability-profiles." + profileId + ".abilities." + rawAbilityId + ".";
        String abilityId = normalizeId(rawAbilityId);
        MobAbilityType type = MobAbilityType.fromInput(configService.getMobsString(path + "type", "CUSTOM"));
        String trigger = configService.getMobsString(path + "trigger", "TIMER");
        long cooldownTicks = Math.max(0L, configService.getMobsInt(path + "cooldown-ticks", 0));
        return new MobAbilityData(abilityId, type, trigger, cooldownTicks, readParameterMap(path + "parameters"));
    }

    private Map<String, String> readParameterMap(String path) {
        ConfigurationSection section = configService.getMobsSection(path);
        if (section == null) {
            return Map.of();
        }

        Map<String, String> parameters = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value != null) {
                parameters.put(key, String.valueOf(value));
            }
        }
        return parameters;
    }

    private String normalizeId(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
