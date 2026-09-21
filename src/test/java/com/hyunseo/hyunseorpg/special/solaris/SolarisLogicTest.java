package com.hyunseo.hyunseorpg.special.solaris;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SolarisLogicTest {
 @Test void dawnClaimsFirstHitBlocksWindowAndAllowsExpiry(){Map<UUID,Long> m=new HashMap<>();UUID id=UUID.randomUUID(),stale=UUID.randomUUID();m.put(stale,0L);assertTrue(SolarisLogic.claimDawn(m,id,1000,20000));assertFalse(SolarisLogic.claimDawn(m,id,20999,20000));assertTrue(SolarisLogic.claimDawn(m,id,21000,20000));assertFalse(m.containsKey(stale));}
 @Test void dawnDenseOverlappingChainRegistersEachTargetOnce(){Map<String,List<String>> graph=Map.of("A",List.of("B","C"),"B",List.of("A","C"),"C",List.of("A","B"));Set<String> propagation=new HashSet<>(),damaged=new HashSet<>();Map<String,Integer> hits=new HashMap<>();Deque<String> queue=new ArrayDeque<>(List.of("A"));while(!queue.isEmpty()){String origin=queue.remove();if(!propagation.add(origin))continue;for(String target:graph.get(origin)){if(SolarisLogic.registerDawnDamage(damaged,target))hits.merge(target,1,Integer::sum);queue.add(target);}}assertEquals(Set.of("A","B","C"),propagation);assertEquals(Set.of("A","B","C"),damaged);assertTrue(hits.values().stream().allMatch(count->count==1));}
 @Test void expiryBoundariesAreInclusive(){assertFalse(SolarisLogic.isExpired(1000,20999,20000));assertTrue(SolarisLogic.isExpired(1000,21000,20000));assertFalse(SolarisLogic.isExpired(1000,5999,5000));assertTrue(SolarisLogic.isExpired(1000,6000,5000));}
 @Test void orbRoundingCapAndZeroAreJavaMathRound(){assertEquals(0,SolarisLogic.orbCount(0,1.5,20));assertEquals(2,SolarisLogic.orbCount(1,1.5,20));assertEquals(5,SolarisLogic.orbCount(3,1.5,20));assertEquals(20,SolarisLogic.orbCount(100,1.5,20));}
 @Test void killWindowExpiresAtBoundary(){Deque<Long> q=new ArrayDeque<>(List.of(0L,1L,30000L));SolarisLogic.pruneKills(q,30000,30000);assertEquals(List.of(1L,30000L),List.copyOf(q));}
 @Test void distributionIsNearestFirstTwiceAndSkipsInvalid(){var c=List.of(new SolarisLogic.Candidate<>("far",9,true),new SolarisLogic.Candidate<>("dead",1,false),new SolarisLogic.Candidate<>("near",4,true));assertEquals(List.of("near","far","near","far"),SolarisLogic.distribute(c,10,2));assertTrue(SolarisLogic.distribute(List.of(),2,2).isEmpty());}
 @Test void meteorLocationsAreDeterministicBoundedAndNotTargets(){var a=SolarisLogic.meteorOffsets(42,30,30);var b=SolarisLogic.meteorOffsets(42,30,30);assertEquals(a,b);assertEquals(30,a.size());assertTrue(a.stream().allMatch(p->p.x()*p.x()+p.z()*p.z()<=900.000001));}
 @Test void impactUsesSphericalRadius(){assertTrue(SolarisLogic.withinImpact(1,1,1,2.5));assertFalse(SolarisLogic.withinImpact(2,2,2,2.5));}
 @Test void judgmentRiseTracksPositionAndFacingAndFrozenValueIsStable(){var first=SolarisLogic.judgmentRisePosition(10,64,20,1,0,2,5);var frozen=first;var moved=SolarisLogic.judgmentRisePosition(20,65,30,0,1,2,5);assertNotEquals(first,moved);assertEquals(new SolarisLogic.Position(8,70,20),frozen);assertEquals(new SolarisLogic.Position(20,71,28),moved);assertEquals(new SolarisLogic.Position(8,70,20),frozen);}
 @Test void judgmentRiseHandlesNearZeroHorizontalFacing(){var position=SolarisLogic.judgmentRisePosition(10,64,20,1e-20,-1e-20,2,5);assertTrue(Double.isFinite(position.x()));assertTrue(Double.isFinite(position.y()));assertTrue(Double.isFinite(position.z()));assertEquals(new SolarisLogic.Position(10,70,20),position);}
 @Test void schedulingCoversRequestedCountWithinDuration(){var ticks=SolarisLogic.scheduleTicks(30,160);assertEquals(30,ticks.size());assertEquals(0,ticks.getFirst());assertTrue(ticks.getLast()<160);assertTrue(SolarisLogic.scheduleTicks(0,160).isEmpty());}
}
