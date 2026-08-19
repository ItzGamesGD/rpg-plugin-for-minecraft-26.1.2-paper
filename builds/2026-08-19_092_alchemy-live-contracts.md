# HyunseoRPG Alchemy Live Contract Follow-up

- Base: `alchemy-error-detected`
- Base HEAD: `dc407bd8c58d69eeaddaa58e30e3c6df2c99f079`
- Work branch: `fix/alchemy-live-contracts`
- Status: `LIVE_SERVER_VERIFICATION_REQUIRED`
- Current HEAD: `1ce9cdd3aeda972f6e57b71f8e45cb3a25bf55dd`
- Automated test inventory: 45 test classes / 158 `@Test` methods (inventory from source; execution not completed).
- Gradle clean build/test: **NOT EXECUTED**. The GitHub connector has no build runner, and an unauthenticated local clone was rejected; no pass/fail result is being claimed.
- JAR: **NOT BUILT**; SHA-256: **N/A**.
- Server JAR: do not overwrite the existing JAR automatically.

## Code-contract changes in this branch

1. Slime no longer uses the normal gameplay wall-clock timer to call `finish()`. A long safety boundary converts an active slime runtime to `FORCE_FINAL_SPLASH` at the last known projectile location instead of silently deleting the projectile.
2. Slime phase contract is `INITIAL_SPLASH -> BOUNCE -> ... -> FINAL_PROJECTILE -> FINAL_SPLASH -> FINISHED`. The maximum count changes the next collision into the final splash; it is not a deletion condition.
3. Shock root uses generation-safe explicit unlock scheduling. Root is active only between the pulse-specific start/end ticks and preserves yaw/pitch.
4. `sculk` and `wind_charge` now declare `delivery: SPLASH` and are routed through the unified special splash dispatcher. Their runtime origin is the actual splash impact; wind direction comes from projectile velocity.
5. Finish reasons are recorded for runtime cleanup and safety termination. `SAFETY_TIMEOUT` during normal slime bouncing is a failed live-test result.

## Consolidated live-test checklist

### A. Baseline potion and status-effect regression
- [ ] Issue canonical potion and verify potion PDC.
- [ ] Drink one potion with no active effect; verify one effect applies.
- [ ] With one active effect already present, drink a second potion; verify the active count increases to two.
- [ ] Verify `/rpg effect list` and `/rpg effect debug <player>`.
- [ ] Verify direct `test_speed` application and removal.
- [ ] Verify potion use does not self-apply to the source when the target is another entity.
- [ ] Verify DRINK, SPLASH, and LINGERING delivery item materials and PDC values.

### B. Shock pulse/root
- [ ] Apply shock and verify movement remains possible at T+0.
- [ ] At T+60 verify damage occurs and root becomes active.
- [ ] At T+60-T+79 verify XYZ movement is blocked.
- [ ] During root verify yaw and pitch rotation remain possible.
- [ ] At T+80 verify root is explicitly inactive even without another PlayerMoveEvent.
- [ ] At T+80-T+119 verify free movement.
- [ ] At T+120 verify the next damage pulse and root activation.
- [ ] At T+140 verify root is inactive.
- [ ] Remove/expire shock and verify root cleanup.
- [ ] Verify debug values: `shockNextPulse`, `shockRootActive`, `shockRootStart`, `shockRootEnd`, `currentTick`.

### C. Slime bounce
- [ ] Throw the transformed SPLASH_POTION and verify the initial splash is intercepted.
- [ ] Verify normal bounce sequence remains visible and functional.
- [ ] Verify the potion does not disappear during ordinary flight before a collision.
- [ ] Verify the old `(lifetimeTicks * maxCount) + 20` wall-clock finish path is absent from normal gameplay behavior.
- [ ] Verify bounce counts 1 through N.
- [ ] Verify the Nth bounce projectile continues flying.
- [ ] On the next collision verify one final splash and one effect application to affected entities.
- [ ] Verify no duplicate final splash/effect.
- [ ] If a safety boundary is reached, verify `FORCE_FINAL_SPLASH` at the last known location rather than silent deletion.
- [ ] Verify finish reason is not `SAFETY_TIMEOUT` during a normal successful bounce.
- [ ] Verify admin clear, chunk unload, world unload, quit/death cleanup only remove explicitly cancelled executions.

### D. Echo
- [ ] Throw echo SPLASH_POTION.
- [ ] Verify exactly the affected entities receive the intended repeated effect.
- [ ] Verify the source is excluded when appropriate.
- [ ] Verify no regression after unified dispatch.

### E. Sculk
- [ ] Transform using SCULK_CATALYST and verify output material is SPLASH_POTION.
- [ ] Verify ordinary SCULK is not accepted as the catalyst.
- [ ] Verify `/rpg alchemy inspect catalysts` reports `loaded-material=SCULK_CATALYST`.
- [ ] Throw the potion and verify the first cloud is at the actual splash impact location, not the player origin.
- [ ] Verify dark teal DUST and visual radius 1.25.
- [ ] Verify propagation starts after 10 ticks.
- [ ] Verify bounded propagation and attenuation.
- [ ] Verify cloud cleanup and no unintended vanilla AreaEffectCloud application.

### F. Wind charge
- [ ] Transform using WIND_CHARGE and verify output material is SPLASH_POTION.
- [ ] Throw the potion and verify runtime begins at splash impact.
- [ ] Verify direction follows the projectile velocity at impact.
- [ ] Turn the player's camera after throwing; verify the direction does not change.
- [ ] Verify target hit, max distance, and lifetime cleanup.
- [ ] Verify no drink-path execution remains for wind charge.

### G. Existing catalyst and lifecycle regression
- [ ] Echo, fireball, vanilla catalyst, potion PDC, and effect command regression.
- [ ] Reload configs and verify external YAML remains authoritative.
- [ ] Verify plugin disable, world unload, chunk unload, player quit/death, and admin clear cleanup.
- [ ] Verify no existing server JAR was overwritten.

## Result policy

- Automated tests must pass.
- A successful build does not count as live verification.
- Keep `LIVE_SERVER_VERIFICATION_REQUIRED` until all applicable items above are checked on the actual server.
