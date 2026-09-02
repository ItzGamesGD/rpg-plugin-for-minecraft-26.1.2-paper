package com.hyunseo.hyunseorpg.special.thanatos;

import com.hyunseo.hyunseorpg.core.config.ConfigService;

public record ThanatosConfig(int mortalDelayTicks, double mortalDamage, int mortalModel,
 int oppressionCooldownTicks, double oppressionRadius, int oppressionDurationTicks, double oppressionDamage,
 int sentenceCooldownTicks, double sentenceRange, double sentenceCosine, int sentenceFallTicks,
 double groundRadius, int groundDurationTicks, int witherDurationTicks, int witherAmplifier,
 int ultimatumCooldownTicks, int chargeTicks, double launchVelocity, double impactRadius, double impactDamage) {
 public static ThanatosConfig from(ConfigService c) { String p="special-equipment.items.thanatos_mace.abilities.";
  return new ThanatosConfig(c.getSpecialEquipmentInt(p+"mortal.delay-ticks",100),c.getSpecialEquipmentDouble(p+"mortal.damage",6),c.getSpecialEquipmentInt(p+"mortal.sword-custom-model-data",23601),
   c.getSpecialEquipmentInt(p+"deaths-oppression.cooldown-ticks",200),c.getSpecialEquipmentDouble(p+"deaths-oppression.radius",5),c.getSpecialEquipmentInt(p+"deaths-oppression.duration-ticks",40),c.getSpecialEquipmentDouble(p+"deaths-oppression.damage",4),
   c.getSpecialEquipmentInt(p+"death-sentence.cooldown-ticks",300),c.getSpecialEquipmentDouble(p+"death-sentence.target-range",24),c.getSpecialEquipmentDouble(p+"death-sentence.minimum-facing-cosine",.35),c.getSpecialEquipmentInt(p+"death-sentence.telegraph-ticks",24),
   c.getSpecialEquipmentDouble(p+"death-sentence.ground-radius",5),c.getSpecialEquipmentInt(p+"death-sentence.ground-duration-ticks",120),c.getSpecialEquipmentInt(p+"death-sentence.wither-duration-ticks",100),c.getSpecialEquipmentInt(p+"death-sentence.wither-amplifier",1),
   c.getSpecialEquipmentInt(p+"ultimatum.cooldown-ticks",600),c.getSpecialEquipmentInt(p+"ultimatum.charge-ticks",15),c.getSpecialEquipmentDouble(p+"ultimatum.launch-velocity",1.55),c.getSpecialEquipmentDouble(p+"ultimatum.impact-radius",10),c.getSpecialEquipmentDouble(p+"ultimatum.damage",12)); }
}
