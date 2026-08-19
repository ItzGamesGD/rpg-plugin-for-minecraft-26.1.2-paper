# E6 exploration status snapshot 및 명령 경계 감사

기준 브랜치: codex/exploration-e5-lifecycle-observability  
기준 커밋: bbaca1963900c11e184cb6e82cb63aa6ec11109e  
작업 브랜치: codex/exploration-e6-status-snapshot

## 이번 단위의 목표

E5에서 메모리 내부에 기록한 종료 사유를 이후 관리자 도구가 안전하게 사용할 수 있도록, persistence와 runtime을 함께 보여주는 불변 status snapshot 계약을 추가했다. 기존 명령어 체계가 확인되지 않은 상태에서 새로운 rpg 명령을 임의로 만들지는 않았다.

## 구현

- ExplorationStatusSnapshot을 추가했다.
- snapshot은 구조물 ID/type/variant, 영속 state, runtime 활성 여부, 참여자 수, objective 수, 마지막 종료 사유를 함께 표현한다.
- 음수 카운터를 거부해 debug 출력이 잘못된 상태를 정상값처럼 보이지 않게 했다.
- ExplorationRuntimeManager.status(UUID)와 statuses()를 추가했다.
- ExplorationModule.status(UUID)를 추가해 상위 plugin bootstrap이 같은 계약을 사용할 수 있게 했다.
- snapshot은 진단용이며 영속 gameplay 상태를 변경하지 않는다.

## E1~E5 충돌/회귀 점검

- E1 registry/persistence 정책은 변경하지 않았다.
- E2 malformed load fail-closed, retryable world load, stale index isolation은 변경하지 않았다.
- E3 trigger/activation policy와 proximity 경계는 변경하지 않았다.
- E4 objective spawn 실패 및 activation rollback은 변경하지 않았다.
- E5 종료 사유 enum과 getter를 그대로 사용하며, snapshot이 별도 종료 사유를 재해석하지 않는다.
- 기존 명령어가 없는 상태에서 명령 라우팅을 추측해 추가하지 않았다.

## 정적 검증

추가 테스트: 2개

- 정상 snapshot 필드 보존
- 음수 participant/objective 카운터 거부

소스 대조 경로:

persistent record → optional runtime → status snapshot → future admin/debug consumer

정적 소스 검증: 완료.  
Gradle/JDK 25 실행: 이 환경에서는 수행하지 않음.  
GitHub Actions: 최종 커밋 연결 결과 확인 필요.  
Paper live server: 수행하지 않음.

## 구현 불가·보류·누락 보고

- OBSERVABILITY_MISSING: 실제 관리자 명령/debug UI는 기존 command infrastructure가 확인되지 않아 구현하지 않았다. 이번 snapshot API가 후속 명령 구현의 입력 계약이다.
- LIVE_SERVER_VERIFICATION_REQUIRED: 실제 Paper에서 활성/비활성 구조물, objective 수, 종료 사유가 expected runtime과 일치하는지 확인해야 한다.
- CONTENT_MISSING: swamp hut 이벤트, 보상, 퍼즐, 연출 및 추가 구조물 variant는 여전히 별도 콘텐츠 작업이다.
- DEFERRED: 최신 데스크톱 소스의 MobService/RPGItemService adapter signature와 실제 Paper structure key/bounds는 라이브 또는 최신 소스 대조가 필요하다.
- YAML 전역 enabled=false, 선택확률 0.0은 유지했다.
- 저장소에서 prompts/ 원문은 확인되지 않았다.

상태: IMPLEMENTED_STATIC_VERIFIED / STATUS_API_READY / OBSERVABILITY_COMMAND_DEFERRED / CONTENT_MISSING_REPORTED / LIVE_SERVER_VERIFICATION_REQUIRED
