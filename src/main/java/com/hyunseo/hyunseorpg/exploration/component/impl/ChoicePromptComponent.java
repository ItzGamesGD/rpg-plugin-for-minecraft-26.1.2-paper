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
import java.util.Set;
import java.util.UUID;

/** Presents a bounded, owner-checked exploration choice before any combat objectives exist. */
public final class ChoicePromptComponent implements ExplorationComponent {
    @Override public String type() { return "choice_prompt"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.ACTIVATE; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        UUID owner = context.runtime().participants().stream().findFirst()
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
        player.sendMessage(Component.text("약탈자들이 습격을 준비합니다!", NamedTextColor.RED));
        player.sendMessage(choiceButton(context, "tier1", "[1단계 정찰대]", "최대 4마리, heavy 없음", NamedTextColor.GREEN)
                .append(Component.space())
                .append(choiceButton(context, "tier2", "[2단계 습격대]", "최대 6마리, heavy 최대 1", NamedTextColor.GOLD))
                .append(Component.space())
                .append(choiceButton(context, "tier3", "[3단계 전쟁대]", "최대 8마리, heavy 최대 2", NamedTextColor.RED))
                .append(Component.space())
                .append(choiceButton(context, "flee", "[도망치기]", "전투를 시작하지 않습니다", NamedTextColor.GRAY)));
    }

    private Component choiceButton(ExplorationEventContext context, String choice, String label,
                                   String hover, NamedTextColor color) {
        if (!context.runtime().allowedChoices().contains(choice)) return Component.empty();
        String command = "/rpg exploration choose " + context.record().structureId() + " " + choice;
        return Component.text(label, color)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.YELLOW)));
    }
}
