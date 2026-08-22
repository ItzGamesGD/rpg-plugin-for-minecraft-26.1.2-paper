# E7 전 단계 — 데스크톱 소스 통합 기록

기준 브랜치: `codex/exploration-latest-sync`

## 완료

1. 탐험 패키지의 `src/main/java/com/hyunseo/hyunseorpg/exploration/` 전체를 최신 작업 브랜치의 동일 경로에 편입했다.
2. `src/main/resources/exploration/`을 편입했다.
3. 탐험 단위 테스트 4개를 `src/test/java`에 편입했다.
4. `ExistingHyunseoRpgAdapters`의 현재 소스 signature를 확인했다.
   - `MobService#spawnCustomMob(Location, String, Integer, String)`
   - `RPGItemService#create(String, int)`
   - `InventoryDeliveryService#giveOrDrop(Player, Location, ItemStack)`
5. `HyunseoRPGPlugin`에 최소 bootstrap을 추가했다.
   - module 단일 소유
   - 기존 Mob/Item/Inventory service adapter 연결
   - `exploration` reload 등록
   - disable 시 exploration stop/flush
6. 안전 기본값은 변경하지 않았다. 운영 서버에서 모듈이 자동 활성화되지 않는다.

## 최신 설계 경계

탐험 E1-E6와 영지 T1-T9는 이 브랜치에서 합치지 않는다. 최신 영지 확정안은 `builds/exploration-package-v1/latest-design-sync.md`에 반영했다.

## 다음 게이트

- JDK 25 환경에서 `./gradlew clean test`
- 탐험 관련 compile/test 오류와 기존 프로젝트 오류 분리
- 개발 월드에서만 prototype을 활성화해 detection → activation → clear/abandon → cleanup live test
- E7 실제 구조물 이벤트는 E6 live 검증 통과 후 착수
