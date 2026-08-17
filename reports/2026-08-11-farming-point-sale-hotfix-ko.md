# 농사 풍요 포인트·괭이 판매 보너스 정합성 Hotfix

기준일: 2026-08-11

## 1. 확인된 문제

### 풍요 포인트 배율

기존 계산은 `configured * hoeMultiplier` 결과를 `Math.round()`로 정수화했다.
기본 포인트가 1이면 1.05배부터 1.25배까지 모두 1점으로 반올림되어
괭이 농사 승급의 풍요 포인트 효과가 실제 지급량에 나타나지 않았다.

### 괭이 판매 보너스

판매 경로는 판매 순간의 `player.getInventory().getItemInMainHand()`를
`ShopService.sell()`에 전달하고, 해당 괭이의 강화 단계로 품질 작물 판매가를 계산한다.
수확 시점 괭이 정보를 작물에 저장하는 구조는 존재하지 않는다.

## 2. 수정 내용

### 소수부 확률 보정

풍요 포인트 정수 지급은 stochastic rounding으로 변경했다.

```text
기본값 × 배율 = 정수부 + 소수부
지급량 = 정수부
소수부 확률에 당첨되면 +1
```

예시:

```text
1 × 1.05 = 1점, 5% 확률로 2점
1 × 1.25 = 1점, 25% 확률로 2점
7 × 2.00 = 항상 14점
```

따라서 승급 배율이 1점 단위에서 무효화되지 않으며,
별도 플레이어 remainder 필드나 아이템 PDC를 추가하지 않는다.

간접 파괴, 미성숙 작물, 무효 수확, 직접 플레이어 수확이 아닌 원인은 기존처럼 0점이다.

### 판매 보너스 정책 명시

현재 구조를 다음 정책으로 확정해 코드 주석과 경계에 명시했다.

```text
품질 작물 보너스 = 판매 시점에 손에 들고 있는 괭이의 강화 효과
```

작물에 수확 당시 괭이 instance를 저장하지 않는다. 따라서 품질 작물은 동일 품질·ID 기준으로
계속 정상 스택되며, 수확 시점 provenance PDC를 다시 도입하지 않는다.

이는 의도된 계정 장비 효과로 처리한 것이다. 수확 당시 괭이 성과에 귀속하는 정책으로 바꾸려면
별도 provenance·스택 정책이 필요하므로 이번 범위에서는 구현하지 않았다.

## 3. 설계 대조

- 직접 수확만 풍요 포인트 지급: 유지
- 괭이 승급 배율: 실제 정수 지급에 반영
- 간접 파괴·자동화 포인트: 미지급 유지
- 품질 작물 PDC·스택 구조: 변경 없음
- 판매·경제 가격 공식: 변경 없음
- 기존 플레이어 데이터·아이템 PDC: 변경 없음

## 4. 변경 파일

- `src/main/java/com/hyunseo/hyunseorpg/farming/HarvestService.java`
- `src/main/java/com/hyunseo/hyunseorpg/equipment/HoeHarvestModifierService.java`
- `src/main/java/com/hyunseo/hyunseorpg/shop/ShopService.java`
- `src/test/java/com/hyunseo/hyunseorpg/farming/FarmingStage12IntegrationContractTest.java`

## 5. 테스트 결과

- 전체 테스트: 119개
- 실패: 0개
- 건너뜀: 0개
- 컴파일: 성공
- JAR 생성: 성공
- `libs` 폴더: 수정하지 않음

추가 검증:

- 1점 × 1.25배, 확률 0.0 → 2점
- 1점 × 1.25배, 확률 0.99 → 1점
- 기존 직접 수확·간접 파괴 판정 회귀 통과
- 7점 × 2.0배 → 14점 유지

## 6. 실서버 확인 필요

- 동일 괭이 승급 단계에서 여러 번 직접 수확하고 평균 풍요 포인트가 기대값에 수렴하는지 확인
- 괭이를 들지 않거나 약한 괭이를 들고 수확한 품질 작물을 보관
- 판매 직전에 강화 괭이를 들었을 때 판매 보너스가 적용되는지 확인
- 판매 시 괭이를 바꾸는 정책이 운영 의도와 일치하는지 최종 확인
- 재접속·재시작 후 풍요 포인트 저장 유지

## 7. 산출물

JAR:
`builds/HyunseoRPG-0.1.0-SNAPSHOT-farming-point-sale-hotfix.jar`

SHA-256:
`DBF522419D28E31165FB552B6D2BE066367BC98812A8BAEFF9F8A071526528A2`
