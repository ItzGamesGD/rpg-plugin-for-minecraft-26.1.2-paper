# 양조 U11 진입 전 Production Completion·Migration Gap Audit

## 결론

현재 판정은 `NOT_READY_FOR_U11`이다. U11 측정 양식부터 작성하고 수치를 넣는 것은
순서가 아니며, 실제 운영 양조 루프가 아직 완성되지 않았다. 이번 작업에서는 임의의
전투 수치나 제작 비용을 확정하지 않고, 안전하게 회수 가능한 공통 포션 경로만 연결했다.

## 감사 기준과 이중 검증

대조 대상은 다음 네 묶음이다.

- 원본 scaffold의 Java/resource 계약, status, deferred/pending, Prompt 13 gate
- Java/Desktop Integration Plan의 U0-U12 구현 순서
- 현재 `src/main/java`, `src/main/resources`, plugin bootstrap 및 reload 경로
- 기존 U0-U11 보고서와 자동 테스트

판정은 “파일 또는 Registry가 존재하는가”가 아니라 `설정 → Registry → GUI/명령 →
트랜잭션 → ItemStack/PDC → 사용 이벤트 → EffectService` 호출 경로가 실제로 이어지는지를
기준으로 했다.

## Migration Gap Audit

| 요구 영역 | 현재 소스 판정 | 근거 | 조치 |
|---|---|---|---|
| Effect Registry/Definition | `PARTIALLY_IMPLEMENTED` | 8개 ID와 YAML 정의는 있으나 production handler가 `HandlerRegistry`에 등록되지 않음 | U11 전 회수 필요 |
| Effect handler/apply/tick/cleanup | `MISSING_IMPLEMENTATION` | `EffectService`는 handler가 없으면 비어 있지 않은 `handler-id` 효과 적용을 거부함 | 활성화 금지 |
| Potion Registry/PDC | `PARTIALLY_IMPLEMENTED` | Registry와 PDC 계약 존재. 기존에는 실제 Factory/소비 listener가 없음 | 이번 작업에서 Factory·소비 listener 회수 |
| Potion ItemStack 생성 | `IMPLEMENTED_BUT_DISABLED_FOR_VALID_REASON` | `PotionFactory`가 ItemRegistry와 PDC를 함께 사용하지만 production potion이 disabled | 효과 handler 후 활성화 |
| Potion 사용 | `PARTIALLY_IMPLEMENTED` | `PaperPotionUseService`는 존재하나 bootstrap 소비 이벤트가 없었음. 이번에 listener 등록 | 효과 handler 후 재검증 |
| Production recipe 8종 | `CONTRACT_ONLY` | `alchemy/recipes.yml`은 있으나 전부 disabled이고 canonical `crafting.yml`의 alchemy recipe도 없음 | 현재 측정 불가 |
| 풍요의 정수 | `PARTIALLY_IMPLEMENTED` | canonical point-only crafting recipe와 farming bridge는 있으나 최신 외부 설정 migration 및 운영 GUI 실측 필요 | 외부 설정 확인 필요 |
| 부식의 정수 | `CONTRACT_ONLY` | ItemRegistry ID는 있으나 풍요의 정수에서 원자적으로 생성하는 실제 recipe/transaction 경로가 없음 | U11 전 회수 필요 |
| Vanilla catalyst | `CONTRACT_ONLY` | Registry·검증·PDC 변환 서비스는 있으나 enabled 항목과 실제 적용 이벤트가 없음 | 활성화 금지 |
| Special catalyst | `CONTRACT_ONLY` | bounded job registry는 있으나 production definition이 disabled이고 실제 potion 변환 진입이 없음 | 미래가 아닌 현재 목록은 회수 필요 |
| GUI | `PARTIALLY_IMPLEMENTED` | Hub와 기존 Crafting GUI 진입은 존재하나 active alchemy recipe가 0개라 제작 화면이 비어 있음 | recipe·handler 후 검증 |
| Reload/cleanup | `IMPLEMENTED_BUT_DISABLED_FOR_VALID_REASON` | alchemy Registry reload와 job/GUI cleanup은 bootstrap에 연결됨 | 전역 원자 snapshot은 잔여 위험 |
| Symbolic ingredient | `IMPLEMENTED` | 현재 source recipe는 `vanilla:<MATERIAL>`과 실제 processed ID를 사용하며 tomato/chili placeholder는 제거됨 | 외부 YML 재검사 필요 |

원본 문서의 개별 계약 수를 숫자로 부풀리지 않았다. U11 gate에 직접 연결되는 13개
운영 영역을 모두 추적했으며, 그중 `MISSING_IMPLEMENTATION`이 현재 production 범위에
남아 있으므로 전체 gate는 실패다.

## 이번에 회수한 구현

1. `PotionFactory` 추가
   - enabled Registry 정의만 생성
   - `RPGItemService.create(output-item-id)` 사용
   - `potion_id`, `data_version` PDC 기록
   - 존재하지 않거나 disabled인 결과는 생성하지 않음
2. `PaperPotionUseListener` 추가
   - PDC가 없는 일반 아이템은 건드리지 않음
   - PDC가 있는 아이템만 `PotionUseService`로 전달
   - 실패 시 `PlayerItemConsumeEvent` 취소
3. `PaperPotionUseService`에 ItemRegistry identity 검증 추가
   - PDC만 복사한 가짜 물약 거부
   - Potion ID, data version, enabled 상태, canonical output item을 함께 검사
4. canonical `CraftingTransactionService`에 PotionFactory dynamic output 연결
   - 일반 제작 output 경로를 복제하지 않음
   - 활성 potion recipe가 생기면 같은 원자 트랜잭션을 사용
5. `/rpg alchemy give`가 직접 `RPGItemService + PDC`를 조합하지 않고 PotionFactory를 사용

## 8개 production effect 상태

| 효과 | Definition | Handler | Apply/Tick | 상태 |
|---|---|---|---|---|
| 흡혈 | 있음 | 없음 | 미연결 | `MISSING_IMPLEMENTATION` |
| 광폭화 | 있음 | 없음 | 미연결 | `MISSING_IMPLEMENTATION` |
| 부식 | 있음 | 없음 | 미연결 | `MISSING_IMPLEMENTATION` |
| 동상 | 있음 | 없음 | 미연결 | `MISSING_IMPLEMENTATION` |
| 감전 | 있음 | 없음 | 미연결 | `MISSING_IMPLEMENTATION` |
| 출혈 | 있음 | 없음 | 미연결 | `MISSING_IMPLEMENTATION` |
| 취약 | 있음 | 없음 | 미연결 | `MISSING_IMPLEMENTATION` |
| 괴사 | 있음 | 없음 | 미연결 | `MISSING_IMPLEMENTATION` |

수치가 `BALANCE_PENDING`인 것은 구현 보류 사유가 될 수 있지만, 현재처럼 handler
자체가 없는 것은 수치 보류가 아니라 기능 누락이다.

## 8개 production potion 상태

| 물약 | ItemRegistry/PDC | Recipe | GUI | Use | Effect | 활성 |
|---|---|---|---|---|---|---|
| 흡혈 | 준비됨 | YAML 계약만 | 빈 목록 | Factory/listener 연결 | 없음 | 아니오 |
| 광폭화 | 준비됨 | YAML 계약만 | 빈 목록 | Factory/listener 연결 | 없음 | 아니오 |
| 부식 | 준비됨 | YAML 계약만 | 빈 목록 | Factory/listener 연결 | 없음 | 아니오 |
| 동상 | 준비됨 | YAML 계약만 | 빈 목록 | Factory/listener 연결 | 없음 | 아니오 |
| 감전 | 준비됨 | YAML 계약만 | 빈 목록 | Factory/listener 연결 | 없음 | 아니오 |
| 출혈 | 준비됨 | YAML 계약만 | 빈 목록 | Factory/listener 연결 | 없음 | 아니오 |
| 취약 | 준비됨 | YAML 계약만 | 빈 목록 | Factory/listener 연결 | 없음 | 아니오 |
| 괴사 | 준비됨 | YAML 계약만 | 빈 목록 | Factory/listener 연결 | 없음 | 아니오 |

## 실제 ingredient 감사

현재 source 기준 recipe ingredient는 다음 원칙을 따른다.

- 농사 canonical: `processed_garlic_concentrate_normal`, `processed_chili_extract_normal`
- 공통 농사 재료: `abundance_essence`, `corrosion_essence`
- 바닐라: `vanilla:SPIDER_EYE`, `vanilla:BLAZE_POWDER`, `vanilla:SLIME_BALL`,
  `vanilla:ROTTEN_FLESH`, `vanilla:SNOWBALL`, `vanilla:ICE`, `vanilla:COPPER_INGOT`,
  `vanilla:REDSTONE`, `vanilla:CACTUS`, `vanilla:WITHER_ROSE`
- 제거된 잘못된 placeholder: `tomato_concentrate`, `chili_powder`

`corrosion_essence`는 ItemRegistry에는 있으나 현재 실제 교환 recipe가 없으므로,
recipe가 추가되기 전까지 production loop가 닫혔다고 판정하지 않는다.

## Catalyst 상태

| Catalyst | Registry/정책 | 실제 변환·전파 | 판정 |
|---|---|---|---|
| Redstone / Glowstone | 있음 | Registry enabled 전, GUI 적용 경로 미연결 | `CONTRACT_ONLY` |
| Gunpowder / Dragon's Breath | 있음 | delivery 변환 계약만 있음 | `CONTRACT_ONLY` |
| Fermented Spider Eye | 있음 | inversion 계약만 있음 | `CONTRACT_ONLY` |
| Sculk / Echo / Slime / Wind Charge | 있음 | bounded 실행 경계는 있으나 실제 handler·PDC 사용 경로 없음 | `CONTRACT_ONLY` |

현재 catalyst를 임의 활성화하지 않았다.

## 변경 파일

- `src/main/java/com/hyunseo/hyunseorpg/alchemy/potion/PotionFactory.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/potion/PaperPotionUseListener.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/potion/PaperPotionUseService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/potion/PotionUseService.java`
- `src/main/java/com/hyunseo/hyunseorpg/command/RPGGiveCommand.java`
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`

## 검증 결과

- `./gradlew.bat test`: 성공
- 전체 자동 테스트: `143 tests / 0 failures`
- 컴파일: 성공
- 실제 Paper 서버: 미검증
- 실제 외부 `plugins/HyunseoRPG` YML migration/apply: 미실행

## 남은 BLOCKED / 다음 구현 대상

1. 8개 production effect concrete handler와 combat/skill/healing/lifecycle 연결
2. `corrosion_essence` 원자 교환 recipe와 실제 ItemStack delivery
3. 8개 potion recipe를 canonical Crafting GUI에 연결하고 activation gate 통과
4. vanilla catalyst의 실제 적용·전달·PDC 상태 변환
5. special catalyst의 실제 projectile/entity/job 실행
6. invalid reload snapshot, full inventory, close/logout/kick/reload/shutdown의 실서버 증거
7. 외부 YML migration 후 `/rpg reload all`과 재시작 결과

위 항목은 미래 콘텐츠가 아니라 현재 production 범위 누락이므로 `FUTURE_DISABLED`로
분류하지 않았다. 반대로 `goddess_tear`, `demon_tear`, 미래 원소 장비·보스 연계는
`FUTURE_DISABLED`를 유지한다.

## U11 진입 여부

`NOT_READY_FOR_U11`

현재 Factory·PDC·사용 이벤트 공통 경로는 회수했지만, 8개 효과 handler와 실제
recipe/catalyst runtime path가 없어 Prompt 13 측정 대상으로 넘길 수 없다.
