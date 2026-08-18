# HyunseoRPG 2026-08-18_091

- 빌드 결과: 성공 (`clean test jar`)
- JAR: `2026-08-18_091_alchemy-shock-slime-sculk-runtime.jar`
- SHA-256: `821ADF584044C9E2911B16AAAA27ED00DD06B1423610CD795F975DBF2A1440F0`
- 브랜치: `alchemy-error-detected`
- 구현 상태: COMPLETE
- 자동 검증: 157개 테스트 통과
- 실제 서버 플레이 검증: 미검증 (`LIVE_SERVER_VERIFICATION_REQUIRED`)

## 주요 수정

- 감전 이동 제한을 매 틱 teleport/velocity 고정 방식에서 `PlayerMoveEvent` 기반 20틱 root로 변경했다.
- 감전 중 위치 좌표만 되돌리고 yaw/pitch는 보존한다. 효과 제거, 사망, 로그아웃, 월드 변경, reload/종료 시 상태를 정리한다.
- 슬라임 촉매에 `BOUNCE -> FINAL_SPLASH -> FINISHED` 상태 전이를 추가했다.
- 최대 bounce 수에 도달한 투사체는 다음 충돌에서 실제 `PotionSplashEvent` 영향 엔티티를 1회 처리하고 종료한다.
- 스컬크 확산을 10틱 세대 지연의 단일 대상 체인으로 변경했다.
- 스컬크의 탐색 반경과 시각 반경을 분리했다. 기본값은 탐색 `8.0`, 시각 `1.25`다.
- 스컬크 시각 효과를 설정 가능한 어두운 청록색 RGB `10,70,80` DUST로 변경했다.
- 외부 `alchemy/catalysts.yml`의 legacy `SCULK` 값을 특수 촉매 로드 시 `SCULK_CATALYST`로 안전하게 저장 마이그레이션한다.
- `/rpg alchemy inspect catalysts` 진단을 추가했다. 정상 출력 예: `catalyst=sculk loaded-material=SCULK_CATALYST`.
- 이전 effect command, potion PDC, echo, fireball, vanilla catalyst 수정은 유지했다.

## 설정 변경

`alchemy/catalysts.yml`의 `sculk`에 다음 키를 추가했다.

```yaml
radius: 8.0
visual-radius: 1.25
propagation-delay-ticks: 10
visual-color:
  red: 10
  green: 70
  blue: 80
```

`radius`는 propagation search radius이며 `visual-radius`는 AreaEffectCloud 표시 크기다.

## 수정 파일

- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectMovementLockService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/BoundedSpecialCatalystExecutionService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/CatalystRuntimePhase.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/SpecialCatalystDefinition.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/catalyst/YamlSpecialCatalystRegistry.java`
- `src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigService.java`
- `src/main/java/com/hyunseo/hyunseorpg/command/RPGGiveCommand.java`
- `src/main/resources/alchemy/catalysts.yml`
- 관련 alchemy 계약 및 상태 전이 테스트

## 미검증 테스트

- 라이브 서버에서 감전 T+60 피해/root 시작, T+80 해제, T+120 재발동
- 감전 중 이동 차단과 yaw/pitch 회전 허용
- 슬라임 마지막 충돌의 정상 splash 연출 및 중복 효과 방지
- 스컬크 외부 설정이 실제 파일에서 `SCULK_CATALYST`로 저장되는지
- `/rpg alchemy inspect catalysts`의 실제 출력
- 스컬크 확산이 10틱 간격으로 한 세대씩 보이는지
- 기존 echo/fireball/potion 사용 회귀

## 서버 적용

- 서버 `plugins/` 폴더에는 적용하지 않았다.
- 기존 JAR은 덮어쓰지 않았다.
