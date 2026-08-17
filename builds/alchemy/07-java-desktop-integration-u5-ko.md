# 양조 Java/Desktop Integration U5 보고서

범위: 포션 Registry·PDC·기존 EffectService 전달 경계

## 구현

- `PotionDefinition`, `PotionRegistry`, `YamlPotionRegistry`를 추가했다.
- 활성 포션은 effect ID가 기존 `EffectService.registry()`에 존재할 때만 Registry snapshot에 들어간다.
- `PaperPotionPdcContract`는 `potion_id`, `data_version`, 선택적 `catalyst_id`를 NamespacedKey로 저장·읽는다.
- `PaperPotionUseService`는 PDC·data version·Registry·enabled를 검증한 후 delivery 종류에 맞는 `EffectSourceType`으로 기존 `EffectService.apply()`를 호출한다.
- 현재 기본 `potion_test_speed`도 생산 활성화하지 않았다. 아이템 ID가 확인되지 않은 상태에서 이름/Lore만으로 물약을 만들지 않는다.

## 설계 대조

포션 코드가 효과 수식이나 Attribute를 직접 구현하지 않고 기존 효과 엔진에 위임한다. 알 수 없는 ID, 잘못된 PDC, 구버전 데이터, disabled potion은 거부한다. DRINK/SPLASH/LINGERING은 전달 정책만 구분하고 실제 미확정 효과를 발명하지 않았다.

## 검증

U5 계약 테스트 포함 전체 `133 tests / 0 failures / 0 skipped`. 실제 ItemStack 사용·소비·재접속 및 vanilla brewing/hopper 차단은 LIVE TEST REQUIRED다.
