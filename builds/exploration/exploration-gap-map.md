# 탐험 구현·콘텐츠 누락 대조표

기준일: 2026-08-19  
기준 브랜치: `codex/exploration-structure-runtime`  
기준점: `codex/exploration-latest-sync`의 탐험 코어 편입 HEAD  
목적: 패키지/설계/현재 소스가 갖춘 범위와 실제 구현·콘텐츠·라이브 검증에서 남은 범위를 분리한다.

## 판정 규칙

- `IMPLEMENTED`: 소스와 정적 검증으로 존재를 확인했지만, 실제 서버 동작을 의미하지 않는다.
- `TEMPLATE`: 공통 엔진 또는 hook은 있으나 실제 구조물 콘텐츠/수치가 비어 있다.
- `CONFIG_DISABLED`: 의도적인 안전 기본값이다. 누락으로 세지 않지만 활성화 전 검증이 필요하다.
- `LIVE_PENDING`: Paper 서버에서 실제 결과를 아직 확인하지 않았다.
- `CONTENT_MISSING`: 설계에는 있으나 실제 이벤트·보상·몹·퍼즐 콘텐츠가 없다.
- `SEPARATE_SCOPE`: 탐험 패키지에 섞지 않기로 확정한 범위다.

## 전체 대조표

| 영역 | 설계/패키지 계약 | 현재 소스·리소스 | 상태 | 확인 방법 | 다음 단위 |
|---|---|---|---|---|---|
| 탐험 상태 | VANILLA → UNDISCOVERED → ACTIVE → CLEARED/ABANDONED, 월드 공용 1회성 | 상태 모델·repository·runtime manager 존재 | IMPLEMENTED / LIVE_PENDING | 재부팅·reload·chunk reload 후 재추첨/재활성화 여부 | 구조물 런타임 |
| 구조물 Registry | Minecraft structure key와 RPG 여부/variant 결정 | Registry, selector, record 존재 | IMPLEMENTED / LIVE_PENDING | 실제 Paper 구조물 API key와 YAML 매핑 비교 | 구조물 런타임 |
| 구조물 감지 | 다중 청크 bounds에서 구조물 판정 | StructureIndex 및 scan adapter 골격 | TEMPLATE / LIVE_PENDING | 생성 직후, chunk load, 재접속, 멀티월드에서 감지 | 구조물 런타임 |
| 선택 확률 | 구조물별 selection-chance, 최초 판정 고정 | YAML 정의 존재, 현재 0.0 | CONFIG_DISABLED | 개발 월드에서 한시적으로 확률 활성화 후 결과/재부팅 비교 | 구조물 런타임 |
| 접근 활성화 | trigger radius 접근 시에만 runtime 생성 | heartbeat/runtime manager 존재 | IMPLEMENTED / LIVE_PENDING | 접근 전 객체 0개 → 접근 후 runtime 1개 확인 | 구조물 런타임 |
| 이탈 처리 | abandon radius + grace 이후 ABANDONED | movement/runtime 경로 존재 | IMPLEMENTED / LIVE_PENDING | 물리 이동과 시스템 teleport를 각각 시험 | 구조물 런타임 |
| 영속성 | 월드 공용 기록, atomic 저장, 재부팅 복원 | YAML storage/repository 존재 | IMPLEMENTED / LIVE_PENDING | 서버 재시작·plugin disable·파일 손상 경계 확인 | 구조물 런타임 |
| 공통 이벤트 엔진 | component 조합, phase/context, clear/abandon hook | component API와 최소 구현 존재 | IMPLEMENTED / LIVE_PENDING | 실제 이벤트 component를 한 구조물에 연결 | 이벤트 런타임 |
| 구조물 이벤트 | 구조물마다 최소 1개 실제 이벤트 | swamp hut witch prototype 템플릿만 존재 | CONTENT_MISSING | 실제 objective 생성·완료·실패 결과 확인 | 이벤트 런타임 |
| 몹 생성 | mob adapter 및 spawn component | 기존 MobService adapter 연결 | TEMPLATE / LIVE_PENDING | 실제 mob-id, 위치, 수량, 중복 생성, cleanup 확인 | 이벤트 런타임 |
| 보상 | reward hook, RPG item/inventory port | hook/adapter 경계만 존재 | TEMPLATE / CONTENT_MISSING | 실제 item id와 give/drop, 중복 수령, 사망/이탈 처리 확인 | 이벤트 런타임 |
| 퍼즐/상호작용 | Puzzle Chest, Timed Bomb, Quiz Gate, Key Match, Greed Counter 등 | 일반 Puzzle hook만 있음 | CONTENT_MISSING | 콘텐츠별 interaction/event 구현 후 테스트 | 콘텐츠 확장 |
| cleanup | clear/abandon/disable/world unload 정리 | runtime stop/flush 경계 존재 | IMPLEMENTED / LIVE_PENDING | 몹·임시 객체·레코드·예약 task 잔존 여부 확인 | 구조물 런타임 |
| 명령/debug | inspect/status/force/clear 및 상태 전이 로그 | 최소 reload 연결, 탐험 전용 운영 검증 명령 미확정 | CONTENT_MISSING | 관리자 명령과 상태 스냅샷 출력 확인 | 관측성 |
| 리로드 | exploration 설정 reload | reload callback 등록 | IMPLEMENTED / LIVE_PENDING | 활성 실행 중 reload 시 상태/확률 불변 확인 | 구조물 런타임 |
| 기존 시스템 연결 | farming/alchemy/territory와 소유권 분리 | 직접 결합 없이 adapter 경계 유지 | IMPLEMENTED | 의존성/패키지 방화벽 검사 | 유지 |
| 영지 T1-T9 | 별도 영지 엔진 | 탐험 패키지에 포함하지 않음 | SEPARATE_SCOPE | 탐험 branch에서 구현하지 않음 | 별도 단위 |
| 운영 활성화 | 안전 기본값 유지 | top-level enabled=false, selection chance=0 | CONFIG_DISABLED | 개발 월드 전용 설정으로만 활성화 | 라이브 게이트 |

## 패키지에는 있으나 실제 콘텐츠가 빠진 항목

다음은 “엔진이 존재한다”와 “플레이어가 실제로 할 수 있는 콘텐츠가 있다”를 구분하기 위한 목록이다.

- 구조물별 실제 이벤트 정의
- 구조물별 clear condition과 실패/abandon 결과
- 실제 커스텀 mob ID, spawn 위치, 수량, 중복 방지
- 실제 RPG item ID, 보상 수량, give/drop 정책
- 보상 중복 수령 방지와 완료 기록 연계
- Puzzle Chest
- Timed Bomb
- Quiz Gate
- Key Match
- Greed Counter
- Environment Modifier
- 구조물별 Display/Interaction/사운드/파티클
- 운영용 inspect/status/force/clear 명령
- 상태 전이 및 종료 이유 로그
- 실제 Paper 구조물 scan API와 서버 버전별 예외 처리

## 구조물 런타임 단위의 구현 순서

1. Paper 구조물 API adapter가 실제 structure key와 bounds를 얻는지 확인한다.
2. YAML 정의와 API 결과를 매핑하고, `SCANNED → RPG/vanilla decision`을 한 번만 저장한다.
3. 개발 월드에서만 selection chance를 임시 활성화한다.
4. 접근 전에는 Display·몹·task·이벤트 객체를 생성하지 않는지 확인한다.
5. 접근 시 runtime을 만들고, 플레이어/구조물/variant를 정확히 연결한다.
6. 재접속·chunk reload·reload·disable·world unload 후 같은 기록을 복원/정리한다.
7. 통과 후에만 실제 구조물 이벤트와 보상 콘텐츠를 별도 커밋으로 추가한다.

## 라이브 테스트에서 반드시 수집할 결과

| 테스트 | 기대 결과 | 실패 시 조사 위치 |
|---|---|---|
| 구조물 생성 직후 이동 | 아직 RPG runtime 없음 | scan 시점/청크 경계 |
| 구조물 주변 진입 | runtime 1개, 중복 생성 없음 | trigger/activation |
| 진입 후 재접속 | 동일 record와 동일 variant | persistence |
| chunk unload/load | record 재추첨 없음 | index/storage |
| 정상 이탈 | grace 후 ABANDONED, 임시 객체 정리 | abandon/cleanup |
| 관리자 teleport | abandon 타이머에 영향 없음 | teleport exemption |
| clear | 효과/몹/임시 객체 정리, 보상 1회 | completion/reward |
| plugin disable | 실행 중 상태 flush, task/entity 정리 | lifecycle |
| reload | 실행 중 record/variant 불변 | reload boundary |
| 확률 표본 확인 | 설정한 확률과 결과를 별도 기록 | selector/config |
| 실제 Paper 구조물 key | YAML key와 일치 | API adapter |
| 개발 월드 외 | 운영 기본값에서 자동 활성화 없음 | config gate |

## 현재 결론

- 탐험 엔진의 공통 골격은 소스에 편입되어 있다.
- 구조물 엔진의 Registry/선택/영속성/runtime 골격도 존재한다.
- 이벤트 엔진은 component API와 prototype hook 수준이다.
- 실제 구조물 이벤트·보상·몹·퍼즐 콘텐츠는 아직 완료가 아니다.
- 확률이 YAML에 존재해도 현재 selection chance가 0.0이므로 “RPG 구조물 등록 및 실제 생성”은 정적 확인만으로 완료 처리할 수 없다.
- Paper live test 전까지 최종 상태는 `LIVE_SERVER_VERIFICATION_REQUIRED`다.
