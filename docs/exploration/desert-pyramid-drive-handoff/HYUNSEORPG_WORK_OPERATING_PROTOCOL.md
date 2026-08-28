# HYUNSEORPG WORK OPERATING PROTOCOL

Source: Google Drive document `HYUNSEORPG_WORK_OPERATING_PROTOCOL` (`1t_8fWDEy9WFtQNmqh96HjDoREeO46FJjwkkF9XN85Fs`).
Transferred to GitHub for Codex Cloud access on 2026-08-28 UTC.

## Purpose

This is the standing operating protocol for Chat/Work/task delegation in HyunseoRPG development. Apply it whenever a work prompt or task-path delegation is created.

## 1. DRIVE DOCUMENT LIFECYCLE

For each active development project/component, maintain a compact Drive document set:

- `<PROJECT>_STABLE_SPEC`: authoritative stable design and invariants.
- `<PROJECT>_CURRENT_WORK`: current HEAD/baseline, active defects, current scope, tests, completion conditions, and current execution mode.
- `<PROJECT>_ARCHIVE_INDEX`: historical references only; workers must not read archive/history unless CURRENT_WORK explicitly names a specific item.

Every automated work cycle that changes implementation state must update/overwrite the relevant CURRENT_WORK document before handoff. If stable design changes, update STABLE_SPEC as well. Do not force every worker to reread historical documents.

When one project/component is genuinely completed and work moves to a different project/component, delete the completed project's active Drive work-spec documents and create a fresh three-document set for the new project using the same structure. Do not carry stale CURRENT_WORK instructions into the next project. Preserve historical material only when explicitly required; otherwise the active work-spec set should remain minimal.

## 2. EXECUTION MODE TOGGLE

Every generated implementation prompt must contain a short explicit mode header near the top:

`EXECUTION_MODE=MANUAL`

or

`EXECUTION_MODE=AUTOMATION`

This is the primary toggle. Do not infer mode from old status documents.

## 3. MANUAL MODE

When `EXECUTION_MODE=MANUAL`:

- Scheduled/task-based implementation and reviewer loops are disabled for that work scope.
- The active Work session is the sole implementation worker.
- Do not delegate to a task.
- Do not wait for a task or reviewer.
- Do not stop because a status says AWAITING_REVIEW, IMPLEMENTING, VERIFY_REQUIRED, or similar.
- Do not defer reporting while idle.
- Do not enter a send-and-wait state.
- If CI is running, continue useful static audit, test review, connectivity review, or adversarial inspection instead of merely waiting.
- If CI fails, inspect, fix, commit, and rerun in the same work session where possible.
- A progress message is not a completion condition.
- If execution is interrupted or genuinely blocked, immediately report exact HEAD, last completed commit, CI state, completed work, remaining defect queue, and exact blocker/interruption reason.
- Stop normally only when repository-addressable work for the requested scope is complete and the final candidate has the required verification, or when unavailable live/user evidence is genuinely required.

## 4. AUTOMATION MODE

When `EXECUTION_MODE=AUTOMATION`:

- Modifier and reviewer roles may hand work off through the task path.
- Modifier owns production/test changes.
- Reviewer independently audits and should not casually rewrite production code.
- Candidate ready for review may enter AWAITING_REVIEW.
- Reviewer defect → NEEDS_FIX → modifier resumes.
- Reviewer clean + current-head verification → COMPLETE.
- Automated cycles must update CURRENT_WORK with exact HEAD, completed changes, remaining defects, verification status, and next role before handoff.
- Do not use stale boolean locks as proof that a worker is active. Where a control document is used, include owner/run identity, HEAD, heartbeat/update time, and stale-lock handling.
- If the user starts a manual Work session for the same scope, automation must be toggled OFF before manual modification begins.
- Automation resumes only after an explicit ON request or an unambiguous automation-mode instruction.

## 5. PROMPT GENERATION RULE

Whenever Chat generates a Work implementation prompt or task-path delegation for HyunseoRPG:

- include EXECUTION_MODE explicitly;
- point first to the exact CURRENT_WORK document name;
- point to STABLE_SPEC only as its authoritative stable reference;
- prohibit broad archive/history reading unless CURRENT_WORK explicitly names a needed source;
- require Drive CURRENT_WORK update after implementation state changes in AUTOMATION mode;
- preserve MANUAL mode's no-wait/no-defer rule;
- distinguish repository-side completion from LIVE_SERVER_RETEST_REQUIRED.

## 6. CONTEXT/CREDIT EFFICIENCY

Prefer exact document-name retrieval over injecting long historical prompts repeatedly. External storage itself is not free context: workers should read only the minimum authoritative documents needed. CURRENT_WORK should stay compact and be rewritten as state changes rather than endlessly appended.

## 7. PRIORITY

The current user's explicit instruction always overrides this protocol. A project-specific CURRENT_WORK may add stricter rules but should not silently reverse the execution mode. If a conflict exists, the newest explicit user instruction wins.
