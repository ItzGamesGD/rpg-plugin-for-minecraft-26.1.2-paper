# 양조 U7 검증 보고서: 바닐라 촉매

## 구현

- `alchemy/catalysts.yml` 추가
- `CatalystDefinition`, `YamlCatalystRegistry` 추가
- `CatalystApplication`, `CatalystApplicationService` 추가
- `ConfigService`, `ConfigDoctor` 관리 파일, `ConfigMigrationService`에 catalysts/gui 설정 연결
- 플러그인 시작 시 Potion/Recipe/Catalyst Registry snapshot 로드

## 설계 대조

- 촉매 변환 결과는 원래 `potion_id`를 유지한다.
- PDC `data_version`과 potion Registry를 먼저 검증한다.
- 촉매는 한 번만 적용할 수 있고 기존 `catalyst_id`가 있으면 거부한다.
- 바닐라 재료는 Material ID로만 판정하며 symbolic custom item으로 대체하지 않는다.
- 현재 다섯 바닐라 촉매는 모두 disabled이며 BALANCE_PENDING 값을 임의 확정하지 않았다.

## 코드 대조

- 설정: `ConfigService` → `YamlCatalystRegistry`
- 아이템 신뢰성: `PotionPdcContract`
- potion 유효성: `PotionRegistry`
- 변환: `CatalystApplicationService`
- 기존 EffectService에 효과 로직을 복제하지 않았다.

## 검증

- catalyst 설정 존재·disabled 상태 계약 테스트 통과
- 잘못된 PDC, 비활성 potion/catalyst, 중복 catalyst fail-closed 경계 구현
- 실제 Paper 변환·투사체·잔류 영역 검증은 LIVE TEST REQUIRED

