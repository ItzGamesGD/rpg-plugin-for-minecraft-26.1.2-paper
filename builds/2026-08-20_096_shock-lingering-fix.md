# HyunseoRPG Build Report

- Build result: SUCCESS
- Source base: GitHub `alchemy-error-detected` commit `21002fe`
- Local fix scope: shock interval and dragon-breath lingering delivery
- JAR: `2026-08-20_096_shock-lingering-fix.jar`
- SHA-256: `0A900C40F49B47FEBD9EFFF8F5F7E76773B803B8E9846D362F825A286584B1B2`
- Automated tests: 162 passed, 0 failed, 0 skipped
- Build command: `gradlew.bat --no-daemon clean test jar`

## Changes

- Shock damage interval changed from 80 ticks to 320 ticks.
- Existing external `alchemy/effects.yml` values of exactly 80 ticks are migrated to 320 ticks.
- Shock root duration remains 20 ticks, so movement is released well before the next pulse.
- Dragon-breath lingering clouds now use a scheduled custom-effect scan instead of relying only on native potion-effect application events.
- Lingering cloud cleanup cancels both cleanup and application tasks.
- Glowstone amplifier levels were not changed; level behavior remains unverified.

## Live Verification

- Live Paper server verification: PENDING
- Server `plugins` folder: not overwritten
- Balance verification: PENDING

## Required Tests

1. Apply shock and verify the first damage occurs after 320 ticks (16 seconds).
2. Verify shock root lasts only 20 ticks after damage and movement is available afterward.
3. Verify the next shock damage occurs 320 ticks after the prior pulse.
4. Apply dragon breath to a valid custom potion and confirm a lingering cloud appears.
5. Stand an eligible target in the cloud and confirm the custom effect applies repeatedly while the cloud remains.
6. Leave the cloud, let it expire, and confirm no further effect application occurs.
7. Reload, logout, world-change, death, and chunk-unload cleanup for lingering runtime.
8. Confirm glowstone behavior remains unchanged and record as unverified if amplifier levels are not implemented.
