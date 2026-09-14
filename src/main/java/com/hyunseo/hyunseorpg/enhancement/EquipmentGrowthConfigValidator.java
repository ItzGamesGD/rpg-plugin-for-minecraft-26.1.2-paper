package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.core.config.ConfigService;


/** Validates the new growth configuration before registries consume it. */
public final class EquipmentGrowthConfigValidator {
    private final ConfigService config;

    public EquipmentGrowthConfigValidator(ConfigService config) {
        this.config = config;
    }

    public boolean validate() {
        boolean valid = true;
        int max = config.getEquipmentGrowthInt("enhancement.max-level", 0);
        int vanillaCap = config.getEquipmentGrowthInt("enhancement.caps.vanilla", 0);
        int elementalCap = config.getEquipmentGrowthInt("enhancement.caps.elemental", 0);
        if (max <= 0) valid = error("enhancement.max-level must be positive") && valid;
        if (vanillaCap <= 0) valid = error("enhancement.caps.vanilla must be positive") && valid;
        if (elementalCap <= 0) valid = error("enhancement.caps.elemental must be positive") && valid;
        if (max < Math.max(vanillaCap, elementalCap)) {
            valid = error("enhancement.max-level must cover every category cap") && valid;
        }
        if (config.getEquipmentGrowthInt("enhancement.xp-level-cost.default", 0) <= 0) {
            valid = error("enhancement.xp-level-cost.default must be positive") && valid;
        }
        for (String threshold : config.getEquipmentGrowthKeys("enhancement.xp-level-cost.levels")) {
            if (parseInt(threshold) <= 0 || config.getEquipmentGrowthInt(
                    "enhancement.xp-level-cost.levels." + threshold, 0) <= 0) {
                valid = error("invalid enhancement XP-level cost threshold: " + threshold) && valid;
            }
        }

        return valid;
    }

    private boolean error(String message) {
        config.getPlugin().getLogger().severe("Invalid equipment-growth.yml: " + message);
        return false;
    }

    private int parseInt(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return -1; }
    }
}
