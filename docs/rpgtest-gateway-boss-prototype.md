# Gateway boss RPGTEST prototype

This is an isolated test harness, not a production boss registration.

## Commands

All commands require an operator/player with `hyunseorpg.admin.test`.

* `/rpgtest gateway placement [debug]` — create seven snapshot-aimed launch gateways.
* `/rpgtest gateway pairing` — show the seven `Gx -> Rx` pairs and P0 debug marks.
* `/rpgtest gateway reflection` — run the Arrow reflection round-trip test.
* `/rpgtest gateway payload <payload> [debug]` — force one payload.
* `/rpgtest gateway cycle [debug]` — schedule an unweighted random mixed cycle.
* `/rpgtest gateway cancel` — remove every prototype object owned by the player.
* `/rpgtest basic-swarm random` — run an unweighted random weapon swarm.
* `/rpgtest basic-swarm pattern <pattern> [count]` — force one implemented weapon behavior.

Payload names are `ARROW`, `TRIDENT`, `BLAZE_SMALL_FIREBALL`, `GHAST_FIREBALL`,
`WITHER_SKULL`, `SHULKER_BULLET`, `WIND_CHARGE`, `END_CRYSTAL_BOMB`, `SONIC_BOOM`,
`DRAGON_BREATH`, `FLAME_STREAM`, and `BEAM`.

Basic pattern names are `MACE_MELEE`, `MACE_DROP`, `SPEAR_MELEE`, `SPEAR_LUNGE`,
`TRIDENT_THROWER`, `AXE_MELEE`, `HOE_MELEE`, and `SHIELD_ORBIT`.

## Prototype tuning

The code-level tuning constants are: gateway count `7`, gateway total cap `16`, basic
entity total cap `14`, projectile speed multiplier `0.90`, and per-gateway recovery `8`
ticks. Local caps are Sonic Boom `2`, Beam `2`, Dragon Breath `2`, Flame Stream `3`,
and End Crystal Bomb `3`.

Pattern type counts have no artificial cap and neither selector uses weights or threat
costs. A gateway is a generic reusable launcher; it is not bound to any payload type.

## Specification gaps

No authoritative Pattern 1 or Pattern 2 specification exists in this repository.
They remain deliberately unimplemented as `SPEC_GAP_BASIC_PATTERN_1` and
`SPEC_GAP_BASIC_PATTERN_2`. No behavior was inferred for them.

## Verification boundary

Statically covered behavior includes global/local cap enforcement, same-gateway busy
exclusion and reuse, source gateway preservation, explicit return phases, rejecting
reflection for non-reflectable payloads, and End Crystal fuse state.

Live Paper testing is still required for display scale/readability, portal spacing in
real arenas, attack telegraphs, reflection interaction feel, projectile relative speed,
mixed sustained/projectile readability, dummy-hit feedback, disconnect/world-change/
disable cleanup, and non-destructive ground impacts.
