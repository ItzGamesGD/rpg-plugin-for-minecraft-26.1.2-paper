package com.hyunseo.hyunseorpg.skill.swordmaster;

import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class BladeObject {
    private final UUID ownerId;
    private final UUID throwId;
    private final Location location;
    private long expireTick;
    private final ItemDisplay displayEntity;
    private final double damage;
    private final double hitboxRadius;
    private final Set<UUID> hitEntities = new HashSet<>();
    private BladeState state;
    private Vector velocity;
    private Location destination;

    public BladeObject(UUID ownerId, UUID throwId, Location location, long expireTick, ItemDisplay displayEntity, double damage, double hitboxRadius, BladeState state, Vector velocity, Location destination) {
        this.ownerId = ownerId;
        this.throwId = throwId;
        this.location = location;
        this.expireTick = expireTick;
        this.displayEntity = displayEntity;
        this.damage = damage;
        this.hitboxRadius = hitboxRadius;
        this.state = state;
        this.velocity = velocity;
        this.destination = destination;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public UUID throwId() {
        return throwId;
    }

    public Location location() {
        return location;
    }

    public long expireTick() {
        return expireTick;
    }

    public void expireAt(long expireTick) {
        this.expireTick = expireTick;
    }

    public ItemDisplay displayEntity() {
        return displayEntity;
    }

    public double damage() {
        return damage;
    }

    public double hitboxRadius() {
        return hitboxRadius;
    }

    public BladeState state() {
        return state;
    }

    public void state(BladeState state) {
        this.state = state;
    }

    public Vector velocity() {
        return velocity;
    }

    public void velocity(Vector velocity) {
        this.velocity = velocity;
    }

    public Location destination() {
        return destination;
    }

    public void destination(Location destination) {
        this.destination = destination;
    }

    public boolean markHit(UUID entityId) {
        return hitEntities.add(entityId);
    }

    public void clearHits() {
        hitEntities.clear();
    }
}
