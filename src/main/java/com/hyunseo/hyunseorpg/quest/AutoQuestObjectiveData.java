package com.hyunseo.hyunseorpg.quest;

import com.hyunseo.hyunseorpg.quest.availability.QuestTargetSource;

import java.util.Locale;

/** One immutable target definition with mutable player progress inside a generated quest. */
public final class AutoQuestObjectiveData {
    private final QuestTargetSource source;
    private final String target;
    private final String displayName;
    private final int amount;
    private final int difficulty;
    private int progress;

    public AutoQuestObjectiveData(QuestTargetSource source, String target, String displayName,
                                  int amount, int progress, int difficulty) {
        this.source = source == null ? QuestTargetSource.HYUNSEORPG_CUSTOM_MOB : source;
        this.target = normalize(target);
        this.displayName = displayName == null || displayName.isBlank() ? this.target : displayName;
        this.amount = Math.max(1, amount);
        this.progress = Math.max(0, Math.min(this.amount, progress));
        this.difficulty = Math.max(1, difficulty);
    }

    public QuestTargetSource source() { return source; }
    public String target() { return target; }
    public String displayName() { return displayName; }
    public int amount() { return amount; }
    public int progress() { return progress; }
    public int difficulty() { return difficulty; }
    public boolean complete() { return progress >= amount; }

    public void addProgress(int amount) {
        progress = Math.min(this.amount, progress + Math.max(0, amount));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
