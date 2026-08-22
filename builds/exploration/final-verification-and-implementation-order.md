# 최종 재검증·구현 작업 순서 안내서

기준 브랜치: `codex/exploration-runtime-prototype`
목적: 과거 회귀 검증, 최신 소스 통합, 현재 누락 콘텐츠, 정적 검증, 라이브 검증을 시간축과 기능축으로 분리한다.

## 원칙

1. 과거 기준은 보존하고 최신 기준과 섞어 판정하지 않는다.
2. 브랜치 전체를 무조건 통합하지 않고 필요한 커밋·패치·파일만 가져온다.
3. 정적 검증 통과 전에는 라이브 테스트를 시작하지 않는다.
4. 라이브 실패 시 기능 단위 fix 브랜치에서 수정하고 통합 대상만 갱신한다.
5. 엔진 완료, 최소 실행 콘텐츠 완료, 최종 콘텐츠 완료를 별도 상태로 기록한다.

## 1단계 - 기준선 고정

- 최신 소스의 기준 commit/branch 기록
- 라이브 테스트 완료 branch와 미검증 branch 구분
- 과거 완료 보고서와 최신 소스의 변경점 비교
- main은 수정하지 않고 통합용 branch 생성
- JAR 자동 덮어쓰기 금지

산출물: 기준선 표, branch/commit 매핑, 기존 회귀 목록

## 2단계 - 과거 회귀 검증

기존 양조·농사·효과·명령·아이템 PDC·vanilla catalyst 계약을 현재 소스에서 다시 확인한다.

- 기존 테스트 결과와 현재 테스트 결과 비교
- 이전에 고친 오류가 재발하지 않았는지 확인
- 라이브에서 이미 확인된 동작과 코드상 미검증 동작 분리
- 과거 오류는 최신 기능의 실패로 재분류하지 않는다

## 3단계 - 최신 기능 단위 통합

양조를 먼저 최신 소스 기준으로 통합한다.

- 양조 기능 단위별 선택적 반영
- compile/test
- Gradle clean build
- 제한된 양조 콘텐츠 live test
- 실패 시 `fix/alchemy-...` 브랜치 생성
- 통과한 양조 기준 commit 고정

그 다음 탐험을 반영한다.

- 탐험 engine
- structure runtime
- 최소 실행 prototype
- event/reward 단위
- 구조물별 콘텐츠

## 4단계 - 제한적 실행 콘텐츠 검증

엔진만 있는 상태를 완료로 보지 않는다. 다만 전체 콘텐츠를 한 번에 만들지도 않는다.

첫 번째 실행 경로:

`structure detection → RPG decision → activation → objective → clear/abandon → cleanup`

탐험은 최소 swamp hut/witch prototype으로 시작한다. 이 경로가 정적·라이브 검증을 통과한 뒤 실제 보상·퍼즐·구조물별 이벤트를 추가한다.

## 5단계 - 콘텐츠 확장

콘텐츠는 하나의 묶음씩 추가한다.

- 실제 mob/content ID
- reward/item delivery
- clear condition
- abandon/failure
- visual/interaction
- persistence/duplicate prevention

각 묶음마다 정적 회귀 → 빌드 → 라이브 테스트를 반복한다.

## 6단계 - 오류 수정

라이브 오류가 발생하면:

1. 재현 절차와 실제 결과 기록
2. 로그·debug state·commit diff 대조
3. 원인 기능 단위 확정
4. 해당 통합 branch에서 fix branch 생성
5. 수정 후 정적 회귀와 라이브 재검증
6. 원인 commit이 확실할 때만 revert
7. 검증된 수정만 통합 branch에 반영

문서만 보고 원인을 단정하지 않는다. 로그와 재현 결과가 없으면 `LIVE_SERVER_VERIFICATION_REQUIRED`로 남긴다.

## 7단계 - 최종 검증

- 과거 회귀 전체
- 최신 소스 종합 회귀
- 모든 구현 콘텐츠 대조
- 의도적 미구현과 누락 구현 분리
- 설정/YAML/리소스/명령 확인
- Gradle clean build
- 전체 테스트 수와 신규 테스트 수
- JAR 및 SHA-256
- commit SHA와 branch
- Paper live 결과
- 미검증 항목 명시

## 최종 상태 분류

- `IMPLEMENTED_STATIC_VERIFIED`
- `IMPLEMENTED_LIVE_VERIFIED`
- `INTENTIONALLY_DEFERRED_CONTENT`
- `CURRENT_RUNTIME_BLOCKER`
- `OBSERVABILITY_MISSING`
- `LIVE_SERVER_VERIFICATION_REQUIRED`
- `SEPARATE_SCOPE`

현재 다음 작업은 탐험 최소 실행 prototype이며, 모든 구조물 콘텐츠를 한 번에 구현하는 단계가 아니다.
