package com.hyunseo.hyunseorpg.rpgtest.gateway;

import java.util.UUID;

public final class ReflectableProjectileState {
    public enum Phase { OUTBOUND, RETURNING_TO_SOURCE, RETURNING_TO_BOSS, FUSE, FINISHED }
    public enum CollisionResult { OUTBOUND_STOPPED, ROUTE_FAILED, BOSS_HIT, IGNORED }

    private final UUID id;
    private final int sourceGatewayId;
    private final GatewayPayloadType type;
    private Phase phase = Phase.OUTBOUND;

    public ReflectableProjectileState(UUID id, int sourceGatewayId, GatewayPayloadType type) {
        if (!type.reflectable()) throw new IllegalArgumentException("Non-reflectable payload: " + type);
        this.id = id;
        this.sourceGatewayId = sourceGatewayId;
        this.type = type;
    }

    public UUID id() { return id; }
    public int sourceGatewayId() { return sourceGatewayId; }
    public GatewayPayloadType type() { return type; }
    public Phase phase() { return phase; }
    public void reflect() { if (phase == Phase.OUTBOUND) phase = Phase.RETURNING_TO_SOURCE; }
    public void enterSourceGateway() { if (phase == Phase.RETURNING_TO_SOURCE) phase = Phase.RETURNING_TO_BOSS; }
    public void beginFuse() { if (type == GatewayPayloadType.END_CRYSTAL_BOMB) phase = Phase.FUSE; }
    public CollisionResult collide(boolean hitBoss) {
        CollisionResult result = switch (phase) {
            case OUTBOUND -> CollisionResult.OUTBOUND_STOPPED;
            case RETURNING_TO_SOURCE -> CollisionResult.ROUTE_FAILED;
            case RETURNING_TO_BOSS -> hitBoss ? CollisionResult.BOSS_HIT : CollisionResult.ROUTE_FAILED;
            default -> CollisionResult.IGNORED;
        };
        if (result != CollisionResult.IGNORED) phase = Phase.FINISHED;
        return result;
    }
    public void finish() { phase = Phase.FINISHED; }
}
