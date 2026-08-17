package com.hyunseo.hyunseorpg.alchemy;

import java.util.UUID;

/** Optional damage bridge for handlers that change the normal Paper damage flow. */
public interface CombatEffectModifier {
    default double modifyOutgoing(UUID sourceId, UUID targetId, double damage) { return damage; }
    default double modifyIncoming(UUID sourceId, UUID targetId, double damage) { return damage; }
}
