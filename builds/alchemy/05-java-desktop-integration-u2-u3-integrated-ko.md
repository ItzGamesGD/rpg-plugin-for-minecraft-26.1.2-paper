# 양조 Java/Desktop Integration U2·U3 통합 보고서

기준일: 2026-08-15  
범위: U0 매핑 이후 U2 Paper lifecycle/persistence + U3 combat/skill bridge  
검증 방식: 설계 문서 대조 + 실제 소스 호출 경로 대조의 2중 검증

## 1. 통합 결과

U2와 U3를 한 번에 구현하지 않고 순차 분리했다.

1. U2: EffectService lifecycle, TickManager, YAML player persistence, reload snapshot 경계
2. U2 독립 테스트·JAR·보고서 고정
3. U3: 실제 SkillInputListener 경계의 silence/root/stun gate, combat adapter 계약, attribution 구조
4. U3 독립 테스트·JAR·보고서 고정
5. 전체 테스트와 최종 JAR 재검증

두 단위 모두 기존 농사·제작·상점·강화·승급 저장 구조를 재설계하지 않았다.

## 2. 설계 문서와 실제 코드의 이중 대조

### 2-1. 설계 계약 대조

| 설계 영역 | 구현 상태 | 최종 판정 |
|---|---|---|
| 효과 정의 Registry와 검증된 snapshot | U1 유지, U2 reload에서 사용 | PASS |
| Paper Attribute 적용·plugin-owned restore | U1/U2 기존 `EffectService` 경계 유지 | PASS |
| lifecycle cleanup | U2 death/respawn/quit/kick/world change/shutdown 연결 | PASS |
| player-data canonical alchemy section | U2 기존 YAML에 `alchemy.version` 추가 | PASS |
| Skill/Enchant gate | U3 실제 `SkillInputListener`에 연결 | PASS, 실서버 필요 |
| combat damage/healing 수치 | adapter만 준비, 수치 미적용 | BALANCE_PENDING |
| DoT/area/reflection/chain attribution | 구조만 준비 | IMPLEMENT_IN_FUTURE_UNIT |
| PvP/Boss/loot/quest 정책 | 활성화하지 않음 | BLOCKED 또는 후속 Unit |

### 2-2. 실제 코드 호출 경로 대조

| 기능 | 실제 시작점 | 현재 종점 | 중복 여부 |
|---|---|---|---|
| 초기 효과 로드 | `HyunseoRPGPlugin.onEnable` | `EffectService.load` → `CustomEffectRegistry` | 없음 |
| effect reload | `/rpg reload` 계층 | `configService` → `EffectService.reload` | 기존 load 경로 제거 |
| effect tick | `EffectService.start` | `TickManager` → `EffectService.tick` | 없음 |
| lifecycle | Bukkit event | 등록된 `EffectService` handler → UUID API | U2에서 실제 등록 보완 |
| skill gate | Bukkit input event | `SkillInputListener` → `PaperAlchemyCombatAdapter` | 기존 `SkillService` 재사용 |
| combat source | `CombatService` | transient `DamageContext` → Bukkit damage event | 새 damage 계산 중복 없음 |
| equipment enchant | `EquipmentEffectTriggerListener/Engine` | 기존 engine | U3가 우회하지 않음 |
| player save | 기존 `PlayerDataService` | `YamlPlayerDataRepository` atomic temp/move | 별도 파일 없음 |

## 3. 확인된 실제 설계 충돌과 처리

1. 문서 scaffold의 `EffectLifecycleService`는 현재 실제 `EffectService`에 없는 `ClearReason` 타입을 참조했다. 새 추상화를 기계적으로 추가하지 않고 현재 서비스에 UUID lifecycle API를 확장했다.
2. scaffold의 `VanillaEffectAdapter`와 실제 코드의 Attribute 처리가 겹친다. 중복 adapter를 만들지 않고 기존 `EffectService`를 Paper 경계로 인정했다.
3. 문서의 전투 효과 목록은 현재 실제 YAML에 활성 정의가 없다. 전투 수치를 소스에 임의로 추가하지 않고 게이트·adapter·attribution만 준비했다.
4. CombatService는 ThreadLocal `DamageContext`를 사용한다. U3는 이를 복제하거나 저장하지 않고 후속 modifier 연결 지점으로 기록했다.

## 4. 테스트와 빌드

- U2 독립: `128 tests / 0 failures / 0 skipped`
- U3 통합: `130 tests / 0 failures / 0 skipped`
- 최종 `./gradlew.bat test`: 성공
- 최종 `./gradlew.bat jar`: 성공
- `libs` 수정: 없음

U2 JAR:
`builds/alchemy/HyunseoRPG-0.1.0-SNAPSHOT-alchemy-u2-paper-lifecycle.jar`  
SHA-256: `AE7894BC881179BF19E77DA12032E6E83CE08B7F488E4A13A55274D5EA12E9A5`

U3 JAR:
`builds/alchemy/HyunseoRPG-0.1.0-SNAPSHOT-alchemy-u3-combat-bridge.jar`  
SHA-256: `83AE1A07EE0FAA84CA7030897D0D495554D62B71115D2579FC940314CE566855`

## 5. 남은 항목 분류

- `RESOLVED_MAPPING`: Paper lifecycle, existing Attribute ownership, player YAML, SkillInput route, CombatService transient context
- `IMPLEMENT_IN_FUTURE_UNIT`: healing event/service, full damage modifier, DoT/area/reflected/chained attribution wiring, loot/boss/quest attribution
- `BALANCE_PENDING`: vulnerability/vampirism/berserk/healing/hardening 수치 및 PvP/Boss 배율
- `FUTURE_DISABLED`: potion component runtime, potion item content, unverified combat definitions
- `BLOCKED`: 실제 운영 정책이 없는 PvP/Boss effect activation

U2·U3에서 분류되지 않은 deferred/pending 항목은 남기지 않았다. 구현하지 않은 항목은 모두 위 분류에 명시했다.

## 6. 실서버 통합 테스트 목록

1. 테스트 효과 적용·만료·Attribute 원상 복구
2. 바닐라 및 다른 플러그인 modifier 보존
3. 사망·respawn·quit·kick·월드 이동 lifecycle
4. `/rpg reload effects` 성공·실패 snapshot 보존
5. 서버 재시작 시 비영속 효과 초기화
6. silence/stun의 스킬·인챈트 입력 차단
7. root/stun 이동 및 점프 차단
8. stun 공격 차단
9. 만료·제거 후 입력/이동/공격 복구
10. CombatService direct/custom-skill/projectile/enchant damage의 후속 modifier 중복 여부
11. PvP/Boss/DoT/area/reflection/chain 정책 확인 후 별도 승인
12. 기존 player YAML에서 `alchemy.version` 추가 후 농사·화폐·퀘스트 데이터 보존

## 7. 최종 판정

U2와 U3의 코드 구조는 현재 HyunseoRPG 아키텍처에 맞게 확장되었고, 설계 문서의 의도를 유지하면서 검증되지 않은 전투 수치와 시스템을 활성화하지 않았다. 자동 테스트는 통과했다. 다만 실제 Paper 이벤트 우선순위, Attribute 복구, 전투 입력 차단, 기존 외부 운영 YAML과의 재접속 결과는 서버에서 직접 확인해야 한다.
