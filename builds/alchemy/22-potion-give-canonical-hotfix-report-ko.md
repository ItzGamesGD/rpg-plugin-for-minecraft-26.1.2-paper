# 포션 일반 지급 canonical 경로 Hotfix

## 원인

일반 `/rpg give <item_id>` 경로가 모든 ID를 `RPGItemService.create()`로만 생성하고 있었습니다. 이 생성기는 `item_id`만 기록하므로 포션의 필수 PDC인 `potion_id`와 `data_version`이 누락되었습니다. 그 결과 `PaperPotionUseListener`가 비정규 아이템을 포션으로 인정하지 않았습니다.

## 수정

`RPGGiveCommand`의 일반 지급 경로를 다음처럼 분리했습니다.

- `potion_*` 요청은 `PotionRegistry`에서 활성 정의를 확인한 뒤 `PotionFactory`로 생성
- 포션 Registry 미등록, 비활성, Factory 미준비, 생성 실패는 일반 아이템 생성기로 우회하지 않음
- 일반 아이템은 기존 `RPGItemService` 경로 유지
- `/rpg alchemy give`의 기존 canonical 경로는 변경하지 않음

따라서 일반 지급 포션도 다음 정보를 함께 가집니다.

- `item_id`
- `potion_id`
- `data_version`

## 검증

- `RPGGiveCommand` 포션 ID canonical 경계 테스트 추가
- 전체 테스트: `147 tests / 0 failures / 0 skipped`
- Gradle `test`: 성공
- Gradle `jar`: 성공
- 산출물: `HyunseoRPG-0.1.0-SNAPSHOT-alchemy-potion-give-canonical-hotfix.jar`
- SHA-256: `4D8AD45B3D48589245F00C89E119B14B3E29D0C417BD26D1E09CBDFA3B0DCB37`

## 운영 확인 필요

실서버에서 새 JAR 적용 후 다음을 확인해야 합니다.

1. `/rpg give <player> potion_vampire 1`
2. 지급 아이템을 `/rpg alchemy inspect <player>`로 검사
3. `potion_id=potion_vampire`, `data-version=1` 확인
4. 해당 포션을 직접 섭취
5. `EffectService`를 통한 흡혈 효과 적용 확인
6. `potion_test_speed` 또는 존재하지 않는 `potion_unknown` 일반 지급이 거부되는지 확인

이번 수정은 최종 포션 지속시간·강도·경제 수치를 변경하지 않습니다.
