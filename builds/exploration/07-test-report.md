# E7 전 단계 — 통합 테스트 보고

기준 브랜치: `codex/exploration-latest-sync`  
상태: `LIVE_SERVER_VERIFICATION_REQUIRED`

## 현재 확인

- 탐험 패키지 소스·리소스·단위 테스트 편입: 완료
- 기존 서비스 signature 대조: 완료
- Plugin bootstrap/reload/disable 연결: 완료
- 탐험 module 안전 기본값 유지: 완료
- 실제 Paper 서버 동작 검증: 미실행
- JDK 25 `./gradlew clean test`: 이 작업 단계에서는 아직 실행하지 않음

## 필수 다음 검증

1. JDK 25에서 clean build와 전체 test 실행
2. module disabled 상태의 서버 부팅
3. 개발 월드에서 prototype만 강제 활성화
4. 구조물 감지 후 UUID/variant/state의 reload·재부팅 불변성 확인
5. 접근 전 entity/display가 생성되지 않는지 확인
6. ACTIVE 진입, CLEARED 종료, 물리적 ABANDONED, 시스템 teleport exemption, cleanup 확인
7. Paper 종료 시 persistence flush 확인

정적 소스 편입은 live 동작의 증거가 아니다. 실제 서버 검증 전에는 운영 월드에서 활성화하지 않는다.
