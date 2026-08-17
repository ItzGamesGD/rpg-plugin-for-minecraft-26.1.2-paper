# 양조 U8 검증 보고서: 특수 촉매

## 구현

- `SpecialCatalystDefinition`, `YamlSpecialCatalystRegistry` 추가
- `SpecialCatalystExecution` 계약 추가
- `BoundedSpecialCatalystExecutionService` 추가
- 실행 ID·월드 UUID·최대 동시 실행 수·중복 실행 방지·월드/전체 취소 구현
- 플러그인 종료 시 모든 special execution 정리

## 설계 대조

- sculk, echo_shard, slime, wind_charge는 독립 effect ID를 만들지 않고 기존 potion/effect 경계를 재사용한다.
- 실행 작업은 execution ID로 소유한다.
- 월드 취소와 전체 취소가 가능하다.
- 현재 모든 특수 촉매는 disabled다. 실제 전파·반복·투사체 동작을 임의로 활성화하지 않았다.

## 코드 대조

- Registry snapshot은 `ConfigService`의 catalysts 설정에서 생성된다.
- 실행 서비스는 Registry에 없는 ID, disabled ID, 중복 ID, 동시 실행 한도 초과를 거부한다.
- 실제 Bukkit entity/projectile scheduler adapter는 다음 검증 Unit에서 실서버 검증 후 연결해야 한다.

## 검증

- 중복 execution 거부
- 월드 취소 후 active count 0
- 동시 실행 한도 경계 테스트 통과
- 실제 sculk hop, echo 지연, slime bounce, wind projectile은 LIVE TEST REQUIRED

