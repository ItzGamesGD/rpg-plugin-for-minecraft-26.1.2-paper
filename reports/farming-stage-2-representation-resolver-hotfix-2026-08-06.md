# HyunseoRPG 농사 Stage 2 승인 전 최소 판정 핫픽스 보고서

작성일: 2026-08-06  
작업 상태: `IMPLEMENTED`  
자동 검증: `PASSED`  
실서버 검증: `PENDING`

## 1. 수정 범위

이번 작업은 요청된 두 항목만 수정했다.

1. 단일 블록 작물의 바로 위 블록이 커스텀 작물로 오판되는 문제
2. 옥수수 설치 후 CropIndex 등록 실패 시 하단만 rollback되는 문제

다음 기능은 구현하지 않았다.

- 수확
- 해금
- 플레이어 데이터
- 품질
- 괭이 연동
- 리소스팩·BetterModel·새 표현 Material

기존 Stage 1 성장·청크·간접 파괴·옥수수 2블록 구조는 유지했다.

## 2. 판정 계층 수정

### 기존 문제

`CropIndex.findByRepresentation()`이 모든 작물에 대해 다음 블록을 상단 표현으로 인정했다.

```text
현재 좌표에 CropInstance가 없으면 y - 1 좌표 조회
```

따라서 양파·고추·마늘의 바로 위 블록도 커스텀 작물로 판정될 수 있었다.

### 변경 후 구조

`CropIndex`는 이제 canonical 위치의 저장·조회만 담당한다.

새 클래스:

```text
CropRepresentationResolver
├─ canonical 위치 직접 조회
└─ y - 1 후보 조회 후 CropDefinition.twoBlock() 확인
```

판정 규칙:

- 현재 좌표에 canonical `CropInstance`가 있으면 항상 반환
- 현재 좌표에 없을 때만 같은 X/Z의 `y - 1`을 후보로 조회
- 후보 작물의 `CropDefinition.twoBlock()`이 `true`일 때만 상단 표현으로 반환
- `twoBlock: false`인 양파·고추·마늘은 바로 위 블록을 커스텀 작물로 판정하지 않음
- 다른 X/Z 좌표나 다른 청크는 조회하지 않음

`CropHarvestValidator`와 `CropGrowthService`는 모두 동일한 `CropRepresentationResolver`를 사용한다. 판정 규칙을 각각 복제하지 않았다.

## 3. 설치 rollback 수정

기존 등록 실패 처리:

```java
target.setType(Material.AIR, false);
```

이 방식은 옥수수 하단만 지우고 상단을 남길 수 있었다.

변경 후:

```java
blockAdapter.clearRepresentation(target, definition);
```

등록 실패 시 adapter가 canonical 하단과 2블록 작물의 상단 표현을 함께 제거한다. `CropGrowthService`가 WHEAT 또는 AIR를 직접 하드코딩하지 않는다.

## 4. 변경 파일

- `src/main/java/com/hyunseo/hyunseorpg/farming/CropRepresentationResolver.java`
  - 작물 정의의 `twoBlock` 여부를 반영하는 단일 표현 판정기
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropIndex.java`
  - 잘못된 범용 `findByRepresentation` 제거
  - canonical 위치 조회 전용으로 축소
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropHarvestValidator.java`
  - Resolver 사용으로 판정 규칙 통일
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropGrowthService.java`
  - Resolver 연결
  - 설치 등록 실패 rollback을 `clearRepresentation`으로 변경
- `src/test/java/com/hyunseo/hyunseorpg/farming/FarmingStage1DataTest.java`
  - 옥수수 상단 판정
  - 양파·고추·마늘 상단 오판정 방지
  - canonical 하단 판정 유지

## 5. 자동 테스트

확인 항목:

- 옥수수 상단은 하단 canonical instance로 판정
- 양파 바로 위 블록은 작물로 판정하지 않음
- 고추 바로 위 블록은 작물로 판정하지 않음
- 마늘 바로 위 블록은 작물로 판정하지 않음
- 모든 작물의 canonical 하단은 기존대로 판정
- CropIndex가 CropRegistry에 직접 의존하지 않음
- `CropHarvestValidator`와 `CropGrowthService`가 같은 Resolver 사용
- 기존 Stage 1 전체 테스트 통과

실행:

```text
./gradlew.bat clean build
BUILD SUCCESSFUL
6 actionable tasks: 6 executed
```

기존 Paper API deprecated 경고 16건은 남아 있으나 이번 변경으로 인한 오류는 없다.

## 6. 실서버 확인 항목

1. 옥수수 설치 시 하단·상단이 함께 생성되는지 확인
2. 설치 직후 등록 충돌 상황에서 하단·상단이 모두 rollback되는지 확인
3. 양파·고추·마늘 바로 위에 일반 블록을 설치·파괴해도 커스텀 작물 판정이 발생하지 않는지 확인
4. 옥수수 상단을 물리·폭발·간접 파괴 경로로 처리할 때 하단 canonical 하나만 처리되는지 확인
5. 일반 단일 작물의 직접 파괴·지지 블록 처리 범위가 기존 Stage 1과 동일한지 확인

## 7. 빌드 산출물

- JAR: `C:\Users\User\Documents\Codex\2026-06-29\hyunseorpg-paper-spigot-rpg-hyunseorpg-1\build\libs\HyunseoRPG-0.1.0-SNAPSHOT.jar`
- SHA-256: `E35BB6210EB1E9B2B3544999AF3847645DCFB96364B811A3344D9B9E956DD858`

