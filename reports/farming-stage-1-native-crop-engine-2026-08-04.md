# HyunseoRPG 농사 Stage 1 구현 보고서

작성일: 2026-08-04  
작업 상태: `STAGE 1 IMPLEMENTED`  
자동 검증: `PASSED`  
실제 서버 플레이 검증: `PENDING`  
밸런스 검증: `NOT IN SCOPE`

## 1. 적용한 최종 지침

이번 구현은 승인된 Stage 0 설계를 기준으로 하되, 이후 전달된 정정사항을 우선 적용했다.

- 옥수수 상·하단이 서로 다른 청크에 속한다고 가정하는 로직을 넣지 않았다. Stage 1의 모든 작물은 단일 블록이며, Stage 2에서 옥수수 2블록 표현을 추가하더라도 하단만 canonical 좌표로 사용한다.
- 농사 설정은 단일 `farming.yml`로 합치지 않았다.
  - `plugins/HyunseoRPG/farming/crops.yml`
  - `plugins/HyunseoRPG/farming/growth.yml`
- 품질 작물과 품질별 아이템은 구현하지 않았다. 향후 최종 품질 아이템은 canonical `item_id`, `farming_crop_id`, `farming_quality`, `farming_crop_data_version`을 함께 보유하는 방향으로 남겨두었다.
- 기존 `ActivityBlockRepository`와 `ActivityBlockRewardValidator`를 커스텀 작물 저장소나 핵심 검증기로 사용하지 않았다.
- 커스텀 작물은 별도 `CropIndex`와 `CropHarvestValidator`로 판정하며, 활동 코인과 마석 파편 보상 경로에서 제외했다.
- 기존 `farming-speed` 옵션은 커스텀 작물 성장에 연결하지 않았다.
- 저장 상위 계층은 `CropStorage` 인터페이스에 의존하고, 현재 구현은 `YamlChunkCropStorage`로 분리했다.
- 블록 표현과 논리 작물 상태 사이에 `CropBlockAdapter` 경계를 추가했다.

## 2. Stage 1 구현 범위

### 구현된 항목

- `CropRegistry`
- `CropDefinition`
- `CropInstance`
- `CropPosition`
- `CropIndex`
- `CropStorage`
- `YamlChunkCropStorage`
- `CropGrowthService`
- `CropBlockAdapter`
- `VanillaCropBlockAdapter`
- `CropHarvestValidator`
- 씨앗 설치
- 성장 타이머와 단계 갱신
- 청크 로드·언로드 저장 및 복원
- 서버 종료 시 저장
- 설정 로드·캐시·reload 연결
- 손상되거나 불일치한 저장 데이터의 항목 단위 건너뛰기

### 의도적으로 구현하지 않은 항목

- 플레이어 `FarmingProfile` 저장
- 작물 해금
- 품질 판정
- 품질별 아이템
- 괭이 강화·승급 연동
- 판매·가공·요리
- 풍요의 정수·증표
- 범위 수확
- 커스텀 작물 수확 보상
- 옥수수 2블록 표현

## 3. 동작 구조

### 3.1 설정

내장 기본 설정은 다음 두 파일로 제공한다.

- `src/main/resources/farming/crops.yml`
- `src/main/resources/farming/growth.yml`

Stage 1에는 다음 4종을 등록했다.

| Crop ID | 씨앗 Item ID | 일반 작물 Item ID | 임시 표현 블록 | 2블록 |
|---|---|---|---|---|
| `corn` | `seed_corn` | `crop_corn` | `WHEAT` | `false` |
| `onion` | `seed_onion` | `crop_onion` | `CARROTS` | `false` |
| `chili` | `seed_chili` | `crop_chili` | `BEETROOTS` | `false` |
| `garlic` | `seed_garlic` | `crop_garlic` | `POTATOES` | `false` |

성장 시간은 `growth.yml`의 작물별 `seconds-per-stage`에서 읽는다. 현재 기본값은 Stage 1 동작 확인용으로 60초이며, 최종 경제·성장 밸런스 값이 아니다.

### 3.2 설치

플레이어가 설정된 토양 블록을 바라보고 주손에 canonical 씨앗을 들고 우클릭하면 다음을 검증한다.

1. ItemRegistry/PDC에서 씨앗 `item_id` 확인
2. CropRegistry에서 작물 정의 조회
3. 설정된 토양과 대상 AIR 확인
4. CropIndex에 하단 위치 등록
5. CropBlockAdapter를 통해 Stage 0 표현 배치
6. 씨앗 1개 차감
7. 해당 청크 dirty 표시

해금, 플레이어 농사 레벨, FarmingProfile은 검사하지 않는다.

### 3.3 성장

동기 스케줄러가 로드된 CropIndex만 확인하고, `nextGrowthAt`이 지난 작물만 갱신한다. 성장 단계가 마지막에 도달하면 성장을 중지하고 현재 상태를 보존한다.

월드 블록 변경은 Bukkit 메인 스레드에서 수행한다. 미등록 정의, 다른 월드, 다른 청크, 잘못된 단계, 표현 블록 불일치가 발견되면 해당 작물만 제거하고 로그를 남긴다.

### 3.4 저장 및 복원

청크별 파일 경로:

```text
plugins/HyunseoRPG/farming/crops/<world-uuid>/<chunk-x>_<chunk-z>.yml
```

각 파일에는 schema version, world UUID, chunk 좌표와 crop ID, 위치, 단계, planted-at, next-growth-at, data version을 저장한다.

- ChunkLoadEvent: 해당 청크 파일을 메모리로 복원
- ChunkUnloadEvent: 해당 청크 상태 저장 후 인덱스 제거
- 플러그인 종료: 로드된 청크 상태 저장 후 storage 종료
- 임시 파일 작성 후 atomic move를 우선 사용
- 잘못된 항목은 전체 파일이 아니라 항목 단위로 건너뜀
- 현재는 안전성을 위해 YAML 파일 쓰기를 동기 처리한다. 향후 데이터 DTO snapshot과 비동기 파일 큐로 교체할 수 있도록 상위 계층과 구현체를 분리했다.

## 4. 활동 보상과의 분리

`CropHarvestValidator`는 CropIndex 등록 여부만 판정하는 독립 계층이다.

- `ActivityCoinListener`: CropIndex 등록 작물 파괴를 즉시 제외
- `MagicStoneFragmentService`: CropIndex 등록 작물 파괴를 즉시 제외
- `ActivityBlockRepository`: 커스텀 작물 상태를 저장하지 않음
- Stage 1에서는 커스텀 작물 수확 자체를 취소하여 바닐라 드롭 및 기존 auto-replant 경로가 작동하지 않게 보호함

따라서 커스텀 작물은 현재 활동 코인과 마석 파편을 지급하지 않는다. 수확과 품질 보상은 후속 단계의 `CropHarvestValidator` 확장 지점에서 별도로 연결해야 한다.

## 5. 변경 파일

### Java

- `src/main/java/com/hyunseo/hyunseorpg/farming/CropPosition.java`
  - 월드 UUID와 블록 좌표를 보유하는 canonical 위치 값 객체
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropInstance.java`
  - 작물의 성장 단계와 시간 상태
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropDefinition.java`
  - crops.yml 기반 작물 정의
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropIndex.java`
  - 월드·청크·canonical 위치 기반 인메모리 인덱스
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropStorage.java`
  - 저장 구현체 교체를 위한 인터페이스
- `src/main/java/com/hyunseo/hyunseorpg/farming/YamlChunkCropStorage.java`
  - 청크별 YAML 저장 및 안전 복원
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropRegistry.java`
  - 별도 crops.yml/growth.yml을 읽고 ItemRegistry와 ID를 검증
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropBlockAdapter.java`
  - 논리 작물과 블록 표현 사이의 필수 경계
- `src/main/java/com/hyunseo/hyunseorpg/farming/VanillaCropBlockAdapter.java`
  - Stage 1 임시 바닐라 Ageable 표현
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropHarvestValidator.java`
  - 활동 보상과 분리된 커스텀 작물 판정
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropGrowthService.java`
  - 설치·성장·청크 생명주기·저장·복원 이벤트 통합 서비스
- `src/main/java/com/hyunseo/hyunseorpg/config/ConfigService.java`
  - farming/crops.yml 및 farming/growth.yml 로드·reload 추가
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
  - 서비스 생성, Listener 등록, 시작·종료·reload 연결
- `src/main/java/com/hyunseo/hyunseorpg/activity/ActivityCoinListener.java`
  - CropIndex 작물 보상 제외
- `src/main/java/com/hyunseo/hyunseorpg/progression/MagicStoneFragmentService.java`
  - CropIndex 작물 마석 파편 보상 제외

### YAML 및 테스트

- `src/main/resources/farming/crops.yml`
- `src/main/resources/farming/growth.yml`
- `src/main/resources/items.yml`
  - Stage 1 씨앗 4종 및 일반 작물 4종의 canonical ID 연결
- `src/test/java/com/hyunseo/hyunseorpg/farming/FarmingStage1DataTest.java`
  - 설정 schema, canonical ID, Stage 1의 단일 블록 정책, CropIndex 중복 방지 검증

외부 서버의 `plugins/HyunseoRPG/farming/` 파일은 이번 작업에서 직접 수정하지 않았다. 신규 서버 또는 파일이 없는 환경에서는 ConfigService가 내장 기본값을 복사한다. 기존 외부 `items.yml`이 최신 canonical ID를 아직 갖고 있지 않다면 기존 프로젝트의 items 마이그레이션 절차를 별도로 실행해야 한다.

## 6. 기존 구조 분류

| 기존 구조 | 분류 | 처리 |
|---|---|---|
| `ActivityBlockRepository` | KEEP | 설치 블록 ledger 역할 유지, 작물 저장과 분리 |
| `ActivityBlockRewardValidator` | KEEP | 기존 활동 보상 검증 유지, 커스텀 작물 핵심 판정에는 사용하지 않음 |
| `ActivityCoinListener` | EXTEND | CropHarvestValidator를 통해 커스텀 작물 제외 |
| `MagicStoneFragmentService` | EXTEND | CropHarvestValidator를 통해 커스텀 작물 제외 |
| `ConfigService` | EXTEND | farming 두 설정 파일과 reload 추가 |
| PlayerDataService | KEEP | Stage 1에서 FarmingProfile 미추가 |
| `CropStorage` | ADD | 저장 추상화 |
| `YamlChunkCropStorage` | ADD | 현재 Stage 1 저장 구현 |
| `CropIndex` | ADD | 커스텀 작물 전용 논리 인덱스 |
| `CropBlockAdapter` | ADD | 표현 계층 교체 경계 |

## 7. Custom-Crops 참고 부록

참고 파일: `C:\Users\User\Desktop\Custom-Crops-main.zip`

코드를 복사하거나 런타임 의존성으로 추가하지 않았다. 다음 파일·클래스에서 구조 원칙만 참고했다.

| Custom-Crops 실제 파일·클래스 | 참고한 원칙 |
|---|---|
| `api/src/main/java/net/momirealms/customcrops/api/core/block/CropBlock.java` | 논리 작물 블록을 별도 경계로 다루는 개념 |
| `api/src/main/java/net/momirealms/customcrops/api/core/block/CustomCropsBlock.java` | 블록 표현과 작물 상호작용을 한 계층에서 관리하는 방식 |
| `api/src/main/java/net/momirealms/customcrops/api/core/block/AbstractCustomCropsBlock.java` | 표현 구현을 추상 경계 뒤에 두는 방식 |
| `api/src/main/java/net/momirealms/customcrops/api/core/mechanic/crop/CropConfig.java` | 작물 정의를 설정 객체로 분리하는 방식 |
| `api/src/main/java/net/momirealms/customcrops/api/core/mechanic/crop/CropStageConfig.java` | 성장 단계와 단계별 상태를 분리하는 방식 |
| `api/src/main/java/net/momirealms/customcrops/api/core/mechanic/crop/VariationData.java` | 작물 표현·변형 데이터를 논리 상태와 분리하는 방식 |
| `api/src/main/java/net/momirealms/customcrops/api/core/world/CustomCropsChunk.java` | 청크 단위 작물 소유권과 조회 경계 |
| `core/src/main/java/net/momirealms/customcrops/api/core/world/CustomCropsChunkImpl.java` | 청크별 인메모리 상태 관리 방식 |
| `api/src/main/java/net/momirealms/customcrops/api/core/world/CustomCropsWorld.java` | 월드별 작물 상태 분리 |
| `core/src/main/java/net/momirealms/customcrops/api/core/world/CustomCropsWorldImpl.java` | 월드와 청크 저장소의 상위 관계 |
| `core/src/main/java/net/momirealms/customcrops/core/world/WorldManager.java` | 월드 생명주기와 로드 경계 |
| `core/src/main/java/net/momirealms/customcrops/core/data/DataBlock.java` | 저장 가능한 논리 블록 데이터 모델 |
| `core/src/main/java/net/momirealms/customcrops/core/data/SerializableChunk.java` | 청크 데이터 직렬화 경계 |
| `core/src/main/java/net/momirealms/customcrops/core/data/SerializableSection.java` | 저장 데이터 분할 원칙 |
| `core/src/main/java/net/momirealms/customcrops/core/scheduler/WorldScheduler.java` | 월드 단위 성장 작업 스케줄링 원칙 |
| `core/src/main/java/net/momirealms/customcrops/core/scheduler/DelayedTickTask.java` | due time 기반 지연 성장 작업 개념 |

Custom-Crops의 기능, 설정, API, 코드 구현을 HyunseoRPG에 그대로 이식하지 않았으며, 계절·관개·스프링클러·비료·토양·해충·농장 기계는 이번 범위에 포함하지 않았다.

## 8. 검증 결과

실행한 검증:

```text
./gradlew.bat clean build
BUILD SUCCESSFUL
6 actionable tasks: 6 executed
```

자동 검증 결과:

- Stage 1 설정·ID 테스트 통과
- CropIndex 중복 canonical 위치 방지 테스트 통과
- CropIndex 청크 조회·제거 테스트 통과
- 기존 전체 테스트 통과
- Java 컴파일 성공

기존 코드에서 발생한 경고 16건은 Paper API의 deprecated API 사용 경고이며, 이번 농사 구현으로 추가된 컴파일 오류는 없다.

## 9. 라이브 서버 미검증 항목

다음은 실제 서버에서 확인해야 한다.

1. 신규 JAR 배포 후 플러그인 활성화
2. 외부 `plugins/HyunseoRPG/items.yml`에 `seed_*`, `crop_*` 8개 ID가 실제 등록되는지 확인
3. `/rpg give seed_corn 1` 등 기존 관리자 지급 경로로 씨앗 지급
4. 경작지에 씨앗을 심었을 때 Stage 0 표현 생성 및 씨앗 1개 차감
5. 성장 시간 경과 후 단계 변경
6. 작물 블록 파괴가 취소되고 vanilla 드롭·활동 코인이 발생하지 않는지 확인
7. 청크 언로드 후 재로드 시 작물 단계와 시간 상태 복원
8. 서버 재시작 후 복원
9. 잘못된 crop ID·잘못된 좌표·깨진 YAML 항목이 다른 작물 로드를 막지 않는지 확인
10. `/rpg reload` 후 crops.yml/growth.yml과 CropRegistry가 함께 갱신되는지 확인

## 10. 후속 단계

- Stage 2: 옥수수 2블록 표현. 같은 X/Z 청크와 하단 canonical 좌표를 고정하고 상단 조회·중복 방지·불일치 복구만 추가한다.
- Stage 3: FarmingProfile 및 해금
- Stage 4: 수확·씨앗 반환·기본 수확물
- Stage 5: 괭이 강화·내구도·농사 승급
- Stage 6: 품질 판정과 canonical 품질 아이템

이번 보고서에서는 후속 기능을 구현하거나 선행 저장 구조를 임의로 추가하지 않았다.

## 11. 빌드 산출물

- JAR: `C:\Users\User\Documents\Codex\2026-06-29\hyunseorpg-paper-spigot-rpg-hyunseorpg-1\build\libs\HyunseoRPG-0.1.0-SNAPSHOT.jar`
- SHA-256: `A0DC5DD0B2880D22D73C56927C0B9E2BECAE7ADCB811DA503BDA129CA5320F47`

