package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.mob.MobData;
import com.hyunseo.hyunseorpg.mob.MobLevelScalingService;
import com.hyunseo.hyunseorpg.mob.MobService;
import com.hyunseo.hyunseorpg.mob.MobTagService;
import com.hyunseo.hyunseorpg.mob.MonsterBehaviorService;
import com.hyunseo.hyunseorpg.mythic.MythicCustomMobService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class RPGMobCommand implements CommandExecutor, TabCompleter {
    private final ConfigService configService;
    private final MobService mobService;
    private final MobLevelScalingService mobLevelScalingService;
    private final MobTagService mobTagService;
    private final MythicCustomMobService mythicCustomMobService;
    private final MonsterBehaviorService monsterBehaviorService;

    public RPGMobCommand(ConfigService configService, MobService mobService, MobLevelScalingService mobLevelScalingService,
                         MythicCustomMobService mythicCustomMobService, MonsterBehaviorService monsterBehaviorService) {
        this.configService = configService;
        this.mobService = mobService;
        this.mobLevelScalingService = mobLevelScalingService;
        this.mobTagService = mobService.getMobTagService();
        this.mythicCustomMobService = mythicCustomMobService;
        this.monsterBehaviorService = monsterBehaviorService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by a player.", NamedTextColor.RED));
            return true;
        }

        if (!hasAnyMobPermission(player)) {
            player.sendMessage(Component.text("You do not have permission to use RPG mob commands.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || isHelp(args[0])) {
            sendHelp(player, label);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info" -> handleInfo(player);
            case "spawn" -> handleSpawn(player, label, args);
            case "spawncustom", "spawncostom" -> handleSpawnCustom(player, label, args);
            case "clear" -> handleClear(player);
            case "debug" -> handleDebug(player, args);
            case "scaling" -> handleScaling(player, label, args);
            default -> {
                player.sendMessage(Component.text("Unknown RPG mob command: " + args[0], NamedTextColor.RED));
                sendHelp(player, label);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("info", "spawn", "spawncustom", "clear", "debug", "scaling"), args[0]);
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            if (args.length == 2) {
                return filter(livingEntityTypeNames(), args[1]);
            }
            if (args.length == 3) {
                return filter(List.of("rpg", "elite", "boss"), args[2]);
            }
            if (args.length == 4) {
                return filter(List.of("1", "5", "10", "20", "50"), args[3]);
            }
        }

        if (args[0].equalsIgnoreCase("spawncustom") || args[0].equalsIgnoreCase("spawncostom")) {
            if (args.length == 2) {
                List<String> ids = new java.util.ArrayList<>(mobService.getMobRegistry().getMobIds());
                ids.addAll(mythicCustomMobService.getConfiguredMobIds());
                return filter(ids, args[1]);
            }
            if (args.length == 3) {
                return filter(List.of("1", "5", "10", "20", "50"), args[2]);
            }
        }

        if (args[0].equalsIgnoreCase("debug") && args.length == 2) {
            return filter(List.of("on", "off"), args[1]);
        }

        if (args[0].equalsIgnoreCase("scaling")) {
            if (args.length == 2) {
                return filter(List.of("get", "set", "sample"), args[1]);
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
                return filter(List.copyOf(mobLevelScalingService.getScalingKeys()), args[2]);
            }
        }

        return List.of();
    }

    private boolean handleInfo(Player player) {
        if (!hasPermission(player, "hyunseorpg.mob.info")) {
            return true;
        }

        Optional<LivingEntity> target = findLookedAtMob(player).or(() ->
                mobService.findLookedAtOrNearestTaggedMob(player.getLocation(), getDefaultRadius())
        );
        if (target.isEmpty()) {
            player.sendMessage(Component.text("No looked-at or nearby RPG mob found.", NamedTextColor.YELLOW));
            return true;
        }

        LivingEntity entity = target.get();
        player.sendMessage(Component.text("RPG Mob Info", NamedTextColor.GOLD));
        player.sendMessage(Component.text("type=" + entity.getType().name(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("isRpgMob=" + mobTagService.isRpgMob(entity), NamedTextColor.GRAY));
        player.sendMessage(Component.text("tags=" + mobTagService.getTags(entity), NamedTextColor.GRAY));
        player.sendMessage(Component.text("mobId=" + mobTagService.getMobId(entity), NamedTextColor.GRAY));
        player.sendMessage(Component.text("mobLevel=" + mobTagService.getMobLevel(entity), NamedTextColor.GRAY));
        player.sendMessage(Component.text("displayName=" + mobTagService.getMobDisplayName(entity), NamedTextColor.GRAY));
        player.sendMessage(Component.text("boss=" + mobService.isBossMob(entity) + ", elite=" + mobService.isEliteMob(entity), NamedTextColor.GRAY));
        player.sendMessage(Component.text("baseExpReward=" + mobService.getBaseExpReward(entity), NamedTextColor.GRAY));
        player.sendMessage(Component.text("classExpReward=" + mobService.getClassExpReward(entity), NamedTextColor.GRAY));

        Optional<MobData> mobData = mobService.getMobData(entity);
        if (mobData.isPresent()) {
            MobData data = mobData.get();
            player.sendMessage(Component.text("region=" + data.region(), NamedTextColor.GRAY));
            player.sendMessage(Component.text("dropTable=" + data.dropTableId(), NamedTextColor.GRAY));
            player.sendMessage(Component.text("vanillaType=" + data.vanillaType().name(), NamedTextColor.GRAY));
        }

        mobService.getAbilityProfile(entity).ifPresent(profile ->
                player.sendMessage(Component.text("abilityProfile=" + profile.profileId() + ", abilities=" + profile.abilities().size(), NamedTextColor.GRAY))
        );
        player.sendMessage(Component.text("scoreboardTags=" + entity.getScoreboardTags(), NamedTextColor.DARK_GRAY));
        return true;
    }

    private boolean handleSpawn(Player player, String label, String[] args) {
        if (!hasPermission(player, "hyunseorpg.mob.spawn")) {
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /" + label + " spawn [mobType] [rpg|elite|boss] [level]", NamedTextColor.YELLOW));
            return true;
        }

        Optional<EntityType> entityType = parseEntityType(args[1]);
        if (entityType.isEmpty() || !entityType.get().isAlive() || !mobService.isManageableRpgCandidate(entityType.get())) {
            player.sendMessage(Component.text("Cannot spawn living mob type: " + args[1], NamedTextColor.RED));
            return true;
        }

        String category = args[2].toLowerCase(Locale.ROOT);
        if (!List.of("rpg", "elite", "boss").contains(category)) {
            player.sendMessage(Component.text("Category must be one of: rpg, elite, boss", NamedTextColor.RED));
            return true;
        }

        Integer level = parseOptionalLevel(player, args, 3);
        if (level == null && args.length >= 4) {
            return true;
        }

        Location spawnLocation = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(2.0D));
        LivingEntity spawned = mobService.spawnTaggedMob(spawnLocation, entityType.get(), category, level == null ? 1 : level);
        player.sendMessage(Component.text("Spawned RPG mob: " + spawned.getType().name()
                + " / " + category + " / level=" + mobTagService.getMobLevel(spawned), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSpawnCustom(Player player, String label, String[] args) {
        if (!hasPermission(player, "hyunseorpg.mob.spawn")) {
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /" + label + " spawncustom [mobId] [level]", NamedTextColor.YELLOW));
            return true;
        }

        Integer level = parseOptionalLevel(player, args, 2);
        if (level == null && args.length >= 3) {
            return true;
        }

        Location spawnLocation = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(2.0D));
        Optional<LivingEntity> spawned;
        if (mobService.getMobRegistry().get(args[1]).isPresent()) {
            // Native YAML monster definitions must use the core MobService path so
            // attributes, PDC identity, behavior handlers, and normal rewards agree.
            spawned = mobService.spawnCustomMob(spawnLocation, args[1], level);
        } else {
            spawned = mythicCustomMobService.spawnConfigured(spawnLocation, args[1], level);
        }
        if (spawned.isEmpty()) {
            player.sendMessage(Component.text("Unknown mobId: " + args[1], NamedTextColor.RED));
            List<String> ids = new java.util.ArrayList<>(mobService.getMobRegistry().getMobIds());
            ids.addAll(mythicCustomMobService.getConfiguredMobIds());
            player.sendMessage(Component.text("Registered mobIds: " + ids, NamedTextColor.GRAY));
            return true;
        }

        MobData data = mobService.getMobData(spawned.get()).orElse(null);
        String description = data == null ? args[1] : data.mobId() + " type=" + data.vanillaType().name();
        player.sendMessage(Component.text("Spawned custom RPG mob: " + description
                + " level=" + mobTagService.getMobLevel(spawned.get()), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleClear(Player player) {
        if (!hasPermission(player, "hyunseorpg.mob.spawn")) return true;
        int count = mythicCustomMobService.clearCustomMobs();
        player.sendMessage(Component.text("Removed tracked Mythic custom mobs: " + count, NamedTextColor.GREEN));
        return true;
    }

    private boolean handleDebug(Player player, String[] args) {
        if (!hasPermission(player, "hyunseorpg.mob.info")) return true;
        if (args.length == 1) {
            LivingEntity target = findLookedAtMob(player).orElse(null);
            if (target == null) {
                player.sendMessage(Component.text("바라보는 커스텀 몹을 찾을 수 없습니다.", NamedTextColor.YELLOW));
                return true;
            }
            List<String> debugLines = monsterBehaviorService.debugInfo(target);
            if (debugLines.isEmpty()) {
                player.sendMessage(Component.text("이 몹은 MonsterBehaviorService 네이티브 행동 대상이 아닙니다.", NamedTextColor.YELLOW));
                return true;
            }
            player.sendMessage(Component.text("MonsterBehaviorService debug", NamedTextColor.AQUA));
            debugLines.forEach(line -> player.sendMessage(Component.text(line, NamedTextColor.GRAY)));
            return true;
        }
        if (args.length < 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
            player.sendMessage(Component.text("Usage: /rpgmob debug [on|off]", NamedTextColor.YELLOW));
            return true;
        }
        mythicCustomMobService.setDebug(player, args[1].equalsIgnoreCase("on"));
        return true;
    }

    private boolean handleScaling(Player player, String label, String[] args) {
        if (!hasPermission(player, "hyunseorpg.mob.scaling")) {
            return true;
        }

        if (args.length < 2 || isHelp(args[1])) {
            sendScalingHelp(player, label);
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("get")) {
            sendScalingInfo(player);
            return true;
        }
        if (action.equals("sample")) {
            MobLevelScalingService.LevelSample sample = mobLevelScalingService.samplePlayerLevels(Bukkit.getOnlinePlayers());
            player.sendMessage(Component.text("Player level sample: count=" + sample.playerCount()
                    + ", minLevel=" + sample.minLevel()
                    + ", averageLevel=" + sample.averageLevel(), NamedTextColor.AQUA));
            return true;
        }
        if (action.equals("set")) {
            if (args.length < 4) {
                player.sendMessage(Component.text("Usage: /" + label + " scaling set <key> <value>", NamedTextColor.YELLOW));
                return true;
            }
            return setScalingValue(player, args[2], args[3]);
        }

        player.sendMessage(Component.text("Unknown scaling action: " + args[1], NamedTextColor.RED));
        sendScalingHelp(player, label);
        return true;
    }

    private boolean setScalingValue(Player player, String key, String rawValue) {
        try {
            double value = Double.parseDouble(rawValue);
            Optional<String> error = mobLevelScalingService.setScalingValue(key, value);
            if (error.isPresent()) {
                player.sendMessage(Component.text(error.get(), NamedTextColor.RED));
                player.sendMessage(Component.text("Keys: " + mobLevelScalingService.getScalingKeys(), NamedTextColor.GRAY));
                return true;
            }

            player.sendMessage(Component.text("Saved mob scaling setting: " + key + "=" + value, NamedTextColor.GREEN));
            return true;
        } catch (NumberFormatException exception) {
            player.sendMessage(Component.text("Value must be a number: " + rawValue, NamedTextColor.RED));
            return true;
        }
    }

    private Optional<LivingEntity> findLookedAtMob(Player player) {
        World world = player.getWorld();
        RayTraceResult result = world.rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                20.0D,
                0.5D,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );

        if (result == null || result.getHitEntity() == null) {
            return Optional.empty();
        }

        Entity hitEntity = result.getHitEntity();
        if (hitEntity instanceof LivingEntity livingEntity) {
            return Optional.of(livingEntity);
        }
        return Optional.empty();
    }

    private Optional<EntityType> parseEntityType(String input) {
        String normalized = input.toUpperCase(Locale.ROOT).replace("-", "_");
        return Arrays.stream(EntityType.values())
                .filter(type -> type.name().equals(normalized))
                .findFirst();
    }

    private Integer parseOptionalLevel(Player player, String[] args, int index) {
        if (args.length <= index) {
            return null;
        }

        try {
            int level = Integer.parseInt(args[index]);
            if (level < 1) {
                player.sendMessage(Component.text("level must be at least 1.", NamedTextColor.RED));
                return null;
            }
            return Math.min(level, 1000);
        } catch (NumberFormatException exception) {
            player.sendMessage(Component.text("level must be a number: " + args[index], NamedTextColor.RED));
            return null;
        }
    }

    private boolean hasPermission(Player player, String permission) {
        if (player.hasPermission("hyunseorpg.admin") || player.hasPermission(permission)) {
            return true;
        }
        player.sendMessage(Component.text("Missing permission: " + permission, NamedTextColor.RED));
        return false;
    }

    private boolean hasAnyMobPermission(Player player) {
        return player.hasPermission("hyunseorpg.admin")
                || player.hasPermission("hyunseorpg.mob.info")
                || player.hasPermission("hyunseorpg.mob.spawn")
                || player.hasPermission("hyunseorpg.mob.scaling");
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(Component.text("HyunseoRPG RPG mob commands", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/" + label + " info", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " spawn [mobType] [rpg|elite|boss] [level]", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " spawncustom [mobId] [level]", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " clear", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " debug <on|off>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " scaling <get|set|sample>", NamedTextColor.YELLOW));
    }

    private void sendScalingHelp(Player player, String label) {
        player.sendMessage(Component.text("RPG mob scaling commands", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/" + label + " scaling get", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " scaling sample", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/" + label + " scaling set <key> <value>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Keys: " + mobLevelScalingService.getScalingKeys(), NamedTextColor.GRAY));
    }

    private void sendScalingInfo(Player player) {
        player.sendMessage(Component.text("RPG mob level scaling", NamedTextColor.GOLD));
        for (Map.Entry<String, Double> entry : mobLevelScalingService.getCurrentSettings().entrySet()) {
            player.sendMessage(Component.text(entry.getKey() + "=" + entry.getValue(), NamedTextColor.GRAY));
        }
    }

    private boolean isHelp(String value) {
        return value.equals("?") || value.equalsIgnoreCase("help");
    }

    private int getDefaultRadius() {
        return Math.max(1, configService.getMobsInt("admin.default-radius", 8));
    }

    private List<String> livingEntityTypeNames() {
        return Arrays.stream(EntityType.values())
                .filter(EntityType::isAlive)
                .filter(mobService::isManageableRpgCandidate)
                .map(EntityType::name)
                .toList();
    }

    private List<String> filter(List<String> candidates, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .toList();
    }
}
