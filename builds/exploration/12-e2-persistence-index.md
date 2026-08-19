# E2 Persistence·YAML·Index 및 E1 회귀 감사

기준: E1 `codex/exploration-e1-registry-recheck` @ `65850dd8550d2c933f85e1010db0fef5ce10d911`  
작업 브랜치: `codex/exploration-e2-persistence-index`

## E2 범위

- world별 YAML snapshot load/save
- schema/world UUID 검증
- malformed record fail-closed
- repository load retry 경계
- StructureIndex의 bounds 재인덱싱과 world 격리
- E1의 VANILLA/RPG/variant 최초 판정 보존

E3의 trigger/activation/runtime 생성은 건드리지 않는다.

## E1 회귀 점검

- E1에서 추가한 비활성 등록 구조물의 VANILLA 최초 기록 정책은 유지된다.
- E1 stable ID와 StructureRecord의 RPG/variant/state 제약은 수정하지 않았다.
- E1 Registry의 key/chance 정적 검증은 그대로 유지된다.
- E1의 top-level disabled 안전 기본값은 그대로다.

## 발견 및 수정

### 1. malformed YAML record의 조용한 삭제 경로

기존 `YamlStructureStorage.loadWorld()`는 깨진 record를 로그만 남기고 건너뛰었다. 이후 flush/save가 실행되면 해당 record가 파일에서 사라질 수 있었다.

수정: 하나라도 malformed record가 있으면 `IOException`으로 전체 load를 실패시킨다. 저장 작업은 진행되지 않으며, 원본 파일을 보존한다.

### 2. load 실패 후 재시도 불가

기존 `StructureRepository.ensureWorldLoaded()`는 실제 load 전에 world를 loaded 집합에 넣었다. 한 번 실패하면 수정된 파일이 있어도 재시도하지 않았다.

수정: snapshot을 완전히 읽고 index에 반영한 뒤에만 loaded로 표시한다.

### 3. index stale entry

`StructureIndex.upsert()`가 기존 bounds를 제거하고 새 bounds를 등록하는지, 서로 다른 world의 같은 chunk 좌표가 섞이지 않는지 회귀 테스트를 추가했다.

## Registry source boundary

현재 탐험 소스에는 별도 코드 기반 built-in registry가 없고 YAML Registry가 canonical source다. 따라서 “built-in + YAML 간 중복”은 현재 병합 경로가 존재하지 않아 임의의 두 번째 registry를 만들지 않았다. 현재 적용되는 중복 방어는 YAML 내 duplicate ID와 duplicate Minecraft key rejection이다. 별도 built-in source가 추가되는 E7 이전에 병합 계약을 만들면 그때 교차 소스 중복 테스트를 추가한다.

## 검증 상태

- E1 회귀 diff 및 테스트 확인 완료
- E2 focused regression tests 추가
- 실제 Gradle/JDK 25 실행: 아직 안 함
- Paper live server: 아직 안 함

상태: `IMPLEMENTED_STATIC_VERIFIED / LIVE_SERVER_VERIFICATION_REQUIRED`
