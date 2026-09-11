package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GatewaySession {
    private final UUID ownerId;
    private final Location snapshot;
    private final Location bossTarget;
    private final List<GatewayPair> pairs;
    private final List<Entity> entities = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private PrototypeBossDummy dummy;

    public GatewaySession(Player owner, Location snapshot, Location bossTarget, List<GatewayPair> pairs) {
        this.ownerId = owner.getUniqueId();
        this.snapshot = snapshot.clone();
        this.bossTarget = bossTarget.clone();
        this.pairs = List.copyOf(pairs);
    }

    public UUID ownerId() { return ownerId; }
    public Location snapshot() { return snapshot.clone(); }
    public Location bossTarget() { return bossTarget.clone(); }
    public List<GatewayPair> pairs() { return pairs; }
    public List<Entity> entities() { return entities; }
    public List<BukkitTask> tasks() { return tasks; }
    public PrototypeBossDummy dummy() { return dummy; }
    public void setDummy(PrototypeBossDummy dummy) { this.dummy = dummy; }

    public void cleanup() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        entities.forEach(entity -> { if (entity.isValid()) entity.remove(); });
        entities.clear();
    }
}
