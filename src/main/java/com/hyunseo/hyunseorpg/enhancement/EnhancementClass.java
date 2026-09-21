package com.hyunseo.hyunseorpg.enhancement;

/** Canonical vanilla-anvil enhancement capability boundary. */
public enum EnhancementClass {
    VANILLA(true, "enhancement.caps.vanilla", 30),
    ELEMENTAL(true, "enhancement.caps.elemental", 40),
    SPECIAL(false, "", 0),
    ENDGAME(false, "", 0),
    UNSUPPORTED(false, "", 0);

    private final boolean enhanceable;
    private final String capConfigPath;
    private final int defaultCap;

    EnhancementClass(boolean enhanceable, String capConfigPath, int defaultCap) {
        this.enhanceable = enhanceable;
        this.capConfigPath = capConfigPath;
        this.defaultCap = defaultCap;
    }

    public boolean enhanceable() { return enhanceable; }
    public String capConfigPath() { return capConfigPath; }
    public int defaultCap() { return defaultCap; }
}
