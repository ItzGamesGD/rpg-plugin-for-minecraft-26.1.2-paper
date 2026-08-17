package com.hyunseo.hyunseorpg.alchemy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveEffectStoreTest {
    @Test
    void unrelatedEffectsCoexistAndSingleRemovalPreservesTheOtherEffect() {
        UUID target = UUID.randomUUID();
        ActiveEffectStore store = new ActiveEffectStore();
        ActiveEffectInstance vampire = instance(target, "effect_vampire");
        ActiveEffectInstance bleed = instance(target, "effect_bleed");

        store.put(target, vampire);
        store.put(target, bleed);

        assertEquals(2, store.size(target));
        assertTrue(store.contains(target, "EFFECT_VAMPIRE"));
        assertTrue(store.contains(target, "effect_bleed"));
        assertSame(vampire, store.remove(target, "effect_vampire"));
        assertEquals(1, store.size(target));
        assertFalse(store.contains(target, "effect_vampire"));
        assertSame(bleed, store.get(target, "effect_bleed"));
    }

    @Test
    void sameEffectIdUsesOneSlotWhileTargetAndEffectKeysStayIndependent() {
        UUID firstTarget = UUID.randomUUID();
        UUID secondTarget = UUID.randomUUID();
        ActiveEffectStore store = new ActiveEffectStore();
        ActiveEffectInstance first = instance(firstTarget, "effect_bleed");
        ActiveEffectInstance replacement = instance(firstTarget, "effect_bleed");
        ActiveEffectInstance otherTarget = instance(secondTarget, "effect_bleed");

        store.put(firstTarget, first);
        store.put(firstTarget, replacement);
        store.put(secondTarget, otherTarget);

        assertEquals(1, store.size(firstTarget));
        assertSame(replacement, store.get(firstTarget, "effect_bleed"));
        assertEquals(1, store.size(secondTarget));
        assertSame(otherTarget, store.get(secondTarget, "effect_bleed"));
    }

    @Test
    void removeAllClearsOnlyTheRequestedTarget() {
        UUID firstTarget = UUID.randomUUID();
        UUID secondTarget = UUID.randomUUID();
        ActiveEffectStore store = new ActiveEffectStore();
        store.put(firstTarget, instance(firstTarget, "effect_vampire"));
        store.put(firstTarget, instance(firstTarget, "effect_bleed"));
        store.put(secondTarget, instance(secondTarget, "effect_shock"));

        List<ActiveEffectInstance> removed = store.removeAll(firstTarget);

        assertEquals(2, removed.size());
        assertEquals(0, store.size(firstTarget));
        assertEquals(1, store.size(secondTarget));
        assertTrue(store.targetIds().contains(secondTarget));
    }

    private ActiveEffectInstance instance(UUID target, String effectId) {
        CustomEffectDefinition definition = new CustomEffectDefinition(
                effectId, effectId, true, 0, 100, 0, 1,
                EffectTargetPolicy.LIVING_ENTITY, EffectStackPolicy.REFRESH_DURATION,
                true, false, false, "", List.of());
        return new ActiveEffectInstance(UUID.randomUUID(), target, definition,
                new EffectSource(null, EffectSourceType.POTION_DRINK, "potion_" + effectId),
                100L, 1);
    }
}
