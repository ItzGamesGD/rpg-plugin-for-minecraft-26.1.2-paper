# HyunseoRPG 양조 U10 운영·보안 통합 보고서

기준일: 2026-08-15
범위: U10 운영 진단, 관리자 검증 도구, reload 정합성, 바닐라 양조·호퍼 우회 차단

## 1. 구현 결과

- `/rpg alchemy reload` 추가
- `/rpg alchemy give <player> <potion_id> [amount]` 추가
- `/rpg alchemy inspect <player>` 추가
- `/rpg alchemy clearjobs` 추가
- `/rpg doctor alchemy`, `/rpg doctor effects`의 실제 Registry 상태 진단 보강
- `/rpg reload all`에 effects → potions → alchemy recipes → catalysts → special catalysts 순서 추가
- reload 성공 시에만 특수 촉매 작업과 양조 GUI 세션 정리
- `AlchemyAuditLog` 추가
- 커스텀 물약이 포함된 양조대에 한해 Bukkit 양조, 클릭, 드래그, 호퍼 이동 차단
- 일반 바닐라 물약·바닐라 양조대는 차단하지 않음
- PotionDefinition에 명시적 output item ID 연결

현재 `alchemy/potions.yml`, `alchemy/catalysts.yml`, `alchemy/gui.yml`의 production 활성값은 기존처럼 비활성이다. U10은 밸런스 수치나 콘텐츠를 임의 활성화하지 않았다.

## 2. 설계 구조 대조

설계 계약:

`명령 dispatcher → Registry/PDC → 기존 Service → Audit`

실제 구현:

- 명령은 기존 `RPGGiveCommand`에 확장
- 효과는 기존 `EffectService`와 `CustomEffectRegistry` 사용
- 물약은 `PotionRegistry`, `PaperPotionPdcContract`, `RPGItemService` 사용
- 작업 정리는 기존 `BoundedSpecialCatalystExecutionService`, `AlchemyGuiControllerService` 사용
- 설정 reload는 `ConfigService`와 기존 `RPGReloadService` 사용
- 바닐라 우회 차단은 실제 Bukkit 이벤트 리스너에 연결

별도 물약 엔진, 별도 저장소, 별도 플레이어 데이터, 별도 GUI 실행 로직은 만들지 않았다.

## 3. Reload 원자성 및 상태 정리

`/rpg alchemy reload`는 다음 순서로 후보 상태를 검증한다.

1. effects
2. potions
3. recipes
4. catalysts
5. special-catalysts

앞 단계가 실패하면 뒤 단계는 `SKIPPED` 처리한다. 전체 성공 전에는 stale 작업과 GUI 세션을 정리하지 않는다. 전체 성공 후에만 특수 촉매 실행과 GUI 세션을 정리하고 audit log를 남긴다.

`/rpg reload all`도 같은 양조 Registry 순서를 포함한다.

## 4. Fail-closed 및 권한

- 비활성 물약 지급 거부
- 미등록 물약 지급 거부
- ItemRegistry에 없는 출력 아이템 지급 거부
- 일반 플레이어의 양조 관리자 명령 거부
- 미등록·비활성 촉매는 기존 실행 서비스에서 거부
- PDC가 없는 일반 아이템은 양조 우회 차단 대상이 아님

## 5. 변경 파일

- `src/main/java/com/hyunseo/hyunseorpg/alchemy/AlchemyAuditLog.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/AlchemyVanillaBypassListener.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/SpecialCatalystExecution.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/potion/PotionDefinition.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/potion/YamlPotionRegistry.java`
- `src/main/java/com/hyunseo/hyunseorpg/command/RPGGiveCommand.java`
- `src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigDoctor.java`
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
- `src/test/java/com/hyunseo/hyunseorpg/alchemy/AlchemyU10ContractTest.java`

## 6. 자동 검증

- `140 tests completed`
- failures: `0`
- Gradle `test`: PASS
- Gradle `jar`: PASS
- 기존 U0-U9 테스트 포함 전체 회귀: PASS

검증한 계약:

- production 물약·촉매 기본 비활성 유지
- 비활성 콘텐츠 fail-closed
- output item ID 계약
- GUI 비활성 상태 유지
- 기존 바닐라 양조 전체 차단 금지 정책

## 7. 미검증·LIVE TEST REQUIRED

- Paper 실서버에서 `/rpg doctor effects` 출력과 실제 활성 Registry 수 일치 확인
- `/rpg doctor alchemy`의 missing output item 진단 확인
- 비활성 물약 `/rpg alchemy give` 거부 확인
- 운영자가 임시로 활성화한 물약의 지급 → PDC → `/rpg alchemy inspect` 확인
- 커스텀 PDC 물약이 든 양조대에서 양조·클릭·드래그·호퍼 이동 차단 확인
- 일반 바닐라 물약의 양조·호퍼 이동이 유지되는지 확인
- `/rpg alchemy reload` 실패 시 기존 Registry와 작업이 유지되는지 확인
- 성공 reload 후 stale GUI/job이 정리되는지 확인
- 서버 재시작 후 listener 중복 등록과 작업 중복이 없는지 확인

U10에서는 production 물약·촉매·양조 GUI를 활성화하지 않았으므로, 실제 물약 효과와 촉매 수치 검증은 해당 콘텐츠 활성화 후 별도 LIVE TEST가 필요하다.

코드 구조상 각 Registry는 실패 시 자체 마지막 정상 snapshot을 유지한다. 다만 여러 Registry를 하나의 객체로 묶은 전역 cross-registry snapshot 교체까지 완전한 단일 트랜잭션으로 만든 것은 아니므로, 후속 운영 안정화에서 실패 중간 상태에 대한 통합 rollback 검증이 남아 있다. U10에서는 실패 시 stale effect/job/session의 선행 정리를 막고, 세션 정리는 전체 성공 뒤로 제한했다.

## 8. 산출물

JAR:

`C:\Users\User\Documents\Codex\2026-06-29\hyunseorpg-paper-spigot-rpg-hyunseorpg-1\builds\alchemy\HyunseoRPG-0.1.0-SNAPSHOT-alchemy-u10-operations.jar`

SHA-256:

`D12A27F166764E74D1FAD97228E56ECB75B95CC25DBA25FDB33843A87F47F40E`
