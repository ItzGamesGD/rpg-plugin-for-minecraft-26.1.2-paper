# 양조 Pre-U11 Scaffold -> Java 이식 완전성 감사

기준: 최신 `src/main`, `src/test`, 기본 리소스 및 기존 19-23 보고서
범위: U11 Prompt 13 이전의 코드·구조 감사. 최종 밸런스는 확정하지 않음.

## 최종 판정

`SCAFFOLD_MIGRATION_COMPLETE_READY_FOR_U11`

현재 production 8개 물약·효과와 A/B 촉매는 Registry 존재만이 아니라 Java runtime caller와 실행 sink까지 연결되어 있다. Paper 서버에서만 확인 가능한 체감·실제 entity mutation은 `IMPLEMENTED_BUT_LIVE_UNVERIFIED`로 분리했다. U11/U12는 시작하지 않았다.

## 1. 조사 범위

### 실제 소스·리소스

- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/ProductionEffectListener.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/AbstractProductionEffectHandler.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/*EffectHandler.java` 8종
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/potion/PotionFactory.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/potion/PaperPotionUseListener.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/potion/PaperPotionUseService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/CatalystApplicationService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/BoundedSpecialCatalystExecutionService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/gui/AlchemyGuiControllerService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/gui/AlchemyCatalystGuiService.java`
- `src/main/java/com/hyunseo/hyunseorpg/crafting/CraftingRecipeRegistry.java`
- `src/main/java/com/hyunseo/hyunseorpg/crafting/CraftingGuiService.java`
- `src/main/java/com/hyunseo/hyunseorpg/crafting/CraftingTransactionService.java`
- `src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigMigrationService.java`
- `src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigDoctor.java`
- `src/main/resources/alchemy/{effects,potions,catalysts,recipes,components}.yml`
- `src/main/resources/crafting.yml`, `src/main/resources/items.yml`

### 기존 기록

- `builds/alchemy/19-u11-production-completion-migration-gap-audit-ko.md`
- `builds/alchemy/20-production-completion-a-report-ko.md`
- `builds/alchemy/21-production-completion-b-report-ko.md`
- `builds/alchemy/22-potion-give-canonical-hotfix-report-ko.md`
- `builds/alchemy/23-pre-u11-runtime-correctness-fix-report-ko.md`
- 첨부된 U11 Migration Gap Audit 원문

원본 scaffold ZIP 전체는 현재 작업공간에 없었다. 따라서 원문 전체를 읽었다고 가장하지 않고, 확보된 U11 audit/19-23 기록과 실제 source/resource를 교차 근거로 사용했다.

## 2. Scaffold Contract Matrix

| 계약 | 실제 실행 경로 | 최종 sink | 상태 |
|---|---|---|---|
| 8개 effect 로드 | `CustomEffectRegistry.load()` -> plugin bootstrap | enabled effect snapshot | `IMPLEMENTED_RUNTIME` |
| handler 등록 | `registerProductionEffectHandlers()` -> `HandlerRegistry` | concrete handler | `IMPLEMENTED_RUNTIME` |
| active effect 저장 | `EffectService.apply/remove/tick` -> `ActiveEffectStore` | UUID + normalized effect ID | `IMPLEMENTED_RUNTIME` |
| effect 적용 | debug/potion/special -> `EffectService.apply()` | handler `onApply/onTick/onDamage` | `IMPLEMENTED_BUT_LIVE_UNVERIFIED` |
| 피해 보정 | `ProductionEffectListener` -> `EffectService.modifyDamage()` | `EntityDamageEvent#setDamage` | `IMPLEMENTED_BUT_LIVE_UNVERIFIED` |
| potion 생성 | crafting/give -> `PotionFactory` | item_id + potion_id + data_version PDC | `IMPLEMENTED_RUNTIME` |
| potion 소비 | `PaperPotionUseListener` -> `PaperPotionUseService` | `EffectService` 또는 special job | `IMPLEMENTED_BUT_LIVE_UNVERIFIED` |
| canonical recipe | `crafting.yml` -> `CraftingRecipeRegistry` -> GUI -> transaction | PotionFactory output | `IMPLEMENTED_BUT_LIVE_UNVERIFIED` |
| 부식의 정수 | `crafting.yml` -> generic crafting transaction | canonical `corrosion_essence` item | `IMPLEMENTED_BUT_LIVE_UNVERIFIED` |
| vanilla catalyst | registry -> `CatalystApplicationService` -> catalyst GUI | transformed potion PDC | `IMPLEMENTED_BUT_LIVE_UNVERIFIED` |
| special catalyst | registry -> bounded execution service | delayed job -> EffectService | `IMPLEMENTED_BUT_LIVE_UNVERIFIED` |
| lifecycle | enable/reload/quit/death/world/chunk/shutdown | task/modifier/session cleanup | `IMPLEMENTED_BUT_LIVE_UNVERIFIED` |

현재 production 범위에서 `MISSING_IMPLEMENTATION`, `CONTRACT_ONLY`, `NOT_WIRED`, `NO_OP_IMPLEMENTATION`은 0건이다.

## 3. Forward Trace

### 물약 제작

`AlchemyGuiControllerService.openInventory()`
-> `CraftingGuiService.openAlchemyPotions()`
-> `CraftingRecipeRegistry.getAllByFarmingType("alchemy_potion")`
-> `CraftingRecipeRenderer`
-> `CraftingTransactionService.craft()`
-> plugin의 dynamic output resolver
-> `PotionFactory.create()`
-> canonical ItemStack/PDC 지급

### 물약 사용

`PlayerItemConsumeEvent`
-> `PaperPotionUseListener.onConsume()`
-> `PaperPotionUseService.use()`
-> PDC/version/item validation
-> `PotionRegistry.find()`
-> effect ID
-> `EffectService.apply()`
-> `ActiveEffectStore`
-> `HandlerRegistry`
-> concrete handler
-> Bukkit 피해·회복·속성·tick sink

PDC를 가진 포션의 검증 또는 dispatch가 실패하면 consume event를 취소한다. 따라서 invalid/disabled 포션이 효과 없이 소모되는 fallback은 없다.

### 촉매

`AlchemyCatalystGuiService.apply()`
-> `CatalystApplicationService.transform()`
-> canonical potion/catalyst/allowlist/duplicate validation
-> transformed PDC
-> input/catalyst consume
-> `InventoryDeliveryService.giveExactly()`
-> transformed potion consume
-> `EffectService` 또는 `BoundedSpecialCatalystExecutionService`

## 4. Reverse Trace: production effect 8종

| effect ID | YAML handler-id | concrete handler | executable sink |
|---|---|---|---|
| `effect_vampire` | `vampirism` | `VampirismEffectHandler` | 최종 피해 통지 -> 체력 회복 |
| `effect_berserk` | `berserk` | `BerserkEffectHandler` | outgoing/incoming 피해 배율 |
| `effect_corrosion` | `corrosion` | `CorrosionEffectHandler` | 받는 피해 배율 |
| `effect_frostbite` | `frostbite` | `FrostbiteEffectHandler` | MOVEMENT_SPEED modifier |
| `effect_shock` | `shock` | `ShockEffectHandler` | TickManager -> 주기 피해 |
| `effect_bleed` | `bleed` | `BleedEffectHandler` | TickManager -> 주기 피해 |
| `effect_vulnerability` | `vulnerability` | `VulnerabilityEffectHandler` | 받는 피해 배율 |
| `effect_necrosis` | `necrosis` | `NecrosisEffectHandler` | MAX_HEALTH modifier + health clamp |

8개 handler는 `HyunseoRPGPlugin.registerProductionEffectHandlers()`에서 등록된다. YAML handler ID와 Java handler ID를 별도 namespace로 비교했으며 모두 일치한다.

## 5. migration residue와 기존 보고서 정정

### `alchemy/recipes.yml` vs `crafting.yml`

`YamlAlchemyRecipeRegistry`는 `alchemy/recipes.yml`을 Doctor/reload 검증용으로 읽지만 실제 플레이어 제작 caller는 `CraftingRecipeRegistry`가 `crafting.yml`을 읽는다. 따라서 전자는 실제 제작 sink가 아니다.

상태: `PARTIALLY_IMPLEMENTED`인 레거시 검증 호환 경로. 현재 canonical production recipe의 누락으로 세지 않는다. 두 파일을 임의로 합치지 않았고, 실제 GUI가 사용하는 `crafting.yml`을 canonical 제작 source로 기록했다. 후속 정리 후보는 중복 source-of-truth 제거 검토다.

### `potion-effect` 컴포넌트

`components.yml`에는 계약이 있지만 현재 `EffectComponentRegistry`와 `EffectService.applyComponents()` 실행기는 `attribute`만 지원한다. production 8종은 concrete handler가 피해·tick·attribute를 담당하므로 potion-effect component를 요구하지 않는다.

상태: `VALID_FUTURE / UNUSED_COMPONENT_CONTRACT`. 현재 production gap으로 억지 구현하지 않았다.

### `CombatEffectAdapter` 수치 메서드

`modifyDamage/modifyHealing`은 identity/no-op지만 현재 production 전투 path는 `ProductionEffectListener`와 `EffectService`가 담당한다. `PaperAlchemyCombatAdapter`는 skill/enchantment/movement/attack gate 경계다.

상태: `INTENTIONAL_NO_OP / FUTURE_BRIDGE`. 현재 8종 runtime이 의존하지 않으므로 production no-op이 아니다.

### `/rpg give` canonical path

`RPGGiveCommand`는 potion registry를 먼저 조회하고 potion ID에 generic `RPGItemService.create()` fallback을 허용하지 않으며 `PotionFactory`를 호출한다. 별도 recovery는 필요하지 않았다.

## 6. 전역 충돌 및 수명주기

- potion consume listener는 HIGHEST/ignoreCancelled로 등록된다.
- vanilla bypass listener는 brewing stand/hopper만 차단하며 consume을 취소하지 않는다.
- custom item vanilla blocker는 block/ender pearl 경로만 차단하며 POTION consume을 덮어쓰지 않는다.
- 피해 보정은 HIGHEST, 최종 피해 통지는 MONITOR로 분리된다.
- `EffectService.start()`는 enable에서 호출되고 shutdown에서 TickManager를 중단한다.
- effect는 death/logout/world change/reload/shutdown에서 정책에 따라 정리된다.
- special job은 quit/death/world/chunk/world unload와 shutdown에서 취소된다.
- `ActiveEffectStore` 키는 target UUID 단독이 아니므로 unrelated effect를 덮어쓰지 않는다.

## 7. 테스트 품질 및 결과

이번 실행:

- `./gradlew.bat clean test`: 성공
- `152 tests / 0 failures / 0 skipped`
- 신규: `AlchemyScaffoldMigrationCompletenessTest`

테스트 유형:

- STATIC_CONTRACT: YAML ID, handler-id, recipe/output, class 존재
- LOGIC_UNIT: ActiveEffectStore, registry/policy/bounds
- RUNTIME_BEHAVIOR: 실제 Paper entity/inventory mutation은 아직 없음

정적 계약 테스트를 runtime 증거로 계산하지 않았다.

## 8. LIVE_SERVER_UNVERIFIED

- 8개 포션 소비 후 실제 효과 체감 및 피해·회복·속도·주기 피해
- vanilla/custom/projectile/skill 피해 합성
- 장비 modifier와 frostbite/necrosis 충돌 여부
- redstone/glowstone/gunpowder/dragon breath/inversion 실제 변환
- sculk/echo/slime/wind charge 실제 entity·world·chunk 동작
- GUI shift/number/offhand/drag/close/logout/shutdown

## 9. LIVE_EXTERNAL_CONFIG_UNVERIFIED

실제 서버 폴더와 운영 JAR은 이번 workspace에서 직접 읽지 못했다.

- 외부 `alchemy/effects.yml`, `potions.yml`, `catalysts.yml`
- 외부 `crafting.yml`의 8개 potion 및 `corrosion_essence`
- 외부 `items.yml` canonical item ID
- `/rpg doctor reload`
- `/rpg migrate alchemy --dry-run`, `--apply`

위 항목은 source implementation gap과 별도이며, 배포 전 실서버에서 확인해야 한다.

## 10. 변경 파일

- `src/test/java/com/hyunseo/hyunseorpg/alchemy/AlchemyScaffoldMigrationCompletenessTest.java`
- `builds/alchemy/24-pre-u11-scaffold-migration-completeness-audit-ko.md`

이번 감사에서는 Java runtime/YML production 변경을 추가하지 않았다. 기존 canonical potion give, multi-effect store, fail-closed consume 경로를 확인했고 중복 구현하지 않았다.

## 11. 종료 조건

현재 production 범위의 MISSING_IMPLEMENTATION/CONTRACT_ONLY/NOT_WIRED/NO_OP_IMPLEMENTATION은 0건으로 판정했다. BALANCE_PENDING은 유지되며, 미래 콘텐츠는 활성화하지 않았다.

최종: `SCAFFOLD_MIGRATION_COMPLETE_READY_FOR_U11`

U11 Prompt 13 및 U12는 자동 시작하지 않았다.
