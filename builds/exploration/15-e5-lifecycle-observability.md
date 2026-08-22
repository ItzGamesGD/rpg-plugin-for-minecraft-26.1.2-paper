# E5 런타임 생명주기 종료 사유 및 정리 관측성 감사

기준 브랜치: `codex/exploration-e4-minimal-execution`  
기준 커밋: `d027a5c24bd753326dbc3101563e27193474026a`  
작업 브랜치: `codex/exploration-e5-lifecycle-observability`

## 이번 단위의 목표

E4의 objective spawn 안전성 위에, 런타임이 왜 종료되었거나 종료 시도가 실패했는지 확인할 수 있는 진단 계약을 추가했다. 영속적인 `StructureEventState`와 운영 진단용 종료 사유를 분리해, 진단 정보가 gameplay 상태 전이를 대신하지 않도록 했다.

## 구현

- `ExplorationEndReason`을 추가했다.
- `ExplorationRuntimeManager`가 구조물 UUID별 최신 lifecycle outcome을 메모리에서 보관한다.
- `FINAL_CLEAR`, `ABANDON_GRACE`, `ACTIVATION_FAILURE`, `COMPLETION_FAILURE`, `ABANDON_FAILURE`, `STATE_INVALID`, `PLUGIN_DISABLE`을 구분한다.
- `lastEndReason(UUID)`과 snapshot `lastEndReasons()`를 제공해 이후 관리자 debug/inspect surface가 동일한 진단값을 사용할 수 있게 했다.
- plugin shutdown에서 teleport exemption도 구조물별로 함께 정리하도록 보강했다.

## E1~E4 충돌/회귀 점검

- E1 등록 구조물의 per-structure disabled → VANILLA persistence 정책은 변경하지 않았다.
- E2 malformed persistence fail-closed, world-load retry, stale bounds/index 격리는 변경하지 않았다.
- E3 trigger policy와 UNDISCOVERED/ACTIVE 허용 범위는 변경하지 않았다.
- E4 빈 objective spawn 실패, 유효 entity ID만 objective/tracker 등록, activation rollback 경계는 유지했다.
- 정상 clear/abandon의 영속 state 전이는 기존 순서를 유지하고 진단 사유만 후행 기록한다.

## 정적 검증

추가 테스트: 1개

- 운영 lifecycle 종료 사유 집합이 정상 clear, abandon, activation 실패, completion 실패, state 불일치, plugin disable을 모두 구분하는지 검증

소스 대조 경로:

`trigger → activate → component execution → objective heartbeat → clear/abandon → cleanup`

정적 소스 검증: 완료.  
Gradle/JDK 25 실행: 이 환경에서는 수행하지 않음.  
GitHub Actions: 이 커밋에 연결된 workflow 결과 확인 필요.  
Paper live server: 수행하지 않음.

## 구현 불가·보류·누락 보고

- `OBSERVABILITY_MISSING`: 현재 관리자 명령어 또는 debug UI가 없어 새 getter를 실제 플레이어/관리자 명령으로 노출하지는 않았다.
- `LIVE_SERVER_VERIFICATION_REQUIRED`: 실제 Paper에서 objective defeat, abandon grace, plugin disable, teleport exemption cleanup, reload/chunk unload/reconnect 경계를 확인해야 한다.
- `CONTENT_MISSING`: swamp hut 내부 이벤트, 보상 item id, 퍼즐, 시각/사운드 연출 및 다른 구조물 variant는 여전히 콘텐츠 정의가 없어 구현하지 않았다.
- `DEFERRED`: 실제 Paper structure key/bounds adapter와 최신 MobService/RPGItemService signature는 서버 및 최신 데스크톱 소스 대조가 필요하다.
- YAML 전역 `enabled=false`, 선택확률 `0.0`은 안전 기본값으로 유지했다.
- 저장소의 `prompts/` 원문은 현재 브랜치 검색에서 확인되지 않아 이번 범위는 코드와 기존 exploration 감사 문서를 기준으로 삼았다.

상태: `IMPLEMENTED_STATIC_VERIFIED / CONTENT_MISSING_REPORTED / OBSERVABILITY_PARTIAL / LIVE_SERVER_VERIFICATION_REQUIRED`
