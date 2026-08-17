# HyunseoRPG 농사 프롬프트 11 작업 보고서

기준 문서: `2de1f1cf-a333-4f83-b5e2-d4254a9206d6/pasted-text.txt`

## 작업 상태

프롬프트 7의 괭이 강화·승급 구조 보정과 프롬프트 11의 관리자·진단·마이그레이션 보완을 함께 반영했다. 기존 플레이어의 강화·승급·인챈트·농사 수치는 초기화하지 않았다.

현재 농사 플레이어 내부 버전은 기존 구현의 `3`을 유지한다. 문서의 예시 `version: 2`보다 직접 수확 풍요 포인트와 배달 상태까지 포함한 확장 형식이며, 하위 데이터는 손실 없이 상향 호환한다.

## 프롬프트 7 설계 대조

- 괭이 강화: 내구도 소비 방지와 품질 작물 판매 보너스만 사용한다.
- 괭이 승급: 아이템의 tier/star 고정 수동 효과에서 품질 분포 이동, 풍요 포인트 배율, 희귀 씨앗 확장점을 읽는다.
- 강화 설정의 `quality-density-shift` 등 승급 전용 수치는 제거·레거시 경고 대상이다.
- 플레이어 농사 단계와 괭이의 아이템 로컬 tier/star를 별도 데이터로 유지한다.
- 직접 수확 풍요 포인트는 최신 사용자 지침에 따라 유지하고, 괭이 승급 배율을 공통 지급 경로에서 사용한다.

주요 코드:

- `FarmingHoePromotionService.load()`가 YAML의 tier/star passive를 실제로 읽도록 수정했다.
- `HoeHarvestModifierService`가 강화의 `quality-sale-bonus`를 읽도록 수정했다.
- `HarvestService`가 품질은 승급 passive, 포인트는 공통 포인트 서비스 경로를 사용하도록 정리했다.
- 승급 성공 시 `FarmingPromotionService`가 괭이의 로컬 tier/star를 갱신한다.

## 프롬프트 11 반영

### 관리자 명령

기존 `/rpg farming` 명령 체계를 확장했다.

- `setpoints`, `addpoints`
- `setfavor`, `addfavor`
- `delivery reroll <player> <provider>`
- `delivery expire <player> <provider>`
- `delivery complete <player> <provider>`
- `giveprocessed <player> <crop|processed-item-id> <quality> [amount]`
- `giveessence <player> [amount]`
- `debugquality [crop] [trials]`
- `debugdelivery`

기존 `status`, 해금·잠금, 단계·수확량, 증표, `debugharvest`, `repairchunk`, `reset`은 유지했다. 관리자 권한, 콘솔 실행, 잘못된 인자 차단, 관리자 감사 로그를 기존 명령 경계에서 유지한다. 아이템 지급은 `PendingRewardService`를 사용한다.

### 자동완성

새 관리자 동작과 배달 lifecycle 인자를 자동완성 목록에 추가했다. 기존 씨앗·품질·단계 자동완성은 유지한다.

### 플레이어 데이터 마이그레이션

`ConfigMigrationService#migratePlayers()`가 명시적 마이그레이션에서만 다음을 수행한다.

- farming 섹션이 없으면 기본 프로필을 생성한다.
- 기존 farming 섹션의 누락 필드만 추가한다.
- `unlocked-crops` 경로가 없을 때만 `corn`을 추가한다.
- 명시적으로 저장된 빈 해금 목록은 유지한다.
- 풍요 포인트·기존 해금·수확량·배달 상태를 덮어쓰지 않는다.
- 변경 전 백업과 dry-run/apply 정책을 기존 마이그레이션 경로로 사용한다.
- `migrate all`에도 농사 마이그레이션을 포함시켰다.

### Doctor

`/rpg doctor farming`에서 다음을 추가로 검사한다.

- 품질 확률의 음수·비정상 값
- 품질 확률 합계 100%
- 플레이어 farming version
- 음수 풍요 포인트
- 음수 또는 상한 초과 호감도
- 손상된 배달 상태·active delivery 만료 시각
- 최신 괭이 강화·승급 설정 키
- `farming/cooking.yml` 존재 시 최신 기준 비활성 레거시 파일 경고

`farming/cooking.yml`은 최신 기준의 관리 파일 목록과 Registry 로딩 경로에 포함하지 않았다. 기존 파일은 운영 데이터 조사를 위해 삭제하지 않고 Doctor에서 비활성 상태로 표시한다.

### 설정 목록

Doctor와 마이그레이션 관리 목록에 `farming/favor.yml`을 포함했다. 현재 공식 농사 설정은 crops, growth, harvest, progression, hoe_enhancement, hoe_promotion, quality, deliveries, favor, essence, stat_tokens이다.

## 자동 검증

- `.\gradlew.bat test`: PASS
- 관리자 자동완성 회귀 테스트: PASS
- 프롬프트 7 괭이 강화·승급 설정 구조 테스트: PASS
- 품질 분포 설정의 기본 합계: 100 확인
- `.\gradlew.bat jar`: PASS

자동 테스트는 Bukkit 실서버의 명령 권한·콘솔·YAML 백업·온라인/오프라인 배달 상태 전이를 모두 대체하지 않는다.

## LIVE TEST REQUIRED

다음은 실제 서버에서 확인해야 한다.

1. `/rpg farming setpoints/addpoints` 후 재접속·재시작 저장 유지
2. `setfavor/addfavor`의 provider별 저장과 Doctor 상한 경고
3. `delivery reroll/expire/complete` 후 GUI 상태와 재생성 시점
4. 오프라인 대상 명령 실행과 PendingReward 지급
5. `/rpg migrate farming --dry-run`의 파일 무변경
6. `/rpg migrate farming --apply`의 백업 생성과 반복 실행 idempotency
7. `/rpg migrate all --apply`가 favor를 포함한 농사 설정을 병합하는지
8. `/rpg doctor farming`의 실제 외부 설정·플레이어 파일 진단
9. 괭이 강화 판매 보너스와 괭이 승급 품질 분포/풍요 포인트 배율의 실제 적용
10. 기존 괭이의 레거시 PDC와 강화·승급·내구도 보존

## 빌드

최종 JAR:

`C:\Users\User\Documents\Codex\2026-06-29\hyunseorpg-paper-spigot-rpg-hyunseorpg-1\builds\HyunseoRPG-0.1.0-SNAPSHOT-prompt11-admin-doctor-migration.jar`

SHA-256:

`B1D5E765A728F9A1F8EC3A67E4E5CBED51812234E313A31B7B32B50C5B5BED56`

`libs` 폴더는 수정하지 않았다.
