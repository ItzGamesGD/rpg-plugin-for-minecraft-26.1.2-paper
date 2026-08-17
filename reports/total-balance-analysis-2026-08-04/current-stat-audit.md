# HyunseoRPG 현재 수치·계산 경로 감사

기준일: 2026-08-04
상태: `SOURCE_DEFAULT_UPDATED` / `EXTERNAL_CONFIG_MIGRATION_REQUIRED` / `PLAYTEST_VERIFIED 아님`

## 범위와 원칙

이번 문서는 경제·전투·몬스터·도구 성장의 현재 구현을 복원한 감사 보고서다. 수치의 운영 적용이나 기존 플레이어 데이터 변경은 수행하지 않았다. 모든 수치는 다음 표기를 사용한다.

| 표기 | 의미 |
|---|---|
| `SOURCE_DEFAULT` | JAR에 포함되는 기본 YAML 값 |
| `EXTERNAL_RUNTIME` | 실제 서버 외부 YAML 값 |
| `INFERRED_FROM_CODE` | Java 계산 경로에서 확인한 기본값 또는 공식 |
| `PLAYTEST_REQUIRED` | 실제 틱·전투·행동 측정이 없으면 확정할 수 없음 |
| `BALANCE_PENDING` | 후속 밸런스 승인 전 적용하지 않음 |

## 핵심 현재값

| 영역 | 현재값 | 출처 | 판정 |
|---|---:|---|---|
| 최대 강화 | 50 | `equipment-growth.yml`, `EquipmentTierService` | `SOURCE_DEFAULT` |
| RPG 최대 레벨 | 100 | `exp.yml` | `SOURCE_DEFAULT` |
| 채광 보상 | 25코인 | `progression-loop.yml` | `SOURCE_DEFAULT`, 실측 필요 |
| 벌목 보상 | 20코인 | `progression-loop.yml` | `SOURCE_DEFAULT`, 실측 필요 |
| 농사 보상 | 15코인 | `progression-loop.yml` | `SOURCE_DEFAULT`, 실측 필요 |
| 교배 보상 | 12코인 | `progression-loop.yml` | `SOURCE_DEFAULT`, 실측 필요 |
| 도축 보상 | 6코인 | `progression-loop.yml` | `SOURCE_DEFAULT`, 실측 필요 |
| 낚시 보상 | FISH 10 / TREASURE 35 / JUNK 3 | `progression-loop.yml` | 확률 누락, 기대값 보류 |
| 강화석 상점 | 350 구매 | `shops.yml` | 정책 충돌, 변경 보류 |
| 신호기 | 60,000 구매 | `shops.yml` | `BALANCE_PENDING` |
| 수리 최소 비용 | 50코인 | `config.yml`, `EquipmentRepairService` | 현재값 |
| 수리 내구도 가중치 | 0.0 | `config.yml` | 내구도 계수 효과 미활성 |

## 설정 불일치

| 항목 | 기본 YAML | 외부 서버 | 의미 |
|---|---|---|---|
| 건축 활동 | 없음 | `BUILDING` 경로 잔존 | 외부 설정 마이그레이션 필요 |
| 마석 제작 | 9 파편 → 1 마석 | 외부 레시피 누락 | 외부 설정 마이그레이션 필요 |
| XP 배율 | 1.0 | 키 없음, 코드 기본 1.0 | 0.90 미적용 |
| 인챈트 상점 | 3개 구매 가능 | 32개 정의, 모두 구매 불가 | 상점 정책 재조사 필요 |

## 전투력 계산 경로

`CombatService`는 직접 피해에 RPG 공격력, 강화, 승급, 방어 감소를 계산한다. 별도로 `EquipmentActualEffectListener`가 `EntityDamageByEntityEvent`에서 강화 공격력·승급 공격력·도끼 보정·투사체·삼지창·치명타를 적용한다. 따라서 다음은 코드상 위험 지점이다.

1. 직접 공격과 플러그인 생성 피해가 어느 경로를 통과하는지 실제 이벤트 로그로 구분해야 한다.
2. 같은 이벤트가 두 서비스에서 모두 계산되면 강화·승급 값이 중복될 수 있다.
3. `CombatService`의 스킬 피해와 이벤트 리스너의 직접 공격 피해는 적용 순서가 다르다.
4. 인챈트 피해는 별도 경로이므로 장비 승급의 전체 스킬 피해와 중복 여부를 확인해야 한다.

## 장비 강화·승급

- 일반 재질 장비와 네더라이트의 canonical 최대 강화는 +50이다.
- 네더라이트 최대 승급 단계는 6단계다.
- 겉날개는 +50 강화 상한이 있으나 강화 수치가 실제 전투 능력으로 변환되지는 않는다.
- 강화 성공 확률은 시작 1.0, 종료 0.20, 실패 보너스 0.04, 상한 1.0이다.
- 강화 코인과 강화석 곡선은 정규화 진행률 0~1을 기준으로 한다.
- 도구 +50은 `0.65 × 50 × 0.1 = 3.25`의 `BLOCK_BREAK_SPEED` 보정이 계산상 발생한다. 실제 블록 파괴 틱은 도구·블록·효율 인챈트·서버 버전에 따라 측정해야 한다.

## 수리

`EquipmentRepairService`는 `Damageable`의 damage를 손실 내구도로 사용한다.

`최종 비용 = (base + damage × cost-per-durability) × durability-factor × tier-factor × enhancement-factor × promotion-factor`

현재 `durability.weight=0.0`이므로 내구도 비율 지수식은 비용을 추가로 가중하지 않는다. 현재 구현이 요구된 “손실 내구도 영향 최우선” 정책과 일치하는지는 실측 및 후속 밸런스 승인 전 보류한다.

## 즉시 확인이 필요한 위험

| 우선순위 | 항목 | 이유 |
|---|---|---|
| 높음 | 전투 계산 중복 | DPS와 강화 효율이 실제보다 높게 측정될 수 있음 |
| 높음 | 도구 +50 파괴 틱 | lore/attribute 값만으로 Efficiency V와 비교할 수 없음 |
| 높음 | 커스텀 몬스터 스케일링 | 고정 HP·피해와 일반 레벨 스케일이 분리되어 있음 |
| 중간 | 외부 건축 경로 | 실제 서버에서 폐지 정책과 불일치할 수 있음 |
| 중간 | 낚시 확률 | 보상값만 있고 등급 확률이 없음 |
| 중간 | 인챈트 상점 | 소스 기본과 외부 운영 설정의 구매 상태가 다름 |

