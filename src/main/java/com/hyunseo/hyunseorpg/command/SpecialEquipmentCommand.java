package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.special.SpecialEquipmentData;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentService;
import com.hyunseo.hyunseorpg.special.SpecialEquipmentMenuService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

/** Admin and test entry point for the YAML-defined special equipment system. */
public final class SpecialEquipmentCommand implements CommandExecutor, TabCompleter {
    private final SpecialEquipmentService service;
    private final SpecialEquipmentMenuService menu;

    public SpecialEquipmentCommand(SpecialEquipmentService service, SpecialEquipmentMenuService menu) {
        this.service = service;
        this.menu = menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/specialequipment <menu|list|info|give|unlock|soul|craft|debug>", NamedTextColor.YELLOW));
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("menu")) {
            if (sender instanceof Player player) menu.open(player);
            else sender.sendMessage(Component.text("This action requires a player.", NamedTextColor.RED));
            return true;
        }
        if (action.equals("list")) {
            sender.sendMessage(Component.text("Special equipment: " + service.registry().getAll().stream().map(SpecialEquipmentData::id).toList(), NamedTextColor.AQUA));
            return true;
        }
        if (action.equals("info")) return info(sender, args);
        if (action.equals("give")) return give(sender, args);
        if (action.equals("unlock")) return unlock(sender, args);
        if (action.equals("craft")) return craft(sender, args);
        if (action.equals("soul")) return soul(sender, args);
        if (action.equals("debug")) return debug(sender, args);
        sender.sendMessage(Component.text("Unknown special equipment action.", NamedTextColor.RED));
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("/specialequipment info <equipmentId>", NamedTextColor.YELLOW));
            return true;
        }
        SpecialEquipmentData data = service.registry().get(args[1]).orElse(null);
        if (data == null) {
            sender.sendMessage(Component.text("Unknown equipment.", NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text(data.id() + " / " + data.displayName() + " / " + data.element(), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Recipe: " + data.recipeInputs(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Unlock: level " + data.requiredRpgLevel() + ", worlds " + data.requiredWorlds()
                + ", mobs " + data.requiredMobKills(), NamedTextColor.GRAY));
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hyunseorpg.special.admin")) return denied(sender);
        if (args.length < 3) {
            sender.sendMessage(Component.text("/specialequipment give <player> <equipmentId> [amount]", NamedTextColor.YELLOW));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player is offline.", NamedTextColor.RED));
            return true;
        }
        int amount = parseAmount(args, 3);
        ItemStack item = service.create(args[2], amount);
        if (item == null) {
            sender.sendMessage(Component.text("Unknown equipment.", NamedTextColor.RED));
            return true;
        }
        target.getInventory().addItem(item).values().forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
        sender.sendMessage(Component.text("Special equipment given.", NamedTextColor.GREEN));
        return true;
    }

    private boolean unlock(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hyunseorpg.special.admin")) return denied(sender);
        if (args.length < 2) {
            sender.sendMessage(Component.text("/specialequipment unlock <player> [lock]", NamedTextColor.YELLOW));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player is offline.", NamedTextColor.RED));
            return true;
        }
        boolean enabled = args.length < 3 || !args[2].equalsIgnoreCase("lock");
        service.setUnlockBypass(target, enabled);
        sender.sendMessage(Component.text("Unlock bypass " + (enabled ? "enabled" : "disabled") + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean craft(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This action requires a player.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("/specialequipment craft <equipmentId>", NamedTextColor.YELLOW));
            return true;
        }
        service.craft(player, args[1]);
        return true;
    }

    private boolean soul(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hyunseorpg.special.admin")) return denied(sender);
        if (args.length < 4) {
            sender.sendMessage(Component.text("/specialequipment soul <get|add> <player> <mobId> [amount]", NamedTextColor.YELLOW));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(Component.text("Player is offline.", NamedTextColor.RED));
            return true;
        }
        ItemStack item = target.getInventory().getItemInMainHand();
        if (!service.isSpecial(item)) {
            sender.sendMessage(Component.text("Target is not holding special equipment.", NamedTextColor.RED));
            return true;
        }
        long amount = args.length >= 5 ? parseLong(args[4]) : 1L;
        if (args[1].equalsIgnoreCase("add")) service.addSoul(item, args[3], amount);
        sender.sendMessage(Component.text("Soul progress: " + service.getSoul(item, args[3]), NamedTextColor.AQUA));
        return true;
    }

    private boolean debug(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This action requires a player.", NamedTextColor.RED));
            return true;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        String id = service.getSpecialId(held);
        player.sendMessage(Component.text("Special id: " + (id.isBlank() ? "none" : id), NamedTextColor.AQUA));
        if (!id.isBlank()) player.sendMessage(Component.text("Missing: " + service.missingRequirements(player, id), NamedTextColor.GRAY));
        return true;
    }

    private boolean denied(CommandSender sender) {
        sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
        return true;
    }

    private int parseAmount(String[] args, int index) {
        if (args.length <= index) return 1;
        try { return Math.max(1, Math.min(2304, Integer.parseInt(args[index]))); }
        catch (NumberFormatException ignored) { return 1; }
    }

    private long parseLong(String raw) {
        try { return Math.max(1L, Long.parseLong(raw)); }
        catch (NumberFormatException ignored) { return 1L; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return prefix(List.of("menu", "list", "info", "give", "unlock", "soul", "craft", "debug"), args[0]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("craft"))) {
            return prefix(service.registry().getAll().stream().map(SpecialEquipmentData::id).toList(), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("unlock"))) return onlinePlayers(args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("soul")) return prefix(List.of("get", "add"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("soul")) return onlinePlayers(args[2]);
        return List.of();
    }

    private List<String> prefix(List<String> values, String input) {
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }

    private List<String> onlinePlayers(String input) {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
        return prefix(names, input);
    }
}
