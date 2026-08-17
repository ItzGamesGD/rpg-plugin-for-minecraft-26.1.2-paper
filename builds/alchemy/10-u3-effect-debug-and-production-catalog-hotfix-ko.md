# 양조 U3 추가 핫픽스 보고서

기준일: 2026-08-15  
범위: Custom Effect Debug Command 및 1차 양조 생산 카탈로그 복구

## 1. 구현 내용

### 관리자 디버그 명령

다음 명령을 기존 `/rpg` dispatcher에 연결했다.

```text
/rpg effect apply <player> <effect_id> <duration_ticks> <amplifier>
/rpg effect remove <player> <effect_id>
/rpg effect clear <player>
/rpg effect list
/rpg effect reload
```

- `hyunseorpg.admin` 권한 필요
- 온라인 대상 플레이어만 허용
- `CustomEffectRegistry`의 현재 활성 snapshot에 있는 ID만 허용
- duration은 1~72000 tick, amplifier는 0~10 범위만 허용
- 입력값은 설정이나 아이템에 저장하지 않음
- 적용은 기존 `EffectService`의 공통 적용 경로를 사용
- Potion Registry, Potion PDC, recipe, catalyst를 우회하는 테스트 harness
- tab completion에 대상 플레이어, 활성 effect ID, duration, amplifier 추가
- 알 수 없는 ID·비활성 ID·범위 밖 입력은 fail-closed

### EffectService 연결

`applyDebug()`는 임시 duration/amplifier를 복사한 정의에만 적용하고 원본 설정은 변경하지 않는다. 실제 적용·충돌·Attribute 부여·수명주기는 `applyInternal()`을 공유한다.

`handler-id`가 있는 활성 효과는 `HandlerRegistry`에 `CombatEffectHandler`가 등록되어 있지 않으면 적용하지 않는다. 등록된 handler가 있을 때만 onApply/onTick/onRemove 수명주기를 호출한다.

## 2. 생산 콘텐츠 카탈로그 점검

단순 Registry 누락으로 생산 콘텐츠를 `FUTURE_DISABLED` 처리하지 않았다.

### 복구한 기본 Item ID

- `corrosion_essence`
- `potion_vampire`
- `potion_berserk`
- `potion_corrosion`
- `potion_frostbite`
- `potion_shock`
- `potion_bleed`
- `potion_vulnerability`
- `potion_necrosis`

### 설정 상태

`alchemy/effects.yml`, `alchemy/potions.yml`, `alchemy/recipes.yml`에 1차 생산 ID를 명시했다. 현재 전투 handler 구현과 수치가 실제 소스에서 확정되지 않은 항목은 다음 상태로 남겼다.

- handler 연결: `IMPLEMENT_IN_UNIT_3_HANDLER`
- 지속시간·강도·제작 비용: `BALANCE_PENDING`

이는 미래 콘텐츠로 폐기한 것이 아니다. 다만 검증되지 않은 전투 효과를 활성화하지 않아 운영 환경에 노출하지 않는다. 현재 활성 효과가 아닌 생산 effect ID는 디버그 명령에서 거부되는 것이 정상이다.

`corrosion_essence`는 `items.yml`에 등록했으며, 레시피에서는 `vanilla:SLIME_BALL`처럼 명시적인 바닐라 Material 매핑만 사용한다. 실제 농사 Registry에 없는 `tomato_concentrate`와 별도 획득 경로가 없는 `chili_powder`는 production item에서 제거하고, 기존 등급 가공품(`processed_garlic_concentrate_normal`, `processed_chili_extract_normal`)을 임시 호환 재료로 연결했다. 수량과 최종 재료 선택은 `BALANCE_PENDING`이다.

## 3. 레시피 Registry 정합성 수정

`YamlAlchemyRecipeRegistry`가 레시피 ID 자체를 결과 물약 ID로 사용하던 문제를 수정하여 `result-potion-id`를 authoritative 값으로 읽는다.

또한 `vanilla:` 재료는 `Material.matchMaterial()`로 실제 Bukkit Material인지 확인한다. 일반 재료는 `RPGItemService`/ItemRegistry를 통해서만 유효성을 확인한다.

## 4. 설계·코드 이중 검증

설계 검증:

- debug command는 production potion 경로와 분리된 EffectService harness다.
- 테스트 입력값은 production balance에 저장되지 않는다.
- 비활성·미등록 effect는 적용하지 않는다.
- 현재 생산 콘텐츠 ID는 카탈로그에 복구하되 미확정 전투 수치를 임의 활성화하지 않는다.
- 바닐라 재료는 명시적인 Material 매핑만 허용한다.

코드 검증:

- `RPGGiveCommand` → `EffectService.applyDebug()` → `applyInternal()` 호출 경로 확인
- 적용·제거·전체 제거·reload·자동완성 경로 확인
- `EffectService`가 설정 snapshot 외 값을 영구 저장하지 않음 확인
- `YamlAlchemyRecipeRegistry`의 결과 ID 및 바닐라 재료 검증 확인
- 필수 Item ID와 생산 effect/recipe/potion ID 기본 리소스 존재 확인

## 5. 테스트

- 전체 테스트: `134 tests / 0 failures / 0 skipped`
- 생산 Item ID 11개 기본 리소스 검사: PASS
- 생산 effect 카탈로그 및 `IMPLEMENT_IN_UNIT_3_HANDLER`/`BALANCE_PENDING` 상태 검사: PASS
- 양조 레시피 `result-potion-id` 검사: PASS
- 명시적 `vanilla:` 재료 형식 검사: PASS
- Gradle JAR 빌드: PASS

## 6. 아직 실서버에서 확인할 항목

- OP/관리자 권한으로 `/rpg effect apply <player> effect_test_speed 1200 1` 실행 후 이동속도 적용
- `/rpg effect remove` 및 `/rpg effect clear` 후 Attribute 복원
- 종료 시각 만료 후 자동 제거
- 잘못된 effect ID, 비활성 생산 effect ID, duration/amplifier 범위 밖 입력 차단
- 기존 외부 `effects.yml`, `items.yml`, `alchemy/*.yml`이 기본 리소스를 덮어쓰는 경우 `/rpg reload` 결과
- 실제 생산 effect handler 구현 후 각 production effect ID를 동일 명령으로 직접 검증

## 7. 산출물

JAR: `builds/alchemy/HyunseoRPG-0.1.0-SNAPSHOT-alchemy-u3-effect-debug-hotfix.jar`  
SHA-256: `F02FFD9315528B5299C22ECEA4B86A0157EE7234DB4F1E4B87D3B8E2A726D7D5`

`libs` 디렉터리는 수정하지 않았다.
