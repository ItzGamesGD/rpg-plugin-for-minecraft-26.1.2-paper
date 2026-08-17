# U10 양조 GUI 연결 추가 보고서

## 확인 결과

- `AlchemyGuiController`, `AlchemyGuiControllerService`, `AlchemyGuiSession`, `AlchemyGuiSessionImpl`, `AlchemyMenuHolder`는 기존에 존재했다.
- 기존 구현은 세션 교체·클릭 분류·우회 차단 계약만 있었고 실제 `Bukkit.createInventory` 호출은 없었다.
- 메인 메뉴에는 양조 진입 버튼과 호출 경로가 없었다.
- 기존 제작 연결은 농사 가공·풍요의 정수에만 존재했다.

## 수정 결과

- 메인 메뉴 슬롯 19에 `양조` 버튼 추가
- `alchemy/gui.yml`의 `enabled: true`일 때 실제 양조 Inventory 생성
- 양조 메뉴를 다음 세 영역으로 분리
  - 풍요의 정수 제작
  - 물약 제작
  - 촉매 적용
- 풍요의 정수는 기존 `CraftingGuiService.openFarmingEssence()` 사용
- 물약은 `farming-type: alchemy_potion` 레시피만 표시
- 촉매는 `farming-type: alchemy_catalyst` 레시피만 표시
- 두 영역 모두 기존 `CraftingRecipeRegistry → CraftingGuiService → CraftingTransactionService` 경로 사용
- 메뉴 클릭·드래그·세션 불일치 차단
- 메인 메뉴 복귀·닫기 처리

## 중요한 운영 상태

현재 기본 `alchemy/gui.yml`은 `enabled: false`이며 production 물약·촉매 레시피도 비활성이다. 따라서 기본 상태에서 물약·촉매 화면에 제작 결과가 없는 것은 정상이다. 설정을 활성화하고 canonical crafting recipe가 등록된 경우에만 해당 목록과 제작 버튼이 표시된다.

촉매의 실제 물약 PDC 상태 변환은 `CatalystApplicationService`의 책임으로 남아 있으며, 일반 아이템 재료를 결과 아이템으로 만드는 `CraftingTransactionService`와의 직접 변환 트랜잭션은 아직 production 활성화하지 않았다. 촉매 레시피를 활성화하기 전에는 이 두 계약을 별도 통합 검증해야 한다.

## 변경 파일

- `src/main/java/com/hyunseo/hyunseorpg/alchemy/gui/AlchemyMenuHolder.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/gui/AlchemyGuiControllerService.java`
- `src/main/java/com/hyunseo/hyunseorpg/crafting/CraftingGuiService.java`
- `src/main/java/com/hyunseo/hyunseorpg/ui/RPGMenuService.java`
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`

## 검증

- 기존 전체 자동 테스트: PASS
- 최신 결과: `140 tests / 0 failures`
- `alchemy/gui.yml` 기본 비활성 보존: PASS
- 기존 U7-U10 세션·Registry 계약: PASS

## 실서버 테스트 필요

1. 외부 `alchemy/gui.yml`을 `enabled: true`로 설정 후 `/rpg reload alchemy`
2. 메인 메뉴에서 양조 버튼 클릭
3. 세 영역 Inventory 생성 확인
4. 풍요의 정수 제작 클릭이 기존 제작 GUI로 이동하는지 확인
5. 물약·촉매 canonical recipe 활성화 후 각 목록 노출 확인
6. 좌클릭 제작·Shift 제작·재료 부족·인벤토리 부족 확인
7. 촉매 적용 시 Potion PDC와 catalyst PDC가 함께 검증되는지 확인
8. Shift-click·숫자키·드래그로 양조 메뉴 아이템 탈취가 불가능한지 확인
