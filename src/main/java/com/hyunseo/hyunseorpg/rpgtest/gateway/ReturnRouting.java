package com.hyunseo.hyunseorpg.rpgtest.gateway;

import java.util.List;
import java.util.Optional;

public final class ReturnRouting {
    private ReturnRouting() {}

    public static Optional<GatewayPair> pairFor(List<GatewayPair> pairs, ReflectableProjectileState projectile) {
        return pairs.stream().filter(pair -> pair.id() == projectile.sourceGatewayId()).findFirst();
    }
}
