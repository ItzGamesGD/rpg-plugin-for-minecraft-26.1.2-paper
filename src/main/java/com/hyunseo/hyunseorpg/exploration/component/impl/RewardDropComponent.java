package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import org.bukkit.Bukkit;

/** Reward execution is fail-closed: a configured reward must not silently disappear. */
public final class RewardDropComponent implements ExplorationComponent {
    @Override public String type() { return "reward_drop"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.CLEAR; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        String rewardId = spec.string("reward-id", "");
        if (rewardId.isBlank()) throw new IllegalArgumentException("reward_drop requires reward-id");
        int amount = Math.max(1, spec.integer("amount", 1));
        var fallback = ComponentLocations.relative(context, spec);

        int onlineParticipants = 0;
        int successfulDeliveries = 0;
        for (var playerId : context.runtime().participants()) {
            var player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) continue;
            onlineParticipants++;
            if (context.ports().rewards().grant(player, rewardId, amount, fallback, spec.options())) {
                successfulDeliveries++;
            }
        }

        if (onlineParticipants == 0) {
            throw new IllegalStateException("reward_drop has no online participant; clear must be retried safely");
        }
        if (successfulDeliveries != onlineParticipants) {
            throw new IllegalStateException("reward_drop delivery failed for one or more participants");
        }
    }
}
