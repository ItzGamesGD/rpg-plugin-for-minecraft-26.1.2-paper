package com.hyunseo.hyunseorpg.boss;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.item.InventoryDeliveryService;
import com.hyunseo.hyunseorpg.item.PendingRewardService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.economy.CoinService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BossSessionManager implements Listener {
    private final JavaPlugin plugin;
    private final ConfigService config;
    private final BossRewardService rewardService;
    private final Map<UUID, BossSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> respawnCooldownRemaining = new ConcurrentHashMap<>();
    private final File cooldownFile;
    private final Map<UUID, TextDisplay> warningDisplays = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> warningTasks = new ConcurrentHashMap<>();
    private BukkitTask ticker;

    public BossSessionManager(JavaPlugin plugin, ConfigService config, RPGItemService itemService,
                              InventoryDeliveryService delivery, CoinService coins, PlayerDataService playerData,
                              PendingRewardService pendingRewards) {
        this.plugin = plugin;
        this.config = config;
        this.cooldownFile = new File(plugin.getDataFolder(), "boss-cooldowns.yml");
        this.rewardService = new BossRewardService(config, itemService, delivery, coins, playerData, pendingRewards);
    }

    public void start() {
        if (ticker != null) return;
        loadCooldowns();
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(LivingEntity.class)) {
                BossType.from(entity.getType()).ifPresent(type -> {
                    if (enabled(type) && !sessions.containsKey(entity.getUniqueId())) startSession((LivingEntity) entity, type);
                });
            }
        }
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (ticker != null) ticker.cancel();
        ticker = null;
        saveCooldowns();
        for (BossSession session : new ArrayList<>(sessions.values())) end(session, BossEndReason.CANCELLED, false);
        sessions.clear();
        respawnCooldownRemaining.clear();
        for (BukkitTask task : warningTasks.values()) task.cancel();
        warningTasks.clear();
        for (TextDisplay display : warningDisplays.values()) if (display != null && display.isValid()) display.remove();
        warningDisplays.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSpawn(EntitySpawnEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        BossType.from(entity.getType()).ifPresent(type -> {
            if (!enabled(type) || hasActiveSession(type, entity.getWorld().getUID())
                    || respawnRemainingSeconds(type, entity.getWorld().getUID()) > 0L) {
                event.setCancelled(true);
                return;
            }
            startSession(entity, type);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        BossSession session = sessions.get(event.getEntity().getUniqueId());
        Player player = attackingPlayer(event.getDamager());
        if (player == null) return;
        if (event.getEntity() instanceof EnderCrystal) {
            session = sessions.values().stream().filter(value -> value.bossType() == BossType.ENDER_DRAGON
                    && !value.ended() && value.worldUuid() != null
                    && value.worldUuid().equals(player.getWorld().getUID())).findFirst().orElse(null);
            if (session != null) {
                double percent = Math.max(0.0D, config.getBossesDouble(
                        "boss-sessions.ender-dragon.contribution.end-crystal.score-percent-of-boss-max-health", 0.03D));
                Entity boss = Bukkit.getEntity(session.bossUuid());
                double maxHealth = boss instanceof LivingEntity living && living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                        ? living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() : 200.0D;
                session.addCrystalContribution(event.getEntity().getUniqueId(), player.getUniqueId(), maxHealth * percent);
            }
            return;
        }
        if (session == null || session.ended()) return;
        if (player.getWorld().getName().equalsIgnoreCase(session.worldName())) {
            double health = ((LivingEntity) event.getEntity()).getHealth();
            double effective = Math.max(0.0D, Math.min(event.getFinalDamage(), health));
            double weight = config.getBossesDouble("boss-sessions." + session.bossType().configId()
                    + ".contribution.damage-weight", 1.0D);
            session.addDamage(player.getUniqueId(), effective * Math.max(0.0D, weight));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        BossType type = BossType.from(event.getEntity().getType()).orElse(null);
        if (type == null) return;
        BossSession session = sessions.get(event.getEntity().getUniqueId());
        if (session == null) {
            // Recover the cooldown even when the entity was loaded before the session listener
            // or another plugin removed the session record before EntityDeathEvent.
            setRespawnCooldown(type, event.getEntity().getWorld().getUID());
            return;
        }
        if (session.ended()) return;
        if (System.currentTimeMillis() - session.startTimeMillis() >= session.durationSeconds() * 1000L) {
            end(session, BossEndReason.TIMEOUT, false);
            return;
        }
        if (session.contributionScores().isEmpty()) {
            end(session, BossEndReason.BOSS_INVALID, false);
            return;
        }
        rewardService.distribute(session);
        session.setRewardGiven(true);
        end(session, BossEndReason.CLEARED, false);
    }

    public boolean startTest(Player player, BossType type) {
        if (player == null || type == null || !enabled(type)) return false;
        if (hasActiveSession(type, player.getWorld().getUID()) || respawnRemainingSeconds(type, player.getWorld().getUID()) > 0L) return false;
        Entity entity = player.getWorld().spawnEntity(player.getLocation(), type.entityType());
        if (!(entity instanceof LivingEntity living)) return false;
        BossSession session = startSession(living, type);
        if (session == null) return false;
        session.addParticipant(player.getUniqueId());
        return true;
    }

    public boolean forceComplete(Player player, BossType type) {
        BossSession session = find(type, player == null ? null : player.getWorld().getName());
        if (session == null) return false;
        if (System.currentTimeMillis() - session.startTimeMillis() >= session.durationSeconds() * 1000L) {
            end(session, BossEndReason.TIMEOUT, true);
            return false;
        }
        addNearbyParticipants(session, player.getLocation());
        rewardService.distribute(session);
        session.setRewardGiven(true);
        end(session, BossEndReason.CLEARED, false);
        return true;
    }

    public boolean endFor(Player player, BossType type) {
        BossSession session = find(type, player == null ? null : player.getWorld().getName());
        if (session == null) return false;
        end(session, BossEndReason.CANCELLED, true);
        return true;
    }

    public List<BossSession> getSessions() { return List.copyOf(sessions.values()); }

    public BossSession getSession(UUID bossUuid) { return sessions.get(bossUuid); }

    public long remainingSeconds(BossType type) {
        BossSession session = sessions.values().stream()
                .filter(value -> value.bossType() == type && !value.ended())
                .findFirst().orElse(null);
        if (session == null) return 0L;
        return Math.max(0L, session.durationSeconds()
                - ((System.currentTimeMillis() - session.startTimeMillis()) / 1000L));
    }

    public long respawnRemainingSeconds(BossType type) {
        if (type == null) return 0L;
        return respawnCooldownRemaining.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(type.configId() + ":"))
                .mapToLong(Map.Entry::getValue).max().orElse(0L);
    }

    private long respawnRemainingSeconds(BossType type, UUID worldUuid) {
        if (type == null || worldUuid == null) return respawnRemainingSeconds(type);
        return Math.max(0L, respawnCooldownRemaining.getOrDefault(cooldownKey(type, worldUuid), 0L));
    }

    public boolean isSummonBlocked(BossType type) {
        if (type == null || !enabled(type)) return false;
        if (respawnRemainingSeconds(type) > 0L || hasActiveSession(type)) return true;
        // A boss can survive a reload or be loaded before the session listener sees its spawn.
        // Rebuild the session before allowing another vanilla summon.
        LivingEntity liveBoss = findLiveBoss(type);
        if (liveBoss == null) return false;
        startSession(liveBoss, type);
        return true;
    }

    public void showRemainingTime(Player player, BossType type) {
        if (player == null || type == null) return;
        UUID uuid = player.getUniqueId();
        TextDisplay existing = warningDisplays.get(uuid);
        if (existing != null && existing.isValid()) return;

        long combatRemaining = remainingSeconds(type);
        long cooldownRemaining = combatRemaining > 0L ? 0L : respawnRemainingSeconds(type);
        String message = combatRemaining > 0L
                ? formatTime("\uC81C\uD55C\uC2DC\uAC04", combatRemaining)
                : cooldownRemaining > 0L
                ? formatTime("\uC7AC\uC18C\uD658 \uB300\uAE30\uC2DC\uAC04", cooldownRemaining)
                : "\uD604\uC7AC \uC18C\uD658 \uAC00\uB2A5";
        long minutes = combatRemaining / 60L;
        long seconds = combatRemaining % 60L;
        TextDisplay display = player.getWorld().spawn(
                player.getLocation().clone().add(0.0D, 2.2D, 0.0D), TextDisplay.class);
        display.setText("제한시간: " + minutes + "분 " + seconds + "초");
        display.setText(message);
        display.setBillboard(Display.Billboard.CENTER);
        display.setSeeThrough(true);
        display.setShadowed(true);
        display.setViewRange(24.0F);
        warningDisplays.put(uuid, display);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            warningDisplays.remove(uuid, display);
            warningTasks.remove(uuid);
            if (display.isValid()) display.remove();
        }, 100L);
        warningTasks.put(uuid, task);
    }

    public String status(BossType type) {
        BossSession session = sessions.values().stream().filter(value -> value.bossType() == type && !value.ended()).findFirst().orElse(null);
        if (session == null) return "대기 중";
        long remaining = Math.max(0L, session.durationSeconds() - ((System.currentTimeMillis() - session.startTimeMillis()) / 1000L));
        return "진행 중 / 남은 시간 " + remaining + "초 / 참여자 " + session.participants().size();
    }

    public List<String> configuredBossIds() {
        return config.getBossesKeys("boss-sessions").stream()
                .filter(id -> !id.equalsIgnoreCase("enabled"))
                .toList();
    }

    public String status(String bossId) {
        return BossType.fromInput(bossId).map(this::status)
                .orElse("대기 중 / 제한시간 " + configuredDuration(bossId) + "초");
    }

    public List<String> menuLore(String bossId) {
        BossType type = BossType.fromInput(bossId).orElse(null);
        if (type != null) return menuLore(type);
        String root = "boss-sessions." + bossId;
        long duration = configuredDuration(bossId);
        long radius = Math.round(config.getBossesDouble(root + ".participation-radius", 80.0D));
        long coins = config.getBossesLong(root + ".rewards.coins",
                config.getBossesLong(root + ".first-clear-rewards.coins", 0L));
        return List.of("제한 시간: " + duration + "초", "참여 반경: " + radius + "블록",
                "고정 보상 코인: " + coins);
    }

    public List<String> menuLore(BossType type) {
        String root = "boss-sessions." + type.configId();
        long duration = config.getBossesLong(root + ".duration-seconds", type == BossType.WITHER ? 600L : 1200L);
        long radius = Math.round(config.getBossesDouble(root + ".participation-radius", type == BossType.WITHER ? 80.0D : 160.0D));
        List<String> lore = new ArrayList<>();
        lore.add("제한 시간: " + duration + "초");
        lore.add("참여 반경: " + radius + "블록");
        lore.add("제한 시간 안에 처치한 참여자만 보상");
        long fixedCoins = config.getBossesLong(root + ".rewards.coins",
                config.getBossesLong(root + ".first-clear-rewards.coins", 0L));
        lore.add("고정 보상 코인: " + fixedCoins);
        lore.add("마석 파편과 보스 전리품은 일반 사냥에서도 획득 가능");
        return lore;
    }

    private BossSession startSession(LivingEntity entity, BossType type) {
        if (!enabled(type) || sessions.containsKey(entity.getUniqueId())) return sessions.get(entity.getUniqueId());
        long duration = config.getBossesLong("boss-sessions." + type.configId() + ".duration-seconds", type == BossType.WITHER ? 600L : 1200L);
        BossBar bar = Bukkit.createBossBar(type.configId(), BarColor.RED, BarStyle.SOLID);
        BossSession session = new BossSession(type, entity.getUniqueId(), entity.getWorld().getUID(), entity.getWorld().getName(),
                System.currentTimeMillis(), Math.max(1L, duration), bar);
        sessions.put(entity.getUniqueId(), session);
        addNearbyParticipants(session, entity.getLocation());
        updateBossBar(session, entity.getLocation());
        plugin.getLogger().info("Boss session started: " + type.configId() + " " + entity.getUniqueId());
        return session;
    }

    private void tick() {
        respawnCooldownRemaining.replaceAll((key, remaining) -> Math.max(0L, remaining - 1L));
        respawnCooldownRemaining.entrySet().removeIf(entry -> entry.getValue() <= 0L);
        for (BossSession session : new ArrayList<>(sessions.values())) {
            Entity boss = Bukkit.getEntity(session.bossUuid());
            if (!(boss instanceof LivingEntity living) || living.isDead() || !living.isValid()) {
                if (!session.ended()) end(session, BossEndReason.BOSS_INVALID, false);
                continue;
            }
            updateBossBar(session, living.getLocation());
            long elapsed = System.currentTimeMillis() - session.startTimeMillis();
            double progress = Math.max(0.0D, Math.min(1.0D, 1.0D - (elapsed / 1000.0D) / session.durationSeconds()));
            session.bossBar().setProgress(progress);
            if (elapsed >= session.durationSeconds() * 1000L) end(session, BossEndReason.TIMEOUT, true);
        }
    }

    private void addNearbyParticipants(BossSession session, Location location) {
        double radius = config.getBossesDouble("boss-sessions." + session.bossType().configId() + ".participation-radius",
                session.bossType() == BossType.WITHER ? 80.0D : 160.0D);
        double squared = radius * radius;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(location.getWorld()) || player.getLocation().distanceSquared(location) > squared) continue;
            session.addParticipant(player.getUniqueId());
            session.bossBar().addPlayer(player);
        }
    }

    private void end(BossSession session, BossEndReason reason, boolean removeBoss) {
        if (session.ended()) return;
        session.setEnded(true);
        session.setEndReason(reason);
        if (reason == BossEndReason.CLEARED) {
            setRespawnCooldown(session.bossType(), session.worldUuid());
        }
        session.bossBar().removeAll();
        sessions.remove(session.bossUuid());
        Entity entity = Bukkit.getEntity(session.bossUuid());
        if (removeBoss && entity != null && entity.isValid()) entity.remove();
    }

    private void updateBossBar(BossSession session, Location location) {
        double radius = config.getBossesDouble("boss-sessions." + session.bossType().configId() + ".participation-radius", 80.0D);
        double squared = radius * radius;
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean inside = player.getWorld().equals(location.getWorld())
                    && player.getLocation().distanceSquared(location) <= squared;
            if (inside) {
                session.bossBar().addPlayer(player);
                session.addParticipant(player.getUniqueId());
            } else {
                session.bossBar().removePlayer(player);
            }
        }
    }

    private BossSession find(BossType type, String worldName) {
        return sessions.values().stream().filter(value -> value.bossType() == type && !value.ended()
                && (worldName == null || value.worldName().equalsIgnoreCase(worldName))).findFirst().orElse(null);
    }

    private boolean enabled(BossType type) { return config.getBossesBoolean("boss-sessions." + type.configId() + ".enabled", true); }

    private boolean hasActiveSession(BossType type) {
        return sessions.values().stream().anyMatch(value -> value.bossType() == type && !value.ended());
    }

    private boolean hasActiveSession(BossType type, UUID worldUuid) {
        return sessions.values().stream().anyMatch(value -> value.bossType() == type && !value.ended()
                && worldUuid != null && worldUuid.equals(value.worldUuid()));
    }

    private LivingEntity findLiveBoss(BossType type) {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(LivingEntity.class)) {
                if (entity.getType() == type.entityType() && !entity.isDead() && entity.isValid()) {
                    return (LivingEntity) entity;
                }
            }
        }
        return null;
    }

    private void setRespawnCooldown(BossType type, UUID worldUuid) {
        long cooldown = configuredRespawnCooldown(type);
        if (worldUuid != null) respawnCooldownRemaining.put(cooldownKey(type, worldUuid), cooldown);
    }

    private String cooldownKey(BossType type, UUID worldUuid) {
        return type.configId() + ":" + worldUuid;
    }

    private void loadCooldowns() {
        if (!cooldownFile.isFile()) return;
        org.bukkit.configuration.file.YamlConfiguration yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(cooldownFile);
        org.bukkit.configuration.ConfigurationSection section = yaml.getConfigurationSection("cooldowns");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            long seconds = Math.max(0L, section.getLong(key, 0L));
            if (seconds > 0L) respawnCooldownRemaining.put(key, seconds);
        }
    }

    private void saveCooldowns() {
        if (respawnCooldownRemaining.isEmpty()) return;
        org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
        yaml.set("schema-version", 1);
        respawnCooldownRemaining.forEach((key, value) -> yaml.set("cooldowns." + key, Math.max(0L, value)));
        try {
            if (!cooldownFile.getParentFile().exists() && !cooldownFile.getParentFile().mkdirs()) throw new IOException("cannot create data folder");
            File temp = new File(cooldownFile.getParentFile(), cooldownFile.getName() + ".tmp");
            yaml.save(temp);
            Files.move(temp.toPath(), cooldownFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to save boss cooldowns", exception);
        }
    }

    private long configuredDuration(BossType type) {
        return Math.max(1L, config.getBossesLong("boss-sessions." + type.configId() + ".duration-seconds",
                type == BossType.WITHER ? 600L : 1200L));
    }

    private long configuredDuration(String bossId) {
        return Math.max(1L, config.getBossesLong("boss-sessions." + bossId + ".duration-seconds", 600L));
    }

    private long configuredRespawnCooldown(BossType type) {
        return configuredRespawnCooldown(type.configId());
    }

    private long configuredRespawnCooldown(String bossId) {
        return Math.max(0L, config.getBossesLong("boss-sessions." + bossId + ".respawn-cooldown-seconds", 1800L));
    }

    private String formatTime(String label, long totalSeconds) {
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return label + ": " + minutes + "\uBD84 " + seconds + "\uCD08";
    }

    private Player attackingPlayer(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }
}
