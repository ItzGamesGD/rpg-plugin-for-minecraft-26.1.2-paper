# HyunseoRPG 농사 Stage 4 직접 수확·간접 파괴 보고서

## 상태

- Implementation status: COMPLETE
- Automated verification: PASSED
- Live gameplay verification: PENDING
- Balance verification: PENDING
- 품질·괭이 강화·승급·멀티플·상점 가격: 미구현

Stage 1·2의 CropIndex canonical 하단, CropRepresentationResolver, CropBlockAdapter, 옥수수 상·하단 표현 구조를 유지하고 Stage 4 수확 경로만 확장했다.

## 직접 수확

`HarvestService.handleDirect()`를 추가하고 `CropGrowthService.onCropBreak()`에서 호출한다.

- 맨손·일반 도구·괭이 모두 허용
- 완전 성장(`stage >= maxStage`) 작물만 수확
- 미성숙 작물은 이벤트만 취소하고 작물·데이터를 유지
- 다른 플레이어가 심은 작물도 소유자 검사 없이 수확 가능
- 보호구역 로직 없음
- 옥수수 상단 또는 하단 파괴 모두 Resolver를 통해 하단 canonical 좌표 하나로 처리
- 바닐라 드롭 차단
- 기본 작물 아이템과 씨앗을 인벤토리 전달 서비스로 지급
- 정상 처리 완료 후 `addValidHarvest(player, cropId, 1)`을 정확히 한 번 호출
- 괭이 여부와 현재 농사 단계는 `HarvestContext`에 기록하지만 보너스 계산에는 사용하지 않음

인벤토리 초과분은 기존 `InventoryDeliveryService`와 `PendingRewardService` 경로를 사용한다.

## 간접 파괴

`HarvestService.handleIndirect()`를 통해 다음 원인을 분리한다.

- 물 흐름: `WATER`
- 피스톤: `PISTON`
- 폭발: `EXPLOSION`
- 경작지 소실·직접 경작지 파괴: `SOIL_LOSS`
- 기타 비플레이어 변경: `OTHER_NON_PLAYER`

정책:

- CropIndex와 표현을 먼저 제거
- 성숙 작물만 낮은 확률로 작물 아이템 1개를 월드에 드롭
- 씨앗 없음
- 플레이어 농사 진행도 없음
- 괭이 진행도·보너스 없음
- 품질·멀티플·인챈트 보너스 없음
- 피스톤은 영향을 받는 작물을 정리한 뒤 이벤트를 취소해 이동 우회를 차단
- 물 흐름은 영향을 받는 작물을 정리한 뒤 이벤트를 취소
- 폭발 블록 목록은 작물별 canonical 좌표 기준으로 중복 처리 방지

간접 드롭 확률은 `farming/harvest.yml`에서 조정한다. 기본값 `0.10`은 Stage 4 임시값이며 최종 밸런스 값이 아니다.

## 중복 방지

`HarvestService`에 canonical 좌표와 원인을 결합한 처리 키를 둔다.

- 옥수수 상·하단 이벤트는 동일 canonical 좌표로 수렴
- 동일 이벤트 재진입은 처리 중 Set에서 차단
- 폭발 블록 목록 중복은 Index 제거 성공 여부로 차단
- BlockBreakEvent와 간접 이벤트가 연속 발생해도 Index가 이미 제거된 경우 재지급하지 않음
- 처리 종료 시 `finally`에서 잠금 제거

## 설정

추가 파일:

`plugins/HyunseoRPG/farming/harvest.yml`

```yaml
schema-version: 1
enabled: true
direct:
  crop-amount: 1
  seed-amount: 1
indirect:
  crop-drop-chance: 0.10
  crop-amount: 1
```

기존 `crops.yml`, `growth.yml`과 분리했으며, `/rpg reload farming`과 전체 reload에 harvest 설정 reload를 연결했다.

## 변경 파일

신규:

- `src/main/java/com/hyunseo/hyunseorpg/farming/HarvestCause.java`
- `src/main/java/com/hyunseo/hyunseorpg/farming/HarvestContext.java`
- `src/main/java/com/hyunseo/hyunseorpg/farming/HarvestService.java`
- `src/main/resources/farming/harvest.yml`
- `src/test/java/com/hyunseo/hyunseorpg/farming/FarmingStage4DataTest.java`

수정:

- `src/main/java/com/hyunseo/hyunseorpg/farming/CropGrowthService.java`
  - 직접 수확 연결
  - 토양 소실·물·피스톤·폭발 간접 파괴 연결
  - 피스톤 목적지까지 검사 후 안전 취소
- `src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigService.java`
  - harvest.yml 로드·접근·reload 추가
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
  - farming reload에 harvest.yml 추가

기존 Stage 1·2 클래스와 데이터 구조는 삭제하지 않았다.

## 검증 결과

실행:

```text
./gradlew.bat test
./gradlew.bat clean build
```

결과:

- `BUILD SUCCESSFUL`
- Stage 1·2 기존 테스트 통과
- Stage 3 기존 테스트 통과
- Stage 4 harvest.yml 정책 테스트 통과
- 최종 빌드 테스트 포함 통과
- 기존 Paper API deprecated 경고 16건

## 실서버 검증 절차

1. 신규 또는 옥수수 해금 플레이어로 옥수수 심기
2. 성장 완료 전 맨손 파괴: 작물 유지, 드롭 없음
3. 성장 완료 후 맨손 파괴: 작물 1개·씨앗 1개 지급
4. 일반 도구로 동일 테스트
5. 괭이로 동일 테스트
6. 다른 플레이어가 심은 작물을 수확
7. 옥수수 하단 파괴 후 1회만 지급
8. 옥수수 상단 파괴 후 1회만 지급
9. 성숙 작물에 물 흐름: 낮은 확률 작물 드롭, 씨앗·진행도 없음
10. 피스톤으로 작물 방향 이동 시 작물 정리 및 피스톤 차단
11. 폭발로 작물·경작지를 동시에 처리해 중복 드롭이 없는지 확인
12. 경작지 직접 파괴·자연 fade·몹 밟기 확인
13. 인벤토리 가득 찬 상태에서 직접 수확 후 PendingReward 확인
14. 같은 틱에 중복 파괴 이벤트를 발생시켜 수확 수와 아이템이 1회인지 확인
15. 재접속 후 `farming.total-valid-harvests`와 작물별 수확 수 확인

## 미검증·보류

- 실제 Paper 서버 이벤트 순서와 바닐라 표현 블록의 폭발 드롭 동작
- 물·피스톤·폭발 확률 드롭 체감 및 밸런스
- PendingReward 실제 수령과 수확 트랜잭션의 운영 데이터 왕복
- 괭이 내구도 방지·강화·승급 연동
- 품질 작물·멀티플·인챈트 보너스
- 상점·판매·가공·요리

## 릴리스 산출물

- JAR: `builds/2026-08-06_111-farming-stage-4-harvest-engine.jar`
- SHA-256: `CAF58A9F0C8644996BE640CF6FF182287F80630C62BB688BF9072BEA0724EB6C`

