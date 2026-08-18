# 포션 PDC 복구 및 상태효과 목록 GUI

## 변경 내용

- canonical 포션 아이템이 선택적 양조 메타를 잃었을 때 원본 포션으로 복구
- 기존 포션 ID와 아이템 ID가 일치하면 현재 데이터 버전으로 보정
- 손상되거나 오래된 촉매 메타가 원본 포션 사용을 막지 않도록 보정
- `/rpg effect list`를 채팅 목록 대신 읽기 전용 GUI로 변경
- GUI에 효과 이름, ID, 남은 시간, 스택, 출처 표시
- GUI 아이템 이동·드래그 차단

## 수정 파일

- `src/main/java/com/hyunseo/hyunseorpg/alchemy/potion/PaperPotionUseService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectService.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectListGuiHolder.java`
- `src/main/java/com/hyunseo/hyunseorpg/alchemy/EffectListGuiService.java`
- `src/main/java/com/hyunseo/hyunseorpg/command/RPGGiveCommand.java`
- `src/main/java/com/hyunseo/hyunseorpg/HyunseoRPGPlugin.java`

## 테스트

- `gradlew.bat clean test`: 성공
- `gradlew.bat clean build`: 성공
- 컴파일 경고: 기존 Paper API deprecated 경고 28건
- 실제 서버 플레이 테스트: 미검증

## 빌드

- JAR: `2026-08-18_083_potion-pdc-effect-list.jar`
- SHA-256: `505A9676F5C123F3DF8B508418DA8EEC2A37EE8BB040BE9ECC2FA9D262F92FB8`

## 미검증 테스트

- 제작된 8종 포션 소비
- `/rpg alchemy give` 지급 포션 소비
- 기존에 만들어진 포션의 PDC 복구
- 촉매 변환 포션 사용
- `/rpg effect list` GUI 표시와 남은 시간 갱신
- 효과 중복·만료·재접속·사망 처리
