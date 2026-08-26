package com.hyunseo.hyunseorpg.item;

import com.hyunseo.hyunseorpg.economy.CoinService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Persistent overflow queue for item and coin rewards. All Bukkit inventory access stays on the main thread. */
public final class PendingRewardService {
    private final JavaPlugin plugin;
    private final CoinService coins;
    private final File file;
    private final Map<UUID, List<PendingReward>> pending = new LinkedHashMap<>();
    /** Idempotent mailbox tokens survive claim/removal; normal random rewards are not recorded here. */
    private final Map<UUID, Long> completedTokens = new LinkedHashMap<>();
    private Consumer<ItemStack> itemNormalizer = item -> { };
    private boolean dirty;

    public PendingRewardService(JavaPlugin plugin, CoinService coins) {
        this.plugin = plugin;
        this.coins = coins;
        this.file = new File(plugin.getDataFolder(), "pending-rewards.yml");
        load();
    }

    public synchronized int count(UUID uuid) {
        return pending.getOrDefault(uuid, List.of()).size();
    }

    public void setItemNormalizer(Consumer<ItemStack> itemNormalizer) {
        this.itemNormalizer = itemNormalizer == null ? item -> { } : itemNormalizer;
    }

    public synchronized void deliverOrQueue(Player player, ItemStack item, String cause) {
        if (player == null || item == null || item.getType().isAir() || item.getAmount() <= 0) return;
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
        for (ItemStack leftover : leftovers.values()) queueItem(player.getUniqueId(), leftover, cause);
        if (!leftovers.isEmpty()) notifyPending(player);
    }

    public synchronized void queueItem(UUID uuid, ItemStack item, String cause) {
        if (uuid == null || item == null || item.getType().isAir() || item.getAmount() <= 0) return;
        add(uuid, PendingReward.item(UUID.randomUUID(), uuid, item, cause));
    }

    /** Durable idempotent mailbox enqueue. The token is persisted as the pending reward id. */
    public synchronized boolean queueItemOnce(UUID uuid, UUID token, ItemStack item, String cause) {
        if (uuid == null || token == null || item == null || item.getType().isAir() || item.getAmount() <= 0) return false;
        if (completedTokens.containsKey(token)) return true;
        List<PendingReward> existing = pending.getOrDefault(uuid, List.of());
        if (existing.stream().anyMatch(reward -> token.equals(reward.id()))) {
            // A previous enqueue may have populated memory but failed its file write.
            // Retry that durable flush instead of treating the uncertain item as committed.
            return !dirty || save();
        }
        return add(uuid, PendingReward.item(token, uuid, item, cause));
    }

    public synchronized void queueCoins(UUID uuid, long amount, String cause) {
        if (uuid == null || amount <= 0L) return;
        add(uuid, PendingReward.coins(UUID.randomUUID(), uuid, amount, cause));
    }

    public synchronized int claim(Player player) {
        if (player == null) return 0;
        UUID uuid = player.getUniqueId();
        List<PendingReward> rewards = new ArrayList<>(pending.getOrDefault(uuid, List.of()));
        int claimed = 0;
        List<PendingReward> remaining = new ArrayList<>();
        for (PendingReward reward : rewards) {
            if (reward.coins() > 0L) {
                coins.addCoins(player, reward.coins());
                claimed++;
                continue;
            }
            ItemStack item = reward.item();
            if (item == null) continue;
            itemNormalizer.accept(item);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            if (leftovers.isEmpty()) {
                // Only deterministic queueItemOnce tokens are eligible for durable tombstones.
                // A Pyramid token is structure-scoped and must remain idempotent after claim.
                if (reward.cause().startsWith("exploration:")) completedTokens.put(reward.id(), System.currentTimeMillis());
                claimed++;
            } else {
                ItemStack left = leftovers.values().iterator().next();
                itemNormalizer.accept(left);
                remaining.add(reward.withItem(left));
            }
        }
        if (remaining.isEmpty()) pending.remove(uuid); else pending.put(uuid, remaining);
        dirty = true;
        save();
        if (claimed > 0) player.sendMessage(Component.text("미수령 보상 " + claimed + "개를 수령했습니다.", NamedTextColor.GREEN));
        return claimed;
    }

    public synchronized void notifyPending(Player player) {
        int count = count(player.getUniqueId());
        player.sendMessage(Component.text("인벤토리 공간 부족으로 보상이 보관되었습니다. /rpg pending claim", NamedTextColor.YELLOW));
        if (count > 0) player.sendActionBar(Component.text("미수령 보상: " + count + "개", NamedTextColor.YELLOW));
    }

    public synchronized boolean save() {
        if (!dirty) return true;
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("schema-version", 2);
        for (Map.Entry<UUID, Long> token : completedTokens.entrySet())
            yaml.set("completed-tokens." + token.getKey(), token.getValue());
        for (Map.Entry<UUID, List<PendingReward>> entry : pending.entrySet()) {
            int index = 0;
            for (PendingReward reward : entry.getValue()) {
                String root = "players." + entry.getKey() + ".rewards." + index++;
                yaml.set(root + ".id", reward.id().toString());
                yaml.set(root + ".cause", reward.cause());
                yaml.set(root + ".created-at", reward.createdAt());
                yaml.set(root + ".coins", reward.coins());
                if (reward.item() != null) yaml.set(root + ".item", reward.item().serialize());
            }
        }
        try {
            File temp = new File(file.getParentFile(), file.getName() + ".tmp");
            if (!temp.getParentFile().exists() && !temp.getParentFile().mkdirs()) throw new IOException("cannot create data folder");
            yaml.save(temp);
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            dirty = false;
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to save pending rewards", exception);
            return false;
        }
    }

    private boolean add(UUID uuid, PendingReward reward) {
        if (reward.item() != null) itemNormalizer.accept(reward.item());
        pending.computeIfAbsent(uuid, ignored -> new ArrayList<>()).add(reward);
        dirty = true;
        return save();
    }

    private void load() {
        if (!file.isFile()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection completed = yaml.getConfigurationSection("completed-tokens");
        if (completed != null) for (String token : completed.getKeys(false)) {
            try { completedTokens.put(UUID.fromString(token), completed.getLong(token)); } catch (IllegalArgumentException ignored) { }
        }
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) return;
        for (String rawUuid : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(rawUuid);
                ConfigurationSection rewards = players.getConfigurationSection(rawUuid + ".rewards");
                if (rewards == null) continue;
                for (String key : rewards.getKeys(false)) {
                    ConfigurationSection section = rewards.getConfigurationSection(key);
                    if (section == null) continue;
                    ItemStack item = null;
                    Map<String, Object> serialized = section.getConfigurationSection("item") == null
                            ? null : section.getConfigurationSection("item").getValues(false);
                    if (serialized != null) {
                        try { item = ItemStack.deserialize(serialized); } catch (RuntimeException ignored) { }
                    }
                    pending.computeIfAbsent(uuid, ignored -> new ArrayList<>()).add(new PendingReward(
                            parseUuid(section.getString("id")), uuid, section.getLong("created-at", System.currentTimeMillis()),
                            section.getString("cause", "unknown"), section.getLong("coins", 0L), item));
                }
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid pending reward player UUID: " + rawUuid);
            }
        }
    }

    private UUID parseUuid(String raw) {
        try { return UUID.fromString(raw); } catch (Exception ignored) { return UUID.randomUUID(); }
    }

    private record PendingReward(UUID id, UUID playerUuid, long createdAt, String cause, long coins, ItemStack item) {
        static PendingReward item(UUID id, UUID uuid, ItemStack item, String cause) {
            return new PendingReward(id, uuid, System.currentTimeMillis(), cause == null ? "unknown" : cause, 0L, item.clone());
        }
        static PendingReward coins(UUID id, UUID uuid, long amount, String cause) {
            return new PendingReward(id, uuid, System.currentTimeMillis(), cause == null ? "unknown" : cause, amount, null);
        }
        PendingReward withItem(ItemStack replacement) {
            return new PendingReward(id, playerUuid, createdAt, cause, coins, replacement.clone());
        }
    }
}
