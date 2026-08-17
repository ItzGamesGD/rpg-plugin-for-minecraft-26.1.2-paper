package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import java.util.UUID;

public final class CorrosionEffectHandler extends AbstractProductionEffectHandler {
    public CorrosionEffectHandler(ConfigService config) { super(config, "corrosion"); }
    @Override public double modifyIncoming(UUID sourceId, UUID targetId, double damage) {
        return damage * value("effect_corrosion", "incoming-multiplier", 1.10D);
    }
}
