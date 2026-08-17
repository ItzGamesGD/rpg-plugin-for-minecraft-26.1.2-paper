package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.attribute.Attribute;
import java.util.UUID;

public final class NecrosisEffectHandler extends AbstractProductionEffectHandler {
    public NecrosisEffectHandler(ConfigService config) { super(config, "necrosis"); }
    @Override public void onApply(UUID targetId, ActiveEffectInstance instance) {
        addModifier(living(targetId), instance, Attribute.MAX_HEALTH,
                value("effect_necrosis", "max-health-multiplier", -0.10D));
    }
}
