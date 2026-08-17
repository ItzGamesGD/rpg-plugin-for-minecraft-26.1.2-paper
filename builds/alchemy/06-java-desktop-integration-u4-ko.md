# 양조 Java/Desktop Integration U4 보고서

범위: 풍요 포인트·정수 연계 경계

## 구현

- `AbundanceBridge`, `AbundanceActivityEvent`, `EssenceDefinition`, `EssenceRegistry` 계약을 실제 `FarmingProfileService`, `AbundancePointService`, `FarmingEssenceService`에 연결했다.
- `YamlEssenceRegistry`는 `alchemy/abundance.yml`의 snapshot만 읽고, 비활성·밸런스 보류 정의를 자동 활성화하지 않는다.
- `FarmingAbundanceBridge`는 직접 수확·등록 납품·등록 활동만 허용하고, 중복 event ID를 한 번만 수락한다.
- 정수 교환은 기존 `AbundancePointService.spend()`를 사용하며 포인트 부족·오버플로·비활성 정수는 실패한다.

## 설계 대조

설계의 “풍요 포인트는 농사 이벤트가 원장 데이터에 직접 쓰지 않고 단일 서비스 경계를 통과한다”는 조건을 충족한다. 판매·가공·양조·간접 파괴를 활동 Registry에 등록하지 않았고, 정수 정의는 현재 비활성으로 유지했다. 별도 플레이어 파일이나 새 경제를 만들지 않았다.

## 검증

U4 계약 테스트 포함 전체 `133 tests / 0 failures / 0 skipped`. 실제 수확·납품 서버 이벤트에서의 포인트 지급은 아직 LIVE TEST REQUIRED다.
