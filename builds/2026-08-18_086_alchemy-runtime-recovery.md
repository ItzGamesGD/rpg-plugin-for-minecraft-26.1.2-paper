# HyunseoRPG Alchemy Runtime Recovery / Completion Audit

## Build

- JAR: `2026-08-18_086_alchemy-runtime-recovery.jar`
- SHA-256: `5579E4413720D8A1A5E780682B7BE1BC191968DBBC12A26D5F309346BC4FFD2D`
- Gradle `clean build`: 성공
- Automated tests: 152개 통과
- Server deployment: 하지 않음. `builds/`에만 보관

## 기존 누락 및 회귀

- 기본 포션의 선택적 transformation PDC 누락 처리는 기존 수정 상태를 유지
- 기본 포션이 변환 delivery로 오인되어 음용되는 경로를 차단
- 변환 포션의 splash 대상은 고정 반경 재검색이 아니라 `PotionSplashEvent` 대상 목록을 사용
- lingering 포션이 1회 지연 적용으로 끝나던 경로를 실제 `AreaEffectCloudApplyEvent` 기반으로 변경
- 특수 촉매가 단순 scheduler만 존재하고 실제 runtime 결과가 없던 부분을 bounded execution으로 연결
- 스컬크는 엔티티 직접 전파가 아니라 cloud 생성 및 cloud 연쇄 전파로 변경
- 에코는 즉시 1회 적용 후 설정된 지연 뒤 동일 효과 1회만 추가 적용
- 슬라임 물리 bounce는 구현하지 않고 비활성 보류
- `fireball` 커스텀 아이템을 촉매로 인식하는 전용 투사체·범위 효과 전달 경로 추가

## U5-U11 재분류

| 구간 | 상태 | 근거 |
|---|---|---|
| U5 기본 포션 runtime | IMPLEMENTED / LIVE_SERVER_UNVERIFIED | optional PDC 기본값, consume-to-EffectService 연결, 변조 검증 유지 |
| U7 vanilla catalyst | IMPLEMENTED / LIVE_SERVER_UNVERIFIED | redstone duration, glowstone amplifier, gunpowder splash, dragon breath lingering, fermented spider eye 허용 조합 |
| U8 sculk | IMPLEMENTED / LIVE_SERVER_UNVERIFIED | execution ID, visited 대상, bounded cloud 생성, 거리·개수 제한, cleanup |
| U8 echo | IMPLEMENTED / LIVE_SERVER_UNVERIFIED | 즉시 1회 + 지연 1회 후 종료 |
| U8 fireball | IMPLEMENTED / LIVE_SERVER_UNVERIFIED | custom `fireball` PDC 식별, 실제 SmallFireball, 무피해 폭발 연출, 범위 EffectService 적용 |
| U8 slime | DEFERRED_COMPLEX_RUNTIME | 물리 bounce·충돌별 반복 폭발은 비활성화 |
| U9 catalyst GUI | IMPLEMENTED / LIVE_SERVER_UNVERIFIED | 기존 atomic preflight·consume·give·rollback 구조 유지 |
| U10 lifecycle | IMPLEMENTED / LIVE_SERVER_UNVERIFIED | quit, death, world/chunk unload, plugin disable/reload cleanup 연결 |
| U11 balance/test contract | AUTOMATED_VERIFIED / LIVE_SERVER_UNVERIFIED | YAML 및 서비스 계약 테스트 통과, 수치 최종 확정은 보류 |

## 수정 파일

- `src/main/java/com/hyunseo/hyunseorpg/alchemy/potion/PaperPotionUseService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/potion/PaperPotionUseListener.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/BoundedSpecialCatalystExecutionService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/SpecialCatalystDefinition.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/YamlSpecialCatalystRegistry.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/CatalystDefinition.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/CatalystRegistry.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/YamlCatalystRegistry.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/CatalystApplicationService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/gui/AlchemyCatalystGuiService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectSourceType.java`
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
- `src/main/resources/alchemy/catalysts.yml`
- U7-U11 contract tests: slime 보류 및 fireball 계약 반영

## 설정 변경

- `sculk.max-total-distance: 64.0` 추가
- `slime.enabled: false`
- `fireball` 특수 촉매 추가
- `fireball.item-id: fireball`로 canonical RPG item 식별
- splash radius 등 수치는 `BALANCE_PENDING` 상태로 유지

## 라이브 테스트 필요 항목

1. 기본 포션 음용 시 실제 상태효과 적용 및 1개 소비
2. 변환 PDC가 없는 기본 포션이 `INVALID_PDC`가 되지 않는지 확인
3. 화약 변환 포션이 실제 투척되고 대상에게만 효과가 적용되는지 확인
4. 용의 숨결 변환 포션의 cloud가 지속되는 동안 반복 적용되는지 확인
5. 레드스톤 지속시간 증가와 발광석 강도 증가 비교
6. 발효된 거미눈 허용 조합 및 비허용 조합 거부
7. 스컬크 cloud가 대상 위치에 새 cloud를 만들고 같은 대상을 재사용하지 않는지 확인
8. 에코가 즉시 1회와 지연 1회만 실행하는지 확인
9. `fireball` 촉매가 실제 SmallFireball을 발사하고 블록 파괴·화재·피해 없이 효과만 전달하는지 확인
10. GUI 닫기, 로그아웃, reload 중 입력 복구 및 active job cleanup

## 상태

- `AUTOMATED_IMPLEMENTATION_COMPLETE`
- `LIVE_SERVER_VERIFICATION_REQUIRED`
- `BALANCE_PENDING`
- `Slime catalyst physical bounce runtime: DEFERRED_COMPLEX_RUNTIME`
