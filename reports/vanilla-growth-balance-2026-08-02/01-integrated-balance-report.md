# HyunseoRPG 통합 밸런스 보고서 — 최신 재계산본

기준일: 2026-08-02  
범위: 바닐라 성장·활동 경제·강화·승급·수리·보조 아이템·보스 보상·XP 모델

## 릴리스 상태

| 항목 | 상태 |
|---|---|
| 소스 기본값 | SOURCE_DEFAULT_UPDATED |
| 외부 설정 | EXTERNAL_CONFIG_MIGRATION_REQUIRED |
| 실제 서버 런타임 | RUNTIME_APPLIED 아님 |
| 실제 플레이 검증 | PLAYTEST_VERIFIED 아님 |
| 밸런스 확정 | balance-pending |

외부 서버 파일은 읽기 전용으로 비교했다. 기존 플레이어의 강화·승급·인챈트·PDC는 변경하지 않는다.

## 1. 구버전 분리와 적용 원칙

과거 `2026-08-02_105` 통합 보고서는 `reports/archive-2026-08-02_105_integrated-balance-analysis.md`로 이동하고 `ARCHIVED — 운영 적용 금지`를 표시했다. 다음 값은 최신 기준으로 사용하지 않는다.

- 채광 6코인
- 강화석 상점가 350
- 강화석 코인 직접 구매
- 인챈트북 마석 3·5·4
- 추출 수수료 4,000
- 리롤 수수료 1,200
- 강화 수수료 15,282코인 확정
- 신호기 60,000코인 확정
- XP 배율 0.90 확정

현재 보고서에는 최신 소스와 외부 YAML의 현재값을 별도로 적고, 실측 없는 값은 최종 적용값으로 표현하지 않는다.

## 2. 외부 설정과 기본 설정 차이

| 항목 | 소스 기본 YAML | 실제 외부 YAML | 판정 |
|---|---|---|---|
| 채광 코인 | 25 | 25 | 현재값만 기록, 최종 확정 아님 |
| 벌목 코인 | 20 | 20 | 실측 필요 |
| 농사 코인 | 15 | 15 | 실측 필요 |
| 낚시 | FISH 10 / TREASURE 35 / JUNK 3 | 동일 | 확률이 YAML에 없어 기대값 확정 불가 |
| 교배·도축 | 12 / 6 | 12 / 6 | 실측 필요 |
| 건축 | 없음 | BUILDING 1 존재 | 외부 마이그레이션 필요 |
| XP 배율 | 1.0 | 키 없음, 코드 기본 1.0 | 0.90 미적용 상태가 최신 기준 |
| 신호기 | 60,000 | 60,000 | 기존값, balance-pending |
| 마석 레시피 | 존재 | 없음 | 외부 마이그레이션 필요 |

외부 `progression-loop.yml`의 건축 경로와 외부 `crafting.yml`의 마석 레시피 누락은 이번 JAR의 명시적 마이그레이션으로 처리한다. 외부 서버에 실제 적용하기 전에는 백업과 `/rpg migrate configs -apply`가 필요하다.

## 3. 활동 행동률 재작성

이전의 180·300·450회/시간 가정은 폐기한다. 채광 이론 모델은 실제 블록 파괴 속도를 직접 입력한다.

| 속도 | 시간당 유효 블록 | 현재 25코인 설정을 적용한 참고 총수입 |
|---:|---:|---:|
| 0.5블록/초 | 1,800 | 45,000 |
| 1블록/초 | 3,600 | 90,000 |
| 2블록/초 | 7,200 | 180,000 |
| 4블록/초 | 14,400 | 360,000 |

표의 코인은 설정값을 대입한 참고값이며 최종 경제값이 아니다. 이동·탐색·도구 교체·인벤토리 정리·설치 블록 제외·사망·실패 행동을 반영한 실측값이 없기 때문이다.

실측 입력은 `02-activity-speed-sensitivity.md`와 `05-repair-measurement-template.md`의 양식을 사용한다. 최소 필드는 총 측정 시간, 유효 행동 수, 설치 블록 제외 수, 미성숙 작물 제외 수, 획득 코인, 강화석 파편, 마석 파편, 내구도 손실, 정리 시간, 이동·탐색 시간이다.

## 4. 낚시 기대값

사용자가 제시한 70% 물고기·10% 보물·20% 쓰레기와 보상 8·30·2를 사용할 경우:

`8 × 0.70 + 30 × 0.10 + 2 × 0.20 = 9코인/행동`

그러나 현재 외부 YAML에는 확률이 없고 등급별 보상만 `FISH=10`, `TREASURE=35`, `JUNK=3`으로 존재한다. 따라서 9코인과 시간당 수입은 참고 시나리오로만 기록하며, 실제 낚시 결과 분포를 측정하기 전에는 확정하지 않는다.

## 5. 강화 수수료 후보

기존 강화 기대값인 약 91.8회 시도와 약 1,447.1 강화석은 유지한다. 강화석 1개가 평균 50개 유효 블록을 요구하고 채광 보상이 25코인이라는 가정을 임시 대입하면:

- 필요 유효 블록: `1,447.1 × 50 = 약 72,356개`
- 참고 생성 코인: `72,356 × 25 = 약 1,808,900코인`
- 기존 수수료 15,282코인: 약 0.84%

강화 수수료 후보는 다음처럼 비교한다.

| 후보 | 0-50 총 수수료 참고값 |
|---:|---:|
| 채광 과정 코인의 10% | 180,890 |
| 20% | 361,780 |
| 30% | 542,670 |
| 35% | 633,246 |

위 값은 `25코인/블록`과 `50블록/강화석`을 동시에 가정한 민감도 계산이다. 채광 25코인을 최종 유지할지, 활동 코인을 낮추고 수수료를 완만하게 둘지는 사용자 확인 및 실측 후 결정한다.

비교안:

- A안: 채광 25 유지, 강화 수수료를 위 후보 중 선택
- B안: 활동 보상 하향, 강화 수수료는 기존 곡선에 가까운 완만한 수준

이번 작업에서는 A/B 어느 쪽도 운영값으로 확정하지 않았다.

## 6. 공동 생산 재화 모델

강화석 파편과 마석 파편은 같은 활동에서 함께 나올 수 있으므로 필요 활동 수를 합산하지 않는다.

`requiredActions = max(upgradeStones × 10 / 0.20, magicStones × 9 / 0.08)`

단위 기대값:

- 강화석 1개: `10 / 0.20 = 50회`
- 마석 1개: `9 / 0.08 = 112.5회`

0-50 강화만 보면 약 1,447.1 강화석에 대해 `72,355회`가 강화석 병목이다. 네더라이트 최대 승급만 보면 기존 보고서의 189 승급석 환산을 기준으로 강화석 189개와 마석 189개가 필요하다.

- 강화석 측: `189 × 50 = 9,450회`
- 마석 측: `189 × 112.5 = 21,262.5회`
- 승급만의 병목: 마석

강화와 최대 승급을 동시에 수행하면 0-50 강화의 강화석 요구량이 더 커져 전체 병목은 강화석으로 이동할 수 있다. 따라서 “네더라이트 전체 성장”과 “최대 승급 단독”을 분리해서 보고한다.

## 7. 보스 보상 활동 절약량

현재 보상은 임시 유지이며 권장 확정값이 아니다.

| 보스 | 보상 | 공동 생산 기준 활동 수 | 해석 |
|---|---|---:|---|
| 위더 | 5,000코인 + 강화석 32 + 마석 20 | `max(32×50, 20×112.5)=2,250` | 마석 병목 |
| 엔더 드래곤 | 20,000코인 + 마석 80 | `80×112.5=9,000` | 마석 병목 |

시간 절약은 `필요 활동 수 ÷ 실측 활동 속도`로 계산한다. 최초 도달 시간, 소환 재료 획득 시간, 전투 시간, 실패율, 서버 가동시간 쿨다운, 기여도 분배가 입력되기 전에는 보스 보상의 경제 가치를 확정하지 않는다.

## 8. 신호기 정책

최신 정책은 다음과 같다.

- 위더 처치 시 네더의 별 드롭 차단
- 바닐라 신호기 제작 차단
- 통합 상점에서 코인으로만 구매
- 추가 재료 요구 없음
- 판매 불가

현재 YAML의 60,000코인은 기존값으로 남아 있지만 `balance-pending`이다. 실측 시간당 코인을 기준으로 후보만 제시한다.

`시간당 코인 × 목표 시간`으로 계산하며 목표는 2시간, 4시간, 8시간이다. 시간당 수입 범위가 확정될 때까지 가격을 자동 변경하지 않는다.

## 9. XP와 원소 진입 레벨

XP `required-exp-multiplier: 0.90`은 확정하지 않는다. 소스 기본값을 1.0으로 되돌렸고, 외부 YAML에는 해당 키가 없어 런타임 기본값도 1.0이다.

비교 대상은 주력 무기 +20·+30·+40·+50, 첫 승급·중간 승급, 복수 인챈트 획득·사용, 리롤·추출 경험이다. XP 공급원은 일반 RPG 몬스터, 커스텀 몬스터, 자동 퀘스트, 일반 퀘스트, 보스, 활동을 시간당으로 측정해야 한다.

원소 진입 레벨은 이번 작업에서 변경하지 않는다. 현재 값은 `mining_giant=8`, `frost_blaze=12`, `explosive_breeze=16`, `riptide_drowned=18`이며, 각 레벨 도달 시점의 강화·승급·인챈트·리롤 경험을 측정한 뒤 조정한다. 레벨 미달자의 원소 파편 미지급 정책은 유지한다.

## 10. 최신 인챈트 상점 대조

소스 `items.yml`와 `enchants.yml`에는 32개 인챈트북 ID가 있다. 외부 `shops.yml`에도 이 32개가 존재하지만 모두 `purchasable: false`, `buy-price: 0`으로 기록되어 있다. 소스 기본 `shops.yml`의 정식 구매 경로는 3개뿐이다.

| 소스 기본 정식 구매 | 가격 | 통화 | 외부 상태 |
|---|---:|---|---|
| enchant_book_blade_throw | 8 | magic_stone | 같은 ID, 구매 불가 |
| enchant_book_light_greatsword | 10 | magic_stone | 같은 ID, 구매 불가 |
| enchant_book_laser_arrow | 9 | magic_stone | 같은 ID, 구매 불가 |

외부에 존재하는 전체 인챈트북은 다음과 같다. 가격과 구매 여부는 전체 목록 확정 전 수정하지 않는다.

`enchant_book_blade_throw`, `enchant_book_light_greatsword`, `enchant_book_axe_heavy_strike`, `enchant_book_chain_logging`, `enchant_book_titans_wrath`, `enchant_book_auto_smelt`, `enchant_book_area_mining_pickaxe`, `enchant_book_auto_replant`, `enchant_book_protection`, `enchant_book_fire_protection`, `enchant_book_projectile_protection`, `enchant_book_skill_protection`, `enchant_book_respiration`, `enchant_book_swift_sneak`, `enchant_book_rolling_landing`, `enchant_book_soul_speed`, `enchant_book_explosive_mace`, `enchant_book_aqua_affinity`, `enchant_book_blast_protection`, `enchant_book_frost_walker`, `enchant_book_depth_strider`, `enchant_book_thorns`, `enchant_book_laser_arrow`, `enchant_book_wind_arrow`, `enchant_book_fire_arrow_rain`, `enchant_book_treasure_finder`, `enchant_book_multi_catch`, `enchant_book_crossbow_barrage`, `enchant_book_elytra_launch`, `enchant_book_precision_flight`, `enchant_book_durability_save_pickaxe`, `enchant_book_mining_bonus_drop`.

효과 구현 여부는 `enchants.yml`의 handler와 실제 런타임 테스트를 따로 확인해야 하며, 상점 등록만으로 구현 완료로 판정하지 않는다.

## 11. 마석 레시피 구현과 마이그레이션

정식 레시피는 `magic_stone_fragment 9개 → magic_stone 1개`다. 소스 기본 `crafting.yml`에는 이미 존재하고, 외부 `crafting.yml`에는 누락되어 있었다.

`ConfigMigrationService#migrateCraftingAmounts()`에 다음 동작을 추가했다.

1. 외부 레시피가 없을 때 기본 YAML의 전체 레시피 subtree 복사
2. 재료 카테고리 목록에 누락 시 추가
3. canonical layout에 누락 시 추가
4. 기존 사용자 레시피와 가격·배치는 덮어쓰지 않음
5. 이후 `CraftingRecipeRegistry`가 재생성되어 통합 GUI와 `CraftingTransactionService`에서 사용

바닐라 제작대나 레거시 `/craft`, `/craft2` 경로를 새로 열지 않는다. 쉬프트 최대 제작, 원자적 재료 차감·결과 지급, 공간 초과 PendingReward 처리는 기존 CraftingTransactionService 경로를 사용한다.

## 12. 변경 파일과 적용 상태

| 파일 | 변경 | 상태 |
|---|---|---|
| `src/main/java/.../ConfigMigrationService.java` | 외부 마석 레시피·카테고리·layout 병합 | SOURCE_DEFAULT_UPDATED / 외부 적용 필요 |
| `src/main/resources/exp.yml` | 미검증 XP 0.90을 1.0으로 중립화 | SOURCE_DEFAULT_UPDATED |
| `src/test/java/.../BalanceConfigurationTest.java` | XP 중립값·마석 레시피 회귀 검사 | 자동 검증 대상 |
| `reports/vanilla-growth-balance-2026-08-02/` | 최신 계산·민감도·보류 상태 문서화 | 최신 문서 |
| `reports/archive-2026-08-02_105_integrated-balance-analysis.md` | 구버전 보고서 보관 | 운영 적용 금지 |

외부 서버의 `crafting.yml`은 아직 수정하지 않았다. 따라서 현재 서버는 마이그레이션 전 상태이며, JAR 배포만으로 외부 설정이 자동으로 바뀐다고 간주하지 않는다.

## 13. 검증 결과와 남은 불확실성

자동 검증은 기본 YAML의 레시피 정의, layout, XP 중립값 및 기존 테스트를 대상으로 수행한다. 실제 활동 행동률, 낚시 확률, 수리비, 보스 전투시간, XP 공급량, 인챈트 상점 전체 구매정책, 신호기 목표가격은 실측 전이다.

운영 적용 절차는 다음과 같다.

1. 외부 `plugins/HyunseoRPG` 전체 백업
2. 새 JAR을 테스트 서버에 적용
3. `/rpg migrate configs -apply`
4. 외부 `crafting.yml`에 마석 레시피·layout이 추가됐는지 확인
5. `/rpg doctor` 실행
6. `/rpg reload` 및 완전 재시작
7. 마석 파편 9개로 1개 제작, 쉬프트 제작, 공간 부족 처리를 확인
8. 플레이 로그를 보고서 실측 입력 양식에 반영

이번 재계산의 결론은 “경제 수치 확정”이 아니라 “오류가 있는 확정값을 제거하고, 실제 측정값을 입력할 수 있는 모델과 안전한 마이그레이션을 준비함”이다.
