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

        String recipient = spec.string("recipient", "participants").trim().toLowerCase(java.util.Locale.ROOT);
        java.util.Collection<java.util.UUID> recipients;
        if (recipient.equals("looter")) {
            if (context.runtime().looter() == null) {
                throw new IllegalStateException("reward_drop recipient looter is unavailable");
            }
            recipients = java.util.List.of(context.runtime().looter());
        } else if (recipient.equals("entry_actor")) {
            if (context.runtime().entryActor() == null) {
                throw new IllegalStateException("reward_drop recipient entry_actor is unavailable");
            }
            recipients = java.util.List.of(context.runtime().entryActor());
        } else if (recipient.equals("participants")) {
            recipients = context.runtime().participants();
        } else {
            throw new IllegalArgumentException("unsupported reward_drop recipient: " + recipient);
        }

        boolean pyramid = "desert_pyramid".equals(context.record().structureType());
        if (pyramid && "RECOVERY_REQUIRED".equals(context.record().activationMetadata()
                .getOrDefault("pyramid-failure-state", ""))) {
            context.plugin().getLogger().warning("Pyramid reward refused: recovery required for "
                    + context.record().structureId());
            return;
        }
        if (pyramid) {
            String rewardState = context.record().activationMetadata().getOrDefault("pyramid-reward-state", "pending");
            if ("finalized".equalsIgnoreCase(rewardState) || "delivered".equalsIgnoreCase(rewardState)) return;
        }
        int onlineParticipants = 0;
        int successfulDeliveries = 0;
        for (var playerId : recipients) {
            if ("desert_pyramid".equals(context.record().structureType())
                    && playerId.toString().equals(context.record().activationMetadata()
                    .get("pyramid-reward-delivered-to"))) {
                onlineParticipants++;
                successfulDeliveries++;
                continue;
            }
            var player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) continue;
            onlineParticipants++;
            String token = context.record().structureId() + ":" + playerId + ":" + rewardId + ":" + amount;
            boolean delivered = pyramid
                    ? context.ports().rewards().enqueueDurable(player, rewardId, amount, fallback, spec.options(), token)
                    : context.ports().rewards().grant(player, rewardId, amount, fallback, spec.options());
            if (delivered) {
                successfulDeliveries++;
                if (pyramid) context.runtime().sequence().setFlag("pyramid.reward.delivered." + playerId);
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
