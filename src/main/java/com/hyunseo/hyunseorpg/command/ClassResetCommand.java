package com.hyunseo.hyunseorpg.command;

import com.hyunseo.hyunseorpg.classsystem.ClassService;
import com.hyunseo.hyunseorpg.classsystem.RPGClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ClassResetCommand implements CommandExecutor {
    private final ClassService classService;

    public ClassResetCommand(ClassService classService) {
        this.classService = classService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("이 명령어는 플레이어만 사용할 수 있습니다.", NamedTextColor.RED));
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(Component.text("OP 권한을 가진 플레이어만 직업을 초기화할 수 있습니다.", NamedTextColor.RED));
            return true;
        }

        RPGClass previousClass = classService.resetClass(player);
        if (previousClass == null) {
            player.sendMessage(Component.text("초기화할 직업이 없습니다.", NamedTextColor.YELLOW));
            return true;
        }

        player.sendMessage(Component.text("직업을 초기화했습니다: " + previousClass.koreanName(), NamedTextColor.GREEN));
        player.sendMessage(Component.text("테스트용 초기화라 기존 직업 무기는 삭제하지 않았습니다.", NamedTextColor.GRAY));
        return true;
    }
}
