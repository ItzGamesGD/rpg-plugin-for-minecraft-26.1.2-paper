package com.hyunseo.hyunseorpg.special.thanatos;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

public final class ThanatosTargeting {
    private ThanatosTargeting() { }
    public record Candidate<T>(T value, double x, double y, double z) { }
    public static <T> List<T> forward(List<Candidate<T>> candidates, double dx, double dy, double dz,
                                      double range, double minimumCosine) {
        double dl = Math.sqrt(dx*dx+dy*dy+dz*dz); if (dl == 0) return List.of();
        List<T> result = new ArrayList<>(); double range2 = range * range;
        for (Candidate<T> c : candidates) { double l2=c.x*c.x+c.y*c.y+c.z*c.z;
            if (l2 == 0 || l2 > range2) continue;
            if ((c.x*dx+c.y*dy+c.z*dz)/(Math.sqrt(l2)*dl) >= minimumCosine) result.add(c.value);
        }
        return List.copyOf(result);
    }
    public static <T> T random(List<T> valid, RandomGenerator random) { return valid.isEmpty() ? null : valid.get(random.nextInt(valid.size())); }
}
