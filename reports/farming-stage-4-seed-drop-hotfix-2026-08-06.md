# HyunseoRPG 농사 Stage 4 씨앗 복제·바닐라 드롭 누출 핫픽스 보고서

## 상태

- Implementation status: COMPLETE
- Automated verification: PASSED
- Live gameplay verification: PENDING
- Balance verification: NOT IN SCOPE

## 원인

2블록 작물인 옥수수는 WHEAT 블록을 하단과 상단에 쌓아 표현하고 있었다. 기존 `clearRepresentation()`은 하단을 먼저 AIR로 바꾼 뒤 상단을 제거했다. 하단이 지지 블록 역할을 잃는 순간 상단 WHEAT에 바닐라 물리 갱신이 발생할 수 있어, WHEAT와 WHEAT_SEEDS가 커스텀 제거 과정에 섞일 수 있었다.

또한 제거 중인 표현 좌표를 추적하는 방어 계층이 없어, 지연된 `BlockDropItemEvent`가 일반 바닐라 농사 이벤트인지 커스텀 작물 제거 이벤트인지 구분하지 못했다.

간접 작물 드롭 확률은 기존 코드가 모든 값을 0.0~1.0으로 clamp했다. 따라서 운영 설정에 `10`이 들어가면 1.0으로 바뀌어 확정 드롭이 되는 위험이 있었다.

## 변경 내용

### 1. 표현 제거 순서

`VanillaCropBlockAdapter.clearRepresentation()`을 다음 순서로 변경했다.

1. 2블록 작물의 상단 표현 제거
2. 하단 표현 제거

두 블록 모두 `setType(Material.AIR, false)`만 사용하며 `breakNaturally()`와 `getDrops()`는 사용하지 않는다.

### 2. 바닐라 드롭 방어

`HarvestService`에 짧은 수명의 제거 트랜잭션 좌표 집합을 추가했다.

- 하단 canonical 좌표 등록
- 2블록 작물은 상단 좌표도 함께 등록
- 표현 제거가 끝난 다음 틱에 자동 정리
- 서버 저장 대상 아님
- `BlockDropItemEvent`에서 해당 좌표의 WHEAT, WHEAT_SEEDS, CARROT, POTATO, BEETROOT, BEETROOT_SEEDS 및 바닐라 작물 표현 드롭 제거
- 일반 바닐라 농사는 `CropIndex` 기반 제거 마커가 없으므로 영향 없음

### 3. 간접 드롭 확률 검증

`indirect.crop-drop-chance`는 비율값만 허용한다.

- 허용 범위: `0.0 <= value <= 1.0`
- `0.10`: 10%
- `1.0`: 100%
- `10`, 음수, NaN, Infinity: 안전 기본값 `0.10`
- 시작 및 farming reload 시 실제 적용값 로그 출력

현재 작업 공간에는 실제 운영 서버의 `plugins/HyunseoRPG/farming/harvest.yml`가 없어 외부 설정값이 `1.0` 또는 `10`이었는지는 확인하지 못했다. 내장 기본값은 `0.10`이다.

### 4. 씨앗 canonical 정규화

`RPGItemService`에 농사 아이템 정규화 경로를 추가했다.

대상 ID:

- `seed_corn`, `seed_onion`, `seed_chili`, `seed_garlic`
- `crop_corn`, `crop_onion`, `crop_chili`, `crop_garlic`

동일 ID가 확인되면 Registry 정의에서 ItemStack을 다시 생성하여 Material, 이름, Lore, rarity, CustomModelData, item ID PDC를 canonical 값으로 맞춘다. 기존 장비 전용 PDC나 임의 메타는 농사 아이템에 전파하지 않는다. PDC가 없는 바닐라 WHEAT_SEEDS를 `seed_corn`으로 자동 변환하지 않는다.

정규화는 다음 생성·전달 경로에 연결했다.

- RPGItemService 생성 결과
- InventoryDeliveryService 보상 전달
- 접속·인벤토리·상자 이동 등 기존 VanillaStackingService 정규화 경로

## 변경 파일

- `src/main/java/com/hyunseo/hyunseorpg/farming/VanillaCropBlockAdapter.java`
  - 2블록 표현을 상단부터 무드롭 제거
- `src/main/java/com/hyunseo/hyunseorpg/farming/HarvestService.java`
  - 제거 마커, 간접 확률 검증, 제거 순서와 인덱스 반영 순서 보강
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropGrowthService.java`
  - `BlockDropItemEvent` 바닐라 작물 드롭 방어 추가
- `src/main/java/com/hyunseo/hyunseorpg/item/RPGItemService.java`
  - 농사 아이템 canonical 생성·정규화 추가
- `src/main/java/com/hyunseo/hyunseorpg/item/VanillaStackingService.java`
  - 기존 아이템 정규화 시 농사 아이템 정규화 연결
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
  - 생성·보상·인벤토리 경로에 canonical 정규화 연결
- `src/test/java/com/hyunseo/hyunseorpg/farming/FarmingStage4DataTest.java`
  - 잘못된 간접 드롭 확률이 확정 드롭으로 변하지 않는지 검증

## 자동 검증

실행:

```text
./gradlew.bat test
./gradlew.bat clean build
```

결과:

- `test`: PASSED
- `clean build`: PASSED
- 컴파일 및 테스트 오류: 없음
- 기존 API deprecation 경고 16개: 남아 있음. 이번 농사 핫픽스와 무관하여 수정하지 않음

자동 검증에서 확인한 항목:

- `0.10`, `1.0` 확률 유지
- `10.0`, 음수, NaN, Infinity의 `0.10` fallback
- Stage 4 direct/indirect 정책 분리
- 기존 전체 테스트 통과

## 실서버 미검증 목록

다음은 실제 서버에서 확인해야 한다.

1. 미성숙 옥수수 설치·파괴 100회 후 커스텀 씨앗 총량 동일
2. 미성숙 제거 중 WHEAT와 WHEAT_SEEDS 0개
3. 물 파괴 100회에서 바닐라 드롭 0개
4. 물이 같은 이벤트에서 즉시 흐르는지 확인
5. 옥수수 상단만 타격 시 씨앗 1개만 반환
6. 하단·상단 동시 이벤트에서 CropIndex 고아 데이터와 중복 지급 없음
7. `/rpg give`, 상점, 성숙 수확, 미성숙 반환, PendingReward 회수 씨앗의 정상 스택 여부
8. 운영 `harvest.yml`의 실제 `crop-drop-chance` 값과 로그 일치 여부

## 빌드 산출물

- JAR: `builds/2026-08-06_113-farming-stage-4-seed-drop-hotfix.jar`
- 원본 Gradle JAR: `build/libs/HyunseoRPG-0.1.0-SNAPSHOT.jar`
- SHA-256: `B41CB3368718B56FB8EAA3B38EBEA84BEC3CF1A53AB73839F93ED42089FD9915`

이번 변경에서는 품질, 괭이 강화·승급, 판매, 가공, 요리, 리소스팩, Stage 5 기능을 구현하지 않았다.
