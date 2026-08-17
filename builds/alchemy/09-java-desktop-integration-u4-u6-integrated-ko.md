# 양조 Java/Desktop Integration U4~U6 통합 보고서

## 전체 호출 구조

`farming player-data → AbundancePointService → FarmingAbundanceBridge → EssenceRegistry`

`ItemStack PDC → PaperPotionPdcContract → PotionRegistry → PaperPotionUseService → EffectService`

`alchemy/recipes.yml → YamlAlchemyRecipeRegistry → PotionRegistry/RPGItemService 검증 → 후속 Crafting GUI transaction`

## 이중 검증 결과

설계 문서 대조에서는 풍요 포인트·포션·레시피를 각각 독립 경계로 분리하고, 미확정 콘텐츠를 활성화하지 않는 조건을 확인했다. 코드 대조에서는 기존 Farming/Effect/Item 서비스 재사용, snapshot 교체, PDC 검증, disabled/unknown ID 거부를 확인했다. 두 검증 모두에서 기존 농사 저장 구조나 전투 수식의 중복 구현은 발견되지 않았다.

## 상태 분류

- `RESOLVED_MAPPING`: 기존 PlayerRPGData/FarmingProfile, AbundancePointService, EffectService, RPGItemService 연결
- `IMPLEMENT_IN_UNIT_9`: GUI 슬롯 매핑, 원자적 재료 차감·출력 전달, 입력 반환
- `IMPLEMENT_IN_UNIT_10`: 관리자 doctor/보안 감사/운영 명령
- `BALANCE_PENDING`: 생산 물약 수치, 지속시간, 강도, PvP/Boss 배율, 재료 수량
- `FUTURE_DISABLED`: 미확인 farming/loot 재료, corrosion essence, production potion, catalyst 실행

## 자동 검증

`133 tests / 0 failures / 0 skipped`.

빌드 산출물: [HyunseoRPG-0.1.0-SNAPSHOT-alchemy-u4-u6-integrated.jar](C:\Users\User\Documents\Codex\2026-06-29\hyunseorpg-paper-spigot-rpg-hyunseorpg-1\builds\alchemy\HyunseoRPG-0.1.0-SNAPSHOT-alchemy-u4-u6-integrated.jar)

SHA-256: `D46823756BF074AE7EEB00AB5DEBF2E897E17C0D068B22AB8C39EF891A0CBEE2`

## 실서버 미검증 목록

- 현재 기본 설정에는 `silence`, `root`, `stun` 활성 정의가 없어 해당 상태효과의 스킬 입력·이동·공격 차단은 이 단계의 인게임 항목이 아니다. 효과 정의가 활성화된 후 별도 전투 검증으로 넘긴다.
- 일반 이동·채팅과 차단 상태의 상호작용, 점프·슬라임·경사면 예외, 피격과 공격 판정 분리는 현재 활성 효과가 없어 실서버에서 판정할 수 없다.
- 포션 실제 ItemStack 생성·사용·소비와 vanilla brewing/hopper 우회 차단은 U5 계약만 완료된 상태다.
- GUI 제작·출력 공간 부족·중복 클릭·입력 반환은 U6 후속 GUI Unit에서 검증한다.
