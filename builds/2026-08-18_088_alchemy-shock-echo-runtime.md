# HyunseoRPG Alchemy Shock and Echo Runtime 088

## Status

- Implementation status: COMPLETE
- Automated verification: PASSED (`Gradle clean build`)
- Automated tests: 153 passed, 0 failures, 0 errors
- Live server verification: `LIVE_SERVER_VERIFICATION_REQUIRED`
- Server plugin folder: not overwritten

## Changes

### Shock

- `effect_shock` pulse interval changed from 20 to 60 ticks.
- Damage remains configurable at `damage: 1.0`.
- Each pulse applies a configurable 40-tick movement lock.
- The movement lock anchors the entity, clears velocity every tick, prevents jump/movement escape, and clears on effect removal, death, quit, world change, or plugin shutdown.
- The lock is implemented as the shared `EffectMovementLockService`, registered with the plugin lifecycle.

### Echo catalyst

- `echo_shard` keeps `ECHO_SHARD` as its material and now declares `delivery: SPLASH`.
- Catalyst conversion changes the result to `Material.SPLASH_POTION` through the existing transformation path.
- Echo effects are no longer applied on consumption or automatically to the thrower.
- `PotionSplashEvent` captures only the actual affected target UUIDs.
- Each captured target receives one immediate effect and one delayed effect using the same target UUID.
- The delayed task does not search nearby entities again and cleans itself up if the target is gone or the runtime is cancelled.

### Existing runtime fixes retained

- Slime splash/bounce runtime and bounds
- `SCULK_CATALYST` material mapping
- Custom fireball direct-damage cancellation
- Existing potion PDC recovery, splash/lingering routing, atomic GUI transaction, and lifecycle cleanup

## Modified files

- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectMovementLockService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/ShockEffectHandler.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/BoundedSpecialCatalystExecutionService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/potion/PaperPotionUseListener.java`
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
- `src/main/resources/alchemy/effects.yml`
- `src/main/resources/alchemy/catalysts.yml`
- Alchemy contract tests for shock and echo delivery

## Release

- JAR: `2026-08-18_088_alchemy-shock-echo-runtime.jar`
- SHA-256: `2D81ABB7768D78289EAFE0974236A7CF4C9E49457F7DD55171CF191170334139`

## Live test checklist

- Use `potion_shock` and confirm damage plus a 40-tick complete movement lock every 60 ticks.
- Confirm the target can move during the gap between pulses and is not permanently rooted for all 1200 ticks.
- Confirm shock removal, death, logout, world change, and reload leave no movement lock or velocity residue.
- Apply `echo_shard` and confirm the output is a splash potion.
- Throw it at one target and confirm exactly two effects on that target: immediate and after `delay-ticks`.
- Confirm the thrower and nearby non-affected entities receive no echo effect.
- Remove or unload the target before the delay and confirm no error or second application occurs.
- Recheck gunpowder, dragon breath, sculk, slime, fireball, redstone, glowstone, and inversion regressions.

