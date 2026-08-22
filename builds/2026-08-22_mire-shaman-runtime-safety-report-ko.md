# 수렁 주술사 프로토타입 런타임 안전성 보고서

## 기준

- 브랜치: `feature/golden-bulwark-mire-shaman`
- 시작 HEAD: `163a5b639a0debc55a603a1143bf8ed4edbc4152`
- 종료 HEAD: 작업 트리 변경 미커밋 상태. HEAD는 시작 커밋과 동일하다.
- 기준 소스: 최신 원격 기능 브랜치. 기존 `main` 작업 트리의 사용자 체크포인트 파일은 변경하지 않았다.

## 실제 확인 결과

`mire_shaman`은 기존 `MobService.spawnCustomMob()` → `MobRegistry` → `MobTagService` →
`MonsterBehaviorService` 경로를 사용하는 네이티브 RPG 커스텀 몹이다. MythicMobs 의존성이나
새 몹 프레임워크는 추가하지 않았다.

기존 브랜치에는 독성 진흙 웅덩이, 작은 슬라임 하수인, 느린 독성 투사체, HP 50% 이하 1회성
웅덩이 회수/회복 구현이 있었다. 그러나 정의가 `custom-mobs` 밖에 있었기 때문에
`MobRegistry`가 `mire_shaman`을 찾지 못하는 실제 등록 결함이 있었다.

## 수정

- `mobs.yml`
  - `mire_shaman`, `golden_bulwark` 정의를 canonical `custom-mobs` 아래로 이동했다.
  - 수렁 주술사 표시 이름을 `수렁 주술사`로 수정했다.
- `ConfigMigrationService`
  - 명시적 config migration에서 과거 top-level `mire_shaman`/`golden_bulwark` 정의를
    `custom-mobs`로 이동하고, 없는 키만 기본값으로 보충한다.
  - 기존 운영자 값은 덮어쓰지 않으며, apply 전용 migration의 백업 정책을 따른다.
- `VanillaWitchSpawnBlockListener`
  - 자연 스폰, 스포너, 알, 명령 등 바닐라가 제어하는 WITCH 스폰을 차단한다.
  - 플러그인 `CUSTOM` 원인은 예외로 둔다. 수렁 주술사는 Bukkit 생성 뒤 PDC가 붙으므로
    이 예외가 없으면 수렁 주술사도 생성 시점에 취소된다.
- `MonsterBehaviorService`
  - 수렁 주술사 투사체를 `ThrownPotion` 대신 `Snowball`로 교체해 바닐라 Witch potion
    splash 처리와 분리했다. 투사체는 발사 시점 snapshot 방향만 사용하며 유도하지 않는다.
  - 하수인 사망/무효 상태를 소유 추적에서 제거하고, 소환 수가 active cap을 넘지 않도록 했다.
  - 웅덩이 회수 조건을 순수 정책(`MireShamanPolicy`)으로 분리해 소유 웅덩이가 있고
    최초 50% 이하 진입일 때만 발동하도록 검증했다.

## 자동 검증

- `MireShamanPrototypeTest`
  - canonical 설정 위치와 WITCH 기반 정의
  - `수렁 주술사` 표시 이름
  - 회수 1회성/소유 웅덩이 조건
  - 하수인 cap
  - 바닐라 WITCH 차단과 CUSTOM WITCH 허용
- 전체 결과: `162 tests / 0 failures / 0 skipped`
- `./gradlew clean build`: 성공
- JAR: `build/libs/HyunseoRPG-0.1.0-SNAPSHOT.jar`

## 탐험 연결 감사

현재 이 브랜치에는 `exploration` 모듈, `swamp_hut`, `ScriptedSpawnComponent`, 구조물 variant
설정이 존재하지 않는다. 따라서 `vanilla:witch`를 `custom:mire_shaman`으로 바꿀 실제 설정/호출
지점이 없다.

이 연결을 임의의 신규 구조물 엔진으로 만들어서는 안 되므로, 다음은 **미구현이 아니라
현재 소스 부재로 인한 BLOCKED** 상태다.

- `swamp_hut` prototype의 custom spawn ID 교체
- objective UUID 등록/activation/abandon/rejoin lifecycle 연결
- structure runtime 실패 로그에 structure/variant 식별자 추가

탐험 모듈이 포함된 실제 후속 소스를 제공하거나 병합한 뒤, 그 모듈의 기존 scripted spawn
resolver에 `mire_shaman`을 연결해야 한다.

## Paper 라이브 테스트

1. `migrate configs --dry-run`으로 legacy top-level 정의 이동 예정 여부를 확인하고,
   필요할 때만 `--apply` 후 `/rpg reload all`을 실행한다.
2. 관리자 권한으로 `/rpg mob spawncustom mire_shaman`을 실행한다.
3. 이름이 `수렁 주술사`이고 WITCH 모델이며, 독성 장판 telegraph/고정 장판, 작은 슬라임,
   비유도 투사체가 동작하는지 확인한다.
4. HP 50% 이하에서 자기 장판만 한 번 회수하고 회복하는지 확인한다.
5. 주술사 처치와 플러그인 disable 뒤 장판 및 소환 슬라임이 남지 않는지 확인한다.
6. 자연/스포너/알/명령으로 WITCH를 만들었을 때 취소되는지, `spawncustom mire_shaman`은
   정상 생성되는지 확인한다.

## 범위 밖

- 상징 3개 의식 퍼즐
- 가마솥 상호작용 및 최종 소환 연출
- 보상 지점 및 reward pool
- 오두막 외형/마탑 variant
- 구조물 엔진 및 `swamp_hut` 연결
- 최종 패턴 수치 밸런스
