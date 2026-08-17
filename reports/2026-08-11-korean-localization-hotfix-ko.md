# HyunseoRPG 한국어 표시 정합성 Hotfix 보고서

기준일: 2026-08-11
범위: 농사 작물·가공품·풍요의 정수·증표·레시피 표시·농사 상점·기본 재료 표시

## 1. 원인

기본 리소스의 `items.yml`, `crafting.yml`, `shops.yml`은 한국어였지만,
`ConfigService.loadManagedConfig()`가 이미 존재하는 외부 YML을 우선 사용하고 있었다.
따라서 새 JAR을 넣어도 외부 `plugins/HyunseoRPG/items.yml`의 영문 `display-name`과 `lore`가
그대로 Registry에 들어갔다.

추가로 레시피 렌더러와 상점은 Registry에 들어온 표시값을 사용하므로,
작물·가공품 이름, 품질, 재료 이름, 농사 상점 제목이 함께 영어로 노출될 수 있었다.

## 2. 수정 내용

### 런타임 표시 계층

`KoreanDisplay`에 다음 canonical 표시를 추가했다.

- 옥수수·양파·고추·마늘과 씨앗
- 일반·초급·중급·고급·최고급 품질
- 작물 품질 아이템
- 옥수수 전분·양파 농축액·고추 추출액·마늘 농축액
- 풍요의 정수와 생명·포만·풍요의 증표
- 재료·장비·특수 장비·소모품·농사·요리 카테고리
- 레시피에서 사용되는 밀·밀 씨앗·화염구·삼지창 등 기본 재료

`RPGItemRegistry`는 다음 우선순위를 사용한다.

1. canonical 한국어 표시
2. 외부 값이 한국어 또는 운영자 지정 값인 경우 외부 값
3. 외부 표시값에 영문이 있고 JAR 기본 리소스에 한국어 값이 있는 경우 기본 한국어 값
4. 최종 ID fallback

로어도 같은 방식으로 처리한다. 재료, 가격, 태그, 카테고리, PDC, 모델 데이터는 변경하지 않는다.

### 명시적 migration

`ConfigMigrationService`의 `migrate farming --apply`에 표시 문구 migration을 연결했다.

- `items.yml`: 농사 관련 표시 이름·로어만 기본 한국어로 보정
- `shops.yml`: 농사 상점 제목을 `농사`로 보정
- `crafting.yml`: canonical 제작 카테고리 이름을 보정
- `farming/quality.yml`: 품질 이름을 한국어로 보정

일반 reload는 파일을 수정하지 않는다. 실제 외부 파일 반영은 명시적 migration에서만 수행한다.
기존 운영자의 gameplay 설정은 보존한다.

## 3. 설계 대조

이번 수정은 표시 계층만 보정하며 제작·판매·수확·품질 계산 경로를 변경하지 않는다.

`crafting.yml` → `CraftingRecipeRegistry` → `CraftingRecipeRenderer` 흐름은 유지하고,
렌더러가 표시할 때 동일한 `KoreanDisplay.itemId()`를 사용하게 했다.
따라서 새 농사 레시피를 추가해도 Java에 레시피 ID를 추가할 필요가 없다.

외부 YML은 authoritative 설정이라는 기존 원칙도 유지했다. 단, 영문 표시값만 런타임에서
안전하게 한국어 기본값으로 대체하며, 운영자가 이미 한국어로 지정한 문구는 덮어쓰지 않는다.

## 4. 변경 파일

- `src/main/java/com/hyunseo/hyunseorpg/ui/KoreanDisplay.java`
- `src/main/java/com/hyunseo/hyunseorpg/item/RPGItemRegistry.java`
- `src/main/java/com/hyunseo/hyunseorpg/shop/ShopRegistry.java`
- `src/main/java/com/hyunseo/hyunseorpg/crafting/CraftingLayoutRegistry.java`
- `src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigMigrationService.java`
- `src/main/resources/shops.yml`
- `src/test/java/com/hyunseo/hyunseorpg/ui/KoreanDisplayLocalizationTest.java`

## 5. 검증 결과

- 전체 자동 테스트: 119개
- 실패: 0개
- 건너뜀: 0개
- Java 컴파일: 성공
- JAR 생성: 성공
- `libs` 폴더: 수정하지 않음

## 6. 운영 서버 적용 순서

새 JAR만 교체하면 기존 외부 YML은 자동으로 덮어쓰지 않는다. 다음 순서로 적용한다.

```text
/rpg migrate farming --dry-run
/rpg migrate farming --apply
/rpg reload all
```

적용 전 migration이 제시한 변경 파일과 백업을 확인한다. 재시작을 사용할 경우에는
`--apply` 이후 재시작하면 된다.

## 7. LIVE TEST REQUIRED

- 농사 가공 GUI에서 일반·초급·중급·고급·최고급 작물과 가공품 이름 확인
- 풍요의 정수와 세 증표의 이름·로어 확인
- 농사 상점의 제목과 상품명 확인
- 가공 레시피의 결과·재료·품질 로어 확인
- 기존 외부 YML의 한국어 운영자 지정 문구가 유지되는지 확인
- migration 적용 후 재시작·reload에서도 같은 표시가 유지되는지 확인
- 일반 전투·강화·인챈트 아이템에서 영문 표시가 남아 있는지 `/rpg doctor`와 인게임에서 확인

## 8. 산출물

JAR:
`builds/HyunseoRPG-0.1.0-SNAPSHOT-korean-localization-hotfix.jar`

SHA-256:
`50C3A56810020627D5B5158BC9C6BAA8B352A83C218A13718DE01A065F1A0F35`
