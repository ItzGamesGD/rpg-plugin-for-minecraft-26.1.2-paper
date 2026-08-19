# E8 구조 콘텐츠 프로토타입 및 회귀 검증 보고서

## 범위

- 기준 브랜치: codex/exploration-e7-full-regression-audit
- 기준 커밋: 4c9589683580f7eed638e0021f5ceec03281ef1b
- 작업 브랜치: codex/exploration-e8-structure-content-prototype
- 목표: 탐험 패키지의 제한적 구조 콘텐츠 1종을 기존 엔진에 연결
- 제외: 영지/territory 구현, 실제 서버 JAR 교체, 운영 설정 활성화

이번 단계는 구조 콘텐츠의 첫 번째 실제 정의를 추가하되, 기본 상태에서는 실행되지 않도록 닫아 둔 단계다.

## 구현 내용

### Pillager Outpost 프로토타입

src/main/resources/exploration/structures.yml 의 pillager_outpost에 다음 변형을 추가했다.

- variant: scout_wave_prototype
- component: scripted_spawn
- mob-id: vanilla:pillager
- count: 2
- objective: true
- 상대 위치: (0, 1, 0)
- prototype: true

기존 ExplorationComponentRegistry, ScriptedSpawnComponent, ExplorationRuntime, RuntimeObjectTracker를 그대로 사용한다. 별도 우회 실행 경로와 별도 정리 경로는 만들지 않았다.

### 안전 기본값

다음 값은 그대로 유지된다.

- package enabled: false
- pillager_outpost.enabled: false
- pillager_outpost.selection-chance: 0.0
- 기존 구조의 선택 확률도 변경하지 않음
- 기존 swamp hut 프로토타입도 활성화하지 않음

따라서 이 커밋만으로 운영 서버에서 구조물이 자동 실행되거나 pillager가 생성되지 않는다.

## 정적 검증 추가

추가 테스트:

ExplorationStructureContentPrototypeTest.outpostScoutWaveIsDefinedButSafeByDefault

검증 항목:

- 전체 패키지 기본 비활성
- outpost 기본 비활성
- outpost 선택 확률 0
- variant와 prototype 플래그 존재
- scripted_spawn 계약
- vanilla:pillager
- count 2
- objective true
- 상대 위치 (0, 1, 0)

실행 환경에서 Gradle 테스트를 직접 실행하지 않았으므로 결과는 LIVE_SERVER_VERIFICATION_REQUIRED 및 BUILD_VERIFICATION_REQUIRED 상태로 남긴다.

## E1~E7 충돌·회귀 점검

| 단계 | 점검 결과 |
|---|---|
| E1 registry/detection | 기존 namespaced Minecraft key와 registry parser를 사용한다. 중복 key를 추가하지 않았다. |
| E2 persistence | 저장 모델·schema-version·기존 record 재선정 규칙을 변경하지 않았다. |
| E3 trigger/activation | 새 component는 기존 activation phase 경로만 사용한다. 기본 비활성이라 trigger를 추가로 발생시키지 않는다. |
| E4 minimal execution | objective 2개를 기존 scripted spawn 결과로 추적한다. 유효 UUID가 없으면 기존 activation failure 계약을 따른다. |
| E5 lifecycle | 기존 tracker cleanup, heartbeat invalid-object cleanup, end reason 기록을 그대로 사용한다. |
| E6 status snapshot | 별도 상태 필드를 만들지 않고 runtime의 objective/tracked object 집계를 사용한다. |
| E7 regression audit | E7에서 확인한 live-only 항목과 의도적 보류 항목을 해소했다고 주장하지 않는다. 이번 변경은 content definition과 static contract만 추가한다. |

## 여전히 의도적으로 보류된 항목

다음은 이번 단계에서 구현하지 않았다.

- 실제 라이브 서버에서 outpost 위치 감지 및 variant 선택 결과
- 운영용 확률·반경·grace 밸런스
- outpost의 실제 RPG 이벤트 연출·경고 UI
- 커스텀 pillager/NMS 데이터
- chest loot 또는 바닐라 보상 변경
- RPG reward 정의 및 reward delivery
- clear 조건의 최종 기획값
- 재접속·월드 unload·서버 재시작의 운영 데이터 검증
- 다중 플레이어 참여/이탈 정책의 최종값
- 구조물별 추가 콘텐츠
- 영지/territory 연결

즉 이번 단계에서 남은 CONTENT_MISSING은 단순 누락이 아니라, 현재 공통 엔진의 안전한 프로토타입과 실제 콘텐츠 설계를 분리하기 위해 남겨 둔 항목이다.

## E8 라이브 테스트 목록

실제 테스트 서버에서는 운영 설정을 직접 덮어쓰지 말고 별도 테스트 사본에서만 일시적으로 활성화한다.

1. 기본 설정으로 서버 기동 시 outpost 이벤트와 pillager가 발생하지 않는지 확인
2. 테스트 설정에서 outpost만 활성화하고 확률을 임시 지정했을 때 registry가 올바른 Minecraft key를 인식하는지 확인
3. 실제 minecraft:pillager_outpost 진입 시 outpost runtime이 1회 생성되는지 확인
4. scout_wave_prototype이 선택되고 pillager 2마리가 구조물 기준 상대 위치에 생성되는지 확인
5. 두 생성 entity가 objective로 runtime에 등록되는지 확인
6. objective entity가 유효한 동안 heartbeat가 runtime을 조기 종료하지 않는지 확인
7. 두 entity가 모두 제거된 뒤 정상 종료되는지 확인
8. 구조물에서 abandon radius를 벗어난 뒤 grace 기간 후 entity와 runtime이 정리되는지 확인
9. 재접속 후 동일 persistent record가 재선정되거나 중복 실행되지 않는지 확인
10. 서버 reload/restart 이후 저장된 record와 runtime 복원이 설계와 일치하는지 확인
11. plugin disable/world unload 시 명시적 lifecycle cleanup만 수행되는지 확인
12. pillager outpost의 바닐라 chest/loot가 변경되지 않는지 확인
13. reward 설정이 없는 프로토타입에서 허위 RPG 보상이 발생하지 않는지 확인
14. /rpg exploration status 또는 관리자 진단에서 runtime/objective/last end reason이 확인되는지 확인
15. 테스트 설정을 원복한 뒤 다음 기동에서도 outpost가 다시 자동 실행되지 않는지 확인

각 항목은 다음 정보를 함께 기록해야 한다.

- commit SHA와 JAR SHA-256
- 서버 버전 및 플러그인 버전
- 테스트 월드/구조물 좌표
- runtime ID, variant ID
- 생성 entity UUID
- 시작 tick, 종료 tick
- 종료 reason
- 재현 여부 및 서버 로그

## 판정

- 정적 설계 계약: 추가됨
- 구조 콘텐츠 정의: 추가됨
- 운영 활성화: 하지 않음
- Gradle clean build: 미실행
- 자동 테스트 실행: 미실행
- 라이브 서버 검증: 미실행
- 최종 상태: LIVE_SERVER_VERIFICATION_REQUIRED, BUILD_VERIFICATION_REQUIRED

영지 단계로 넘어가지 않고, 다음 단위에서는 이 프로토타입을 테스트 서버에서 검증하거나 구조 콘텐츠를 추가하기 전에 E8의 static contract부터 통과시켜야 한다.
