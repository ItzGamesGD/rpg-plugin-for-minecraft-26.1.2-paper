package com.hyunseo.hyunseorpg.alchemy.potion;

import com.hyunseo.hyunseorpg.alchemy.EffectContext;
import com.hyunseo.hyunseorpg.alchemy.EffectService;
import com.hyunseo.hyunseorpg.alchemy.EffectSourceType;
import com.hyunseo.hyunseorpg.alchemy.catalyst.BoundedSpecialCatalystExecutionService;
import com.hyunseo.hyunseorpg.alchemy.catalyst.CatalystDefinition;
import com.hyunseo.hyunseorpg.alchemy.catalyst.CatalystRegistry;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public final class PaperPotionUseService implements PotionUseService<ItemStack> {
    private final PotionRegistry potions; private final PotionPdcContract<ItemStack> pdc; private final EffectService effects; private final RPGItemService items; private final int supportedVersion;
    private final CatalystRegistry catalysts;
    private final BoundedSpecialCatalystExecutionService specialExecutions;
    public PaperPotionUseService(PotionRegistry potions, PotionPdcContract<ItemStack> pdc, EffectService effects, int supportedVersion) {
        this(potions, pdc, effects, null, supportedVersion);
    }
    public PaperPotionUseService(PotionRegistry potions, PotionPdcContract<ItemStack> pdc,
                                 EffectService effects, RPGItemService items, int supportedVersion) {
        this(potions, pdc, effects, items, supportedVersion, null, null);
    }
    public PaperPotionUseService(PotionRegistry potions, PotionPdcContract<ItemStack> pdc,
                                 EffectService effects, RPGItemService items, int supportedVersion,
                                 CatalystRegistry catalysts,
                                 BoundedSpecialCatalystExecutionService specialExecutions) {
        this.potions = potions; this.pdc = pdc; this.effects = effects; this.items = items; this.supportedVersion = supportedVersion;
        this.catalysts = catalysts; this.specialExecutions = specialExecutions;
    }
    @Override public UseResult use(UUID playerId, ItemStack item) {
        String id = pdc.readPotionId(item); if (id.isBlank()) return UseResult.NOT_A_POTION;
        PotionDefinition definition = potions.find(id).orElse(null); if (definition == null) return UseResult.UNKNOWN_POTION;
        if (!definition.enabled()) return UseResult.DISABLED_POTION;
        if (items != null && !items.isItem(item, definition.outputItemId())) return UseResult.INVALID_ITEM;
        // Crafting and older give paths may preserve the canonical item ID while
        // dropping the optional potion metadata. Recover that original potion
        // instead of rejecting a valid canonical item as INVALID_PDC.
        if (pdc.readDataVersion(item) != supportedVersion) {
            pdc.write(item, definition, "", supportedVersion);
        }
        String catalystId = pdc.readCatalystId(item);
        CatalystDefinition catalyst = null;
        if (!catalystId.isBlank()) {
            if (catalysts == null) {
                pdc.write(item, definition, "", supportedVersion);
                catalystId = "";
            }
            catalyst = catalysts == null || catalystId.isBlank() ? null : catalysts.find(catalystId).orElse(null);
            if (!catalystId.isBlank() && (catalyst == null || !catalyst.enabled())) {
                pdc.write(item, definition, "", supportedVersion);
                catalystId = "";
                catalyst = null;
            }
            if (catalyst.mode() == CatalystDefinition.Mode.SPECIAL) {
                if (specialExecutions == null) return UseResult.EFFECT_REJECTED;
                return specialExecutions.start(playerId, definition.id(), catalyst.catalystId())
                        == com.hyunseo.hyunseorpg.alchemy.catalyst.SpecialCatalystExecution.Result.STARTED
                        ? UseResult.USED : UseResult.EFFECT_REJECTED;
            }
        }
        String delivery = pdc.readDelivery(item);
        if (catalystId.isBlank() && !delivery.isBlank() && !"ORIGINAL".equalsIgnoreCase(delivery)) {
            return UseResult.INVALID_PDC;
        }
        PotionDefinition.Delivery effectiveDelivery = definition.delivery();
        try {
            if (!"ORIGINAL".equalsIgnoreCase(delivery)) effectiveDelivery = PotionDefinition.Delivery.valueOf(delivery);
        } catch (IllegalArgumentException ignored) { return UseResult.INVALID_PDC; }
        EffectSourceType source = switch (effectiveDelivery) {
            case DRINK -> EffectSourceType.POTION_DRINK;
            case SPLASH -> EffectSourceType.POTION_SPLASH;
            case LINGERING -> EffectSourceType.POTION_LINGERING;
        };
        String effectId = pdc.readEffectOverride(item);
        if (pdc.readInverted(item) && effectId.isBlank()) return UseResult.INVALID_PDC;
        if (effectId.isBlank()) effectId = definition.effectId();
        var effectDefinition = effects.registry().get(effectId).orElse(null);
        if (effectDefinition == null || !effectDefinition.enabled()) return UseResult.EFFECT_DISABLED;
        int duration = effectDefinition.durationTicks();
        int durationPercent = pdc.readDurationPercent(item);
        if (durationPercent < 1 || durationPercent > 1000) return UseResult.INVALID_PDC;
        int amplifier = Math.max(0, effectDefinition.amplifier() + pdc.readAmplifierDelta(item));
        boolean transformed = !catalystId.isBlank() && (durationPercent != 100 || amplifier != effectDefinition.amplifier()
                || !effectId.equals(definition.effectId()));
        boolean applied = transformed
                ? effects.applyWithOverrides(playerId, effectId, new EffectContext(playerId, source, definition.id(), playerId, null),
                Math.max(1, Math.round(duration * Math.max(1, durationPercent) / 100.0F)), amplifier)
                : effects.apply(playerId, effectId, new EffectContext(playerId, source, definition.id(), playerId, null));
        return applied ? UseResult.USED : UseResult.EFFECT_REJECTED;
    }
}
