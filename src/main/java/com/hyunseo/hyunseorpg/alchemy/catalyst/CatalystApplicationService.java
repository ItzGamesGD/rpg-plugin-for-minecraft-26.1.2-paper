package com.hyunseo.hyunseorpg.alchemy.catalyst;

import com.hyunseo.hyunseorpg.alchemy.potion.PotionDefinition;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionPdcContract;
import com.hyunseo.hyunseorpg.alchemy.potion.PotionRegistry;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/** Validates and creates a canonical transformed potion without consuming inputs. */
public final class CatalystApplicationService {
    private final PotionRegistry potions;
    private final PotionPdcContract<ItemStack> pdc;
    private final CatalystRegistry catalysts;
    private final RPGItemService items;
    private final int supportedDataVersion;

    public CatalystApplicationService(PotionRegistry potions, PotionPdcContract<ItemStack> pdc,
                                      CatalystRegistry catalysts, int supportedDataVersion) {
        this(potions, pdc, catalysts, null, supportedDataVersion);
    }

    public CatalystApplicationService(PotionRegistry potions, PotionPdcContract<ItemStack> pdc,
                                      CatalystRegistry catalysts, RPGItemService items,
                                      int supportedDataVersion) {
        this.potions = potions;
        this.pdc = pdc;
        this.catalysts = catalysts;
        this.items = items;
        this.supportedDataVersion = supportedDataVersion;
    }

    public Result transform(ItemStack source, ItemStack catalystItem) {
        if (source == null || source.getType().isAir() || catalystItem == null || catalystItem.getType().isAir()) {
            return Result.rejected(Status.INVALID_INPUT);
        }
        String potionId = normalize(pdc.readPotionId(source));
        if (potionId.isBlank() || pdc.readDataVersion(source) != supportedDataVersion) {
            return Result.rejected(Status.INVALID_POTION_PDC);
        }
        PotionDefinition potion = potions.find(potionId).orElse(null);
        if (potion == null || !potion.enabled()) return Result.rejected(Status.POTION_DISABLED);
        if (items != null && !items.isItem(source, potion.outputItemId())) {
            return Result.rejected(Status.INVALID_POTION_ITEM);
        }
        if (!pdc.readCatalystId(source).isBlank()) return Result.rejected(Status.DUPLICATE_CATALYST);
        CatalystDefinition catalyst = catalysts.findByItem(catalystItem).orElse(null);
        if (catalyst == null || !catalyst.enabled()) return Result.rejected(Status.CATALYST_DISABLED);
        if (!catalyst.allowsPotion(potion.id())) return Result.rejected(Status.INCOMPATIBLE_CATALYST);

        String effectOverride = catalyst.inversionEffectIds().getOrDefault(potion.id(), "");
        if (catalyst.mode() == CatalystDefinition.Mode.INVERSION && effectOverride.isBlank()) {
            return Result.rejected(Status.INVERSION_NOT_ALLOWED);
        }
        String delivery = catalyst.delivery();
        if (catalyst.mode() == CatalystDefinition.Mode.DELIVERY) {
            if ("gunpowder".equals(catalyst.catalystId())) delivery = "SPLASH";
            else if ("dragon_breath".equals(catalyst.catalystId())) delivery = "LINGERING";
        }
        if ("SPLASH".equals(delivery) && potion.delivery() != PotionDefinition.Delivery.DRINK) {
            return Result.rejected(Status.INCOMPATIBLE_CATALYST);
        }
        if ("LINGERING".equals(delivery) && potion.delivery() == PotionDefinition.Delivery.LINGERING) {
            return Result.rejected(Status.INCOMPATIBLE_CATALYST);
        }
        ItemStack converted = source.clone();
        pdc.write(converted, potion, catalyst.catalystId(), supportedDataVersion);
        pdc.writeTransformation(converted, delivery, catalyst.durationMultiplierPercent(),
                catalyst.amplifierDelta(), catalyst.mode() == CatalystDefinition.Mode.INVERSION, effectOverride);
        CatalystApplication application = new CatalystApplication(potion.id(), catalyst.catalystId(),
                catalyst.durationMultiplierPercent(), catalyst.amplifierDelta(), parseDelivery(delivery),
                catalyst.mode() == CatalystDefinition.Mode.INVERSION, effectOverride);
        return Result.applied(converted, application);
    }

    private CatalystApplication.Delivery parseDelivery(String raw) {
        try { return CatalystApplication.Delivery.valueOf(raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return CatalystApplication.Delivery.ORIGINAL; }
    }

    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }

    public enum Status {
        APPLIED, INVALID_INPUT, INVALID_POTION_PDC, INVALID_POTION_ITEM, POTION_DISABLED,
        CATALYST_DISABLED, DUPLICATE_CATALYST, INCOMPATIBLE_CATALYST, INVERSION_NOT_ALLOWED
    }

    public record Result(Status status, ItemStack convertedItem, CatalystApplication application) {
        public static Result applied(ItemStack item, CatalystApplication application) {
            return new Result(Status.APPLIED, item, application);
        }
        public static Result rejected(Status status) { return new Result(status, null, null); }
        public boolean applied() { return status == Status.APPLIED && convertedItem != null; }
    }
}
