# 인챈트북 가격 심층 분석

기준일: 2026-08-04  
상태: `BALANCE_PENDING` / 실제 상점·획득 경로 확정 전 적용 금지

## 1. 현재 구조

소스 `enchants.yml`에는 32개 인챈트 정의와 대응 책 ID가 있다. 소스 기본 `shops.yml`은 다음 3개만 구매 가능하게 정의한다.

| 책 ID | 소스 기본 가격 | 통화 |
|---|---:|---|
| `enchant_book_blade_throw` | 8 | `magic_stone` |
| `enchant_book_light_greatsword` | 10 | `magic_stone` |
| `enchant_book_laser_arrow` | 9 | `magic_stone` |

실제 외부 `shops.yml`은 32개 인챈트북을 보관하고 있지만 모두 `purchasable: false`, `buy-price: 0`이다. 따라서 현재 운영 상태는 “32개가 상점에서 판매된다”가 아니라 “32개 상품 정의는 있으나 구매 경로가 닫혀 있다”로 기록한다.

`crafting.yml`에는 인챈트북 제작 레시피가 없다. 따라서 책별 제작 원가, 평균 재획득 시간, 90백분위 재획득 시간은 현재 설정만으로 계산할 수 없다. 드롭 테이블 또는 별도 획득 Registry가 추가로 필요하다.

## 2. 전수 목록

| 인챈트 | 분류 | 적용 장비 | 최대 단계 | 코드 핸들러 | 가격 계산 상태 |
|---|---|---|---:|---|---|
| `blade_chain` | 전투 기능 | 검 | I | content | 획득 경로 미확정 |
| `light_greatsword` | 전투 기능 | 검 | I | skill | 획득 경로 미확정 |
| `laser_arrow` | 전투 기능 | 활 | I | skill | 소스 상점 9, 외부 구매 불가 |
| `axe_heavy_strike` | 전투 기능 | 도끼 | I | content | 획득 경로 미확정 |
| `titans_wrath` | 전투 기능 | 도끼 | I | content | 획득 경로 미확정 |
| `protection` | 방어 수치 | 방어구 | IV | content | 획득 경로 미확정 |
| `fire_protection` | 방어 수치 | 방어구 | IV | content | 획득 경로 미확정 |
| `blast_protection` | 방어 수치 | 방어구 | IV | content | 획득 경로 미확정 |
| `projectile_protection` | 방어 수치 | 방어구 | IV | content | 획득 경로 미확정 |
| `skill_protection` | 방어 수치 | 방어구 | IV | content | 획득 경로 미확정 |
| `thorns` | 전투 보조 | 방어구 | III | content | 획득 경로 미확정 |
| `rolling_landing` | 이동·생존 편의 | 부츠 | I | content | 획득 경로 미확정 |
| `respiration` | 이동·편의 | 투구 | III | content | 획득 경로 미확정 |
| `aqua_affinity` | 채집·편의 | 투구 | I | content | 획득 경로 미확정 |
| `swift_sneak` | 이동·편의 | 레깅스 | III | content | 획득 경로 미확정 |
| `depth_strider` | 이동·편의 | 부츠 | III | content | 획득 경로 미확정 |
| `soul_speed` | 이동·편의 | 부츠 | III | content | 획득 경로 미확정 |
| `frost_walker` | 이동·편의 | 부츠 | II | content | 획득 경로 미확정 |
| `wind_arrow` | 전투 기능 | 활 | I | content | 획득 경로 미확정 |
| `fire_arrow_rain` | 전투 기능 | 활 | I | skill | 획득 경로 미확정 |
| `crossbow_barrage` | 전투 기능 | 쇠뇌 | I | content | 획득 경로 미확정 |
| `unbreaking` | 내구도 보조 | 장비 | I | content | 책 ID는 `durability_save_pickaxe` |
| `mining_bonus_drop` | 채집 수익 | 곡괭이 | I | content | 책 ID는 `mining_bonus_drop` |
| `area_excavation` | 채집 편의 | 곡괭이·삽·괭이 | I | content | 책 ID는 `area_mining_pickaxe` |
| `auto_replant` | 채집 편의 | 괭이 | I | content | 획득 경로 미확정 |
| `auto_smelt` | 채집 편의 | 곡괭이 | I | content | 획득 경로 미확정 |
| `chain_logging` | 채집 편의 | 도끼 | I | content | 획득 경로 미확정 |
| `treasure_finder` | 채집 수익 | 낚싯대 | I | content | 획득 경로 미확정 |
| `multi_catch` | 채집 수익 | 낚싯대 | I | content | 획득 경로 미확정 |
| `elytra_launch` | 이동·편의 | 겉날개 | I | content | 획득 경로 미확정 |
| `precision_flight` | 이동·편의 | 겉날개 | III | content | 획득 경로 미확정 |
| `explosive_mace` | 전투 기능 | 철퇴 | I | content | 획득 경로 미확정 |

`handler-id`가 존재한다는 것은 코드 연결이 있다는 뜻이지, 실제 플레이 성능이 검증됐다는 뜻은 아니다. 특히 가격을 성능으로 환산할 때는 실전 DPS·블록 틱·낚시 결과·내구도 측정이 필요하다.

## 3. 현재 설정값과 경제적 의미

| 항목 | 소스 기본 | 외부 런타임 | 판정 |
|---|---:|---:|---|
| 추출권 제작 | 마석 4 + 승급석 4 | 별도 확인 필요 | 레시피 기준 계산 가능 |
| 리롤권 제작 | 마석 2 + 승급석 1 | 별도 확인 필요 | 레시피 기준 계산 가능 |
| 추출 코인 수수료 | 6,000 | 0 | 외부가 수수료 우회 상태 |
| 리롤 코인 수수료 | 2,000 | 0 | 외부가 수수료 우회 상태 |
| 추출권 상점 | 마석 12 | 외부 1,000, 통화 공란 | 런타임 해석 확인 필요 |
| 리롤권 상점 | 마석 6 | 외부 1,000, 통화 공란 | 런타임 해석 확인 필요 |

## 4. 가격 구조 비교

### 전체 동일 가격

현재처럼 일부 책만 동일한 마석 단위로 파는 구조는 관리가 쉽지만, 자동 제련과 철퇴 폭발처럼 시간 절약과 전투 영향이 다른 기능을 동일하게 평가한다. 강한 책만 선택되는 위험이 있다.

### 유형별 가격

채집 편의·채집 수익·전투 보조·전투 기능·이동 편의의 기준 가격을 분리하는 방식이다. 현재처럼 성능 실측이 부족한 단계에서는 이 방식의 세부 숫자를 바로 확정하면 안 된다.

### 희귀도별 가격

`items.yml`의 RARE/EPIC을 기본 등급으로 사용할 수 있지만, 표시 희귀도가 실제 획득 난이도와 항상 일치하는지 먼저 검증해야 한다.

### 권고

현 단계 권고는 **유형별 기본 가격 + 희귀 기능만 개별 추가 재료**다. 다만 책의 실제 획득 경로가 확정되기 전까지는 가격을 `BALANCE_PENDING`으로 유지한다. 종결 장비가 아직 없으므로 종결 장비 전용 가격 단계도 만들지 않는다.

## 5. 코인 우회 검사

현재 외부 수수료가 0이면 플레이어는 추출·리롤을 코인 없이 반복할 수 있다. 또한 외부 상점의 티켓 가격이 1,000이고 통화 ID가 공란이면 거래 서비스의 기본 통화 해석을 확인하지 않는 한 코인 우회 또는 무료 구매 가능성을 배제할 수 없다.

가격 확정 전에 반드시 확인할 것:

1. 외부 `equipment-support.yml`의 0 수수료가 실제 런타임에 적용되는지
2. 외부 상점 티켓 `buy-price: 1000`이 코인인지, 통화 공란으로 거래 불가인지
3. 책이 상점·드롭·관리자 지급 중 어느 경로로 생성되는지
4. 추출한 책이 재적용 후 다시 추출 가능한지
5. 책과 장비를 거래하거나 복제했을 때 PDC가 유지되는지

## 6. 최종 가격 결론

현재 데이터로 확정 가능한 가격은 기존 설정값의 **참고값**뿐이다. 신규 가격을 YML에 입력하지 않았다. 책별 가격은 실제 획득 시간, 전투·채집 성능, 슬롯 경쟁, 재사용 가능성을 측정한 뒤 결정해야 한다.

