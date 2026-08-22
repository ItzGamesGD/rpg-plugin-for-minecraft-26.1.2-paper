# 탐험 패키지 최신 설계 동기화

기준일: 2026-08-19  
기준 브랜치: `codex/exploration-latest-sync`  
원본: `codex/exploration-package-v1`의 `HyunseoRPG_exploration_package_v1.zip`  
최신 설계 기준: 3차 영지-탐험 확정안 및 최근 대화 확정사항

## 판정

패키지의 탐험 코어(E1-E6)는 최신 탐험 상태 전이와 일치한다. 다만 패키지는 2026-08-13 설계 기준으로 만들어졌고, 영지 최신 확정안은 문서·코드에 포함되어 있지 않았다. 따라서 이번 브랜치에서 최신 내용을 다음처럼 덮어쓴 것으로 취급한다.

- 탐험과 영지를 서로 다른 엔진·소유권 모델로 유지한다.
- 탐험은 월드 공용 1회성 이벤트다. `VANILLA → UNDISCOVERED → ACTIVE → CLEARED/ABANDONED` 이후 재추첨·기본 재도전은 없다.
- 구조물 콘텐츠는 단순 보상 껍데기가 아니라 구조물마다 실제 이벤트를 하나 이상 가진다.
- E7 순서는 사막 피라미드 → 시련의 회당 → 보루 잔해 → 바다 신전 → 엔드 도시다.
- 영지는 탐험 엔진에 섞지 않는다. 영지 후보 마을, 종/우물 Anchor, 해방 납품, 보호구역, 현장 상점·농사 납품, 선택적 습격은 별도 T1-T9 범위다.
- 영지 해방은 전투가 아니라 실제 아이템 투입이며, 영지 습격의 5코어는 해방이 아닌 Raid 전용이다.
- 영지 습격 실패·미참여는 폐허화·재건 비용·경제적 손실을 만들지 않는다.
- 영지 습격은 서버 전역 최대 1개, 영지별 cooldown, 낮은 발생률을 사용한다.
- 영지 현장 Display/Interaction과 상점 갱신은 실제 소유자 진입·GUI 열기 시점의 lazy 처리로 두며, 영지별 상시 tick 스케줄러를 두지 않는다.
- 농사 납품의 기존 GUI/명령은 현장 인터페이스가 안정화될 때까지 유지하며, `refreshAt`과 납품 대기/제한시간은 별도 상태로 다룬다.
- 영지 보호는 영구적인 월드 변경을 차단하되 문·버튼·레버·영지 Interaction 같은 정상 상호작용은 허용한다.

## 이번 브랜치에서 실제 반영한 범위

- 탐험 패키지 Java 소스 40개와 리소스 2개, 테스트 4개를 실제 `src/main`·`src/test` 트리에 편입했다.
- `HyunseoRPGPlugin`에 탐험 모듈을 단일 객체로 bootstrap했다.
- 기존 `MobService`, `RPGItemService`, `InventoryDeliveryService`에 대한 package adapter를 연결했다.
- `exploration` reload 항목을 기존 `RPGReloadService`에 등록했다.
- 플러그인 disable 시 플레이어 데이터 저장 전에 탐험 persistence flush/cleanup이 수행되도록 연결했다.
- `exploration/structures.yml`의 안전 기본값(enabled=false, selection chance=0)을 유지했다.

## 아직 의도적으로 하지 않은 범위

- 영지 T1-T9 구현
- E7 실제 구조물 이벤트 콘텐츠와 밸런스 수치
- items.yml/mobs.yml/alchemy/farming 내부 수정
- 운영 월드에서 탐험 모듈 활성화
- Paper 서버 live test

이 문서는 패키지 zip을 최신 설계로 재포장했다는 뜻이 아니라, zip의 엔진과 실제 소스 편입본에 적용할 최신 설계 경계를 고정하는 기록이다. zip 바이너리는 원본 보존용으로 유지한다.
