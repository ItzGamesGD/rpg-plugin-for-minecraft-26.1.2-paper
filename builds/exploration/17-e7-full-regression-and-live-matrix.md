# E7 전체 구조 회귀 감사 및 라이브 검증 매트릭스

- 기준 브랜치: codex/exploration-e6-status-snapshot
- 기준 커밋: 401faa347d9068af0f80e5f03f731cd268528222
- 작업 브랜치: codex/exploration-e7-full-regression-audit
- 범위: E1~E6 탐험 엔진의 정적 회귀 감사, 설계 누락 대조, 라이브 테스트 목록 확정
- 이번 단위의 코드 변경: 없음
- 서버 JAR 덮어쓰기: 없음
- 최종 판정: LIVE_SERVER_VERIFICATION_REQUIRED

## 1. 이번 단위의 결론

E1~E6까지는 탐험 엔진의 핵심 뼈대와 최소 실행 프로토타입이 연결되어 있다.

정적 기준으로 확인된 범위:

- 구조물 RPG 판정·Registry 경계
- 영속 상태와 YAML 저장 경계
- 접근 반경 기반 activation
- 최소 objective 실행과 CLEAR
- physical exit 기반 ABANDON 및 teleport exemption
- cleanup과 plugin disable lifecycle
- end reason 내부 기록
- 불변 status snapshot API
- 기본 비활성 및 확률 0 안전 설정

그러나 다음은 아직 실제 서버 동작으로 확정되지 않았다.

- Paper 버전의 실제 구조물 생성·감지 이벤트와 구조물 키/경계
- 실제 월드에서의 1회 판정 및 재시작 후 중복 방지
- chunk/world unload 및 reconnect의 경계
- adapter를 통한 실제 MobService/RPGItemService 연동
- 실제 구조물 콘텐츠, 보상, 퍼즐, 관리자 명령
- 성능, 저장 복구, 운영 중 reload
- 최신 데스크톱 소스와의 API 호환성

따라서 이 브랜치는 “정적 구조 회귀 기준선”이지 라이브 통과 브랜치가 아니다.

## 2. 회귀 상태 분류

| 영역 | 현재 상태 | 정적 판단 | 라이브/후속 |
|---|---|---|---|
| 상태 전이 | IMPLEMENTED_STATIC_VERIFIED | UNDISCOVERED → ACTIVE → CLEARED/ABANDONED 경계가 존재 | 실제 전이·재시작 검증 필요 |
| Registry/선택 | IMPLEMENTED_STATIC_VERIFIED + CONFIG_DISABLED | YAML Registry와 0.0 확률 안전값 존재 | 실제 구조물 키·확률 샘플 필요 |
| 구조물 감지 | TEMPLATE | Paper API 연결은 adapter 경계에 있음 | LIVE_SERVER_VERIFICATION_REQUIRED |
| Persistence | IMPLEMENTED_STATIC_VERIFIED | 구조물별 영속 상태, fail-closed/retry 경계 존재 | 파일 복구·재시작·동시성 필요 |
| Trigger/Activation | IMPLEMENTED_STATIC_VERIFIED | trigger radius 안에서 runtime 생성 | 실제 거리·중복 activation 필요 |
| Objective/Clear | IMPLEMENTED_STATIC_VERIFIED | 최소 scripted spawn/objective와 clear 경계 존재 | 실제 entity lifecycle 필요 |
| Abandon | IMPLEMENTED_STATIC_VERIFIED | physical exit만 grace 후 abandon, teleport exemption 분리 | knockback/reconnect/chunk unload 필요 |
| Cleanup/Lifecycle | IMPLEMENTED_STATIC_VERIFIED | tracker 기반 정리와 disable flush 경계 존재 | 실제 서버 잔류 entity/task 확인 필요 |
| Reload | TEMPLATE | definition reload 경계만 존재 | active runtime 영향 확인 필요 |
| Status snapshot | IMPLEMENTED_STATIC_VERIFIED | 불변 snapshot과 counters/end reason 제공 | 명령 UI·실제 값 확인 필요 |
| End reason | TEMPLATE | 내부 last-end reason 기록 존재 | 운영 로그/명령 노출 없음 |
| 구조물 콘텐츠 | CONTENT_MISSING | swamp hut witch prototype template만 있음 | 구조물별 콘텐츠 구현 후 테스트 |
| 보상 | CONTENT_MISSING | reward port 경계만 있음 | 실제 아이템 ID·중복 방지 필요 |
| 퍼즐 컴포넌트 | CONTENT_MISSING | 공통 포트 경계만 있음 | Puzzle Chest 등 개별 구현 필요 |
| 관리자 명령 | CONTENT_MISSING | status/inspect/force/clear/reload 명령 없음 | 운영 검증 차단 |
| 영지/마을 | SEPARATE_SCOPE | 탐험과 별도 시스템으로 분리 | territory unit에서 진행 |
| 양조/연금술 | SEPARATE_SCOPE | 현재 탐험 회귀 범위 아님 | alchemy 최신 브랜치에서 별도 검증 |

## 3. E1~E6 정적 회귀 감사

### E1 — Registry/Recheck

- 구조물 definition을 Registry로 해석하는 경계가 유지되어야 한다.
- 알 수 없는 structure type, variant, objective type은 안전하게 거부되어야 한다.
- 기본 enabled=false 및 selection chance=0 설정은 유지되어야 한다.
- detection adapter가 Registry를 우회해 직접 runtime을 만들지 않는지 확인했다.
- 라이브 미확정: 실제 Paper 구조물 키, 생성 시점, 멀티 청크 경계.

판정: STATIC_BASELINE_OK / LIVE_SERVER_VERIFICATION_REQUIRED

### E2 — Persistence/Index

- 구조물 RPG 판정은 저장 후 재평가하지 않는 계약을 유지한다.
- malformed record는 전체 서버 기동을 중단하지 않고 해당 record를 격리하는 fail-closed 경계를 유지한다.
- world/structure UUID가 섞이지 않도록 index key를 분리한다.
- save failure는 성공한 것처럼 상태를 확정하지 않아야 한다.
- 라이브 미확정: 재시작, 파일 손상, 부분 기록, world unload/load.

판정: STATIC_BASELINE_OK / LIVE_SERVER_VERIFICATION_REQUIRED

### E3 — Trigger/Activation

- 접근 전에는 runtime object와 scripted entity가 없어야 한다.
- trigger 반경 진입 시 activation은 1회만 수행되어야 한다.
- activation failure 시 최초 상태를 복구하고 생성물을 정리해야 한다.
- runtime은 player instance가 아니라 월드 공용 구조물 execution 경계를 따른다.
- 라이브 미확정: 반경 경계, 다른 플레이어 접근, 재접속, chunk reload.

판정: STATIC_BASELINE_OK / LIVE_SERVER_VERIFICATION_REQUIRED

### E4 — Minimal Execution

- objective가 정상 생성되면 runtime이 active가 된다.
- objective가 비어 있거나 생성 실패하면 무한 active가 되지 않아야 한다.
- objective entity ID와 cleanup 등록이 연결되어야 한다.
- clear 시 reward phase와 cleanup 순서가 중복 실행되지 않아야 한다.
- 라이브 미확정: 실제 Bukkit entity invalid/dead/unloaded 처리.

판정: STATIC_BASELINE_OK / LIVE_SERVER_VERIFICATION_REQUIRED

### E5 — Lifecycle/Observability

- 종료 사유를 FINAL_CLEAR, ABANDON_GRACE, ACTIVATION_FAILURE, COMPLETION_FAILURE, ABANDON_FAILURE, STATE_INVALID, PLUGIN_DISABLE로 구분한다.
- plugin disable은 lifecycle cleanup 예외이며 active runtime을 남기지 않아야 한다.
- 관리자/system teleport는 physical abandon으로 오인하지 않아야 한다.
- 내부 상태와 종료 사유 getter는 존재하지만 명령·운영 로그는 아직 없다.
- 라이브 미확정: 실제 disable/world unload/teleport 시 entity와 저장 상태.

판정: STATIC_BASELINE_OK / OBSERVABILITY_MISSING / LIVE_SERVER_VERIFICATION_REQUIRED

### E6 — Status Snapshot

- UUID, structure type, variant, state, runtimeActive, participantCount, objectiveCount, lastEndReason을 불변 snapshot으로 노출한다.
- 음수 counter와 null state/reason을 거부한다.
- manager와 module pass-through 경계가 있다.
- 현재 statuses()는 모든 영속 record를 나열하는 API가 아니라 active runtime 또는 last end reason이 기록된 구조물만 반환한다.
- 따라서 향후 관리자 전체 목록 명령을 만들 때 repository/index 전체 열거 API가 추가로 필요하다.

판정: STATIC_BASELINE_OK / COMMAND_AND_FULL_ENUMERATION_DEFERRED

## 4. 설계상 누락 또는 추가 결정이 필요한 부분

아래는 단순히 라이브에서 확인할 문제가 아니라, 확인 후 정책을 확정해야 하는 설계 공백이다.

1. Event Flow/Sequence 규격
   - trigger, spawn, display, interaction, objective, clear, reward, cleanup의 순서와 실패 시 rollback을 구조물별로 명시해야 한다.
2. Structure detection contract
   - 생성 시점 이벤트 우선인지, chunk-load 보완 감지인지, 실제 Paper API의 key와 bounding box를 어떤 adapter로 읽을지 확정해야 한다.
3. Persistence schema/version
   - schema version, migration, backup, repair, partial write, duplicate UUID 정책이 필요하다.
4. Participant semantics
   - 현재 heartbeat는 같은 world의 반경 내 online player를 기준으로 판단한다. participant 집합만 기준으로 할지, 월드 공용 구조물의 관전자까지 포함할지 결정해야 한다.
5. Reconnect/offline semantics
   - disconnect가 physical exit인지, grace를 정지하는지, 재접속 시 active runtime을 재연결하는지 명시해야 한다.
6. Unload semantics
   - chunk unload, world unload, server stop 중 어느 경우에 PAUSE, ABANDON, PLUGIN_DISABLE을 기록할지 결정해야 한다.
7. Objective invalidation semantics
   - entity death, plugin removal, chunk unload, server restart를 모두 “objective 완료”로 처리하면 안 될 수 있다. 실제 운영 정책을 확정해야 한다.
8. Reload semantics
   - reload가 정의만 갱신하는지, active runtime은 이전 snapshot을 유지하는지, 잘못된 새 설정을 어떻게 격리할지 정해야 한다.
9. Clear/reward atomicity
   - clear 저장, reward 지급, cleanup 사이에 장애가 발생했을 때 중복 보상과 미지급을 어떻게 막을지 필요하다.
10. Status scope
    - E6 snapshot은 active/last-end 중심이다. 전체 persistent state 조회, world별 조회, 구조물별 조회의 API 범위를 정해야 한다.
11. End reason visibility
    - 내부 enum을 관리자 명령, structured log, audit record 중 어디에 노출할지 결정해야 한다.
12. Content definition ownership
    - YAML가 flow를 얼마나 표현하고, Java component가 어떤 정책을 소유할지 정해야 한다. YAML만으로 복잡한 퍼즐을 표현하지 않는다.

## 5. 의도적 보류와 content_missing

### 의도적으로 보류된 항목

- enabled=true로 실제 월드에 자동 노출하는 운영 기본값
- selection chance를 0보다 크게 바꾸는 운영 설정
- 서버 JAR 자동 배포 또는 기존 서버 파일 덮어쓰기
- 실제 Paper 구조물 scan API의 버전 의존 구현
- 최신 데스크톱 소스의 MobService/RPGItemService signature 확정
- 관리자 명령 UI 및 운영 로그 표면
- 영지/마을 T1~T9 시스템
- 양조·농사·연금술·전투의 별도 최신 통합
- 모든 구조물을 한 번에 콘텐츠화하는 작업

### content_missing — 아직 실제 콘텐츠가 없는 항목

- swamp hut의 최종 이벤트 규칙, 명확한 성공/실패/보상 정의
- desert pyramid 퍼즐/트랩 variant
- pillager outpost chest/horn/forced combat/reward flow
- trial chamber key-match event
- ocean monument event/relocation flow
- witch hut custom elite mob 정책과 실제 MobService ID
- structure별 display target, interaction target, sound, particle, warning timing
- Puzzle Chest
- Timed Bomb
- Quiz Gate
- Key Match
- Greed Counter
- Environment Modifier
- Forced Relocation의 구조물별 허용 범위
- Reward Drop Pool의 실제 RPG item ID, 수량, 중복 방지, 지급 시점
- structure별 clear condition과 abandon/failure result
- custom mob의 stats, skill, spawn quantity, duplicate protection
- 운영자용 inspect/status/force/clear/reload 명령과 permission

이 목록은 실패 보고가 아니라 E1~E6의 엔진 단위와 분리된 콘텐츠 구현 대기열이다. 콘텐츠 구현은 라이브 smoke가 끝난 후 별도 브랜치 단위로 진행한다.

## 6. 전체 라이브 테스트 매트릭스

현재 이 목록은 서버에 실제로 배포·실행하지 않았으므로 모두 LIVE_SERVER_VERIFICATION_REQUIRED 또는 CONTENT_MISSING으로 닫는다.

| ID | 테스트 | 기대 결과 | 현재 상태 |
|---|---|---|---|
| L-01 | 기본 설정으로 기동 | 탐험 모듈 비활성, 기존 기능 영향 없음 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-02 | enabled=true + 유효 YAML 기동 | Registry와 heartbeat가 정상 로드 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-03 | 정의 없음/잘못된 정의 기동 | fail-closed, 서버 전체 중단 없음 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-04 | 실제 Paper 구조물 key 확인 | vanilla structure key와 adapter 결과 일치 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-05 | 구조물 생성·감지 | 생성 직후 정확히 1회 판정·저장 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-06 | chunk 경계 구조물 | bounding box 전체가 올바른 구조물로 묶임 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-07 | multi-chunk 구조물 | chunk load 순서와 무관하게 중복 없음 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-08 | 비-RPG 판정 | VANILLA 상태 저장, runtime 없음 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-09 | RPG 확률 샘플 | 임시 개발 확률에서 기대 범위 내 결과 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-10 | 재시작 후 판정 | 기존 결과 재추첨 금지 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-11 | reload 후 판정 | 기존 결과 재추첨 금지, 새 정의만 안전 로드 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-12 | duplicate scan | 같은 structure UUID가 한 record/runtime만 보유 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-13 | 접근 전 상태 | entity/task/runtime 미생성 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-14 | trigger radius 진입 | runtime 1회 생성, objective 정상 등록 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-15 | trigger 경계 | 경계 안/밖의 판정이 일관됨 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-16 | 다른 world 접근 | world isolation 유지 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-17 | objective spawn | 위치·수량·UUID·cleanup 등록 일치 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-18 | objective 정상 완료 | CLEAR 1회, reward/cleanup 중복 없음 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-19 | objective dead/invalid | 정책에 맞는 실패 또는 완료, 무한 active 없음 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-20 | activation failure | 상태 rollback, 생성물·task 정리 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-21 | physical exit | grace 시작, 즉시 ABANDON하지 않음 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-22 | 경계 왕복/knockback | grace가 정책대로 유지·취소됨 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-23 | admin/system teleport | physical exit로 기록하지 않음 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-24 | disconnect/reconnect | reconnect 정책대로 runtime 재연결 또는 grace 처리 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-25 | active chunk unload/load | runtime과 영속 상태가 유실되지 않음 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-26 | world unload | 정의된 end reason과 cleanup 수행 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-27 | plugin disable | task/entity cleanup, 저장 flush, PLUGIN_DISABLE 기록 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-28 | abandon 완료 | ABANDONED 영속, 재접근으로 재활성화되지 않음 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-29 | malformed persistence | 해당 record 격리 또는 복구, 타 world 영향 없음 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-30 | 여러 구조물 동시 실행 | runtime/participant/objective 간 교차 오염 없음 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-31 | status snapshot | state/counter/lastEndReason이 실제 runtime과 일치 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-32 | full persistent status 목록 | active/cleared/abandoned/vanilla 조회 범위가 명확함 | CONTENT_MISSING / API DEFERRED |
| L-33 | 실제 MobService/RPGItemService | 최신 데스크톱 소스의 실제 ID·지급 결과 일치 | CONTENT_MISSING / LIVE_SERVER_VERIFICATION_REQUIRED |
| L-34 | 실제 구조물 콘텐츠 | swamp hut 또는 다음 콘텐츠의 clear/reward/cleanup 일치 | CONTENT_MISSING / LIVE_SERVER_VERIFICATION_REQUIRED |
| L-35 | 관리자 명령 | inspect/status/force/clear/reload 권한·출력 검증 | CONTENT_MISSING |
| L-36 | 성능/반복 실행 | 접근 전 object storm 없음, heartbeat와 저장 비용 허용 범위 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-37 | 확률 통계 | 개발 확률과 실제 반복 샘플 편차 기록 | LIVE_SERVER_VERIFICATION_REQUIRED |
| L-38 | 서버 버전/JAR 산출 | clean build, JAR SHA-256, commit/branch 기록 | LIVE_SERVER_VERIFICATION_REQUIRED |

라이브 실행 순서는 L-01 → L-03 → L-04~L-13 → L-14~L-20 → L-21~L-30 → L-31~L-32 → L-36~L-38이다. L-33~L-35는 실제 콘텐츠와 명령 구현 이후에 실행한다.

## 7. 라이브 테스트 기록 형식

각 테스트에서 다음 값을 남긴다.

- server/Paper build와 Java 버전
- plugin JAR SHA-256
- 탐험 브랜치와 commit SHA
- structures.yml의 enabled, heartbeat, selection chance
- world UUID와 structure UUID
- structure type, variant, state
- runtime active 여부
- participant count, objective count
- last end reason
- activation tick, clear/abandon tick
- 생성 entity UUID와 cleanup 결과
- 저장 파일 변경 전후 checksum
- 재시작/reload/unload 여부
- 콘솔 오류와 stack trace
- 기대 결과/실제 결과/판정
- 재현 절차와 수정 브랜치

## 8. 다음 작업 단위

E7 회귀 감사가 끝난 뒤의 권장 순서:

1. L-01~L-03 안전 기동 smoke
2. L-04~L-13 Paper detection/persistence live adapter 확인
3. L-14~L-30 activation/lifecycle live 확인
4. 오류 발생 시 해당 작업 단위의 최소 코드만 새 fix 브랜치로 분리
5. L-31~L-32 observability/status API 보완
6. 실제 콘텐츠 1개만 선택해 E8 콘텐츠 브랜치로 구현
7. L-33~L-35 콘텐츠·보상·명령 검증
8. L-36~L-38 최종 clean build와 산출물 검증
9. 마지막에 탐험 브랜치들을 필요한 커밋만 선택적으로 최신 통합 브랜치에 반영

## 9. 검증 한계

- 이 커밋에서는 Gradle clean build를 실행하지 않았다.
- GitHub Actions workflow 실행은 이 문서 커밋에 대해 별도로 확인한다.
- 실제 Paper 서버, 월드, 최신 데스크톱 소스, 서버 JAR을 사용하지 않았다.
- 따라서 정적 회귀 통과는 라이브 동작 통과를 의미하지 않는다.
- 본 문서의 CONTENT_MISSING과 LIVE_SERVER_VERIFICATION_REQUIRED를 구현 완료로 표시하면 안 된다.

최종 상태: STATIC_REGRESSION_AUDIT_RECORDED / LIVE_SERVER_VERIFICATION_REQUIRED
