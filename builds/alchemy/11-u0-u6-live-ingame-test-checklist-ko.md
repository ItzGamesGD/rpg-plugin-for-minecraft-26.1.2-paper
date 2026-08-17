# HyunseoRPG 양조 U0~U6 통합 인게임 테스트 목록

기준일: 2026-08-15  
대상 빌드: `builds/alchemy/HyunseoRPG-0.1.0-SNAPSHOT-alchemy-u3-effect-debug-hotfix.jar`  
목적: U0~U6 구조와 최신 U3 디버그 핫픽스의 실제 Paper 서버 검증

## 0. 테스트 전 준비

- [ ] 테스트 서버를 운영 서버와 분리
- [ ] 기존 `plugins/HyunseoRPG/` 전체 백업
- [ ] `items.yml`, `effects.yml`, `alchemy/*.yml`, 플레이어 YAML 백업
- [ ] 테스트 계정 2개 준비: 관리자 A, 일반 플레이어 B
- [ ] 관리자 A에 `hyunseorpg.admin` 권한 부여
- [ ] 일반 플레이어 B에는 관리자 권한을 부여하지 않음
- [ ] 서버 콘솔 로그를 별도 저장
- [ ] 테스트 시작 전 `/rpg doctor alchemy` 실행 및 결과 기록
- [ ] 테스트 시작 전 `/rpg reload all` 실행 및 결과 기록

## 1. 명령어·권한·자동완성

| 번호 | 실행 | 기대 결과 | 결과 |
|---|---|---|---|
| 1-1 | 관리자 A: `/rpg effect` | 사용법 또는 상태효과 메뉴 출력 | [ ] |
| 1-2 | 관리자 A: `/rpg effect list` | 현재 활성 효과 목록 출력 | [ ] |
| 1-3 | 관리자 A: `/rpg effect apply` | 사용법 출력, 적용 없음 | [ ] |
| 1-4 | 일반 B: `/rpg effect list` | 권한 부족으로 거부 | [ ] |
| 1-5 | 관리자 A: `/rpg effect apply` Tab | 플레이어 이름, 활성 effect ID, duration, amplifier 자동완성 | [ ] |
| 1-6 | 관리자 A: `/rpg effect remove` Tab | 플레이어와 활성 effect ID 자동완성 | [ ] |
| 1-7 | 일반 B: `/rpg effect apply ...` | 효과 적용·설정 변경·메시지 우회 모두 불가 | [ ] |

## 2. Custom Effect Debug Harness

현재 기본 활성 효과는 `effect_test_speed`이다. 생산 effect는 handler 미구현 및 수치 보류로 비활성 상태이며 아래 2-8에서 fail-closed를 확인한다.

| 번호 | 실행 | 기대 결과 | 결과 |
|---|---|---|---|
| 2-1 | `/rpg effect apply B effect_test_speed 1200 1` | B에게 효과 적용 메시지 출력 | [ ] |
| 2-2 | B의 Attribute/이동속도 확인 | 설정된 테스트 효과가 실제 적용 | [ ] |
| 2-3 | `/rpg effect list` | effect ID, 출처 COMMAND, 활성 상태 확인 | [ ] |
| 2-4 | `/rpg effect remove B effect_test_speed` | 효과 제거 및 Attribute 원복 | [ ] |
| 2-5 | 다시 apply 후 `/rpg effect clear B` | 모든 활성 효과 제거 | [ ] |
| 2-6 | duration `1`로 적용 후 만료 대기 | 만료 직후 효과와 Attribute 제거 | [ ] |
| 2-7 | duration `0`, `72001`, amplifier `-1`, `11` 입력 | 적용 거부, active state 변화 없음 | [ ] |
| 2-8 | `effect_vampire`, `effect_berserk`, `effect_corrosion`, `effect_frostbite`, `effect_shock`, `effect_bleed`, `effect_vulnerability`, `effect_necrosis` 적용 | 비활성 ID로 거부, 효과·PDC·설정 변경 없음 | [ ] |
| 2-9 | 존재하지 않는 ID 적용 | fail-closed, 서버 오류·부분 적용 없음 | [ ] |
| 2-10 | 적용 후 사망·재접속·월드 이동 | 기본 비영속 정책에 따라 제거 | [ ] |

## 3. Effect reload와 lifecycle

| 번호 | 절차 | 기대 결과 | 결과 |
|---|---|---|---|
| 3-1 | effect 적용 후 `/rpg reload effects` | 성공 시 기존 active effect 정리 | [ ] |
| 3-2 | 잘못된 `effects.yml`로 reload | 실패 상세 출력, 이전 정상 snapshot 유지 | [ ] |
| 3-3 | reload 중 Attribute 확인 | 다른 플러그인 modifier 삭제 없음 | [ ] |
| 3-4 | 서버 정상 종료·재시작 | 비영속 effect 복원 없음, 서버 활성화 유지 | [ ] |
| 3-5 | 효과 적용 후 kick/quit/respawn/world change | 정책에 따른 즉시 정리 | [ ] |
| 3-6 | 효과 적용·제거 반복 20회 | modifier 누적·중복 없음 | [ ] |

## 4. U3 전투·스킬 효과 게이트 상태

현재 기본 설정에는 `silence`, `root`, `stun` 활성 정의가 없다. 따라서 다음 항목은 지금 PASS/FAIL로 판정하지 않고 보류한다.

- [ ] `silence` 활성 정의 배포 후 스킬 입력 1회 차단
- [ ] `stun` 활성 정의 배포 후 스킬 입력·공격 차단
- [ ] `root`/`stun` 활성 정의 배포 후 이동·점프·슬라임·경사면 판정
- [ ] 차단 상태에서도 일반 이동·채팅의 의도된 동작
- [ ] 피격과 공격 판정 분리
- [ ] 효과 만료 즉시 입력·이동·공격 복구
- [ ] 다른 플러그인의 damage cancel 및 우선순위 충돌

상태: `EFFECT_DEFINITION_REQUIRED`  
현재 단계에서 활성화하지 않은 효과를 테스트했다고 보고하지 않는다.

## 5. U4 풍요 포인트·정수 연계

| 번호 | 절차 | 기대 결과 | 결과 |
|---|---|---|---|
| 5-1 | 정상 직접 수확 1회 | 기존 농사 규칙에 따라 포인트 변화 확인 | [ ] |
| 5-2 | 동일 수확 이벤트 중복 유도 | 포인트 1회만 증가 | [ ] |
| 5-3 | 물·피스톤·폭발로 작물 제거 | 포인트 증가 없음 | [ ] |
| 5-4 | 납품 완료 | Delivery 경로를 통해 포인트 지급 여부 확인 | [ ] |
| 5-5 | 판매·가공·양조 | 포인트 직접 지급 없음 | [ ] |
| 5-6 | 코인 액션바 확인 | 포인트가 임시 표시되는지 확인 | [ ] |
| 5-7 | 포인트 100 이상 보유 후 풍요의 정수 레시피 진입 | 농사 탭의 정수 메뉴에서 접근 | [ ] |
| 5-8 | 포인트 부족 상태에서 제작 클릭 | 포인트·재료 차감 없이 거부 | [ ] |
| 5-9 | 포인트 충분 상태에서 제작 | `abundance_essence` 지급, 포인트 원자 차감 | [ ] |
| 5-10 | 인벤토리 가득 찬 상태 제작 | 결과가 삭제되지 않고 PendingReward로 보관 | [ ] |
| 5-11 | 재접속·재시작 후 포인트 확인 | 기존 PlayerRPGData와 포인트 유지 | [ ] |

## 6. U5 Potion PDC·Registry·사용 경로

현재 production potion은 비활성 상태이므로 활성 포션 제작 성공을 전제로 하지 않는다.

| 번호 | 절차 | 기대 결과 | 결과 |
|---|---|---|---|
| 6-1 | 포션 ID 없는 일반 POTION 사용 | `NOT_A_POTION` 또는 동등 거부 | [ ] |
| 6-2 | 잘못된 potion ID PDC ItemStack 사용 | `UNKNOWN_POTION`, 효과 적용 없음 | [ ] |
| 6-3 | 잘못된 data version 사용 | `UNSUPPORTED_DATA_VERSION`, 효과 적용 없음 | [ ] |
| 6-4 | 비활성 `potion_vampire` 사용 | `DISABLED_POTION`, 효과 적용 없음 | [ ] |
| 6-5 | PDC에 potion ID·version·catalyst 기록 후 재접속 | PDC 값 유지 | [ ] |
| 6-6 | vanilla 제작대·vanilla brewing·hopper 우회 시도 | 커스텀 포션 생성·효과 적용 없음 | [ ] |
| 6-7 | `/rpg give potion_vampire` | 아이템 ID 존재 여부 확인. 비활성 효과 자동 적용은 없음 | [ ] |
| 6-8 | 활성 포션 정의가 준비된 빌드에서 사용 | `Potion → EffectService.apply()` 공통 경로 확인 | [ ] |

## 7. U6 레시피·재료 Registry

| 번호 | 절차 | 기대 결과 | 결과 |
|---|---|---|---|
| 7-1 | `/rpg give corrosion_essence 1` | 부식의 정수 지급 | [ ] |
| 7-2 | `/rpg give processed_garlic_concentrate_normal 1` | 기존 농사 가공품 지급 및 양조 재료 호환 확인 | [ ] |
| 7-3 | `/rpg give processed_chili_extract_normal 1` | 기존 고추 가공품 지급 및 양조 재료 호환 확인 | [ ] |
| 7-4 | `/rpg reload all` | 양조 파일 로드 실패 없이 기존 Registry 유지 | [ ] |
| 7-5 | `/rpg doctor alchemy` | effects/potions/recipes/abundance 파일과 ID 진단 | [ ] |
| 7-6 | `vanilla:SPIDER_EYE`, `vanilla:SLIME_BALL` 등 실제 재료 준비 | symbolic custom item으로 대체되지 않음 | [ ] |
| 7-7 | 잘못된 `vanilla:NOT_A_MATERIAL`을 임시 설정에 입력 후 reload | 레시피 후보 거부, 기존 정상 snapshot 유지 | [ ] |
| 7-8 | `result-potion-id`가 레시피 ID와 다른 테스트 레시피로 reload | 결과 ID를 `result-potion-id` 기준으로 처리 | [ ] |
| 7-9 | 생산 recipe의 `enabled: false` 상태 확인 | GUI·제작·포션 지급 경로에 노출되지 않음 | [ ] |
| 7-10 | 서버 재시작 후 필수 ID 확인 | items/alchemy 설정과 Registry 상태 유지 | [ ] |

## 8. reload·migration 운영 안전성

| 번호 | 절차 | 기대 결과 | 결과 |
|---|---|---|---|
| 8-1 | 외부 설정 백업 후 `/rpg migrate alchemy --dry-run` | 파일 변경·백업 생성 없음 | [ ] |
| 8-2 | dry-run 결과 확인 | 누락 파일·필수 ID·변경 예정 사항 출력 | [ ] |
| 8-3 | `/rpg migrate alchemy --apply` | 백업 후 필요한 기본 항목만 병합 | [ ] |
| 8-4 | apply 후 기존 운영값 비교 | 기존 값·플레이어 데이터 덮어쓰기 없음 | [ ] |
| 8-5 | 같은 apply 재실행 | 추가 변경 없음 | [ ] |
| 8-6 | apply 후 `/rpg reload all` | migration을 재실행하지 않고 Registry만 재로드 | [ ] |
| 8-7 | 잘못된 YAML reload | 마지막 정상 snapshot 유지, 플러그인 비활성화 없음 | [ ] |
| 8-8 | 서버 재시작 | 외부 YAML 기준으로 동일한 Registry 재생성 | [ ] |

## 9. 중복·보안·보존 확인

- [ ] 일반 플레이어가 effect debug 명령을 사용할 수 없음
- [ ] 비활성 effect 적용 실패 시 active state가 생성되지 않음
- [ ] debug duration/amplifier가 YAML·PDC·플레이어 데이터에 기록되지 않음
- [ ] effect 적용 실패 시 Attribute modifier가 남지 않음
- [ ] effect 제거 시 다른 플러그인 modifier가 삭제되지 않음
- [ ] 동일 effect 재적용 시 stack policy 외 중복 instance가 생성되지 않음
- [ ] reload 실패 시 기존 정상 effect Registry가 유지됨
- [ ] 양조 재료 지급·제작 실패 시 아이템 복제·소실 없음
- [ ] 포인트 제작 실패 시 포인트와 아이템이 동시에 차감되지 않음
- [ ] 기존 농사·강화·승급·인챈트 PDC가 변경되지 않음

## 10. 판정 규칙

### 즉시 PASS 가능 항목

- 명령어 권한·자동완성
- 활성 `effect_test_speed` 직접 적용·제거
- 잘못된 effect 입력 fail-closed
- 생산 Item ID 존재
- 비활성 생산 recipe/potion 차단
- 명시적 vanilla Material 검증
- dry-run 파일 무변경

### 실제 콘텐츠 활성 후 재검증할 항목

- production effect handler별 직접 적용
- 포션 제작 및 실제 사용
- `silence/root/stun` 입력·이동·공격 차단
- 피해·치유 수치와 PvP/Boss 정책
- catalyst 실제 소비와 전파
- GUI 원자적 제작·출력·중복 클릭

### 실패 시 기록할 정보

- 실행한 명령과 전체 인자
- 플레이어 UUID
- effect/potion/recipe ID
- 실행 전후 active effect 목록
- 실행 전후 PDC 키·수량·포인트
- 콘솔 오류 전문
- 외부 YAML 변경 여부
- 사용한 JAR 경로와 SHA-256

## 11. 현재 누적 미검증 요약

- Paper 서버에서 Attribute modifier가 실제로 적용·복구되는지
- `effect_test_speed` 만료·lifecycle 정리
- 생산 effect handler 실제 구현 및 각 ID 직접 적용
- 활성 포션 ItemStack 생성·사용·소비
- vanilla brewing/hopper 우회 차단
- 풍요 포인트·정수의 실서버 원자성
- 양조 GUI/제작 transaction 실동작
- 외부 운영 YAML 마이그레이션 후 재시작 유지

현재 테스트 목록은 구현되지 않은 효과를 PASS 처리하지 않으며, 해당 기능이 활성화된 빌드가 배포된 뒤 같은 번호를 재사용해 회귀 검증한다.
