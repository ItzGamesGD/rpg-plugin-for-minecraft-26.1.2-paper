# HyunseoRPG 2026-08-18_085

## Build result

- Gradle `clean build`: 성공
- JAR: `2026-08-18_085_effect-list-command.jar`
- SHA-256: `BC93372152C7B26354D2E5516D3B1B0D912A071FA91A5302569AB87D22FFA06F`

## 변경 내용

- `/rpg effect list`가 상태효과 채팅 요약이 아니라 `EffectListGuiService`의 상태효과 GUI를 열도록 연결
- 기본 `/effect` 명령어와의 충돌을 피하기 위해 `/effectlist` 독립 명령 추가
- `/effectlist` 또는 `/effectlist list`로 현재 활성 상태효과 GUI 열기
- 상태효과 GUI는 메인 메뉴에 추가하지 않음
- 상태효과 목록 조회에는 관리자 권한을 요구하지 않음
- 기존 관리자용 상태효과 적용·삭제·리로드 동작은 유지

## 수정 파일

- `src/main/java/com/hyunseo/hyunseorpg/command/EffectCommand.java`
- `src/main/java/com/hyunseo/hyunseorpg/command/RPGGiveCommand.java`
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`
- `src/main/resources/plugin.yml`

## 테스트

- 자동 빌드: 통과
- 실제 서버에서 `/effectlist` GUI 표시: 미검증
- 실제 서버에서 `/rpg effect list` GUI 표시: 미검증
- 활성 효과 이름·설명·남은 시간 표시: 미검증
- 효과가 없을 때 빈 목록 표시: 미검증

## 적용 방법

서버를 완전히 종료한 뒤 기존 플러그인 JAR을 덮어쓰지 않고 이 빌드로 교체하고 재시작한다. `/reload`만으로 적용 완료로 판단하지 않는다.

## 상태

- Implementation status: COMPLETE
- Automated verification: PASSED
- Live gameplay verification: PENDING
- Balance verification: NOT REQUIRED
