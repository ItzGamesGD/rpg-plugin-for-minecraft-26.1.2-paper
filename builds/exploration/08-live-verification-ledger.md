# 탐험 최소 실행 경로 라이브 검증 단위

- 기준 브랜치: `codex/exploration-runtime-prototype`
- 기준 commit: `19c1d8f0af49f0dd205428c58349d35d68264efa`
- 작업 브랜치: `codex/exploration-live-verification`
- 상태: `LIVE_SERVER_VERIFICATION_REQUIRED`
- 범위: `content_missing` 구현 제외

## 이번 단계의 목적

탐험 엔진이 실제 Paper 서버에서 구조물 단위 실행을 시작하고 종료할 수 있는지 확인한다. 전체 구조물 콘텐츠를 구현하는 단계가 아니라, 이미 편입된 공통 runtime 경로의 실제 동작을 검증하는 단계다.

검증 경로:

`structure detection → RPG decision → activation → objective/runtime → clear or abandon → cleanup`

## 정적 기준

다음 항목은 최신 탐험 브랜치의 소스·리소스·정적 테스트에 존재한다.

- 구조물 Registry와 variant 선택
- 구조물 후보 감지 및 다중 청크 bounds 모델
- 월드 공용 record와 YAML persistence
- 접근 trigger와 runtime manager
- clear/abandon 상태 모델
- chunk/world/plugin lifecycle 경계
- teleport exemption 경계
- 탐험 설정의 안전 기본값
- 구조물 selector, config data, index, record transition 테스트

이 목록은 실제 Paper 동작을 통과했다는 뜻이 아니다.

## 라이브 검증 순서

1. 탐험 모듈 비활성 기본값으로 서버가 정상 부팅되는지 확인한다.
2. 개발 월드에서만 탐험 모듈과 구조물 selection chance를 임시 활성화한다.
3. 실제 Paper 구조물 key와 YAML key가 일치하는지 확인한다.
4. 구조물 생성·청크 로드 직후 RPG runtime이 중복 생성되지 않는지 확인한다.
5. 접근 반경 밖에서는 display, mob, task, objective가 생성되지 않는지 확인한다.
6. 접근 시 runtime이 하나만 생성되고 구조물 UUID·variant·state가 연결되는지 확인한다.
7. 재접속과 chunk unload/load 후 record가 재추첨되지 않는지 확인한다.
8. 시스템 teleport는 abandon을 발생시키지 않는지 확인한다.
9. 실제 이탈 후 grace가 지나면 ABANDONED가 되고 임시 객체와 task가 정리되는지 확인한다.
10. clear 경로에서 CLEARED 전환과 cleanup 경계를 확인한다.
11. plugin disable/world unload 때 persistence flush와 runtime 정리를 확인한다.
12. reload 중 활성 runtime의 record·variant·state가 변하지 않는지 확인한다.

## 판정 규칙

- 구조물 확률 값이 YAML에 존재하는 것만으로 생성 성공으로 판정하지 않는다.
- 실제 구조물 key, 실제 생성 결과, RPG 판정, 접근 전후 runtime 수를 별도로 기록한다.
- 로그나 관리자 debug 출력이 부족하면 기능 실패로 단정하지 않고 `OBSERVABILITY_MISSING`으로 기록한다.
- 실제 실행 콘텐츠가 없는 항목은 `CONTENT_MISSING`으로 유지하며 이번 단계에서 구현하지 않는다.
- 라이브 결과가 없는 항목은 `LIVE_SERVER_VERIFICATION_REQUIRED`로 유지한다.
- 문제 발생 시 이 브랜치에서 직접 무조건 수정하지 않고, 원인 기능 단위의 새 fix 브랜치를 만든다.

## 이번 단계 종료 조건

다음이 모두 확인되면 탐험 공통 runtime 검증을 통과로 본다.

- 감지·판정·접근 활성화가 정확히 한 번 실행됨
- 재접속·chunk reload·reload에서 중복 runtime이 없음
- clear와 abandon이 구분됨
- 시스템 teleport가 오판정을 만들지 않음
- plugin/world/chunk lifecycle cleanup이 남지 않음
- persistence가 재부팅 후 동일 record를 복원함
- 운영 기본값에서 자동 활성화되지 않음

이후에만 실제 swamp hut/witch prototype을 최소 콘텐츠 단위로 구현한다. Puzzle Chest, Timed Bomb, Quiz Gate, Key Match, Greed Counter, 구조물별 보상과 몹 등은 다음 콘텐츠 단위로 분리한다.
