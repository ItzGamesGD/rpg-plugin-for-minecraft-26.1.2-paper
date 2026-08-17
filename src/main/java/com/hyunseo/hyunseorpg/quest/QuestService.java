package com.hyunseo.hyunseorpg.quest;

import com.hyunseo.hyunseorpg.economy.CoinService;
import com.hyunseo.hyunseorpg.exp.ExpService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import com.hyunseo.hyunseorpg.progression.RequirementChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class QuestService {
    private final PlayerDataService playerDataService;
    private final QuestRegistry questRegistry;
    private final RequirementChecker requirementChecker;
    private final CoinService coinService;
    private final ExpService expService;

    public QuestService(
            PlayerDataService playerDataService,
            QuestRegistry questRegistry,
            RequirementChecker requirementChecker,
            CoinService coinService,
            ExpService expService
    ) {
        this.playerDataService = playerDataService;
        this.questRegistry = questRegistry;
        this.requirementChecker = requirementChecker;
        this.coinService = coinService;
        this.expService = expService;
    }

    public boolean startQuest(Player player, String questId) {
        Optional<QuestData> quest = questRegistry.get(questId);
        if (quest.isEmpty() || !requirementChecker.areMet(player, quest.get().requirements())) {
            return false;
        }

        PlayerRPGData data = playerDataService.getOrLoad(player);
        String currentState = data.getQuestState(quest.get().questId());
        if (QuestState.ACTIVE.name().equals(currentState) || QuestState.READY_TO_CLAIM.name().equals(currentState)
                || QuestState.COMPLETED.name().equals(currentState)) {
            return false;
        }

        data.setQuestState(quest.get().questId(), QuestState.ACTIVE.name());
        for (QuestObjective objective : quest.get().objectives()) {
            data.setQuestProgress(quest.get().questId(), objective.objectiveId(), 0);
        }
        playerDataService.savePlayer(player);
        player.sendMessage(Component.text("퀘스트를 시작했습니다: " + quest.get().displayName(), NamedTextColor.GREEN));
        return true;
    }

    public List<QuestData> getQuests() {
        return questRegistry.getAll();
    }

    public Optional<QuestData> getQuest(String questId) {
        return questRegistry.get(questId);
    }

    public String getQuestState(Player player, String questId) {
        return playerDataService.getOrLoad(player).getQuestState(questId);
    }

    public int getQuestProgress(Player player, String questId, String objectiveId) {
        return playerDataService.getOrLoad(player).getQuestProgress(questId, objectiveId);
    }

    public void updateProgress(Player player, QuestObjectiveType type, String target, int amount) {
        if (amount <= 0) {
            return;
        }

        String normalizedTarget = normalizeId(target);
        PlayerRPGData data = playerDataService.getOrLoad(player);
        boolean changed = false;

        for (QuestData quest : questRegistry.getAll()) {
            if (!QuestState.ACTIVE.name().equals(data.getQuestState(quest.questId()))) {
                continue;
            }

            boolean questChanged = false;
            for (QuestObjective objective : quest.objectives()) {
                if (objective.type() != type || !objective.target().equals(normalizedTarget)) {
                    continue;
                }

                int current = data.getQuestProgress(quest.questId(), objective.objectiveId());
                int next = Math.min(objective.amount(), current + amount);
                if (next != current) {
                    data.setQuestProgress(quest.questId(), objective.objectiveId(), next);
                    changed = true;
                    questChanged = true;
                }
            }

            if (questChanged && isComplete(data, quest)) {
                data.setQuestState(quest.questId(), QuestState.READY_TO_CLAIM.name());
                player.sendMessage(Component.text("퀘스트 보상을 받을 수 있습니다: " + quest.displayName(), NamedTextColor.AQUA));
            }
        }

        if (changed) {
            playerDataService.savePlayer(player);
        }
    }

    public boolean completeQuest(Player player, String questId) {
        Optional<QuestData> quest = questRegistry.get(questId);
        if (quest.isEmpty()) {
            return false;
        }

        PlayerRPGData data = playerDataService.getOrLoad(player);
        if (!isComplete(data, quest.get())) {
            return false;
        }

        data.setQuestState(quest.get().questId(), QuestState.READY_TO_CLAIM.name());
        playerDataService.savePlayer(player);
        return true;
    }

    public boolean claimReward(Player player, String questId) {
        Optional<QuestData> quest = questRegistry.get(questId);
        if (quest.isEmpty()) {
            return false;
        }

        PlayerRPGData data = playerDataService.getOrLoad(player);
        if (!QuestState.READY_TO_CLAIM.name().equals(data.getQuestState(quest.get().questId()))) {
            return false;
        }

        for (QuestReward reward : quest.get().rewards()) {
            applyReward(player, data, reward);
        }
        data.setQuestState(quest.get().questId(), QuestState.COMPLETED.name());
        playerDataService.savePlayer(player);
        player.sendMessage(Component.text("퀘스트를 완료했습니다: " + quest.get().displayName(), NamedTextColor.GOLD));
        return true;
    }

    public boolean cancelQuest(Player player, String questId) {
        Optional<QuestData> quest = questRegistry.get(questId);
        if (quest.isEmpty()) {
            return false;
        }

        PlayerRPGData data = playerDataService.getOrLoad(player);
        if (!QuestState.ACTIVE.name().equals(data.getQuestState(quest.get().questId()))) {
            return false;
        }

        data.setQuestState(quest.get().questId(), QuestState.CANCELLED.name());
        playerDataService.savePlayer(player);
        return true;
    }

    private boolean isComplete(PlayerRPGData data, QuestData quest) {
        for (QuestObjective objective : quest.objectives()) {
            if (data.getQuestProgress(quest.questId(), objective.objectiveId()) < objective.amount()) {
                return false;
            }
        }
        return !quest.objectives().isEmpty();
    }

    private void applyReward(Player player, PlayerRPGData data, QuestReward reward) {
        switch (reward.type()) {
            case BASE_EXP -> expService.giveBaseExp(player, reward.amount());
            case CLASS_EXP -> expService.giveClassExp(player, reward.amount());
            case COINS -> coinService.addCoins(player, reward.amount());
            case STAT_POINTS -> data.setStatPoints(data.getStatPoints() + (int) reward.amount());
            case SKILL_POINTS -> data.setSkillPoints(data.getSkillPoints() + (int) reward.amount());
            case CLASS_STAT_POINTS -> data.setClassStatPoints(data.getClassStatPoints() + (int) reward.amount());
            case PROGRESSION_FLAG, WORLD_UNLOCK, NEXT_QUEST -> data.addProgressionFlag(reward.target());
            case ITEM -> {
                // Item rewards will be connected after the custom item service is expanded.
            }
        }
    }

    private String normalizeId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
