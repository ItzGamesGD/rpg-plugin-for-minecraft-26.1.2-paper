package com.hyunseo.hyunseorpg.rpgtest.gateway;

public final class PrototypeBossHealth {
    private final double maximum;
    private double current;

    public PrototypeBossHealth(double maximum) {
        if (!Double.isFinite(maximum) || maximum <= 0) throw new IllegalArgumentException("maximum must be positive");
        this.maximum = maximum;
        this.current = maximum;
    }

    public double maximum() { return maximum; }
    public double current() { return current; }
    public boolean defeated() { return current == 0; }
    public boolean damage(double amount) {
        if (!Double.isFinite(amount) || amount <= 0 || defeated()) return false;
        current = Math.max(0, current - amount);
        return true;
    }
}
