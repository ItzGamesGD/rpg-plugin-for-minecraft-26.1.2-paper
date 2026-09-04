package com.hyunseo.hyunseorpg.special.solaris;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SolarisLogicTest {
 @Test void dawnClaimsFirstHitBlocksWindowAndAllowsExpiry(){Map<UUID,Long> m=new HashMap<>();UUID id=UUID.randomUUID();assertTrue(SolarisLogic.claimDawn(m,id,1000,20000));assertFalse(SolarisLogic.claimDawn(m,id,20999,20000));assertTrue(SolarisLogic.claimDawn(m,id,21000,20000));}
 @Test void dawnPropagationClaimsSplashOnceAndCannotLoop(){Map<UUID,Long> m=new HashMap<>();UUID a=UUID.randomUUID(),b=UUID.randomUUID();assertTrue(SolarisLogic.claimDawn(m,a,0,20000));assertTrue(SolarisLogic.claimDawn(m,b,0,20000));assertFalse(SolarisLogic.claimDawn(m,a,0,20000));assertFalse(SolarisLogic.claimDawn(m,b,0,20000));}
 @Test void denseDawnChainDamagesEachReachableTargetOnlyOnce(){Set<String> damaged=new HashSet<>();Map<String,Integer> hits=new HashMap<>();Map<String,List<String>> graph=Map.of("A",List.of("B","C"),"B",List.of("A","C"),"C",List.of("A","B"));for(String origin:graph.keySet())for(String target:graph.get(origin))if(SolarisLogic.registerDawnDamage(damaged,target))hits.merge(target,1,Integer::sum);assertEquals(Set.of("A","B","C"),damaged);assertTrue(hits.values().stream().allMatch(count->count==1));}
 @Test void dawnPruningExpiresInclusivelyAtResetBoundary(){UUID id=UUID.randomUUID();Map<UUID,Long> timestamps=new HashMap<>(Map.of(id,1000L));SolarisLogic.pruneExpired(timestamps,20999,20000);assertTrue(timestamps.containsKey(id));SolarisLogic.pruneExpired(timestamps,21000,20000);assertFalse(timestamps.containsKey(id));}
 @Test void damageMarkExpiryIsInclusiveAtValidityBoundary(){assertFalse(SolarisLogic.expired(1000,5999,5000));assertTrue(SolarisLogic.expired(1000,6000,5000));}
 @Test void judgmentRiseTracksCurrentPositionAndHorizontalFacing(){var first=SolarisLogic.judgmentRisePosition(0,64,0,0,1,5);var moved=SolarisLogic.judgmentRisePosition(10,65,3,1,0,5);assertEquals(new SolarisLogic.Point(0,70,-2),first);assertEquals(new SolarisLogic.Point(8,71,3),moved);var vertical=SolarisLogic.judgmentRisePosition(2,3,4,0,0,5);assertEquals(new SolarisLogic.Point(2,9,4),vertical);}
 @Test void frozenJudgmentCenterDoesNotTrackLaterMovement(){var frozen=SolarisLogic.judgmentRisePosition(0,64,0,0,1,10);SolarisLogic.judgmentRisePosition(20,70,20,-1,0,10);assertEquals(new SolarisLogic.Point(0,75,-2),frozen);}
 @Test void orbRoundingCapAndZeroAreJavaMathRound(){assertEquals(0,SolarisLogic.orbCount(0,1.5,20));assertEquals(2,SolarisLogic.orbCount(1,1.5,20));assertEquals(5,SolarisLogic.orbCount(3,1.5,20));assertEquals(20,SolarisLogic.orbCount(100,1.5,20));}
 @Test void killWindowExpiresAtBoundary(){Deque<Long> q=new ArrayDeque<>(List.of(0L,1L,30000L));SolarisLogic.pruneKills(q,30000,30000);assertEquals(List.of(1L,30000L),List.copyOf(q));}
 @Test void distributionIsNearestFirstTwiceAndSkipsInvalid(){var c=List.of(new SolarisLogic.Candidate<>("far",9,true),new SolarisLogic.Candidate<>("dead",1,false),new SolarisLogic.Candidate<>("near",4,true));assertEquals(List.of("near","far","near","far"),SolarisLogic.distribute(c,10,2));assertTrue(SolarisLogic.distribute(List.of(),2,2).isEmpty());}
 @Test void meteorLocationsAreDeterministicBoundedAndNotTargets(){var a=SolarisLogic.meteorOffsets(42,30,30),b=SolarisLogic.meteorOffsets(42,30,30);assertEquals(a,b);assertEquals(30,a.size());assertTrue(a.stream().allMatch(p->p.x()*p.x()+p.z()*p.z()<=900.000001));}
 @Test void impactUsesSphericalRadius(){assertTrue(SolarisLogic.withinImpact(1,1,1,2.5));assertFalse(SolarisLogic.withinImpact(2,2,2,2.5));}
 @Test void schedulingCoversRequestedCountWithinDuration(){var ticks=SolarisLogic.scheduleTicks(30,160);assertEquals(30,ticks.size());assertEquals(0,ticks.getFirst());assertTrue(ticks.getLast()<160);assertTrue(SolarisLogic.scheduleTicks(0,160).isEmpty());}
}
