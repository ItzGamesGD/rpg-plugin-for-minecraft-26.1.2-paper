package com.hyunseo.hyunseorpg.alchemy;

import java.util.UUID;

public final class ActiveEffectInstance {
    private final UUID instanceId;
    private final UUID targetId;
    private final CustomEffectDefinition definition;
    private final EffectSource source;
    private long expiresAtTick;
    private int stacks;

    public ActiveEffectInstance(UUID instanceId, UUID targetId, CustomEffectDefinition definition,
                                EffectSource source, long expiresAtTick, int stacks) {
        this.instanceId = instanceId;
        this.targetId = targetId;
        this.definition = definition;
        this.source = source;
        this.expiresAtTick = expiresAtTick;
        this.stacks = stacks;
    }

    public UUID instanceId() { return instanceId; }
    public UUID targetId() { return targetId; }
    public CustomEffectDefinition definition() { return definition; }
    public EffectSource source() { return source; }
    public long expiresAtTick() { return expiresAtTick; }
    public int stacks() { return stacks; }
    public boolean expired(long currentTick) { return currentTick >= expiresAtTick; }
    public long remainingTicks(long currentTick) { return Math.max(0L, expiresAtTick - currentTick); }
    public void refresh(long expiresAtTick) { this.expiresAtTick = expiresAtTick; }
    public void addStack() { stacks = Math.min(definition.maxStacks(), Math.max(1, stacks + 1)); }
}
