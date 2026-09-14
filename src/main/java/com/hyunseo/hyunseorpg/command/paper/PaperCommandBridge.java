package com.hyunseo.hyunseorpg.command.paper;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Adapts the existing Bukkit executor contracts to Paper's lifecycle command registrar. */
@SuppressWarnings("UnstableApiUsage")
public final class PaperCommandBridge implements BasicCommand {
    private final String name;
    private final Command command;
    private final CommandExecutor executor;
    private final TabCompleter completer;

    public PaperCommandBridge(String name, CommandExecutor executor, TabCompleter completer) {
        this.name = name;
        this.executor = executor;
        this.completer = completer;
        this.command = new Command(name) {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                return PaperCommandBridge.this.executor.onCommand(sender, this, label, args);
            }
        };
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, @NotNull String[] args) {
        executor.onCommand(source.getSender(), command, name, args);
    }

    @Override
    public @NotNull List<String> suggest(@NotNull CommandSourceStack source, @NotNull String[] args) {
        if (completer == null) return List.of();
        List<String> suggestions = completer.onTabComplete(source.getSender(), command, name, args);
        return suggestions == null ? List.of() : suggestions;
    }
}
