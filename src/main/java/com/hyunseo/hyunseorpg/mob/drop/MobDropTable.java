package com.hyunseo.hyunseorpg.mob.drop;

import java.util.List;

public record MobDropTable(String tableId, List<MobDropEntry> entries) {
    public MobDropTable {
        entries = List.copyOf(entries);
    }
}
