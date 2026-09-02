package com.hyunseo.hyunseorpg.special.thanatos;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.random.RandomGenerator;
import static org.junit.jupiter.api.Assertions.*;

class ThanatosTargetingTest {
 @Test void candidatesAreConstrainedToRangeAndForwardCone(){var input=List.of(new ThanatosTargeting.Candidate<>("front",0,0,5),new ThanatosTargeting.Candidate<>("behind",0,0,-2),new ThanatosTargeting.Candidate<>("far",0,0,11),new ThanatosTargeting.Candidate<>("side",5,0,0));assertEquals(List.of("front"),ThanatosTargeting.forward(input,0,0,1,10,.5));}
 @Test void randomSelectionCanOnlyReturnValidCandidate(){List<String> valid=List.of("a","b");RandomGenerator random=new Random(4);for(int i=0;i<100;i++)assertTrue(valid.contains(ThanatosTargeting.random(valid,random)));assertNull(ThanatosTargeting.random(List.of(),random));}
}
