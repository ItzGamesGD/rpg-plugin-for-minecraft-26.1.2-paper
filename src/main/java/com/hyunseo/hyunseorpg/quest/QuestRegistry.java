package com.hyunseo.hyunseorpg.quest;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.progression.Requirement;
import com.hyunseo.hyunseorpg.progression.RequirementParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class QuestRegistry {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final Map<String, QuestData> questsById = new LinkedHashMap<>();

    public QuestRegistry(JavaPlugin plugin, ConfigService configService) {
        this.plugin = plugin;
        this.configService = configService;
    }

    public void load() {
        questsById.clear();
        for (String questId : configService.getQuestsKeys("quests")) {
            parseQuest(questId).ifPresent(quest -> questsById.put(quest.questId(), quest));
        }
    }

    public Optional<QuestData> get(String questId) {
        return Optional.ofNullable(questsById.get(normalizeId(questId)));
    }

    public List<QuestData> getAll() {
        return List.copyOf(questsById.values());
    }

    private Optional<QuestData> parseQuest(String rawQuestId) {
        ConfigurationSection section = configService.getQuestsSection("quests." + rawQuestId);
        if (section == null) {
            return Optional.empty();
        }

        String questId = normalizeId(rawQuestId);
        List<Requirement> requirements = RequirementParser.parseList(section.getMapList("requirements"));
        List<QuestObjective> objectives = parseObjectives(questId, section.getMapList("objectives"));
        List<QuestReward> rewards = parseRewards(questId, section.getMapList("rewards"));
        return Optional.of(new QuestData(
                questId,
                section.getString("display-name", questId),
                requirements,
                objectives,
                rewards
        ));
    }

    private List<QuestObjective> parseObjectives(String questId, List<Map<?, ?>> rawObjectives) {
        List<QuestObjective> objectives = new ArrayList<>();
        for (Map<?, ?> rawObjective : rawObjectives) {
            String objectiveId = stringValue(rawObjective, "id", "");
            if (objectiveId.isBlank()) {
                objectiveId = stringValue(rawObjective, "objective-id", "");
            }
            String rawType = stringValue(rawObjective, "type", "");
            try {
                Object rawTarget = rawObjective.get("target");
                String targetType = rawTarget instanceof Map<?, ?> targetMap
                        ? stringValue(targetMap, "type", "") : "";
                QuestObjectiveType type = parseObjectiveType(rawType, targetType);
                objectives.add(new QuestObjective(
                        objectiveId,
                        type,
                        parseTarget(rawTarget),
                        intValue(rawObjective, "amount", 1)
                ));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignoring invalid quest objective in " + questId + ": " + rawObjective);
            }
        }
        return objectives;
    }

    private QuestObjectiveType parseObjectiveType(String rawType, String targetType) {
        String candidate = rawType == null || rawType.isBlank() ? targetType : rawType;
        String normalized = candidate.toUpperCase(Locale.ROOT).replace("-", "_");
        if ("CUSTOM_MOB".equals(normalized)) return QuestObjectiveType.CUSTOM_MOB_KILL;
        if ("VANILLA_ENTITY".equals(normalized)) return QuestObjectiveType.VANILLA_ENTITY_KILL;
        if ("MONSTER_TAG".equals(normalized)) return QuestObjectiveType.MONSTER_TAG_KILL;
        return QuestObjectiveType.valueOf(normalized);
    }

    private String parseTarget(Object rawTarget) {
        if (rawTarget instanceof Map<?, ?> targetMap) {
            String id = stringValue(targetMap, "id", "");
            return id.isBlank() ? stringValue(targetMap, "target", "") : id;
        }
        return rawTarget == null ? "" : String.valueOf(rawTarget);
    }

    private List<QuestReward> parseRewards(String questId, List<Map<?, ?>> rawRewards) {
        List<QuestReward> rewards = new ArrayList<>();
        for (Map<?, ?> rawReward : rawRewards) {
            String rawType = stringValue(rawReward, "type", "");
            try {
                QuestRewardType type = QuestRewardType.valueOf(rawType.toUpperCase(Locale.ROOT).replace("-", "_"));
                rewards.add(new QuestReward(
                        type,
                        stringValue(rawReward, "target", ""),
                        longValue(rawReward, "amount", 0L)
                ));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignoring invalid quest reward in " + questId + ": " + rawReward);
            }
        }
        return rewards;
    }

    private String stringValue(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private int intValue(Map<?, ?> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private long longValue(Map<?, ?> map, String key, long defaultValue) {
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

    private String normalizeId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
