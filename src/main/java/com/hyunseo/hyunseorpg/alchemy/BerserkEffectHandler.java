package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import java.util.UUID;

public final class BerserkEffectHandler extends AbstractProductionEffectHandler {
    public BerserkEffectHandler(ConfigService config) { super(config, "berserk"); }
    @Override public double modifyOutgoing(UUID sourceId, UUID targetId, double damage) {
        return damage * value("effect_berserk", "outgoing-multiplier", 1.10D);
    }
    @Override public double modifyIncoming(UUID sourceId, UUID targetId, double damage) {
        return damage * value("effect_berserk", "incoming-multiplier", 1.05D);
    }
}
