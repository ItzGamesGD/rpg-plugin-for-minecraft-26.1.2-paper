# 인챈트 상점 조사 — 최신본

외부 `shops.yml`의 최신 상태를 우선한다. 외부에는 32개 인챈트북이 존재하지만 전부 `purchasable: false`, `buy-price: 0`이다. 소스 기본값에서 구매 가능하도록 정의된 것은 다음 3개뿐이다.

| ID | 소스 가격 | 통화 | 외부 |
|---|---:|---|---|
| enchant_book_blade_throw | 8 | magic_stone | 구매 불가 |
| enchant_book_light_greatsword | 10 | magic_stone | 구매 불가 |
| enchant_book_laser_arrow | 9 | magic_stone | 구매 불가 |

전체 ID 목록은 통합 보고서에 기록했다. 효과 구현 여부·장비 대상·반복 사용 가치·추출·리롤 비용을 전부 확정하기 전에는 8·10·9를 운영 가격으로 확정하지 않는다.
