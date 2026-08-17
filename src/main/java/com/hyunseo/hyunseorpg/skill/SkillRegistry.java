package com.hyunseo.hyunseorpg.skill;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.weapon.WeaponType;
import org.bukkit.Material;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Skill definitions are data-driven and indexed by weapon type plus input. */
public final class SkillRegistry {
    private final ConfigService configService;
    private final Map<String, SkillData> skillsById = new ConcurrentHashMap<>();
    private final Map<String, SkillData> skillsByWeaponInput = new ConcurrentHashMap<>();

    public SkillRegistry(ConfigService configService) {
        this.configService = configService;
    }

    public void load() {
        skillsById.clear();
        skillsByWeaponInput.clear();
        for (String rawId : configService.getSkillsKeys("skills")) {
            String id = normalize(rawId);
            String path = "skills." + rawId;
            WeaponType weaponType = WeaponType.fromInput(configService.getSkillsString(path + ".weapon-type", "")).orElse(null);
            SkillInputType inputType = parseInputType(configService.getSkillsString(path + ".input", ""));
            if (weaponType == null || inputType == null) {
                continue;
            }
            register(new SkillData(
                    id,
                    weaponType,
                    inputType,
                    configService.getSkillsString(path + ".display-name", id),
                    parseMaterial(configService.getSkillsString(path + ".icon", "BOOK")),
                    Math.max(1, configService.getSkillsInt(path + ".max-level", 5)),
                    Math.max(0.0D, configService.getSkillsDouble(path + ".mana-cost", 0.0D)),
                    Math.max(0.0D, configService.getSkillsDouble(path + ".cooldown-seconds", 0.0D)),
                    Math.max(1, configService.getSkillsInt(path + ".required-proficiency-level", 1))
            ));
        }
    }

    public Optional<SkillData> getSkill(String skillId) {
        return Optional.ofNullable(skillsById.get(normalize(skillId)));
    }

    public Optional<SkillData> getSkill(WeaponType weaponType, SkillInputType inputType) {
        return Optional.ofNullable(skillsByWeaponInput.get(key(weaponType, inputType)));
    }

    public List<SkillData> getSkills(WeaponType weaponType) {
        return getAll().stream().filter(skill -> skill.weaponType() == weaponType).toList();
    }

    public List<SkillData> getAll() {
        return skillsById.values().stream().sorted(Comparator.comparing(SkillData::skillId)).toList();
    }

    private void register(SkillData skillData) {
        skillsById.put(normalize(skillData.skillId()), skillData);
        skillsByWeaponInput.put(key(skillData.weaponType(), skillData.inputType()), skillData);
    }

    private String key(WeaponType weaponType, SkillInputType inputType) {
        return weaponType.id() + ":" + inputType.configKey();
    }

    private SkillInputType parseInputType(String input) {
        try {
            return SkillInputType.valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Material parseMaterial(String input) {
        try {
            return Material.valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Material.BOOK;
        }
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
