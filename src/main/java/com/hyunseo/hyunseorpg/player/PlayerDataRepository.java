package com.hyunseo.hyunseorpg.player;

import java.io.IOException;
import java.util.UUID;

public interface PlayerDataRepository {
    PlayerRPGData load(UUID uuid) throws IOException;

    void save(PlayerRPGData data) throws IOException;

    boolean exists(UUID uuid);
}
