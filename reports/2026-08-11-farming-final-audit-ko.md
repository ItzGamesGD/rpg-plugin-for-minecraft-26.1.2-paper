# HyunseoRPG 농사 최종 구조 대조 보고서

- 기준일: 2026-08-11
- 상태: 소스 구조 수정 및 자동 검증 완료
- 범위: 최신 farming ZIP 대조 후 남은 포인트 활동 경계 보완

## 1. 최종 판정

최신 소스와 농사 설계 문서를 대조한 결과, 직접 수확과 납품은 공통 `AbundancePointService`를 통해 포인트를 지급한다. 판매·가공·양조·자동화·간접 파괴에는 포인트 지급 호출을 추가하지 않았다.

설계에 명시된 “기타 등록 농사 활동”은 임의의 이벤트가 포인트를 지급하지 않도록 명시적 등록 경계를 추가했다. 현재 등록된 기타 활동은 없으므로 기존 게임 플레이의 포인트 총량은 변하지 않는다.

## 2. 최종 수정 사항

`src/main/java/com/hyunseo/hyunseorpg/farming/AbundancePointService.java`에 다음 API를 추가했다.

- `registerFarmingActivity(String activityId)`
- `unregisterFarmingActivity(String activityId)`
- `isFarmingActivityRegistered(String activityId)`
- `addRegisteredActivityPoints(UUID playerId, String activityId, long amount)`

활동 ID는 앞뒤 공백 제거 후 소문자로 정규화한다. 등록되지 않은 ID는 포인트를 지급하지 않는다. 따라서 판매, 가공, 양조 등에서 잘못된 source 이름을 넘겨 포인트가 발생하는 경로를 허용하지 않는다.

포인트 지급 원칙은 다음과 같다.

| 경로 | 처리 |
|---|---|
| 정상 직접 수확 | 지급 가능 |
| 납품 완료 | 지급 가능 |
| 등록된 기타 농사 활동 | 등록 후 지급 가능 |
| 판매 | 직접 지급 없음 |
| 가공 | 직접 지급 없음 |
| 양조 | 직접 지급 없음 |
| 물·피스톤·폭발 등 간접 파괴 | 지급 없음 |
| 자동화 | 지급 없음 |

## 3. 코드·설계 이중 검증

### 코드 검증

- 직접 수확 호출은 `HarvestService`에서 `Source.HARVEST`를 사용한다.
- 납품 호출은 `DeliveryService`에서 `Source.DELIVERY`를 사용한다.
- `FARMING_ACTIVITY`는 등록 API를 통과하지 않으면 지급할 수 없다.
- 포인트는 `PlayerRPGData`에 저장되며 별도 플레이어 파일을 만들지 않는다.
- 저장 실패 시 기존 포인트로 복구한다.

### 설계 검증

- 직접 수확과 납품을 모두 공급원으로 인정한다.
- 기타 활동은 등록된 활동만 허용하여 향후 확장 지점을 보존한다.
- 판매·가공·양조를 포인트 공급원으로 임의 확정하지 않는다.
- 포인트 수치와 배율은 밸런스 단계까지 확정하지 않는다.
- 농사 단계, 괭이 단계, 품질, 가공 비율, 배달 시간 모델은 이번 수정에서 변경하지 않는다.

## 4. 최신 대조에서 확인된 상태

- 씨앗 상점과 작물 해금 검사는 연결되어 있다.
- 괭이 단계와 플레이어 농사 단계는 분리된 흐름을 사용한다.
- 가공 레시피는 20:1 구조와 `farming/processing.yml` 계약을 사용한다.
- 요리 기능은 현재 활성 제작 경로에서 비활성 상태다.
- `refresh-seconds`와 의뢰 `time-limit-seconds`는 분리되어 있다.
- 희귀 씨앗 실제 지급은 아직 구현하지 않은 확장 항목이다.
- 증표는 기본 리소스에서 비활성 상태다.
- 기존 플레이어 농사 데이터는 명시적 migration 없이 자동으로 상위 버전으로 저장하지 않는다.

## 5. 미검증 및 후속 항목

- 실제 서버에서 새 JAR 적용 후 직접 수확·납품 포인트의 재접속 및 재시작 보존 확인
- 실제 관리자 명령 또는 후속 모듈에서 활동 ID를 등록한 뒤 한 번만 지급되는지 확인
- 포인트 수치, 괭이 배율, 품질·멀티플 드롭의 밸런스 확정
- 외부 `shops.yml`, `crafting.yml`, `farming/` 설정은 백업 후 명시적 migration 적용 필요
- 대규모 농장 환경의 저장 성능은 별도 부하 테스트 필요

## 6. 빌드 및 산출물

- 자동 테스트: `122 tests / 0 failures / 0 skipped` PASS
- 테스트 리포트 생성 시각: 2026-08-11 18:20
- 후속 재실행은 코드 오류가 아니라 Maven Central의 `sqlite-jdbc:3.46.1.0` 다운로드 권한/SSL 오류로 의존성 확인 단계에서 중단됨
- `libs` 디렉터리: 변경하지 않음
- 최종 JAR: `builds/HyunseoRPG-0.1.0-SNAPSHOT-farming-final-audit-hotfix.jar`
- SHA-256: `3086DB9DFD874DA4B62200111D57ECC649EBA0E3851FB0D510172037B255409E`
