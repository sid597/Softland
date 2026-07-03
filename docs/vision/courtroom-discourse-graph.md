# Vision 4: The Courtroom — Discourse Graph Edition

## ONE-SCREEN VERSION (the real UI)

```
╔══════════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                                                                                          ║
║        C O U R T   O F   C O D E   R E V I E W  ──  C A S E   # 8 4 7                                    ║
║        The People v. auth-per-route                                                                      ║
║                                                                                                          ║
║  ┌── [Q] THE CASE ──────────────────────────────────────────────────────────────────────────────┐        ║
║  │  "Should Softland adopt per-route token validation to replace the global auth policy?"       │        ║
║  │   Filed: 2h ago  ·  Branch: auth-per-route  ·  Defendant: @sid                               │        ║
║  └──────────────────────────────────────────────────────────────────────────────────────────────┘        ║
║                                                                                                          ║
║  Judge: @alex          Counts: 3/5 examined          Confidence: █████████████████░░░░░░░░ 72%           ║
║                                                                                                          ║
╠═══  PROSECUTION [C]  ═══════════════════════════════╦═══ EVIDENCE LOCKER [E] ════════════════════════════╣
║                                                     ║                                                    ║
║  "The existing auth applies a single global         ║  ◆ EXHIBIT A  [E:code]                             ║
║   policy to all routes. This fails three            ║    middleware.clj:47-63                            ║
║   security requirements."                           ║    (defn wrap-per-route-auth                       ║
║                                                     ║      [handler route-schemas]                       ║
║                                                     ║      (fn [req] (case (validate ...)                ║
║  C1 ● "Global policy unsafe"                        ║        :valid   (handler req)                      ║
║       ◆ Ex.A, Ex.B              PROVEN              ║        :expired {:status 401 ...})))               ║
║                                                     ║    ── supports ──▶ C1, C4                          ║
║  C2 ● "No perf regression"                          ║                                                    ║
║       ◆ Ex.C                    PROVEN              ║  ◆ EXHIBIT B  [E:test]                             ║
║                                                     ║    auth_test.clj:109-125                           ║
║  C3 ◐ "All edge cases covered"                      ║    (deftest expired-token-per-route-test           ║
║       ◆ Ex.B  ✗ Obj#1          CHALLENGED           ║      (is (= 401 (:status (handler ...)))))         ║
║                                                     ║    ── supports ──▶ C1, C3, C4                      ║
║  C4 ● "Tests cover core paths"                      ║                                                    ║
║       ◆ Ex.B, Ex.D              PROVEN              ║  ◆ EXHIBIT C  [E:bench]                            ║
║                                                     ║    $ ab -n 1000 -c 50 /api/admin                   ║
║  C5 ○ "Migration is safe"                           ║    p99 latency: 2ms  ·  throughput: 12k rps        ║
║       ◆ (none)                  UNSUBSTANTIATED     ║    ── supports ──▶ C2                              ║
║       ⚠ No exhibit entered                          ║                                                    ║
║                                                     ║  ◆ EXHIBIT D  [E:???]                              ║
║                                                     ║    N O T   S U B M I T T E D                       ║
║                                                     ║    Required to prove C5: "Migration safe"          ║
║                                                     ║                                                    ║
╠═════════════════════════════════════════════════════╬════════════════════════════════════════════════════╣
║                                                     ║                                                    ║
║  WARNINGS TO THE COURT [R]                          ║  ⚡ OBJECTION #1  [F]  ── @alex, 2m ago            ║
║                                                     ║                                                    ║
║  ▲ R1  "Legacy tokens in prod — no migration"       ║  "Count 3 claims all edge cases are covered,       ║
║     opposes C5  ·  blocks Verdict                   ║   but no exhibit demonstrates refresh token        ║
║     Severity: MEDIUM  ·  Court's own motion         ║   handling with mixed OAuth scopes."               ║
║                                                     ║                                                    ║
║  ▲ R2  "Refresh tokens + mixed scopes untested"     ║  ── opposes ──▶ C3                                 ║
║     opposes C3  ·  blocks Verdict                   ║  ── informs ──▶ R2                                 ║
║     Severity: HIGH  ·  From Objection #1            ║                                                    ║
║                                                     ║  [SUSTAIN]    [OVERRULE]    [REPLY]                ║
║                                                     ║                                                    ║
╠═══════════════════════════════════════════════════ ═╩════════════════════════════════════════════════════╣
║  PRIOR ART [D:rejected]                                                                                  ║
║  ✗ "Global whitelist" — rigid, unscalable           ✗ "Per-role chains" — N+1 calls, 3x latency          ║
║  ── both superseded_by ──▶  ✓ "Per-route schema ma p" — O(1) lookup, composable, testable     ADOPTED    ║
╠══════════════════════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                                          ║
║  [D] V E R D I C T                                                                                       ║
║  depends_on:  C1 ✓    C2 ✓    C3 ⚡    C4 ✓    C5  ✗           blocked_by:  R1 ⚠    R2 ⚠                 ║
║                                                                                                          ║
║  ┌─────────────────┐        ┌───────────────────── ─────────┐        ┌──────────────────────┐            ║
║  │   ✓  ACQUIT     │        │   ◐  CONTINUE DELIBE RATION   │        │   ✗  CONVICT         │            ║
║  │   (approve)     │        │      ◀── RECOMMENDED          │        │   (request changes)  │            ║
║  │   LOCKED — 2    │        │                               │        │                      │            ║
║  │   open risks    │        │                               │        │                      │            ║
║  └─────────────────┘        └───────────────────────────────┘        └──────────────────────┘            ║
║                                                                                                          ║
╚══════════════════════════════════════════════════════════════════════════════════════════════════════════╝
```

---

## DETAILED VERSION (scroll, for deep reference)
## Full immersive layout with typed nodes and edges

### Discourse Graph Node Types Used
- `[Q]` Question — "The case before the court"
- `[C]` Claim — "The charges / counts"
- `[E]` Evidence — "Exhibits A, B, C, D"
- `[D]` Decision — "The verdict" + rejected alternatives
- `[R]` Risk — "Warnings to the court"
- `[F]` Feedback — "Objections from the bench"

### Discourse Graph Edge Types Used
- `supports` — Exhibit backs a Count
- `opposes` — Objection/Risk weakens a Count
- `addresses` — Count answers the root Question
- `informs` — Feedback creates/strengthens a Risk
- `depends_on` — Verdict requires Count resolution
- `blocks` — Risk prevents Verdict
- `supersedes` — Chosen approach replaces rejected ones

### The Layout

```
╔════════════════════════════════════════════════════════════════════════════════════════╗
║                                                                                      ║
║          C O U R T   O F   C O D E   R E V I E W  ──  C A S E   # 8 4 7            ║
║          ─────────────────────────────────────────────────────────────────            ║
║          The People v. auth-per-route                                                ║
║                                                                                      ║
║  ┌── [Q] THE CASE BEFORE THE COURT ─────────────────────────────────────────────┐   ║
║  │                                                                              │   ║
║  │   "Should Softland adopt per-route token validation to replace the global    │   ║
║  │    auth policy?"                                                             │   ║
║  │                                                                              │   ║
║  │   Filed: 2h ago    Branch: auth-per-route    Defendant: @sid                 │   ║
║  └──────────────────────────────────────────────────────────────────────────────┘   ║
║                                                                                      ║
║  ┌── THE BENCH ─────────────────────────────────────────────────────────────────┐   ║
║  │   Presiding: Judge @alex                                                     │   ║
║  │                                                                              │   ║
║  │   Counts examined: 3/5     Exhibits reviewed: 4/5     Objections filed: 1    │   ║
║  │                                                                              │   ║
║  │   ┌────────────────────────────────────────────────────────────────────┐      │   ║
║  │   │  CONFIDENCE OF THE COURT                                          │      │   ║
║  │   │  ████████████████████████████████████░░░░░░░░░░░░░░░░░  72%       │      │   ║
║  │   │  ▲ 3 counts proved    ▲ 1 challenged    ▲ 1 unsubstantiated      │      │   ║
║  │   └────────────────────────────────────────────────────────────────────┘      │   ║
║  └──────────────────────────────────────────────────────────────────────────────┘   ║
║                                                                                      ║
║  ┌── PROSECUTION ───────────────────────┐ ┌── EVIDENCE LOCKER ────────────────────┐ ║
║  │                                       │ │                                       │ ║
║  │  OPENING STATEMENT:                   │ │  ◆ EXHIBIT A  [E:code]                │ ║
║  │  "The existing auth system applies a  │ │  ┌───────────────────────────────┐    │ ║
║  │   single global policy to all routes. │ │  │  middleware.clj:47-63         │    │ ║
║  │   This fails three security require-  │ │  │  (defn wrap-per-route-auth    │    │ ║
║  │   ments. I present five counts."      │ │  │    [handler route-schemas]    │    │ ║
║  │                                       │ │  │    (fn [request]              │    │ ║
║  │                                       │ │  │      (let [route (:uri req)   │    │ ║
║  │  ■ COUNT 1  [C]  ────────────────     │ │  │            schema (get ...    │    │ ║
║  │  "Global policy is unsafe"            │ │  └───────────────────────────────┘    │ ║
║  │   ◆ supported_by: Ex.A, Ex.B         │ │  ── supports ──▶ Count 1, Count 4     │ ║
║  │   STATUS: █████ PROVEN                │ │  Admitted: ✓                          │ ║
║  │                                       │ │                                       │ ║
║  │  ■ COUNT 2  [C]  ────────────────     │ │  ───────────────────────────────────  │ ║
║  │  "No performance regression"          │ │                                       │ ║
║  │   ◆ supported_by: Ex.C               │ │  ◆ EXHIBIT B  [E:test]                │ ║
║  │   STATUS: █████ PROVEN                │ │  ┌───────────────────────────────┐    │ ║
║  │                                       │ │  │  auth_test.clj:109-125        │    │ ║
║  │  ■ COUNT 3  [C]  ────────────────     │ │  │  (deftest expired-token-      │    │ ║
║  │  "All edge cases covered"             │ │  │    per-route-test             │    │ ║
║  │   ◆ supported_by: Ex.B (partial)     │ │  │    (is (= 401 (:status ...)   │    │ ║
║  │   ◆ opposed_by: Objection #1         │ │  │  (deftest scope-mismatch-     │    │ ║
║  │   STATUS: ███░░ CHALLENGED            │ │  │    test ...                   │    │ ║
║  │                                       │ │  └───────────────────────────────┘    │ ║
║  │  ■ COUNT 4  [C]  ────────────────     │ │  ── supports ──▶ Count 1,3,4         │ ║
║  │  "Tests cover core paths"             │ │  Admitted: ✓                          │ ║
║  │   ◆ supported_by: Ex.B, Ex.D         │ │                                       │ ║
║  │   STATUS: █████ PROVEN                │ │  ───────────────────────────────────  │ ║
║  │                                       │ │                                       │ ║
║  │  ■ COUNT 5  [C]  ────────────────     │ │  ◆ EXHIBIT C  [E:bench]               │ ║
║  │  "Migration is safe"                  │ │  ┌───────────────────────────────┐    │ ║
║  │   ◆ supported_by: (none)             │ │  │  $ ab -n 1000 -c 50 ...       │    │ ║
║  │   STATUS: ░░░░░ UNSUBSTANTIATED      │ │  │  Requests/sec:  12,847        │    │ ║
║  │                                       │ │  │  p99 latency:   2ms          │    │ ║
║  │   ┌────────────────────────────┐      │ │  │  p50 latency:   0.8ms        │    │ ║
║  │   │ ⚠ THE COURT NOTES:        │      │ │  └───────────────────────────────┘    │ ║
║  │   │ No exhibit has been        │      │ │  ── supports ──▶ Count 2              │ ║
║  │   │ entered in support of      │      │ │  Admitted: ✓                          │ ║
║  │   │ Count 5. This count cannot │      │ │                                       │ ║
║  │   │ be proved without evidence.│      │ │  ───────────────────────────────────  │ ║
║  │   └────────────────────────────┘      │ │                                       │ ║
║  │                                       │ │  ◆ EXHIBIT D  [E:???]                 │ ║
║  └───────────────────────────────────────┘ │  ┌───────────────────────────────┐    │ ║
║                                             │  │                               │    │ ║
║  ┌── OBJECTIONS FILED ──────────────────┐  │  │   N O T   S U B M I T T E D   │    │ ║
║  │                                       │  │  │                               │    │ ║
║  │  ⚡ OBJECTION #1  [F]                 │  │  │   Required to prove           │    │ ║
║  │  Filed by: Judge @alex, 2 min ago     │  │  │   Count 5: "Migration safe"   │    │ ║
║  │                                       │  │  │                               │    │ ║
║  │  ┌─────────────────────────────────┐  │  │  └───────────────────────────────┘    │ ║
║  │  │ "Objection! The prosecution     │  │  │  ── supports ──▶ (nothing yet)       │ ║
║  │  │  claims all edge cases are      │  │  │  Admitted: PENDING                   │ ║
║  │  │  covered (Count 3), yet no      │  │  │                                       │ ║
║  │  │  exhibit demonstrates refresh   │  │  ├── EXHIBIT CROSS-REFERENCE ──────────┤ ║
║  │  │  token handling with mixed      │  │  │                                       │ ║
║  │  │  OAuth scopes."                 │  │  │  Ex.A ──supports──▶ C1, C4           │ ║
║  │  │                                 │  │  │  Ex.B ──supports──▶ C1, C3, C4       │ ║
║  │  │  ── opposes ──▶ Count 3         │  │  │  Ex.C ──supports──▶ C2               │ ║
║  │  │  ── informs ──▶ Risk 2          │  │  │  Ex.D ──supports──▶ (pending)        │ ║
║  │  │                                 │  │  │                                       │ ║
║  │  │  [SUSTAIN]  [OVERRULE]  [REPLY] │  │  │  Obj.1 ──opposes──▶ C3              │ ║
║  │  └─────────────────────────────────┘  │  │        ──informs──▶ R2               │ ║
║  └───────────────────────────────────────┘  └───────────────────────────────────────┘ ║
║                                                                                      ║
║  ┌── WARNINGS TO THE COURT  [R] ────────────────────────────────────────────────┐   ║
║  │                                                                              │   ║
║  │  ▲ RISK 1  [R]                           ▲ RISK 2  [R]                      │   ║
║  │  "Legacy tokens in production            "Refresh tokens with mixed OAuth    │   ║
║  │   may not conform to new schema.          scopes have not been tested.       │   ║
║  │   No migration plan presented."           Raised by Objection #1."           │   ║
║  │                                                                              │   ║
║  │  ── opposes ──▶ Count 5                  ── opposes ──▶ Count 3             │   ║
║  │  ── blocks  ──▶ Verdict                  ── blocks  ──▶ Verdict             │   ║
║  │  Severity: MEDIUM                        Severity: HIGH                     │   ║
║  │  [ACKNOWLEDGE]  [ACTION]  [WAIVE]        [ACKNOWLEDGE]  [ACTION]  [WAIVE]   │   ║
║  └──────────────────────────────────────────────────────────────────────────────┘   ║
║                                                                                      ║
║  ┌── PRIOR ART DISMISSED  [D:rejected] ────────────────────────────────────────┐    ║
║  │                                                                              │    ║
║  │  Case "Global Whitelist"              Case "Per-Role Chains"                 │    ║
║  │  opposed_by: "rigid, unscalable"      opposed_by: "N+1, 3x latency"         │    ║
║  │  superseded_by: Chosen approach       superseded_by: Chosen approach         │    ║
║  │  DISMISSED ✗                          DISMISSED ✗                            │    ║
║  │                                                                              │    ║
║  │  Chosen: "Per-route schema map" [D:accepted]                                 │    ║
║  │  supported_by: Ex.A, Ex.C     ADOPTED ✓                                     │    ║
║  └──────────────────────────────────────────────────────────────────────────────┘    ║
║                                                                                      ║
╠══════════════════════════════════════════════════════════════════════════════════════╣
║  [D] V E R D I C T                                                                  ║
║                                                                                      ║
║  depends_on:  C1 ✓    C2 ✓    C3 ⚡    C4 ✓    C5 ✗                                ║
║  blocked_by:  R1 (open, medium)    R2 (open, high)                                  ║
║                                                                                      ║
║  ┌─────────────────┐  ┌─────────────────────┐  ┌───────────────────────────┐        ║
║  │  ✓  ACQUIT      │  │  ◐  CONTINUE        │  │  ✗  CONVICT              │        ║
║  │  (approve)      │  │  DELIBERATION       │  │  (request changes)       │        ║
║  │  LOCKED — 2     │  │  ◀── RECOMMENDED    │  │                          │        ║
║  │  open risks     │  │                     │  │                          │        ║
║  └─────────────────┘  └─────────────────────┘  └───────────────────────────┘        ║
╚══════════════════════════════════════════════════════════════════════════════════════╝
```
