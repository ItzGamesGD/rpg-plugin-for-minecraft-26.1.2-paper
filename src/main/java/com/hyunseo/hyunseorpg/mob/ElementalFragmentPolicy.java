package com.hyunseo.hyunseorpg.mob;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;

/** Allows elemental fragments only for explicitly registered elemental RPG mobs. */
public final class ElementalFragmentPolicy {
    private final ConfigService config;
    private final MobService mobs;
    private final PlayerDataService playerData;

    public ElementalFragmentPolicy(ConfigService config, MobService mobs, PlayerDataService playerData) {
        this.config = config;
        this.mobs = mobs;
        this.playerData = playerData;
    }

    public Optional<String> allowedItem(Player player, LivingEntity defeated) {
        if (player == null || defeated == null) return Optional.empty();
        if (!config.getMobsBoolean("elemental-fragments.enabled", true)) return Optional.empty();
        String mobId = mobs.getMobId(defeated);
        if (mobId.isBlank()) return Optional.empty();
        var section = mobs.getMobRegistry().getSection(mobId).orElse(null);
        String root = "elemental-fragments.mobs." + mobId;
        boolean enabled = section != null && section.getBoolean("elemental-fragment.enabled", false)
                || config.getMobsBoolean(root + ".enabled", false);
        if (!enabled) return Optional.empty();
        int required = Math.max(1, config.getMobsInt(root + ".min-rpg-level",
                section == null ? config.getMobsInt("elemental-fragments.default-min-rpg-level", 1)
                        : section.getInt("elemental-fragment.min-rpg-level", config.getMobsInt("elemental-fragments.default-min-rpg-level", 1))));
        if (playerData.getOrLoad(player).getBaseLevel() < required) return Optional.empty();
        String itemId = config.getMobsString(root + ".item-id", section == null ? "" : section.getString("elemental-fragment.item-id", ""))
                .trim().toLowerCase(Locale.ROOT);
        return itemId.isBlank() ? Optional.empty() : Optional.of(itemId);
    }

    public boolean allows(Player player, LivingEntity defeated, String itemId) {
        return allowedItem(player, defeated).filter(itemId::equalsIgnoreCase).isPresent();
    }

    public static boolean isElementalFragment(String itemId) {
        if (itemId == null) return false;
        return switch (itemId.toLowerCase(Locale.ROOT)) {
            case "fire_fragment", "water_fragment", "wind_fragment", "earth_fragment", "ice_fragment" -> true;
            default -> false;
        };
    }
}
