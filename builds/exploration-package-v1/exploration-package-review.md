# 탐험 패키지 설계 대조 및 Codex 통합 기록

기준일: 2026-08-18
대상: `HyunseoRPG_exploration_package_v1(1).zip`
기준 설계: `3차 영지-탐험 확정안`, 영지 1차·2차 확정안, 관련 개발 체크리스트

## 최종 판정

`EXPLORATION_CORE_PACKAGE_READY / DESKTOP_INTEGRATION_PENDING / TERRITORY_NOT_INCLUDED`

이 패키지는 아이디어 모음이 아니라 구조물 탐험의 공통 엔진과 독립 삽입 가능한 Java 구현 골격을 포함한다. 양조 패키지와 같은 의미의 “간단 엔진 + 다음 실제 구현 단계”는 갖추어져 있다.

다만 탐험 설계 전체를 완성한 패키지는 아니다. 구조물 이벤트 엔진은 E1-E6까지 코어·최소 프로토타입이 준비되어 있고, 실제 최신 소스 이식·Paper 서버 검증·구조물별 콘텐츠는 후속 작업이다. 영지 시스템은 설계 문서상 T1-T9 단계가 존재하지만 이 패키지에는 포함되지 않았다.

## 1. 엔진 포함 여부

| 설계 계약 | 패키지 구현 | 판정 |
|---|---|---|
| 구조물 Registry와 Minecraft key 매핑 | `ExplorationRegistry`, `ExplorationStructureDefinition` | 구현 |
| RPG 여부·Variant 최초 1회 결정 | `DeterministicStructureSelector`, `StructureRecord` | 구현 |
| VANILLA도 저장해 재추첨 방지 | detection/repository 경로 | 구현 |
| 월드 공용 상태 모델 | `VANILLA/UNDISCOVERED/ACTIVE/CLEARED/ABANDONED` | 구현 |
| 구조물 ID 결정성 | world UUID + key + bounds 기반 | 구현 |
| 월드별 영속성 | `YamlStructureStorage`, atomic replace | 구현 |
| 다중 청크 bounds 조회 | `StructureIndex` | 구현 |
| 접근 시에만 런타임 생성 | `ExplorationRuntimeManager`, heartbeat | 구현 |
| 정상 이탈·grace·ABANDONED | movement listener/runtime manager | 구현 |
| 시스템 텔레포트 면제 | `TeleportExemptionService` | 구현 |
| 공통 Event Component API | registry/context/phase 및 7개 component | 구현 |
| 외부 모듈 결합 경계 | `ExplorationPorts`, Bukkit/기존 서비스 adapter | 구현 |
| 최소 실제 이벤트 프로토타입 | disabled `swamp_hut/elite_witch_prototype` | 템플릿 구현 |
| 실제 보상·커스텀 몹·수치 | adapter/hook 또는 BALANCE_PENDING | 미구현, 의도적 |

따라서 코어 엔진은 존재한다. 단, `RewardDropComponent`와 일부 외부 기능은 최종 서비스 연결 전까지 계약/hook 수준이며, 이를 실제 콘텐츠 완성으로 계산하면 안 된다.

## 2. 후속 구현 작업의 구체성

패키지에는 다음 순서가 명시돼 있다.

1. E1 최신 소스 재확인·구조물 감지/Registry
2. E2 Persistence/YAML/Index
3. E3 Runtime activation
4. E4 Completion/Abandon/Cleanup
5. E5 공통 Component 실제 서비스 연결
6. E6 단순 구조물 전체 생명주기 프로토타입
7. 데스크톱 소스 bootstrap·adapter·reload·빌드·Paper live test
8. E7 구조물별 콘텐츠 확장
9. E8 안정화 이후 외형 Variant

각 단계별 프롬프트에 완료 조건, 금지사항, 산출 문서와 서버 검증 항목이 있다. 특히 E7은 구조물마다 trigger, components, clear condition, abandon, cleanup, reward hook, server cost를 문서화하도록 되어 있어 실제 구현 작업으로 이어지는 명세가 충분하다.

## 3. 확정안 대조

### 일치하는 부분

- RPG 여부와 Variant를 재부팅·reload·chunk reload에서 재추첨하지 않는다.
- 구조물 이벤트는 월드 공용 1회성이다.
- 접근 전에는 Display·몹·임시 객체를 만들지 않는다.
- 물리적 도주만 ABANDONED로 처리하고 시스템 이동은 면제한다.
- 거대한 구조물별 Handler보다 작은 Component 조합을 우선한다.
- 외형 변경보다 이벤트 엔진을 먼저 닫는다.
- 구조물별 확장 순서를 단순 프로토타입에서 사막 피라미드·시련의 회당·보루·바다 신전·엔드 도시로 단계화한다.
- 영지와 탐험 구조물의 소유권 모델을 분리한다. 탐험은 월드 공용, 영지는 플레이어 귀속이다.

### 차이 또는 주의점

1. 설계는 구조물 생성 시점 감지를 우선 방향으로 적었지만, 현재 패키지의 실제 listener는 청크 로드 시 구조물 API를 조회한다. 패키지 명세에도 청크 로드 방식이 적혀 있으므로 현재 코어 결함이라기보다 Paper API 및 실제 서버 검증 후 결정할 기술 선택 사항이다. 다만 대규모 월드에서 감지 비용과 생성 직후 판정 시점은 E1/E6 live test에서 반드시 확인해야 한다.

2. 설계에 있는 `Trigger Zone`은 별도 Component로 분리되지 않고 runtime manager의 trigger radius로 흡수돼 있다. 현재 기능상 문제는 없지만, 상자 루팅·특정 오브젝트 파괴처럼 접근 이외의 시작 조건이 필요해질 때 Component 확장이 필요하다.

3. `Puzzle Chest`, `Timed Bomb`, `Quiz Gate`, `Key Match`, `Greed Counter`, `Environment Modifier` 등은 설계 목록에는 있으나 현재 패키지의 실제 Component 구현에는 없다. `PuzzleComponent`는 일반 puzzle hook이고, 구조물별 퍼즐은 E7에서 추가해야 한다.

4. 3차 확정안의 영지 T1-T9, 마을 anchor/농장/대장간 탐지, 플레이어별 바이옴 귀속, 실제 아이템 투입 해방, 현장 상점·농사 납품, 습격·5코어·보호구역은 패키지에서 의도적으로 제외됐다. `Territory: 미구현/별도 패키지 권장`은 정확한 상태 표기다.

5. 현재 `structures.yml`은 module disabled이며 selection chance가 0이다. 이는 누락이 아니라 안전한 기본값이며, 통합·Paper live test 이전에 활성화하면 안 된다.

## 4. 검증 상태

패키지 내부 보고서 기준:

- Core 직접 javac 및 smoke test 통과
- production Java API-stub compile 통과
- test source stub compile 통과
- YAML 안전 기본값 및 9개 구조물 정의 검사 통과
- farming/alchemy/effects/territory 직접 import 방화벽 검사 통과
- 전체 Gradle test와 실제 Paper 서버 검증은 실행 환경 제한으로 미실행
- 대상 프로젝트가 Java 25이므로 데스크톱 통합 시 JDK 25에서 재검증 필요

따라서 이 기록의 표현은 `implemented core`, `live unverified`, `external config unverified`를 분리한다. 정적 컴파일 결과를 실제 서버 동작 증거로 확대 해석하지 않는다.

## 5. Codex 통합 작업 기록

이 패키지는 기존 repository의 별도 브랜치에 보존한다.

- Repository: `ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper`
- Base: `main` at `412cd43817b86855203f50ba32eb8a32f9c55963`
- Branch: `codex/exploration-package-v1`
- 저장 위치: `builds/exploration-package-v1/`
- 통합 시작점: `prompts/00-latest-source-recheck.md`
- 실제 소스 편입 단계: `prompts/07-desktop-integration-and-build.md`

브랜치에는 패키지 파일과 이 검수 문서를 함께 보존한다. 아직 main에 병합하거나 탐험 모듈을 활성화하지 않는다. 이후 Codex 통합 작업에서는 먼저 최신 소스 재확인과 adapter signature 비교를 수행하고, 그 다음에만 E1부터 순서대로 진행한다.

## 6. 다음 통합 게이트

1. 최신 main 소스의 bootstrap, MobService, RPGItemService, reward/inventory, reload, command, Java/Paper target 재확인
2. 패키지 source/resource를 `builds/exploration-package-v1/`에서 작업 기준으로 사용
3. `HyunseoRPGPlugin` bootstrap은 최소 변경으로만 추가
4. JDK 25에서 `./gradlew clean test`
5. module disabled 상태로 부팅 확인
6. prototype만 활성화해 detection → activation → objective → clear/abandon → cleanup 검증
7. 통합 검증 완료 후에만 E7 콘텐츠와 별도 영지 패키지 착수

현재 결론: **탐험 엔진과 실제 구현 로드맵은 충분히 준비되어 있으며, 영지와 구조물별 콘텐츠는 아직 후속 구현 범위다.**
