# HyunseoRPG 농사 Stage 4 실서버 검증 핫픽스 보고서

## 상태

- Implementation status: COMPLETE
- Automated verification: PASSED
- Live gameplay verification: PENDING
- Balance verification: PENDING

Stage 1~4 구조는 유지하고 직접 수확·간접 파괴 이벤트와 `/rpg` 자동완성만 수정했다.

## 수정 내용

### 미성숙 직접 파괴

기존 `HarvestService.handleDirect()`는 미성숙 작물에서 `true`만 반환했고, 호출부가 항상 `BlockBreakEvent`를 취소해 작물이 영구 유지됐다.

수정 후:

- `DirectHarvestResult` 추가
- 미성숙 작물은 `IMMATURE_UPROOTED`
- CropIndex에서 canonical 작물 제거
- 옥수수 상·하단 표현 제거
- 씨앗 1개만 `InventoryDeliveryService`로 지급
- 작물 아이템·수확량·품질·멀티플·괭이 진행도·활동 보상 없음
- 성숙 작물은 기존 작물 1개·씨앗 1개 지급 및 유효 수확 1회 증가
- 보상 생성 실패 시 작물과 데이터는 유지

### 물 흐름

기존에는 작물을 제거한 뒤 `event.setCancelled(true)`를 호출해 물 흐름까지 멈췄다.

수정 후:

- `event.getToBlock()`만 기본 검사
- 물 원본 블록은 처리하지 않음
- 작물 제거 성공 시 `BlockFromToEvent` 허용
- 제거 실패 또는 CropIndex 잔존 시에만 취소
- 다른 플러그인이 먼저 취소한 이벤트는 `HIGHEST, ignoreCancelled=true`로 건드리지 않음

### 피스톤 이동

기존에는 작물 정리 후 피스톤 이벤트를 항상 취소했다.

수정 후:

- 원본과 목적지에서 canonical 위치를 수집
- `Set<CropPosition>`으로 옥수수 상·하단 중복 제거
- 모든 작물 제거 성공 시 첫 피스톤 이벤트 허용
- 제거 실패·경로 잔존 시에만 취소
- 지지 FARMLAND가 이동하는 경우에도 상단 작물을 먼저 정리
- `BlockPistonExtendEvent`, `BlockPistonRetractEvent`를 `HIGHEST, ignoreCancelled=true`로 변경

### `/rpg` 자동완성

- root 목록에 `pending` 추가
- `/rpg pending`에 `claim` 추가
- 기존 `give`, `reload`, `doctor`, `migrate`, `debug` 유지
- `reload` completion에서 `farming` 유지
- seed_corn, seed_onion, seed_chili, seed_garlic completion 회귀 테스트 추가

## 변경 파일

신규:

- `src/main/java/com/hyunseo/hyunseorpg/farming/DirectHarvestResult.java`
- `src/main/java/com/hyunseo/hyunseorpg/farming/IndirectHarvestResult.java`
- `src/test/java/com/hyunseo/hyunseorpg/command/RPGGiveCommandCompletionTest.java`

수정:

- `src/main/java/com/hyunseo/hyunseorpg/farming/HarvestService.java`
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropGrowthService.java`
- `src/main/java/com/hyunseo/hyunseorpg/command/RPGGiveCommand.java`

## 자동 검증

```text
./gradlew.bat test
./gradlew.bat clean build
```

- `BUILD SUCCESSFUL`
- Stage 1~3 기존 테스트 통과
- Stage 4 설정 테스트 통과
- `/rpg pending`, `claim`, `farming`, 씨앗 자동완성 테스트 통과
- Paper API deprecated 경고 16건 유지

## 실서버 테스트 필요

1. 성장 0단계 옥수수 직접 파괴: 씨앗 1개만 반환
2. 중간 성장 작물 직접 파괴: 씨앗 1개만 반환
3. 완전 성장 작물 직접 파괴: 작물·씨앗 지급 및 수확량 1 증가
4. 물이 작물 칸으로 같은 이벤트에서 흐르는지 확인
5. 피스톤 첫 동작이 지연 없이 진행되는지 확인
6. 다른 플러그인이 물·피스톤을 취소한 경우 작물이 보존되는지 확인
7. 옥수수 상·하단 동시 영향 시 1회만 처리
8. 폭발·경작지 소실·몹 밟기 처리 확인
9. 인벤토리 초과 씨앗 반환 시 PendingReward 저장 확인
10. `/rpg pending` 및 `/rpg pending claim` 자동완성 확인

## 릴리스

- JAR: `builds/2026-08-06_112-farming-stage-4-live-hotfix.jar`
- SHA-256: `BAEEBE857B7F697DE07B6CD30A825FB78D7722D2D3B953B1FC4A0A8EC242C02A`

