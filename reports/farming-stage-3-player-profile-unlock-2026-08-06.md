# HyunseoRPG 농사 Stage 3 구현 보고서

## 상태

- Implementation status: COMPLETE
- Automated verification: PASSED
- Live gameplay verification: PENDING
- Balance verification: OUT OF SCOPE
- 작업 범위: 플레이어 농사 데이터, 씨앗 해금 상태, 심기 차단

Stage 1·Stage 2의 작물 저장, canonical 하단 좌표, 표현 Resolver, 옥수수 상·하단 롤백 구조는 변경하지 않았다.

## 구현 내용

### 농사 단계

`FarmingStage`를 추가했다.

| 내부 값 | 표시 이름 |
|---|---|
| `BASIC` | 기본 |
| `SKILLED` | 숙련 |
| `PROFICIENT` | 능숙 |
| `ADVANCED` | 상급 |
| `EXPERT` | 전문 |

실제 승급 조건은 이번 단계에서 구현하지 않았다.

### 플레이어 데이터

기존 `PlayerRPGData`에 다음 농사 전용 데이터를 추가했다. 별도 플레이어 파일은 만들지 않는다.

```yaml
farming:
  version: 1
  stage: BASIC
  total-valid-harvests: 0
  crop-harvests: {}
  unlocked-crops:
    - corn
  stat-token-uses: {}
```

기존 플레이어 파일에 `farming` 항목이 없으면 기본값으로 초기화된다.

- 기본 농사 단계: `BASIC`
- 기본 해금 작물: `corn`
- 양파·고추·마늘: 잠금
- 유효 수확 수: `0`
- 농사 데이터 버전: `1`

기존 최상위 `schema-version`은 변경하지 않았다.

### 공용 관리 API

`FarmingProfileService`가 기존 `PlayerDataService`를 통해 다음 기능을 제공한다.

- `getFarmingProfile(Player)`
- `isCropUnlocked(Player, cropId)`
- `unlockCrop(Player, cropId)`
- `lockCrop(Player, cropId)`
- `setStage(Player, FarmingStage)`
- `addValidHarvest(Player, cropId, amount)`
- `getCropHarvestCount(Player, cropId)`

변경 메서드는 기존 저장 서비스에 dirty 상태를 등록한다. 실제 디스크 저장은 기존 자동 저장·로그아웃·서버 종료 흐름을 사용한다.

### 씨앗 심기 차단

`CropGrowthService.onPlant()`에서 `CropRegistry`가 찾은 작물 ID를 `FarmingProfileService`로 검사한다.

- 해금 작물: 기존 Stage 1 심기 흐름 진행
- 잠긴 작물: `PlayerInteractEvent`를 씨앗 차감 전에 취소
- 잠긴 씨앗: 별도 지급하지 않음. 이벤트 취소로 원래 손에 든 수량이 그대로 유지됨
- 중복 반환: 별도 반환 로직이 없어 발생하지 않음
- 잠금 상태: 플레이어 데이터에 귀속되며 씨앗·괭이·아이템 소유권으로 이전되지 않음

해금 API는 준비했지만 실제 승급·수확 조건과 자동 해금은 구현하지 않았다.

## 변경 파일

### 신규

- `src/main/java/com/hyunseo/hyunseorpg/farming/FarmingStage.java`
  - 농사 단계 enum과 내부/표시명 변환
- `src/main/java/com/hyunseo/hyunseorpg/farming/FarmingProfile.java`
  - 플레이어 농사 데이터의 불변 조회 모델
- `src/main/java/com/hyunseo/hyunseorpg/farming/FarmingProfileService.java`
  - 기존 플레이어 데이터 서비스와 농사 기능 사이의 공용 경계
- `src/test/java/com/hyunseo/hyunseorpg/farming/FarmingStage3DataTest.java`
  - 기본 해금, 플레이어별 분리, 수확 수·단계·토큰 데이터 검증

### 수정

- `src/main/java/com/hyunseo/hyunseorpg/player/PlayerRPGData.java`
  - 농사 데이터 필드와 조회·변경 API 추가
  - 수확 수 누적 시 음수 차단 및 long overflow 포화 처리
- `src/main/java/com/hyunseo/hyunseorpg/player/PlayerDataService.java`
  - UUID 기반 lazy 조회, 로드 완료 확인, 농사 섹션 마이그레이션 dirty 등록, 즉시 저장 경계 추가
- `src/main/java/com/hyunseo/hyunseorpg/player/YamlPlayerDataRepository.java`
  - `farming.*` YAML 로드·저장 추가
  - 기존 파일에 농사 항목이 없어도 기본값으로 호환
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropGrowthService.java`
  - 잠긴 작물의 심기 이벤트 차단
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
  - `FarmingProfileService` 생성·주입·getter 연결

삭제한 클래스와 기존 기능 삭제는 없다.

## 검증 결과

실행:

```text
./gradlew.bat test
./gradlew.bat clean build
```

결과:

- `BUILD SUCCESSFUL`
- 기존 테스트 및 Stage 3 테스트 통과
- 최종 빌드 테스트 포함 통과
- 기존 Paper API deprecated 경고 16건 존재
- 이번 변경으로 발생한 컴파일 오류 없음

자동 검증 범위:

- 새 프로필 기본값은 BASIC이며 corn만 해금
- 다른 `PlayerRPGData` 인스턴스와 해금·수확 수가 분리됨
- 농사 단계 한글명·영문명 변환
- 농사 단계 외부 입력 오류 무시 가능
- 유효 수확 수와 작물별 수확 수 분리
- 토큰 사용 데이터 ID 정규화

## 실서버 검증 필요

다음은 실제 Paper 서버에서 확인해야 한다.

1. 신규 플레이어 접속 후 옥수수 씨앗 심기
2. 양파·고추·마늘 씨앗을 보유한 상태에서 심기 차단 및 수량 유지
3. 관리자 또는 향후 API 호출로 양파 해금 후 실제 심기
4. 로그아웃·재접속 후 해금 상태와 농사 단계 유지
5. 서버 재시작 후 `players/<uuid>.yml`의 `farming.*` 유지
6. 기존 Stage 1 작물 성장·청크 저장·복원 회귀 확인
7. 옥수수 상단 표현과 Stage 2의 canonical 하단 판정 회귀 확인

## 이번 단계에서 구현하지 않은 항목

- 실제 농사 승급 조건
- 수확 처리 및 수확 보상
- 품질 작물
- FarmingProfile 기반 해금 자동화
- 괭이 강화·승급 연동
- 판매·가공·요리
- 풍요의 정수·증표
- 범위 수확

## 릴리스 산출물

## 데이터 안전화 보완

후속 검토에서 확인된 오류 예방 사항을 같은 Stage 3 범위 안에서 반영했다.

- `setStage()`는 단계만 변경하며 기존 해금을 잠그거나 삭제하지 않는다.
- `resetFarmingProfile()`만 단계·해금·유효 수확 수·토큰 사용을 초기화한다.
- `unlocked-crops` 경로가 없을 때만 기본 corn을 사용한다.
- 경로가 존재하지만 빈 목록이면 빈 목록을 그대로 유지한다.
- 기존 파일에 `farming` 섹션이 없으면 기본 프로필을 dirty 상태로 등록해 저장한다.
- `unlockCrop()`·`lockCrop()`은 정규화 후 현재 `CropRegistry`에 존재하는 ID만 저장한다.
- UUID 기반 API를 추가해 향후 오프라인 관리자 작업으로 확장할 수 있게 했다.
- `applyPromotionResult(UUID, ...)`에서 단계 변경과 작물 해금을 한 번에 저장하고 저장 실패 시 메모리 상태를 복구한다.
- 플레이어 데이터가 로드되지 않은 상태에서는 심기 이벤트를 취소한다.
- Stage 4 수확 호출부는 아직 없으며, `addValidHarvest`는 향후 확정 수확 완료 지점에서 한 번만 연결해야 한다.

### 보완 후 실서버 미검증

- 기존 본계정 YAML의 농사 외 필드 왕복 보존
- farming 섹션 없는 기존 파일의 자동 dirty 저장
- `unlocked-crops: []` 재접속 유지
- 잠긴 씨앗 수량 유지 및 플레이어 간 해금 분리
- 서버 재시작 후 farming 하위 데이터 유지

최신 산출물:

- 파일: `builds/2026-08-06_110-farming-stage-3-player-profile-unlock.jar`
- SHA-256: `F3857CE68BCA507CCCF25F095A8EC3FFFD7888C925E9D60284D89617A5916A28`
