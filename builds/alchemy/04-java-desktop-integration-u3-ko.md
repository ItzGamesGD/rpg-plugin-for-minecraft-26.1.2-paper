# 양조 Java/Desktop Integration U3 보고서

기준일: 2026-08-15  
단위: U3 Core/combat/skill/enchantment bridge  
상태: 구조 구현 및 자동 검증 완료, 전투 수치·실서버 검증 대기

## 1. 이번 단위의 범위

U3는 현재 HyunseoRPG의 실제 전투·스킬 입력 경계에 양조 효과 bridge를 연결했다. 활성 전투 효과 정의나 임의 피해 배율은 추가하지 않았다. 현재 `alchemy/effects.yml`에는 테스트 이동속도 효과만 활성화되어 있으므로 피해·치유 수식은 identity operation으로 남겼다.

## 2. 설계안 대조

| 설계 계약 | 판정 | 근거 |
|---|---|---|
| Core effect ID는 하나의 정규 이름을 사용한다 | 충족 | `CoreEffectIds`에 silence/root/stun/vulnerability/vampirism/berserk/healing/hardening/vitality ID를 정규화해 등록했다. |
| Skill/Enchant 입력을 실제 입력 라우터에서 차단한다 | 부분 충족 | `SkillInputListener`의 실제 `processInputResult` 경계에 silence/stun 게이트를 연결했다. 직접 `SkillService` 호출과 아직 확인되지 않은 관리자·특수 입력 경로는 후속 검증 대상이다. |
| Root/Stun 이동 차단은 실제 이동 이벤트에서 처리한다 | 충족 | `PlayerMoveEvent` 경로에서 root/stun이 활성일 때 목적지를 원위치로 고정한다. 현재 활성 정의가 없어 기본 동작은 변하지 않는다. |
| Stun 공격 차단은 실제 피해 입력에서 처리한다 | 충족 | `SkillInputListener.onEntityDamageByEntity`에서 플레이어 공격을 차단한다. 다른 엔티티·간접 피해 정책은 전투 정책 확정 후 별도 연결한다. |
| Damage/Healing modifier는 공통 Combat adapter를 사용한다 | 구조 준비 | `CombatEffectAdapter`와 `PaperAlchemyCombatAdapter`에 계약을 만들었지만, 수치와 이벤트 우선순위가 확인되지 않은 상태에서 피해량을 변경하지 않는다. |
| 직접/DoT/광역/반사/연쇄 attribution을 보존한다 | 구조 준비 | `CombatEffectAttributionService`가 source·target·effect·DamageKind별 application/contribution을 보존한다. 실제 전투 이벤트의 모든 종류에 기록하는 작업은 후속 Unit으로 남겼다. |
| 전투 효과는 PvP/Boss/연쇄 정책을 확인한 뒤 활성화한다 | 준수 | 현재 effect YAML에 전투 효과를 추가하지 않았고, PvP·Boss·loot 연결을 임의 활성화하지 않았다. |
| 피해·치유 수치는 밸런스 단계에서 결정한다 | 준수 | adapter의 `modifyDamage`/`modifyHealing`은 identity operation이며 수치를 새로 만들지 않았다. |

## 3. 실제 코드 구조 대조

확인한 실제 경로:

`PlayerInteractEvent / PlayerAnimationEvent / PlayerSwapHandItemsEvent`  
→ `SkillInputListener`  
→ `processInputResult`  
→ `SkillService.handleInput`  
→ `EquipmentEffectTriggerEngine` / `EnchantService` / legacy skill 경로

U3 연결:

`processInputResult`  
→ `PaperAlchemyCombatAdapter.blocksAnyActiveSkill()`  
→ active `silence` 또는 `stun`이면 입력을 소비하고 실행하지 않음

공격 경로:

`EntityDamageByEntityEvent`  
→ `SkillInputListener.onEntityDamageByEntity`  
→ 플레이어 공격자 active `stun` 확인  
→ 이벤트 취소

이동 경로:

`PlayerMoveEvent`  
→ `SkillInputListener.onSneakingJump`  
→ active `root` 또는 `stun` 확인  
→ 이동 목적지를 이전 위치로 되돌림

전투 피해의 현재 공통 경로는 다음으로 확인했다.

`CombatService`  
→ transient `DamageContext` 생성  
→ `target.damage(...)`  
→ `EquipmentEffectTriggerListener`가 `getActiveDamageContext()`를 읽음

따라서 후속 피해 modifier는 이 경계를 중복 구현하지 않고, `CombatService`의 context와 Bukkit damage event를 함께 대조한 뒤 연결해야 한다. 이번 단위에서는 event 우선순위 충돌과 DoT·area·reflected·chained source 확정 전의 임의 수정을 피했다.

## 4. 추가·수정 파일

- `src/main/java/com/hyunseo/hyunseorpg/alchemy/CoreEffectIds.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/CombatEffectAdapter.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/PaperAlchemyCombatAdapter.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/CombatEffectAttributionService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/CombatEffectHandler.java`
- `src/main/java/com/hyunseo/hyunseorpg/skill/SkillInputListener.java`
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
- `src/test/java/com/hyunseo/hyunseorpg/alchemy/AlchemyEffectEngineContractTest.java`

## 5. 하드코딩·중복 구현 점검

- 기존 `CombatService`를 복제하지 않았다.
- 기존 `SkillService`·`EnchantService`의 입력 실행을 복제하지 않았다.
- 기존 장비 인챈트 trigger engine을 우회하지 않았다.
- 전투 효과 ID는 `CoreEffectIds` 한 곳에서 관리한다.
- 피해 배율·치유 배율·PvP/Boss 보정값은 추가하지 않았다.
- attribution은 별도 파일 저장이나 보상 지급을 하지 않는 in-memory 경계다.

## 6. 자동 검증

- 전체 테스트: `130 tests / 0 failures / 0 skipped`
- U2 기존 테스트 회귀: 통과
- Core effect ID 정규성: 통과
- 활성 effect가 없을 때 skill/movement/attack adapter가 기본 동작을 변경하지 않음: 통과
- attribution source·target·effect·DamageKind 보존: 통과
- `./gradlew.bat test`: 성공
- `./gradlew.bat jar`: 성공

## 7. 의도적 보류

다음 항목은 실패가 아니라 근거 부족 또는 밸런스 보류다.

| 항목 | 상태 | 이유 |
|---|---|---|
| vulnerability/vampirism/berserk 수치 | BALANCE_PENDING | 현재 활성 YAML에 전투 정의·수치 계약이 없다. |
| healing amplification/suppression | IMPLEMENT_IN_FUTURE_UNIT | 실제 회복 서비스와 Bukkit regain event 전체 경로를 먼저 확정해야 한다. |
| DoT·area·reflected·chained attribution 실제 기록 | IMPLEMENT_IN_FUTURE_UNIT | 현재 모든 발생 경로와 source attribution을 확인하지 않았다. |
| PvP/Boss 적용 정책 | BLOCKED | 현재 양조 전투 정의와 운영 정책이 없다. |
| loot/quest/boss 보상 귀속 | IMPLEMENT_IN_FUTURE_UNIT | Combat attribution을 보상 서비스에 연결할 설계 승인 필요. |
| potion effect adapter | FUTURE_DISABLED | 현재 YAML에서 potion component가 실행 대상이 아니다. |

## 8. 산출물

JAR: `builds/alchemy/HyunseoRPG-0.1.0-SNAPSHOT-alchemy-u3-combat-bridge.jar`  
SHA-256: `83AE1A07EE0FAA84CA7030897D0D495554D62B71115D2579FC940314CE566855`

`libs` 디렉터리는 수정하지 않았다.

## 9. 실서버 필수 확인

1. 테스트용 effect definition으로 silence 활성화 후 모든 스킬 입력이 1회만 차단되는지
2. stun 활성화 후 공격·스킬 입력이 차단되는지
3. root/stun 활성화 후 일반 이동·점프·월드 이동 경계가 의도대로 동작하는지
4. effect 만료 즉시 입력과 이동이 복구되는지
5. 다른 플러그인의 damage cancel과 함께 사용해 이벤트 순서 문제가 없는지
6. `CombatService` custom skill, vanilla attack, projectile, enchant damage에서 후속 modifier 연결 시 중복 적용이 없는지
7. 사망·respawn·quit·kick·reload 후 gate가 남지 않는지
