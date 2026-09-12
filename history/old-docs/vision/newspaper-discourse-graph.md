# Vision 6: The Newspaper — Discourse Graph Edition

## ONE-SCREEN VERSION (the real UI)

```
╔═══════════════════════════════════════════════════════════════════════════════════════════════════════╗
║  ┌─────────────────────────────────────────────────────────────────────────────────────────────────┐  ║
║  │                             T  H  E     D  A  I  L  Y     D  I  F  F                            │  ║
║  │                                "All the Code That's Fit to Ship"                                │  ║
║  │ Vol. CXLVII  ·  No. 847  ·  Thu Feb 13, 2026                                      FINAL EDITION │  ║
║  └─────────────────────────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                                       ║
║  [Q] AUTH MIDDLEWARE OVERHAULED;                  ║  STATS                                            ║
║  PER-ROUTE VALIDATION REPLACES                    ║  Files: 3   Lines: +47/-12   Tests: 4             ║
║  GLOBAL POLICY AFTER TWO                          ║                                                   ║
║  FAILED ATTEMPTS                                  ║  DISCOURSE GRAPH                                  ║
║                                                   ║  ──────────────────────────────────────           ║
║  Three routes gain independent token              ║  [Q] Questions:  1 root (+3 sub)                  ║
║  validation, ending months of security            ║  [C] Claims:     5  ( 3● · 1◐ · 1○ )              ║
║  workarounds. Author @sid delivers a              ║  [E] Evidence:   4  (+1 gap)                      ║
║  composable schema-based approach after           ║  [D] Decisions:  3  ( 1✓ · 2✗ )                   ║
║  evaluating and rejecting two alternatives.       ║  [R] Risks:      2  (both open)                   ║
║  Two risks remain open.                           ║  [F] Feedback:   1  objection                     ║
║                                                   ║                                                   ║
║                                                   ║  Confidence: █████████████████░░░░░░░ 72%         ║
║                                                   ║  Risk: MEDIUM    Est. review: ~12 min             ║
╠═══════════════════════════════════════════════════╬═══════════════════════════════════════════════════╣
║  SECTION A: WHAT CHANGED [Q]                      ║  SECTION B: WHY THIS WAY [Q]                      ║
║  ───────────────────────────────────────────      ║  ──────────────────────────────────────           ║
║                                                   ║                                                   ║
║  [C] "New wrap-per-route-auth fn replaces         ║  [D:rejected] "GLOBAL WHITELIST"                  ║
║       old single-policy handler"                  ║  Rigid, doesn't scale past 5 routes.              ║
║                                                   ║  ── opposed_by: "unscalable"                      ║
║  ◆ [E:code] middleware.clj:47-63                  ║  ── superseded_by: per-route schema               ║
║  │  (defn wrap-per-route-auth                     ║                                                   ║
║  │    [handler route-schemas]                     ║  [D:rejected] "PER-ROLE CHAINS"                   ║
║  │    (fn [req] (case (validate ...)              ║  N+1 middleware calls, 3x latency.                ║
║  │      :valid   (handler req)                    ║  ── opposed_by: [E:bench] p99=6ms                 ║
║  │      :expired {:status 401 ...})))             ║  ── superseded_by: per-route schema               ║
║  ── supports ──▶ C1, C4                           ║                                                   ║
║                                                   ║  [D:accepted] "PER-ROUTE SCHEMA MAP"              ║
║  ◆ [E:test] auth_test.clj:109-140                 ║  O(1) lookup. Composable. Testable.               ║
║  │  ✓ expired-token    ✓ scope-mismatch           ║  No performance regression.                       ║
║  │  ✓ malformed        ✓ valid-passthrough        ║  ── supported_by: Ex.A, Ex.C                      ║
║  ── supports ──▶ C1, C3, C4                       ║  [E:bench] p99=2ms · throughput=12k rps           ║
╠═══════════════════════════════════════════════════╩═══════════════════════════════════════════════════╣
║                                                                                                       ║
║  E D I T O R I A L    [Q] "What risks remain before this ships?"                                      ║
║                                                                                                       ║
║  ┌─ ▲ [R] RISK 1 ───────────────────────────────┐   ┌─ ▲ [R] RISK 2 ───────────────────────────────┐  ║
║  │ "Legacy tokens in production may             │   │ "Refresh tokens with mixed OAuth             │  ║
║  │  not conform to new per-route                │   │  scopes have not been tested.                │  ║
║  │  schema. No migration plan or                │   │  Raised by @alex, Objection #1."             │  ║
║  │  rollback strategy presented."               │   │                                              │  ║
║  │ ── opposes C5 · blocks Verdict               │   │ ── opposes C3 · blocks Verdict               │  ║
║  │ Severity: MEDIUM       [WAIVE]               │   │ Severity: HIGH         [WAIVE]               │  ║
║  └──────────────────────────────────────────────┘   └──────────────────────────────────────────────┘  ║
║                                                                                                       ║
║  LETTERS TO THE EDITOR [F]                                                                            ║
║  @alex: "Count 3 claims complete edge-case coverage, yet no exhibit shows refresh token + mixed       ║
║   scope testing. This gap undermines confidence."    ── opposes C3  ── informs R2     [REPLY]         ║
║                                                                                                       ║
╠═══════════════════════════════════════════════════════════════════════════════════════════════════════╣
║                                                                                                       ║
║  [D] V E R D I C T                                                                                    ║
║  depends_on:  C1 ✓    C2 ✓    C3 ⚡    C4 ✓    C5 ✗             blocked_by:  R1 ⚠    R2 ⚠              ║
║                                                                                                       ║
║  ┌─────────────────┐        ┌──────────────────────────────┐        ┌──────────────────────┐          ║
║  │  ✓ SHIP IT      │        │  ◐ HOLD THE PRESS            │        │  ✗ KILL THE STORY    │          ║
║  │  (locked)       │        │     ◀── RECOMMENDED          │        │  (request changes)   │          ║
║  └─────────────────┘        └──────────────────────────────┘        └──────────────────────┘          ║
║                                                                                                       ║
╚═══════════════════════════════════════════════════════════════════════════════════════════════════════╝
```

---

## DETAILED VERSION (scroll, for deep reference)
## Full immersive layout with typed nodes and edges

### Discourse Graph Node Types Used
- `[Q]` Question — "The headline" + section sub-questions
- `[C]` Claim — "The article body / assertions"
- `[E]` Evidence — "Sources cited / diffs / benchmarks"
- `[D]` Decision — "Ship It / Hold" + rejected approaches
- `[R]` Risk — "Editorial opinions / concerns"
- `[F]` Feedback — "Letters to the editor"

### Key Insight: Projection
A newspaper is a PROJECTION of a discourse graph onto a reading-optimized layout.
Same nodes, same edges, different spatial arrangement optimized for scan-then-deep-read.

### Projection Mapping
- ABOVE THE FOLD = `[Q]` root + summary `[C]` claims + metadata
- SECTION A = `[Q]` sub-question + `[E]` diff evidence
- SECTION B = `[Q]` sub-question + `[D]` decisions + `[E]` benchmark evidence
- EDITORIAL = `[Q]` sub-question + `[R]` risk nodes
- LETTERS = `[F]` feedback nodes with edge annotations
- VERDICT BAR = `[D]` verdict node with blockers

### The Layout

```
╔════════════════════════════════════════════════════════════════════════════════════════╗
║                                                                                      ║
║  ┌────────────────────────────────────────────────────────────────────────────────┐  ║
║  │                                                                                │  ║
║  │               T  H  E     D  A  I  L  Y     D  I  F  F                        │  ║
║  │                                                                                │  ║
║  │            "All the Code That's Fit to Ship"                                   │  ║
║  │                                                                                │  ║
║  │   Vol. CXLVII  No. 847       Thursday, Feb 13, 2026        FINAL EDITION      │  ║
║  │                                                                                │  ║
║  └────────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                      ║
║ ═══════════════════════════════════════════════════════════════════════════════════   ║
║  ABOVE THE FOLD                                                          [Q] root   ║
║ ═══════════════════════════════════════════════════════════════════════════════════   ║
║                                                                                      ║
║   AUTH MIDDLEWARE OVERHAULED IN                  │                                    ║
║   LANDMARK REFACTOR; PER-ROUTE                  │   QUICK STATS                      ║
║   VALIDATION REPLACES GLOBAL                    │   ──────────────                   ║
║   POLICY AFTER TWO FAILED ATTEMPTS              │   Files changed:  3                ║
║                                                  │   Lines added:    +47              ║
║   Three routes gain independent token            │   Lines removed:  -12              ║
║   validation rules, ending months of             │   New tests:      4                ║
║   security workarounds. Author @sid              │   Risk level:     MEDIUM           ║
║   delivers composable schema-based               │   Confidence:     72%              ║
║   approach after evaluating and rejecting        │   Est. review:    ~12 min          ║
║   two alternatives on performance and            │                                    ║
║   scalability grounds.                           │   DISCOURSE SUMMARY                ║
║                                                  │   ──────────────────               ║
║   Two risks remain open. The editorial           │   [Q] Questions:   1 (+3 sub)      ║
║   board urges caution before merge.              │   [C] Claims:      5               ║
║                                                  │       ● proved:    3               ║
║   [CONTINUED BELOW...]                          │       ◐ partial:   1               ║
║                                                  │       ○ bare:      1               ║
║                                                  │   [E] Evidence:    4 (+1 missing)  ║
║                                                  │   [D] Decisions:   3               ║
║                                                  │       ✓ adopted:   1               ║
║                                                  │       ✗ rejected:  2               ║
║                                                  │   [R] Risks:       2 (both open)   ║
║                                                  │   [F] Feedback:    1 objection     ║
║                                                  │                                    ║
║ ═══════════════════════════════════════════════════════════════════════════════════   ║
║                                                                                      ║
║  SECTION A: WHAT CHANGED              ┃  SECTION B: WHY THIS WAY                    ║
║  [Q] "What changed in this PR?"       ┃  [Q] "Why this approach over others?"       ║
║  ─────────────────────────────────     ┃  ─────────────────────────────────────      ║
║                                        ┃                                              ║
║  [C] "New per-route auth function      ┃  [D:rejected] "GLOBAL WHITELIST"            ║
║   replaces old single-policy handler"  ┃  DISMISSED — First approach tried           ║
║                                        ┃                                              ║
║   ◆ [E:code] middleware.clj            ┃  The prosecution initially considered        ║
║   ┌────────────────────────────────┐   ┃  a global whitelist mapping every route      ║
║   │  middleware.clj:47-63          │   ┃  to an allow/deny flag. Dismissed as         ║
║   │  +16 lines / -3 lines         │   ┃  rigid and unscalable beyond 5 routes.       ║
║   │                                │   ┃                                              ║
║   │  + (defn wrap-per-route-auth   │   ┃   ── opposed_by: "rigid, doesn't scale"     ║
║   │  +   [handler route-schemas]   │   ┃   ── superseded_by: per-route schema        ║
║   │  +   (fn [request]             │   ┃                                              ║
║   │  +     (let [route (:uri ...)  │   ┃  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─      ║
║   │  +           schema (get ...)] │   ┃                                              ║
║   │  +       (case (validate ...)  │   ┃  [D:rejected] "PER-ROLE CHAINS"             ║
║   │  +         :valid (handler ..) │   ┃  DISMISSED — Second approach tried           ║
║   │  +         :expired {:status   │   ┃                                              ║
║   │  +           401 ...}          │   ┃  A chain of role-specific middleware          ║
║   │  - (fn [request]               │   ┃  layers was prototyped. Benchmark showed     ║
║   │  -   (if (valid-token? ...)    │   ┃  N+1 middleware invocations and a 3x         ║
║   │  -     (handler request)       │   ┃  latency regression on hot paths.            ║
║   │                                │   ┃                                              ║
║   │  [VIEW FULL DIFF]              │   ┃   ◆ [E:bench] opposed_by:                   ║
║   └────────────────────────────────┘   ┃     p99: 6ms (was 2ms), 3x regression       ║
║   ── supports ──▶ C1,C4               ┃   ── superseded_by: per-route schema         ║
║                                        ┃                                              ║
║   ◆ [E:test] auth_test.clj            ┃  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─      ║
║   ┌────────────────────────────────┐   ┃                                              ║
║   │  auth_test.clj:109-140        │   ┃  [D:accepted] "PER-ROUTE SCHEMA MAP"        ║
║   │  4 new tests                   │   ┃  ADOPTED — Final approach ✓                  ║
║   │                                │   ┃                                              ║
║   │  ✓ expired-token-per-route     │   ┃  Single O(1) hash-map lookup per request.    ║
║   │  ✓ scope-mismatch-test         │   ┃  Each route declares its own token schema.   ║
║   │  ✓ malformed-header-test       │   ┃  Composable, testable, no performance hit.   ║
║   │  ✓ valid-token-passthrough     │   ┃                                              ║
║   │                                │   ┃   ◆ [E:code] supported_by: Ex.A             ║
║   │  [VIEW FULL DIFF]              │   ┃   ◆ [E:bench] supported_by: Ex.C            ║
║   └────────────────────────────────┘   ┃     p99: 2ms, throughput: 12k rps            ║
║   ── supports ──▶ C1,C3,C4            ┃                                              ║
║                                        ┃                                              ║
║ ═══════════════════════════════════════════════════════════════════════════════════   ║
║                                                                                      ║
║  E D I T O R I A L                                                                   ║
║  [Q] "What risks remain before this ships?"                                          ║
║  ──────────────────────────────────────────────────────────────────────────────────   ║
║                                                                                      ║
║  OPINION: TWO RISKS DEMAND ATTENTION BEFORE MERGE                                    ║
║                                                                                      ║
║  ┌── ▲ [R] RISK 1 ──────────────────────────┐ ┌── ▲ [R] RISK 2 ─────────────────┐  ║
║  │                                            │ │                                  │  ║
║  │  "Legacy tokens in production"             │ │  "Refresh tokens with mixed      │  ║
║  │                                            │ │   OAuth scopes"                  │  ║
║  │  Tokens currently in the wild may not      │ │                                  │  ║
║  │  conform to the new per-route schema       │ │  No test covers the scenario     │  ║
║  │  format. No migration plan, rollback       │ │  where a refresh token carries   │  ║
║  │  strategy, or compatibility shim has       │ │  scopes from multiple route      │  ║
║  │  been presented to the court.              │ │  schemas simultaneously.         │  ║
║  │                                            │ │                                  │  ║
║  │  ── opposes  ──▶ C5 ("migration safe")    │ │  ── opposes  ──▶ C3 ("all edge   │  ║
║  │  ── blocks   ──▶ [D] Verdict              │ │     cases covered")              │  ║
║  │                                            │ │  ── blocks   ──▶ [D] Verdict     │  ║
║  │  Severity: MEDIUM                          │ │                                  │  ║
║  │  Source: Court's own motion                │ │  Severity: HIGH                  │  ║
║  │                                            │ │  Source: Objection #1            │  ║
║  │  [SEE EVIDENCE GAP]  [WAIVE WITH REASON]   │ │                                  │  ║
║  └────────────────────────────────────────────┘ │  [SEE OBJECTION]  [WAIVE]        │  ║
║                                                  └──────────────────────────────────┘  ║
║                                                                                      ║
║ ═══════════════════════════════════════════════════════════════════════════════════   ║
║                                                                                      ║
║  LETTERS TO THE EDITOR  [F]                                                          ║
║  ──────────────────────────────────────────────────────────────────────────────────   ║
║                                                                                      ║
║  ┌── Letter from @alex ──────────────────────────────────────────────────────────┐   ║
║  │                                                                                │   ║
║  │  "To the Editor — I have reviewed the prosecution's exhibits with care.       │   ║
║  │   Count 3 claims complete edge-case coverage, yet Exhibit B demonstrates      │   ║
║  │   only expired tokens and scope mismatch. What about refresh tokens           │   ║
║  │   carrying mixed OAuth scopes? This gap undermines confidence."               │   ║
║  │                                                                                │   ║
║  │  ── opposes  ──▶ [C] Count 3 ("all edge cases covered")                      │   ║
║  │  ── informs  ──▶ [R] Risk 2 ("refresh + mixed scope")                        │   ║
║  │                                                                                │   ║
║  │  Impact on confidence:  Count 3 downgraded ● ──▶ ◐                            │   ║
║  │  Impact on verdict:     +1 blocker (R2)                                       │   ║
║  │                                                                                │   ║
║  │  [REPLY]  [SUSTAIN OBJECTION]  [OVERRULE]                                     │   ║
║  └────────────────────────────────────────────────────────────────────────────────┘   ║
║                                                                                      ║
║ ═══════════════════════════════════════════════════════════════════════════════════   ║
║                                                                                      ║
║  [D] V E R D I C T                    ┃  GRAPH HEALTH                                ║
║  ─────────────────────────────────    ┃  ──────────────────────────                   ║
║                                        ┃                                              ║
║  depends_on:                           ┃  Nodes:  Q:1  C:5  E:4  D:3  R:2  F:1      ║
║   [C1] ✓ proved                        ┃  Edges:  supports:6  opposes:3              ║
║   [C2] ✓ proved                        ┃          informs:1   blocks:2               ║
║   [C3] ◐ challenged ←── Obj.#1        ┃          supersedes:2                       ║
║   [C4] ✓ proved                        ┃                                              ║
║   [C5] ○ unsubstantiated              ┃  Unresolved:                                 ║
║                                        ┃   2 [R] nodes blocking [D] verdict          ║
║  blocked_by:                           ┃   1 [E] slot empty (Count 5)                ║
║   [R1] ⚠ open (medium)                ┃   1 [C] downgraded by [F]                   ║
║   [R2] ⚠ open (high)                  ┃                                              ║
║                                        ┃  Graph completeness: 72%                     ║
║  ┌──────────────────┐                  ┃                                              ║
║  │  HOLD THE PRESS  │ ◀── RECOMMENDED  ┃                                              ║
║  └──────────────────┘                  ┃                                              ║
║                                        ┃                                              ║
║  ┌──────────┐  ┌──────────────────┐    ┃                                              ║
║  │ SHIP IT  │  │ REQUEST CHANGES  │    ┃                                              ║
║  │ (locked) │  │                  │    ┃                                              ║
║  └──────────┘  └──────────────────┘    ┃                                              ║
╚════════════════════════════════════════════════════════════════════════════════════════╝
```
