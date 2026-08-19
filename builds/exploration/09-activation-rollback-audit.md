# 탐험 활성화 실패 원상복구 감사

- 기준 브랜치: `codex/exploration-live-verification`
- 수정 브랜치: `fix/exploration-activation-rollback`
- 수정 commit: `e8fa370e68a3bc08037878510efc0d536de09eb5`
- 상태: `STATIC_PATCH_APPLIED` / `BUILD_AND_LIVE_VERIFICATION_REQUIRED`
- 범위: 탐험 공통 runtime. `content_missing` 구현 제외.

## 발견한 결함

활성화 경로는 다음 순서였다.

`UNDISCOVERED` record 저장 → `ACTIVE` 전환 → runtime 등록 → ACTIVATE component 실행

ACTIVATE component가 예외를 던지면 기존 코드는 Bukkit runtime object만 정리하고 persistent record는 `ACTIVE`로 남겼다. 그 결과 다음 상태가 가능했다.

- `StructureRecord.state = ACTIVE`
- `activeRuntimes`에는 해당 구조물 없음
- heartbeat가 재활성화하지 않음
- 다음 접근에서 정상적인 `UNDISCOVERED → ACTIVE` 재시도 불가

## 수정 내용

ACTIVATE 단계 실패 시:

1. active map에서 runtime 제거
2. 이미 등록된 임시 object cleanup
3. 원래 `UNDISCOVERED` record를 repository에 다시 저장
4. 다음 heartbeat/접근에서 안전하게 재시도 가능하도록 유지

rollback 저장 자체가 실패하면 원래 예외에 suppressed exception으로 남긴다. 운영 서버에서 이 경우에는 persistence 오류로 별도 조사한다.

## 정적 확인 항목

- 정상 경로: `UNDISCOVERED → ACTIVE` 후 runtime 1개
- ACTIVATE 실패: `UNDISCOVERED` 유지, runtime 0개
- ACTIVATE 중 생성된 entity/display/interaction은 cleanup
- 이미 `ACTIVE`인 record의 재접근은 기존 runtime에 participant만 추가
- `CLEARED`/`ABANDONED` record는 재활성화하지 않음

## 라이브 확인 항목

- component가 실패하는 개발용 구조물에서 영구 `ACTIVE` 고착이 없는지
- 실패 후 재접근 시 동일 record가 재시도되는지
- 실패 전후 임시 entity/display/block/task가 남지 않는지
- 정상 swamp hut prototype에서는 이 rollback 경로가 오작동하지 않는지

## 검증 제한

이 환경에서는 GitHub 소스 수정과 정적 대조만 수행했다. Gradle clean build, 전체 테스트, Paper 서버 live test는 아직 실행하지 않았다. 따라서 이 브랜치의 최종 판정은 `BUILD_AND_LIVE_VERIFICATION_REQUIRED`다.
