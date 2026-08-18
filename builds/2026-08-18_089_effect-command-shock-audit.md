# HyunseoRPG 2026-08-18_089

## Release

- Build result: `Gradle clean build` PASS
- JAR: `2026-08-18_089_effect-command-shock-audit.jar`
- SHA-256: `427745B5344B48DC40F8A4BA4814CBD7CE4BC9AAEFEBC753836D3FA76EE199DD`
- Branch: `alchemy-error-detected`
- Live server deployment: not performed

## Changes

- Unified the active `/rpg effect` executor with the contract exposed by tab completion.
- Added administrator target selection, optional duration and amplifier overrides, validation, player lookup errors, and permission handling.
- Kept the existing self-target shorthand `/rpg effect apply <effect_id>` and `/rpg effect remove <effect_id>` for compatibility.
- Added `/rpg effect debug [player]` output for active effect instances and owned attribute modifiers.
- Added optional combat preview diagnostics:
  `/rpg effect debug <source> <target> <base_damage>`
  This reports base, outgoing, incoming, final, and estimated vampirism healing values without mutating combat state.
- Added `EffectService.DamageTrace` so production damage calculation and diagnostics use the same outgoing/incoming pipeline.
- Shock pulses now use an interval relative to effect application instead of global server tick modulo timing.
- Shock movement lock is capped at 20 ticks and is created only after the pulse damage call.
- Updated `alchemy/effects.yml` shock `stun-duration-ticks` to `20`; pulse interval remains `60` ticks and damage remains `1.0`.
- Preserved prior splash, lingering, echo, slime, sculk, fireball, and potion runtime changes.

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

`/rpg effect list` remains the player-facing status GUI. Administrative subcommands require `hyunseorpg.admin`.

## Production effect audit

| Effect | Intended runtime | Automated | Live |
|---|---|---:|---|
| Berserk | outgoing +10%, incoming +5% | PASS | LIVE_SERVER_VERIFICATION_REQUIRED |
| Corrosion | incoming +10% | PASS | LIVE_SERVER_VERIFICATION_REQUIRED |
| Frostbite | movement -15% attribute modifier | PASS | LIVE_SERVER_VERIFICATION_REQUIRED |
| Shock | 60 tick pulse, 20 tick movement lock after damage | PASS | LIVE_SERVER_VERIFICATION_REQUIRED |
| Vampirism | final damage 5% heal to attacker | PASS | LIVE_SERVER_VERIFICATION_REQUIRED |
| Vulnerability | incoming +20% | PASS | LIVE_SERVER_VERIFICATION_REQUIRED |
| Necrosis | max health -10% | PASS | LIVE PASS previously observed |
| Bleed | existing periodic damage behavior retained | PASS | LIVE_SERVER_VERIFICATION_REQUIRED |

## Combat pipeline audit

The active path remains:

`Paper damage event -> ProductionEffectListener HIGHEST -> EffectService outgoing modifiers -> incoming modifiers -> event damage -> MONITOR final damage -> notifyDamage -> source handler onDamage`.

Projectile shooter attribution is preserved. The repository contains other HIGHEST damage listeners for equipment and special equipment; the production effect listener is registered before those listeners, so later equipment adjustments are included in `getFinalDamage()` before vampirism notification. Custom `CombatService` damage still enters Bukkit damage events through `target.damage(damage, attacker)`.

## Verification status

- Automated tests: PASS, including command completion and shock configuration checks.
- Build: PASS with existing Paper API deprecation warnings only.
- Live gameplay: `LIVE_SERVER_VERIFICATION_REQUIRED`
- Server JAR: not overwritten.
- Existing alchemy runtime fixes: retained.
