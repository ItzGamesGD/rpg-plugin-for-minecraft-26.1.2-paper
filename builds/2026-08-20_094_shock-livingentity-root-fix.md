# HyunseoRPG 감전 root 런타임 수정

- 대상 브랜치: alchemy-error-detected
- 작업 상태: 정적 수정 완료
- 실제 Paper 서버 검증: LIVE_UNVERIFIED
- 최종 밸런스: LIVE_BALANCE_UNVERIFIED

## 정적 원인 판정

현재 브랜치에서 EffectMovementLockService는 플러그인 리스너로 등록되어 있었으므로 등록 누락은 아니었다.

확인된 구조적 문제:

1. lock(LivingEntity, durationTicks)가 Player가 아니면 즉시 반환하여 Mob에 root가 전혀 적용되지 않았다.
2. Shock handler가 설정된 root duration을 읽은 뒤에도 Math.min(20L, ...)으로 잘라 설정값을 사실상 하드코딩했다.
3. Player root가 실제 런타임에서 적용되지 않는지는 정적 소스만으로 단정할 수 없으므로, HIGHEST 단계와 MONITOR 단계의 진단 로그를 추가했다.
4. PlayerMoveEvent 후속 리스너가 최종 destination을 덮어쓰는지 MONITOR 로그로 판정할 수 있다.

## 반영 내용

- Shock 첫 pulse timing은 기존처럼 effect 적용 후 interval 대기 구조를 유지했다.
- 임시 baseline을 60틱에서 80틱으로 변경했다.
- root duration은 interval보다 항상 짧게 clamp한다.
- Player는 X/Y/Z를 고정하고 yaw/pitch를 허용한다.
- Mob은 원래 hasAI()를 저장한 뒤 AI를 일시적으로 끄고, 종료·제거 시 원래 값으로 복원한다.
- 같은 entity에 대한 재잠금에서는 원래 AI 상태를 덮어쓰지 않는다.
- death, quit, world change, effect remove, plugin disable 경로에서 cleanup을 유지한다.
- alchemy-runtime-debug.enabled: false 기본값으로 진단 로그를 비활성화했다.
- pure timing policy unit test를 추가했다.

## 라이브 진단 판정 방법

debug를 켠 뒤 감전을 재현한다.

- pulse 로그 없음: CASE A
- pulse 로그는 있으나 lock 로그 없음: CASE B
- lock 로그는 있으나 HIGHEST PlayerMove 로그의 isLocked=false: CASE C
- HIGHEST에서 action=setTo가 있으나 MONITOR 최종 finalTo가 positional: CASE D/E
- MONITOR에서 cancelled=true: 다른 listener가 선행 취소했는지 확인

진단 종료 후 alchemy-runtime-debug.enabled를 다시 false로 둔다. 로그는 최종 운영 설정에서 spam되지 않는다.

## 라이브 체크 순서

1. Player: T+79까지 이동 가능, T+80 피해와 root, T+100 release, T+160 재피해와 재root.
2. Mob: T+80 피해와 AI 정지, T+100 복귀, T+160 재정지.
3. 원래 AI=false Mob: root 종료 후 false 유지.
4. effect remove/death/quit/world change/plugin disable 후 stale root 및 AI 상태 확인.
