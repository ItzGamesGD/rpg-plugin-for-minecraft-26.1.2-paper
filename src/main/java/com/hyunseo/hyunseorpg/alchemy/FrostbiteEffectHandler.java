package com.hyunseo.hyunseorpg.alchemy;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import org.bukkit.attribute.Attribute;
import java.util.UUID;

public final class FrostbiteEffectHandler extends AbstractProductionEffectHandler {
    public FrostbiteEffectHandler(ConfigService config) { super(config, "frostbite"); }
    @Override public void onApply(UUID targetId, ActiveEffectInstance instance) {
        addModifier(living(targetId), instance, Attribute.MOVEMENT_SPEED,
                value("effect_frostbite", "movement-multiplier", -0.15D));
    }
}
