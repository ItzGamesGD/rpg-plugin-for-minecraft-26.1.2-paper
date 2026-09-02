package com.hyunseo.hyunseorpg.special.thanatos;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ThanatosStateTest {
 @Test void mortalIsFirstApplicationOwnedUntilResolution(){ThanatosState s=new ThanatosState();UUID id=UUID.randomUUID();assertTrue(s.beginMortal(id,10,100));assertFalse(s.beginMortal(id,20,100));assertFalse(s.mortalDue(id,109));assertTrue(s.mortalDue(id,110));assertTrue(s.beginMortalFall(id,110,8));assertEquals(ThanatosState.MortalPhase.FALLING,s.mortalPhase(id));assertFalse(s.consumeMortalImpact(id,117));assertTrue(s.consumeMortalImpact(id,118));assertFalse(s.consumeMortalImpact(id,118));assertFalse(s.hasMortal(id));assertTrue(s.beginMortal(id,119,100));}
 @Test void clearCleansEveryLifecycle(){ThanatosState s=new ThanatosState();s.beginMortal(UUID.randomUUID(),0,100);s.beginUltimatum(UUID.randomUUID(),UUID.randomUUID(),0,15);s.clear();assertEquals(0,s.mortalCount());}
 @Test void ultimatumRequiresChargeLaunchFallAndMatchingGroundedWorld(){ThanatosState s=new ThanatosState();UUID p=UUID.randomUUID(),w=UUID.randomUUID();assertTrue(s.beginUltimatum(p,w,0,15));assertFalse(s.chargeDue(p,14));assertTrue(s.chargeDue(p,15));assertFalse(s.land(p,w,true));assertTrue(s.launch(p));assertTrue(s.beginFall(p));assertFalse(s.land(p,UUID.randomUUID(),true));assertFalse(s.land(p,w,false));assertTrue(s.land(p,w,true));assertFalse(s.hasUltimatum(p));}
 @Test void cancelledUltimatumCannotBecomeStaleLanding(){ThanatosState s=new ThanatosState();UUID p=UUID.randomUUID(),w=UUID.randomUUID();s.beginUltimatum(p,w,0,15);s.cancelUltimatum(p);assertFalse(s.land(p,w,true));}
}
