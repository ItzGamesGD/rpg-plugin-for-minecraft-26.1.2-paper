package com.hyunseo.hyunseorpg.alchemy;

public record EffectComponentDefinition(String id, boolean enabled, int priority,
                                        String attribute, String operation, double amount) { }
