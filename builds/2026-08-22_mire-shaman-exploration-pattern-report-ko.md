# 수렁 주술사 전투 패턴 및 탐험 엔진 연결 보고서

기준 브랜치: `feature/golden-bulwark-mire-shaman`
기준 커밋: `4c5979e` 이후 E1-E8 탐험 모듈 병합 및 패턴 수정

## 1. 이중 검증 결과

### 코드 논리 검증

- 탐험 구조의 `scripted_spawn`이 `mob-id: custom:mire_shaman`을 읽는다.
- `ExistingHyunseoRpgAdapters.mobPort()`가 `custom:` 네임스페이스를 제거한 뒤 기존 `MobService.spawnCustomMob(location, id, level, "EXPLORATION")`를 호출한다.
- 생성된 엔티티 UUID는 `ScriptedSpawnComponent`를 거쳐 탐험 runtime objective/tracker에 등록된다.
- `MobService`가 부여한 `mire_shaman` 태그를 `MonsterBehaviorService`가 인식해 전투 tick을 수행한다.
- 탐험 모듈은 `HyunseoRPGPlugin` enable에서 생성·start되고, `reloadService`에 등록되며, disable에서 stop/flush된다.
- 기본 `exploration/structures.yml`의 전역 `enabled: false`, `selection-chance: 0.0`은 운영 서버 자동 활성화를 막는 안전 기본값이다. 외부 설정에서 활성화해야 실제 구조물 encounter가 실행된다.

### 설계 논리 검증

- 기존 E1-E8의 Registry, detection, persistent record, runtime manager, component, lifecycle 구조를 재사용했다.
- 탐험 코어에 수렁 주술사 전용 로직을 넣지 않고 `ExplorationPorts`의 범용 custom mob spawn 경계만 확장했다.
- 전투 패턴은 원거리 포션 투척이 아니라 `Toxic Pool` 공간 제어와 제한된 하수인 압박으로 구성했다.
- Golden Bulwark, 양조, 농사, 별도 AI 프레임워크는 변경하지 않았다.
- 수치와 활성화 여부는 설정에 남겼고, 최종 밸런스 및 라이브 검증 완료로 표시하지 않는다.

## 2. 변경 내용

### 탐험 연결

실행 경로:

`swamp_hut scripted_spawn`
→ `custom:mire_shaman`
→ `ExistingHyunseoRpgAdapters.mobPort`
→ `MobService.spawnCustomMob`
→ `mire_shaman` PDC/Registry 태그
→ `MonsterBehaviorService` 전투 tick

`vanilla:` 스폰도 기존 E1-E8 테스트/확장성을 위해 어댑터에서 별도로 지원한다. vanilla Witch를 custom mob으로 오인하지 않으며, `VanillaWitchSpawnBlockListener`의 `CUSTOM` 예외는 유지한다.

### 독 웅덩이

- 기존 수렁 주술사 Snowball 독 투사체와 `mire_toxic` 피해 분기를 제거했다.
- 바닐라 Witch 포션 splash는 계속 차단해 우발적인 포션 공격을 막는다.
- 예고: 밝은 외곽 ring + 약한 내부 cloud.
- 발동: 색이 진한 ring 확산, explosion particle, slime block sound.
- 활성: 보라색 외곽 ring + 올리브 내부 particle + cloud, 기존 Slowness/Poison 판정 유지.
- 예고 시간, 반경, 지속시간은 `mobs.yml`에서 읽는다.

### 하수인

- 전투 중 weighted pattern으로만 실행되며 생성 직후 자동 소환하지 않는다.
- 후보는 `SLIME`, `HUSK`, `BOGGED`다.
- 한 번에 1~2마리, 살아 있는 하수인 총량은 주술사당 5마리로 제한한다.
- Slime 크기는 최소 중간 크기인 2로 보정한다.
- 사망, 주술사 제거, 서비스 stop 시 ownership과 엔티티를 정리한다.
- active count 5에서는 summon pattern을 선택하지 않는다.

## 3. 변경 파일

- E1-E8 탐험 패키지 전체: `src/main/java/com/hyunseo/hyunseorpg/exploration/**`
- 탐험 설정 및 테스트: `src/main/resources/exploration/structures.yml`, `src/test/java/com/hyunseo/hyunseorpg/exploration/**`
- 플러그인 부트스트랩: `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
- custom mob 어댑터: `src/main/java/com/hyunseo/hyunseorpg/exploration/integration/ExistingHyunseoRpgAdapters.java`
- 수렁 주술사 정책: `src/main/java/com/hyunseo/hyunseorpg/mob/MireShamanPolicy.java`
- 수렁 주술사 runtime: `src/main/java/com/hyunseo/hyunseorpg/mob/MonsterBehaviorService.java`
- 몹 설정: `src/main/resources/mobs.yml`
- 회귀 테스트: `src/test/java/com/hyunseo/hyunseorpg/mob/MireShamanPrototypeTest.java`

## 4. 테스트 및 빌드

- clean build: 성공
- 전체 테스트: 183
- 실패: 0
- 스킵: 2
- 스킵 사유: 실제 Paper 월드가 필요한 `ScriptedSpawnComponentTest`
- 경고: 기존 Paper API deprecated 경고만 존재하며 이번 변경으로 인한 컴파일 오류는 없음

## 5. LIVE_SERVER_UNVERIFIED

- 외부 `exploration/structures.yml`에 `enabled: true`를 적용한 뒤 swamp hut가 실제 감지·기록되는지
- 오두막 encounter에서 실제 `mire_shaman`이 생성되는지
- Witch 자연/스포너/알/명령 생성 차단과 custom spawn 예외가 실서버에서 일치하는지
- 늪 biome에서 예고 ring과 활성 pool ring이 충분히 구분되는지
- Toxic Pool 피해 범위와 표시 ring이 일치하는지
- 전투 중 summon이 1~2마리로 실행되는지
- Slime 중간 크기, Husk/Bogged AI, 5마리 cap
- 주술사 사망·청크 unload·서버 종료 후 하수인과 ownership 정리

## 6. 범위 외

- Golden Bulwark 재설계 없음
- 양조/농사 코드 변경 없음
- 탐험 전역 활성화 및 최종 selection chance 확정 없음
- 라이브 서버 검증 및 최종 밸런스 확정 없음

판정: `BUILD_PASS_LIVE_SERVER_UNVERIFIED`
