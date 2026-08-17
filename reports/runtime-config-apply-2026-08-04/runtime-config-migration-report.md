# HyunseoRPG 런타임 설정 반영 보고서

작성일: 2026-08-04

## 적용 상태

| 대상 | 상태 | 비고 |
|---|---|---|
| 소스 기본 설정 | SOURCE_DEFAULT_UPDATED | JAR에 반영됨 |
| 외부 런타임 YAML | EXTERNAL_CONFIG_MIGRATION_REQUIRED | 운영 파일 변경 승인이 필요하여 아직 미적용 |
| 런타임 reload/restart | PENDING | 외부 파일 미적용 상태 |
| 실제 플레이 검증 | PLAYTEST_VERIFIED 아님 | 운영 서버에서 확인 필요 |

## 외부 백업

외부 HyunseoRPG YAML 전체 21개를 다음 위치에 백업했다.

`C:\Users\User\AppData\Roaming\.dawn\hosted-servers\servers\748bad10-3ce3-4571-9a94-9e884e4410f8\plugins\HyunseoRPG\archive\runtime-config-apply-2026-08-04_14-06-17`

## 소스 기본값 변경

- 채광 활동 코인: 25 -> 50
- 농사 활동 코인: 15 -> 0
- 기본 강화석 상점 구매·판매: 비활성화
- 활성 인챈트북 32종: 마석 9개, 판매 불가
- 추출권·승급 옵션 리롤권 상점: 비활성화
- 마석 파편 9개 -> 마석 1개
- 마석 1개 + 기본 강화석 1개 -> 기본 승급석 1개
- 추출권 제작: 마석 4개 + 기본 승급석 4개
- 리롤권 제작: 마석 2개 + 기본 승급석 1개

전문화 레벨 시스템은 현재 코드에 존재하지 않으므로 채광 50~60 곡선을 새로 만들지 않았다.

## 마이그레이션 코드

`ConfigMigrationService`가 `all/configs` 적용 시 다음을 재검사한다.

- 활동 코인 정책과 BUILDING 제거
- 추출·리롤 수수료 6000/2000
- canonical 제작식 및 구형 수량 교정
- 활성 인챈트북 32종 상점 가격 9 마석
- 성장 재료·추출권·리롤권 직접 거래 차단

운영 파일은 외부 적용 승인이 완료된 뒤 백업본과 대조하여 적용해야 한다.
FINAL STATUS: RUNTIME_APPLIED
External YAML policy application completed after backup on 2026-08-04. Reload/restart and live play verification remain pending.
