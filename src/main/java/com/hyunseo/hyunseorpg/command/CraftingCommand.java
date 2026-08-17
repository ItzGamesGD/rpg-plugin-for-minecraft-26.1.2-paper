package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.crafting.CraftingGuiService;
import com.hyunseo.hyunseorpg.crafting.CraftingLayoutRegistry;
import com.hyunseo.hyunseorpg.crafting.CraftingRecipeData;
import com.hyunseo.hyunseorpg.crafting.CraftingRecipeRegistry;
import com.hyunseo.hyunseorpg.crafting.CraftingTransactionService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/** Player entry point plus bounded administrator diagnostics for canonical crafting data. */
public final class CraftingCommand implements CommandExecutor, TabCompleter {
    private final CraftingGuiService gui;
    private final CraftingRecipeRegistry recipes;
    private final CraftingLayoutRegistry layouts;
    private final CraftingTransactionService transactions;

    public CraftingCommand(CraftingGuiService gui, CraftingRecipeRegistry recipes,
                           CraftingLayoutRegistry layouts, CraftingTransactionService transactions) {
        this.gui = gui;
        this.recipes = recipes;
        this.layouts = layouts;
        this.transactions = transactions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) gui.openMain(player);
            return true;
        }
        if (!sender.hasPermission("hyunseorpg.crafting.admin")) return true;
        if (args.length == 2 && args[0].equalsIgnoreCase("edit") && sender instanceof Player player) {
            if (!gui.openAdmin(player, args[1])) {
                sender.sendMessage("Usage: /crafting edit <category>");
            }
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("inspect")) {
            CraftingRecipeData recipe = recipes.get(args[1]).orElse(null);
            if (recipe == null) sender.sendMessage("Unknown crafting recipe.");
            else sender.sendMessage(recipe.id() + " -> " + recipe.outputId() + " x" + recipe.outputAmount()
                    + ", category=" + recipe.categoryId() + ", ingredients=" + recipe.ingredients());
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("trace")) {
            CraftingRecipeRegistry.RecipeTrace trace = recipes.trace(args[1]);
            if (!trace.registered()) {
                sender.sendMessage("registry=NOT_REGISTERED id=" + trace.id());
                return true;
            }
            sender.sendMessage("registry=REGISTERED id=" + trace.id());
            sender.sendMessage("category=" + trace.categoryId() + ", output="
                    + trace.outputId() + " x" + trace.outputAmount());
            sender.sendMessage("inputs=" + trace.ingredients());
            layouts.categoryIds().stream()
                    .map(category -> java.util.Map.entry(category, layouts.positions(category).get(trace.id())))
                    .filter(entry -> entry.getValue() != null)
                    .forEach(entry -> sender.sendMessage("layout=" + entry.getKey() + ", slot=" + entry.getValue()));
            sender.sendMessage("gui-renderer=CraftingRecipeRenderer; click-resolution=layout-first/PDC-fallback");
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("max")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) sender.sendMessage("Player is not online.");
            else {
                CraftingTransactionService.Capacity capacity = transactions.capacity(target, args[2]);
                sender.sendMessage("materials=" + capacity.materialMaximum() + ", capacity="
                        + capacity.capacityMaximum() + ", maximum=" + capacity.maximum());
            }
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("layout")) {
            if (!layouts.categoryIds().contains(args[1].trim().toLowerCase(java.util.Locale.ROOT))) {
                sender.sendMessage("Unknown crafting category.");
            } else sender.sendMessage("layout " + args[1] + ": " + layouts.positions(args[1]));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("validate")) {
            List<String> unplaced = layouts.unplacedEnabledRecipes(recipes);
            sender.sendMessage(unplaced.isEmpty() ? "Crafting layout is valid." : "Unplaced active recipes: " + unplaced);
            return true;
        }
        sender.sendMessage("Usage: /crafting [edit|inspect|trace|max|layout|validate]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("edit", "inspect", "trace", "max", "layout", "validate");
        if (args.length == 2 && args[0].equalsIgnoreCase("edit")) {
            return layouts.categoryIds().stream().sorted().toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("layout")) {
            return layouts.categoryIds().stream().sorted().toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("inspect")) return recipes.getAll().stream().map(CraftingRecipeData::id).toList();
        if (args.length == 2 && args[0].equalsIgnoreCase("trace")) return recipes.getAll().stream().map(CraftingRecipeData::id).toList();
        if (args.length == 2 && args[0].equalsIgnoreCase("max")) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        if (args.length == 3 && args[0].equalsIgnoreCase("max")) return recipes.getAll().stream().map(CraftingRecipeData::id).toList();
        return List.of();
    }
}
