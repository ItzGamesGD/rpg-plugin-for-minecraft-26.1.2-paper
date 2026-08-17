package com.hyunseo.hyunseorpg.quest;

import com.hyunseo.hyunseorpg.core.event.RPGMobKillEvent;
import com.hyunseo.hyunseorpg.core.event.MonsterKillContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class QuestProgressListener implements Listener {
    private final QuestService questService;
    private final AutoQuestService autoQuestService;

    public QuestProgressListener(QuestService questService) {
        this(questService, null);
    }

    public QuestProgressListener(QuestService questService, AutoQuestService autoQuestService) {
        this.questService = questService;
        this.autoQuestService = autoQuestService;
    }

    @EventHandler
    public void onRpgMobKill(RPGMobKillEvent event) {
        if (event.getMobId().isBlank()) {
            return;
        }

        if (autoQuestService != null) {
            autoQuestService.onMobKill(event);
        }

        questService.updateProgress(event.getKiller(), QuestObjectiveType.MOB_KILL, event.getMobId(), 1);
        MonsterKillContext context = event.getContext();
        if (context.customMob() && !context.customMobId().isBlank()) {
            questService.updateProgress(event.getKiller(), QuestObjectiveType.CUSTOM_MOB_KILL,
                    context.customMobId(), 1);
        }
        questService.updateProgress(event.getKiller(), QuestObjectiveType.VANILLA_ENTITY_KILL,
                context.vanillaType().name(), 1);
        for (String tag : context.tags()) {
            questService.updateProgress(event.getKiller(), QuestObjectiveType.MONSTER_TAG_KILL, tag, 1);
        }
        if (event.isBoss()) {
            questService.updateProgress(event.getKiller(), QuestObjectiveType.BOSS_KILL, event.getMobId(), 1);
        }
    }
}
