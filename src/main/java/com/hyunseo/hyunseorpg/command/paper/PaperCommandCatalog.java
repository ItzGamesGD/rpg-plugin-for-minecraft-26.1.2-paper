package com.hyunseo.hyunseorpg.command.paper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PaperCommandCatalog {
    public static final List<PaperCommandSpec> ALL = List.of(
            c("rpg", "HyunseoRPG utility command"),
            c("rpgtest", "Operator-only testing utilities"),
            c("weaponinfo", "Inspect weapon proficiency", List.of("weaponlevel")),
            c("rpgstat", "Admin stat command", List.of("statadmin")),
            c("rpgstatbalance", "Stat balance command", List.of("statbalance")),
            c("rpglevel", "Admin level command", List.of("leveladmin")),
            c("rpgcooldown", "Admin cooldown command", List.of("cooldownadmin")),
            c("rpgmob", "RPG mob management command"),
            c("rpgquest", "RPG quest command", List.of("quest")),
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
