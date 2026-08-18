# HyunseoRPG Alchemy Runtime Fixes 087

## Status

- Implementation status: COMPLETE
- Automated verification: PASSED (`Gradle clean build`, all automated tests)
- Live gameplay verification: PENDING
- Balance verification: PENDING
- Server plugin folder: not overwritten

## Changes

- Slime catalyst is enabled and configured with `SLIME_BALL` plus `delivery: SPLASH`.
- Slime-converted potions are actual `SPLASH_POTION` items and use bounded real thrown-potion bounce entities.
- Each slime splash applies the normal custom effect, then creates the next bounded bounce until `max-propagation-count` is reached.
- Bounce projectiles are tagged with the execution PDC and cleaned up on timeout, player/world lifecycle events, or completion.
- Sculk catalyst material corrected from `SCULK` to `SCULK_CATALYST`.
- Custom fireball direct entity impact damage is cancelled using the execution PDC. Custom area effect delivery remains active.
- Echo catalyst material is `ECHO_SHARD` (메아리 조각); its existing immediate plus delayed execution remains unchanged.

## Modified files

- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/BoundedSpecialCatalystExecutionService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/potion/PaperPotionUseListener.java`
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
- `src/main/resources/alchemy/catalysts.yml`
- Alchemy contract tests updated for enabled slime and `SCULK_CATALYST`.

## Verification

- `./gradlew.bat clean build`: PASSED
- Automated test suite: PASSED
- Java compiler emitted existing Paper API deprecation warnings only.

## Release

- JAR: `2026-08-18_087_alchemy-slime-splash-fixes.jar`
- SHA-256: `97C253CB4EB03A51F01DDA6A47C8DC2F0CDFF17C8F527FC761A9FB51DD3EF927`

## Live test checklist

- Apply slime catalyst to a normal drink potion and confirm the output becomes a throwable potion.
- Throw the slime potion and confirm the first effect, visible bounce projectile, bounded repeated impacts, and no infinite loop.
- Confirm the sculk catalyst item is recognized when using the actual Sculk Catalyst block.
- Use the custom fireball and confirm area effect delivery without direct impact damage, block damage, or ignition.
- Use an Echo Shard and confirm the immediate and delayed effect applications.
- Confirm logout, death, world change, chunk unload, and reload do not leave bounce projectiles or runtime effects behind.

