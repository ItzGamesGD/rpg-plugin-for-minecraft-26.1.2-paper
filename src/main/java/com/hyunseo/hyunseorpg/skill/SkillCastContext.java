package com.hyunseo.hyunseorpg.skill;

import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import com.hyunseo.hyunseorpg.weapon.WeaponType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public final class SkillCastContext {
    private final Player caster;
    private final PlayerRPGData casterData;
    private final WeaponType weaponType;
    private final SkillInputType inputType;
    private final String skillName;
    private final int skillLevel;
    private final Location originLocation;
    private final Vector direction;
    private final Map<String, Object> metadata;

    public SkillCastContext(
            Player caster,
            PlayerRPGData casterData,
            WeaponType weaponType,
            SkillInputType inputType,
            String skillName,
            int skillLevel,
            Location originLocation,
            Vector direction,
            Map<String, Object> metadata
    ) {
        this.caster = caster;
        this.casterData = casterData;
        this.weaponType = weaponType;
        this.inputType = inputType;
        this.skillName = skillName;
        this.skillLevel = skillLevel;
        this.originLocation = originLocation.clone();
        this.direction = direction.clone();
        this.metadata = new HashMap<>(metadata);
    }

    public Player caster() {
        return caster;
    }

    public PlayerRPGData casterData() {
        return casterData;
    }

    public WeaponType weaponType() {
        return weaponType;
    }

    public SkillInputType inputType() {
        return inputType;
    }

    public String skillName() {
        return skillName;
    }

    public int skillLevel() {
        return skillLevel;
    }

    public Location originLocation() {
        return originLocation.clone();
    }

    public Vector direction() {
        return direction.clone();
    }

    public Map<String, Object> metadata() {
        return metadata;
    }
}
