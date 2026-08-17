# 양조 프롬프트 1 효과 엔진 구현 보고서

기준 문서: `C:/Users/User/Desktop/HyunseoRPG_양조_문서모듈_2026-08-06.zip` 내부
`HyunseoRPG_Alchemy_Document_Module/prompts/01-effect-engine.md`

상태: `SOURCE_DEFAULT_UPDATED / JAVA_IMPLEMENTED / BALANCE_PENDING / LIVE_TEST_REQUIRED`

## 설계 대조

프롬프트 1은 물약·레시피를 구현하지 않고 물약과 독립적인 상태효과 기반만 구현하도록 요구한다. 이에 따라 새 `alchemy` 패키지는 효과 정의, 효과 인스턴스, 출처, 대상 정책, 스택 정책, 충돌 판정, 컴포넌트 경계, 틱 관리자, 수명주기 정리를 담당한다. 농사·전투·인챈트·물약 서비스는 직접 참조하지 않는다.

`effect_test_speed`만 기본 활성 정의로 등록했다. 포션 ID, 포션 레시피, 촉매, 8개 전투 효과는 추가하지 않았다.

## 구현 경로

`alchemy/effects.yml` → `ConfigService` → `CustomEffectRegistry` → `EffectService` → `AttributeModifier`

효과 적용 시 대상 UUID, 적용자 UUID, `EffectSourceType`, source ID, instance UUID를 보존한다. 같은 효과 재적용은 YAML의 `REFRESH_DURATION` 정책을 따르고, modifier UUID는 instance와 Attribute로 결정해 중복 누적을 막는다.

사망·로그아웃·월드 이동·만료·플러그인 종료 시 `EffectService`가 instance와 modifier를 정리한다. 저장 복구는 구현하지 않았으며 문서 정책대로 기본적으로 재시작 후 효과를 복구하지 않는다.

## 운영 연결

- `/rpg effect list`, `debug`: 현재 플레이어의 활성 효과와 출처 조회
- `/rpg effect apply <effect_id>`: 관리자 테스트용 효과 적용
- `/rpg effect remove <effect_id>`, `clear`: 관리자 테스트용 제거
- `/rpg effect reload`: 효과 YAML 검증 후 성공한 snapshot만 교체
- `/rpg doctor alchemy`, `/rpg doctor effects`: YAML·필수 `effect_test_speed` 진단
- `/rpg migrate alchemy --dry-run|--apply`: 네 개 양조 기반 YAML의 누락 키 병합

일반 reload는 설정을 수정하지 않으며, 잘못된 효과 설정은 기존 정상 registry snapshot을 유지한다. 양조 migration은 기존 운영자 값을 덮어쓰지 않고 필요한 누락 트리만 보완하며 APPLY 전에 백업한다.

## 변경 파일

- `src/main/java/com/hyunseo/hyunseorpg/alchemy/*`
- `src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigService.java`
- `src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigDoctor.java`
- `src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigMigrationService.java`
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
- `src/main/java/com/hyunseo/hyunseorpg/command/RPGGiveCommand.java`
- `src/main/resources/alchemy/effects.yml`
- `src/main/resources/alchemy/components.yml`
- `src/main/resources/alchemy/conflicts.yml`
- `src/main/resources/alchemy/scaling.yml`
- `src/test/java/com/hyunseo/hyunseorpg/alchemy/AlchemyEffectEngineContractTest.java`

## 검증 결과

- Java 컴파일: PASS
- 전체 테스트: `125 tests / 0 failures / 0 skipped`
- 효과 엔진 계약 테스트: `3 tests / 0 failures / 0 skipped`
- 물약·레시피 미구현 경계: PASS
- 실제 Paper 서버 `/rpg effect apply`, 만료, 사망, 로그아웃, 월드 이동: LIVE TEST REQUIRED
- 외부 서버 YAML에서 `/rpg migrate alchemy --dry-run`과 `--apply`: LIVE TEST REQUIRED

빌드 산출물: `builds/alchemy/HyunseoRPG-0.1.0-SNAPSHOT-alchemy-prompt1-effect-engine.jar`

SHA-256: `7CA1C1D917275742ADF090D272CB45DE894AE5F06F4CE67721B535ECD3F38CAA`

`libs`는 변경하지 않았다.
