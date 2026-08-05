# studio — independent gate record (P1)

Date: 2026-07-31 → 2026-08-01 (one gate session spanning midnight)
Gate: Fable, fresh context (the P1 report was its boot input — used as a
prior-pass record, never as authority; the code was read in full)
Subject: the uncommitted P1 package on `docs/current-mental-model-local`
at HEAD `10c27b7`; contract SHA re-verified at open:
`f16ed4d0345ef96b28cbedbf60a2ba66769f04a764ac375589d5da9dcb247454`.

## 0. Tier ruling

CONTRACT §8 names the session "slim gate" but binds the tier to the P0
close decision — and P0 recorded the escalation ("FULL gate tier": P1 had
to change the durable promotion/report owner and `facet-master/activate!`,
the explicit FULL trigger). This gate therefore ran FULL substance:
independent full-suite run, fence re-hash against P0's own pins, full diff
read, an independent live probe of the one layer no JVM bank covers, the
G6 fresh falsifier, and independent reproduction of every finding that
drove the verdict. NOT independently re-run: the headed browser drive
(G5). Grounds: §8 assigns G5 to the P1 implementer; the isolated rig was
torn down at P1 close; the durable half of every headed claim was
re-driven here in-JVM against real runtimes; and G7 — the very next act —
is a headed drive of the real land by the package's own design. Free
falsifiers: Sid's G7 itself, and Sid's next shadow compile (the G2
Electric-wall carve-out written into §8).

## 1. Independent re-verification (this session's own runs)

| Check | Result |
|---|---|
| G1 FULL suite (own run) | 53 namespaces · 506 tests · 7,247 assertions · 0 failures · 0 errors; shards 0–5 green; 3 registered flakes attempt-1 green; flake registry unchanged |
| G2 CLJS compile (own run) | 277 files, 0 warnings (fully cached — byte-identical sources to P1's compile) |
| G3 protected hashes (own re-hash vs P0.md:596–600, not P1's copy) | all five exact: `relation_kernel.clj ab283b47…` · `episode.clj 7fce46b2…` · `cascade.clj 5fa0c085…` · `verb_registry.cljc f7282a79…` · `matter_room.cljc acee5f77…` |
| G3 x-ray spans | no diff hunk intersects old-line spans 573–987 or 3578–3659 (hunk positions machine-listed); unchanged old lines are byte-identical to P0's pinned base by construction |
| G3 fence sweep | no new endpoint/verb/primitive/module/depot/PState/topology/source-family/import-key-family/reserved-gesture row; `face_primitives.cljc`, `facet_masters.cljc`, `activation_event.cljc`, `binding_material.cljc` untouched; `git diff --check` clean; `env.clj` ignored (`.gitignore:16`), never read |
| T6 string scan (own run) | zero `fm:*`/`rev:*`/32-hex literals in the two studio namespaces and the added Ground hunks |
| Live dispatch probe (new — no JVM bank loads `app.server-jetty`) | real handler fns on an isolated ObjectContainer runtime: unknown op refused · non-draft birth refused · birth accepted · marker-first promote accepted with authoritative prior revision · **activate WITHOUT report refused `:studio-report/missing` through the real endpoint fn** · PASS report accepted · guarded flip carries exactly the owner-computed report ground · rollback via recovery offer restores prior · release accepted |
| Seven-op closure | `anatomy/edit-candidate` refuses any op outside the closed set before composing; `compose-edits` routes every gesture through it |

## 2. G6 — fresh falsifier

Launched per §8: fresh context, ONE finder, default-fail, aimed at the
gesture→grammar compiler and the test-gated accept path including
required-vs-exempt activation classification. Full round banked verbatim
in `G6.md` (its pass-evidence list is as load-bearing as its findings).
Falsifier verdict: **FAIL** — 2 HIGH, 6 MEDIUM, 3 LOW, both HIGHs
reproduced by the finder on a real runtime.

## 3. Gate adjudication (every finding independently verified here)

**HIGH-1 — caller-elected gating — reclassified LITERAL-vs-INTENT, not a
P1 defect.** Reproduced here: unmarked ordinary import of studio-shaped
bytes + activate → accepted, gate never required, empty grounds. But the
contract's authorizing sections pre-bless exactly this: S1 requires the
discriminator to leave "existing bootstrap, migration, rollback, and
x-ray activation paths … behavior-identical"; T3's second sentence
REQUIRES "one existing non-Studio activation and rollback replay
unchanged"; P0's recorded ruling says "marker absence preserves …
ordinary non-Studio activation." The ordinary master-candidate lane IS
the x-ray's own retain/activate flow — it must stay open. §3f/TRAP-2's
"fail-closed" reads on the Studio lane: a MARKED candidate can never flip
without a matching durable PASS (proven), and marked bytes cannot be
laundered through the plain hand (content-derived revision ids pin the
marker to the bytes forever). What remains is the pre-existing substrate
reality that a deliberate actor with matter-room access can change a
default through the engineer's lane — v0-true for every surface on the
land, not introduced by P1. Banked as the design-boundary residue with
the falsifier's demonstration attached; the honest sentence for the trust
story: **law 4 currently protects against accident, not against a
deliberate actor on the engineer's lane** — the next arc's authn/actor
work is the extension point.

**HIGH-2 — FAIL→PASS laundering via instance-set re-mark — CONFIRMED
DEFECT, repaired in place.** Reproduced here end-to-end before repair:
`promote(C,I1) accepted · FAIL report accepted · activate refused
[:studio-report/not-passing] · re-promote(C,I2-fabricated) accepted, same
revision · PASS report accepted · activate accepted · FAILED candidate is
the default`. Root cause: gate container ADDRESS is keyed by candidate
revision alone while marker BYTES include the instance set, so a
different-set re-promotion is a fresh import that supersedes the
FAIL-bearing marker; every earlier layer (P0's round, the implementer,
the test bank, this gate's own diff pass) probed the same-set REPLAY axis
— only the falsifier varied the instance-set axis. Violates the
contract's pinned report semantics (S1: conflicting-id refusal with "the
accepted report remaining current"; the bank's own stated rule "a failed
report may not be rewritten into PASS — the passing path uses a fresh
candidate revision") and the marker's own "immutable" docstring. Repairs
R1a/R1b below.

**MEDIUM dispositions.** M5 (RAF throw strands the three-instance
preview) and M6 (refused durable write leaves a phantom client preview —
also independently found by this gate's own pass) — REPAIRED in place
(R2, R3). M1 (instance ids never resolved against durable truth) —
residue: with R1 in place fabricated sets no longer launder a FAIL, and
first-promotion fabrication is subsumed by M3's v0 honesty boundary;
route = the agent-eyes package (§2 extension point), falsifier = one
server-side existence read at promote/report time. M2 (five report rows
candidate-independent) — residue: v0 validator depth; the closed schema
admits it; deepens with agent-eyes; falsifier = a candidate that breaks
interaction while compiling and rendering. M3 (report content
caller-asserted, no authn) — the v0 trust boundary the contract itself
sets ("client validation is evidence, not authority" — the guard proves
durable record consistency, not that checks physically ran); becomes real
work when agent-authored writers multiply. M4 (duplicate lane
op-receipts literal) — residue, cosmetic: the duplicated FORM is itself
prior compiler output and re-enters the grammar on every subsequent edit;
falsifier = receipt parity assertion on the duplicate lane.

**LOW dispositions.** L1 (studio branch skips the closed act grammar's
common validation; `matter_room.cljc` docstring stale) — residue;
`normalize-actor` coerces at the owners; align at the next matter-room
touch. L2 (no rebase/lineage check on draft instance edits; two hands
last-writer-wins) — residue for the P2 accept-lane work; TRAP-7's rebase
law currently holds only client-side. L3 (birth handle is a fixed
world-space slot at 24,24) — **pre-G7 advisory to Sid**, not a patch:
if the real land's camera is far from origin, the handle is off-screen
and the cold script's first step is unreachable. Surface this to Sid
BEFORE he runs G7 only as logistics (where to stand), never as script.

## 4. Gate's own falsification pass — additional residues (non-blocking)

- Same-bytes retry after a FAIL is impossible forever (report id is
  candidate+set identity; FAIL is terminal by design — R1 now enforces
  it). Contract-pinned; the lived cost (a transient validation failure
  poisons that candidate+set pair) is P2 material.
- After put-back, re-accepting the SAME unchanged candidate refuses at
  re-promotion (its marker already carries the report). Fail-closed UX
  consequence of the same pinned law; bank for P2.
- Accept-chain refusal labels overwrite: an early-stage refusal ends as
  "Accepting the component could not finish" (details preserved).
- Word-rider durable acts wear `{:actor/id "studio" :actor/type :human}`;
  honest actor routing belongs with the real agent lane.
- TOCTOU window between `studio-gate-evaluation` and the pointer append
  inside one `activate!` call — a racing gate-container tamper could slip
  a stale-PASS flip; requires direct runtime append access (any such
  actor already owns HIGH-1's lane); post-tamper state refuses via the
  audit content-hash check. Falsifier: re-read-and-compare immediately
  before the append if agent-authored writers ever multiply.
- `data-only?` admits sets inside check/flow receipts; set print order is
  not canonical across processes, so an exact semantic retry could read
  as changed bytes and be refused as a conflict (fail-closed direction).
- The "report bound to a DIFFERENT candidate" adversary is enforced
  structurally (reports live inside the marker addressed by the
  candidate's own revision id + mutual mismatch checks) and exercised at
  submission; a forged-raw-edit variant at the guard is the named cheap
  falsifier (R1b's history check now also covers the raw-import door).
- TRAP citations in code comments are sparse (TRAP-6 twice pre-repair;
  R1 adds TRAP-2/TRAP-7 citations at the guard).

## 5. Repair phase (in-session; fresh implementer subagent, Sonnet lane)

Enumerated by this gate, applied by a fresh-context implementer, verified
independently below (author/reviewer separation held):

- **R1a** — promotion pre-append guard: an existing gate container
  refuses re-promotion unless the proposed nil-report marker is
  byte-identical to the current one (`:studio/gate-already-reported` /
  `:studio/gate-marker-conflict`); nothing is appended on refusal.
- **R1b** — canonical-owner history terminality: `studio-gate-evaluation`
  walks the gate container's revision history (fail-closed on a full page
  or unparseable row) and refuses on any historical instance-set
  mutation, superseded report id, or historical FAIL
  (`:studio-gate/marker-mutated` / `:studio-report/superseded` /
  `:studio-report/failed-terminally`) — the first durable report is
  terminal per candidate revision, closing the raw-import door too. The
  unmarked path gains no reads and stays behavior-identical.
- **R2** — the RAF validation callback try/catches; a throw resolves into
  the existing refusal path, and the terminal `.catch` also clears the
  three-instance preview: no path strands preview wear.
- **R3** — refused durable draft writes revert the subject's preview to
  the last durably-acked source (or clear it when none exists) and
  rebuild, so the client never keeps rendering material durable truth
  refused.

### Repair verification (gate session's own runs, post-repair)

- The gate's own laundering repro, re-run at the repaired bytes:
  re-promote(C,I2) REFUSED `:studio/gate-already-reported` · the PASS
  report under I2 refused · direct activation refused with
  `:studio-report/failed-terminally` leading · FAILED-CANDIDATE-IS-DEFAULT?
  **false**.
- Touched banks (own run): 30 tests · 510 assertions · 0 failures — now
  including the exact-replay idempotency adversary, the re-mark attack
  (the gate container's revision proven byte-identical after the refused
  attempt), the raw-runtime marker-v2 bypass refused by the history
  check, and the R3 client-state tests.
- Happy-path live dispatch probe (own re-run): the full birth → promote →
  refuse-without-report → report → guarded flip with the owner's report
  ground → rollback → release chain unchanged and green.
- FULL suite (own run at the final bytes): 53 namespaces · 507 tests ·
  7,269 assertions · 0 failures · 0 errors; registered flakes attempt-1
  green; flake registry unchanged.
- CLJS compile (own run at the final bytes): 277 files, 0 warnings.
- Fence at the final bytes: all five protected hashes still exactly P0's
  pins; `git diff --check` clean; the changed-file set unchanged (the
  repair touched only `facet_master.clj`, `ground.cljs`, the client
  `studio.cljc`, and the two test banks — no protected file, no x-ray
  span, no new file).
- The repair diff was read in full by this gate; author/reviewer
  separation held: a fresh implementer subagent authored exactly what
  this gate enumerated, and the gate verified independently.

## 6. Verdict

**PASS to G7, with residues.** The G6 round's own verdict (FAIL) stands
as that round's honest record, preserved in `G6.md`. The gate's package
verdict is PASS because: the single confirmed defect (HIGH-2) was
repaired under the enumerated-fixes rule by a separate fresh context and
independently re-verified at the final bytes; HIGH-1 is the contract's
own pre-blessed design boundary (classified literal-vs-intent, banked
with its demonstration, never patched); and every remaining finding is a
fail-closed or cosmetic residue with its falsifier named in §3–§4. The
trust-law sentence to carry into G7: a MARKED candidate can never become
the default without a matching, terminal, durable PASS — and the
engineer's unmarked lane remains exactly as open as it was before Studio
existed.

## 7. Evaluation notes (the layers' scorecard)

- **The single aimed falsifier caught the package's only confirmed HIGH**
  after five layers missed it: P0's fresh validation round, the P1
  implementer (who found seven real counterexamples of his own), the
  package's test bank (which asserted the violated invariant by
  construction), the implementer's headed drive, and this gate's own
  full-diff falsification pass (which probed the replay axis of the
  marker but never varied the instance-set axis). The 2026-07-13 sizing
  rule (ONE finder, aimed at the genuinely-new machinery) is again the
  whole story: the finder aimed at the accept path found the accept-path
  hole.
- **The live dispatch probe pattern is cheap and load-bearing**: the only
  layer no JVM bank covers (`server_jetty` studio dispatch) got a real
  receipt for ~3 minutes of wall clock because the dispatch takes an
  explicit runtime handle — worth repeating whenever a package rides an
  existing endpoint.
- **Letter-vs-intent fired again** (HIGH-1), and the classification rule
  absorbed it without a stop: the contract carries both sentences and
  they reconcile on scope. The recurring lesson for contract authoring:
  when a fail-closed law is scoped, name the scope IN the fail-closed
  sentence itself.
- The P1 report's claim "a report for a different candidate/content
  identity" was proven structurally rather than by a run adversary — a
  small overstatement of test coverage, now moot under R1b's history
  check; noted for implementer-artifact hygiene.

## 8. What must still happen (unchanged order)

1. **G7 — Sid, cold, ONE-SHOT, real land, LAST.** Logistics-only
   advisory: start with the camera near the world origin (the draw handle
   is a world-space slot at 24,24 — L3). A G7 FAIL routes to a direction
   session, never a patch.
2. Sid's verbatim reaction lands in this gate record.
3. Sid's commit ruling — code and docs commits separate, docs-local
   branch, never pushed.
4. Post-commit: re-run HEAD-dynamic suites at committed HEAD (close rule).
