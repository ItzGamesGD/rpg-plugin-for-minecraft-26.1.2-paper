# 「천 개의 눈」 프로토타입 수동 검증

이 프로토타입은 던전/스폰 테이블에 등록되지 않는다. OP 권한으로 다음 명령을 사용한다.

```text
/rpgtest thousand-eyes spawn [seed]
/rpgtest thousand-eyes central-laser
/rpgtest thousand-eyes gateway-burst
/rpgtest thousand-eyes scatter-lasers
/rpgtest thousand-eyes path-dash
/rpgtest thousand-eyes remove
```

`spawn`의 기본 seed는 `1000`이다. 스킬 실행 중 다른 스킬 요청은 거절된다.

## 수동 체크리스트

- 중앙 ENDER_EYE가 충분히 입체적으로 보이고 clipping하지 않는지 확인한다.
- inner slab이 수직이며, 공전 중 넓은 면이 중앙을 향하는지 확인한다.
- central laser charge에서 inner ring이 감속하며 로컬 forward 쪽으로 이동해 눈을 덮는지 확인한다.
- release에서 눈이 급히 열리고 sonic boom과 일치하며 정상 속도로 감쇠하는지 확인한다.
- outer layer가 더 바깥에서 돌다가 발사 직전 수축/가속하고 다시 팽창하는지 확인한다.
- END_GATEWAY BlockDisplay가 실제 client에서 별빛 block-entity 외형을 렌더링하는지 확인한다. 확인 전 상태는 **NOT VISUALLY VERIFIED**이다.
- gateway가 10 tick 간격으로 8개 나타나고 마지막 생성 구간 뒤 20 tick 대기와 10 tick 전조 후 피해 범위와 함께 사라지는지 확인한다.
- 기존 satellite 9개가 scatter 위치로 이동하고 snapshot 조준을 유지한 채 시간차 사격 후 복귀하는지 확인한다.
- path marker가 10 tick 간격으로 1~9번 기록되고 움직이는 플레이어를 따라가지 않는지 확인한다.
- marker 원형 위험지역이 작게 유지되고, 본체가 각 10 tick 동안 반드시 1→9 순서로 이동하며 이동 방향을 바라보는지 확인한다.
- death, despawn/target 이탈, world unload, 강제 remove, plugin disable 후 display와 task가 남지 않는지 확인한다.

Persistent display는 중앙 ItemDisplay 1, inner BlockDisplay 8, satellite ItemDisplay 9, outer BlockDisplay 5로 총 23개다. Gateway 중에는 temporary BlockDisplay가 최대 8개 추가되어 총 31개다.
