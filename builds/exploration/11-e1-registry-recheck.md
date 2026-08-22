# E1 최신 소스 재확인·Registry 감사

기준 브랜치: `fix/exploration-persistence-fail-closed`  
작업 브랜치: `codex/exploration-e1-registry-recheck`  
기준 커밋: `55846e97c68645201467e6f1008746ba33b7c4cd`

## 범위

- 최신 탐험 소스의 구조물 key 입력 경계 확인
- YAML Registry의 ID/key/chance/variant 계약 확인
- 구조물 ID와 RPG/vanilla 최초 판정의 영속화 경계 확인
- 구조물 런타임, 실제 구조물 이벤트, 보상, 확률 활성화는 E1에서 실행하지 않음

## 확인 결과

- `ExplorationRegistry`는 ID와 Minecraft key를 정규화하고 중복을 거부한다.
- `ExplorationStructureDefinition`은 chance를 0..1로 제한하고, 활성 RPG 정의에는 활성 variant가 필요하다.
- `ReflectivePaperStructureCandidateProvider`는 Paper 구조물 API가 없으면 안전하게 감지를 중지한다.
- `DeterministicStructureSelector`는 world/key/bounds 기반 stable ID와 결정적 RPG/variant 선택을 제공한다.
- `structures.yml`은 top-level `enabled=false`, selection chance `0.0`의 안전 기본값을 유지한다.
- E1 보정으로 등록됐지만 per-structure `enabled=false`인 구조물도 모듈이 켜진 뒤 최초 관측 시 `VANILLA` 레코드로 저장한다. 이후 설정 변경으로 같은 구조물이 재추첨되지 않는다.

## 정적 검증

- 모든 등록 구조물은 namespaced Minecraft key를 가진다.
- 등록 key는 중복되지 않는다.
- 모든 selection chance는 0..1 범위다.
- 기존 안전 기본값과 swamp hut prototype 설정은 유지된다.
- 실제 Gradle/JDK 25 빌드와 Paper 서버 검증은 아직 실행하지 않았다.

## 상태

`IMPLEMENTED_STATIC_VERIFIED / LIVE_SERVER_VERIFICATION_REQUIRED`

다음 E2에서 이 기록을 기준으로 persistence/YAML/index를 검증한다. 운영 월드 활성화와 실제 확률 표본 테스트는 최종 live gate까지 보류한다.
