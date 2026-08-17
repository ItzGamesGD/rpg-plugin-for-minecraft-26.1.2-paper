# 양조 Java/Desktop Integration U2 보고서

기준일: 2026-08-15  
단위: U2 Paper vanilla wrapper·lifecycle·persistence foundation  
상태: 코드 구현 및 자동 검증 완료, 실서버 검증 대기

## 1. 이번 단위의 범위

U2는 양조 효과의 Paper/Bukkit 적용 경계, 효과 틱 수명주기, 사망·재접속·강제 종료·월드 이동 정리, 설정 reload 시 snapshot 안전성, 기존 플레이어 YAML의 `alchemy.version` 연결만 다뤘다. Combat·Skill·Enchant 실제 효과 수식과 전투 이벤트 연결은 U3 범위로 남겼다.

## 2. 설계안 대조

| 설계 계약 | 판정 | 근거 |
|---|---|---|
| vanilla wrapper는 효과 서비스와 Bukkit 객체 경계에 둔다 | 부분 충족 | 현재 실제 구조에는 별도 `VanillaEffectAdapter` 구현체가 없고 `EffectService`가 Attribute API를 직접 감싼다. 중복 adapter를 만들지 않고 기존 서비스 경계를 유지했다. |
| Attribute modifier는 플러그인 소유분만 제거한다 | 충족 | deterministic UUID(`alchemy:<instance>:<attribute>`)로 생성·삭제하며 다른 modifier는 건드리지 않는다. |
| 효과 틱 작업은 시작·중지 가능해야 한다 | 충족 | `TickManager.isRunning()`을 추가하고 `EffectService.start()/shutdown()`과 연결했다. |
| 사망·로그아웃·월드 이동·respawn·kick에서 정책에 따라 정리한다 | 충족 | `EffectService`에 UUID 기반 lifecycle API와 `PlayerKickEvent`, `PlayerRespawnEvent` 처리를 추가했다. |
| 비영속 효과는 reload/restart에서 남지 않는다 | 충족 | 성공한 effect snapshot commit 뒤 기존 active instance를 정리한다. 서버 종료 시에도 전체 instance와 tick task를 정리한다. |
| 실패한 설정 reload는 이전 정상 snapshot을 유지한다 | 충족 | `CustomEffectRegistry.load()`의 기존 보존 동작을 유지하고 `EffectService.reload()`는 registry 성공 뒤에만 active 상태를 정리한다. |
| 플레이어 canonical data에 alchemy version을 둔다 | 충족 | 기존 플레이어 YAML의 `alchemy.version`만 추가하며 별도 파일을 만들지 않는다. |
| 기존 데이터와 무관한 필드는 보존한다 | 충족 | 기존 `YamlPlayerDataRepository` 전체 저장 경로에 새 section만 추가했다. |
| U2에서 combat/potion/recipe를 활성화한다 | 의도적 보류 | 실제 효과 정의와 공통 전투 계약은 U3에서 실제 호출 경로 대조 후 연결한다. |

## 3. 실제 코드 구조 대조

현재 호출 경로는 다음과 같다.

`HyunseoRPGPlugin.onEnable()`  
→ `EffectService` 생성  
→ `CustomEffectRegistry.load()`  
→ `registerListeners()`에서 `EffectService` 등록  
→ `EffectService.start()`  
→ `TickManager`의 1 tick 반복 작업  
→ 만료·사망·로그아웃·월드 이동 정리

기존 코드에는 `EffectService` 객체가 생성되고 시작되지만 Bukkit listener 등록이 빠져 있었다. 따라서 내부 lifecycle 메서드가 있어도 실제 이벤트 호출 경로에 도달하지 않는 결함이었다. U2에서 `registerEvents(effectService, this)`를 등록했다.

reload 경로는 다음처럼 바뀌었다.

`/rpg reload effects 또는 all`  
→ `configService.reloadAlchemyEffectsConfigs()`  
→ `EffectService.reload()`  
→ candidate registry 검증  
→ 성공 시 기존 active instance 정리  
→ 실패 시 기존 registry와 active 상태 유지

기존의 단순 `effectService.load()` 호출을 reload 경로에서 제거했다. 초기 enable은 기존 `load()`를 사용한다.

## 4. 변경 파일

- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectService.java`
  - UUID 기반 death/logout/kick/world-change/respawn lifecycle API 추가
  - Bukkit lifecycle event handler 추가
  - `reload()` 및 전체 active 정리 추가
  - tick 상태 조회 추가
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/TickManager.java`
  - `isRunning()` 추가
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
  - EffectService listener 등록
  - effects reload 경로를 `reload()`로 변경
- `src/main/java/com/hyunseo/hyunseorpg/player/PlayerRPGData.java`
  - `alchemyDataVersion`, `alchemyDataMigrationRequired` 추가
- `src/main/java/com/hyunseo/hyunseorpg/player/YamlPlayerDataRepository.java`
  - `alchemy.version` 읽기·쓰기 추가
- `src/main/java/com/hyunseo/hyunseorpg/player/PlayerDataService.java`
  - alchemy migration flag를 dirty 저장 대상으로 반영
  - 성공 저장 시 alchemy migration flag 해제

## 5. 저장·마이그레이션 정책

- 저장 위치: 기존 `players/<uuid>.yml`
- 새 경로: `alchemy.version`
- 별도 alchemy 플레이어 파일: 생성하지 않음
- 기존 section 부재: 메모리 기본 version 1과 migration-required 상태로 로드
- autosave: 기존 dirty batch에 포함되어 새 section을 원자적으로 저장
- 원자 저장: 기존 `tmp` 작성 후 `ATOMIC_MOVE` 경로를 그대로 사용
- 기존 플레이어 데이터: farming, coin, stat, quest 및 나머지 필드를 재설정하지 않음
- 효과 인스턴스 자체: 현재 기본 정책에서 영속화하지 않음. 서버 종료·성공한 effect reload 후 제거

## 6. 자동 검증

- 전체 테스트: `128 tests / 0 failures / 0 skipped`
- `./gradlew.bat test`: 성공
- `./gradlew.bat jar`: 성공
- 기존 U1 effect contract 테스트: 통과
- 실제 Paper 이벤트 등록 및 Attribute modifier 제거: 자동 테스트 환경에서 Bukkit live lifecycle을 재현하지 못하므로 실서버 항목으로 남김

## 7. 산출물

JAR: `builds/alchemy/HyunseoRPG-0.1.0-SNAPSHOT-alchemy-u2-paper-lifecycle.jar`  
SHA-256: `AE7894BC881179BF19E77DA12032E6E83CE08B7F488E4A13A55274D5EA12E9A5`

`libs` 디렉터리는 수정하지 않았다.

## 8. 실서버 필수 확인

1. 효과 적용 후 만료 시 Attribute modifier가 정확히 0개가 되는지
2. 다른 플러그인 modifier와 바닐라 modifier가 보존되는지
3. 사망·respawn 후 `remove-on-death` 정책별 결과
4. quit·kick·재접속 후 `persist-on-logout` 정책별 결과
5. 월드 이동 후 `persist-on-world-change` 정책별 결과
6. `/rpg reload effects` 성공·실패 시 기존 효과와 registry 상태
7. 서버 종료·재시작 후 비영속 효과가 남지 않는지
8. 기존 플레이어 YAML에 alchemy section이 추가될 때 다른 데이터가 보존되는지

## 9. U3 진입 판정

U2의 Paper lifecycle 및 저장 경계는 구현되었다. U3에서 실제 `CombatService`, `DamageContext`, `SkillInputListener`, `SkillService`, `EnchantService`, Bukkit damage/heal 이벤트의 연결 가능성을 조사하고, 근거가 확인된 adapter만 추가한다. 수치와 비활성 상태인 전투 효과는 임의로 활성화하지 않는다.
