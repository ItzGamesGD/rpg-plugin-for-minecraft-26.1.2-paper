# HyunseoRPG 농사 Stage 2 옥수수 2칸 구조 및 데이터 복구 보고서

작성일: 2026-08-06  
작업 상태: `IMPLEMENTED`  
자동 검증: `PASSED`  
실서버 검증: `PENDING`

## 1. 적용 범위

프롬프트 분할본의 프롬프트 2만 실행했다. Stage 1의 안정성 수정은 유지했으며, 다음 기능은 구현하지 않았다.

- 직접 수확 보상
- 플레이어 FarmingProfile 및 해금
- 품질
- 괭이 강화·승급
- 판매·가공·요리
- 범위 수확
- Stage 3 이후 관리자 명령

사용자가 고정한 청크 정책을 우선 적용했다.

- 옥수수 하단만 `CropInstance`의 canonical 좌표
- 상단은 저장하지 않는 표현 블록
- 상·하단은 항상 같은 X/Z 청크
- 서로 다른 청크를 전제로 한 로직 없음
- 상단 조회는 같은 X/Z에서 `y - 1` 하단만 확인

## 2. 구현 내용

### 2.1 옥수수 2블록 표현

`crops.yml`에서 `corn.two-block: true`로 변경했다. 현재 `VanillaCropBlockAdapter`는 WHEAT를 하단과 상단에 각각 표시한다.

설치 시:

1. 하단이 될 블록이 AIR인지 확인
2. 상단이 될 블록도 AIR인지 확인
3. 하단에 논리 stage를 적용
4. 상단에 동일한 stage를 적용
5. 하단 위치 하나만 CropIndex에 등록

양파·고추·마늘은 기존 단일 블록 표현을 유지한다.

### 2.2 성장 동기화

성장 단계가 변경될 때 `CropBlockAdapter.applyStage(canonical, definition, stage)`를 한 번 호출한다. 2블록 작물의 하단과 상단 변경은 같은 메인 스레드 작업 안에서 실행된다.

상단이 누락된 경우:

- 하단이 유효하면 저장된 논리 stage로 상단을 복구
- 상단 위치가 AIR이면 새 표현 생성
- 상단 위치가 다른 블록이면 해당 작물을 안전하게 제거

하단이 누락되거나 잘못된 표현이면:

- CropIndex에서 해당 instance 제거
- 남아 있는 상단 표현 제거
- 청크 dirty 표시

### 2.3 상단·하단 이벤트 단일 처리

`CropIndex.findByRepresentation()`을 추가했다.

- 이벤트 좌표가 하단이면 하단 instance 반환
- 이벤트 좌표가 상단이면 바로 아래 하단 instance 반환
- 주변 X/Z 좌표는 검색하지 않음
- 별도의 상단 CropInstance는 생성하지 않음

따라서 상단·하단 어느 쪽에서 이벤트가 발생해도 하나의 canonical instance만 처리한다.

## 3. CropBlockAdapter 확장

추가한 경계:

```java
default boolean isUpperRepresentation(Block block, CropDefinition definition)
default boolean isCompleteRepresentation(Block canonicalBlock, CropDefinition definition)
boolean applyStage(Block canonicalBlock, CropDefinition definition, int stage)
boolean clearRepresentation(Block canonicalBlock, CropDefinition definition)
```

`VanillaCropBlockAdapter`의 처리:

- 하단 representation 확인
- 상단 representation 확인
- 하단·상단 stage 동시 적용
- 하단 또는 상단 표현 제거
- 상단에 다른 블록이 있으면 덮어쓰지 않고 실패

향후 리소스팩·BetterModel 등 표현 구현체로 교체할 수 있도록 `CropGrowthService`가 상단 블록 Material을 직접 조작하지 않는다.

## 4. 복구 대상 처리

| 상태 | 처리 |
|---|---|
| 데이터와 하단·상단 모두 정상 | 저장 stage로 동기화 후 등록 |
| 데이터는 있으나 하단 없음 | instance 제거, 남은 상단 제거, dirty |
| 데이터는 있으나 상단 없음 | 하단이 정상이면 상단 복구 |
| 상단만 존재 | 하단 instance가 있으면 복구 처리, 없으면 데이터 없이 남은 표현 정리 대상 |
| 하단 표현이 다른 블록으로 교체 | instance 제거 및 알려진 표현 정리 |
| 상단 위치가 다른 블록 | 기존 블록을 덮어쓰지 않고 instance 제거 |
| 중복 CropInstance | canonical 위치 중복 등록 거부 |
| 청크 로드 중 단일 항목 오류 | 해당 항목만 건너뛰고 나머지 로드 유지 |

저장 데이터는 계속 하단 canonical 좌표 하나만 사용한다. 상단을 별도 파일 항목으로 저장하지 않는다.

## 5. 기존 Stage 1 핫픽스 유지 여부

다음 기존 동작은 유지했다.

- `BlockPhysicsEvent`에서 FARMLAND를 이유로 주변 작물을 삭제하지 않음
- 등록 작물의 physics는 토양·하단 표현·상단 표현을 검증한 뒤 해당 canonical 하나만 처리
- `EntityChangeBlockEvent`에서 지지 FARMLAND 바로 위 작물 하나만 처리
- FARMLAND 직접 파괴·fade는 바로 위 작물만 처리
- 물 흐름·폭발은 canonical 위치 기준 멱등 정리
- 피스톤은 이동 원본과 목적지 검사
- randomTickSpeed와 커스텀 성장 분리
- 직접 수확은 Stage 1 정책에 따라 계속 취소
- 활동 코인·마석 파편·씨앗·품질·진행도 보상 없음

## 6. 변경 파일

- `src/main/java/com/hyunseo/hyunseorpg/farming/CropIndex.java`
  - 상단 표현 좌표에서 하단 canonical instance를 찾는 `findByRepresentation` 추가
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropBlockAdapter.java`
  - 상단 표현과 완전한 표현 검증 기본 API 추가
- `src/main/java/com/hyunseo/hyunseorpg/farming/VanillaCropBlockAdapter.java`
  - 2블록 설치·성장·복구·제거 구현
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropGrowthService.java`
  - 상단·하단 이벤트를 하단 canonical으로 통합
  - 청크 로드·언로드 복구
  - 상단 누락 복구 및 하단 누락 정리
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropHarvestValidator.java`
  - 상단 표현도 등록 커스텀 작물로 판정
- `src/main/resources/farming/crops.yml`
  - corn을 2블록 작물로 활성화
- `src/test/java/com/hyunseo/hyunseorpg/farming/FarmingStage1DataTest.java`
  - corn 2블록 설정과 상단 canonical 조회 테스트 추가

## 7. 자동 테스트 결과

실행:

```text
./gradlew.bat clean build
```

결과:

```text
BUILD SUCCESSFUL
6 actionable tasks: 6 executed
```

자동 검증:

- corn만 2블록 설정인지 확인
- 양파·고추·마늘 단일 블록 유지
- 상단 표현 좌표가 하단 canonical으로 연결됨
- CropIndex 중복 canonical 위치 방지
- 성장 catch-up 계산 통과
- maxStage clamp 통과
- 기존 Stage 1 전체 테스트 통과
- Paper API 컴파일 통과

실제 Bukkit 월드 이벤트의 완전한 자동화는 현재 MockBukkit 의존성이 없는 테스트 구조이므로 실서버 목록으로 분리했다.

## 8. 실서버 검증 절차

1. 최신 JAR로 서버를 완전히 재시작한다.
2. corn 씨앗을 경작지에 심는다.
3. 하단과 상단 WHEAT 표현이 동시에 생성되는지 확인한다.
4. 성장 시간이 지나면 하단·상단 age가 함께 변경되는지 확인한다.
5. 상단 블록을 외부 방식으로 제거한다.
6. 다음 성장 tick 또는 청크 reload 후 상단이 하단의 논리 stage로 복구되는지 확인한다.
7. 하단 블록을 외부 방식으로 제거한다.
8. 상단이 함께 제거되고 CropIndex가 정리되는지 확인한다.
9. 상단과 하단을 연속으로 건드려도 중복 처리·중복 드롭이 없는지 확인한다.
10. 하단 FARMLAND를 파괴한다.
11. 상단·하단 표현과 CropIndex가 함께 정리되고 FARMLAND 파괴는 허용되는지 확인한다.
12. 물 흐름으로 하단을 변경한다.
13. 피스톤으로 하단·상단 또는 목적지에 작물을 덮으려 한다.
14. 피스톤이 원본·목적지 모두에서 안전하게 차단되는지 확인한다.
15. 폭발로 하단·상단을 동시에 포함시킨다.
16. 한 canonical 작물만 정리되고 중복 처리가 없는지 확인한다.
17. 청크를 언로드하고 서버를 재시작한 뒤 다시 로드한다.
18. 하단만 데이터로 복원되고 상단은 adapter가 재생성하는지 확인한다.

## 9. 남은 제한

- 현재 WHEAT 두 블록을 사용하는 임시 표현이다.
- 상단·하단이 서로 다른 청크에 속하는 경우는 지원하지 않는다.
- 직접 수확 보상은 아직 없다.
- 품질·씨앗·진행도·판매 보상은 아직 없다.
- 상단만 존재하고 하단 CropInstance가 완전히 없는 경우, 임의로 작물 ID를 추측해 복구하지 않는다.

## 10. 빌드 산출물

- JAR: `C:\Users\User\Documents\Codex\2026-06-29\hyunseorpg-paper-spigot-rpg-hyunseorpg-1\build\libs\HyunseoRPG-0.1.0-SNAPSHOT.jar`
- SHA-256: `FEC40F26F435D39B2D9E0211E65E3BD60BB036444C1FD8CB16BF5901291D2B20`

