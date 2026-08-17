package com.hyunseo.hyunseorpg.quest;

import com.hyunseo.hyunseorpg.quest.availability.QuestTargetSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoQuestDataTest {
    @Test
    void multiTargetQuestRequiresEveryObjective() {
        AutoQuestData quest = new AutoQuestData(1, "test", AutoQuestType.HUNT, List.of(
                new AutoQuestObjectiveData(QuestTargetSource.VANILLA_MOB, "zombie", "Zombie", 2, 0, 1),
                new AutoQuestObjectiveData(QuestTargetSource.VANILLA_MOB, "skeleton", "Skeleton", 1, 0, 1)
        ), "Hunt", 1L, 2L, 10L, 5L, AutoQuestStatus.ACTIVE);

        quest.addProgress(QuestTargetSource.VANILLA_MOB, "zombie", 2);
        assertFalse(quest.isComplete());
        quest.addProgress(QuestTargetSource.VANILLA_MOB, "skeleton", 1);
        assertTrue(quest.isComplete());
    }

    @Test
    void legacyConstructorRetainsSingleObjective() {
        AutoQuestData quest = new AutoQuestData(1, "legacy", AutoQuestType.ITEM_DELIVERY,
                "coal", "Coal", 8, 0, 1L, 2L, 10L, 5L);

        assertTrue(quest.objectives().size() == 1);
        assertFalse(quest.isComplete());
    }
}
