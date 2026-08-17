# HyunseoRPG 농사 구조 정밀 대조 핫픽스 보고서

기준: 최신 `src/main`, 기본 YML, 기존 테스트
상태: `SOURCE_DEFAULT_UPDATED` / `EXTERNAL_CONFIG_MIGRATION_REQUIRED`

## 1. 수정 요약

### 씨앗 구매 루프

- 농사 상점에 `seed_corn`, `seed_onion`, `seed_chili`, `seed_garlic`을 추가했다.
- 각 상품은 `required-farming-crop`으로 작물 ID를 가진다.
- `ShopService.buy()`가 구매 직전에 FarmingProfile 준비 상태와 작물 해금 여부를 검사한다.
- 미해금 씨앗 구매는 차단하며 코인과 인벤토리는 변경하지 않는다.
- 기본 가격은 현재 밸런스 확정값이 아니라 임시 설정이다: 옥수수 5, 양파 10, 고추 15, 마늘 20.

### 괭이 승급 분리

- 승급 대상 단계 계산을 플레이어 FarmingStage가 아니라 괭이의 item-local tier 기준으로 변경했다.
- tier 0 → SKILLED, tier 1 → PROFICIENT, tier 2 → ADVANCED, tier 3 → EXPERT로 매핑한다.
- `ordinal() + 1`을 제거해 첫 승급이 tier 2가 되던 오프바이원을 수정했다.
- 플레이어의 단계·해금 데이터는 괭이 승급에서 변경하지 않는다.
- Expert 플레이어가 새 tier 0 괭이를 성장시키는 경로를 열었다.
- 괭이 승급 서비스가 연결되지 않은 경우 성공처럼 처리하지 않고 미리보기를 거부한다.

이제 플레이어 진행도와 괭이 성장은 별도 데이터다. 플레이어 해금은 `FarmingProfileService`의 별도 API와 관리자/향후 진행 트랜잭션이 담당한다.

### 가공

- 기본 `crafting.yml`의 20개 `process_*` 레시피를 작물 20개 → 가공품 1개로 변경했다.
- 기존 외부 `crafting.yml`에 입력량이 정확히 1로 남아 있는 레거시 가공 레시피는 `migrate farming --apply`에서 20으로 보정한다.
- `farming/processing.yml`을 추가했다. 레시피를 중복 저장하지 않고 `crafting.yml`이 유일한 실행 원본임을 선언한다.
- Doctor가 processing 계약과 모든 가공 레시피의 20:1 입력 비율을 검사한다.

### 요리

- 요리 기능은 활성 기능으로 노출하지 않는다.
- Farming Hub 버튼을 `요리 (비활성)`으로 변경했고 클릭 동작을 제거했다.
- 기존 `cooking.yml`, Registry, 클래스와 아이템은 기존 데이터 손상을 피하기 위해 보존하되 활성 제작 경로에서는 계속 제외한다.

### 멀티플 드롭·품질 진단

- 괭이 승급 passive에 `crop-drop-multiplier`, `seed-drop-multiplier` 경계를 추가했다.
- HarvestService는 정상 직접 수확에서만 두 multiplier를 적용하고 stochastic rounding으로 정수화한다.
- 기본값은 모두 1.0이므로 이번 변경으로 밸런스 수치는 바뀌지 않는다.
- 품질 시뮬레이터가 전달받은 품질 density shift를 실제 분포에 적용하도록 수정했다.

### 플레이어 데이터 로드

- 구버전 FarmingProfile을 읽을 때 메모리 version을 자동으로 3으로 올리지 않는다.
- 로드만으로 migration-required 플레이어를 자동 dirty 처리하지 않는다.
- 따라서 일반 autosave가 구버전 데이터를 조용히 최신 farming schema로 덮어쓰는 경로를 제거했다.
- 명시적 player/config migration과 실제 변경 저장은 별도 운영 절차로 유지한다.

## 2. 설계 대조 결과

| 항목 | 결과 |
|---|---|
| 씨앗 보유·거래 가능 | 기존 ItemRegistry 구조 유지 |
| 씨앗 심기 시 해금 검사 | 기존 CropGrowthService 유지 |
| 씨앗 상점 구매 시 해금 검사 | ShopService 공통 경계 추가 |
| 플레이어 농사 단계 | PlayerRPGData/FarmingProfile에만 저장 |
| 괭이 tier | 괭이 PDC에만 저장 |
| 가공 실행 경로 | CraftingRecipeRegistry → 기존 GUI/TransactionService 유지 |
| 가공 원본 | `crafting.yml` 단일 원본 |
| 요리 | 활성 노출·실행 차단, 기존 정의는 보존 |
| 품질 | 단일 roll 및 기존 품질 Registry 유지 |
| 간접 파괴 | 기존 포인트·씨앗·진행도 미지급 경로 유지 |

## 3. 변경 파일

- `src/main/java/com/hyunseo/hyunseorpg/shop/ShopItemData.java`
- `src/main/java/com/hyunseo/hyunseorpg/shop/ShopRegistry.java`
- `src/main/java/com/hyunseo/hyunseorpg/shop/ShopService.java`
- `src/main/java/com/hyunseo/hyunseorpg/shop/ShopTransactionReason.java`
- `src/main/java/com/hyunseo/hyunseorpg/shop/ShopGuiService.java`
- `src/main/java/com/hyunseo/hyunseorpg/farming/FarmingPromotionService.java`
- `src/main/java/com/hyunseo/hyunseorpg/farming/FarmingHoePromotionService.java`
- `src/main/java/com/hyunseo/hyunseorpg/farming/HarvestService.java`
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropQualityService.java`
- `src/main/java/com/hyunseo/hyunseorpg/farming/FarmingHubGuiService.java`
- `src/main/java/com/hyunseo/hyunseorpg/farming/FarmingStatTokenService.java`
- `src/main/java/com/hyunseo/hyunseorpg/player/PlayerDataService.java`
- `src/main/java/com/hyunseo/hyunseorpg/player/YamlPlayerDataRepository.java`
- `src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigDoctor.java`
- `src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigMigrationService.java`
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
- `src/main/resources/shops.yml`
- `src/main/resources/crafting.yml`
- `src/main/resources/farming/processing.yml`
- `src/main/resources/farming/hoe_promotion.yml`
- `src/main/resources/farming/stat_tokens.yml`
- `src/test/java/com/hyunseo/hyunseorpg/crafting/CraftingCanonicalRecipeStructureTest.java`
- `src/test/java/com/hyunseo/hyunseorpg/farming/FarmingStage7StructureTest.java`

## 4. 검증 결과

- 전체 Gradle 테스트: PASS
- Java 컴파일: PASS
- JAR 생성: PASS
- 추가 검증:
  - 4종 씨앗 상점 등록·구매 가능·해금 필드 존재: PASS
  - 가공 20종의 20:1 입력 계약: PASS
  - `farming/processing.yml`의 `crafting.yml` 원본 선언: PASS
  - cooking 활성 경로 차단: 소스 검증 PASS
  - 품질 simulator modifier 적용 경로: 컴파일·기존 테스트 PASS

## 5. 외부 서버 적용 절차

이번 빌드는 기본 리소스와 migration 코드만 변경했으며 운영 서버 외부 YAML을 자동 수정하지 않는다.

1. 기존 `shops.yml`, `crafting.yml`, `farming/` 백업
2. 새 JAR 적용
3. `/rpg migrate farming --dry-run`
4. 씨앗 4종, `processing.yml`, 레거시 1:1 가공 레시피 보정 내역 확인
5. `/rpg migrate farming --apply`
6. `/rpg doctor farming`
7. `/rpg reload all`

## 6. 미검증·잔여 위험

- seed 가격 5/10/15/20은 밸런스 확정값이 아니다.
- 실제 Shop GUI에서 미해금 구매 시 코인 미차감과 메시지를 확인해야 한다.
- 괭이 승급은 실제 인게임에서 tier 0→1, Expert 플레이어의 신규 괭이, 기존 강화·인챈트·내구도 보존을 확인해야 한다.
- 외부 YAML이 기존 값을 1이 아닌 다른 값으로 가지고 있으면 migration은 운영자 값을 강제로 20으로 덮지 않는다. Doctor가 20:1 위반으로 보고한다.
- 작물 청크 YAML의 load/save는 아직 Bukkit 메인 스레드에서 실행된다. 불변 snapshot 기반 비동기 저장으로 바꾸는 작업은 별도 성능 작업으로 남겼다.
- `rare-seed-chance`의 실제 추가 씨앗 지급은 아직 비활성이다. 이번 변경은 일반 작물·씨앗 multiplier 경계만 복구했다.
- stat token은 기본 리소스에서 비활성화했지만 외부 `stat_tokens.yml`이 `enabled: true`이면 명시적 farming migration 또는 외부 설정 정리가 필요하다.
- 기존 cooking 클래스·아이템·보관 설정은 데이터 보존을 위해 삭제하지 않았다. 활성 경로 차단 상태를 `/rpg doctor farming`과 인게임에서 확인해야 한다.
- 플레이어 구버전 데이터의 전체 운영 migration은 별도 `players` 적용 절차로 확인해야 한다.

## 7. 산출물

- JAR: `builds/HyunseoRPG-0.1.0-SNAPSHOT-farming-structure-audit-hotfix.jar`
- SHA-256: `DFDDC2ADDBBDB6E5CBC99DE44FC3591CC4D7334A19CBAE7CF55A95F0BEDECE6`
