package com.hyunseo.hyunseorpg.command.paper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PaperCommandCatalog {
    public static final List<PaperCommandSpec> ALL = List.of(
            c("rpg", "HyunseoRPG utility command"),
            c("effectlist", "Open the active status effect list"),
            c("rpgtest", "Operator-only testing utilities"),
            c("crafting", "Open or edit crafting", List.of()),
            c("weaponinfo", "Inspect weapon proficiency", List.of("weaponlevel")),
            c("rpgstat", "Admin stat command", List.of("statadmin")),
            c("rpgstatbalance", "Stat balance command", List.of("statbalance")),
            c("stats", "Open common stat menu", List.of("stat")),
            c("skillstats", "Open skill stat menu", List.of("skillstat")),
            c("weaponstats", "Open weapon stat menu", List.of("classstats", "weaponstat")),
            c("rpglevel", "Admin level command", List.of("leveladmin")),
            c("rpgcooldown", "Admin cooldown command", List.of("cooldownadmin")),
            c("rpgmob", "RPG mob management command"),
            c("rpgquest", "RPG quest command", List.of("quest")),
            c("shop", "Open shop"),
            c("shopadmin", "Shop administration"),
            c("specialequipment", "Special equipment administration")
    );
    public static final Map<String, PaperCommandSpec> BY_NAME = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(PaperCommandSpec::name, Function.identity()));

    private PaperCommandCatalog() { }
    private static PaperCommandSpec c(String name, String description) { return c(name, description, List.of()); }
    private static PaperCommandSpec c(String name, String description, List<String> aliases) {
        return new PaperCommandSpec(name, description, aliases);
    }
}
