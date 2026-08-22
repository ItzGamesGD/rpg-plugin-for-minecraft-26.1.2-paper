# 탐험 E1-E8 영지 이전 동결·종합 회귀 감사

## 판정 기준

- 작업 브랜치: codex/exploration-pre-territory-freeze-audit
- 기준 브랜치: codex/exploration-e8-structure-content-prototype
- 기준 커밋: 52438a0304c73e0f0f48db2701f0ba1b2e02a2ec
- 범위: 탐험 E1-E8
- 제외: 영지/territory 및 영지 기반 콘텐츠
- 운영 서버 JAR 덮어쓰기: 하지 않음

이번 단계는 새로운 게임 기능을 추가하는 단계가 아니다. E1-E8의 결과물을 영지 이전 기준선으로 고정하고, 과거 회귀·최신 종합 검증·의도적 보류·라이브 전용 검증을 분리한다.

## E1-E8 기준선

| 단계 | 브랜치/역할 | 커밋 | 현재 판정 |
|---|---|---|---|
| E1 | registry/detection | 65850dd8550d2c933f85e1010db0fef5ce10d911 | 정적 구조 반영, 라이브 미검증 |
| E2 | persistence/index | 688a33d705df2d7dbd8c021cd81f3a40a37af288 | 정적 구조 반영, 라이브 미검증 |
| E3 | trigger/activation | 377359a4e24276954c39f14796936d03de05f227 | 정적 구조 반영, 라이브 미검증 |
| E4 | minimal execution | d027a5c24bd753326dbc3101563e27193474026a | 정적 구조 반영, 라이브 미검증 |
| E5 | lifecycle/observability | bbaca1963900c11e184cb6e82cb63aa6ec11109e5 | 정적 구조 반영, 라이브 미검증 |
| E6 | status snapshot | 401faa347d9068af0f80e5f03f731cd268528222 | 정적 구조 반영, 라이브 미검증 |
| E7 | full regression audit | 4c9589683580f7eed638e0021f5ceec03281ef1b | 감사 보고서 반영, 라이브 미검증 |
| E8 | structure prototype | 52438a0304c73e0f0f48db2701f0ba1b2e02a2ec | outpost 프로토타입과 계약 테스트 반영, 라이브 미검증 |

## 동결 대상

다음은 영지 이전까지 기준선으로 유지한다.

- ExplorationRegistry와 구조 key 매핑
- persistent record 및 기존 record 재선정 방지 규칙
- trigger와 activation 흐름
- ExplorationRuntime의 participant/objective/tracker 구조
- heartbeat cleanup과 lifecycle end reason
- status snapshot의 관찰 모델
- 구조물 설정의 안전 기본값
- swamp hut의 기존 prototype 정의
- pillager outpost의 scout_wave_prototype 정의
- E1-E8 보고서와 각 단계별 브랜치 이력

영지 이전에는 탐험 엔진의 의미를 바꾸는 리팩터링, 구조 key 변경, persistence schema 변경, 공통 cleanup 계약 변경을 추가하지 않는다. 오류가 확인되면 해당 작업 단위에서 최소 수정 브랜치를 만들고, 수정 범위를 최신 기준선에 선택적으로 반영한다.

## E1-E8 회귀 점검

### Registry와 detection

- 모든 구조 key는 안정적인 namespaced Minecraft key를 사용해야 한다.
- 중복 key와 빈 key가 없어야 한다.
- 알 수 없는 구조·변형·component는 안전하게 무시되거나 비활성화되어야 한다.
- top-level enabled가 false인 경우 탐험 모듈은 실행되지 않아야 한다.
- 현재 설정의 모든 selection-chance는 0.0이며 운영 자동 실행을 유발하지 않아야 한다.

### Persistence

- 기존 persistent record가 설정 변경만으로 재선정되지 않아야 한다.
- 구조 key, variant ID, 상태, 위치가 기록 모델과 일치해야 한다.
- schema-version 변경 없이 기존 데이터를 임의로 해석하지 않아야 한다.
- reload/restart 시 중복 runtime이 생성되지 않아야 한다.

### Trigger와 activation

- 구조 감지와 runtime 시작이 분리되어야 한다.
- 동일 구조에 대해 중복 activation이 발생하지 않아야 한다.
- participant 없는 상태에서 reward 또는 objective 실행이 발생하지 않아야 한다.
- activation 실패 시 tracker와 partial object가 정리되어야 한다.

### Runtime과 lifecycle

- objective entity가 유효한 동안 heartbeat가 정상 runtime을 종료시키지 않아야 한다.
- invalid/dead entity는 정리 대상이며 정상 종료와 구분되어야 한다.
- abandon grace가 끝나기 전 조기 종료하지 않아야 한다.
- plugin disable, world unload, admin clear는 명시적 lifecycle cleanup으로 기록되어야 한다.
- cleanup은 중복 호출되어도 안전해야 한다.
- 마지막 종료 reason을 진단 화면에서 확인할 수 있어야 한다.

### E8 구조 콘텐츠

- package enabled는 false
- pillager_outpost enabled는 false
- pillager_outpost selection-chance는 0.0
- scout_wave_prototype는 scripted_spawn으로 pillager 2마리를 objective로 정의
- 별도의 일반 경로 우회, 보상 자동 지급, 바닐라 chest 변경은 없음

## 상태 분리

### 정적 구현 완료

- 클래스·registry·component·runtime·persistence·lifecycle의 기본 계약
- 구조 설정 parser와 구조 content definition
- E1-E8 단계별 보고서
- E8 구조 콘텐츠 계약 테스트 소스

### CONTENT_MISSING 또는 BALANCE_PENDING

- 실제 구조물별 RPG 이벤트 연출
- 구조물별 경고와 결과 연출
- 구조물별 커스텀 몹/스탯
- 실제 loot/reward 정책
- chest와 바닐라 보상에 대한 최종 상호작용
- clear 조건 및 재진입 정책의 최종 수치
- 다중 플레이어 참여·이탈의 최종 기획
- 확률·반경·grace·보상 밸런스
- 구조물별 전체 콘텐츠 카탈로그
- 영지 연결

위 항목들은 현재 엔진 결함으로 판정하지 않는다. 다만 실제 콘텐츠를 완성하기 전까지는 구현되지 않은 항목으로 계속 보고해야 한다.

### LIVE_SERVER_VERIFICATION_REQUIRED

- 실제 Minecraft 구조 key detection
- 실제 runtime 생성과 중복 방지
- 실제 objective entity 추적
- entity 사망·무효화·수동 제거 cleanup
- abandon radius와 grace
- reload/restart/reconnect
- plugin disable/world unload
- 관리자 clear와 마지막 종료 reason
- outpost scout wave의 실제 위치·개수·objective 처리
- 바닐라 chest/loot 비변경
- 테스트 설정 원복 후 자동 실행 방지

## 영지 이전 전 실행 순서

1. 과거 기준선 확인: E1-E8 커밋과 브랜치별 변경 파일을 대조한다.
2. 최신 소스 통합: 데스크톱 최신 소스에 E1-E8의 필요한 변경만 선택 반영한다.
3. 정적 종합 검증: Gradle clean build, 전체 테스트, YAML/resource 검증을 실행한다.
4. 정적 실패 처리: 실패한 작업 단위만 별도 오류 수정 브랜치에서 고친다.
5. 최신 기준 재검증: 수정된 브랜치에서 전체 종합 검증을 반복한다.
6. 제한적 라이브 테스트: 기본 비활성 상태와 테스트 전용 활성 상태를 분리해 실행한다.
7. 라이브 실패 처리: 로그·runtime ID·entity UUID·종료 reason을 기준으로 오류 위치를 특정하고 최소 수정만 반영한다.
8. 라이브 재검증: 실패 항목과 관련 회귀 항목을 재실행한다.
9. 기준선 동결: 정적·라이브 결과와 미검증 목록을 기록한 뒤에만 영지 단계로 이동한다.

## 이번 단계의 실행 결과

- 새로운 Java/YAML 기능 추가: 없음
- 기준선 외 설정 변경: 없음
- 영지 구현: 없음
- Gradle clean build: 실행하지 않음
- 자동 테스트: 실행하지 않음
- 실제 서버 테스트: 실행하지 않음
- GitHub Actions workflow: 별도 실행 확인 필요
- 최종 상태: BUILD_VERIFICATION_REQUIRED
- 최종 상태: LIVE_SERVER_VERIFICATION_REQUIRED

이 브랜치는 E1-E8을 영지 이전 검증 기준으로 동결하기 위한 문서 브랜치다. 영지 작업은 정적 종합 검증과 라이브 매트릭스가 완료되기 전에는 시작하지 않는다.
