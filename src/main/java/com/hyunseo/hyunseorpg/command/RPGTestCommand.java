package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.boss.BossSessionManager;
import com.hyunseo.hyunseorpg.boss.BossType;
import com.hyunseo.hyunseorpg.core.config.RPGReloadService;
import com.hyunseo.hyunseorpg.economy.CoinService;
import com.hyunseo.hyunseorpg.enhancement.EquipmentEnhancementService;
import com.hyunseo.hyunseorpg.equipment.EquipmentData;
import com.hyunseo.hyunseorpg.equipment.EquipmentDefinition;
import com.hyunseo.hyunseorpg.equipment.EquipmentMetadataService;
import com.hyunseo.hyunseorpg.equipment.EquipmentRegistry;
import com.hyunseo.hyunseorpg.item.RPGItemRegistry;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.crafting.SoulboundItemService;
import com.hyunseo.hyunseorpg.prototype.thousandeyes.ThousandEyesController;
import com.hyunseo.hyunseorpg.rpgtest.basic.BasicWeaponPattern;
import com.hyunseo.hyunseorpg.rpgtest.gateway.GatewayPayloadType;
import com.hyunseo.hyunseorpg.rpgtest.gateway.GatewayPrototypeService;
import com.hyunseo.hyunseorpg.weapon.WeaponItemService;
import com.hyunseo.hyunseorpg.weapon.WeaponType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class RPGTestCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "hyunseorpg.admin.test";
    private final CoinService coins;
    private final RPGItemRegistry itemRegistry;
    private final RPGItemService items;
    private final SoulboundItemService soulbound;
    private final WeaponItemService weapons;
    private final EquipmentEnhancementService enhancement;
    private final EquipmentMetadataService equipmentMetadata;
    private final EquipmentRegistry equipmentRegistry;
    private final BossSessionManager bosses;
    private final RPGReloadService reload;
    private final GatewayPrototypeService gatewayPrototype;
    private final ThousandEyesController thousandEyes;

    public RPGTestCommand(CoinService coins, RPGItemRegistry itemRegistry, RPGItemService items,
                          SoulboundItemService soulbound, WeaponItemService weapons,
                          EquipmentEnhancementService enhancement,
                          BossSessionManager bosses, RPGReloadService reload,
                          EquipmentMetadataService equipmentMetadata, EquipmentRegistry equipmentRegistry,
                          GatewayPrototypeService gatewayPrototype, ThousandEyesController thousandEyes) {
        this.coins = coins;
        this.itemRegistry = itemRegistry;
        this.items = items;
        this.soulbound = soulbound;
        this.weapons = weapons;
        this.enhancement = enhancement;
        this.equipmentMetadata = equipmentMetadata;
        this.equipmentRegistry = equipmentRegistry;
        this.bosses = bosses;
        this.reload = reload;
        this.gatewayPrototype = gatewayPrototype;
        this.thousandEyes = thousandEyes;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) { sender.sendMessage("관리자 테스트 권한이 필요합니다."); return true; }
        if (args.length == 0) { usage(sender, label); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give", "item" -> give(sender, args, label);
            case "coins" -> adjustCoins(sender, args, false, label);
            case "setcoins" -> adjustCoins(sender, args, true, label);
            case "hand", "inspect" -> inspect(sender, args);
            case "maxhand", "maxgrowth" -> maxHand(sender, args);
            case "boss" -> boss(sender, args, label);
            case "gateway" -> gateway(sender, args, label);
            case "basic-swarm" -> basicSwarm(sender, args, label);
            case "orbital-core" -> orbitalCore(sender, args);
            case "thousand-eyes" -> thousandEyes(sender, args, label);
            case "reload" -> sender.sendMessage(reload.reload(args.length > 1 ? args[1] : "all").message());
            default -> usage(sender, label);
        }
        return true;
    }

    private void orbitalCore(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("This prototype must be run by a player."); return; }
        if (args.length >= 2 && args[1].equalsIgnoreCase("cancel")) {
            gatewayPrototype.cleanup(player.getUniqueId()); sender.sendMessage("Orbital core cleaned up."); return;
        }
        int rings = 4;
        try { if (args.length >= 2) rings = Integer.parseInt(args[1]); }
        catch (NumberFormatException exception) { sender.sendMessage("/rpgtest orbital-core [1-4|cancel]"); return; }
        sender.sendMessage(gatewayPrototype.orbitalCore(player, rings));
    }

    private void gateway(CommandSender sender, String[] args, String label) {
        if (!(sender instanceof Player player)) { sender.sendMessage("This prototype must be run by a player."); return; }
        if (args.length < 2) { sender.sendMessage("/" + label + " gateway <boss|cycle|payload|reflection|placement|pairing|weapon-ai|cancel> [type] [debug]"); return; }
        boolean debug = java.util.Arrays.stream(args).anyMatch(value -> value.equalsIgnoreCase("debug"));
        String message = switch (args[1].toLowerCase(Locale.ROOT)) {
            case "boss", "battle" -> gatewayPrototype.bossBattle(player, debug);
            case "cycle" -> gatewayPrototype.randomCycle(player, debug);
            case "reflection" -> gatewayPrototype.reflection(player);
            case "placement" -> gatewayPrototype.place(player, debug);
            case "pairing" -> gatewayPrototype.pairing(player);
            case "weapon-ai" -> args.length >= 3
                    ? meleePattern(args[2]).map(pattern -> gatewayPrototype.weaponAiComparison(player, pattern)).orElse("weapon-ai supports mace, spear, axe, or hoe melee.")
                    : "Usage: /" + label + " gateway weapon-ai <mace|spear|axe|hoe>";
            case "cancel" -> { gatewayPrototype.cleanup(player.getUniqueId()); yield "Gateway/basic prototype cleaned up."; }
            case "payload" -> args.length >= 3
                    ? GatewayPayloadType.fromInput(args[2]).map(type -> gatewayPrototype.payload(player, type, debug)).orElse("Unknown payload.")
                    : "Payload type is required.";
            default -> "Unknown gateway test mode.";
        };
        sender.sendMessage(message);
    }

    private java.util.Optional<BasicWeaponPattern> meleePattern(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "mace" -> java.util.Optional.of(BasicWeaponPattern.MACE_MELEE);
            case "spear" -> java.util.Optional.of(BasicWeaponPattern.SPEAR_MELEE);
            case "axe" -> java.util.Optional.of(BasicWeaponPattern.AXE_MELEE);
            case "hoe" -> java.util.Optional.of(BasicWeaponPattern.HOE_MELEE);
            default -> java.util.Optional.empty();
        };
    }

    private void basicSwarm(CommandSender sender, String[] args, String label) {
        if (!(sender instanceof Player player)) { sender.sendMessage("This prototype must be run by a player."); return; }
        if (args.length < 2 || args[1].equalsIgnoreCase("random")) { sender.sendMessage(gatewayPrototype.basicRandom(player)); return; }
        if (!args[1].equalsIgnoreCase("pattern") || args.length < 3) {
            sender.sendMessage("/" + label + " basic-swarm <random|pattern <name> [count]>"); return;
        }
        BasicWeaponPattern pattern = BasicWeaponPattern.fromInput(args[2]).orElse(null);
        if (pattern == null) { sender.sendMessage("Unknown implemented pattern."); return; }
        int count = 1;
        try { if (args.length >= 4) count = Integer.parseInt(args[3]); }
        catch (NumberFormatException exception) { sender.sendMessage("count must be a number."); return; }
        sender.sendMessage(gatewayPrototype.basicPattern(player, pattern, count));
    }

    private void thousandEyes(CommandSender sender, String[] args, String label) {
        if (!(sender instanceof Player player)) { sender.sendMessage("플레이어만 실행할 수 있습니다."); return; }
        if (args.length < 2) { sender.sendMessage("/" + label + " thousand-eyes <spawn|remove|central-laser|gateway-burst|scatter-lasers|path-dash> [seed]"); return; }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("spawn")) {
            long seed = 1000L;
            if (args.length > 2) try { seed = Long.parseLong(args[2]); } catch (NumberFormatException ignored) { sender.sendMessage("seed는 정수여야 합니다."); return; }
            sender.sendMessage("천 개의 눈 prototype spawn: " + thousandEyes.spawn(player, seed) + " (seed=" + seed + ")"); return;
        }
        if (action.equals("remove")) { thousandEyes.remove(); sender.sendMessage("천 개의 눈 prototype 제거 완료"); return; }
        ThousandEyesController.Skill skill = switch (action) {
            case "central-laser" -> ThousandEyesController.Skill.CENTRAL_LASER;
            case "gateway-burst" -> ThousandEyesController.Skill.GATEWAY_BURST;
            case "scatter-lasers" -> ThousandEyesController.Skill.SCATTER_LASERS;
            case "path-dash" -> ThousandEyesController.Skill.PATH_DASH;
            default -> null;
        };
        sender.sendMessage(skill == null ? "알 수 없는 천 개의 눈 동작입니다." : "스킬 시작: " + thousandEyes.start(skill));
    }

    private void give(CommandSender sender, String[] args, String label) {
        if (args.length < 3) { sender.sendMessage("/" + label + " give <player> <itemId> [amount]"); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage("플레이어를 찾을 수 없습니다."); return; }
        int amount;
        try { amount = Math.max(1, Math.min(2304, args.length > 3 ? Integer.parseInt(args[3]) : 1)); }
        catch (NumberFormatException exception) { sender.sendMessage("수량은 숫자여야 합니다."); return; }
        if (itemRegistry.get(args[2]).filter(data -> data.legacyProfessionItem()).isPresent()) {
            sender.sendMessage("직업 레거시 아이템은 호환성 보존용이라 새로 지급하지 않습니다.");
            return;
        }
        ItemStack prototype = createItem(args[2]);
        if (prototype == null) { sender.sendMessage("등록되지 않은 아이템입니다: " + args[2]); return; }
        for (int remaining = amount; remaining > 0;) {
            int stackAmount = Math.min(remaining, prototype.getMaxStackSize());
            ItemStack stack = prototype.clone();
            stack.setAmount(stackAmount);
            if (soulbound.requiresBinding(stack)) soulbound.bind(stack, target.getUniqueId());
            Map<Integer, ItemStack> leftovers = target.getInventory().addItem(stack);
            leftovers.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
            remaining -= stackAmount;
        }
        sender.sendMessage(target.getName() + "에게 " + args[2] + " x" + amount + " 지급 완료");
    }

    private void adjustCoins(CommandSender sender, String[] args, boolean set, String label) {
        if (args.length < 3) { sender.sendMessage("/" + label + " " + (set ? "setcoins" : "coins") + " <player> <amount>"); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        try {
            long amount = Long.parseLong(args[2]);
            if (target == null || amount < 0) throw new NumberFormatException();
            if (set) coins.setCoins(target, amount); else coins.addCoins(target, amount);
            sender.sendMessage("코인 처리 완료: " + coins.getCoins(target));
        } catch (NumberFormatException exception) { sender.sendMessage("플레이어 또는 수량이 올바르지 않습니다."); }
    }

    private void inspect(CommandSender sender, String[] args) {
        Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player player ? player : null;
        if (target == null) { sender.sendMessage("플레이어를 찾을 수 없습니다."); return; }
        ItemStack item = target.getInventory().getItemInMainHand();
        equipmentMetadata.read(item).ifPresent(data -> {
            EquipmentDefinition definition = equipmentRegistry.find(item).orElse(null);
            sender.sendMessage(formatEquipmentData(data));
            if (definition != null) sender.sendMessage("equipment-definition=" + definition);
        });
        if (item.getType().isAir()) { sender.sendMessage("주손 장비가 없습니다."); return; }
        sender.sendMessage("강화=" + enhancement.getLevel(item));
    }

    private String formatEquipmentData(EquipmentData data) {
        return "equipment-data=id=" + data.itemId()
                + ",type=" + data.equipmentType()
                + ",upgrade=" + data.upgradeLevel()
                + ",enchants=" + data.enchantData()
                + ",kills=" + data.killCount()
                + ",data-version=" + data.dataVersion();
    }

    private void maxHand(CommandSender sender, String[] args) {
        Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player player ? player : null;
        if (target == null) { sender.sendMessage("Player not found."); return; }
        ItemStack item = target.getInventory().getItemInMainHand();
        if (item.getType().isAir()) { sender.sendMessage("Main hand is empty."); return; }

        int finalEnhancement = enhancement.forceEnhancementLevel(item, enhancement.getMaximumLevel(item));
        target.getInventory().setItemInMainHand(item);
        sender.sendMessage("main-hand max growth applied: player=" + target.getName()
                + ", enhancement=+" + finalEnhancement);
    }

    private void boss(CommandSender sender, String[] args, String label) {
        if (args.length < 3) { sender.sendMessage("/" + label + " boss <start|end|complete|status> <wither|dragon> [player]"); return; }
        BossType type = BossType.fromInput(args[2]).orElse(null);
        if (type == null) { sender.sendMessage("보스 종류는 wither 또는 dragon입니다."); return; }
        Player target = args.length > 3 ? Bukkit.getPlayerExact(args[3]) : sender instanceof Player player ? player : null;
        if (target == null) { sender.sendMessage("플레이어를 찾을 수 없습니다."); return; }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> sender.sendMessage("보스 세션 시작: " + bosses.startTest(target, type));
            case "end" -> sender.sendMessage("보스 세션 종료: " + bosses.endFor(target, type));
            case "complete" -> sender.sendMessage("보스 강제 완료: " + bosses.forceComplete(target, type));
            case "status" -> sender.sendMessage(type.configId() + ": " + bosses.status(type));
            default -> sender.sendMessage("/" + label + " boss <start|end|complete|status> <wither|dragon> [player]");
        }
    }

    private ItemStack createItem(String rawId) {
        ItemStack item = items.create(rawId, 1).orElse(null);
        if (item != null) return item;
        if (rawId.toLowerCase(Locale.ROOT).startsWith("basic_")) {
            return WeaponType.fromInput(rawId.substring(6)).map(weapons::create).orElse(null);
        }
        return null;
    }

    private void usage(CommandSender sender, String label) {
        sender.sendMessage("/" + label + " give <player> <itemId> [amount]");
        sender.sendMessage("/" + label + " coins|setcoins <player> <amount>");
        sender.sendMessage("/" + label + " maxhand [player]");
        sender.sendMessage("/" + label + " boss <start|end|complete|status> <wither|dragon> [player]");
        sender.sendMessage("/" + label + " gateway <boss|cycle|payload|reflection|placement|pairing|weapon-ai|cancel> [type] [debug]");
        sender.sendMessage("/" + label + " basic-swarm <random|pattern <name> [count]>");
        sender.sendMessage("/" + label + " orbital-core [1-4|cancel]");
        sender.sendMessage("/" + label + " thousand-eyes <spawn|remove|central-laser|gateway-burst|scatter-lasers|path-dash> [seed]");
        sender.sendMessage("/" + label + " reload [all|bosses|recipes|mobs]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("give", "coins", "setcoins", "hand", "inspect", "maxhand", "maxgrowth", "boss", "gateway", "basic-swarm", "orbital-core", "thousand-eyes", "reload"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("orbital-core")) return filter(List.of("1", "2", "3", "4", "cancel"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("gateway")) return filter(List.of("boss", "cycle", "payload", "reflection", "placement", "pairing", "weapon-ai", "cancel"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("gateway") && args[1].equalsIgnoreCase("payload")) return filter(java.util.Arrays.stream(GatewayPayloadType.values()).map(Enum::name).toList(), args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("gateway") && args[1].equalsIgnoreCase("weapon-ai")) return filter(List.of("mace", "spear", "axe", "hoe"), args[2]);
        if (args.length == 2 && args[0].equalsIgnoreCase("basic-swarm")) return filter(List.of("random", "pattern"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("basic-swarm") && args[1].equalsIgnoreCase("pattern")) return filter(java.util.Arrays.stream(BasicWeaponPattern.values()).map(Enum::name).toList(), args[2]);
        if (args.length == 2 && args[0].equalsIgnoreCase("thousand-eyes")) return filter(List.of("spawn", "remove", "central-laser", "gateway-burst", "scatter-lasers", "path-dash"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("boss")) return filter(List.of("start", "end", "complete", "status"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("boss")) return filter(List.of("wither", "dragon"), args[2]);
        if (args.length == 4 && args[0].equalsIgnoreCase("boss")) return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[3]);
        if (args.length == 2 && args[0].equalsIgnoreCase("reload")) return filter(List.of(reload.ids()), args[1]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("hand")
                || args[0].equalsIgnoreCase("inspect") || args[0].equalsIgnoreCase("maxhand")
                || args[0].equalsIgnoreCase("maxgrowth"))) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> ids = new ArrayList<>(itemRegistry.getAll().stream()
                    .filter(data -> !data.legacyProfessionItem())
                    .map(data -> data.itemId()).toList());
            for (WeaponType type : WeaponType.values()) ids.add("basic_" + type.id());
            return filter(ids, args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized)).toList();
    }
}
