# 양조 U9 검증 보고서: GUI·원자 트랜잭션

## 구현

- `AlchemyGuiSession`, `AlchemyGuiSessionImpl` 추가
- `AlchemyGuiController`, `AlchemyGuiControllerService` 추가
- `AlchemyMenuHolder`, `PaperInventoryEventAdapter` 추가
- `AtomicCraftTransaction`, `TransactionCoordinator`, `AtomicTransactionCoordinator` 추가
- 플러그인 종료 시 GUI session 정리

## 설계 대조

세션 교체와 종료는 기존 세션을 먼저 닫는다. Shift, 숫자키, 보조손, 드래그, 더블클릭,
Collect to Cursor, hopper 이동은 기본 차단한다. GUI 식별은 title 문자열이 아니라
`InventoryHolder`로 한다.

트랜잭션 순서는 다음 계약으로 고정했다.

`validate → output capacity → commit → output delivery confirmation → unlock`

입력 반환은 미커밋 상태에서만 허용하고, 이미 커밋된 입력을 다시 반환하지 않는다.

## 코드 대조

- GUI는 recipe/effect/player-data 로직을 직접 구현하지 않는다.
- 실제 레시피와 재료 계산은 기존 Registry/Service가 담당한다.
- 현재 `alchemy/gui.yml`이 disabled이므로 운영 GUI를 자동 노출하지 않는다.
- Paper 실서버 InventoryClick/Drag/Close와 실제 ItemStack delivery는 아직 LIVE TEST REQUIRED다.

## 남은 위험

- full inventory output 실패 복구
- close/logout/shutdown 중 commit
- hopper·cursor·offhand 우회
- 실제 potion recipe 활성화 후 output PDC 생성

