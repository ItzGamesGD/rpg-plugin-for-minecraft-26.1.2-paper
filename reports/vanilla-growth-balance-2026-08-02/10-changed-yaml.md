# YAML 변경 및 운영 적용 상태

## 변경된 기본값

- `src/main/resources/exp.yml`: `base-level.required-exp-multiplier`를 1.0으로 설정. 실측 전 중립값.
- `src/main/java/.../ConfigMigrationService.java`: 외부 `crafting.yml`에 `magic_stone_from_fragments`와 재료 카테고리·layout을 누락 시 병합.

## 변경하지 않은 값

- 활동 코인
- 신호기 가격
- 인챈트 가격
- 강화 수수료
- 수리비
- 원소 진입 레벨
- 보스 보상

위 값들은 현재 외부 설정값을 보고서에 기록했지만 balance-pending이며 자동 변경하지 않는다.

## 상태

소스 기본값은 SOURCE_DEFAULT_UPDATED, 외부 `crafting.yml`은 EXTERNAL_CONFIG_MIGRATION_REQUIRED, 실제 서버는 RUNTIME_APPLIED 아님이다.
