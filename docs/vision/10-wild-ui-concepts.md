# 10 Wild UI Visions for Threaded Engineering Workspace
## Generated 2026-02-15 from design brief

Each vision is a genuinely distinct metaphor and interaction model for the same core problem:
**Navigate code changes as structured reasoning threads (intent -> claims -> evidence -> decisions -> risks)**

---

## VISION 1: THE CRIME BOARD

*You walk into a detective's office. Red string everywhere. Photos pinned to cork. The change is the case. You're solving it.*

```
+-------------------------------------------------------------------------------------+
|  ░░░░░░░░░░░░░░░░░░░░░░  CORK BOARD TEXTURE  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ |
|  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ |
|  ░░  pin-----------------------------------------------------pin  ░░░░░░░░░░░░░░ |
|  ░░  |          +==============+                                |  ░░░░░░░░░░░░░░ |
|  ░░  |          | PHOTO/SUSPECT|                                |  ░░░░░░░░░░░░░░ |
|  ░░  |          |              |         THE CASE               |  ░░░░░░░░░░░░░░ |
|  ░░  |          |  PR #847     |    "Refactor auth middleware   |  ░░░░░░░░░░░░░░ |
|  ░░  |          |  @sid        |     to support per-route       |  ░░░░░░░░░░░░░░ |
|  ░░  |          |  2h ago      |     token validation"          |  ░░░░░░░░░░░░░░ |
|  ░░  |          +------+-------+                                |  ░░░░░░░░░░░░░░ |
|  ░░  pin---------------+---------------------------------------pin  ░░░░░░░░░░░░░░ |
|  ░░░░░░░░░░░░░░░░░░░░░|░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ |
|  ░░░░░░░░░░░░░░░░░░░░░|░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ |
|  ░░  +============+░░░|░░░░+============+░░░░░░░░░░+============+░░░░░░░░░░░░░░░ |
|  ░░  | LEAD 1     |░░░|░░░░| LEAD 2     |░░░░░░░░░░| LEAD 3     |░░░░░░░░░░░░░░░ |
|  ░░  |            |░░░|░░░░|            |░░░░░░░░░░|            |░░░░░░░░░░░░░░░ |
|  ░░  | "single    |---+----| "no perf   |░░░░░░░░░░| "backward  |░░░░░░░░░░░░░░░ |
|  ░░  |  source of |░░░|░░░░|  regression|░░░░░░░░░░|  compat    |░░░░░░░░░░░░░░░ |
|  ░░  |  truth"    |░░░|░░░░|  on hot    |░░░░░░░░░░|  maintained|░░░░░░░░░░░░░░░ |
|  ░░  |            |░░░|░░░░|  paths"    |░░░░░░░░░░|  for v2    |░░░░░░░░░░░░░░░ |
|  ░░  | STATUS:    |░░░|░░░░|            |░░░░░░░░░░|  clients"  |░░░░░░░░░░░░░░░ |
|  ░░  | PROVEN     |░░░|░░░░| STATUS:    |░░░░░░░░░░|            |░░░░░░░░░░░░░░░ |
|  ░░  +-----+------+░░░|░░░░| PROVEN     |░░░░░░░░░░| STATUS:    |░░░░░░░░░░░░░░░ |
|  ░░░░░░░░░░|░░░░░░░░░░|░░░░+-----+------+░░░░░░░░░░| PARTIAL    |░░░░░░░░░░░░░░░ |
|  ░░░░░░░░░-+----------+░░░░░░░░░░|░░░░░░░░░░░░░░░░░+------+-----+░░░░░░░░░░░░░░░ |
|  ░░░░░░░░░░|░░░░░░░░░░░░░░░░░░░░░|░░░░░░░░░░░░░░░░░░░░░░░░|░░░░░░░░░░░░░░░░░░░░ |
|  ░░  +-----v------+░░░░░░░+-----v------+░░░░░░░░░░░+------v-----+░░░░░░░░░░░░░░░ |
|  ░░  | EVIDENCE   |░░░░░░░| EVIDENCE   |░░░░░░░░░░░| EVIDENCE   |░░░░░░░░░░░░░░░ |
|  ░░  |            |░░░░░░░|            |░░░░░░░░░░░|            |░░░░░░░░░░░░░░░ |
|  ░░  | middleware |░░░░░░░| $ ab -n    |░░░░░░░░░░░| ??? NO     |░░░░░░░░░░░░░░░ |
|  ░░  | .clj:47    |░░░░░░░| 1000 ...   |░░░░░░░░░░░| EVIDENCE   |░░░░░░░░░░░░░░░ |
|  ░░  | [VIEW]     |░░░░░░░| -> p99 2ms |░░░░░░░░░░░| FOUND YET  |░░░░░░░░░░░░░░░ |
|  ░░  +------------+░░░░░░░| [VIEW]     |░░░░░░░░░░░|            |░░░░░░░░░░░░░░░ |
|  ░░░░░░░░░░░░░░░░░░░░░░░░░+------------+░░░░░░░░░░░| COLD CASE  |░░░░░░░░░░░░░░░ |
|  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░+------------+░░░░░░░░░░░░░░░ |
|  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ |
|  ░░░░ +------------------------------------------------------------+ ░░░░░░░░░░░░░ |
|  ░░░░ |  DEAD ENDS (rejected alternatives)                        | ░░░░░░░░░░░░░ |
|  ░░░░ |                                                            | ░░░░░░░░░░░░░ |
|  ░░░░ |  X "Global whitelist"     X "Middleware chain per role"    | ░░░░░░░░░░░░░ |
|  ░░░░ |    crossed out, pinned      crossed out, pinned            | ░░░░░░░░░░░░░ |
|  ░░░░ |    sideways, coffee stain   with "TOO SLOW" scrawled      | ░░░░░░░░░░░░░ |
|  ░░░░ +------------------------------------------------------------+ ░░░░░░░░░░░░░ |
|  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ CASE #847 ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ |
+-------------------------------------------------------------------------------------+
```

**Interaction model:**
- Drag cards around the board freely
- Draw red string between cards (creates reasoning links)
- Pin new evidence photos
- Cross out dead ends with a satisfying scribble animation
- The "cold case" evidence gap pulses red until someone fills it
- Cork board texture, push-pin sounds, string physics

**Strengths:** Intuitive, playful, makes gaps viscerally obvious
**Best for:** Small teams, creative/exploratory reviews


---

## VISION 2: THE METRO MAP

*Every reasoning thread is a transit line. Stations are claims. Transfers are where threads intersect. You ride the line from intent to proof.*

```
+-------------------------------------------------------------------------------------+
|                                                                                     |
|  SOFTLAND REASONING TRANSIT AUTHORITY            +------------------------------+   |
|  ====================================            |  PR #847 -- Auth Refactor    |   |
|                                                  |  3 lines . 8 stations        |   |
|                                                  |  Est. review: 12 min         |   |
|                                                  +------------------------------+   |
|                                                                                     |
|                           +---- ARCHITECTURE LINE (blue) ----------------+          |
|                           |                                              |          |
|         [===]         [===]         [===]         [===]         [===]    |          |
|    =====| ? |====+====| A1|=========| A2|=========| A3|=========| A4|===|          |
|         [===]    |    [===]         [===]         [=|=]         [===]    |          |
|        INTENT    |   Single         Per-route      | Route      Decision:|          |
|        "why?"    |   source         schema         | Transfer   Adopt    |          |
|                  |   of truth       map            |                     |          |
|                  |                                 |                     |          |
|         -- TESTING LINE (green) -------------------|-----               |          |
|                  |                                 |                     |          |
|                  |    [===]         [===]         [=|=]         [===]    |          |
|                  +====| T1|=========| T2|=========| T3|=========| T4|===|          |
|                  |    [===]         [===]         [===]         [===]    |          |
|                  |   Expired        Scope          Edge          All     |          |
|                  |   token test     mismatch       cases ~      pass    |          |
|                  |                  test           GAP HERE              |          |
|                  |                                 ^                     |          |
|         -- RISK LINE (red, dashed) ----------------|-----               |          |
|                  |                                 |                     |          |
|                  |    [===]                        [=|=]                 |          |
|                  +====| R1|========================| R2|=== UNRESOLVED  |          |
|                       [===]                        [===]                |          |
|                      Legacy                       Refresh               |          |
|                      tokens                       token +               |          |
|                      in prod                      mixed scope           |          |
|                                                                         |          |
|  +-- STATION DETAIL (click any station) ----------------------------+   |          |
|  |                                                                  |   |          |
|  |  STATION T3: Edge Cases                                          |   |          |
|  |  Status: ~ PARTIAL -- missing refresh token scenario             |   |          |
|  |                                                                  |   |          |
|  |  Connections:                                                    |   |          |
|  |    -> A3 (Architecture: Route schema)  [TRANSFER]                |   |          |
|  |    -> R2 (Risk: Mixed scope)           [TRANSFER]                |   |          |
|  |                                                                  |   |          |
|  |  Evidence:  auth_test.clj:112  [OPEN IN CODE]                    |   |          |
|  |  Feedback:  1 comment from @alex                                 |   |          |
|  |                                                                  |   |          |
|  |  [< PREV STATION]  [NEXT STATION >]  [SWITCH LINE]  [COMMENT]   |   |          |
|  +------------------------------------------------------------------+   |          |
|                                                                         |          |
|  -- SERVICE STATUS --------------------------------------------------   |          |
|  BLUE  Architecture Line:  RUNNING -- all stations clear                |          |
|  GREEN Testing Line:       DELAYED -- gap at T3                         |          |
|  RED   Risk Line:          SUSPENDED -- unresolved terminal             |          |
+-------------------------------------------------------------------------------------+
```

**Interaction model:**
- Click stations to see detail panel
- "Ride" a line (auto-advance through stations with smooth animation)
- Transfer between lines where threads intersect
- Service Status gives instant review health
- Delayed/Suspended lines demand attention before merge

**Strengths:** Systematic, clear path, familiar metaphor
**Best for:** Large teams, structured review processes


---

## VISION 3: THE GEOLOGICAL CROSS-SECTION

*The change is terrain. The surface is the summary. Dig down through layers to reach bedrock (code). Each layer adds depth and detail.*

```
+-------------------------------------------------------------------------------------+
|                                                                                     |
|   DEPTH GAUGE          SURFACE: "Refactor auth for per-route validation"            |
|   +---+                                                                             |
|   | ^ |  ~~~~ SURFACE (Summary) ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~    |
|   | | |  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░    |
|   | | |  ░  Three routes need distinct token validation. Global policy replaced  ░   |
|   | | |  ░  with per-route schema map. Tests cover core cases. One risk open.    ░   |
|   | | |  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░    |
|   | | |                                                                             |
|   | | |  ==== LAYER 1: Claims ====================================================  |
|   | | |  =                                                                       =  |
|   | | |  =  +----------+  +----------+  +----------+  +----------+  +--------+  =  |
|   | | |  =  |* source  |  |* no perf |  |~ back-   |  |* tests   |  |~ migr- |  =  |
|   | | |  =  |  of truth|  |  regress |  |  compat  |  |  cover   |  |  ation |  =  |
|   | | |  =  +----+-----+  +----+-----+  +----+-----+  +----+-----+  +---+----+  =  |
|   | | |  =========|============|=============|=============|============|========    |
|   | | |           |            |             |             |           |             |
|   | | |  #### LAYER 2: Evidence #################################################   |
|   | | |  #        |            |             |             |           |          #  |
|   | | |  #  +-----v---+  +----v----+   +----v----+  +-----v---+  +---v----+     #  |
|   | | |  #  |code     |  |bench-   |   |   ???   |  |test     |  |  ???   |     #  |
|   | | |  #  |mw.clj:47|  |mark     |   | HOLLOW  |  |auth_test|  |HOLLOW |     #  |
|   | | |  #  |[DRILL]  |  |output   |   | (no     |  |:112     |  |(no    |     #  |
|   | | |  #  +---------+  |[DRILL]  |   |  rock   |  |[DRILL]  |  | rock  |     #  |
|   | | |  #               +---------+   |  here)  |  +---------+  | here) |     #  |
|   | | |  #                              +---------+               +-------+     #  |
|   | | |  ######################################################################## |
|   | | |                                                                             |
|   | | |  :::: LAYER 3: Decisions ::::::::::::::::::::::::::::::::::::::::::::::::::  |
|   | | |  :                                                                       :  |
|   | | |  :   X Global whitelist     X Per-role chains    V Per-route schema       :  |
|   | | |  :   (fossilized)           (fossilized)          (crystallized)          :  |
|   | | |  :                                                                       :  |
|   | | |  :::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::   |
|   | | |                                                                             |
|   | | |  %%%% LAYER 4: Risks (magma) %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%   |
|   | | |  %                                                                       %  |
|   | | |  %   FIRE Legacy tokens in prod      FIRE Refresh + mixed scope          %  |
|   | v |  %      (active, bubbling)              (active, bubbling)               %  |
|   |   |  %                                                                       %  |
|   | * |  %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%|
|   |   |                                                                             |
|   +---+  ==== BEDROCK: Raw Code ================================================    |
|          =  (defn wrap-per-route-auth [handler route-schemas] ...)               =  |
|          ================================================================ =========  |
+-------------------------------------------------------------------------------------+
```

**Interaction model:**
- Scroll down = dig deeper through layers
- "HOLLOW" spots are evidence gaps -- visible holes in the rock
- Click [DRILL] to tunnel into code view
- Depth gauge on left tracks your position
- Risks are magma at bottom -- they glow and bubble (WebGPU particles)
- Rejected decisions are fossils -- clearly dead, preserved

**Strengths:** Natural progressive disclosure, visceral depth metaphor
**Best for:** Reviewers who want to control their own depth of investigation


---

## VISION 4: THE COURTROOM

*The claim is on trial. The author presents the case. The reviewer is the judge. Evidence is exhibits. Objections are feedback.*

```
+-------------------------------------------------------------------------------------+
|                                                                                     |
|  +===============================================================================+  |
|  |                    COURT OF CODE REVIEW -- CASE #847                           |  |
|  |                    The People v. auth-per-route                                |  |
|  +===============================================================================+  |
|                                                                                     |
|  +-- THE BENCH (Reviewer) ------------------------------------------------------+   |
|  |                                                                              |   |
|  |   Judge @alex                              VERDICT STATUS                    |   |
|  |   [ICON]                                   +----------------------+          |   |
|  |                                            | o NOT YET REACHED    |          |   |
|  |   [SUSTAIN]  [OVERRULE]  [REQUEST MORE]    | Exhibits reviewed:   |          |   |
|  |                                            | 3 of 5               |          |   |
|  |                                            | Objections: 1        |          |   |
|  |                                            +----------------------+          |   |
|  +------------------------------------------------------------------------------+   |
|                                                                                     |
|  +-- PROSECUTION (Author's Case) ----------+  +-- EVIDENCE LOCKER -------------+   |
|  |                                          |  |                                |   |
|  |  OPENING STATEMENT:                      |  |  Exhibit A: middleware.clj:47  |   |
|  |  "The existing auth system applies a     |  |  +--------------------------+  |   |
|  |   single global policy. This is unsafe   |  |  | (defn wrap-per-route-    |  |   |
|  |   for routes with different security     |  |  |   auth [handler schemas] |  |   |
|  |   requirements."                         |  |  |   (fn [request] ...))    |  |   |
|  |                                          |  |  +--------------------------+  |   |
|  |  CHARGES (Claims):                       |  |  Status: ADMITTED              |   |
|  |                                          |  |                                |   |
|  |  COUNT 1: * "Global policy is unsafe"    |  |  Exhibit B: auth_test.clj:112  |   |
|  |    Evidence: Exhibit A, Exhibit B        |  |  +--------------------------+  |   |
|  |    Status: PROVEN                        |  |  | (deftest expired-token-  |  |   |
|  |                                          |  |  |   per-route-test ...     |  |   |
|  |  COUNT 2: * "Per-route is fast enough"   |  |  +--------------------------+  |   |
|  |    Evidence: Exhibit C                   |  |  Status: ADMITTED              |   |
|  |    Status: PROVEN                        |  |                                |   |
|  |                                          |  |  Exhibit C: bench.txt          |   |
|  |  COUNT 3: ~ "All edge cases covered"     |  |  +--------------------------+  |   |
|  |    Evidence: Exhibit B (partial)         |  |  | p99 latency: 2ms        |  |   |
|  |    Status: CHALLENGED                    |  |  | throughput: 12k rps     |  |   |
|  |                                          |  |  +--------------------------+  |   |
|  |  COUNT 4: ~ "Migration is safe"          |  |  Status: ADMITTED              |   |
|  |    Evidence: NONE PRESENTED              |  |                                |   |
|  |    Status: UNSUBSTANTIATED               |  |  Exhibit D: ???                |   |
|  |                                          |  |  +--------------------------+  |   |
|  +------------------------------------------+  |  | NOT YET SUBMITTED        |  |   |
|                                                 |  | Required for Count 4     |  |   |
|  +-- OBJECTIONS (Feedback) ----------------+   |  +--------------------------+  |   |
|  |                                          |   |                                |   |
|  |  OBJECTION #1 -- @alex, 2 min ago       |   +--------------------------------+   |
|  |  +----------------------------------+   |                                        |
|  |  | "Objection! Count 3 claims all   |   |                                        |
|  |  |  edge cases covered, but no      |   |                                        |
|  |  |  exhibit shows refresh token +   |   |                                        |
|  |  |  mixed scope testing."           |   |                                        |
|  |  |                                  |   |                                        |
|  |  |  Affects: Count 3               |   |                                        |
|  |  |  [SUSTAIN]  [OVERRULE]  [REPLY]  |   |                                        |
|  |  +----------------------------------+   |                                        |
|  +------------------------------------------+                                       |
|                                                                                     |
|  +-- PRIOR ART (Rejected Alternatives) ----------------------------------------+   |
|  |  Case 1: Global Whitelist -- DISMISSED (rigid, doesn't scale)                |   |
|  |  Case 2: Per-Role Chains  -- DISMISSED (N+1 middleware calls)                |   |
|  +----------------------------------------------------------------------------- +  |
|                                                                                     |
|  ================================================================================   |
|  [RENDER VERDICT]   APPROVE | REQUEST CHANGES | CONTINUE DELIBERATION               |
|  ================================================================================   |
+-------------------------------------------------------------------------------------+
```

**Interaction model:**
- Click "SUSTAIN" on objection -> claim status changes, author prompted for more evidence
- "OVERRULE" dismisses the objection with recorded reasoning
- Verdict button only activates when all counts are PROVEN or explicitly ACCEPTED WITH RISK
- Evidence locker items are clickable portals into code
- Courtroom drama sounds optional but encouraged

**Strengths:** Adversarial rigor, forces completeness, clear roles
**Best for:** Security-sensitive reviews, compliance-heavy teams


---

## VISION 5: THE MUSIC SCORE

*Time flows left-to-right. Different "instruments" play simultaneously: code changes, tests, decisions, risks. You read the score of the engineering session.*

```
+-------------------------------------------------------------------------------------+
|  SCORE: "Auth Refactor in D Minor"                     note = 1 commit   BPM: 12/hr|
|  Composer: @sid    Opus: PR #847    Duration: 2 hours                               |
|======================================================================================|
|                                                                                     |
|  TIME -->   0:00    0:15     0:30     0:45     1:00     1:15     1:30    1:45  NOW  |
|  ----------+--------+--------+--------+--------+--------+--------+-------+-----     |
|            |        |        |        |        |        |        |       |           |
|  CODE      |   n    |  nn    | nnn    |        |   n    |        |       |           |
|  changes   |  start |schema  | route  |  rest  | error  |        |       |           |
|            | refact | map    | valid  |        | bodies |        |       |           |
|  ----------+--------+--------+--------+--------+--------+--------+-------+-----     |
|            |        |        |        |        |        |        |       |           |
|  TESTS     |        |        |   n    |   n    |  nn    |        |  n    |           |
|            |        |        | expire | scope  | edge   |        | final |           |
|            |        |        | test   | test   | cases  |        | pass! |           |
|  ----------+--------+--------+--------+--------+--------+--------+-------+-----     |
|            |        |        |        |        |        |        |       |           |
|  DECIDE    |        |   X    |        |   X    |        |   V    |       |           |
|  V/X       |        | reject |        | reject |        | adopt  |       |           |
|            |        | global |        | per-   |        | per-   |       |           |
|            |        | w.list |        | role   |        | route  |       |           |
|  ----------+--------+--------+--------+--------+--------+--------+-------+-----     |
|            |        |        |        |        |        |        |       |           |
|  RISK      |        |        |        |  ~~~~~~~~~~~~~~~~~~~~~~~~  ######|#####     |
|  (sustain) |        |        |        |  legacy tokens concern     OPEN  |still     |
|            |        |        |        |  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░|░░░░░     |
|            |        |        |        |        |        |  ~~~~~~|~~~~~~~|~~~~~     |
|            |        |        |        |        |        | refresh|mixed  |scope     |
|            |        |        |        |        |        |        | OPEN  |          |
|  ----------+--------+--------+--------+--------+--------+--------+-------+-----     |
|            |        |        |        |        |        |        |       |           |
|  CONFID-   |        |        |        |        |        |        |       |           |
|  ENCE      |_       |_=      |==      |===     |====    |=====   |====   |===       |
|  METER     |        |        |        |        |        |        |  v    | v        |
|            |        |        |        |        |        |        | @alex |objection |
|            |        |        |        |        |        |        | joins |filed     |
|  ----------+--------+--------+--------+--------+--------+--------+-------+-----     |
|                                                                                     |
|  > PLAY   || PAUSE   << REWIND   >> FAST-FWD   ZOOM IN MEASURE   LOOP              |
|                                                                                     |
|  NOW PLAYING: Measure 14 -- "Edge case tests written, risk emerges"                 |
|  Click any note to see the commit/evidence/decision at that moment                  |
+-------------------------------------------------------------------------------------+
```

**Interaction model:**
- Hit PLAY -- score plays forward, you watch the session unfold in fast-forward
- Each note is clickable, expanding to show commit/test/decision detail
- CONFIDENCE METER at bottom is a continuous waveform
- Clusters of notes = intense activity, gaps = thinking time
- You can literally hear the rhythm of work
- Loop a section to study it

**Strengths:** Temporal awareness, shows process not just outcome, beautiful
**Best for:** Understanding HOW someone worked, not just what they produced


---

## VISION 6: THE NEWSPAPER

*"Extra! Extra! Read all about it!" The review is a newspaper. Headlines grab attention. Read deeper only if you care.*

```
+-------------------------------------------------------------------------------------+
| +==================================================================================+|
| | +-------------------------------------------------------------------------+      ||
| | |                    THE DAILY DIFF                                        |      ||
| | |            "All the Code That's Fit to Ship"                             |      ||
| | |                                                                          |      ||
| | |   Vol. CXLVII No. 847        Thursday, Feb 13, 2026         EDITION: PR |      ||
| | +-------------------------------------------------------------------------+      ||
| |                                                                                  ||
| | +-- ABOVE THE FOLD --------------------------------------------------------+     ||
| | |                                                                          |     ||
| | |  AUTH MIDDLEWARE OVERHAULED:           | QUICK STATS                      |     ||
| | |  PER-ROUTE VALIDATION REPLACES        | -----------                      |     ||
| | |  GLOBAL POLICY                        | Files changed: 3                 |     ||
| | |                                       | Lines: +47 / -12                 |     ||
| | |  Three routes gain independent token  | Tests: 4 new                     |     ||
| | |  validation, ending months of sec-    | Risk level: MEDIUM               |     ||
| | |  urity workarounds. Author @sid       | Confidence: 72%                  |     ||
| | |  delivers schema-based approach       | Review time est: ~12m            |     ||
| | |  after evaluating two alternatives.   |                                  |     ||
| | |                                       |                                  |     ||
| | |  [CONTINUED ON PAGE 2...]             |                                  |     ||
| | +---------------------------------------+----------------------------------+     ||
| |                                                                                  ||
| | +-- SECTION A: WHAT CHANGED -----+  +-- SECTION B: WHY ------------------+      ||
| | |                                 |  |                                    |      ||
| | |  middleware.clj -- Major        |  |  REJECTED: "Global Whitelist"      |      ||
| | |  New: wrap-per-route-auth fn    |  |  Rigid. Doesn't scale beyond 5    |      ||
| | |  New: validate-token multi-meth |  |  routes.                          |      ||
| | |  Removed: valid-token? pred     |  |                                    |      ||
| | |                                 |  |  REJECTED: "Per-Role Chains"      |      ||
| | |  [VIEW FULL DIFF]               |  |  N+1 middleware calls. 3x latency |      ||
| | |                                 |  |  increase on hot paths.            |      ||
| | |  auth_test.clj -- 4 new tests  |  |                                    |      ||
| | |  Covers: expired, scope, mal-   |  |  CHOSEN: "Per-Route Schema Map"   |      ||
| | |  formed, valid token paths      |  |  Composable. Testable. O(1)       |      ||
| | |                                 |  |  lookup per request.              |      ||
| | |  [VIEW FULL DIFF]               |  |                                    |      ||
| | +---------------------------------+  +------------------------------------+      ||
| |                                                                                  ||
| | +-- EDITORIAL: RISKS & UNKNOWNS ----------------------------------------+       ||
| | |                                                                        |       ||
| | |  !! OPINION: TWO RISKS DEMAND ATTENTION BEFORE MERGE                   |       ||
| | |                                                                        |       ||
| | |  1. Legacy tokens in production may not conform to new schema.         |       ||
| | |     No migration plan presented. [SEE EVIDENCE GAP]                    |       ||
| | |                                                                        |       ||
| | |  2. Refresh tokens with mixed scopes are untested.                     |       ||
| | |     @alex has filed a formal objection. [READ OBJECTION]               |       ||
| | +------------------------------------------------------------------------+       ||
| |                                                                                  ||
| | +-- LETTERS TO THE EDITOR (Comments) ------------------------------------+       ||
| | |                                                                        |       ||
| | |  @alex writes: "What about refresh tokens with mixed scopes?"          |       ||
| | |                                                          [REPLY]       |       ||
| | +------------------------------------------------------------------------+       ||
| |                                                                                  ||
| |  [APPROVE: SHIP IT]    [CHANGES REQUESTED: HOLD THE PRESS]                      ||
| +==================================================================================+|
+-------------------------------------------------------------------------------------+
```

**Interaction model:**
- Reads like a newspaper -- scan headlines, dig into sections
- Above the fold = instant 10-second orientation
- "CONTINUED ON PAGE 2" = progressive disclosure
- "Letters to the Editor" = reviewer comments
- Editorial section = opinionated risk treatment
- Print-friendly, shareable

**Strengths:** Fastest time-to-orientation, familiar reading pattern
**Best for:** Tech leads reviewing many PRs, busy managers


---

## VISION 7: THE SPACE STATION MISSION CONTROL

*Every change is a mission. Claims are subsystems. Evidence is telemetry. Risks are warnings. GO/NO-GO for launch.*

```
+-------------------------------------------------------------------------------------+
|  +===============================================================================+  |
|  |  MISSION CONTROL -- OPERATION AUTH-REFACTOR          T-00:00:00 HOLD          |  |
|  +===============================================================================+  |
|                                                                                     |
|  +-- MISSION BRIEF ----------------------+  +-- SUBSYSTEM STATUS --------------+   |
|  |  Mission: Per-route token validation  |  |                                  |   |
|  |  Commander: @sid                      |  |  SYS-01 SOURCE-OF-TRUTH          |   |
|  |  Flight Director: @alex              |  |  ################  GO             |   |
|  |  Branch: auth-per-route              |  |                                  |   |
|  |  Payload: +47 / -12 lines            |  |  SYS-02 PERFORMANCE              |   |
|  +---------------------------------------+  |  ################  GO             |   |
|                                              |                                  |   |
|  +-- TELEMETRY --------------------------+  |  SYS-03 BACK-COMPAT              |   |
|  |                                        |  |  ########________  CAUTION       |   |
|  |  TEST COVERAGE      ############__  85%|  |                                  |   |
|  |  CONFIDENCE INDEX   #########_____  72%|  |  SYS-04 TEST-COVERAGE            |   |
|  |  RISK EXPOSURE      ####__________  28%|  |  ############____  CAUTION       |   |
|  |  DECISION CLARITY   ##############  98%|  |                                  |   |
|  |  EVIDENCE DENSITY   #########_____  68%|  |  SYS-05 MIGRATION                |   |
|  |                                        |  |  ####____________  NO-GO          |   |
|  +----------------------------------------+  |                                  |   |
|                                              +----------------------------------+   |
|  +-- WARNING BOARD ----------------------------------------------------------+      |
|  |                                                                            |      |
|  |  !! WARN-01  T-12m  Legacy tokens in prod -- no migration plan            |      |
|  |             > Affects: SYS-03, SYS-05                                      |      |
|  |             > Severity: MEDIUM    > Status: OPEN                           |      |
|  |             > [ACKNOWLEDGE]  [CREATE ACTION ITEM]  [WAIVE]                 |      |
|  |                                                                            |      |
|  |  !! WARN-02  T-2m   Refresh + mixed scope untested -- filed by @alex      |      |
|  |             > Affects: SYS-04                                              |      |
|  |             > Severity: HIGH      > Status: OPEN                           |      |
|  |             > [ACKNOWLEDGE]  [CREATE ACTION ITEM]  [WAIVE]                 |      |
|  |                                                                            |      |
|  +----------------------------------------------------------------------------+      |
|                                                                                     |
|  +-- FLIGHT LOG (Decisions) ------------------------------------------------+       |
|  |  T-1:45  DECISION  Rejected global whitelist (rigid, unscalable)         |       |
|  |  T-0:45  DECISION  Rejected per-role chains (3x latency hit)            |       |
|  |  T-0:30  DECISION  Adopted per-route schema map (composable, O(1))      |       |
|  |  T-0:02  ANOMALY   @alex reports missing test case                      |       |
|  +--------------------------------------------------------------------------+       |
|                                                                                     |
|  +===============================================================================+  |
|  |  LAUNCH AUTHORIZATION                                                         |  |
|  |                                                                               |  |
|  |  GO:    SYS-01   SYS-02                                                       |  |
|  |  HOLD:  SYS-03   SYS-04                                                       |  |
|  |  NO-GO: SYS-05                                                                |  |
|  |                                                                               |  |
|  |         [  GO FOR LAUNCH  ]  [  HOLD FOR REVIEW  ]  [  SCRUB LAUNCH  ]        |  |
|  +===============================================================================+  |
+-------------------------------------------------------------------------------------+
```

**Interaction model:**
- Each subsystem clickable -- drills into claims + evidence
- Warning Board has real-time alerts
- GO/HOLD/NO-GO propagates from subsystem status -- can't launch with NO-GO
- Flight Log is immutable append-only timeline
- WAIVE lets you accept risk explicitly (with sign-off recorded)
- Telemetry bars animate in real-time

**Strengths:** Maximum operational clarity, decision-forcing, no ambiguity
**Best for:** High-stakes production deploys, on-call review culture


---

## VISION 8: THE NEURAL NETWORK

*Reasoning is literally a brain. Neurons are claims. Synapses are evidence links. Firing neurons = strong evidence. Dark neurons = gaps.*

```
+-------------------------------------------------------------------------------------+
|                                                                                     |
|                         N E U R A L   R E V I E W                                   |
|                                                                                     |
|                                 +-----+                                             |
|                                 |  ?  |                                             |
|                           +-----+INTEN+-----+                                       |
|                           |     |     |     |                                       |
|                           |     +--+--+     |                                       |
|                           |        |        |                                       |
|                     ======|========|========|======                                  |
|                    /      |        |        |      \                                 |
|               +----+  +---v--+  +--v---+  +v-----+  +----+                          |
|           +---| C1 |--| C2   |--| C3   |--| C4   |--| C5 |---+                     |
|           |   |    |  |      |  |      |  |      |  |    |   |                     |
|           |   | ## |  | ##   |  | ::   |  | ##   |  | :: |   |                     |
|           |   | ## |  | ##   |  | ..   |  | ##   |  | .. |   |                     |
|           |   +-+--+  +--+--+  +--+--+   +--+--+  +--+-+   |                     |
|           |     |  \      |  \     |        |/       / |     |                     |
|           |     |   \     |   \    |       /|      /   |     |                     |
|           |     |    \    |    \   |     /  |    /     |     |                     |
|           |  +--v--+ +v---v+ +-v--v+ +-v--v+ +v---+        |                     |
|           |  | E1  | | E2  | | E3  | | E4  | | E5 |        |                     |
|           |  |     | |     | |     | |     | |    |        |                     |
|           |  | !!  | | !!  | |     | | !!  | |    |        |                     |
|           |  |FIRE | |FIRE | |DARK | |FIRE | |DARK|        |                     |
|           |  |     | |     | |     | |     | |    |        |                     |
|           |  |code | |bench| | --- | |test | |--- |        |                     |
|           |  | :47 | |mark | |     | |:112 | |    |        |                     |
|           |  +-----+ +-----+ +-----+ +-----+ +----+        |                     |
|           |                                                  |                     |
|           |     +------+          +------+                   |                     |
|           +-----| D1 X |----------| D2 X |-------------------+                     |
|                 |reject|          |reject|        +------+                          |
|                 |global|          |per-  |--------| D3 V |                          |
|                 |      |          |role  |        |adopt |                          |
|                 +------+          +------+        |route |                          |
|                  (dead)            (dead)          +------+                          |
|                                                  (active!)                          |
|                                                                                     |
|         +-------------------------------------------+                               |
|         |  NEURAL ACTIVITY: 68%                     |                               |
|         |  ######################__________          |                               |
|         |  2 dark neurons -- review cannot complete  |                               |
|         |  until they fire or are explicitly pruned  |                               |
|         +-------------------------------------------+                               |
|                                                                                     |
|  [STIMULATE DARK NEURON]   [PRUNE]   [VIEW FULL NETWORK]   [RENDER VERDICT]        |
+-------------------------------------------------------------------------------------+
```

**Interaction model:**
- Hover a neuron -- all its synapses glow
- Dark neurons pulse to draw attention
- "Stimulate" = request evidence from author
- "Prune" = mark as not needed
- Neural activity % is review confidence
- In WebGPU: animated pulses traveling along synapse lines, firing animations, particle effects
- The network self-organizes with force-directed layout

**Strengths:** Visceral, gaps are impossible to miss, beautiful in WebGPU
**Best for:** Visual thinkers, complex multi-threaded reviews


---

## VISION 9: THE COMIC BOOK / STORYBOARD

*The change is told as a story. Each panel is a step in the reasoning. The reviewer reads it like a comic.*

```
+-------------------------------------------------------------------------------------+
|                                                                                     |
|   +==============================================================================+  |
|   |                    THE ADVENTURES OF PR #847                                 |  |
|   |              "THE AUTH REFACTOR SAGA"    by @sid                              |  |
|   +==============================================================================+  |
|                                                                                     |
|   +--- PANEL 1 ----------------+  +--- PANEL 2 --------------------------------+   |
|   |                             |  |                                            |   |
|   |   THE PROBLEM               |  |   THE FIRST ATTEMPT                        |   |
|   |   ===========               |  |   =====================                    |   |
|   |                             |  |                                            |   |
|   |   +-----------------+       |  |    "What if we just   +--------------+     |   |
|   |   |  GLOBAL AUTH    |       |  |     whitelist every-   | WHITELIST    |     |   |
|   |   |  [V] [V] [V]   |       |  |     thing?"           | /api/admin V |     |   |
|   |   |  ALL SAME RULE  |       |  |                       | /api/user  V |     |   |
|   |   +-----------------+       |  |    +----------+       | /api/pub   V |     |   |
|   |                             |  |    |  @sid    |       | /api/...  V  |     |   |
|   |    "One policy to          |  |    |  :worry: |       | (100 more)   |     |   |
|   |     rule them all...       |  |    +----------+       +--------------+     |   |
|   |     what could go wrong?"  |  |                                            |   |
|   |                             |  |     X REJECTED -- doesn't scale!           |   |
|   +-----------------------------+  +--------------------------------------------+   |
|                                                                                     |
|   +--- PANEL 3 ----------------+  +--- PANEL 4 --------------------------------+   |
|   |                             |  |                                            |   |
|   |   THE SECOND ATTEMPT        |  |   THE SOLUTION                             |   |
|   |   ==================        |  |   ============                              |   |
|   |                             |  |                                            |   |
|   |   "Chain middleware         |  |    +-----------------------------+          |   |
|   |    per role!"               |  |    |  ROUTE SCHEMA MAP          |          |   |
|   |                             |  |    |                             |          |   |
|   |   req > [auth] > [role] >  |  |    |  /api/admin > {:scopes     |          |   |
|   |       > [scope] > [rate]   |  |    |                ["admin"]}   |          |   |
|   |       > [log] > handler    |  |    |  /api/user  > {:scopes     |          |   |
|   |                             |  |    |                ["read"]}   |          |   |
|   |    BENCHMARK:               |  |    |  /api/pub   > :public     |          |   |
|   |    p99: 6ms (was 2ms)      |  |    +-----------------------------+          |   |
|   |                             |  |                                            |   |
|   |    +----------+             |  |    +----------+  "O(1) lookup.             |   |
|   |    |  @sid    |             |  |    |  @sid    |   Composable.              |   |
|   |    |  :scream:|             |  |    |  :cool:  |   Testable."              |   |
|   |    +----------+             |  |    +----------+                            |   |
|   |                             |  |                                            |   |
|   |    X REJECTED -- 3x slower! |  |    V ADOPTED!                              |   |
|   +-----------------------------+  +--------------------------------------------+   |
|                                                                                     |
|   +--- PANEL 5 ----------------+  +--- PANEL 6 --------------------------------+   |
|   |                             |  |                                            |   |
|   |   THE EVIDENCE              |  |   THE CLIFFHANGER                          |   |
|   |   ============              |  |   ===============                           |   |
|   |                             |  |                                            |   |
|   |   V Expired token > 401    |  |    +----------+                             |   |
|   |   V Wrong scope > 403      |  |    |  @alex   |                             |   |
|   |   V Valid token > 200      |  |    |  :think: |                             |   |
|   |   V Malformed > 401        |  |    +----------+                             |   |
|   |                             |  |                                            |   |
|   |   +----------+              |  |    "But what about refresh tokens          |   |
|   |   |  @sid    |              |  |     with mixed scopes...?"                 |   |
|   |   |  :flex:  |              |  |                                            |   |
|   |   +----------+              |  |    !! LEGACY TOKENS IN PROD                |   |
|   |                             |  |    !! MIXED SCOPE UNTESTED                 |   |
|   |   "Ship it?"                |  |                                            |   |
|   |                             |  |    TO BE CONTINUED...                      |   |
|   |   [VIEW TEST CODE]          |  |    [ADDRESS RISKS]  [APPROVE ANYWAY]       |   |
|   +-----------------------------+  +--------------------------------------------+   |
|                                                                                     |
|   < PREV PAGE    PAGE 1 of 1    NEXT PAGE >    [FULL DIFF VIEW]    [VERDICT]       |
+-------------------------------------------------------------------------------------+
```

**Interaction model:**
- Panels scroll horizontally like a comic reader
- Each panel is a beat in the story
- Click any panel to expand -- code appears inline
- CLIFFHANGER panel = risks. "TO BE CONTINUED" blocks merge until resolved
- Emoji faces track author's emotional journey
- Author can add panels to tell their story

**Strengths:** Narrative engagement, memorable, makes decisions feel like a journey
**Best for:** Onboarding new team members, understanding WHY decisions were made


---

## VISION 10: THE LIVING TREE

*The change grows like a tree. Trunk is intent. Branches are claims. Leaves are evidence. Dead branches are rejected alternatives. Fruit = decisions. Rot = risks.*

```
+-------------------------------------------------------------------------------------+
|                                                                                     |
|                            THE REASONING TREE                                       |
|                         PR #847 -- Auth Refactor                                    |
|                                                                                     |
|              [FRUIT] Decision:                 [FRUIT] Decision:                    |
|              Adopt per-route                   All tests pass                        |
|                  |                                 |                                 |
|              +---+---+                         +---+---+                             |
|             /| LEAVES|\                       /| LEAVES|\                            |
|            / |evidenc| \                     / |evidenc| \        +----------+        |
|           /  | rich! |  \                   /  |partial|  \       | RISK     |        |
|          /   +---+---+   \                /   +---+---+   \      | Legacy   |        |
|         /        |        \              /        |        \     | tokens   |        |
|  +------+-+ +----+---+ +--+------+  +---+----+ +-+------+  \    | (rotting |        |
|  |LEAF    | |LEAF    | |LEAF     |  |LEAF    | | LEAF   |   \   |  branch) |        |
|  |code    | |bench   | |test     |  |test    | | GAP!   |    \  +----------+        |
|  |mw:47   | |p99=2ms | |expired  |  |scope   | |refresh |     \     /               |
|  +----+---+ +----+---+ +----+---+   +---+---+ |token   |      \   /                |
|       |          |          |            |     |missing |       \ /                  |
|       |          |          |            |     +--------+        X                   |
|       +----------+----------+------------+         ^           / \                   |
|                       |                            |          /   \                  |
|                       |                            |         / +---+-+\              |
|                  +----+-------------------------------+------  | RISK | |             |
|                  |                                    |        | mixed| |             |
|                  |            T  R  U  N  K           |        | scope| |             |
|                  |                                    |        +------+ |             |
|                  |    INTENT: "Per-route validation"  |                 |             |
|                  |                                    |                 |             |
|                  +----------------+-------------------+                 |             |
|                                   |                                    |             |
|                   /\              |                                    |             |
|             +----/  \----+       |                                    |             |
|             |  / STUMP \  |      |                                    |             |
|             | / DEAD     \|  +---+----------+                         |             |
|             |/  BRANCH   \|  | STUMP        |                         |             |
|             |  "global    |  | DEAD BRANCH  |                         |             |
|             |  whitelist" |  | "per-role    |                         |             |
|             |  (cut off,  |  |  chains"     |                         |             |
|             |   stump     |  | (cut off,    |                         |             |
|             |   visible)  |  |  stump)      |                         |             |
|             +-------------+  +--------------+                         |             |
|                                                                       |             |
|  =====================================================================|============ |
|                               R O O T S                               |             |
|                                                                       |             |
|            +--------------+  +--------------+  +--------------+       |             |
|            | Issue #312   |  | Security     |  | @sid's       |       |             |
|            | "route-level |  | audit Q3     |  | experience   |       |             |
|            |  auth needed"|  | finding      |  | with auth    |       |             |
|            +--------------+  +--------------+  +--------------+       |             |
|                                                                                     |
|  TREE HEALTH: ##############________ 72%    DEAD BRANCHES: 2    ROT: 2 spots       |
|                                                                                     |
|  [WATER (add evidence)]  [PRUNE (close risk)]  [HARVEST (approve)]                 |
+-------------------------------------------------------------------------------------+
```

**Interaction model:**
- In WebGPU: actual animated tree -- sways, leaves rustle, fruit glows
- Dead branches are grey stumps, clearly severed
- Rotting spots pulse red with particle effects
- Click any branch to see its evidence
- "Water" = request more evidence from author
- "Prune" = close/accept a risk
- "Harvest" = approve the change
- ROOTS show origin context -- why does this tree exist?
- Temporal playback: watch the tree GROW from seed

**Strengths:** Organic, intuitive, makes gaps feel like wounds, beautiful
**Best for:** Long-lived features, understanding evolution over time


---

## CROSS-CUTTING ANALYSIS

### What all 10 visions share

1. **Gaps are visible** -- hollow rock, dark neurons, cold cases, dead branches, missing exhibits, NO-GO subsystems. The most powerful feature isn't showing what's there -- it's making what's MISSING impossible to ignore.

2. **Verdict gate** -- you can't approve until conditions are met. Every design has a forcing function that prevents rubber-stamping.

3. **Progressive disclosure** -- surface first, depth on demand. No vision dumps everything at once.

4. **Rejected alternatives are preserved** -- fossils, dead ends, stumps, dismissed cases. The reasoning that DIDN'T happen is as important as what did.

5. **Temporal dimension** -- every design has some notion of "this evolved over time" rather than being a static snapshot.

### Which metaphor changes what behavior

| Metaphor | Encourages | Discourages |
|---|---|---|
| Crime Board | Free exploration, connection-making | Systematic coverage |
| Metro Map | Systematic traversal, completeness | Creative leaps |
| Geology | Depth control, self-paced | Quick scanning |
| Courtroom | Adversarial rigor, completeness | Speed, trust |
| Music Score | Temporal awareness, rhythm | Random access |
| Newspaper | Speed, orientation | Deep investigation |
| Mission Control | Operational clarity, decisiveness | Nuance, exploration |
| Neural Network | Pattern recognition, gap detection | Linear reasoning |
| Comic Book | Narrative engagement, empathy | Formality, precision |
| Living Tree | Organic growth, nurturing | Urgency, cutting |

### Buildability on current stack (WebGPU + Rama + Missionary)

All 10 are buildable. The rendering primitives (rects, text, lines, particles) already exist in the WebGPU editor. Rama provides the event log for temporal playback. Missionary provides reactivity for live updates. The question isn't technical feasibility -- it's which metaphor best serves the cognitive flow of code review.
