# HyunseoRPG 농사 Stage 1 실서버 검증 핫픽스 보고서

작성일: 2026-08-04  
작업 상태: `IMPLEMENTED`  
자동 검증: `PASSED`  
실서버 검증: `PENDING`  
Stage 2 옥수수 2블록: `NOT IMPLEMENTED`  
Stage 4 수확 보상: `NOT IMPLEMENTED`

## 1. 정확한 원인

### 1.1 바닐라 성장 충돌

Stage 1의 `VanillaCropBlockAdapter`가 WHEAT, CARROTS, BEETROOTS, POTATOES를 실제 표현으로 사용하지만, `BlockGrowEvent`와 `BlockFertilizeEvent`를 차단하지 않고 있었다.

그 결과 바닐라 랜덤 틱 또는 뼛가루가 `Ageable.age`를 먼저 변경했고, 논리 `CropInstance.stage`는 이전 값으로 남았다. 이후 HyunseoRPG 성장 작업이 논리 단계로 다시 렌더링하면서 작물이 뒤로 되돌아가는 현상이 발생했다.

### 1.2 오프라인·청크 언로드 성장 누락

기존 `tickGrowth()`는 due 상태를 확인한 뒤 한 번에 한 단계만 증가시켰다. 청크가 오래 언로드되었거나 서버가 종료된 동안 여러 성장 주기가 지나도 재로드 시 한 단계만 처리되어, 이후 틱마다 연속 성장하는 것처럼 보일 수 있었다.

### 1.3 경작지 파괴 드롭

등록 작물 자체의 `BlockBreakEvent`만 취소하고, 아래 FARMLAND가 파괴될 때 상단 표현을 먼저 제거하지 않았다. 따라서 바닐라 물리 처리 과정에서 작물 표현이 정상적인 바닐라 작물로 취급되어 씨앗·작물 드롭이 발생할 수 있었다.

### 1.4 간접 파괴 미보호

물 흐름, 물리 갱신, 피스톤, 폭발, FARMLAND의 DIRT 변환 경로에서 CropIndex와 실제 블록 표현을 함께 정리하거나 안전하게 차단하는 공통 처리가 없었다.

## 2. 변경 파일

- `src/main/java/com/hyunseo/hyunseorpg/farming/CropGrowthCalculator.java`
  - Bukkit 객체와 분리된 오프라인 성장 catch-up 계산기
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropInstance.java`
  - 계산 결과를 논리 단계와 nextGrowthAt에 적용하는 메서드 추가
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropBlockAdapter.java`
  - 표현 제거용 `clearRepresentation` 경계 추가
- `src/main/java/com/hyunseo/hyunseorpg/farming/VanillaCropBlockAdapter.java`
  - Stage 1 표현을 드롭 없이 AIR로 제거
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropGrowthService.java`
  - 성장·자연 이벤트·지지 블록·간접 파괴·청크 저장 처리 통합
- `src/test/java/com/hyunseo/hyunseorpg/farming/CropGrowthCalculatorTest.java`
  - 다단계 catch-up, maxStage clamp, due 이전 정지, 비정상 단계 보정 테스트

## 3. 추가 이벤트 리스너

`CropGrowthService`에 다음 이벤트 처리를 추가했다.

### 자연 성장·뼛가루

- `BlockGrowEvent`
  - CropIndex에 등록된 canonical 위치면 취소
- `BlockFertilizeEvent`
  - 비료 대상 목록 중 등록 작물이 하나라도 있으면 취소
- 일반 바닐라 작물은 CropIndex에 등록되어 있지 않으므로 영향을 받지 않는다.

### 지지 블록 및 물리

- `BlockBreakEvent`
  - 등록 작물 직접 파괴는 기존처럼 취소
  - FARMLAND 파괴 시 상단 등록 작물을 먼저 드롭 없이 제거하고 FARMLAND 파괴는 허용
- `BlockFadeEvent`
  - FARMLAND가 DIRT로 변하기 전에 상단 등록 작물 제거
- `BlockPhysicsEvent`
  - 등록 작물 또는 FARMLAND 지지 블록에 대한 물리 변화 시 등록 작물 제거

### 간접 파괴

- `BlockPistonExtendEvent`
- `BlockPistonRetractEvent`
  - 이동 대상이 등록 작물 또는 등록 작물의 FARMLAND 지지 블록이면 이벤트 취소
- `BlockFromToEvent`
  - 물 흐름의 대상 또는 원본이 등록 작물·지지 FARMLAND에 닿으면 이벤트 취소
- `BlockExplodeEvent`
- `EntityExplodeEvent`
  - 폭발 대상 목록에 포함된 등록 작물은 폭발 처리 전에 드롭 없이 제거

모든 제거는 canonical 위치에서 `CropIndex.remove()`를 먼저 수행한다. 이미 제거된 작물은 다시 처리하지 않으므로 중복 정리가 안전하다.

## 4. CropBlockAdapter 변경

새 메서드:

```java
boolean clearRepresentation(Block canonicalBlock, CropDefinition definition);
```

`CropGrowthService`는 AIR를 직접 하드코딩하지 않는다. 현재 `VanillaCropBlockAdapter`만 `Material.AIR`를 사용하며, 향후 다음 구현체로 교체할 수 있다.

- Stage 2 옥수수 상·하단 표현
- 리소스팩 기반 표현
- BetterModel 또는 다른 커스텀 블록 표현

현재 Stage 1의 모든 crop definition은 `two-block: false`다. 옥수수 상·하단의 서로 다른 청크 처리는 추가하지 않았다.

## 5. 오프라인 성장 계산식

순수 계산기 `CropGrowthCalculator`에서 다음과 같이 처리한다.

```text
interval = max(1초, secondsPerStage) × 1000
if now < nextGrowthAt:
    성장 없음
else:
    overdueSteps = floor((now - nextGrowthAt) / interval) + 1
    nextStage = min(currentStage + overdueSteps, maxStage)
```

최대 단계에 도달하면:

```text
nextGrowthAt = Long.MAX_VALUE
```

최대 단계에 도달하지 않은 경우에는 원래 성장 기준 시각에 처리된 단계 수만큼 interval을 더해 다음 성장 시각을 유지한다. 청크 언로드·서버 종료 저장 시점과 청크 로드·서버 재시작 복원 시점이 동일한 계산기를 사용한다.

청크 로드 시 순서:

1. 저장된 CropInstance와 CropDefinition 확인
2. 실제 표현 블록 확인
3. 저장된 논리 stage를 원본으로 catch-up 계산
4. 계산된 최종 stage를 `CropBlockAdapter.applyStage`로 한 번 적용
5. 적용 성공 후 CropIndex 등록
6. 보정이 발생했으면 dirty 청크 표시

따라서 실제 Ageable age가 저장값과 달라도 저장된 논리 stage를 기준으로 복원한다.

## 6. 간접 파괴별 임시 정책

| 경로 | Stage 1 처리 |
|---|---|
| FARMLAND 직접 파괴 | 작물 표현 드롭 없이 제거, CropIndex 제거, FARMLAND 파괴 허용 |
| FARMLAND fade | 상단 작물 먼저 제거, fade 허용 |
| 물리 갱신 | 등록 작물 또는 지지 FARMLAND 감지 시 작물 제거 |
| 피스톤 확장·수축 | 등록 작물·지지 블록이 이동 대상이면 이벤트 차단 |
| 물 흐름 | 등록 작물·지지 FARMLAND에 닿는 흐름 차단 |
| 블록 폭발 | 폭발 대상 작물을 먼저 드롭 없이 제거 |
| 엔티티 폭발 | 폭발 대상 작물을 먼저 드롭 없이 제거 |
| 등록 작물 직접 파괴 | BlockBreakEvent 취소 유지 |

모든 경로에서 다음은 지급하지 않는다.

- 커스텀 수확물
- 씨앗
- 진행도
- 활동 코인
- 마석 파편
- 기존 auto-replant 보상

## 7. 테스트 결과

실행:

```text
./gradlew.bat clean build
```

결과:

```text
BUILD SUCCESSFUL
6 actionable tasks: 6 executed
```

자동 테스트:

- 오프라인 0단계에서 여러 단계 catch-up 통과
- maxStage clamp 통과
- 최대 단계에서 `Long.MAX_VALUE` 정지 통과
- due 이전 성장 없음 통과
- 비정상 stage 상한 보정 통과
- CropIndex canonical 위치 중복 방지 통과
- 기존 전체 테스트 통과
- Java 컴파일 통과

Paper 이벤트 리스너의 실제 월드 상호작용은 현재 테스트 환경에 MockBukkit이 포함되어 있지 않으므로, 아래 실서버 항목을 별도로 남겼다.

## 8. 실서버 검증 목록

1. Stage 1 핫픽스 JAR로 서버 완전 재시작
2. 일반 밀·당근·비트·감자에 뼛가루 사용
   - 등록되지 않은 일반 바닐라 작물은 정상 성장해야 함
3. `seed_corn`을 경작지에 심고 자연 랜덤 틱 대기
   - 바닐라 성장으로 먼저 커지지 않아야 함
   - HyunseoRPG 성장 시점에만 단계가 변경되어야 함
4. 등록 작물에 뼛가루 사용
   - 성장하지 않아야 함
5. 등록 작물의 아래 FARMLAND를 손으로 파괴
   - 작물 표현이 먼저 사라져야 함
   - 씨앗·작물·코인·마석 파편이 없어야 함
   - FARMLAND 자체는 정상적으로 파괴되어야 함
6. 등록 작물의 직접 파괴
   - 모든 도구에서 BlockBreak가 취소되어야 함
7. 등록 작물이 있는 청크를 언로드한 뒤 충분한 시간 후 재로드
   - 한 번에 경과한 단계까지 이동해야 함
   - 틱마다 한 단계씩 연속 보정되지 않아야 함
8. 성장 직전 서버 종료 후 재시작
   - 재시작 이후 동일한 catch-up 규칙 적용
9. 최대 stage 작물 대기
   - 추가 성장이나 반복 저장이 없어야 함
10. 물 흐름으로 경작지 주변을 변경
   - 작물 드롭이 없어야 하며 유령 CropIndex가 남지 않아야 함
11. 피스톤으로 작물 또는 아래 FARMLAND 이동 시도
   - 이벤트가 안전하게 차단되어야 함
12. 폭발로 작물 또는 아래 FARMLAND 파괴
   - 작물은 드롭 없이 정리되어야 함
13. 최대 승급 네더라이트 괭이로 일반 블록 파괴
   - 일반 블록은 정상 파괴되어야 함
14. 같은 괭이로 등록 커스텀 작물 파괴
   - Stage 1 정책에 따라 파괴가 취소되어야 함

## 9. 빌드 산출물

- JAR: `C:\Users\User\Documents\Codex\2026-06-29\hyunseorpg-paper-spigot-rpg-hyunseorpg-1\build\libs\HyunseoRPG-0.1.0-SNAPSHOT.jar`
- SHA-256: `3C00A25309A7E26287AFDF02DF2A40F70044FE0469A1E85E012A789DEDD5A977`

## 10. Stage 2 확장 지점

- `CropBlockAdapter.clearRepresentation`에 상·하단 동시 표현 제거 구현
- `CropBlockAdapter.applyStage`에 옥수수 하단 canonical 기준 상단 표현 갱신
- `CropIndex`는 계속 하단 위치 하나만 저장
- 상단 조회·중복 이벤트 방지·불일치 복구는 같은 X/Z 청크 내부에서만 처리
- 수확 보상은 `CropHarvestValidator` 이후 별도 Stage 4 서비스로 연결
- 현재 직접 수확 취소 정책과 활동 보상 제외 정책은 Stage 4에서 교체

이번 핫픽스에서는 품질, 해금, FarmingProfile, 괭이 강화·승급, 판매, 수확 보상을 추가하지 않았다.
