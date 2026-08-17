# U11 Prompt 13 플레이테스트·밸런스 단계 보고서

## 상태

기존 소스의 운영 경계를 유지한 채 U11 측정 계약 테스트와 보고 양식을 추가했다.
실제 Paper 서버 측정값이 없으므로 최종 수치 상태는 모두 `BALANCE_PENDING`이며,
이번 단계에서 production 활성화나 밸런스 수치 확정은 하지 않았다.

## 설계 대조

Prompt 13의 핵심 규칙인 “BUG를 밸런스 관찰과 분리”, “반복 가능한 원자료 기록”,
“실측 없는 PASS 금지”를 적용했다.

| 설계 계약 | 실제 코드 확인 | 판정 |
|---|---|---|
| 효과 직접 적용 | `EffectService.applyDebug`와 `/rpg effect apply` | 연결 확인 |
| 효과 제거·전체 정리 | `EffectService.remove`, `clearAndReport`, `clearTarget` | 연결 확인 |
| 작업 정리 | 특수 촉매 실행 서비스의 중복·월드 취소 및 `clearjobs` | 연결 확인 |
| GUI 원자 처리 | `AlchemyGuiControllerService`와 기존 `CraftingTransactionService` 경계 | 구조 확인, 실서버 미검증 |
| 생산 물약·촉매 활성화 | 기본 YAML에서 비활성 | 수치·운영 승인 전 보류 |
| 재접속·재시작 잔존 modifier 정리 | `EffectService.onJoin`의 소유 modifier 제거 | 코드 확인, 기존 플레이어 실서버 미검증 |

## 수치·콘텐츠 정책

다음 값은 입력하지 않았다.

- 효과 지속시간·강도·중첩
- PvP·보스 배율
- 촉매 지속시간·강도·전파·튕김·지연
- 풍요의 정수 교환량 및 부식의 정수 생산량
- 생산 물약 재료량과 제작 비용

production effect, potion, catalyst, abundance 정의는 플레이테스트 증거가 생기기 전까지
비활성 및 `BALANCE_PENDING`을 유지한다. 테스트용 `effect_test_speed`와 debug 입력값은
production balance로 저장하지 않는다.

## 반복 테스트 기록 양식

각 행은 하나의 서버 실행과 하나의 반복 조건을 의미한다.

| 항목 | 기록값 |
|---|---|
| 빌드/JAR SHA-256 | 입력 필요 |
| Paper 버전 | 입력 필요 |
| 서버 인원 | 입력 필요 |
| 테스트 월드/시드 | 입력 필요 |
| 측정일/테스터 | 입력 필요 |
| 시작 TPS / 측정 중 TPS / 종료 TPS | 입력 필요 |

### 통합 루프

| 시나리오 | 측정 항목 | 결과 |
|---|---|---|
| 농사 → 풍요 포인트 → 정수 → 물약 | 시간, 포인트, 재료, 제작 성공 | `DEFERRED`: 운영 콘텐츠 비활성 |
| 정수 + 바닐라 재료 → 부식 정수 → 디버프 물약 | 교환 시간, 결과량, 사용률 | `DEFERRED`: 운영 콘텐츠 비활성 |
| 물약 + 바닐라 촉매 | 적용률, 지속시간, 강도, 전달 성공률 | `DEFERRED`: 촉매 비활성 |
| 물약 + 특수 촉매 | 대상 수, 적중 수, 중복 작업, 월드 이동 취소 | `DEFERRED`: 특수 촉매 비활성 |

### 실패 분류

- `BUG`: 손실, 복제, 잔존 modifier/job, 잘못된 대상, 우회, 충돌, 예외. 밸런스 조정보다 먼저 수정한다.
- `BALANCE_OBSERVATION`: 비용, 지속시간, 강도, 사용성에 대한 실측 관찰. 수치를 즉시 확정하지 않는다.
- `DEFERRED`: 현재 실행 환경 또는 비활성 콘텐츠 때문에 증거를 만들 수 없다.

## 코드 구조 검증

구현은 기존 Registry·Service·GUI 경계를 재사용한다. U11에서 새 Potion/Effect/Catalyst
Registry나 별도 저장소를 만들지 않았다. 테스트는 리소스 상태가 수치 확정으로 바뀌거나
비활성 production 콘텐츠가 우발적으로 노출되는 회귀를 차단한다.

남은 구조 위험:

1. 여러 Registry의 reload를 하나의 전역 원자 snapshot으로 교체하는 것은 아직 아니다.
2. 실제 GUI 클릭, 전체 인벤토리, 재접속·재시작, 월드 이동, TPS는 Paper 서버에서 확인해야 한다.
3. 비활성 콘텐츠의 효과·물약·촉매 수치는 코드 테스트만으로 밸런스 적합성을 판단할 수 없다.

## 테스트 결과

- U11 계약 테스트: 추가 완료.
- 전체 자동 테스트: `143 tests / 0 failures`.
- Paper 실서버 플레이테스트: 미실행.
- 밸런스 확정: 하지 않음.

## 빌드

- JAR: `builds/alchemy/HyunseoRPG-0.1.0-SNAPSHOT-alchemy-u11-playtest-contract.jar`
- SHA-256: `254C5E351F354A19B712CB9C8F665DD06BFA38A0C9A40DEFF9DD15945B593A82`
- 테스트 전용 변경은 JAR의 production class/resource를 변경하지 않으므로 U10 GUI JAR과 동일한 SHA가 될 수 있다.

## 다음 실서버 순서

1. 동일 빌드 SHA와 서버 버전을 기록한다.
2. 테스트 월드에서 debug effect 직접 적용·제거·재접속을 측정한다.
3. full inventory, duplicate click, reload, restart, world change를 반복한다.
4. 활성화 승인된 콘텐츠만 별도 테스트하고, 각 결과를 `BUG`/`BALANCE_OBSERVATION`/`DEFERRED`로 분류한다.
5. 반복 측정이 끝난 뒤에만 YAML 수치 변경을 별도 승인 단계로 진행한다.
