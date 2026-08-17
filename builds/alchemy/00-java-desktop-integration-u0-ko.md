# HyunseoRPG 양조 Java/Desktop Integration U0 매핑 보고서

기준일: 2026-08-15  
단계: U0 Source reconnaissance and mapping only  
상태: 조사·매핑 완료, 구현 중단  
기준 소스: 현재 작업공간의 최신 HyunseoRPG 소스  
기준 명세: `C:/Users/User/Desktop/hyunseorpg_alchemy_md_scaffold_v18_java_integration_plan.zip` 및 `C:/Users/User/Desktop/HyunseoRPG_양조_개발_최종_확정안.txt`

## 1. U0 결과 요약

- Java 소스 변경: 없음
- YML 변경: 없음
- 기존 플레이어 데이터 변경: 없음
- ZIP의 Markdown을 Java/YML로 기계 변환하지 않음
- 생성한 문서: 본 보고서 1개
- 현재 구현 확인: 독립 효과 엔진의 기본 계약과 `effect_test_speed` 테스트 정의는 이미 소스에 존재함
- 현재 미구현 확인: 물약 PDC/Registry, 양조 레시피, 촉매 실행, Paper 전투 효과 어댑터, 양조 GUI, 운영 명령
- U1 진입: 조건부 가능. U0에서 확인된 실제 클래스/API를 기준으로 기존 `com.hyunseo.hyunseorpg.alchemy` 효과 엔진을 확장해야 하며, ZIP의 패키지 구조를 그대로 복사하면 안 됨

## 2. 조사한 파일

### 2-1. 확정안·Integration Plan

- `C:/Users/User/Desktop/HyunseoRPG_양조_개발_최종_확정안.txt`
- ZIP `README.md`
- `docs/java-desktop-integration-plan.md`
- `docs/implementation-order.md`
- `docs/implementation-order-legacy.md`
- `docs/implementation-status.md`
- `docs/core-effect-implementation-status.md`
- `docs/combat-effect-implementation-status.md`
- `docs/effect-policy-status.md`
- `docs/effect-test-matrix.md`
- `docs/abundance-bridge-status.md`
- `docs/potion-registry-status.md`
- `docs/first-production-potions-status.md`
- `docs/vanilla-catalysts-status.md`
- `docs/special-catalysts-status.md`
- `docs/gui-crafting-status.md`
- `docs/gui-transaction-hardening.md`
- `docs/hardening-status.md`
- `docs/13-playtest-template.md`
- `docs/13-final-integration-report.md`
- `docs/future-bridge-status.md`
- `docs/desktop-integration-checklist.md`
- `docs/code-text-index.md`

ZIP의 `code-text/java` 및 `code-text/resources` 전체 경로도 목록화하고, 계약명과 실제 소스의 존재 여부를 대조했다. 해당 파일은 구현 대상으로 복사하지 않았다.

### 2-2. 실제 Java 소스

- 부트스트랩·수명주기: `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
- 설정·운영: `core/config/ConfigService.java`, `ConfigDoctor.java`, `ConfigMigrationService.java`, `RPGReloadService.java`
- 현재 효과 엔진: `alchemy/EffectService.java`, `CustomEffectRegistry.java`, `CustomEffectDefinition.java`, `ActiveEffectInstance.java`, `EffectContext.java`, `EffectSource.java`, `EffectSourceType.java`, `EffectTargetPolicy.java`, `EffectStackPolicy.java`, `EffectConflictResolver.java`, `EffectComponentDefinition.java`, `EffectComponentRegistry.java`, `HandlerRegistry.java`, `TickManager.java`
- 아이템·PDC: `item/RPGItemRegistry.java`, `RPGItemService.java`, `RPGItemData.java`
- 플레이어 저장: `player/PlayerRPGData.java`, `PlayerDataService.java`, `YamlPlayerDataRepository.java`, `PlayerDataCache.java`
- 농사·풍요: `farming/FarmingItemBridge.java`, `FarmingEssenceService.java`, `AbundancePointService.java`, `HarvestService.java`, `DeliveryService.java`, `DeliveryDataService.java`, `CropQualityService.java`
- 제작·GUI: `crafting/CraftingRecipeRegistry.java`, `CraftingRecipeData.java`, `CraftingTransactionService.java`, `CraftingGuiService.java`, `CraftingRecipeRenderer.java`, `CraftingLayoutRegistry.java`, `CraftingMenuHolder.java`
- 전투·스킬·인챈트: `combat/CombatService.java`, `combat/DamageContext.java`, `skill/SkillService.java`, `skill/SkillInputListener.java`, `skill/SkillRegistry.java`, `enchant/EnchantRegistry.java`, `EnchantService.java`, `EquipmentEnchantContentService.java`
- 농사 GUI·이벤트: `farming/DeliveryGuiService.java`, `FarmingHubGuiService.java`, `CookingGuiService.java`, `FarmingStatTokenListener.java`

### 2-3. 실제 기본 설정

- `src/main/resources/alchemy/effects.yml`
- `src/main/resources/alchemy/components.yml`
- `src/main/resources/alchemy/conflicts.yml`
- `src/main/resources/alchemy/scaling.yml`
- `src/main/resources/items.yml`
- `src/main/resources/farming/crops.yml`, `essence.yml`, `harvest.yml`, `deliveries.yml`, `processing.yml`, `quality.yml`, `progression.yml`
- `src/main/resources/crafting.yml`
- `src/main/resources/enchants.yml`

## 3. 명세 경계 → 실제 소스 매핑

| 명세 계약 | 실제 연결 대상 | 판정 |
|---|---|---|
| `ConfigBridge` | `ConfigService`의 alchemy 효과/component/conflict/scaling getter 및 reload | `RESOLVED_MAPPING` |
| `ItemBridge` | `RPGItemRegistry` + `RPGItemService.create/getItemId/getData/hasTag` | `RESOLVED_MAPPING` |
| `GuiTransactionBridge` | `CraftingTransactionService`, `InventoryDeliveryService`, `CraftingGuiService` | `RESOLVED_MAPPING`; 전용 양조 GUI 트랜잭션은 후속 |
| `FarmingBridge` | `FarmingItemBridge`, `AbundancePointService`, `FarmingEssenceService`, `DeliveryDataService` | `RESOLVED_MAPPING` |
| `CombatBridge` | `CombatService`와 `DamageContext` | `RESOLVED_MAPPING` for damage entry only; 효과별 정책은 U3 |
| `CoreEffectEventAdapter` | 현재 직접 대응 클래스 없음. `EquipmentEffectTriggerListener`, `SkillInputListener`, 전투 리스너가 분산 경계 | `IMPLEMENT_IN_UNIT_3` |
| `CombatEffectAdapter` | 현재 직접 대응 클래스 없음. `CombatService`가 호출 후보 | `IMPLEMENT_IN_UNIT_3` |
| `Player-data/persistence` | `PlayerRPGData.farming.*`, `PlayerDataService`, `YamlPlayerDataRepository` | `RESOLVED_MAPPING` for farming; alchemy section은 `IMPLEMENT_IN_UNIT_2` |
| `AlchemyModule` | 현재 단일 module 클래스 없음. `HyunseoRPGPlugin`이 각 서비스 직접 초기화 | `IMPLEMENT_IN_UNIT_2` |
| `PotionRegistry/PotionFactory` | 현재 없음 | `IMPLEMENT_IN_UNIT_5` |
| `AlchemyRecipeRegistry` | 현재 없음. 기존 제작 Registry는 `CraftingRecipeRegistry` | `IMPLEMENT_IN_UNIT_6` |
| `CatalystRegistry` | 현재 없음 | `IMPLEMENT_IN_UNIT_7` |
| `SpecialCatalystRegistry` | 현재 없음 | `IMPLEMENT_IN_UNIT_8` |
| `AlchemyGuiController/Session` | 현재 없음. 기존 `CraftingGuiService`와 Holder 계열은 재사용 후보 | `IMPLEMENT_IN_UNIT_9` |
| `AlchemyDoctor/AuditLog` | `ConfigDoctor`와 plugin logger는 존재하나 양조 전용 출력·감사 계약은 없음 | `IMPLEMENT_IN_UNIT_10` |
| `EffectService` | `com.hyunseo.hyunseorpg.alchemy.EffectService`가 이미 존재 | `RESOLVED_MAPPING`, 단 현재 attribute component만 실행 |
| `CustomEffectRegistry` | 실제 동일 이름의 `alchemy.CustomEffectRegistry` 존재 | `RESOLVED_MAPPING` |
| `EffectComponentRegistry/HandlerRegistry` | 실제 동일 이름의 Registry 존재 | `RESOLVED_MAPPING`; 현재 handler 실행은 확장점 수준 |
| `EffectLifecycleService/EffectTickManager` | `EffectService` 내부 lifecycle + `TickManager` | `RESOLVED_MAPPING` with adapter; 별도 중복 서비스 금지 |
| `PotionUseService` | 현재 없음 | `IMPLEMENT_IN_UNIT_5` |
| `PotionItemValidator` | 현재 없음 | `IMPLEMENT_IN_UNIT_5` |
| `AlchemyMaterialResolver` | 현재 없음. `RPGItemService`와 `FarmingItemBridge`가 분산 담당 | `IMPLEMENT_IN_UNIT_6` |
| `AbundanceBridge` | 현재 직접 동일 이름은 없지만 `AbundancePointService`와 `FarmingItemBridge`로 계약 가능 | `RESOLVED_MAPPING`; 실제 양조 연결은 U4 |

## 4. 재사용 대상과 신규 클래스 후보

### 재사용 확정

- 설정: `ConfigService` 및 기존 reload/doctor 책임
- 아이템: `RPGItemRegistry`, `RPGItemService`, 기존 `NamespacedKey` PDC 정책
- 풍요: `AbundancePointService`, `FarmingEssenceService`, `FarmingItemBridge`
- 저장: `PlayerDataService`, `PlayerRPGData`, `YamlPlayerDataRepository`
- 제작 지급: `CraftingTransactionService`, `InventoryDeliveryService`, `PendingRewardService`
- GUI 이벤트 보호: `CraftingMenuHolder`, `DeliveryMenuHolder`, `FarmingHubMenuHolder` 패턴
- 전투: `CombatService`, `DamageContext`
- 스킬·인챈트: `SkillService`, `EnchantService`, `EquipmentEnchantContentService`
- 수명주기: `HyunseoRPGPlugin`의 `onEnable`, listener 등록, reload, shutdown 경로

### 신규 클래스가 실제로 필요한 후보

아래는 U0에서 존재를 추측해 만든 것이 아니라, 후속 Unit에서 계약상 필요한 신규 경계다.

- U2: `AlchemyModule` 또는 현재 plugin bootstrap에 맞는 동등한 lifecycle coordinator, Paper component adapter, alchemy player-data migration adapter
- U3: `CoreEffectEventAdapter`, `CombatEffectAdapter`, 실제 effect handler 구현체
- U4: `AbundanceBridge` 동등 adapter, essence exchange service
- U5: `PotionRegistry`, `PotionDefinition`, `PotionFactory`, `PotionPdcContract`, `PotionItemValidator`, `PotionUseService`, `PotionEffectDispatcher`
- U6: `AlchemyRecipeRegistry`, `AlchemyRecipeDefinition`, `AlchemyMaterialResolver`, `AlchemyCraftService`
- U7: `CatalystRegistry` 및 vanilla catalyst policy/application 경계
- U8: `SpecialCatalystRegistry`, bounded job/propagation service
- U9: `AlchemyGuiController`, session/transaction coordinator 또는 기존 crafting GUI에 맞춘 동등 경계
- U10: 양조 doctor/audit/command 경계. 기존 `ConfigDoctor`, command dispatcher, logger와 통합

실제 클래스명은 각 Unit 시작 시 다시 확인하고, 현재 구조와 맞지 않으면 동등한 기존 서비스 확장으로 대체한다.

## 5. PDC·ItemRegistry 매핑

### 확인된 실제 PDC

- `item_id`: `RPGItemService`가 `NamespacedKey(plugin, "item_id")`로 생성·검증
- `farming_crop_id`: 작물 family
- `farming_quality`: 품질
- `farming_crop_data_version`: 농사 아이템 버전

### 명세 PDC

- `potion_id`: 현재 없음. `IMPLEMENT_IN_UNIT_5`
- `data_version`: 현재 물약 전용 없음. `IMPLEMENT_IN_UNIT_5`
- `catalyst_id`: 명세상 선택/검증 키이며 현재 없음. `IMPLEMENT_IN_UNIT_5`에서 최종 포맷 확정

PDC 없는 이름·Lore 복사품을 허용하지 않는다는 명세는 현재 `RPGItemService.getItemId()`가 item_id PDC를 요구하는 구조와 정합성이 있다. 다만 물약 전용 validator는 아직 없다.

### symbolic item ID 상태

| ID 또는 계열 | 실제 확인 | 상태 |
|---|---|---|
| `abundance_essence` | `items.yml`, `farming/essence.yml`, point-only crafting 경로 확인 | `RESOLVED_MAPPING`; 양조 활성은 U4/U5 이후 |
| `crop_corn`, `crop_onion`, `crop_chili`, `crop_garlic` | `items.yml`, `farming/crops.yml` 확인 | `RESOLVED_MAPPING` |
| `processed_corn_starch_*` | `items.yml`에 normal/basic/proficient/advanced/supreme 확인 | `RESOLVED_MAPPING` |
| `processed_onion_concentrate_*` | `items.yml`에 5품질 확인 | `RESOLVED_MAPPING` |
| `processed_chili_extract_*` | `items.yml`에 5품질 확인 | `RESOLVED_MAPPING` |
| `processed_garlic_concentrate_*` | `items.yml`에 5품질 확인 | `RESOLVED_MAPPING` |
| `tomato_concentrate` | 현재 items/farming/alchemy/crafting에서 미확인 | `BLOCKED`; 관련 recipe disabled |
| `chili_powder` | 현재 Registry ID 미확인. `processed_chili_extract_*`와 동일시 금지 | `BLOCKED` |
| `corrosion_essence` | 현재 미확인 | `BLOCKED`; exchange/recipe disabled |
| `common_cold_loot` | 현재 미확인 | `BLOCKED`; future/loot bridge 필요 |
| `spider_eye`, `blaze_powder`, `sugar`, `slime_ball`, `rotten_flesh`, `snowball`, `copper_ingot`, `redstone`, `cactus`, `wither_rose` | 현재 custom `RPGItemRegistry` ID로는 미확인. Bukkit Material로 조용히 대체할 resolver도 없음 | `BLOCKED` for alchemy activation; Unit 6에서 명시적 vanilla/custom resolver 검증 |
| `vampire`, `berserk`, `corrosion`, `frostbite`, `shock`, `bleed`, `vulnerability`, `necrosis` | 현재 potion item ID 미확인 | `IMPLEMENT_IN_UNIT_5`, production disabled |
| `effect_test_speed` | alchemy effect config 및 현재 테스트 정의 확인 | `RESOLVED_MAPPING`; test-only |
| `goddess_tear`, `demon_tear` | 실제 아이템·레시피·드롭 없음 | `FUTURE_DISABLED` |

`processed_corn` 같은 축약 ID와 실제 canonical `processed_corn_starch_*`는 동일시하지 않는다. 문서의 symbolic ID가 실제 Registry ID와 다르면 해당 레시피는 비활성 상태를 유지한다.

## 6. Player-data·persistence

- 기존 `PlayerRPGData`에는 농사 단계, 해금, 수확량, 풍요 포인트, 납품 상태가 이미 존재한다.
- `YamlPlayerDataRepository`는 `farming.*`를 저장·로드하며 기존 데이터 보존 방식이 있다.
- `AbundancePointService`는 `addPoints`, `spend`, `spendForTransaction`으로 저장 성공·롤백을 처리한다.
- 따라서 양조가 별도 플레이어 YAML을 만들면 안 된다.
- 명세의 `alchemy` canonical section은 현재 PlayerRPGData에 없으므로 U2에서 필요한 최소 필드만 기존 repository에 추가해야 한다.
- 효과 인스턴스는 현재 메모리 기반이며 `persistOnLogout` 등의 정책 필드는 있으나 재시작 영속화는 구현되지 않았다. 기본 비영속 정책으로 U2에서 명시한다.
- 임시 파일·atomic move 요구는 현재 저장소 구현과 직접 대조 후 U2에서 적용 여부를 결정한다. U0에서는 구현하지 않았다.

## 7. Combat·Skill·Enchantment 연결

- 직접 피해 진입점은 `CombatService.applyDirectDamage`, `applySkillDamage`, `applyEnchantDamage` 등으로 확인됐다.
- `DamageContext`가 source/유형 추적의 현재 기반이지만, DoT·반사·연쇄·보스/PvP/파티 정책을 양조 효과가 사용할 수 있는 공통 adapter는 없다.
- 스킬은 `SkillService.handleInput` 및 `executeEnchantment`, 인챈트는 `EnchantService`와 `EquipmentEnchantContentService`가 실제 실행 경로다.
- 따라서 `silence/root/stun`의 입력 차단과 `vulnerability/vampirism/berserk`의 피해 보정은 U3에서 실제 이벤트 우선순위와 함께 매핑한다.
- 현재 `EffectService`는 `attribute` component만 실제 Bukkit AttributeModifier로 적용하고 `POTION_EFFECT`, `DAMAGE`, `HEAL`은 지원하지 않는다. 해당 명세 항목은 `IMPLEMENT_IN_UNIT_2` 또는 `IMPLEMENT_IN_UNIT_3`로 남긴다.
- 전투 효과를 현재 `EffectService` 안에 임의로 직접 삽입하지 않는다.

## 8. Farming·Abundance 연결

- 직접 수확과 납품은 `AbundancePointService.Source.HARVEST/DELIVERY`를 사용할 수 있는 기반이 있다.
- `AbundancePointService`는 등록 활동 ID를 통해 임의 이벤트 지급을 막는 구조를 제공한다.
- `FarmingItemBridge.isAbundanceEssenceUsableFor()`는 `farming/essence.yml`의 `usage-tags`를 `items.yml`의 runtime tag와 분리해 판정한다. 양조는 이 정책 API를 사용해야 한다.
- 풍요 정수 자체는 `FarmingEssenceService`와 point-only `CraftingTransactionService` 경로가 확인됐다.
- 실제 `AbundanceActivityEvent` 발행·중복 방지·양조 교환은 아직 별도 bridge가 없으므로 U4에 배정한다.
- 포인트 교환량·효과 수치·재료 수량은 `BALANCE_PENDING`이며 U11 전에는 확정하지 않는다.

## 9. GUI·InventoryEvent

- 기존 제작은 `CraftingGuiService`가 `CraftingMenuHolder`로 식별하고 `CraftingTransactionService`를 호출한다.
- 배달은 `DeliveryGuiService`가 `DeliveryMenuHolder`로 식별하고 클릭/드래그/닫기/로그아웃 반환을 처리한다.
- 농사 Hub와 기존 farming GUI listener도 등록되어 있다.
- 이 구조는 `InventoryHolder` 기반 식별을 재사용할 수 있는 근거다.
- 그러나 양조 전용 session, 입력 소유권, 원자적 output delivery, hopper/collect-to-cursor/동시 session 방어는 현재 양조 코드에 없다. U9에서 구현·실서버 검증한다.
- 양조 GUI가 기존 제작 로직을 복제하거나 별도 결과 지급을 구현하면 안 된다. 가능하면 기존 `CraftingTransactionService`를 adapter로 호출한다.

## 10. Scheduler·job·lifecycle

- 현재 효과 주기 실행은 `alchemy/TickManager`가 Bukkit scheduler를 사용한다.
- plugin enable 순서는 `ConfigService` → `EffectService` load → player/item/farming/crafting 초기화 → listener 등록으로 확인됐다.
- `EffectService`는 death/quit/world-change에 cleanup listener를 자체 구현하고 shutdown에서 active effect를 정리한다.
- 서버 reload, kick, chunk/world unload, GUI close, delayed projectile/area/special catalyst job의 공통 추적은 아직 없다.
- Unit 2에서 효과 lifecycle을 정리하고, U8에서 bounded job scope와 cleanup을 구현하며, U10에서 reload/clearjobs 운영 경계를 추가한다.
- 월드·엔티티·인벤토리 조작은 main thread에 두고, 계산만 비동기화한다.

## 11. Paper/Bukkit API 매핑

| 요구 | 현재 근거 | 상태 |
|---|---|---|
| Attribute modifier | `EffectService`, `EquipmentEnchantContentService` | `RESOLVED_MAPPING` |
| PotionEffect | 현재 양조 전용 adapter 없음 | `IMPLEMENT_IN_UNIT_2` |
| Entity damage/heal | `EntityDamageEvent` 계열 listener와 `CombatService` 존재 | `IMPLEMENT_IN_UNIT_3` |
| Player quit/death/world change | 기존 listener 및 `EffectService` 일부 존재 | `RESOLVED_MAPPING` for cleanup baseline |
| Inventory click/drag/close | 기존 GUI listeners 존재 | `RESOLVED_MAPPING` baseline, U9 hardening |
| Brewing/hopper blocking | 현재 양조 전용 listener 미확인 | `IMPLEMENT_IN_UNIT_7`/`U10` |
| Projectile/area/chain jobs | 일부 스킬 helper는 존재하나 양조 공통 job 없음 | `IMPLEMENT_IN_UNIT_8` |

## 12. DEFERRED/PENDING 분류표

| 원문 보류 항목 | 분류 | 다음 근거 |
|---|---|---|
| `ATTRIBUTE` wrapper의 완전한 Paper 복구 | `IMPLEMENT_IN_UNIT_2` | 현재 Attribute 적용은 있으나 lifecycle 복구 검증 부족 |
| `POTION_EFFECT` wrapper | `IMPLEMENT_IN_UNIT_2` | Bukkit mapping 신규 필요 |
| `DAMAGE`, `HEAL` wrapper | `IMPLEMENT_IN_UNIT_3` | CombatService/DamageContext 연결 필요 |
| 구체 Paper event listener | `IMPLEMENT_IN_UNIT_2` 또는 `U3` | 이벤트 종류별 lifecycle/combat 의존성 분리 |
| combat effect handlers | `IMPLEMENT_IN_UNIT_3` | 실제 피해·사망·loot 정책 필요 |
| conflict policy | `IMPLEMENT_IN_UNIT_3` | PvP/보스/파티 우선순위 근거 필요 |
| farming event → abundance activity | `IMPLEMENT_IN_UNIT_4` | 현재 farming API 확인 완료 |
| abundance point persistence/migration | `IMPLEMENT_IN_UNIT_4` | 기존 PlayerRPGData/repository 확인 완료 |
| essence exchange | `IMPLEMENT_IN_UNIT_4` | 기존 `spendForTransaction` 재사용 |
| `abundance_essence -> corrosion_essence` 수량 | `BALANCE_PENDING` | U11 전 수치 확정 금지 |
| eight production potion items | `IMPLEMENT_IN_UNIT_5` | potion PDC/validator 먼저 필요 |
| potion PDC adapter | `IMPLEMENT_IN_UNIT_5` | 실제 namespace는 기존 plugin key를 재사용 |
| production effect ID 연결 | `IMPLEMENT_IN_UNIT_5` | U3 handler 완료 전 disabled |
| production ingredient mapping | `IMPLEMENT_IN_UNIT_6` | unresolved ID는 recipe별 disabled |
| `tomato_concentrate`, `chili_powder`, `corrosion_essence`, `common_cold_loot` | `BLOCKED` | 현재 실제 Registry ID 없음 |
| vanilla ingredient 여부 | `IMPLEMENT_IN_UNIT_6` | custom ID와 Bukkit Material을 명시적으로 구분할 resolver 필요 |
| production recipe activation | `IMPLEMENT_IN_UNIT_6` | item/effect/output/transaction gate 필요 |
| vanilla catalyst transformations | `IMPLEMENT_IN_UNIT_7` | potion identity·delivery adapter 필요 |
| special catalyst handlers | `IMPLEMENT_IN_UNIT_8` | scheduler/entity/projectile API 검증 필요 |
| GUI concrete listeners | `IMPLEMENT_IN_UNIT_9` | current holder baseline exists, alchemy transaction absent |
| atomic GUI transaction | `IMPLEMENT_IN_UNIT_9` | input/output/rollback adversarial tests 필요 |
| `/rpg doctor alchemy`, commands, audit | `IMPLEMENT_IN_UNIT_10` | ConfigDoctor/command/logger 통합 필요 |
| vanilla brewing/hopper bypass | `IMPLEMENT_IN_UNIT_10` | 실제 listener와 runtime test 필요 |
| TPS/memory/performance | `IMPLEMENT_IN_UNIT_11` | 실제 서버 부하 측정 필요 |
| duration/strength/stack/PvP/Boss 수치 | `BALANCE_PENDING` | U11 플레이테스트 전 확정 금지 |
| catalyst 수치·전파·튕김·지연 | `BALANCE_PENDING` | U11 측정 대상 |
| `goddess_tear`, `demon_tear` recipes/drops/usage | `FUTURE_DISABLED` | 별도 미래 콘텐츠 프로젝트 |
| future elemental equipment/boss/loot | `FUTURE_DISABLED` | 현재 양조 범위 밖 |
| old/unknown/fake potion rejection | `IMPLEMENT_IN_UNIT_5` | validator 계약 |
| GUI security matrix | `IMPLEMENT_IN_UNIT_9` | shift/number/drag/collect/hopper 등 실이벤트 필요 |
| restart effect persistence | `IMPLEMENT_IN_UNIT_2` | 기본은 비영속, 명시 정책만 복구 |
| full Prompt 13 integrated loops | `IMPLEMENT_IN_UNIT_11` | 실제 서버 증거 필요 |

이 표에 기록된 모든 `DEFERRED`, `PENDING`, `BLOCKED`, `reason_disabled` 항목은 위 다섯 분류 중 하나에 포함했다. 현재 U0에서 활성화로 승격한 항목은 없다.

## 13. 현재 소스와 MD의 충돌

1. MD의 `com.hyunseo.hyunseorpg.effect.*` 계약과 실제 소스의 `com.hyunseo.hyunseorpg.alchemy.*` 패키지가 다르다. 실제 소스의 alchemy 효과 엔진을 기준으로 통합하고 패키지 전체 복사는 금지한다.
2. MD는 `AlchemyRecipeRegistry`, `PotionRegistry`, `CatalystRegistry`, `AlchemyGuiController` 등을 전제로 하지만 현재 소스에는 없다. 기존 제작 Registry/Transaction/Holder를 우선 재사용하고, 부족한 경계만 후속 Unit에서 추가한다.
3. MD의 production ingredient 중 다수는 현재 `RPGItemRegistry`에서 확인되지 않는다. Bukkit Material 이름으로 임의 대체하지 않는다.
4. MD의 `effects/` resource 경로와 실제 운영 경로 `alchemy/`가 다르다. 현재 `ConfigService`와 기본 리소스는 `alchemy/`가 authoritative baseline이다. U1 이후 경로를 추가할 때도 기존 경로와 충돌시키지 않는다.
5. MD는 현재 환경에 Paper event mapping이 없다고 기록하지만, 실제 소스에는 일부 combat/GUI/lifecycle listener가 있다. 따라서 “전부 없음”이 아니라 “양조 공통 adapter가 없음”으로 정정한다.
6. 풍요 정수의 runtime ItemRegistry tag와 `farming/essence.yml` usage-tags는 다른 계약이다. 양조는 `FarmingItemBridge.isAbundanceEssenceUsableFor()`를 사용하고 두 태그 체계를 억지로 합치지 않는다.
7. 현재 `EffectService`가 적용하는 component는 `attribute`뿐이다. 문서의 PotionEffect/Damage/Heal/Handler가 이미 작동한다고 보고하면 안 된다.

## 14. 회수 가능해진 DEFERRED/PENDING

실제 소스 확인으로 다음은 “근거 없음”에서 “연결 대상 확인”으로 회수됐다.

- ConfigBridge → `ConfigService`
- ItemBridge → `RPGItemRegistry`/`RPGItemService`
- FarmingBridge → `FarmingItemBridge`/`AbundancePointService`/`FarmingEssenceService`
- Player persistence → `PlayerRPGData`/`YamlPlayerDataRepository`
- Farming processed item IDs → 4작물 × 5품질의 실제 ID
- Abundance essence point-only craft → `FarmingEssenceService` + `CraftingTransactionService`
- Effect core contracts → 현재 `alchemy` effect classes
- GUI transaction baseline → `CraftingGuiService`, `DeliveryGuiService`, Holder 기반 이벤트 처리
- Combat damage entry → `CombatService`/`DamageContext`

이 항목들은 `RESOLVED_MAPPING`으로 회수했지만, 실제 기능 활성화나 최종 수치 확정으로 간주하지 않는다.

## 15. 후속 Unit 의존성 및 U1 후보 파일

### U1에서 정확히 검토·수정할 후보

U1은 독립 효과 엔진 core만 대상으로 한다. 현재 파일을 재사용·확장하는 후보는 다음과 같다.

- `src/main/java/com/hyunseo/hyunseorpg/alchemy/CustomEffectRegistry.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/CustomEffectDefinition.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/ActiveEffectInstance.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectContext.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectSource.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectComponentDefinition.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectComponentRegistry.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectConflictResolver.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/HandlerRegistry.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/TickManager.java`
- `src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigService.java`
- `src/main/resources/alchemy/effects.yml`
- `src/main/resources/alchemy/components.yml`
- `src/main/resources/alchemy/conflicts.yml`
- `src/main/resources/alchemy/scaling.yml`
- `src/test/java/com/hyunseo/hyunseorpg/alchemy/AlchemyEffectEngineContractTest.java`

U1에서 금지되는 후보: `items.yml`, `farming/*.yml`, production potion/recipe files, farming player-data schema, combat listener, GUI listener, shop/economy. 이들은 후속 Unit 소유다.

### 후속 의존 순서

`U1 effect core` → `U2 Paper/lifecycle/persistence` → `U3 combat/skill/enchantment` → `U4 farming/abundance` → `U5 potion PDC/registry` → `U6 recipe/material` → `U7 vanilla catalyst` → `U8 special catalyst` → `U9 GUI transaction` → `U10 security/doctor/commands` → `U11 Prompt 13 playtest/balance` → `U12 future bridge verification`

## 16. U0 종료 판정

- 모든 발견된 deferred/pending 항목은 `RESOLVED_MAPPING`, `IMPLEMENT_IN_UNIT_X`, `BALANCE_PENDING`, `FUTURE_DISABLED`, `BLOCKED` 중 하나로 분류했다.
- 실제 Registry에 없는 양조 재료·물약·촉매·미래 아이템은 비활성 상태로 유지해야 한다.
- 현재 U1은 진입 가능하지만, U0 보고서 승인 후 별도 실행해야 한다.
- U1은 이 보고서의 후보 파일만 대상으로 하며 자동으로 시작하지 않는다.
- 본 U0 종료 시점에 Java/YML 기능 변경은 없다.
