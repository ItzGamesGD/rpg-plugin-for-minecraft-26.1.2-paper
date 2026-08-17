# 농사 납품 제한시간·재생성 대기시간 분리 Hotfix

기준일: 2026-08-11

## 1. 확인된 구조 문제

기존 `DeliveryGenerator`가 의뢰 생성 시 `refresh-seconds`를 사용해
`expiresAt`을 계산했다. 완료·만료 후 다음 의뢰 생성 시점도 같은 `refresh-seconds`를
사용하고 있어 다음 두 의미가 하나의 설정에 섞여 있었다.

- ACTIVE 의뢰가 제출 가능한 제한시간
- COMPLETED/EXPIRED 이후 다음 의뢰 재생성 대기시간

## 2. 수정된 설정 계약

`farming/deliveries.yml`:

```yaml
refresh-seconds: 1200
time-limit-seconds: 1200
```

- `time-limit-seconds`: ACTIVE 생성 시각부터 만료까지의 시간
- `refresh-seconds`: 완료 또는 만료 처리 시각부터 다음 ACTIVE 생성까지의 대기시간

초기 기본값은 기존 동작과의 호환을 위해 둘 다 1200초로 유지했다.
구체적인 밸런스 수치는 후속 밸런스 단계에서 변경한다.

## 3. 실제 호출 경로

```text
DeliveryRegistry.load()
  ├─ time-limit-seconds 로드
  └─ refresh-seconds 로드

DeliveryGenerator.generate()
  └─ expiresAt = createdAt + time-limit-seconds

DeliveryDataService.complete/expire()
  └─ statusAt = 완료/만료 시각 저장

DeliveryService.getOrCreate()
  ├─ ACTIVE: expiresAt까지 유지
  ├─ ACTIVE 만료: EXPIRED 전환
  └─ COMPLETED/EXPIRED: statusAt + refresh-seconds 이후 새 의뢰 생성
```

기존 `statusAt` 필드가 완료·만료 시각을 이미 저장하고 있으므로 플레이어 데이터 구조를
추가하거나 기존 의뢰를 변환할 필요가 없다.

## 4. 변경 파일

- `src/main/resources/farming/deliveries.yml`
- `src/main/java/com/hyunseo/hyunseorpg/farming/DeliveryRegistry.java`
- `src/main/java/com/hyunseo/hyunseorpg/farming/DeliveryGenerator.java`
- `src/main/java/com/hyunseo/hyunseorpg/farming/DeliveryService.java`
- `src/main/java/com/hyunseo/hyunseorpg/core/config/ConfigDoctor.java`
- `src/main/java/com/hyunseo/hyunseorpg/command/RPGGiveCommand.java`
- `src/test/java/com/hyunseo/hyunseorpg/farming/FarmingStage8DeliveryTest.java`
- `src/test/java/com/hyunseo/hyunseorpg/farming/FarmingStage12IntegrationContractTest.java`

`ConfigMigrationService`의 기존 farming root 병합이 새 기본 키를 외부 설정에 추가하므로,
실제 반영은 기존 정책대로 `/rpg migrate farming --apply`에서만 수행된다.

## 5. 풍요 포인트 정책

이번 작업에서는 포인트 수치를 변경하지 않았다.

- 직접 수확: 지급 가능
- 납품: 지급 가능
- 기타 등록 농사 활동: 확장 가능
- 판매·가공·양조: 직접 지급 없음
- 자동화·간접 파괴: 지급 없음

구체적인 지급량과 활동별 배율은 밸런스 단계에서 확정한다.

## 6. 검증 결과

- 전체 테스트: 120개
- 실패: 0개
- 건너뜀: 0개
- 컴파일: 성공
- JAR 생성: 성공
- `libs` 폴더: 수정하지 않음

추가 검증:

- ACTIVE 만료 시각 계산에 `time-limit-seconds` 사용
- 완료·만료 후 대기 계산에 `refresh-seconds` 사용
- 시간 덧셈 overflow 포화 처리
- 기존 statusAt 기반 저장 데이터와 호환
- Doctor가 두 설정 키를 각각 검사

## 7. LIVE TEST REQUIRED

- `time-limit-seconds: 60`, `refresh-seconds: 300`으로 설정
- 의뢰 생성 후 60초 전까지 제출 가능 확인
- 60초 후 EXPIRED 전환 확인
- 만료 후 300초 동안 새 의뢰가 생성되지 않는지 확인
- 300초 후 새 ACTIVE 의뢰 생성 확인
- 의뢰를 즉시 완료해도 완료 시각부터 refresh 대기가 시작되는지 확인
- 재접속·재시작 후 `expiresAt`과 `statusAt` 유지 확인
- `/rpg reload farming` 후 두 설정이 각각 적용되는지 확인

## 8. 산출물

JAR:
`builds/HyunseoRPG-0.1.0-SNAPSHOT-delivery-time-separation-hotfix.jar`

SHA-256:
`E5712C8297C26979EA95C2F16B7BFAD30731B2D780150EAEA1344B1ABB808135`
