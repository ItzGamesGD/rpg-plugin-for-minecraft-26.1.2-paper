package com.hyunseo.hyunseorpg.farming;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class DeliveryMenuHolder implements InventoryHolder {
    private final UUID playerId;
    private final DeliveryProvider provider;
    private final String deliveryId;
    private final DeliveryHoeBinding hoeBinding;
    private Inventory inventory;

    public DeliveryMenuHolder(UUID playerId, DeliveryProvider provider, String deliveryId) {
        this(playerId, provider, deliveryId, DeliveryHoeBinding.neutral());
    }

    public DeliveryMenuHolder(UUID playerId, DeliveryProvider provider, String deliveryId,
                              DeliveryHoeBinding hoeBinding) {
        this.playerId = playerId;
        this.provider = provider;
        this.deliveryId = deliveryId;
        this.hoeBinding = hoeBinding == null ? DeliveryHoeBinding.neutral() : hoeBinding;
    }

    public UUID playerId() { return playerId; }
    public DeliveryProvider provider() { return provider; }
    public String deliveryId() { return deliveryId; }
    public DeliveryHoeBinding hoeBinding() { return hoeBinding; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
