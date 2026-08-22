# 탐험 persistence fail-closed 감사

- 기준 브랜치: `fix/exploration-activation-rollback`
- 수정 브랜치: `fix/exploration-persistence-fail-closed`
- 범위: 탐험 persistence만 수정. alchemy 및 `content_missing` 구현 제외.
- 상태: `STATIC_PATCH_APPLIED` / `BUILD_AND_LIVE_VERIFICATION_REQUIRED`

## 발견한 위험

`YamlStructureStorage.loadWorld()`가 지원하지 않는 schema-version이나 요청한 월드와 다른 world-uuid를 발견해도 경고 후 `List.of()`를 반환하고 있었다.

그 결과 다음 경로가 가능했다.

1. `StructureRepository.ensureWorldLoaded()`가 손상되거나 다른 월드의 파일을 빈 목록으로 로드한다.
2. 해당 월드를 정상적으로 로드한 것으로 표시한다.
3. 이후 구조물 발견이나 일반 저장이 발생한다.
4. 기존 기록이 없는 것처럼 새 YAML을 저장해 기존 기록을 덮어쓸 수 있다.

이는 오류를 숨기는 fallback이며, 기존 탐험 기록을 보존해야 하는 persistence 계약과 맞지 않는다.

## 수정 내용

schema-version 불일치와 world-uuid 불일치를 모두 `IOException`으로 전환했다.

따라서 `ExplorationModule.start()`의 로드 실패 경로로 전달되고, 해당 파일을 빈 데이터로 간주하거나 저장으로 덮어쓰지 않는다. 파일은 운영자 확인 전까지 보존된다.

개별 record의 malformed 값은 기존처럼 해당 record만 건너뛰지만, 파일 전체의 식별자/스키마 불일치는 fail-closed로 처리한다.

## 정적 확인 항목

- schema-version 불일치가 빈 목록으로 바뀌지 않음
- world-uuid 불일치가 빈 목록으로 바뀌지 않음
- 정상 schema와 UUID는 기존 로드 경로 유지
- 개별 corrupt record skip 동작은 유지
- repository가 실패한 월드를 정상 로드로 표시하지 않는지 확인 필요
- 잘못된 파일을 읽은 뒤 자동 저장으로 덮어쓰는 경로가 없는지 확인 필요

## 라이브/빌드 확인 항목

- Gradle clean build
- 전체 자동 테스트
- 잘못된 schema 파일로 기동 시 파일이 유지되는지
- 다른 월드 UUID 파일로 기동 시 파일이 유지되는지
- 정상 파일은 기존 record를 그대로 로드하는지
- 서버 재기동/월드 로드 후 persistence 오류가 명확히 로그되는지

## 검증 제한

이 브랜치에서는 GitHub 소스 수정과 정적 대조만 수행했다. Gradle build와 Paper 서버 live test는 아직 실행하지 않았다. 최종 판정은 `BUILD_AND_LIVE_VERIFICATION_REQUIRED`다.
