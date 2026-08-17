package com.hyunseo.hyunseorpg.quest;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import com.hyunseo.hyunseorpg.quest.availability.QuestTargetSource;

/** Persisted player-specific quest generated from currently accessible content. */
public final class AutoQuestData {
    private final int slot;
    private final String id;
    private final AutoQuestType type;
    private final QuestTargetSource targetSource;
    private final String target;
    private final String displayName;
    private final int amount;
    private int progress;
    private final long createdAt;
    private final long expiresAt;
    private final long rewardCoins;
    private final long rewardExp;
    private final int difficulty;
    private AutoQuestStatus status;
    private final List<AutoQuestObjectiveData> objectives;

    public AutoQuestData(int slot, String id, AutoQuestType type, String target, String displayName,
                         int amount, int progress, long createdAt, long expiresAt,
                         long rewardCoins, long rewardExp) {
        this(slot, id, type, type == AutoQuestType.ITEM_DELIVERY
                        ? QuestTargetSource.HYUNSEORPG_CUSTOM_ITEM : QuestTargetSource.HYUNSEORPG_CUSTOM_MOB,
                target, displayName, amount, progress, createdAt, expiresAt, rewardCoins, rewardExp,
                1, progress >= amount ? AutoQuestStatus.READY_TO_COMPLETE : AutoQuestStatus.ACTIVE);
    }

    public AutoQuestData(int slot, String id, AutoQuestType type, QuestTargetSource targetSource,
                         String target, String displayName, int amount, int progress, long createdAt, long expiresAt,
                         long rewardCoins, long rewardExp, int difficulty, AutoQuestStatus status) {
        this.slot = Math.max(1, slot);
        this.id = normalize(id);
        this.type = type == null ? AutoQuestType.HUNT : type;
        this.targetSource = targetSource == null
                ? (this.type == AutoQuestType.ITEM_DELIVERY ? QuestTargetSource.HYUNSEORPG_CUSTOM_ITEM : QuestTargetSource.HYUNSEORPG_CUSTOM_MOB)
                : targetSource;
        this.target = normalize(target);
        this.displayName = displayName == null || displayName.isBlank() ? this.target : displayName;
        this.amount = Math.max(1, amount);
        this.progress = Math.max(0, Math.min(this.amount, progress));
        this.createdAt = Math.max(0L, createdAt);
        this.expiresAt = Math.max(0L, expiresAt);
        this.rewardCoins = Math.max(0L, rewardCoins);
        this.rewardExp = Math.max(0L, rewardExp);
        this.difficulty = Math.max(1, difficulty);
        this.status = status == null ? (this.progress >= this.amount ? AutoQuestStatus.READY_TO_COMPLETE : AutoQuestStatus.ACTIVE) : status;
        this.objectives = new ArrayList<>();
        this.objectives.add(new AutoQuestObjectiveData(this.targetSource, this.target, this.displayName,
                this.amount, this.progress, this.difficulty));
    }

    public AutoQuestData(int slot, String id, AutoQuestType type, List<AutoQuestObjectiveData> objectives,
                         String displayName, long createdAt, long expiresAt, long rewardCoins, long rewardExp,
                         AutoQuestStatus status) {
        this(slot, id, type, firstSource(type, objectives), firstTarget(objectives), displayName,
                firstAmount(objectives), firstProgress(objectives), createdAt, expiresAt, rewardCoins, rewardExp,
                firstDifficulty(objectives), status);
        if (objectives != null && !objectives.isEmpty()) {
            this.objectives.clear();
            this.objectives.addAll(objectives);
            refreshStatus();
        }
    }

    public int slot() { return slot; }
    public String id() { return id; }
    public AutoQuestType type() { return type; }
    public QuestTargetSource targetSource() { return targetSource; }
    public String target() { return target; }
    public String displayName() { return displayName; }
    public int amount() { return amount; }
    public int progress() { return progress; }
    public long createdAt() { return createdAt; }
    public long expiresAt() { return expiresAt; }
    public long rewardCoins() { return rewardCoins; }
    public long rewardExp() { return rewardExp; }
    public int difficulty() { return difficulty; }
    public AutoQuestStatus status() { return status; }
    public List<AutoQuestObjectiveData> objectives() { return List.copyOf(objectives); }
    public boolean isComplete() { return objectives.stream().allMatch(AutoQuestObjectiveData::complete); }
    public boolean isExpired(long now) { return expiresAt > 0L && expiresAt <= now; }

    public void addProgress(int amount) {
        if (objectives.isEmpty()) return;
        objectives.getFirst().addProgress(amount);
        progress = objectives.getFirst().progress();
        refreshStatus();
    }

    public void addProgress(QuestTargetSource source, String target, int amount) {
        for (AutoQuestObjectiveData objective : objectives) {
            if (objective.source() != source || !objective.target().equalsIgnoreCase(normalize(target))) continue;
            objective.addProgress(amount);
        }
        if (!objectives.isEmpty()) progress = objectives.getFirst().progress();
        refreshStatus();
    }

    public void setStatus(AutoQuestStatus status) {
        this.status = status == null ? AutoQuestStatus.ACTIVE : status;
    }

    private void refreshStatus() {
        status = isComplete() ? AutoQuestStatus.READY_TO_COMPLETE : AutoQuestStatus.ACTIVE;
    }

    private static QuestTargetSource firstSource(AutoQuestType type, List<AutoQuestObjectiveData> objectives) {
        if (objectives != null && !objectives.isEmpty()) return objectives.getFirst().source();
        return type == AutoQuestType.ITEM_DELIVERY ? QuestTargetSource.HYUNSEORPG_CUSTOM_ITEM : QuestTargetSource.HYUNSEORPG_CUSTOM_MOB;
    }

    private static String firstTarget(List<AutoQuestObjectiveData> objectives) {
        return objectives == null || objectives.isEmpty() ? "" : objectives.getFirst().target();
    }

    private static int firstAmount(List<AutoQuestObjectiveData> objectives) {
        return objectives == null || objectives.isEmpty() ? 1 : objectives.getFirst().amount();
    }

    private static int firstProgress(List<AutoQuestObjectiveData> objectives) {
        return objectives == null || objectives.isEmpty() ? 0 : objectives.getFirst().progress();
    }

    private static int firstDifficulty(List<AutoQuestObjectiveData> objectives) {
        return objectives == null || objectives.isEmpty() ? 1 : objectives.getFirst().difficulty();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
