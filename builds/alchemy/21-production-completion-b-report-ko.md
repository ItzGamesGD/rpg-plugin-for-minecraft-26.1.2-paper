# HyunseoRPG 양조 Production Completion B 보고서

기준: Production Completion A 최신 소스
범위: 바닐라 촉매 런타임, 특수 촉매 bounded 실행, 촉매 GUI 변환, 수명주기 정리
제외: U11 Prompt 13 플레이테스트, 최종 밸런스 확정

## 1. 판정

`READY_FOR_U11`

코드상 현재 Production A 경로와 B 촉매 경로를 연결했고, 자동 테스트와 빌드를 통과했다. Paper 서버에서의 실제 GUI·아이템·월드 엔티티 동작은 아래 실서버 체크리스트가 남아 있다.

## 2. Vanilla 촉매

| 촉매 | Registry | 런타임 | PDC 변환 | GUI | 상태 |
|---|---|---|---|---|---|
| redstone | `YamlCatalystRegistry` | duration 변환 메타데이터 | `catalyst_id`, delivery, duration | 적용 GUI | enabled, 수치 BALANCE_PENDING |
| glowstone_dust | 동일 | amplifier/duration 변환 | 동일 | 적용 GUI | enabled, 수치 BALANCE_PENDING |
| gunpowder | 동일 | DRINK -> SPLASH delivery | canonical potion ID 유지 | 적용 GUI/우클릭 | enabled |
| dragon_breath | 동일 | DRINK -> LINGERING delivery | canonical potion ID 유지 | 적용 GUI/우클릭 | enabled |
| fermented_spider_eye | 동일 | allowlist된 inversion만 effect override | 원본 potion ID + 명시 effect ID | 적용 GUI | enabled, 조합 BALANCE_PENDING |

아이템 매칭은 lore나 이름이 아니라 `vanilla-material`과 실제 `Material`을 비교한다. 변환 전 canonical potion PDC와 RPG item ID를 검증하며, 중복 촉매·미등록 조합·잘못된 data version은 거부한다.

## 3. Special 촉매

| 촉매 | 실행 | 제한 | 정리 |
|---|---|---|---|
| sculk | 반경 대상 확산 후 `EffectService` 적용 | 전파 수, 반경, attenuation, 방문 대상 집합 | 완료·월드/청크 unload·shutdown |
| echo_shard | 지연된 2차 실행 | delay와 단일 execution ID | 대상/소스 사망, quit, world change |
| slime | 주변 대상 연속 bounce | bounce 수, lifetime, 방문 대상 | 완료·중복 실행·shutdown |
| wind_charge | 방향 벡터 직선 탐색 | 거리, lifetime, 충돌 대상 | 충돌·timeout·월드/청크 unload |

모든 실행은 `BoundedSpecialCatalystExecutionService`가 소유하며 중복 execution ID와 전체 동시 실행 상한을 검사한다. 별도의 효과 엔진을 만들지 않고 기존 `EffectService.apply` 또는 `applyWithOverrides`를 호출한다.

## 4. 전체 Production 호출 그래프

```text
farming/사냥 아이템
 -> abundance_essence / corrosion_essence
 -> crafting.yml의 canonical recipe
 -> CraftingRecipeRegistry
 -> CraftingGuiService
 -> CraftingTransactionService
 -> PotionFactory
 -> RPG item ID + Potion PDC
 -> PaperPotionUseListener
 -> PaperPotionUseService
 -> CatalystApplicationService(선택)
 -> Potion PDC delivery/effect override 또는 SpecialCatalystExecution
 -> EffectService
 -> 8개 concrete production effect handler
 -> TickManager / lifecycle cleanup
```

촉매 GUI는 `AlchemyCatalystGuiService`와 `AlchemyCatalystMenuHolder`를 사용한다. 입력은 포션 슬롯과 촉매 슬롯으로 제한하고, 결과 수용 공간을 먼저 검사한 뒤 `CatalystApplicationService`가 변환한 canonical ItemStack을 `InventoryDeliveryService.giveExactly`로 지급한다. 실패 시 입력을 복구한다.

## 5. GUI·거래 보안

- Holder 기반으로만 GUI를 식별한다.
- 모든 클릭은 기본 취소하며 포션·촉매 입력 슬롯만 직접 처리한다.
- Shift-click, 숫자키, offhand swap, double-click, collect-to-cursor, creative click은 아이템 이동이 불가능하다.
- drag와 hopper 이동을 차단한다.
- 미커밋 입력은 close, quit, reload, shutdown 시 반환한다.
- 중복 클릭은 기존 플레이어별 transaction과 GUI 입력 상태로 같은 입력을 다시 소비하지 않는다.
- output capacity 실패 시 입력을 소비하지 않는다.

## 6. 설정·마이그레이션

기본 `alchemy/catalysts.yml`에는 현재 B 생산 범위 9종이 명시적 Material 매핑과 함께 활성화되어 있다. 기존 외부 설정은 authoritative로 유지되며, 새 항목 추가와 활성화는 명시적인 `migrate alchemy --apply` 또는 `migrate all --apply`에서만 수행된다. 일반 reload는 파일을 변경하지 않는다.

외부 설정이 이전 상태라면 다음을 실행해야 한다.

```text
/rpg migrate alchemy --dry-run
/rpg migrate alchemy --apply
/rpg reload alchemy
```

dry-run은 변경하지 않으며, apply는 기존 값을 기본값으로 덮어쓰지 않고 누락된 catalyst subtree와 B 활성화 항목만 병합한다.

## 7. BALANCE_PENDING

- duration multiplier
- amplifier delta
- inversion 조합과 효과 매핑의 최종 게임성
- 특수 촉매 반경·전파 수·attenuation·delay·bounce·거리·lifetime
- 포션별 제작 수량과 촉매 소모 수량

위 값은 런타임 검증용 baseline이며 최종 밸런스 값으로 확정하지 않았다.

## 8. FUTURE_DISABLED

`goddess_tear`, `demon_tear`, 미래 원소 장비, 미래 보스·loot 콘텐츠는 이번 B 범위에서 활성화하지 않았다.

현재 production 촉매 9종에 대해 `CONTRACT_ONLY` 또는 `MISSING_IMPLEMENTATION`으로 남겨 둔 항목은 없다.

## 9. 변경 파일

주요 Java:

- `alchemy/catalyst/CatalystDefinition.java`
- `alchemy/catalyst/CatalystApplicationService.java`
- `alchemy/catalyst/YamlCatalystRegistry.java`
- `alchemy/catalyst/SpecialCatalystDefinition.java`
- `alchemy/catalyst/YamlSpecialCatalystRegistry.java`
- `alchemy/catalyst/BoundedSpecialCatalystExecutionService.java`
- `alchemy/catalyst/SpecialCatalystExecution.java`
- `alchemy/potion/PotionPdcContract.java`
- `alchemy/potion/PaperPotionPdcContract.java`
- `alchemy/potion/PaperPotionUseService.java`
- `alchemy/potion/PaperPotionUseListener.java`
- `alchemy/EffectService.java`
- `alchemy/gui/AlchemyCatalystGuiService.java`
- `alchemy/gui/AlchemyCatalystMenuHolder.java`
- `alchemy/gui/AlchemyGuiControllerService.java`
- `item/InventoryDeliveryService.java`
- `HyunseoRPGPlugin.java`
- `core/config/ConfigMigrationService.java`

설정·테스트:

- `src/main/resources/alchemy/catalysts.yml`
- `AlchemyU7U9ContractTest.java`
- `AlchemyU10ContractTest.java`
- `AlchemyU11BalanceContractTest.java`
- `CatalystRuntimeBehaviorTest.java`

## 10. 검증 결과

- 자동 테스트: `146 tests / 0 failures / 0 skipped`
- `./gradlew.bat test`: 성공
- `./gradlew.bat jar`: 성공
- 빌드 산출물: `builds/alchemy/HyunseoRPG-0.1.0-SNAPSHOT-alchemy-production-completion-b.jar`
- SHA-256: `9F7D80D9B3ED02445DE0BF68E9509E43C95D3C0349EFFA74A369FE800591E011`

## 11. 실서버 확인 목록

1. 외부 catalysts.yml에 대해 dry-run/apply/reload 순서 확인
2. 5개 vanilla 촉매를 실제 포션에 각각 적용하고 입력 1개씩만 차감되는지 확인
3. 변환 포션의 PDC와 아이템 ID가 유지되는지 확인
4. 중복 촉매·가짜 포션·잘못된 PDC가 거부되는지 확인
5. 변환 포션 우클릭 시 원래 포션과 변환 delivery별 효과 확인
6. sculk 전파가 다른 월드로 넘어가지 않고 제한 수를 넘지 않는지 확인
7. echo 2차 실행, slime bounce, wind 충돌·timeout 확인
8. close, quit, reload, shutdown에서 입력과 특수 작업이 남지 않는지 확인
9. 재시작 후 변환 PDC가 정상 유지되고 잘못된 아이템이 승격되지 않는지 확인

U11 Prompt 13 플레이테스트와 수치 측정은 시작하지 않았다.
