package com.hyunseo.hyunseorpg.exploration;

import com.hyunseo.hyunseorpg.exploration.raid.RaidMobDefinition;
import com.hyunseo.hyunseorpg.exploration.raid.RaidWavePlanner;
import com.hyunseo.hyunseorpg.exploration.raid.RaidWavePoolDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RaidWavePlannerTest {
    private final RaidWavePlanner planner = new RaidWavePlanner();

    @Test
    void tierOneNeverSelectsHeavyUnits() {
        RaidWavePoolDefinition tierOne = new RaidWavePoolDefinition("tier1", 4, 0, List.of(
                new RaidMobDefinition("shield_raider", 2, 3, false, 7),
                new RaidMobDefinition("crossbow_raider", 3, 4, false, 6),
                new RaidMobDefinition("charger_raider", 1, 2, false, 7)));
        for (int seed = 0; seed < 100; seed++) {
            List<RaidMobDefinition> wave = planner.plan(tierOne, new Random(seed));
            assertFalse(wave.isEmpty());
            assertTrue(wave.size() <= 4);
            assertTrue(wave.stream().noneMatch(RaidMobDefinition::heavy));
        }
    }

    @Test
    void higherTiersRespectHeavyAndPerMobCaps() {
        RaidWavePoolDefinition tierThree = new RaidWavePoolDefinition("tier3", 8, 2, List.of(
                new RaidMobDefinition("shield_raider", 3, 3, false, 9),
                new RaidMobDefinition("crossbow_raider", 4, 3, false, 8),
                new RaidMobDefinition("charger_raider", 2, 2, false, 9),
                new RaidMobDefinition("banner_raider", 1, 1, true, 10),
                new RaidMobDefinition("spike_evoker", 1, 1, true, 11),
                new RaidMobDefinition("ravager_rider", 1, 1, true, 12)));
        for (int seed = 0; seed < 250; seed++) {
            List<RaidMobDefinition> wave = planner.plan(tierThree, new Random(seed));
            assertTrue(wave.size() <= 8);
            assertTrue(wave.stream().filter(RaidMobDefinition::heavy).count() <= 2);
            assertTrue(wave.stream().filter(mob -> mob.mobId().equals("ravager_rider")).count() <= 1);
            for (RaidMobDefinition entry : tierThree.mobs()) {
                assertTrue(wave.stream().filter(mob -> mob.mobId().equals(entry.mobId())).count() <= entry.maxSpawns());
            }
        }
    }
}
