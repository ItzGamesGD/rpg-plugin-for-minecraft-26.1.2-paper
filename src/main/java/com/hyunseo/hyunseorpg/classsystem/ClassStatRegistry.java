package com.hyunseo.hyunseorpg.classsystem;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassStatRegistry {
    private final ConfigService configService;
    private final Map<String, ClassStatData> statsByFullId = new ConcurrentHashMap<>();

    public ClassStatRegistry(ConfigService configService) {
        this.configService = configService;
    }

    public void load() {
        statsByFullId.clear();
        for (RPGClass rpgClass : RPGClass.values()) {
            String classPath = "class-stats." + rpgClass.id();
            for (String statId : configService.getClassesKeys(classPath)) {
                ClassStatData data = readStat(rpgClass, statId, classPath + "." + statId);
                statsByFullId.put(data.fullId(), data);
            }
        }
    }

    public List<ClassStatData> getStats(RPGClass rpgClass) {
        return statsByFullId.values().stream()
                .filter(data -> data.ownerClass() == rpgClass)
                .sorted(Comparator.comparing(ClassStatData::id))
                .toList();
    }

    public Optional<ClassStatData> get(String fullId) {
        return Optional.ofNullable(statsByFullId.get(normalize(fullId)));
    }

    public List<ClassStatData> getAll() {
        List<ClassStatData> values = new ArrayList<>(statsByFullId.values());
        values.sort(Comparator.comparing(ClassStatData::fullId));
        return values;
    }

    private ClassStatData readStat(RPGClass rpgClass, String statId, String path) {
        String displayName = configService.getClassesString(path + ".display-name", statId);
        Material icon = parseMaterial(configService.getClassesString(path + ".icon", "BOOK"));
        int maxLevel = Math.max(1, configService.getClassesInt(path + ".max-level", 8));
        int pointCost = Math.max(1, configService.getClassesInt(path + ".point-cost", 1));
        String skillId = normalize(configService.getClassesString(path + ".skill-id", statId));
        String targetValue = normalize(configService.getClassesString(path + ".target-value", "value"));
        double bonusPerLevel = configService.getClassesDouble(path + ".bonus-per-level", 1.0D);
        return new ClassStatData(normalize(statId), rpgClass, displayName, icon, maxLevel, pointCost, skillId, targetValue, bonusPerLevel);
    }

    private Material parseMaterial(String input) {
        try {
            return Material.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Material.BOOK;
        }
    }

    private String normalize(String input) {
        return input.trim().toLowerCase(Locale.ROOT);
    }
}
