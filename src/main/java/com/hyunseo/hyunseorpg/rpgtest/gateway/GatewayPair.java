package com.hyunseo.hyunseorpg.rpgtest.gateway;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.UUID;

public record GatewayPair(int id, Location launcher, Location returnGateway, Vector snapshotForward,
                          UUID launcherVisualId, UUID returnVisualId) {
    public GatewayPair {
        launcher = launcher.clone();
        returnGateway = returnGateway.clone();
        snapshotForward = snapshotForward.clone();
    }

    public Location launcher() { return launcher.clone(); }
    public Location returnGateway() { return returnGateway.clone(); }
    public Vector snapshotForward() { return snapshotForward.clone(); }
}
