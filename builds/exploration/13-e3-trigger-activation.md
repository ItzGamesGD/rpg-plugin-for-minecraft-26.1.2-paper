# E3 Trigger·Activation 및 E1/E2 회귀 감사

기준 브랜치: `codex/exploration-e2-persistence-index`  
기준 커밋: `688a33d705df2d7dbd8c021cd81f3a40a37af288`  
작업 브랜치: `codex/exploration-e3-trigger-activation`

## 범위

- proximity trigger eligibility
- UNDISCOVERED → ACTIVE activation entry
- 이미 ACTIVE인 runtime의 participant 재합류
- E1 최초 관측 판정과 E2 persistence/index 계약 보존
- 실제 구조물 이벤트·보상·퍼즐 콘텐츠는 구현하지 않음

## E1/E2 회귀 점검

- E1의 비활성 등록 구조물 VANILLA 최초 기록 정책을 변경하지 않았다.
- E2의 malformed YAML fail-closed와 world load retry 경계를 변경하지 않았다.
- E2의 stale bounds 제거·world 격리 index 정책을 변경하지 않았다.
- E2에서 발견했던 retry 테스트 assertion은 `loadAttempts == 2`로 유지되는 것을 확인했다.
- StructureRecord의 영속 상태 전이 규칙을 우회하지 않는다.

## 구현

기존 HeartbeatTask에 흩어져 있던 trigger 조건을 `ExplorationTriggerPolicy`로 분리했다.

허용 조건:

- 구조물 정의가 enabled
- 플레이어와 구조물의 world UUID가 동일
- record state가 UNDISCOVERED 또는 ACTIVE
- 구조물 bounds까지의 거리가 trigger-radius 이내

CLEARED, ABANDONED, VANILLA, 다른 월드, 반경 밖, disabled 정의는 activation 대상이 아니다. 정책 클래스는 순수 계산만 담당하며 runtime 생성·영속화는 기존 `ExplorationRuntimeManager`가 담당한다.

## 발견한 충돌 및 처리

E3 점검 중 trigger eligibility가 heartbeat 구현 내부에 직접 존재해 다른 진입점이 추가될 경우 상태·월드·반경 조건이 달라질 수 있는 구조를 확인했다. 이번 단위에서 정책을 단일 계약으로 추출하고 focused regression test를 추가했다.

ACTIVATE 예외 시 E2 이전부터 존재하는 rollback 경계도 재확인했다.

- runtime object 제거
- UNDISCOVERED 원복 저장
- 다음 heartbeat에서 재시도 가능

## 검증 상태

- E1 → E2 → E3 변경 경로 정적 확인 완료
- E3 focused trigger policy test 추가
- 실제 Gradle/JDK 25 실행: 아직 안 함
- Paper live server: 아직 안 함

상태: `IMPLEMENTED_STATIC_VERIFIED / LIVE_SERVER_VERIFICATION_REQUIRED`
