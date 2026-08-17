# 양조 Java/Desktop Integration U6 보고서

범위: 양조 레시피·재료 Registry 경계

## 구현

- `AlchemyRecipeDefinition`, `AlchemyRecipeRegistry`, `YamlAlchemyRecipeRegistry`, `AlchemyCraftService`, `AlchemyMaterialResolver` 계약을 추가했다.
- 레시피는 `alchemy/recipes.yml`을 authoritative source로 읽으며, disabled 레시피는 snapshot에서 제외한다.
- 활성 레시피는 Potion Registry 결과가 존재하고 모든 ingredient ID가 기존 `RPGItemService` 또는 명시된 vanilla 경계로 확인될 때만 등록한다.
- 아이템은 Material만으로 custom item을 대체하지 않는다. 실제 재료 차감·GUI 트랜잭션은 후속 GUI Unit에서 기존 CraftingTransactionService와 연결한다.

## 설계 대조

새 레시피마다 Java switch/ID 목록을 추가하지 않는 Registry 구조를 유지했다. 현재 production potion·farming/loot 재료 ID가 확정되지 않았으므로 모두 disabled 상태이며, 누락 ID를 바닐라 재료로 조용히 대체하지 않는다.

## 검증

U6 계약 테스트 포함 전체 `133 tests / 0 failures / 0 skipped`. 실제 GUI 슬롯·원자적 재료 차감·출력 전달은 U9 및 LIVE TEST REQUIRED다.
