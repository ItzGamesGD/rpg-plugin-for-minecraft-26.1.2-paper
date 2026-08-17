# 수리비 실측 입력 양식

현재 수리비는 실측 전이다. 다음 CSV 열을 기록한다.

`session_id,player,activity,equipment_id,material,enhancement,promotion,duration_minutes,starting_damage,ending_damage,max_durability,coins_before,coins_after,repair_cost,inventory_sort_minutes,travel_minutes,notes`

측정 세션:

- 30분 채광
- 30분 벌목
- 30분 일반 전투
- 자연 몬스터 20회
- 위더 1회
- 엔더 드래곤 1회
- 활·쇠뇌·삼지창 사용
- 방어구 피격

계산: `시간당 내구도 손실 × 내구도 1당 수리비 = 시간당 수리비`. 강화·승급 단계와 활동 순코인을 함께 기록한다. 수치 입력 전에는 수리비를 조정하지 않는다.
