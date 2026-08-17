# HyunseoRPG 구형 스킬 감사

기준일: 2026-08-01

## 결론

구형 스킬 패키지를 일괄 삭제하지 않았다. 현재 `SkillService`가 기존 스킬 실행기와 커스텀 인챈트의 공통 실행 경로로 사용되고 있고, `SwordmasterBladeService`는 `blade_chain`의 칼날 투척·칼날 사출 연계를 실제로 제공한다. 삭제하면 현재 인챈트의 실행 경로가 끊기므로 이번 작업에서는 분류와 경계 정리만 수행한다.

## 분류

| 분류 | 대상 | 현재 호출 경로 | 처리 |
|---|---|---|---|
| A 특수/공통 실행 기반 | `skill/SkillService.java` | `EquipmentEffectTriggerEngine`가 인챈트 executor를 전달하고 `SkillService.executeEnchantment`가 기존 실행기를 호출 | 유지. 커스텀 인챈트 호환 계층이다. |
| A 특수/공통 실행 기반 | `skill/swordmaster/SwordmasterBladeService.java` | `EquipmentEnchantContentService`가 `castBladeThrow`, `castBladeLaunch` 호출 | 유지. `blade_chain`의 기존 소드마스터 동작을 담당한다. |
| A 특수/공통 실행 기반 | `skill/bowmaster/BowmasterSkillService.java` | `SkillService` 및 일부 인챈트 executor가 호출 | 유지. 레이저 화살, 화살비, 화염 화살 등 공통 구현이 남아 있다. |
| A 특수/공통 실행 기반 | `skill/lancer/LancerSkillService.java` | `SkillService`의 기존 `spear_throw`, `charge`, `spear_breakthrough` 경로 | 유지하되 인챈트 Registry에서 retired ID와 혼동하지 않도록 별도 감사 대상이다. |
| B 커스텀 인챈트로 이전 | `enchants.yml`의 `blade_chain`, `light_greatsword`, `laser_arrow`, `axe_heavy_strike` 등 | `EnchantRegistry` → `EquipmentEffectTriggerEngine` → handler | 유지. 장비에 부여되는 현재 시스템이다. |
| C 이미 대체/비활성 | `spear_charge`, `spear_throw`, `lancer`, `paladins_blessing`, `holy_counter`, `homing_arrow`, `hunters_mark`의 retired enchant ID | `EnchantRegistry`에서 retired 목록으로 제외, 마이그레이션에서 구형 책도 별도 처리 | 새 인챈트 후보에 포함하지 않는다. 기존 클래스까지 삭제하지 않는다. |
| D 삭제 후보 | 사용되지 않는 독립 GUI·중복 Registry | 이번 전수 검색에서 현재 실행 경로가 남아 있어 확정 삭제 대상 없음 | `UNRESOLVED`로 보류. 다음 입력 충돌 검증 뒤 제거한다. |

## 입력 충돌 주의점

- 기존 `SkillInputListener`와 `EquipmentEffectTriggerListener`가 입력 이벤트를 함께 관찰한다.
- `EquipmentEffectTriggerEngine`에는 인챈트 처리 후 기존 SkillService로 다시 fall-through하지 않도록 하는 경계가 있다.
- 따라서 구형 스킬 클래스를 먼저 삭제하는 방식은 안전하지 않다.
- 현재 실제 정리 기준은 “등록되지 않은 retired enchant ID는 실행하지 않음”이며, 공통 실행기와 소드마스터·보우마스터·랜서 유틸리티는 유지한다.

## 다음 제거 조건

다음 항목을 모두 확인한 뒤에만 클래스 삭제를 검토한다.

1. `skills.yml`의 활성 스킬이 커스텀 인챈트 또는 특수 장비로 완전히 이전됨.
2. `SkillService.executeEnchantment`에서 해당 executor가 더 이상 참조되지 않음.
3. `EquipmentEnchantContentService`의 직접 호출이 제거되거나 새 공통 Handler로 이전됨.
4. 플레이어 데이터의 skill level·cooldown·input binding 마이그레이션이 완료됨.
5. 동일 입력이 두 번 발동하지 않는 자동 테스트가 통과함.

현재 상태: **삭제 보류 / 기능 공백 없음 확인 전까지 유지**
