# 양조 U7-U9 통합 보고서

## 상태

| Unit | 상태 | 설계/코드 검증 |
|---|---|---|
| U7 바닐라 촉매 | 구조 구현, production disabled | PASS |
| U8 특수 촉매 | bounded execution 구현, production disabled | PASS |
| U9 GUI transaction | session/이벤트/원자 조정기 구현, GUI disabled | 구조 PASS, LIVE PENDING |

## 실제 변경 파일

- `ConfigService.java`
- `ConfigDoctor.java`
- `ConfigMigrationService.java`
- `HyunseoRPGPlugin.java`
- `alchemy/catalysts.yml`
- `alchemy/gui.yml`
- `alchemy/catalyst/*`
- `alchemy/gui/*`
- `AlchemyU7U9ContractTest.java`

## 자동 검증

- 전체 Gradle test 성공
- U7 설정 disabled 및 catalyst contract 검증
- U8 중복·한도·월드 취소 검증
- U9 session 교체 및 위험 click 차단 검증

## 설계와 코드의 차이

U7의 실제 splash/lingering 투사체와 U8의 sculk/echo/slime/wind Paper adapter,
U9의 실제 인벤토리 입력·출력·재료 차감은 아직 실서버에서 검증하지 않았다.
따라서 해당 항목은 구현 완료나 production 활성화로 보고하지 않는다.

## 다음 LIVE TEST

1. 비활성 catalyst 변환 거부
2. PDC 없는 이름 복사 potion 거부
3. GUI의 모든 우회 입력 차단
4. full inventory에서 입력 보존
5. close/logout/shutdown 중 입력 복구
6. output delivery 실패 recovery
7. reload/restart 후 Registry·session·job 중복 없음

