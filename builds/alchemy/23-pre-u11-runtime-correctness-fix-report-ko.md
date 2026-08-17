# Alchemy Pre-U11 Runtime Correctness Fix Pass

기준: 최신 소스 / Production Completion A·B 이후

## 판정

`PRE_U11_RUNTIME_PASS`

단, Paper 라이브 서버에서의 실제 섭취·전투·속성 변화는 `LIVE_SERVER_UNVERIFIED`로 남긴다. 이번 작업은 U11 Prompt 13 플레이테스트나 밸런스 확정이 아니다.

## 1. 다중 효과 현상 원인

소스 호출 경로를 대조한 결과, 현재 최신 소스에는 플레이어 UUID 하나에 효과 하나만 저장하는 단일 슬롯 구조가 없었다. 기존 구현도 실질적으로 다음 구조를 사용했다.

`player UUID -> effect_id -> ActiveEffectInstance`

`EffectService.applyInternal()`은 `definition.id()`로 같은 효과만 조회하므로, 서로 다른 `effect_id`는 별도 Map 항목에 저장된다. `conflicts.yml`은 현재 비어 있으며, 현재 production effect 사이에 명시적 충돌도 없다. 따라서 최신 소스만으로는 “서로 다른 물약을 먹었는데 active count가 1로 유지”되는 현상을 재현할 수 없었다.

가장 유력한 과거 원인은 이전 빌드에서 일반 `/rpg give potion_*`가 `RPGItemService.create()`만 호출하여 `potion_id`와 `data_version`을 누락했던 경로다. 현재 소스에는 이 경로가 `PotionFactory`를 사용하도록 이미 수정되어 있다. 이번 패스에서는 이 계약을 유지하면서 효과 저장 구조를 전용 저장소로 분리하고 다중 효과 회귀 테스트를 추가했다.

## 2. 변경 사항

- `ActiveEffectStore` 추가
  - 키: 플레이어 UUID + 정규화된 effect ID
  - 서로 다른 효과 동시 저장
  - 동일 효과 ID는 하나의 슬롯에서 EffectService stack policy가 처리
  - 단일 제거가 다른 효과를 건드리지 않음
  - `removeAll`은 요청한 플레이어만 정리
- `EffectService`가 익명 중첩 Map 대신 `ActiveEffectStore`를 사용
- `ActiveEffectStoreTest` 추가
  - 서로 다른 효과 2개 동시 활성
  - 단일 제거 후 다른 효과 유지
  - 동일 effect ID 교체 슬롯
  - 서로 다른 플레이어 간 동일 effect 분리
  - 전체 제거 범위 확인

## 3. 포션 호출 경로 대조

현재 production 경로는 다음과 같다.

`PotionFactory.create()`
→ RPGItemService canonical item 생성
→ `PotionPdcContract.write()`로 `potion_id`, `data_version` 기록
→ `PlayerItemConsumeEvent`
→ `PaperPotionUseListener.onConsume()`
→ `PaperPotionUseService.use()`
→ PotionRegistry lookup
→ effect ID 검증
→ `EffectService.apply()` 또는 변환 포션의 `applyWithOverrides()`
→ `ActiveEffectStore`
→ `HandlerRegistry`
→ concrete production handler

일반 `/rpg give potion_<id>`도 현재 `PotionRegistry`와 `PotionFactory`를 사용한다. 알 수 없는/비활성 포션 ID는 generic item fallback을 타지 않고 거부한다. `/rpg alchemy give`도 같은 `PotionFactory`를 사용한다.

## 4. 충돌·재적용 정책

- 서로 다른 effect ID: 현재 설정상 충돌 없음, 동시 활성 허용
- 같은 effect ID: 각 Definition의 `REFRESH_DURATION` 등 StackPolicy 적용
- 단일 remove: effect ID 하나만 제거
- clear: 해당 플레이어의 전체 효과 제거
- 만료: 해당 instance만 제거
- death/logout/world change/reload/shutdown: Definition 정책 및 lifecycle cleanup 적용

현재 `EffectConflictResolver`는 동일 effect 슬롯의 재적용 판정을 받는 구조이며, `conflicts.yml`의 명시적 교차 충돌은 현재 등록되어 있지 않다. 이번 패스에서는 임의의 상호 배타 규칙을 추가하지 않았다.

## 5. 8개 production effect 상태

| effect | Registry | Handler 등록 | 현재 coexistence | 수치 |
|---|---|---|---|---|
| effect_vampire | 활성 | vampirism | 가능 | BALANCE_PENDING |
| effect_berserk | 활성 | berserk | 가능 | BALANCE_PENDING |
| effect_corrosion | 활성 | corrosion | 가능 | BALANCE_PENDING |
| effect_frostbite | 활성 | frostbite | 가능 | BALANCE_PENDING |
| effect_shock | 활성 | shock | 가능 | BALANCE_PENDING |
| effect_bleed | 활성 | bleed | 가능 | BALANCE_PENDING |
| effect_vulnerability | 활성 | vulnerability | 가능 | BALANCE_PENDING |
| effect_necrosis | 활성 | necrosis | 가능 | BALANCE_PENDING |

핸들러의 실제 Bukkit 속성 변경, 주기 피해, 피해 보정, 회복 콜백은 Paper 엔티티가 필요한 영역이므로 라이브 서버 검증은 별도다.

## 6. 검증 결과

- 기준선: `147 tests / 0 failures / 0 skipped`
- 수정 후: `150 tests / 0 failures / 0 skipped`
- `./gradlew clean test`: 성공
- Java 컴파일 경고: Paper API의 일부 deprecated Attribute API 경고만 존재하며 이번 범위에서 변경하지 않음

## 7. LIVE_SERVER_UNVERIFIED

- 실제 Paper 서버에서 포션 8종을 연속 섭취했을 때 active count가 1→2→3으로 증가하는지
- 동일 포션 재섭취 시 실제 만료 시각이 갱신되는지
- 포션 섭취 후 각 handler의 AttributeModifier·주기 피해·피해 보정이 실제 엔티티에 적용되는지
- `/rpg give potion_<id>`와 `/rpg alchemy give` 아이템의 실제 PDC 및 섭취 결과
- 죽음·로그아웃·월드 이동·reload 후 Paper 엔티티 attribute 잔존 여부

## 8. 설계 대조

현재 변경은 A/B의 production path를 재구현하지 않고, 효과 instance 저장 책임만 명시적으로 분리했다. 포션 생성은 PotionFactory, 포션 소비는 PaperPotionUseService, 효과 적용은 EffectService, handler 실행은 HandlerRegistry라는 기존 경계를 유지한다. 새 콘텐츠·레시피·밸런스 값·catalyst 로직은 추가하지 않았다.

U11 Prompt 13 및 U12는 자동 시작하지 않는다.
