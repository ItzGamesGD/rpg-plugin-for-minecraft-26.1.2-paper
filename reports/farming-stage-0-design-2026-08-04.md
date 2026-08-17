# HyunseoRPG 농사 시스템 1단계 보고서

## 단계 상태

- 문서 단계: `프롬프트 0 - 농사 구현 사전 조사 및 설계 고정`
- 상태: `완료`
- 실제 농사 기능 구현: `미시작`
- Java/YML 변경: `없음`
- 외부 서버 파일 변경: `없음`
- 이번 단계의 목적: 기존 구조와 충돌 지점을 확인하고 다음 단계의 책임·저장·이벤트 경계를 고정

이번 단계에서는 작물 생성, 성장, 수확, 판매, 품질, 괭이 성장, 농사 GUI를 구현하지 않았다. 문서에서 요구한 1단계만 처리했다.

## 1. 조사 기준

### 프로젝트 실행 환경

- Paper API: `26.1.2.build.72-stable`
- Java: `25`
- 플러그인 진입점: `com.hyunseo.hyunseorpg.HyunseoRPGPlugin`
- 설정 로딩: `ConfigService`
- 플레이어 저장: `PlayerDataService` + `YamlPlayerDataRepository`

### 참고 자료

- `C:\Users\User\Desktop\HyunseoRPG_농사_시스템_최종_확정안.txt`
- `C:\Users\User\Desktop\HyunseoRPG_농사_개발_프롬프트_분할본.txt`
- `C:\Users\User\Desktop\Custom-Crops-main.zip`

Custom-Crops는 런타임 의존성이나 코드 복사 대상으로 사용하지 않았다. API의 작물 정의, 블록 상태, 월드·청크 단위 저장, 지연 성장 작업 구조만 참고했다. 계절, 관개, 스프링클러, 까마귀, 비료, 토양, 해충, 농장 기계 등은 HyunseoRPG 범위에서 제외한다.

## 2. 기존 구조 조사 결과

### 2.1 플러그인 초기화 및 Registry

`HyunseoRPGPlugin.onEnable()`에서 현재 다음 순서로 핵심 서비스를 생성한다.

1. `ConfigMigrationService`
2. `ConfigService`
3. `YamlPlayerDataRepository` / `PlayerDataService`
4. `RPGItemRegistry` / `RPGItemService`
5. 제작·설치 블록·보상·강화·승급·상점 Registry
6. 인챈트·몹·퀘스트·보스 서비스
7. 리로드 서비스와 Listener 등록

현재 농사 전용 `CropRegistry`, `CropStorage`, `FarmingProfile`, `CropScheduler`는 존재하지 않는다. 기존 초기화 흐름에 농사 서비스가 들어갈 위치는 플레이어 데이터 서비스와 아이템 Registry가 준비된 뒤, Listener 등록 전으로 고정한다.

### 2.2 현재 바닐라 농사 경로

현재 농사 관련 기능은 전용 작물 시스템이 아니라 활동 보상 경로다.

- `activity/ActivityCoinListener.java`
  - 성숙 작물 파괴를 `FARMING` 활동으로 처리
  - 플레이어 설치 블록이면 보상 차단
  - `BlockPlaceEvent`에서 작물·원목을 설치 블록 저장소에 기록
  - 현재 농사 코인 보상은 외부 정책상 `0`
- `activity/ActivityBlockRewardValidator.java`
  - 설치 블록 차단
  - 허용 작물 및 성숙 상태 검사
  - 이벤트 단위 추가 보상 중복 방지
- `activity/ActivityBlockRepository.java`
  - SQLite `activity-blocks.db`에 플레이어 설치 블록 좌표 저장
- `progression/MagicStoneFragmentService.java`
  - 블록 파괴 시 공통 보상 검증기를 통해 파편 보상 판정
- `enchant/EquipmentEnchantContentService.java`
  - `auto_replant`가 바닐라 작물을 즉시 재설치

따라서 이후 농사 작물 엔진을 추가할 때 기존 `ActivityCoinListener`를 새 작물 Registry로 대체하거나 분기해야 한다. 기존 설치 블록 저장소를 작물 성장 상태 저장소로 재사용하면 안 된다. 두 저장소의 책임이 다르기 때문이다.

### 2.3 설정 파일

현재 `ConfigService`가 관리하는 목록에는 `farming.yml`이 없다. 농사 관련 현재 설정은 다음에 분산되어 있다.

- `progression-loop.yml`
  - 바닐라 `FARMING` 활동, 허용 작물, 성숙 작물 조건
  - 농사 활동 코인은 현재 `0`
- `enchants.yml`
  - `auto_replant` 허용 작물
- `items.yml`
  - 커스텀 아이템 Registry 원본
- `shops.yml`
  - 상점 상품과 구매 가능 여부
- `crafting.yml`
  - 통합 제작 GUI의 canonical 레시피
- `equipment-growth.yml`
  - 도구 계열을 포함한 기존 강화·승급 옵션

농사 전용 설정은 다음 단계에서 기존 파일에 억지로 섞지 않고 `farming.yml`을 별도 관리하는 방향으로 고정한다. 단, 실제 수확품·가공품·씨앗은 `RPGItemRegistry`가 읽는 `items.yml`에 등록해야 한다.

### 2.4 플레이어 데이터 및 저장

현재 플레이어 데이터는 다음 구조다.

- 위치: `plugins/HyunseoRPG/players/<uuid>.yml`
- 메모리: `PlayerDataCache`
- 서비스: `PlayerDataService`
- 현재 데이터 스키마: `YamlPlayerDataRepository`가 `schema-version: 4` 저장
- 저장 방식: 변경 UUID를 dirty 집합에 모은 뒤 설정된 주기로 저장
- 로그아웃·종료: 저장 후 캐시 제거
- 로드 실패: 로그를 남기고 대체 객체를 만들며, 실패한 UUID는 저장하지 않음

농사 프로필은 별도 플레이어 파일을 만들지 않고 canonical 플레이어 YAML의 `farming` 하위에 추가한다.

권장 형태:

```yaml
schema-version: 5
farming:
  schema-version: 1
  stage: BASIC
  total-valid-harvests: 0
  crop-harvests: {}
  unlocked-crops: [corn]
  stat-token-uses: {}
```

이번 단계에서는 이 필드를 추가하지 않았다. 기존 플레이어 데이터 손실 방지를 위해 실제 추가 시 읽기 기본값, 쓰기 호환, 마이그레이션을 함께 구현해야 한다.

### 2.5 커스텀 아이템과 PDC

- `RPGItemRegistry`가 `items.yml`의 `items.*`를 canonical 목록으로 읽는다.
- `RPGItemService`가 `item_id` PDC를 저장한다.
- Material은 외형 기반이며, 커스텀 아이템 식별 원본은 PDC다.
- 현재 품질이 있는 농산물 ItemDefinition은 없다.

향후 품질 아이템은 이름·lore 문자열을 파싱하지 않고 다음 PDC를 원본으로 둔다.

- `farming_crop_id`
- `farming_quality`
- 필요 시 `farming_crop_data_version`

표시 이름과 lore는 이 데이터를 읽어 생성한다. 커스텀 농산물을 모두 별도 item ID로 복제하는 방식은 품질 단계가 늘어날 때 Registry가 폭증하므로 기본 방식으로 채택하지 않는다.

### 2.6 메뉴·제작·상점·장비 성장

- 제작 진입점은 `CraftingGuiService`와 `CraftingTransactionService`다.
- 제작 레시피는 `CraftingRecipeRegistry`, 배치는 `CraftingLayoutRegistry`가 소유한다.
- 상점은 `ShopRegistry`, `ShopService`, `ShopGuiService`가 담당한다.
- 통합 메뉴는 `RPGMenuService`가 담당한다.
- 장비 성장 GUI는 `EquipmentGrowthGuiService`가 담당한다.

최종 확정안에 따라 초기 농사 구현에는 별도 농사 GUI를 만들지 않는다.

- 씨앗 구매: 기존 상점 GUI
- 가공·요리: 기존 통합 제작 GUI
- 농사 진행 표시: 기존 통합 메뉴 또는 기존 상태 표시 확장
- 괭이 강화·승급: 기존 장비 성장 GUI 재사용

상점의 현재 상품 모델에는 농사 단계 판정이 전용으로 존재하지 않는다. 다음 단계에서 `ShopTransactionService` 진입 전에 공용 `FarmingUnlockService`를 호출하는 확장 지점이 필요하다. GUI에서만 숨기고 서비스 호출을 허용하는 우회는 만들지 않는다.

## 3. 설계 고정안

### 3.1 권장 패키지

다음 패키지를 새 농사 공용 계층으로 사용한다. 이번 단계에서는 클래스를 생성하지 않았다.

```text
com.hyunseo.hyunseorpg.farming
├─ CropRegistry
├─ CropDefinition
├─ CropInstance
├─ CropPosition
├─ CropIndex
├─ CropStorageService
├─ CropGrowthService
├─ CropWorldListener
├─ CropRecoveryService
├─ FarmingProfile
├─ FarmingProfileService
├─ FarmingUnlockService
└─ hook/
   ├─ BeforeHarvestHook
   ├─ AfterHarvestHook
   ├─ ModifyBaseDropHook
   ├─ ModifySeedDropHook
   ├─ ModifyDurabilityHook
   ├─ ModifyQualityRollHook
   ├─ ModifyCollectionHook
   └─ ReplantHook
```

### 3.2 CropDefinition

작물 정의는 설정 기반이며 최소한 다음 필드를 가진다.

- canonical crop ID
- 표시 이름
- 씨앗 item ID
- 수확물 item ID
- 성장 단계 수
- 단계별 표현 방식
- 성장 시간
- 2블록 작물 여부
- 허용 토양
- 해금 단계
- 기본 수확량
- 씨앗 반환량
- 품질 허용 여부
- 가공품 연결 ID
- 간접 파괴 정책
- 데이터 버전

정의는 `farming.yml`의 `crops.*`에서 읽고, 실제 item 존재 여부는 `RPGItemRegistry`로 검증한다.

### 3.3 CropInstance

월드에 심어진 하나의 작물 상태다.

- crop ID
- canonical 기준 위치
- world UUID
- chunk 좌표
- 현재 성장 단계
- 심은 시각
- 마지막 성장 판정 시각
- 데이터 버전
- 2블록 상단 위치 참조 여부

`Block`, `World`, `ItemStack` 객체를 저장 데이터에 보관하지 않는다. 저장에는 UUID·좌표·문자열·정수·시각만 사용한다.

### 3.4 CropIndex

작물 탐색을 위해 월드 전체를 검색하지 않는 인메모리 인덱스다.

- 키: `world UUID + chunk X + chunk Z`
- 값: 해당 청크의 `CropInstance` 목록
- 기준 위치: 작물 하단 블록 한 곳
- corn은 하단만 canonical entry로 기록
- 상단 블록은 하단 인스턴스에서 파생

청크 로드 시 해당 파일을 메모리에 올리고, 청크 언로드 시 dirty snapshot을 저장한다. 서버 시작 시 모든 월드와 모든 블록을 전수 스캔하지 않는다.

### 3.5 CropStorageService

초기 저장은 다음 구조가 가장 안전하다.

```text
plugins/HyunseoRPG/farming/crops/<world-uuid>/<chunk-x>_<chunk-z>.yml
```

파일 예시:

```yaml
schema-version: 1
world-uuid: ...
chunk-x: 12
chunk-z: -4
crops:
  - id: corn
    base: {x: 100, y: 64, z: 200}
    stage: 2
    planted-at: 0
    last-growth-at: 0
    data-version: 1
```

저장 원칙:

1. 메인 스레드에서 안전한 DTO snapshot 생성
2. 임시 파일 작성
3. flush
4. 원자적 교체
5. 실패 시 dirty 유지 및 재시도

비동기 작업에서는 Bukkit `World`, `Block`, `ItemStack`, `ItemMeta`를 읽거나 변경하지 않는다.

### 3.6 FarmingProfile

기존 `PlayerRPGData`에 다음 논리 데이터를 추가한다.

- farming schema version
- farming stage
- 유효 직접 수확 누적량
- 작물별 직접 수확량
- 해금 작물 목록
- 이후 스탯 토큰 사용 기록

단계는 문서 확정값을 사용한다.

- `BASIC` = 기본
- `SKILLED` = 숙련
- `PROFICIENT` = 능숙
- `ADVANCED` = 상급
- `EXPERT` = 전문

초기 해금 방향:

- BASIC: corn
- SKILLED: iron 도구 + onion
- PROFICIENT: diamond 도구 + chili
- ADVANCED: netherite 도구 + garlic
- EXPERT: 후속 콘텐츠

해금 판정은 심을 때만 검사한다. 씨앗은 거래·보관 가능하며, 미해금 씨앗을 심으면 이벤트를 취소하고 씨앗을 반환한다.

### 3.7 Corn 2블록 표현

corn은 반드시 하단 위치를 canonical 기준으로 하고 상단을 종속 상태로 취급한다.

파괴 규칙:

- 하단 파괴: 상단 포함 한 번만 수확 처리
- 상단 파괴: 하단을 찾아 한 번만 수확 처리
- 하단·상단 동시 이벤트: 실행 ID 또는 위치 lock으로 중복 방지
- Stage 0 정정: 옥수수 상·하단은 같은 X/Z 청크에 속한다. Stage 1에서는 2블록 표현을 구현하지 않으며, 향후 Stage 2에서도 하단만 canonical 좌표로 사용하고 같은 청크 안에서 상단 조회·중복 방지·불일치 복구만 처리한다.

현재 프로젝트에는 커스텀 블록 표현 계층이 없으므로, 하단·상단에 사용할 실제 Bukkit Material/BlockData는 아직 확정하지 않는다. 임의의 바닐라 블록을 영구 식별자로 사용하면 수동 설치·교체를 구분하기 어렵기 때문이다.

따라서 다음 단계 시작 전 `CropBlockAdapter` 역할을 별도로 두고, 블록 외형과 논리 작물 인스턴스를 분리한다. 실제 Material은 `farming/crops.yml`의 crop definition으로 지정하고, 인덱스가 최종 식별 원본이 되도록 한다.

### 3.8 동기·비동기 경계

메인 스레드 전용:

- 씨앗 심기·블록 교체
- 성장 단계 블록 반영
- 블록 파괴·수확
- 인벤토리 차감·지급
- 작물 BlockData 판정
- Bukkit 이벤트와 플레이어 상태 접근

비동기 허용:

- DTO snapshot 직렬화
- YAML 임시 파일 작성
- atomic move
- 순수 보상·품질 계산
- 로그용 통계 계산

성장 타이머는 매 틱 전체 작물을 순회하지 않는다. 청크별 다음 성장 시각 큐 또는 낮은 빈도의 due-index를 사용하고, 실제 블록 변경은 메인 스레드에서 수행한다.

## 4. 기존 코드 분류

| 기존 구조 | 분류 | 판단 |
|---|---|---|
| `PlayerDataService` / `PlayerRPGData` | EXTEND | farming profile을 canonical player data에 추가 |
| `YamlPlayerDataRepository` | EXTEND | farming 하위 YAML read/write와 schema migration 추가 |
| `RPGItemRegistry` / `RPGItemService` | EXTEND | 씨앗·작물·가공품 item 정의와 PDC 연결 |
| `ActivityBlockRepository` | KEEP | 플레이어 설치 블록 보상 차단용으로 유지 |
| `ActivityBlockRewardValidator` | EXTEND | 후속 custom crop 간접 파괴 검증과 연결 |
| `ActivityCoinListener` | MIGRATE | custom crop 수확과 바닐라 활동을 분리하고 농사 코인 지급은 계속 사용하지 않음 |
| `EquipmentEnchantContentService.auto_replant` | EXTEND | custom crop ReplantHook과 충돌하지 않게 분기 |
| `CraftingRecipeRegistry` / `CraftingTransactionService` | KEEP/EXTEND | 가공·요리 후속 단계에서 재사용 |
| `ShopRegistry` / `ShopService` | EXTEND | 구매 트랜잭션에 farming unlock 판정 연결 |
| `EquipmentGrowthGuiService` | EXTEND | 괭이 성장 정책을 기존 GUI에 연결 |
| `RPGMenuService` | KEEP/EXTEND | 별도 농사 GUI 없이 표시·진입 경로 확장 |
| `ConfigService` | EXTEND | `farming.yml` 관리와 reload 등록 |
| `ConfigMigrationService` | EXTEND | farming schema와 외부 설정 보완 |
| `ConfigDoctor` | EXTEND | 작물 ID·item ID·성장 시간·청크 데이터 검증 |
| Custom-Crops 코드 | REMOVE from runtime | 참고만 하고 의존성·복사 구현 금지 |

## 5. 충돌·위험 지점

1. `ActivityCoinListener`가 바닐라 작물 파괴를 직접 처리하므로 custom crop 이벤트를 같은 경로에 넣으면 중복 보상이 발생할 수 있다.
2. `MagicStoneFragmentService`도 블록 파괴를 관찰하므로 custom crop은 공통 보상 검증을 거쳐야 한다.
3. `auto_replant`가 vanilla BlockData를 직접 재설치하므로 custom crop과 별도 분기하지 않으면 수확 상태가 꼬일 수 있다.
4. `ActivityBlockRepository`는 설치 블록 ledger이지 성장 작물 저장소가 아니다. 두 데이터를 같은 SQLite 테이블에 섞지 않는다.
5. Player YAML은 현재 스키마 4이며 저장은 dirty 배치 방식이지만 파일 직렬화가 동기 경로다. farming 상태를 추가할 때 Bukkit 객체를 저장 모델에 넣지 않는다.
6. 상점은 현재 일반 `purchasable` 판정 중심이므로 농사 단계 잠금을 GUI에만 구현하면 우회가 생긴다.
7. Stage 2에서 옥수수 상·하단 표현을 추가하더라도 같은 X/Z 청크 전제와 하단 canonical 정책을 유지해야 한다.
8. 물, 피스톤, 폭발, 블록 교체, 농지 파괴로 논리 데이터와 실제 블록이 달라질 수 있다.
9. 서버 종료 중 성장 작업과 저장 작업이 겹치면 작물 한 번 성장 또는 수확이 중복될 수 있다.
10. 현재 커스텀 아이템 생성 시 normalizer가 여러 번 연결되므로 농산물 PDC를 재생성 과정에서 잃지 않도록 해야 한다.

## 6. 단계 의존성

이번 단계 이후 실제 구현 순서는 다음으로 고정한다.

1. **Stage 1: Native Crop Engine**
   - `farming/crops.yml`, `farming/growth.yml`
   - CropRegistry/Definition/Instance/Position/Index
   - 심기·성장·청크 load/unload·복구·버전·손상 안전 처리
2. **Stage 2: Corn 2-block 및 복구 보강**
3. **Stage 3: FarmingProfile 및 해금**
4. **Stage 4: 직접 수확·씨앗·기본 수확물**
5. **Stage 5: 괭이 강화·내구도·농사 승급**
6. **Stage 6: 품질 판정과 품질 아이템**
7. **Stage 7: 판매·가공·요리**
8. **Stage 8: 풍요의 정수·스탯 토큰·후속 연금/원소 장비 연결**

Stage 1에는 품질, 괭이 효과, 승급, 판매, 처리, 요리, 풍요의 정수, 스탯 토큰, 범위 수확을 포함하지 않는다.

## 7. 이번 단계 검증 결과

- 최종 확정안 및 분할 프롬프트의 Stage 0 범위 확인: 통과
- 기존 바닐라 농사 이벤트·설치 블록 검증 조사: 통과
- 플레이어 저장·아이템 PDC·상점·제작·장비 성장 경로 조사: 통과
- Custom-Crops ZIP의 참고 API 구조 확인: 통과
- Java 소스 변경: 없음
- 설정 변경: 없음
- 외부 서버 변경: 없음
- 실제 농사 플레이 테스트: 해당 없음
- Stage 1 기능 테스트: 아직 시작하지 않음

## 8. UNRESOLVED

다음 항목은 Stage 1 시작 전에 별도 결정 또는 설정으로 고정해야 한다.

- Stage 2에서 사용할 corn 상·하단의 실제 Material/BlockData와 시각 표현
- custom crop 블록의 논리 식별 방식과 수동 블록 교체 판정
- `farming/crops.yml` 및 `farming/growth.yml`의 기본 성장 시간·토양·드롭 수치
- 씨앗·작물·가공품의 실제 item ID와 custom-model-data
- 플레이어 YAML schema 4에서 farming schema 5로의 정확한 마이그레이션 방식
- 농사 단계 잠금이 상점 구매와 씨앗 심기에 적용되는 공통 API
- 기존 `farming-speed` 강화 옵션의 의미를 custom crop 성장 속도에 연결할지 여부
- 농사 전용 블록 데이터가 `ActivityBlockRepository`와 어떤 우선순위로 공존할지

## 최종 결론

문서상 1단계인 사전 조사와 설계 고정만 완료했다. 현재 저장소에는 농사 전용 작물 엔진이 없으며, 구현을 시작할 기준 구조는 확정했다. 다음 작업은 Stage 1 Native Crop Engine만 진행해야 하며, 그 단계에서도 품질·판매·괭이 성장·요리·정수·토큰을 함께 구현하지 않는다.
