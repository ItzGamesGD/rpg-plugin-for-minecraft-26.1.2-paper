# HyunseoRPG 양조 Production Completion A 보고서

상태: COMPLETION_A_PASS
범위: production effect 8종, corrosion_essence, production potion 8종
제외: Catalyst runtime, U11 Prompt 13 플레이테스트

## 1. 판정 기준

코드 논리 검증과 설계 논리 검증을 분리해 수행했다.

코드 경로:

canonical ingredient
→ CraftingRecipeRegistry
→ CraftingGuiService
→ CraftingTransactionService
→ PotionFactory
→ Potion PDC ItemStack
→ PlayerItemConsumeEvent
→ PaperPotionUseService
→ EffectService
→ concrete production handler

설계 대조:

- 기존 Registry/Service/GUI/transaction 경계를 재사용했다.
- 신규 포션마다 Java recipe branch를 만들지 않았다.
- tomato_concentrate와 chili_powder를 사용하지 않았다.
- Catalyst는 계속 별도 비활성 범위로 남겼다.
- 수치와 재료량은 임시 기준값이며 최종 밸런스로 확정하지 않았다.

## 2. Production effect 상태

| Effect ID | Handler | 실제 동작 | 정리 경로 | 상태 |
|---|---|---|---|---|
| effect_vampire | VampirismEffectHandler | 공격 이벤트 후 피해량의 임시 5% 회복 | 사망·로그아웃·월드 이동·만료·reload·shutdown | BALANCE_PENDING |
| effect_berserk | BerserkEffectHandler | 가하는 피해 1.10배, 받는 피해 1.05배 | EffectService lifecycle | BALANCE_PENDING |
| effect_corrosion | CorrosionEffectHandler | 받는 피해 1.10배 | EffectService lifecycle | BALANCE_PENDING |
| effect_frostbite | FrostbiteEffectHandler | 이동 속도 임시 -15% AttributeModifier | modifier 제거 및 stale modifier cleanup | BALANCE_PENDING |
| effect_shock | ShockEffectHandler | 20틱마다 임시 1 피해 | 만료·lifecycle 및 대상 사망 | BALANCE_PENDING |
| effect_bleed | BleedEffectHandler | 20틱마다 임시 1 피해 | 만료·lifecycle 및 대상 사망 | BALANCE_PENDING |
| effect_vulnerability | VulnerabilityEffectHandler | 받는 피해 1.20배 | EffectService lifecycle | BALANCE_PENDING |
| effect_necrosis | NecrosisEffectHandler | 최대 체력 임시 -10% AttributeModifier | modifier 제거 후 현재 체력 clamp | BALANCE_PENDING |

모든 handler는 `HandlerRegistry`에 실제 등록되고, `EffectService.apply/applyDebug/tick/remove` 경로를 사용한다. 재적용·충돌·스택·만료 정책은 기존 EffectService의 설정 계약을 사용한다. 현재 production YAML은 `REFRESH_DURATION`, 최대 스택 1로 두었다.

## 3. 부식의 정수

| 항목 | 구현 |
|---|---|
| 입력 | `abundance_essence` 1개 + `vanilla:ROTTEN_FLESH` 1개 |
| 출력 | `corrosion_essence` 1개 |
| 경로 | `crafting.yml` → CraftingRecipeRegistry → 기존 GUI/TransactionService |
| 접근 | 농사 단계 `expert` 이상 임시 기준 |
| 원자성 | 기존 output capacity 선검사·clone/commit/rollback 경로 사용 |
| 상태 | 활성, 재료량과 단계는 BALANCE_PENDING |

부패 재료에 대한 별도 custom item을 만들지 않고 명시적인 vanilla material mapping을 사용했다.

## 4. Production potion 8종

| Potion | Item | PDC | Factory | Recipe/GUI | Consume/Effect | 상태 |
|---|---|---|---|---|---|---|
| potion_vampire | 실제 `potion_vampire` | `potion_id`, `data_version` | 연결 | `alchemy_potion` | effect_vampire | 활성 |
| potion_berserk | 실제 `potion_berserk` | 동일 | 연결 | 동일 | effect_berserk | 활성 |
| potion_corrosion | 실제 `potion_corrosion` | 동일 | 연결 | 동일 | effect_corrosion | 활성 |
| potion_frostbite | 실제 `potion_frostbite` | 동일 | 연결 | 동일 | effect_frostbite | 활성 |
| potion_shock | 실제 `potion_shock` | 동일 | 연결 | 동일 | effect_shock | 활성 |
| potion_bleed | 실제 `potion_bleed` | 동일 | 연결 | 동일 | effect_bleed | 활성 |
| potion_vulnerability | 실제 `potion_vulnerability` | 동일 | 연결 | 동일 | effect_vulnerability | 활성 |
| potion_necrosis | 실제 `potion_necrosis` | 동일 | 연결 | 동일 | effect_necrosis | 활성 |

`PotionFactory`는 Registry에 존재하고 enabled인 정의만 생성한다. `PaperPotionUseService`는 output item과 PDC를 함께 검증하므로 이름이나 PDC만 조작한 fake potion은 거부된다.

## 5. 실제 production ingredient

- `abundance_essence`
- `corrosion_essence`
- `processed_garlic_concentrate_normal`
- `processed_chili_extract_normal`
- `vanilla:SPIDER_EYE`
- `vanilla:BLAZE_POWDER`
- `vanilla:SUGAR`
- `vanilla:SLIME_BALL`
- `vanilla:ROTTEN_FLESH`
- `vanilla:SNOWBALL`
- `vanilla:ICE`
- `vanilla:COPPER_INGOT`
- `vanilla:REDSTONE`
- `vanilla:CACTUS`
- `vanilla:WITHER_ROSE`

현재 소스의 RPGItemRegistry와 대조했으며 `tomato_concentrate`, `chili_powder`는 production ingredient로 사용하지 않았다.

## 6. Migration

`migrate alchemy --apply`와 `migrate all`에서 다음을 명시적으로 처리한다.

- 누락된 alchemy YAML tree 병합
- 누락된 8개 potion item과 `corrosion_essence` item 병합
- 누락된 canonical `crafting.yml` alchemy recipe 병합
- materials fallback category에 recipe ID 병합
- 구현 완료된 8개 effect/potion/recipe와 alchemy GUI 활성화
- production status를 `IMPLEMENTED_RUNTIME`으로 정리

기존 operator 값은 일반적인 누락 병합에서는 덮어쓰지 않는다. Production A 활성화는 명시적 migration apply에서만 수행한다. Catalyst와 `potion_test_speed`는 활성화하지 않는다. `migrate farming`은 alchemy recipe를 임의로 함께 설치하지 않도록 processing/essence와 alchemy migration을 분리했다.

## 7. 변경 파일

Java:

- `src/main/java/com/hyunseo/hyunseorpg/alchemy/CombatEffectModifier.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/AbstractProductionEffectHandler.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/VampirismEffectHandler.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/BerserkEffectHandler.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/CorrosionEffectHandler.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/FrostbiteEffectHandler.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/ShockEffectHandler.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/BleedEffectHandler.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/VulnerabilityEffectHandler.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/NecrosisEffectHandler.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/ProductionEffectListener.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/gui/AlchemyGuiControllerService.java`
- `src/main/java/com/hyunseo/hyunseorpg/crafting/CraftingGuiService.java`
- `src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigMigrationService.java`
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`

YAML:

- `src/main/resources/alchemy/effects.yml`
- `src/main/resources/alchemy/potions.yml`
- `src/main/resources/alchemy/recipes.yml`
- `src/main/resources/alchemy/gui.yml`
- `src/main/resources/crafting.yml`

Tests:

- 기존 Alchemy U10/U11 계약 테스트 갱신
- `AlchemyProductionCompletionATest` 추가

## 8. 검증 결과

- Gradle test: PASS
- 전체 테스트: 145
- 실패: 0
- skipped: 0
- Gradle jar: PASS
- Paper 실서버 테스트: 아직 수행하지 않음

## 9. 빌드 산출물

`builds/alchemy/HyunseoRPG-0.1.0-SNAPSHOT-alchemy-production-completion-a.jar`

- 크기: 15,771,453 bytes
- SHA-256: `F124842FAEE13E491D26640161973B80179586302ADEC600368A6AEEF2D8548D`

## 10. 실서버에서 확인할 항목

1. 기존 서버 설정 백업 후 `migrate alchemy --dry-run` 변경 예정 확인
2. `migrate alchemy --apply` 후 `reload alchemy` 성공 확인
3. `/rpg alchemy`에서 정수·물약 제작 메뉴 노출 확인
4. 부식의 정수 제작 시 재료 부족·인벤토리 부족 atomic rollback 확인
5. 8개 물약 모두 GUI 제작 및 실제 PDC 확인
6. 각 물약 섭취 후 EffectService 활성 상태 확인
7. 관리자 effect debug apply/remove/clear 및 일반 권한 차단 확인
8. 공격 피해, 회복, 이동 속도, 주기 피해, 최대 체력 변화 확인
9. 재적용·만료·사망·로그아웃·월드 이동·reload·shutdown cleanup 확인
10. 외부 `items.yml`, `crafting.yml`, `alchemy/*.yml`의 실제 병합 결과 확인

## 최종 판정

`COMPLETION_A_PASS`

자동 검증과 소스 구조 기준의 A 단계 구현은 완료했다. 실서버 적용·전투 수치 검증과 최종 밸런스 확정은 아직 남아 있으며, Catalyst runtime과 U11 플레이테스트는 시작하지 않았다.
