package com.hyunseo.hyunseorpg.activity;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.economy.CoinService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Pays the same configurable coin reward for an activity regardless of lifestyle selection. */
public final class ActivityCoinRewardService {
    private final JavaPlugin plugin;
    private final ConfigService config;
    private final CoinService coins;
    private final Map<UUID, EnumMap<ActivityType, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Pending> pending = new HashMap<>();

    public ActivityCoinRewardService(JavaPlugin plugin, ConfigService config, CoinService coins) {
        this.plugin = plugin;
        this.config = config;
        this.coins = coins;
    }

    public void reward(Player player, ActivityType activity, Location location) {
        rewardInternal(player, activity, location, path(activity), path(activity));
    }

    /** Pays a sub-reward while sharing the parent activity cooldown and world rules. */
    public void rewardVariant(Player player, ActivityType activity, Location location, String variant) {
        if (variant == null || variant.isBlank()) {
            reward(player, activity, location);
            return;
        }
        String base = path(activity);
        rewardInternal(player, activity, location,
                base + ".tiers." + variant.trim().toUpperCase(java.util.Locale.ROOT), base);
    }

    private void rewardInternal(Player player, ActivityType activity, Location location,
                                String root, String baseRoot) {
        if (player == null || activity == null || !config.getProgressionLoopBoolean("activity-coins.enabled", true)) return;
        if (player.getGameMode() != GameMode.SURVIVAL
                && !(player.getGameMode() == GameMode.ADVENTURE
                && config.getProgressionLoopBoolean(path(activity) + ".allow-adventure", false))) return;

        if (!config.getProgressionLoopBoolean(baseRoot + ".enabled", true)
                || !config.getProgressionLoopBoolean(root + ".enabled", true)) return;
        if (!isAllowedWorld(player, baseRoot)) return;
        if (onCooldown(player, activity, baseRoot)) return;
        if (Math.random() > clamp(config.getProgressionLoopDouble(root + ".chance",
                config.getProgressionLoopDouble(baseRoot + ".chance", 1.0D)))) return;

        int minimum = Math.max(0, config.getProgressionLoopInt(root + ".min-coins",
                config.getProgressionLoopInt(root + ".coins",
                        config.getProgressionLoopInt(baseRoot + ".min-coins",
                                config.getProgressionLoopInt(baseRoot + ".coins", 0)))));
        int maximum = Math.max(minimum, config.getProgressionLoopInt(root + ".max-coins",
                config.getProgressionLoopInt(baseRoot + ".max-coins", minimum)));
        long amount = minimum == maximum ? minimum
                : minimum + (long) (Math.random() * (maximum - minimum + 1));
        double multiplier = config.getProgressionLoopDouble(root + ".multiplier",
                config.getProgressionLoopDouble(baseRoot + ".multiplier", 1.0D));
        if (location != null) {
            multiplier *= config.getProgressionLoopDouble(root + ".world-multipliers."
                    + location.getWorld().getName(), 1.0D);
        }
        amount = Math.max(0L, Math.round(amount * Math.max(0.0D, multiplier)));
        if (amount <= 0L) return;

        coins.addCoins(player, amount);
        queueFeedback(player, amount, activity);
    }

    public void clear() {
        cooldowns.clear();
        pending.clear();
    }

    private boolean isAllowedWorld(Player player, String root) {
        Set<String> worlds = new HashSet<>();
        for (String world : config.getProgressionLoopStringList(root + ".allowed-worlds")) {
            worlds.add(world.toLowerCase(java.util.Locale.ROOT));
        }
        return worlds.isEmpty() || worlds.contains(player.getWorld().getName().toLowerCase(java.util.Locale.ROOT));
    }

    private boolean onCooldown(Player player, ActivityType activity, String root) {
        long cooldown = Math.max(0L, config.getProgressionLoopLong(root + ".cooldown-ticks", 0L));
        if (cooldown <= 0L) return false;
        long now = System.currentTimeMillis();
        EnumMap<ActivityType, Long> byActivity = cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new EnumMap<>(ActivityType.class));
        long until = byActivity.getOrDefault(activity, 0L);
        if (until > now) return true;
        byActivity.put(activity, now + cooldown * 50L);
        return false;
    }

    private void queueFeedback(Player player, long amount, ActivityType activity) {
        if (!config.getProgressionLoopBoolean("activity-coins.feedback.actionbar", true)) return;
        UUID id = player.getUniqueId();
        Pending state = pending.computeIfAbsent(id, ignored -> new Pending());
        state.amount += amount;
        state.activities.add(activity);
        if (state.scheduled) return;
        state.scheduled = true;
        long window = Math.max(1L, config.getProgressionLoopLong("activity-coins.feedback.aggregate-window-ticks", 40L));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> flushFeedback(id), window);
    }

    private void flushFeedback(UUID id) {
        Pending state = pending.remove(id);
        if (state == null) return;
        Player player = plugin.getServer().getPlayer(id);
        if (player == null || !player.isOnline()) return;
        String activity = state.activities.size() == 1
                ? activityName(state.activities.iterator().next()) : "활동";
        player.sendActionBar(Component.text("+" + state.amount + " 코인 (" + activity + ")", NamedTextColor.GOLD));
    }

    private String path(ActivityType activity) {
        return "activity-coins.activities." + activity.name();
    }

    private double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private String activityName(ActivityType activity) {
        return switch (activity) {
            case MINING -> "채굴";
            case LOGGING -> "벌목";
            case FARMING -> "농사";
            case HUNTING -> "사냥";
            case FISHING -> "낚시";
            case HUSBANDRY -> "사육";
            case REPAIRING -> "수리";
        };
    }

    private static final class Pending {
        private long amount;
        private boolean scheduled;
        private final Set<ActivityType> activities = new HashSet<>();
    }
}
