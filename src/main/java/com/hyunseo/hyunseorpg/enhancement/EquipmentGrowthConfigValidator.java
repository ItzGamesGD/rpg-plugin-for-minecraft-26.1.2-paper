package com.hyunseo.hyunseorpg.enhancement;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Validates the new growth configuration before registries consume it. */
public final class EquipmentGrowthConfigValidator {
    private final ConfigService config;

    public EquipmentGrowthConfigValidator(ConfigService config) {
        this.config = config;
    }

    public boolean validate() {
        boolean valid = true;
        int max = config.getEquipmentGrowthInt("enhancement.max-level", 0);
        double start = config.getEquipmentGrowthDouble("enhancement.start-chance", -1.0D);
        double end = config.getEquipmentGrowthDouble("enhancement.final-chance", -1.0D);
        double failBonus = config.getEquipmentGrowthDouble("enhancement.fail-bonus", -1.0D);
        if (max <= 0) valid = error("enhancement.max-level must be positive") && valid;
        if (start < 0.0D || start > 1.0D) valid = error("enhancement.start-chance must be between 0 and 1") && valid;
        if (end < 0.0D || end > 1.0D) valid = error("enhancement.final-chance must be between 0 and 1") && valid;
        if (failBonus < 0.0D) valid = error("enhancement.fail-bonus cannot be negative") && valid;

        double lastCostProgress = config.getEquipmentGrowthKeys("enhancement.cost-curve").stream()
                .mapToDouble(this::parseDouble).max().orElse(0.0D);
        if (lastCostProgress < 0.999999D) valid = error("enhancement.cost-curve must reach progress 1.0") && valid;

        int previousOrder = 0;
        for (String grade : config.getEquipmentGrowthKeys("promotion.grades").stream()
                .sorted(Comparator.comparingInt(id -> config.getEquipmentGrowthInt("promotion.grades." + id + ".order", 0))).toList()) {
            int order = config.getEquipmentGrowthInt("promotion.grades." + grade + ".order", 0);
            int stars = config.getEquipmentGrowthInt("promotion.grades." + grade + ".stars", 0);
            int legacyRequirement = config.getEquipmentGrowthInt("promotion.grades." + grade + ".max-enhancement", 0);
            int requirement = config.getEquipmentGrowthInt(
                    "promotion.grades." + grade + ".required-enhancement", legacyRequirement);
            if (order <= previousOrder) valid = error("promotion.grades order is not strictly increasing: " + grade) && valid;
            if (stars <= 0) valid = error("promotion.grades." + grade + ".stars must be positive") && valid;
            if (requirement < 0 || requirement > max) {
                valid = error("promotion.grades." + grade + ".required-enhancement is outside max level") && valid;
            }
            previousOrder = order;
        }

        Set<String> definitions = config.getEquipmentGrowthKeys("promotion.option-definitions");
        for (String rawId : definitions) {
            String path = "promotion.option-definitions." + rawId + ".value";
            double min = config.getEquipmentGrowthDouble(path + ".min", Double.NaN);
            double maxValue = config.getEquipmentGrowthDouble(path + ".max", Double.NaN);
            double step = config.getEquipmentGrowthDouble(path + ".precision", Double.NaN);
            if (!Double.isFinite(min) || !Double.isFinite(maxValue) || !Double.isFinite(step)
                    || min < 0.0D || maxValue < min || step <= 0.0D) {
                valid = error("invalid promotion option definition: " + rawId) && valid;
            }
        }
        for (String profile : config.getEquipmentGrowthKeys("promotion.profiles")) {
            for (String pool : List.of("general-option-pool", "special-option-pool")) {
                for (String option : config.getEquipmentGrowthStringList("promotion.profiles." + profile + "." + pool)) {
                    if (!definitions.stream().anyMatch(id -> id.equalsIgnoreCase(option))) {
                        valid = error("promotion profile " + profile + " references unknown option: " + option) && valid;
                    } else if (!config.getEquipmentGrowthBoolean(
                            "promotion.option-definitions." + option + ".promotion-eligible", true)) {
                        valid = error("promotion profile " + profile
                                + " contains non-promotion option " + option + "; remove it from the pool") && valid;
                    }
                }
            }
        }
        for (String rawId : definitions) {
            String path = "promotion.option-definitions." + rawId;
            boolean eligible = config.getEquipmentGrowthBoolean(path + ".promotion-eligible", true);
            boolean legacyOnly = config.getEquipmentGrowthBoolean(path + ".legacy-only", !eligible);
            if (eligible && legacyOnly) {
                valid = error("promotion option " + rawId + " cannot be both promotion-eligible and legacy-only") && valid;
            }
        }
        return valid;
    }

    private boolean error(String message) {
        config.getPlugin().getLogger().severe("Invalid equipment-growth.yml: " + message);
        return false;
    }

    private double parseDouble(String value) {
        try { return Double.parseDouble(value); } catch (NumberFormatException ignored) { return -1.0D; }
    }
}
