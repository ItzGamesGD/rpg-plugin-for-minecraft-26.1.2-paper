package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.ui.StatGuiService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SkillStatGuiCommand implements CommandExecutor {
    private final StatGuiService statGuiService;

    public SkillStatGuiCommand(StatGuiService statGuiService) {
        this.statGuiService = statGuiService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용할 수 있습니다.", NamedTextColor.RED));
            return true;
        }

        statGuiService.openSkillStatGui(player);
        return true;
    }
}
