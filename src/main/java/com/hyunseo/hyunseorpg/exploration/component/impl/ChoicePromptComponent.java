package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Config-driven, owner-checked exploration choice; Outpost defaults remain backward compatible. */
public final class ChoicePromptComponent implements ExplorationComponent {
    @Override public String type() { return "choice_prompt"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.ACTIVATE; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        UUID owner = context.runtime().entryActor() != null ? context.runtime().entryActor()
                : context.runtime().looter() != null ? context.runtime().looter()
                : context.runtime().participants().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("choice_prompt requires an activating player"));
        Set<String> choices = new LinkedHashSet<>(spec.stringList("choices"));
        if (choices.isEmpty()) choices.addAll(Set.of("tier1", "tier2", "tier3", "flee"));
        long timeout = Math.max(20L, spec.integer("timeout-ticks", 400));
        String fallback = spec.string("default-choice", "flee");
        if (!context.runtime().beginChoice(owner, spec.string("prompt-id", "exploration-choice"), choices,
                fallback, context.currentTick() + timeout)) {
            throw new IllegalStateException("choice_prompt could not establish a pending choice");
        }
        Player player = Bukkit.getPlayer(owner);
        if (player == null || !player.isOnline()) return;
        player.sendMessage(Component.text(defaultPromptText(context.record().structureType(), spec), NamedTextColor.RED));
        Component buttons = Component.empty();
        boolean first = true;
        for (String choice : context.runtime().allowedChoices()) {
            if (!first) buttons = buttons.append(Component.space());
            first = false;
            buttons = buttons.append(choiceButton(context, choice, label(spec, choice), hover(spec, choice)));
        }
        player.sendMessage(buttons);
    }

    static String defaultPromptText(String structureType, ExplorationComponentSpec spec) {
        String configured = spec.string("prompt-text", "").trim();
        if (!configured.isBlank()) return configured;
        if ("desert_pyramid".equalsIgnoreCase(structureType)) {
            return "피라미드의 수수께끼가 길을 막습니다.";
        }
        return "약탈자들이 습격을 준비합니다!";
    }

    private Component choiceButton(ExplorationEventContext context, String choice, String label, String hover) {
        String command = "/rpg exploration choose " + context.record().structureId() + " " + choice;
        return Component.text(label, NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.YELLOW)));
    }

    private String label(ExplorationComponentSpec spec, String choice) {
        Object labels = spec.options().get("choice-labels");
        if (labels instanceof Map<?, ?> map && map.get(choice) != null) return String.valueOf(map.get(choice));
        return switch (choice) {
            case "tier1" -> "[1단계 정찰대]";
            case "tier2" -> "[2단계 습격대]";
            case "tier3" -> "[3단계 전쟁대]";
            case "flee" -> "[도망치기]";
            default -> "[" + choice + "]";
        };
    }

    private String hover(ExplorationComponentSpec spec, String choice) {
        Object hovers = spec.options().get("choice-hover");
        if (hovers instanceof Map<?, ?> map && map.get(choice) != null) return String.valueOf(map.get(choice));
        return "선택";
    }
}
