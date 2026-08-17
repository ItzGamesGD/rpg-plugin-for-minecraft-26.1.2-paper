# HyunseoRPG 양조 Java/Desktop Integration U1 보고서

기준일: 2026-08-15  
단계: U1 Independent Effect Engine core  
상태: 구현·자동검증 완료, 실서버 검증 대기  
기준 명세: `C:/Users/User/Desktop/hyunseorpg_alchemy_md_scaffold_v18_java_integration_plan.zip`

## 1. 구현 범위

이번 단계는 물약, 촉매, 전투, 스킬, 인챈트, 농사 이벤트, 양조 GUI를 연결하지 않고 독립 효과 엔진 core만 보완했다.

구현한 범위:

- 효과 정의 입력값 검증
- `schema-version` 검증
- `alchemy/scaling.yml` 상한 검증
- Registry 후보 snapshot 생성 후 성공 시에만 교체
- 잘못된 reload 시 마지막 정상 snapshot 보존
- 효과 인스턴스 만료·잔여 틱 계산
- stack 수 상한 보정
- HandlerRegistry 등록·조회·해제·불변 snapshot 조회
- 기존 `effect_test_speed`만 활성 유지

구현하지 않은 범위:

- `POTION_EFFECT`, `DAMAGE`, `HEAL` wrapper
- 실제 전투·스킬·인챈트 연동
- 물약 아이템/PDC
- 제작·촉매·양조 GUI
- 풍요 포인트·정수·농사 이벤트 연결
- production effect 및 balance 값

## 2. 코드 변경

### 수정 파일

- `src/main/java/com/hyunseo/hyunseorpg/alchemy/CustomEffectDefinition.java`
  - effect ID, duration, amplifier, max-stacks, policy, handler ID를 생성 시 검증·정규화
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/ActiveEffectInstance.java`
  - 만료 여부와 잔여 틱 계산 추가
  - stack 증가 시 최소값·최대값 보장
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/HandlerRegistry.java`
  - 기존 빈 불변 Map을 동시성 Map 기반 등록소로 교체
  - 등록·조회·해제·전체 snapshot API 추가
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/CustomEffectRegistry.java`
  - schema version 검증
  - scaling 상한 기반 범위 검증
  - 값 자동 절삭 대신 오류 반환
  - 성공 시에만 snapshot 및 loaded version 갱신
- `src/test/java/com/hyunseo/hyunseorpg/alchemy/AlchemyEffectEngineContractTest.java`
  - 인스턴스 만료·stack 상한 테스트
  - 잘못된 정의 거부 테스트
  - HandlerRegistry 정규화·snapshot 테스트

### 변경하지 않은 파일

- `src/main/resources/alchemy/effects.yml`
- `src/main/resources/alchemy/components.yml`
- `src/main/resources/alchemy/conflicts.yml`
- `src/main/resources/alchemy/scaling.yml`
- `src/main/resources/items.yml`
- `src/main/resources/farming/*.yml`
- 기존 player-data 및 PDC 구조

U1에서는 YML 기본값을 새로 확정하지 않았다.

## 3. 코드 논리 검증

### Registry 흐름

```text
ConfigService
  -> alchemy/effects.yml 읽기
  -> schema-version 검증
  -> effect별 ID/policy/범위/component 검증
  -> candidate snapshot 생성
  -> 오류 없음: snapshot 교체
  -> 오류 있음: 기존 snapshot 유지
```

잘못된 설정을 읽었을 때 일부 효과만 새 snapshot에 반영되는 혼합 상태를 만들지 않는다. `lastErrors`는 실패 원인을 유지하고, 기존 정상 snapshot은 보존한다.

### 현재 활성 효과

- `effect_test_speed`: 활성
- component: `attribute`
- 실제 Bukkit Attribute 적용: 기존 `EffectService` 경로 유지
- production effect: 없음
- 물약·레시피: 없음

### HandlerRegistry

이번 단계에서 handler 실행 로직을 발명하지 않았다. 다만 후속 Unit에서 실제 handler를 등록할 수 있도록 현재의 항상 빈 Map 구조를 제거했다. 등록된 handler의 실행 계약은 U3에서 Paper/combat mapping 후 확정한다.

### Lifecycle

기존 `EffectService`의 다음 경로는 유지했다.

- apply
- 동일 effect 재적용 및 stack policy 처리
- remove
- target clear
- tick 만료 정리
- death/quit/world-change cleanup
- plugin shutdown cleanup

U1에서는 Bukkit Attribute/PotionEffect 복구 범위를 확장하지 않았다. 해당 검증은 U2 소유다.

## 4. 설계 문서 대조 결과

| 설계 요구 | U1 결과 |
|---|---|
| 독립 Effect Engine을 물약과 분리 | 충족 |
| Registry snapshot 교체 | 충족 |
| 잘못된 reload에서 정상 snapshot 유지 | 충족 |
| 기존 test effect만 활성 | 충족 |
| production effect 조기 활성 금지 | 충족 |
| `ATTRIBUTE` wrapper만 현재 사용 | 충족 |
| `POTION_EFFECT/DAMAGE/HEAL` 보류 | 충족 |
| BALANCE_PENDING 수치 유지 | 충족 |
| 실제 Paper 이벤트·전투 연결 전 구현 금지 | 충족 |
| ZIP package 구조 기계 변환 금지 | 충족 |

문서와 실제 소스의 패키지 차이(`effect.*` 대 `alchemy.*`)는 U0에서 확정한 대로 실제 `alchemy` 구조를 유지했다.

## 5. 자동 테스트 및 빌드

- 테스트: `128 tests / 0 failures / 0 skipped`
- 컴파일: 성공
- JAR 빌드: 성공
- 컴파일 경고: Paper API의 기존 Attribute 관련 deprecated 표시 7건
- warning은 이번 단계의 실패가 아니며 U2 Paper API 정리 대상으로 남겼다.

JAR:

`C:/Users/User/Documents/Codex/2026-06-29/hyunseorpg-paper-spigot-rpg-hyunseorpg-1/builds/alchemy/HyunseoRPG-0.1.0-SNAPSHOT-alchemy-u1-effect-core.jar`

SHA-256:

`D9B5D481168AA9E648531C252B12A4E97B9F75BBEF3E3243B44E2464AEFA603E`

`libs` 폴더의 기존 산출물은 수정하지 않았다.

## 6. 실서버 검증 필요 항목

U1 자동 테스트만으로 Bukkit 런타임 전부를 증명할 수 없으므로 다음은 LIVE TEST REQUIRED다.

- 관리자 테스트 명령 또는 후속 임시 진입점으로 `effect_test_speed` 적용
- 적용 시 이동속도 modifier가 1개만 생성되는지 확인
- 재적용 시 modifier 누적 없음
- 만료·remove·clear 시 기존 다른 modifier 보존
- 플레이어 사망·로그아웃·월드 이동 시 기본 cleanup 확인
- `/rpg reload effects` 성공 시 정의 중복 없음
- 잘못된 `alchemy/effects.yml` reload 시 기존 정상 snapshot 유지
- production effect/물약/레시피가 노출되지 않음

## 7. 후속 보류 및 위험

- `EffectService`가 현재 실제로 실행하는 component는 `attribute`뿐이다.
- `HandlerRegistry`는 등록 경계만 준비됐고, 실제 handler 실행은 U3에서 구현한다.
- `potion_id`, `data_version`, `catalyst_id` PDC는 아직 없다.
- production 재료 ID 중 `tomato_concentrate`, `chili_powder`, `corrosion_essence`, `common_cold_loot`는 여전히 실제 Registry에서 확인되지 않는다.
- U2에서 Attribute 원복·quit/kick/restart 정책을 Paper 런타임 기준으로 다시 검증해야 한다.
- U3 이전에는 combat/skill/enchantment 효과를 활성화하지 않는다.

## 8. 종료 판정

U1 exit gate 중 코드·설정·자동 테스트 범위는 통과했다. 실제 Bukkit 적용/정리 동작은 LIVE TEST REQUIRED로 남겼다. U2를 자동 시작하지 않았으며, 다음 단계는 U2 Paper vanilla wrapper 및 lifecycle/persistence foundation이다.
