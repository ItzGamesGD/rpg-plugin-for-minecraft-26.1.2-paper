# HyunseoRPG v0.1.0-dev.1 테스트 명령 가이드

이 문서는 `v0.1.0-dev.1` 라이브 검증용 테스트 명령만 정리한다.
기준 소스는 Release가 가리키는 `main` 커밋이며, 운영/개발 문서는 포함하지 않는다.

## 권한

- `/rpg give`: `hyunseorpg.item.give`
- `/rpgtest`: `hyunseorpg.admin.test`
- `/rpgmob`: `hyunseorpg.admin` 또는 각 mob 권한
- `/specialequipment give/unlock/soul`: `hyunseorpg.special.admin`

OP 환경에서는 일반적으로 테스트가 가장 편하다.

---

## 1. /rpg give

형식:

```
/rpg give <itemId> [amount]
```

수량은 1-2304 범위로 제한된다.

### 기본 무기

```
/rpg give basic_sword
/rpg give basic_bow
/rpg give basic_spear
/rpg give basic_axe
/rpg give basic_magic
```

### 원소/특수 장비 주요 ID

```
fire_sword
fire_bow
water_sword
poseidon_spear
wind_sword
wind_bow
earth_sword
earth_bow
earth_mace
ice_sword
ice_bow
magic_sword
magic_bow

burning_sword
flowing_water_sword
wind_cutting_sword
earth_special_sword
ice_special_sword
dark_energy_sword
burning_bow
wind_archers_bow
earth_heavy_bow
freezing_bow
dark_energy_bow

flame_axe
thunder_gods_axe
solaris
moonlit_afterglow
thanatos_mace
```

특수 장비는 일반 item factory 대신 전용 service를 통해 생성되므로 PDC/로어/전용 상태가 유지된다.

### 인챈트 북

모루 연결 테스트는 아래 책을 지급해서 바닐라 모루에 대상 장비와 함께 넣어 확인한다.

```
enchant_book_blade_throw
enchant_book_light_greatsword
enchant_book_laser_arrow
enchant_book_axe_heavy_strike
enchant_book_titans_wrath
enchant_book_skill_protection
enchant_book_rolling_landing
enchant_book_wind_arrow
enchant_book_fire_arrow_rain
enchant_book_crossbow_barrage
enchant_book_mining_bonus_drop
enchant_book_area_mining_pickaxe
enchant_book_auto_replant
enchant_book_auto_smelt
enchant_book_chain_logging
enchant_book_treasure_finder
enchant_book_multi_catch
enchant_book_elytra_launch
enchant_book_precision_flight
enchant_book_explosive_mace
```

현재 인챈트 ID 대응:

- `blade_chain` → `enchant_book_blade_throw`
- `light_greatsword` → `enchant_book_light_greatsword`
- `laser_arrow` → `enchant_book_laser_arrow`
- `axe_heavy_strike` → `enchant_book_axe_heavy_strike`
- `titans_wrath` → `enchant_book_titans_wrath`
- `skill_protection` → `enchant_book_skill_protection`
- `rolling_landing` → `enchant_book_rolling_landing`
- `wind_arrow` → `enchant_book_wind_arrow`
- `fire_arrow_rain` → `enchant_book_fire_arrow_rain`
- `crossbow_barrage` → `enchant_book_crossbow_barrage`
- `mining_bonus_drop` → `enchant_book_mining_bonus_drop`
- `area_excavation` → `enchant_book_area_mining_pickaxe`
- `auto_replant` → `enchant_book_auto_replant`
- `auto_smelt` → `enchant_book_auto_smelt`
- `chain_logging` → `enchant_book_chain_logging`
- `treasure_finder` → `enchant_book_treasure_finder`
- `multi_catch` → `enchant_book_multi_catch`
- `elytra_launch` → `enchant_book_elytra_launch`
- `precision_flight` → `enchant_book_precision_flight`
- `explosive_mace` → `enchant_book_explosive_mace`

### 재료/기타 테스트 ID

```
earth_fragment
fire_fragment
wind_fragment
water_fragment
ice_fragment
soul_stone

fire_heart
water_crystal
condensed_vortex
earth_heart
frozen_heart
dead_soul

basic_upgrade_stone
corrosion_essence
potion_vampire
potion_berserk
potion_corrosion
potion_frostbite
potion_shock
potion_bleed
potion_vulnerability
potion_necrosis
```

---

## 2. /rpgtest

현재 최상위 테스트 명령:

```
/rpgtest give <player> <itemId> [amount]
/rpgtest hand [player]
/rpgtest inspect [player]
/rpgtest maxhand [player]
/rpgtest maxgrowth [player]
/rpgtest gateway ...
/rpgtest basic-swarm ...
/rpgtest orbital-core ...
/rpgtest thousand-eyes ...
/rpgtest reload [target]
```

### 장비 테스트

```
/rpgtest give <player> <itemId> [amount]
```

현재 `/rpgtest give`는 일반 custom item과 `basic_sword/basic_bow/basic_spear/basic_axe/basic_magic`을 생성한다.

```
/rpgtest hand [player]
/rpgtest inspect [player]
```

주손 장비의 equipment metadata와 강화 상태를 확인한다.

```
/rpgtest maxhand [player]
/rpgtest maxgrowth [player]
```

주손 장비의 강화 레벨을 해당 장비의 최대치로 강제한다.

### Gateway 프로토타입

```
/rpgtest gateway boss [debug]
/rpgtest gateway cycle [debug]
/rpgtest gateway payload <type> [debug]
/rpgtest gateway reflection
/rpgtest gateway placement [debug]
/rpgtest gateway pairing
/rpgtest gateway weapon-ai <mace|spear|axe|hoe>
/rpgtest gateway cancel
```

### Basic swarm

```
/rpgtest basic-swarm random
/rpgtest basic-swarm pattern <pattern> [count]
```

`pattern` 이름은 탭 완성에 표시되는 `BasicWeaponPattern` 값 기준이다.

### Orbital core

```
/rpgtest orbital-core [1|2|3|4]
/rpgtest orbital-core cancel
```

### Thousand Eyes

```
/rpgtest thousand-eyes spawn [seed]
/rpgtest thousand-eyes remove
/rpgtest thousand-eyes central-laser
/rpgtest thousand-eyes gateway-burst
/rpgtest thousand-eyes scatter-lasers
/rpgtest thousand-eyes path-dash
```

---

## 3. 몹 테스트: /rpgmob

주의: 현재 소스에는 `/rpgtest spawnmob` 명령이 없다.
현재 대응 기능은 `/rpgmob spawn`과 `/rpgmob spawncustom`으로 분리되어 있다.

### 바닐라 타입을 RPG 몹으로 생성

```
/rpgmob spawn <mobType> <rpg|elite|boss> [level]
```

예:

```
/rpgmob spawn ZOMBIE rpg 10
/rpgmob spawn SKELETON elite 20
/rpgmob spawn RAVAGER boss 50
```

### YAML 커스텀 몹 생성

```
/rpgmob spawncustom <mobId> [level]
```

탭 완성으로 현재 등록된 mob ID를 확인할 수 있다.

현재 `mobs.yml`에서 확인되는 주요 ID 예:

```
golden_bulwark
mire_shaman
shield_raider
crossbow_raider
charger_raider
banner_raider
spike_evoker
ravager_rider
frost_blaze
explosive_breeze
riptide_drowned
mining_giant
lava_cube
stone_armored_zombie
fake_explosion_creeper
swapping_witch
charging_zombie
rapid_shooter
splitting_creeper
forest_wolf
zombie_basic
elite_zombie
skeleton_guard
skeleton_king
```

### 몹 상태 확인

```
/rpgmob info
/rpgmob debug
/rpgmob debug on
/rpgmob debug off
```

### 스케일링

```
/rpgmob scaling get
/rpgmob scaling sample
/rpgmob scaling set <key> <value>
```

### 테스트 몹 정리

```
/rpgmob clear
```

---

## 4. 특수 장비 전용 테스트

```
/specialequipment list
/specialequipment info <equipmentId>
/specialequipment give <player> <equipmentId> [amount]
/specialequipment unlock <player> [lock]
/specialequipment soul <get|add> <player> <mobId> [amount]
/specialequipment debug
```

`unlock`은 테스트용 요구조건 bypass를 켜며, 마지막 인수로 `lock`을 주면 다시 끈다.

---

## 권장 라이브 테스트 순서

1. `/rpg give`로 인챈트 북과 대상 바닐라 장비 지급
2. 바닐라 모루에서 합성 및 로어/인챈트 적용 확인
3. 적용 장비의 실제 트리거 입력 확인
4. `/rpgmob spawncustom`으로 대상 몹 강제 생성
5. 원소/특수 장비의 공격, 스킬, 드롭, 진행도 확인
6. `/rpgtest hand` 또는 `/rpgtest inspect`로 PDC/강화 상태 확인
7. 자연 야생에서는 같은 기능이 정상적으로 도달 가능한지 별도 확인

이 문서는 테스트 편의를 위한 명령 참조이며, 야생 획득 가능 여부를 의미하지 않는다.
