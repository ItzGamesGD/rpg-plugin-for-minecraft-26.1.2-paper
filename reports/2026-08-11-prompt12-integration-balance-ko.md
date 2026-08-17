# HyunseoRPG 농사 Prompt 12 통합 검증 및 임시 밸런스 보고서

- 기준일: 2026-08-11
- 범위: 농사 Prompt 0~11 회귀 검증, 코드·설계 정합성 점검, 임시 밸런스 측정 모델
- 상태: `SOURCE_DEFAULT_UPDATED` 아님, `PLAYTEST_VERIFIED` 아님
- 운영 적용: 기본 리소스 기준 검증만 완료. 외부 서버 YML 및 실서버 플레이는 별도 확인 필요

## 1. 결론

현재 소스는 다음 핵심 구조를 유지한다.

`직접 수확 -> HarvestService -> AbundancePointService -> PlayerRPGData`

`납품 -> DeliveryService -> DeliveryDataService -> AbundancePointService -> PlayerRPGData`

간접 파괴는 포인트 지급 경로에 들어가지 않으며, 풍요의 정수는 아이템 재료가 없는 포인트 전용 레시피로 유지된다. 괭이 강화는 내구도 방지·판매 보너스, 괭이 승급은 품질 분포·직접 수확 포인트 배율로 분리되어 있다.

이번 단계에서는 이 구조를 변경하지 않고 회귀 계약 테스트를 추가했다. 납품 포인트, 성장 시간, 판매 가격은 현재 임시값이므로 최종 밸런스 승인값이 아니다.

## 2. 코드·설계 대조

| 항목 | 코드 경로 | 설계 판정 |
|---|---|---|
| 직접 수확 | `HarvestService.handleDirect()`에서 성숙·유효 수확 후 포인트 지급 | 일치 |
| 직접 수확 중복 | 작물 좌표·원인 기반 처리 잠금 후 제거 | 일치, 실서버 중복 이벤트는 추가 확인 필요 |
| 간접 파괴 | `handleIndirect()`는 작물 제거와 제한적 일반 드롭만 처리 | 포인트·씨앗·진행도 미지급 원칙과 일치 |
| 품질 | `CropQualityService.roll()` 1회, 승급 shift는 분포에만 적용 | 일치 |
| 괭이 강화 | `hoe_enhancement.yml`의 내구도 방지·품질 판매 보너스 | 일치 |
| 괭이 승급 | `hoe_promotion.yml`의 고정 tier/star passives | 일치 |
| 납품 | `DeliveryRewardCalculator`가 품질·호감도·괭이 계수를 계산하고 `DeliveryDataService`가 저장 | 구조 일치, 현재 보상 점수는 0 임시값 |
| 풍요의 정수 | `required-abundance-points`만 요구하고 `inputs` 없음 | 일치 |
| 저장 | `PlayerRPGData` 및 기존 PlayerDataService 사용 | 별도 농사 파일 생성 없음 |
| 표시 | `CoinDisplayTask`가 코인과 풍요 포인트를 함께 표시 | 임시 표시 정책과 일치 |

주요 근거 파일:

- `src/main/java/com/hyunseo/hyunseorpg/farming/HarvestService.java:129`
- `src/main/java/com/hyunseo/hyunseorpg/farming/HarvestService.java:211`
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropQualityService.java:74`
- `src/main/java/com/hyunseo/hyunseorpg/farming/CropQualityService.java:106`
- `src/main/java/com/hyunseo/hyunseorpg/farming/DeliveryRewardCalculator.java:27`
- `src/main/java/com/hyunseo/hyunseorpg/farming/DeliveryDataService.java:53`
- `src/main/java/com/hyunseo/hyunseorpg/economy/CoinDisplayTask.java:47`

## 3. 추가한 자동 검증

추가 파일:

`src/test/java/com/hyunseo/hyunseorpg/farming/FarmingStage12IntegrationContractTest.java`

검증 내용:

- 품질 5등급 분포가 100%이고 음수가 아님
- 승급 tier별 품질 density shift 후에도 합계가 100%임
- advanced/supreme 기대 비율이 승급 단계에 따라 감소하지 않음
- 강화 설정에 품질 분포·풍요 포인트·희귀 씨앗 필드가 없음
- 승급 설정에 강화 판매 보너스 필드가 없음
- 직접 수확 포인트는 양수이고 물·피스톤·폭발은 0임
- 납품 점수·풍요의 정수·성장 시간·상점 입력값이 현재 계약과 일치함
- 풍요의 정수는 `inputs` 없이 포인트만 요구함

## 4. 품질 분포 이론값

현재 기본 분포는 `일반 55 / 초급 25 / 중급 12 / 고급 6 / 최고급 2`이다. 승급 보정은 각 등급 ordinal에 shift를 더한 뒤 다시 100%로 정규화한다. 아래 값은 난수 실측 결과가 아니라 현재 코드 공식의 기대값이다.

| 괭이 tier | shift | 일반 | 초급 | 중급 | 고급 | 최고급 |
|---:|---:|---:|---:|---:|---:|---:|
| 0 | 0.0 | 55.000% | 25.000% | 12.000% | 6.000% | 2.000% |
| 1 | 0.4 | 52.885% | 24.423% | 12.308% | 6.923% | 3.462% |
| 2 | 0.8 | 50.926% | 23.889% | 12.593% | 7.778% | 4.815% |
| 3 | 1.2 | 49.107% | 23.393% | 12.857% | 8.571% | 6.071% |
| 4 | 1.6 | 47.414% | 22.931% | 13.103% | 9.310% | 7.241% |
| 5 | 2.0 | 45.833% | 22.500% | 13.333% | 10.000% | 8.333% |

주의: 실제 수확 표본, 괭이 star별 분포, 외부 YML 값은 아직 실서버에서 검증하지 않았다.

## 5. 임시 밸런스 입력과 계산 모델

| 항목 | 현재 기본값 | 계산 또는 의미 | 상태 |
|---|---:|---|---|
| 성장 단계 | 4 | 작물별 4단계 | 임시 |
| 단계당 성장 시간 | 60초 | 1회 완전 성장 이론상 240초 | 임시 |
| 직접 수확 포인트 | 1 | 기본 수확 1회당 1점 | 임시 |
| 풍요의 정수 요구량 | 100점 | 직접 수확만 가정하면 기본 100회 | 임시 |
| 납품 기본 포인트 | 0점 | 현재 납품 보상은 잠정 비활성 | 미확정 |
| 납품 갱신 | 1200초 | 20분 | 구조값, 실서버 확인 필요 |
| favor | 비활성 | 배율 1.0, 상한 0 | 미확정 |
| 간접 일반 드롭 | 10% | 품질·씨앗·진행도·포인트 없음 | 임시 |
| 일반 작물 판매 | 1코인 | 네 작물 기본값 | 임시 |
| 품질 작물 판매 | 2~5코인 | 초급~최고급 | 임시 |

이론상 한 개의 재배 칸만 사용하고 설치·수확·이동 시간을 제외하면:

- 풍요의 정수 1개: `100회 × 240초 = 24,000초`, 약 400분
- 시간당 수확 횟수: `3,600 / 240 = 15회`
- 품질 판매 기대값: `55%×1 + 25%×2 + 12%×3 + 6%×4 + 2%×5 = 1.85코인`
- 한 칸의 이론상 시간당 판매액: `15 × 1.85 = 27.75코인`

위 값은 여러 칸 병렬 재배, 씨앗 재공급, 이동, 인벤토리 정리, 괭이 보너스, 수리비를 반영하지 않는다. 따라서 최종 수입이나 인플레이션 결론으로 사용하지 않는다.

## 6. 납품 기대값

현재 `farmer_crops`, `farmer_processed`, `alchemist_processed`는 모두 요구량 1개이며 `preview-base-points: 0`이다. 따라서 현재 기본 리소스 기준 기대 풍요 포인트는 다음과 같다.

| 의뢰 | 요구량 | 기본점수 | 현재 기대 포인트 | 상태 |
|---|---:|---:|---:|---|
| 농부 작물 | 1 | 0 | 0 | balance-pending |
| 농부 가공품 | 1 | 0 | 0 | balance-pending |
| 연금술사 가공품 | 1 | 0 | 0 | balance-pending |

실제 납품 완료는 `DeliveryDataService.completeWithRewards()`를 통해서만 저장되어야 하며, 품질·호감도·괭이 배율은 해당 서비스가 계산한 최종값을 사용한다. 현재 값으로는 납품을 풍요의 정수 수급원으로 평가할 수 없다.

## 7. 플레이 루프 폐쇄·미검증 항목

다음은 코드 구조상 연결 지점은 있으나 실제 플레이 흐름이 닫혔는지 확인이 필요한 항목이다.

| 항목 | 현재 상태 | 실서버 확인 |
|---|---|---|
| 씨앗 구매 | 상점 정의·잠금 구조 존재 | 외부 shops.yml과 해금 전 구매 차단 확인 필요 |
| 직접 심기 | CropIndex·해금 검사 존재 | 네 작물별 씨앗 차감·반환 확인 필요 |
| 성장 | 스케줄러·청크 복원 존재 | 실제 4단계 시간과 재접속 확인 필요 |
| 직접 수확 | 작물·씨앗·품질·포인트 경로 존재 | 빈 인벤토리, 꽉 찬 인벤토리, 중복 이벤트 확인 필요 |
| 판매 | 작물·등급 작물·가공품 상점 등록 | 실제 판매 금액·괭이 보너스 확인 필요 |
| 가공 | 기존 제작 Registry 경로 | 20개 등급 레시피 전체 클릭·shift 제작 확인 필요 |
| 요리 | 설정/구조는 있으나 비활성 | 활성화하지 않음 |
| 납품 | GUI·상태·제출·완료 구조 존재 | 완료 후 정확한 수량 차감·재생성 확인 필요 |
| 풍요 포인트 확인 | 코인 action bar와 Farming Hub/상태 명령 | 접속 직후·재접속 후 표시 확인 필요 |
| 풍요의 정수 | 농사 탭의 포인트 전용 제작 구조 | 전문 단계·100점 차감·결과 지급 확인 필요 |
| 양조 연결 | usage contract/bridge만 존재 | 실제 양조 소비처는 미구현 |

## 8. 금지·미구현 항목 확인

이번 단계에서 다음은 구현하거나 활성화하지 않았다.

- 요리 섭취 효과·요리 경제
- 요리 납품
- 실제 호감도 수치
- 희귀 씨앗 보상
- 영지/NPC 연동
- 범위 수확·자동 수집
- 양조 효과 본체
- 증표 효과 본체
- 원소·종결 장비 연결
- 최종 경제 수치 확정

## 9. 자동 테스트 결과

- `./gradlew.bat test`: PASS
- 총 테스트: 117
- 실패: 0
- 오류: 0
- 스킵: 0
- 새 Stage 12 계약 테스트: PASS

빌드 다운로드 문제로 최초 실행은 네트워크 권한 부족으로 실패했으나, 권한 승인 후 재실행하여 통과했다.

## 10. LIVE TEST REQUIRED

1. 네 작물 심기·성장·수확을 각각 수행
2. 직접 수확 1회당 풍요 포인트가 정확히 1회만 증가하는지 확인
3. 물·피스톤·폭발·경작지 파괴에서 포인트가 0인지 확인
4. 품질 표본 1,000회 이상을 tier 0~5에서 기록
5. 괭이 강화 레벨별 판매 보너스와 내구도 방지 확인
6. 괭이 승급 tier/star별 품질·풍요 포인트 보정 확인
7. 납품 아이템 요구 수량만 차감되고 초과분이 반환되는지 확인
8. 납품 완료·만료·재접속·서버 재시작 후 상태 전이 확인
9. `/rpg reload farming` 후 캐시와 GUI가 최신 설정을 사용하는지 확인
10. 외부 YML 기준 `doctor farming` 및 `migrate farming --dry-run/apply` 확인
11. 풍요의 정수 제작 시 포인트 차감·결과 지급·실패 롤백 확인
12. action bar 풍요 포인트 표시와 기존 코인 표시 동시 확인

## 11. 최종 밸런스에서 재계산할 값

- 작물별 실제 성장 시간과 병렬 재배 수
- 작물별 기본 수확량·씨앗 순증가량
- 품질별 실제 분포와 괭이 tier/star 보정
- 직접 수확 포인트
- 납품별 요구량·기본 포인트·품질·호감도 배율
- 풍요의 정수 요구 포인트
- 작물·품질·가공품 판매가
- 수리비와 괭이 내구도 손실
- 이동·탐색·인벤토리 정리 시간
- 서버 자동화 및 간접 파괴 악용 가능성

## 12. 산출물

- 추가 테스트: `src/test/java/com/hyunseo/hyunseorpg/farming/FarmingStage12IntegrationContractTest.java`
- 본 보고서: `reports/2026-08-11-prompt12-integration-balance-ko.md`
- 빌드: `builds/HyunseoRPG-0.1.0-SNAPSHOT-prompt12-integration-balance.jar`
- SHA-256: `B1D5E765A728F9A1F8EC3A67E4E5CBED51812234E313A31B7B32B50C5B5BED56`
