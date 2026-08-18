# 양조 기본 포션 및 촉매 전달 방식 수정

## 원인 수정

- 기본 포션의 선택적 `delivery` PDC가 없을 때 `ORIGINAL`로 처리
- 정상 기본 포션이 빈 문자열을 `PotionDefinition.Delivery.valueOf("")`에 전달해 `INVALID_PDC`가 되던 경로 제거
- 데이터 버전과 등록되지 않은 촉매는 계속 엄격하게 거부

## 촉매 변환 수정

- 화약 변환 결과 Material을 `SPLASH_POTION`으로 동기화
- 드래곤의 숨결 변환 결과 Material을 `LINGERING_POTION`으로 동기화
- RPG item ID와 PDC는 기존 방식대로 유지

## 이벤트 라우팅 수정

- 변환 포션 우클릭 시 플레이어 자신에게 즉시 적용하던 가짜 처리 제거
- `PotionSplashEvent`에서 실제 투척 위치 주변 대상에게 효과 적용
- `LingeringPotionSplashEvent`에서 실제 잔류 포션 위치 주변 대상에게 효과 적용
- `PotionUseService`의 대상 지정 적용 경로 추가

## 검증

- `gradlew.bat clean test`: 성공
- `gradlew.bat clean build`: 성공
- 기존 Paper API deprecated 경고 28건, 컴파일 오류 없음
- 실제 서버 플레이 테스트: 미검증

## 빌드

- JAR: `2026-08-18_084_alchemy-delivery-routing.jar`
- SHA-256: `05D4B828E166EE1C297AF38E88204962D181447122638584C01A3C1100DF7CD7`

## 미검증 항목

- 기본 제작 포션 8종 소비
- 화약 촉매 후 실제 투척 동작
- 드래곤의 숨결 촉매 후 잔류 영역 동작
- 포션 수량 1개 차감
- 자기 자신에게 잘못 적용되지 않는지
- 투척 대상별 효과 적용
- 촉매 중복 적용 차단
- 인벤토리 가득 참·재접속·서버 재시작
