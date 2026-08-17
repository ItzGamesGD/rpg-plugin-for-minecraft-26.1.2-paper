# 경제 런타임 검증 기록

## 자동 검증

- `./gradlew test`: 통과
- `./gradlew clean build`: 통과
- 기존 플레이어 데이터 및 PDC: 변경하지 않음
- 외부 YAML 백업: 완료

## 아직 운영에서 확인하지 못한 항목

- 외부 progression-loop.yml의 채광 50, 농사 0, BUILDING 제거
- 외부 shops.yml의 성장 재료 직접 거래 차단
- 외부 crafting.yml의 canonical 마석·승급석·추출권·리롤권 제작식
- 외부 equipment-support.yml의 6000/2000 수수료
- `/rpg reload` 및 서버 재시작 후 Registry 반영
- 실제 활동 중 중복 코인 지급 여부

## 운영 적용 후 확인 순서

1. 외부 YAML 백업 경로 확인
2. `migrate all -apply` 또는 승인된 설정 적용 절차 실행
3. `/rpg doctor all`
4. `/rpg reload`
5. 서버 완전 재시작
6. 채광 1회, 농사 수확 1회, 벌목 1회 보상 확인
7. 상점에서 성장 재료·추출권·리롤권 구매 차단 확인
8. 제작 GUI에서 다섯 canonical 제작식 확인
FINAL STATUS: RUNTIME_APPLIED
External economy YAML application and value verification completed. Reload/restart and live play verification remain pending.
