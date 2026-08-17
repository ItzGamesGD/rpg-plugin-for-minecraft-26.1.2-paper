package com.hyunseo.hyunseorpg.equipment;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Reads option definitions; gameplay effects are introduced per option type later. */
public final class EquipmentOptionRegistry {
    private final ConfigService configService;
    private final Map<String, EquipmentOptionData> optionsById = new ConcurrentHashMap<>();

    public EquipmentOptionRegistry(ConfigService configService) {
        this.configService = configService;
    }

    public void load() {
        optionsById.clear();
        for (String rawId : configService.getEquipmentOptionsKeys("options")) {
            String id = normalize(rawId);
            String path = "options." + rawId;
            EquipmentOptionType type = EquipmentOptionType.fromInput(configService.getEquipmentOptionsString(path + ".type", "")).orElse(null);
            if (type == null) {
                continue;
            }
            optionsById.put(id, new EquipmentOptionData(
                    id,
                    configService.getEquipmentOptionsString(path + ".display-name", id),
                    type,
                    configService.getEquipmentOptionsDouble(path + ".value", 0.0D),
                    Math.max(1, (int) Math.round(configService.getEquipmentOptionsDouble(path + ".slot-cost", 1.0D))),
                    normalize(configService.getEquipmentOptionsString(path + ".required-material-id", ""))
            ));
        }
    }

    public Optional<EquipmentOptionData> get(String id) {
        return Optional.ofNullable(optionsById.get(normalize(id)));
    }

    public List<EquipmentOptionData> getAll() {
        return optionsById.values().stream().sorted(Comparator.comparing(EquipmentOptionData::id)).toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
