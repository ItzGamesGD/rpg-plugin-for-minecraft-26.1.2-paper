package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;

public final class PrototypeBossDummy {
    public static final double MAX_HEALTH = 100.0;
    private final ArmorStand entity;
    private final PrototypeBossHealth health = new PrototypeBossHealth(MAX_HEALTH);

    public PrototypeBossDummy(ArmorStand entity) {
        this.entity = entity;
        refreshName();
    }

    public ArmorStand entity() { return entity; }
    public double health() { return health.current(); }
    public boolean damage(double amount) {
        if (!health.damage(amount)) return false;
        refreshName();
        if (health.defeated()) {
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 1.2f);
            entity.setCustomName("§aGateway dummy defeated");
        }
        return true;
    }

    private void refreshName() {
        entity.setCustomName("§5Gateway boss dummy §f%.0f/%.0f HP".formatted(health.current(), MAX_HEALTH));
        entity.setCustomNameVisible(true);
    }
}
