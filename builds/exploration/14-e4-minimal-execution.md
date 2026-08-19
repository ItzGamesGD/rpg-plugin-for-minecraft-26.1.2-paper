# E4 최소 실행 프로토타입 및 objective spawn 계약 감사

기준 브랜치: `codex/exploration-e3-trigger-activation`  
기준 커밋: `377359a4e24276954c39f14796936d03de05f227`  
작업 브랜치: `codex/exploration-e4-minimal-execution`

## 이번 단위의 해석

E3 이후 계획 문서가 지정한 첫 실행 단위인 “탐험 최소 실행 prototype”을 대상으로 했다. 현재 YAML의 swamp hut / elite_witch_prototype을 최소 실행 기준으로 삼되, 전역 안전 기본값(enabled=false, selection-chance=0.0)은 유지했다.

## 구현

- `ScriptedSpawnComponent`가 외부 MobSpawnPort의 null collection과 null entity id를 안전하게 정규화한다.
- objective=true인 scripted spawn이 유효한 entity id를 하나도 만들지 못하면 빈 objective로 성공 처리하지 않고 `IllegalStateException`을 발생시킨다.
- 따라서 activation 단계에서 실패가 `ExplorationRuntimeManager`의 기존 rollback 경계로 들어가며, UNDISCOVERED record가 ACTIVE로 고립되지 않는다.
- 유효한 entity id만 tracker와 objective set에 등록한다.
- `vanilla:witch` 기본 Bukkit adapter는 그대로 유지되어 실제 Paper 서버에서 최소 witch objective를 생성할 수 있는 연결을 보존했다.

## 발견한 충돌 및 수정

기존 흐름은 spawn adapter가 빈 결과를 반환해도 objectiveMode가 켜지지 않은 채 activation이 성공할 수 있었다. 그 상태에서는 “objective가 죽어서 clear”되는 실제 목표가 없는데도 runtime이 살아 있거나, adapter별 구현에 따라 잘못된 완료 경계가 생길 수 있었다. E4에서 빈 objective spawn을 activation 실패로 명시해 E3의 trigger/activation 계약과 충돌하지 않도록 수정했다.

E1/E2 회귀 재확인:

- E1 비활성 등록 구조물의 VANILLA 기록 정책은 변경하지 않았다.
- E2 malformed persistence fail-closed, world-load retry, stale bounds/index 격리는 변경하지 않았다.
- E3 proximity trigger 정책과 UNDISCOVERED→ACTIVE 진입 경계는 변경하지 않았다.
- E3 activation 예외 시 runtime cleanup 및 최초 record rollback 경계를 재사용한다.

## 정적 검증

추가 테스트: 2개

- valid entity id만 objective로 등록되는지
- 빈 objective spawn이 activation 계약 위반으로 실패하는지

소스 대조로 확인한 실행 경로:

`structure detection → RPG selection → trigger policy → activation → scripted_spawn → objective tracking → heartbeat objective removal → clear`

실제 Gradle/JDK 25 실행: 이 환경에서는 수행하지 않음.  
GitHub Actions 결과: 이 커밋에 연결된 workflow run 확인 필요.  
Paper live server: 수행하지 않음.

## 구현 불가·보류·누락 보고

- `CONTENT_MISSING`: 실제 swamp hut 구조물 내부 이벤트, 보상 item id, 퍼즐, 시각/사운드 연출은 아직 콘텐츠 정의가 없어 구현하지 않았다.
- `CONTENT_MISSING`: pillager outpost 이하 다른 구조물의 실제 RPG variant는 아직 없다.
- `DEFERRED`: 실제 Paper 구조물 scan API의 버전별 adapter 검증은 서버 실행이 필요하다. 현재 reflective provider는 존재하지만 실서버에서 key/bounds 산출을 확인하지 않았다.
- `DEFERRED`: MobService·RPGItemService·InventoryDeliveryService의 최신 데스크톱 소스와 adapter signature 일치 여부는 별도 최신 소스 대조가 필요하다.
- `OBSERVABILITY_MISSING`: activation/clear/abandon의 구조화된 end-reason 로그와 운영용 inspect/status/force/clear 명령은 아직 없다.
- `LIVE_SERVER_VERIFICATION_REQUIRED`: 실제 구조물 등록, swamp hut 판정, witch spawn, objective 제거 후 clear, 재접속/chunk reload/world unload cleanup은 Paper 서버에서 확인해야 한다.
- 현재 YAML의 전역 `enabled=false`와 선택확률 `0.0`은 의도적인 안전 보류이며, E4에서 자동 실행하도록 바꾸지 않았다.
- 저장소 내 `prompts/` 원문은 이번 브랜치 기준 검색에서 확인되지 않아 E4 범위는 탐험 계획 문서와 현재 코드 계약을 기준으로 정했다.

상태: `IMPLEMENTED_STATIC_VERIFIED / CONTENT_MISSING_REPORTED / LIVE_SERVER_VERIFICATION_REQUIRED`
