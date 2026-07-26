# multi-cascade — declaration slice (R1) · CONTRACT

STAGED 2026-07-26 (Fable, the monster-round session). Dark-lane organ #1
(board block; decisions.md "How engine work lands"; terrain: organ III /
CORE P in `engine-terrain.md`). Binding with decisions.md; this contract
governs over every derived artifact.

## 1. Purpose

Softland's server acts are welded to their triggers: the episode-turn lane
hard-calls the autotag act inline (`server_jetty.clj:988`, a `future` +
`try/catch` inside `run-episode-turn`). This slice mints the **cascade
declaration table** — acts declared as data against named triggers — and
`react!`, the one dispatch point. Emission sites say WHAT happened; the
table says what follows. Autotag becomes **row #1, behavior-identically**:
same guard, same threading, same idempotency bytes, same logs' facts.

Consumers, in order: (1) the episode-turn lane (autotag, day one) ·
(2) episode-retry — the durable runner's first honest customer (R2, not
this slice) · (3) zoom band-crossing acts (space punt table) · (4) the
consolidator on the clock pulse (R4). (2)–(4) consume the TABLE SHAPE,
not code built now.

Non-goals / refusals (each an extension point): NO durable runner, no
executor/thread machinery beyond the call site's existing `future`-per-act
(R2) · NO log-consuming topology — R1 observes in-process code points only
(R2, via /rama + rama-pitfalls) · NO rows-as-editable-material — the table
is KERNEL code this slice; material arrives at 2–3 real rows (R3) · NO
clock pulse (R4) · NO new durable event kinds, depot writes, PState or
topology changes of any kind.

## 2. Placement ruling

NEW namespace `src/app/server/cascade.clj` — server-side plain Clojure.
Not `.cljc`: no client consumer exists in this slice. Not inside
`material_circulation.clj`: circulation is one HAND; the table is the
loom — separate organs, and circulation stays byte-untouched (§4 T1). The
table is kernel-class like `verb_registry.cljc` and is deliberately NOT
added to `default-material-policy-paths` (same rationale recorded there
for the kernel files: listing it would cry escape on every table edit and
teach readers to ignore the alarm; rows-as-material at R3 is where policy
tier begins). Reversal cost: one namespace + one call-site swap — revert
by commit (mechanism ceremony, see §7).

## 3. The cut

- **Row grammar** (a row is a plain map; the table a vector — declaration
  order is fire order):
  `:cascade/id` (namespaced kw, unique) · `:cascade/trigger` (kw naming
  the emission point) · `:cascade/handler` (fully-qualified SYMBOL,
  resolved at fire time via `requiring-resolve` — loads pure, no
  namespace cycle) · `:cascade/effect-class` (from the verb-registry
  effect-class vocabulary; autotag = `:external-via-derived-worker`) ·
  `:cascade/idempotency` (STRING documenting the story + its scope and
  transition law) · `:cascade/actor` (the acting identity string).
- **`react!`**: `(react! ctx trigger payload)` — for each row whose
  `:cascade/trigger` matches, in table order: spawn `(future (try
  ((requiring-resolve handler) ctx payload) (catch Throwable t (log ...))))`
  — exactly the call site's current semantics (async, isolated,
  best-effort, never delays the caller). Returns a vector of
  `{:cascade/id … :dispatched? true}` receipts immediately. Logs
  `[CASCADE]` per fired row with the row id + trigger.
- **Row #1**: `:cascade/material-autotag`, trigger `:episode/turn-durable`,
  handler = the (made-public) autotag entry in `server_jetty.clj`; the
  existing guard `(and (nil? gold-receipt) rk-rt)` moves INTO the handler
  head (returns `:skipped` receipt in the result map when it declines) —
  the row fires on every emission, the handler owns its own decline, and
  the call site becomes one line: build payload, `react!`.
  Idempotency string: "run-id derived in material_circulation from
  (object-key, input-hash, autotag-version, salt) — identical input = one
  run forever; recorded runs never re-invoke the adapter; a NEW key is
  minted only by input change or explicit salt (no lifecycle
  transitions)."
- **Emission payload** at the call site: `{:object-key (:address durable)
  :source-unit-id … :text … :receipt (:receipt durable) :gold-receipt …}`;
  `ctx` carries runtimes `{:oc-rt … :rk-rt …}`. The llm-runtime `delay`
  STAYS private in `server_jetty.clj` (deref'd inside the handler, as
  today) — runtime handles never live in the table (§4 T2).
- **Enumeration**: `(cascade/rows)` → the declared table, printable; this
  is the serve surface until R3 (a console read, not facet material).

## 4. Traps ledger (cite T-numbers in code comments)

- **T1 — idempotency bytes are durable truth.** Naive: "clean up" id
  derivation while refactoring. Failure: run-id/record-id/bundle-id/
  turn-id derivations feed DURABLE records — any byte change makes every
  historical record invisible to the replay check and the adapter
  re-fires against the model (real spend, duplicate silver edges).
  Ruling: `material_circulation.clj` is READ-ONLY this slice; the handler
  passes through the exact argument shapes; G1 byte-compares derived ids.
- **T2 — loads pure, or it isn't dark.** Naive: table rows closing over
  runtime handles, or a `defonce`/`delay` boot in cascade.clj. Failure:
  namespace load runs top-level forms — a boot side effect makes the
  organ fire at require time (dark-organ law, decisions.md). Ruling:
  rows are data + SYMBOLS; runtimes ride `ctx` at call time; cascade.clj
  holds zero stateful top-level forms; G3 proves it.
- **T3 — threading is the call site's, not the table's.** Naive: an
  executor/queue inside react!. Failure: that is the durable runner (R2)
  arriving unreviewed — retry/ordering semantics minted silently.
  Ruling: react! reproduces the current `future`-per-act shape exactly;
  anything more is a stop clause.
- **T4 — one row's throw must not starve siblings.** Naive: bare `doseq`
  dispatch. Failure: first throwing handler kills the rest of the
  cascade for that emission (invisible with one row, lethal at two).
  Ruling: per-row future + try/catch + `[CASCADE][FAILED]` log; G4.
- **T5 — trigger names are code points, not durable vocabulary.** Naive:
  minting a "cascade-fired" durable event or writing trigger kinds into
  truth. Failure: durable-touch = cutover-class creep (decisions.md
  boundary test) — R1's whole safety case is zero durable change.
  Ruling: triggers are in-process names; G7 scans the diff.
- **T6 — behavior-identity includes the DECLINE path.** Naive: guard at
  the call site (as today) with the row firing conditionally. Failure:
  the emission becomes conditional — future rows on the same trigger
  silently never fire when autotag's guard declines. Ruling: emission is
  unconditional; each handler owns its decline (`:skipped`); G1 covers
  both branches.
- **T7 — the table is enumerable or it is folklore.** Naive: rows as
  private code only. Failure: the acts stratum becomes invisible glue
  again — the exact disease this organ treats. Ruling: `(cascade/rows)`
  public + G5.

## 5. Acceptance gates (partition: ALL of G1–G8 run in P1 — sum-checked
here at staging; owners named per gate)

- **G1 — behavior-identity, both branches (suite).** Through-table
  autotag with the canned `:lines` seam produces the same result-map
  facts (status, run-id, record-id, edge relation-id) as a direct
  `autotag-material!` call on identical input; derived ids compare as
  BYTES. Decline branch: gold-receipt present → handler returns
  `:skipped`, adapter never called. Owner: implementer.
- **G2 — replay convergence (suite).** Two identical `react!` emissions:
  second yields `:already-recorded`, `:adapter-called? false` (the
  existing pattern at `material_circulation_test.clj:311` re-driven
  through the table). Owner: implementer.
- **G3 — loads pure (suite + style).** Fresh classload of
  `app.server.cascade` starts no runtime, mints no state (assert: no
  llm-module boot observable; style scan: zero `defonce`/effectful
  top-level forms in cascade.clj — scan STOPS at cascade.clj). Owner:
  implementer.
- **G4 — isolation + order (suite).** Fixture table, two rows on one
  trigger, first handler throws → second fires; receipts show
  declaration order; `[CASCADE][FAILED]` logged for the thrower. Owner:
  implementer.
- **G5 — enumeration (suite).** `(cascade/rows)` lists row #1 with
  trigger, `:external-via-derived-worker`, actor, idempotency string.
  Owner: implementer.
- **G6 — live parity (dev app).** A real Ctrl+Enter turn on the dev app:
  `[CASCADE]` fires row #1; autotag outcome fields match the pre-cut
  `[CIRCULATION][AUTOTAG]` facts; the turn's SSE ack timing is not
  delayed (act stays async). Owner: implementer drives; **Fable
  re-drives at gate review**.
- **G7 — zero durable touch (style, diff-scoped).** The diff contains no
  depot/PState/topology edit, no new durable event kind, and
  `material_circulation.clj` + `verb_registry.cljc` byte-untouched. Scan
  stops at this package's diff. Owner: implementer; Fable re-checks.
- **G8 — dark capacity honest (suite).** A TEST-ONLY row on a trigger
  nothing emits stays inert through the full suite and enumerates as
  declared (the dark-interval specimen in miniature; the production
  table ships with exactly row #1). Owner: implementer.

## 6. Stop clauses (escalate per the work-package skill; never improvise)

- The manifest (§8) mismatches disk — call shape, guard, threading
  wrapper, or alias drift at `server_jetty.clj` or llm-module arities.
- Any need to touch the rung-3 in-flight fence set (§7), or
  `material_circulation.clj`, or `verb_registry.cljc`.
- Any durable-touch discovered (new event kind, depot write, projection
  change) — this reclassifies work cutover-side; stop, never absorb.
- react! provably cannot reproduce the call site's semantics without
  executor machinery (T3) — that is R2 arriving early; stop for ruling.

## 7. Landing sort + dark-lane bookkeeping (decisions.md "How engine work
lands")

This slice is **MECHANISM-shaped**: it merges by deploy-under-proof (G1
replay/parity proofs; revert by commit), not by activation — there is no
material row selecting it (registry law untouched). Boundary test run at
staging: zero durables touched (G7 enforces). Board: dark-lane block,
organ #1. **Interval clause**: the organ's dark capacity (rows beyond #1)
activates at the first NEW row (episode-retry or first band-act); THAT
session appends the dark-interval lessons to the board note and
`engine-terrain.md` Lineage — did the table drift under main, was it
remembered, were proofs still green, was the board line accurate.

## 8. Input manifest (implementer re-verifies EVERY line against disk
before code — line numbers drift)

- `src/app/server_jetty.clj` :800–:841 (`ambient-autotag-runtime` delay +
  `run-ambient-autotag!`) and :988–:998 (the call: `when` guard
  `(and (nil? gold-receipt) rk-rt)` → `future` → `try/catch Throwable` →
  log `[CIRCULATION][AUTOTAG-FAILED]`); the surrounding
  `run-episode-turn` flow :843+ (durable-BEFORE-agent order).
- `src/app/server/rama/material_circulation.clj` :42–:43 (versions/actor)
  · :571–:605 (`autotag-material!` signature, run-id/record-id
  derivation, `:already-recorded` + `:terminal-without-record` branches)
  · :467–:544 (input/run-id/prompt/parse fns — READ-ONLY).
- `src/app/shared/verb_registry.cljc` :18–:36 (effect-class vocabulary
  incl. `:external-via-derived-worker`).
- `test/app/material_circulation_test.clj` :305–:335 (the replay-pattern
  tests G2 re-drives).
- Threading/SSE context: the turn lane's SSE contract at
  `server_jetty.clj` :780–:798 (the act must never delay the ack).
- Suite classification (added by RULING 2026-07-26 — the staged manifest
  omitted the runner's fail-closed registry): `test/app/test_runner.clj`
  :214–:247 (`test-inventory` globs every `*_test.clj`;
  `assert-inventory!` fails closed on any namespace absent from all three
  tier registries) · :64 `isolation-exceptions` (namespace → one-line
  reason map).

## 9. Handoff

Single implementation phase **P1** (all gates G1–G8; this contract's §5
partition IS the sum-check) in a fresh context, after rung 3 lands
(shared-tree sequencing; fence set §7/§6). Allowlist — NEW:
`src/app/server/cascade.clj` + `test/app/cascade_table_test.clj`.
EDITED: `server_jetty.clj` (the autotag block + call site ONLY: privacy
flip, guard move, one-line emission) · `test/app/test_runner.clj`
(RULING 2026-07-26: EXACTLY one classification entry — add
`app.cascade-table-test` to `isolation-exceptions` with a one-line
reason; it boots fresh OC/RK/LLM runtimes, the machine-cut/circulation
isolation class. No runner-semantics change; no other namespace's tier
moves; the gate diff-scans this file to that one entry). FENCED (rung-3
WIP, do not touch):
`binding_material.cljc` · `ground.cljs` · `facet_master.clj` ·
`space_material.cljc` · `face_wiring.cljs` + their test files. Then: ONE
falsification finder aimed at the genuinely-new machinery (react!
dispatch + the behavior-identity cut), then Fable gate review (re-drive
G6, re-check G7, falsification pass), then close + retro per the skill.
After green: the organ sits with row #1 live and dark capacity declared;
next slices pull per the terrain ladder (R2 runner at episode-retry, R3
rows-as-material at 2–3 rows, R4 clock) — each its own contract.
