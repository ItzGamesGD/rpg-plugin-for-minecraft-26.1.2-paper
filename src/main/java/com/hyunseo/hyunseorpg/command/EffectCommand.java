package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.alchemy.EffectListGuiService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/** Direct command entry point for the read-only active effect GUI. */
public final class EffectCommand implements CommandExecutor, TabCompleter {
    private final EffectListGuiService gui;

    public EffectCommand(EffectListGuiService gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            gui.open(player);
            return true;
        }
        player.sendMessage("사용법: /effectlist");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length <= 1 ? List.of("list") : List.of();
    }
}
