# HyunseoRPG 2026-08-18_090

## Release

- Build result: `Gradle clean build` PASS
- JAR: `2026-08-18_090_effect-command-shock-audit.jar`
- SHA-256: `DEA14784D23537FFAFF66BE89FFDD29B0D5E0CA2768032C807DFB6EDEDB3998E`
- Branch: `alchemy-error-detected`
- Commit: `860d6e55e5a8f91bd6569d8f2eff5f3dbeff8c7b`
- Live server deployment: not performed

## Changes

- Unified the active `/rpg effect` executor and tab completion around one administrator command contract.
- Added target player selection, optional duration/amplifier overrides, range validation, lookup errors, and permission handling.
- Preserved self-target compatibility for `/rpg effect apply <effect_id>` and `/rpg effect remove <effect_id>`.
- Added `/rpg effect debug [player]` active-effect and attribute-modifier diagnostics.
- Added optional `/rpg effect debug <source> <target> <base_damage>` combat preview output.
- Added `EffectService.DamageTrace`; production damage calculation and diagnostics now share outgoing/incoming/final stages.
- Shock pulses now use a relative 60-tick interval from application instead of global tick modulo timing.
- Shock movement lock is applied after pulse damage and capped at 20 ticks.
- Updated `alchemy/effects.yml` to `stun-duration-ticks: 20`; shock damage remains `1.0` and pulse interval remains `60`.
- Removed the obsolete debug-harness method from the active command implementation.
- Preserved all prior alchemy runtime fixes.

## Canonical commands

```text
/rpg effect apply <player> <effect_id> [duration_ticks] [amplifier]
/rpg effect remove <player> <effect_id>
/rpg effect clear <player>
/rpg effect debug [player]
/rpg effect debug <source> <target> <base_damage>
/rpg effect reload
/rpg effect list
```

Administrative subcommands require `hyunseorpg.admin`. `list` remains the player-facing status GUI.

## Production effect audit

| Effect | Intended runtime | Automated | Live |
|---|---|---:|---|
| Berserk | outgoing +10%, incoming +5% | PASS | LIVE_SERVER_VERIFICATION_REQUIRED |
| Corrosion | incoming +10% | PASS | LIVE_SERVER_VERIFICATION_REQUIRED |
| Frostbite | movement -15% attribute modifier | PASS | LIVE_SERVER_VERIFICATION_REQUIRED |
| Shock | 60-tick damage pulse, 20-tick lock after damage | PASS | LIVE_SERVER_VERIFICATION_REQUIRED |
| Vampirism | final damage 5% heal to attacker | PASS | LIVE_SERVER_VERIFICATION_REQUIRED |
| Vulnerability | incoming +20% | PASS | LIVE_SERVER_VERIFICATION_REQUIRED |
| Necrosis | max health -10% | PASS | LIVE PASS previously observed |
| Bleed | existing periodic damage behavior retained | PASS | LIVE_SERVER_VERIFICATION_REQUIRED |

## Combat pipeline audit

`Paper damage event -> ProductionEffectListener HIGHEST -> outgoing modifiers -> incoming modifiers -> event damage -> MONITOR final damage -> notifyDamage -> source handler onDamage` remains the active path. Projectile shooter attribution is preserved. `CombatService` uses Bukkit `target.damage(damage, attacker)`, so it enters the same event path. Other equipment listeners can adjust the event afterward; Vampirism reads `getFinalDamage()` at MONITOR.

## Verification status

- Automated tests: PASS.
- Gradle clean build: PASS; existing Paper API deprecation warnings only.
- Live gameplay: `LIVE_SERVER_VERIFICATION_REQUIRED`.
- Server JAR: not overwritten.
- Previous alchemy fixes: retained.
