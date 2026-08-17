# HyunseoRPG 농사 Stage 1 경작지 밟기 십자가 삭제 핫픽스 보고서

작성일: 2026-08-04  
작업 상태: `IMPLEMENTED`  
자동 검증: `PASSED`  
실서버 검증: `PENDING`

## 1. 십자가 삭제의 정확한 이벤트 순서

기존 흐름은 다음과 같았다.

1. 플레이어가 중앙 FARMLAND를 밟음
2. 서버가 중앙 및 인접 FARMLAND에 `BlockPhysicsEvent`를 전달
3. `CropGrowthService.onBlockPhysics()`가 이벤트 블록이 FARMLAND이면 `clearSupportedCrop()` 실행
4. 각 FARMLAND의 바로 위 작물을 CropIndex에서 제거
5. 결과적으로 중앙·상·하·좌·우 작물이 한 번에 삭제

`BlockPhysicsEvent`는 실제 파괴나 토양 변환을 의미하지 않으므로, 이 이벤트를 지지 작물 제거에 사용한 것이 근본 원인이었다.

## 2. BlockPhysicsEvent 수정

다음 분기를 제거했다.

```java
else if (event.getBlock().getType() == Material.FARMLAND) {
    clearSupportedCrop(event.getBlock());
}
```

등록 작물 블록에 `BlockPhysicsEvent`가 발생한 경우에도 무조건 제거하지 않는다. 다음 상태를 모두 확인한다.

- CropIndex에 해당 canonical 작물 존재
- CropDefinition 존재
- 바로 아래 블록의 Material이 해당 정의의 soil에 포함
- 현재 블록 표현이 `CropBlockAdapter.isRepresentation()`과 일치

검증 실패 시에만 해당 이벤트 블록의 canonical 작물 하나를 제거한다. 주변 FARMLAND나 주변 작물은 조회하지 않는다.

## 3. EntityChangeBlockEvent 추가

`EntityChangeBlockEvent`를 등록했다.

처리 조건:

- 이벤트 블록이 FARMLAND
- `event.getTo()`가 FARMLAND가 아님
- 동일 좌표의 바로 위 블록에 등록 작물 존재

처리 결과:

- 해당 FARMLAND 바로 위 작물 하나만 제거
- CropIndex에서 canonical 위치 제거
- `CropBlockAdapter.clearRepresentation()`으로 드롭 없이 표현 제거
- 청크 dirty 표시
- EntityChangeBlockEvent 자체는 취소하지 않음

플레이어·동물 등 Entity 종류를 구분하지 않는다.

## 4. 정확한 좌표 처리

모든 지지 블록 처리는 오직 다음 좌표만 사용한다.

```text
support: (x, y, z)
crop:    (x, y + 1, z)
```

북·남·동·서·대각선 탐색은 없다.

현재 Stage 1에서는 이벤트 하나가 제거할 수 있는 canonical 작물은 최대 1개다. Stage 2 옥수수에서도 이 하나의 canonical 작물이 상·하 표현을 함께 제거하는 구조로 확장할 수 있으며, 주변 canonical 작물에는 영향을 주지 않는다.

## 5. 직접 파괴·Fade 정밀화

### BlockBreakEvent

- FARMLAND 직접 파괴 시 바로 위 작물만 제거
- FARMLAND 파괴는 허용
- 주변 작물 유지

### BlockFadeEvent

- FARMLAND의 새 Material을 확인
- 새 Material이 해당 작물 정의의 허용 soil이면 제거하지 않음
- 허용 soil이 아니면 바로 위 작물 하나만 제거
- 주변 작물 유지

`BlockPhysicsEvent`는 위 두 이벤트의 대체 수단으로 사용하지 않는다.

## 6. 피스톤 목적지 검증

확장·수축 모두 이동 원본과 목적지를 확인한다.

```text
확장 목적지 = source.getRelative(direction)
수축 목적지 = source.getRelative(direction.getOppositeFace())
```

다음 중 하나라도 해당하면 이벤트를 취소한다.

- 이동 원본이 등록 작물
- 이동 원본이 등록 작물의 바로 아래 FARMLAND
- 이동 목적지가 등록 작물
- 이동 목적지가 등록 작물의 바로 아래 FARMLAND

이동 목록은 CropIndex의 canonical 위치와 정확한 상하 관계만 검사하며, 주변 블록을 탐색하지 않는다.

## 7. 랜덤 틱 정책

커스텀 작물은 `randomTickSpeed`를 사용하지 않는다.

- Bukkit 스케줄러
- `nextGrowthAt`
- `growth.yml`의 `seconds-per-stage`

만으로 성장한다. 따라서 `randomTickSpeed`를 높여도 커스텀 작물 성장 시간은 변하지 않아야 한다. 테스트 시 성장 시간을 바꾸려면 `growth.yml`의 `seconds-per-stage`를 조정한다.

## 8. 디버그 로그

`src/main/resources/farming/growth.yml`에 다음 설정을 추가했다.

```yaml
debug:
  events: false
```

기본값은 `false`다. `true`로 설정하면 주요 이벤트에 대해 다음을 기록한다.

- 이벤트 종류
- 이벤트 블록 월드 UUID와 좌표
- BlockPhysicsEvent source 좌표
- 변경 전 Material
- 변경 예정 Material
- 제거된 canonical 작물 좌표

테스트 후 다시 `false`로 두면 된다.

## 9. 변경 파일

- `src/main/java/com/hyunseo/hyunseorpg/farming/CropGrowthService.java`
  - BlockPhysicsEvent 처리 축소
  - EntityChangeBlockEvent 추가
  - Fade·직접 파괴 정밀화
  - 피스톤 목적지 검사
  - 이벤트 디버그 로그
- `src/main/resources/farming/growth.yml`
  - `debug.events: false` 추가
- `src/test/java/com/hyunseo/hyunseorpg/farming/FarmingStage1DataTest.java`
  - 디버그 기본값 false 검증

## 10. 자동 검증 결과

실행:

```text
./gradlew.bat clean build
```

결과:

```text
BUILD SUCCESSFUL
6 actionable tasks: 6 executed
```

확인 항목:

- 기존 전체 테스트 통과
- Stage 1 설정 테스트 통과
- CropIndex canonical 중복 방지 테스트 통과
- 성장 catch-up 순수 계산 테스트 통과
- 디버그 기본값 false 테스트 통과
- Paper 이벤트 API 컴파일 통과

기존 Paper API deprecated 경고 16건은 남아 있으나 이번 변경으로 인한 컴파일 오류는 없다.

## 11. 3x3 실서버 테스트 절차

1. 핫픽스 JAR 배포 후 서버를 완전히 재시작한다.
2. 3x3 FARMLAND를 만들고 9칸 모두에 커스텀 작물을 설치한다.
3. 중앙 FARMLAND를 플레이어가 밟는다.
4. 중앙 작물만 제거되는지 확인한다.
5. 북·남·동·서·대각선 8개 작물이 유지되는지 확인한다.
6. 각 모서리 FARMLAND도 순서대로 밟고 해당 위치 작물 하나만 제거되는지 확인한다.
7. 작물 옆에 일반 블록을 설치·파괴한다.
8. 주변 작물이나 중앙 작물이 삭제되지 않는지 확인한다.
9. FARMLAND를 직접 파괴한다.
10. 바로 위 작물 하나만 드롭 없이 제거되고 주변 작물은 유지되는지 확인한다.
11. FARMLAND가 자연적으로 DIRT로 fade되도록 한다.
12. 바로 위 작물 하나만 제거되는지 확인한다.
13. 몹이 FARMLAND를 밟도록 한다.
14. 플레이어와 동일하게 해당 위치 작물 하나만 제거되는지 확인한다.
15. `growth.yml`의 `debug.events`를 `true`로 바꾸고 reload 또는 재시작한다.
16. 로그에서 이벤트 블록 좌표와 제거된 canonical 좌표가 동일한 상하 관계인지 확인한다.
17. `debug.events`를 다시 `false`로 변경한다.
18. `randomTickSpeed`를 크게 높인다.
19. 커스텀 작물 성장 시간이 `growth.yml` 설정과 동일하게 유지되는지 확인한다.
20. 피스톤으로 일반 블록을 작물 방향으로 밀어 작물을 덮으려 한다.
21. 작물 또는 지지 FARMLAND를 덮는 목적지라면 피스톤이 차단되는지 확인한다.

## 12. 빌드 산출물

- JAR: `C:\Users\User\Documents\Codex\2026-06-29\hyunseorpg-paper-spigot-rpg-hyunseorpg-1\build\libs\HyunseoRPG-0.1.0-SNAPSHOT.jar`
- SHA-256: `CA2E7EC13FC4A13E84DBA3EEC1A414B47295F94394E6C4F543C4A38EA56FEC58`

