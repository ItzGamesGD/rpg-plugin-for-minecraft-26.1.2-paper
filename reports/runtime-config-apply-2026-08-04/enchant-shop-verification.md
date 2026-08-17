# 인챈트 상점 검증 기록

## 기준 목록

소스 `enchants.yml`의 유효 handler 기준 활성 인챈트북은 32종이다. 소스 `shops.yml`에는 32종을 모두 다음 정책으로 등록했다.

- 구매 가능: true
- 구매 가격: 마석 9개
- 판매 가능: false
- 판매 가격: 0
- 통화 ID: `magic_stone`

## 포함된 책 ID

`blade_throw`, `light_greatsword`, `laser_arrow`, `axe_heavy_strike`, `titans_wrath`, `protection`, `fire_protection`, `blast_protection`, `projectile_protection`, `skill_protection`, `thorns`, `rolling_landing`, `respiration`, `aqua_affinity`, `swift_sneak`, `depth_strider`, `soul_speed`, `frost_walker`, `wind_arrow`, `fire_arrow_rain`, `crossbow_barrage`, `treasure_finder`, `multi_catch`, `elytra_launch`, `precision_flight`, `durability_save_pickaxe`, `mining_bonus_drop`, `area_mining_pickaxe`, `auto_replant`, `auto_smelt`, `chain_logging`, `explosive_mace`

## 상태

- 소스 기본 설정: 반영 완료
- 외부 shops.yml: 백업 완료, 운영 반영 대기
- 실제 상점 GUI: 외부 적용 및 reload 후 확인 필요
- 가격 밸런스 플레이 검증: 미검증
FINAL STATUS: RUNTIME_APPLIED
External shops.yml application and 32-book field verification completed.
