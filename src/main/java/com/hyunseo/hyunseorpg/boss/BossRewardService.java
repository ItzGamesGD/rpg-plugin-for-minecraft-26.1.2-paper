package com.hyunseo.hyunseorpg.boss;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.item.PendingRewardService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Fixed-total boss reward distribution. The session owns contribution history; this class only settles it. */
public final class BossRewardService {
    private final ConfigService config;
    private final RPGItemService itemService;
    private final InventoryDeliveryService delivery;
    private final PlayerDataService playerData;
    private final PendingRewardService pendingRewards;

    public BossRewardService(ConfigService config, RPGItemService itemService, InventoryDeliveryService delivery,
                             PlayerDataService playerData, PendingRewardService pendingRewards) {
        this.config = config;
        this.itemService = itemService;
        this.delivery = delivery;
        this.playerData = playerData;
        this.pendingRewards = pendingRewards;
    }

    public void distribute(BossSession session) {
        if (session == null) return;
        BossType type = session.bossType();
        String root = "boss-sessions." + type.configId() + ".rewards";
        if (config.getBossesSection(root) == null) root = "boss-sessions." + type.configId() + ".first-clear-rewards";
        Map<UUID, Double> scores = session.contributionScores();
        double totalScore = scores.values().stream().filter(value -> value != null && value > 0.0D).mapToDouble(Double::doubleValue).sum();
        if (!(totalScore > 0.0D) || !Double.isFinite(totalScore)) return;

        double threshold = Math.max(0.0D, Math.min(1.0D,
                config.getBossesDouble(root + ".progress-min-contribution-ratio",
                        config.getBossesDouble("boss-sessions." + type.configId() + ".progress-min-contribution-ratio", 0.05D))));
        int experience = Math.max(0, config.getBossesInt(root + ".experience", 0));
        ConfigurationSection items = config.getBossesSection(root + ".items");
        Map<String, Integer> itemTotals = readItemTotals(items);
        for (Map.Entry<UUID, Double> entry : scores.entrySet()) {
            UUID uuid = entry.getKey();
            double ratio = entry.getValue() / totalScore;
            if (!(ratio > 0.0D)) continue;
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                if (experience > 0) player.giveExp(experience);
                giveItems(player, itemTotals, scores);
                player.sendMessage(Component.text(type.configId() + " 보상을 획득했습니다.", NamedTextColor.GOLD));
            } else {
                queueItems(uuid, itemTotals, scores);
            }
            if (ratio + 1.0E-9D >= threshold) recordProgress(uuid, type, player);
        }
    }

    private void giveItems(Player player, Map<String, Integer> totals, Map<UUID, Double> scores) {
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            long share = RewardAllocation.allocate(entry.getValue(), scores).getOrDefault(player.getUniqueId(), 0L);
            if (share <= 0L) continue;
            ItemStack item = itemService.create(entry.getKey(), (int) Math.min(Integer.MAX_VALUE, share)).orElse(null);
            if (item != null) delivery.giveOrDiscard(player, item);
        }
    }

    private void queueItems(UUID uuid, Map<String, Integer> totals, Map<UUID, Double> scores) {
        if (pendingRewards == null) return;
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            long share = RewardAllocation.allocate(entry.getValue(), scores).getOrDefault(uuid, 0L);
            if (share <= 0L) continue;
            ItemStack item = itemService.create(entry.getKey(), (int) Math.min(Integer.MAX_VALUE, share)).orElse(null);
            if (item != null) pendingRewards.queueItem(uuid, item, "boss:" + entry.getKey());
        }
    }

    private void recordProgress(UUID uuid, BossType type, Player online) {
        PlayerRPGData data = online == null ? playerData.loadPlayer(uuid) : playerData.getOrLoad(online);
        if (type == BossType.WITHER) {
            data.setFirstWitherClear(true);
            data.incrementWitherClearCount();
        } else {
            data.setFirstEnderDragonClear(true);
            data.incrementEnderDragonClearCount();
        }
        data.incrementBossKillCount(type.configId());
        if (online != null) playerData.savePlayer(online); else playerData.savePlayer(uuid);
    }

    private Map<String, Integer> readItemTotals(ConfigurationSection items) {
        if (items == null) return Map.of();
        Map<String, Integer> result = new java.util.LinkedHashMap<>();
        for (String itemId : items.getKeys(false)) {
            int amount = readAmount(items, itemId);
            if (amount > 0) result.put(itemId, amount);
        }
        return Map.copyOf(result);
    }

    private int readAmount(ConfigurationSection items, String itemId) {
        Object value = items.get(itemId);
        if (value instanceof Number number) return Math.max(0, number.intValue());
        ConfigurationSection range = items.getConfigurationSection(itemId);
        if (range == null) return 0;
        int min = Math.max(0, range.getInt("min", 0));
        int max = Math.max(min, range.getInt("max", min));
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
