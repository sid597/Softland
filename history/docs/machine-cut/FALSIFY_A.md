# FALSIFY_A — machine-cut driver: lifecycle / async-ordering / durability class

Fresh-context adversarial falsification of `src/app/server/rama/machine_cut.clj`
against `CONTRACT.md` §§3-5,8,9 and the CLAUDE.md falsification protocol.
Class: **driver lifecycle + async ordering + durability semantics**.
Verdict discipline: DEFAULT-FAIL. Each finding is CONFIRMED (traced true at
file:line) or PLAUSIBLE (mechanism traced; needs a run to exhibit).

Scope note: `face_projection.clj` / `file_viewer.cljc` boot wiring is Lane-B /
INT, not in this file — where a finding's blast radius reaches them it is
flagged and its manifestation marked PLAUSIBLE.

---

## Severity summary

| # | Sev | Verdict | Class | One line |
|---|-----|---------|-------|----------|
| F1 | HIGH | CONFIRMED | noop-complete guard | `:succeeded` run ⇒ noop-complete with ZERO edge writes, never verifies edges exist/agree; run-state and edge-state are separate ephemeral systems written non-atomically |
| F2 | HIGH | CONFIRMED | reconcile / journal ABA | assert→retract→**re-assert** reuses the first assert's stable journal key `mc:<rel>`; the relation journal drops it; the edge stays `:retracted`, map lies |
| F3 | HIGH | CONFIRMED | WAL replay totality | multi-line WAL with an A→B→A pairing does NOT converge on the last line's state (same journal-ABA); §5.5 "reconstructs the edges exactly" is false |
| F4 | HIGH | CONFIRMED | barrier honesty | every `await-relation` result is discarded; on timeout it returns the stale row WITHOUT throwing ⇒ driver reports `:completed` + bumps epoch on unmaterialized edges |
| F5 | MED | CONFIRMED | validate-output totality | empty-`:responses` pair is counted as a pair and removed from `:unpaired`, but yields ZERO edges ⇒ WAL/receipt counts disagree with what the projection can serve |
| F6 | MED | CONFIRMED (asymmetry) / PLAUSIBLE (harm) | replay scope | `replay-wal!` reconciles **address-wide**; the live path reconciles **window-scoped** ⇒ replay can retract out-of-window edges §5.4 forbids |
| F7 | MED | PLAUSIBLE | pending ordering | `run-one-pending-with-claude!` runs the lowest-sorted pending run-id, not the submitted one; a stale/concurrent pending run is run with the wrong bundle ⇒ `load-bundle` hash-mismatch THROWS out of `annotate-conversation!` (contract says never throws) |
| F8 | MED | CONFIRMED | prior-failed wedge | a transiently-failed deterministic run-id is permanently `:failed`-noop for that (input,salt); only a salt change (new identity, new paid call, possibly different guess) escapes |
| F9 | MED | PLAUSIBLE | terminal-await race | the terminal `await-run` uses the hardcoded 2000 ms default (not `:timeout-ms`); fold slower than 2 s ⇒ false `:failed` + zero edges ⇒ F1 noop wedge on retry |
| F10 | LOW | PLAUSIBLE | replay vs live race | boot replay-in-a-future racing a REPL annotation on the same relation-id ⇒ order-dependent final status; no coordination between replay and live rk appends |
| F11 | LOW | CONFIRMED | validate-output count | prompt uuid listed in its own `:responses` is silently dropped and mis-counted as a `:duplicate`; non-vector `:responses` (a string) becomes a char-seq counted as N unknown-ids |

Counts: **HIGH 4, MED 5, LOW 2** (11 total). F2/F3/F4 are one root (stable
journal key + unchecked barrier) with three surfaces; F1 is the guard that then
freezes the damage.

---

## F1 — noop-complete guard trusts run-state as a proxy for edge-state — HIGH, CONFIRMED

**Where.** `machine_cut.clj:553-564`. `existing-run (llm/read-run llm-rt run-id)`
then:
- `:554-558` `(and existing-run (= :succeeded (:status existing-run)))` →
  `{:status :noop-complete … :edges-asserted 0 :edges-retracted 0 :epoch-bumped? false}`.
- The LLM terminal is reached at `:619` (`await-run … terminal-statuses`), which
  is **before** any edge write (`:684` WAL, `:700-740` appends+barriers). Run
  terminal and edge materialization live in two independent IPC clusters
  (`:llm-rt` and `:rk-rt`, distinct handles in `ctx`), with no transactional link.

**Writers/readers/clearers.** The `:succeeded` run row is written by the
llm-module fold; the edges by the rk topology; nothing writes both atomically.
The guard **reads only** run status and **infers** edge state. Nothing ever
reconciles the two, and a `:succeeded` run's `:noop-complete` return path issues
zero reads against rk.

**Failing scenario (state stuck masking truth).** Any path that leaves a
`:succeeded` run with wrong/absent edges freezes permanently:
1. via F4/F2: a re-assert dropped by the journal leaves the edge `:retracted`;
   the run is `:succeeded`; re-invoking the same (input,salt) → `:noop-complete`,
   the missing pair is never repaired.
2. via F9: terminal-await races the fold → the driver records `:failed` + zero
   edges while the run later flips `:succeeded`; re-invoke → `:noop-complete`,
   the valid annotation's edges are never written — the map stays empty.
3. Independent-restart variant (PLAUSIBLE, INT boot territory): `:rk-rt` is
   relaunched (fresh, ephemeral, §5.5) while `:llm-rt` persists and no WAL replay
   runs → `:succeeded` run + empty rk → `:noop-complete` → edges absent. (Both
   clusters share nothing but the driver; the guard cannot tell fresh-rk from
   done.)

Only a `:salt` change escapes — but that mints a new run-id, a new paid LLM call,
and (real Claude) possibly a different guess, so it is not an idempotent repair.

**Minimal falsifying test shape.** IPC: run G5 to `:completed`; directly retract
one asserted edge via `rk/append-relation-request!` (simulating F2/F9 loss);
re-invoke `annotate-conversation!` with the same input+salt; assert the result is
`:noop-complete` AND the retracted edge is still `:retracted` (the guard refused
to repair). Gate the fix on: a `:succeeded` prior run must re-derive desired
edges and reconcile before returning noop.

**Gate coverage.** None. G6 asserts idempotent re-run is `:noop-complete` on a
**correct** edge set only; no gate diverges edge-state from run-state.

---

## F2 — assert→retract→re-assert collides on the stable journal key `mc:<rel>` — HIGH, CONFIRMED

**Where.** Idempotency keys are stable-per-relation, two-valued:
- assert: `machine_cut.clj:421` `:request-id (str "mc:" rel-id)`, used as
  `:idempotency-key` at `:708`.
- retract: `machine_cut.clj:721-722` `:idempotency-key (str "mc:" (:relation-id row) ":retract")`.

The relation journal gate is keyed on `(relation-id, journal-key)` and **drops
the whole request** when a prior decision exists:
`relation_kernel.clj:686-692`
```
(local-select> [(keypath *relation-id *journal-key)] $$relation-decisions-by-idempotency :> *prior-decision)
(filter> (nil? *prior-decision))
```
`reconcile-plan` correctly routes a `:retracted`-but-desired row into `:asserts`
(`:467`; also pinned by test `g3 … retracted → re-assert`).

**The A-B-A trace (three runs on relation R, from = e2, to = e1):**
- Run 1 (salt s1): R desired → assert, key `mc:R` → `Journal[R][mc:R] = d1`, R `:asserted`.
- Run 2 (salt s2): pairing moves off R → R asserted-not-desired → retract, key
  `mc:R:retract` → `Journal[R][mc:R:retract] = d2`, R `:retracted`.
- Run 3 (salt s3): pairing returns to R → existing read includes retracted
  (`:690` `true`), R `:retracted` ∉ asserted-rids → R lands in `:asserts` →
  driver appends assert with key `mc:R`. **`Journal[R][mc:R]` already = d1 →
  `filter> (nil? …)` drops it → zero writes. R stays `:retracted`.**

The map now omits a pair the driver reported asserted. This is the exact
"replay a stale decision" the falsification brief names: the re-assert aliases
run 1's journal entry.

**Smoking gun.** `machine_cut.clj:719-721` comment: *"retract key carries a
transition suffix so a re-retract gets a fresh journal key."* The suffix
`":retract"` is **constant per relation-id** — it does NOT freshen per
transition. The author believed the ABA was avoided; it is not. The symmetric
retract-side collision exists too (re-retract of a re-asserted R would reuse
`mc:R:retract`), but re-assert is already dead upstream so it is downstream.

**Minimal falsifying test shape.** IPC, three salted `annotate-conversation!`
calls with canned lines pairing e2→e1, then e2 unpaired, then e2→e1 again; after
run 3 read `rk/read-relation-detail` for R and assert `:relation-status
:asserted`. Today it is `:retracted`. (The fix is a per-transition monotonic
idempotency component in BOTH keys, or reading the current status into the key.)

**Gate coverage.** None. G8/G11 exercise a single A→B move only; no gate flips
back to A.

---

## F3 — WAL replay does not converge on the last line's state under ABA — HIGH, CONFIRMED

**Where.** `replay-wal!` `machine_cut.clj:829-851` applies each `:completed`
line's reconcile in file order via `apply-edge-reconcile!` `:787-802`, which
uses the SAME stable keys (`(:request-id e)` = `mc:<rel>`; retract
`mc:<rel>:retract`).

**Failing scenario.** A WAL from the F2 sequence — lines
`[assert R][move-off-R][move-back-to-R]` — replayed on a fresh cluster:
- line 1: assert R (key `mc:R`, fresh) → R `:asserted`.
- line 2: reconcile → retract R (key `mc:R:retract`) → R `:retracted`.
- line 3: reconcile → assert R (key `mc:R`) → **collides with line 1 → dropped**
  → R `:retracted`.

Final state R `:retracted`, but the last line's desired state is R `:asserted`.
§5.5's "a fresh cluster reconstructs the edges exactly" and "later lines re-apply
reconcile in file order" are **false** for any A-B-A history. Duplicate replay is
deterministically-wrong the same way (idempotent, but idempotent on the wrong
value). Torn-trailing-line handling (`:833-849`) is fine and is the only replay
behavior the suite pins.

**Minimal falsifying test shape.** Hand-write a 3-line WAL (assert R; then a line
whose `:mc/edges` omits R; then a line that re-includes R) on a fresh
relation-kernel; `replay-wal!`; assert R materializes `:asserted`. Fails today.

**Gate coverage.** G11 replays exactly the single A→B move from G8; no A-B-A.

---

## F4 — barrier honesty: awaited outcomes are discarded; timeout returns stale without throwing — HIGH, CONFIRMED

**Where.** `machine_cut.clj:728-735` (live) and `:803-808` (replay):
```
_ (doseq [e (:asserts plan)] (rk/await-relation #(rk/read-relation-detail …) #(= :asserted …)))
```
The result is bound to `_` and never inspected. `await-relation`
(`relation_kernel.clj:1001-1011`) polls to a 3000 ms deadline and, on timeout,
**returns the last value `v`** — it does not throw:
```
(>= (System/currentTimeMillis) deadline) v
```
`wrote?` (`:736`) is computed from `(count assert-reqs)+(count retract-reqs)` —
the PLAN size, not materialized acks — and drives the epoch bump (`:740`) and the
returned `:edges-asserted`/`:edges-retracted` (`:745-746`).

**Consequence.** When an append is dropped (F2) or delayed past 3 s, the barrier
silently returns the stale row, the driver proceeds to `:completed`, bumps the
ingest epoch (faces re-render), and reports non-zero `:edges-asserted` — all
while the edge is unmaterialized. The driver has NO "read-back count == plan
count" check; that assertion lives only in the G13 receipt gate, which is INT /
live-only. So every suite-green run with the fake adapter can hide an unhonored
barrier. This is precisely the brief's "reports `:completed` with unmaterialized
edges + a bumped epoch."

**Minimal falsifying test shape.** Inject an rk stub whose `read-relation-detail`
never reaches `:asserted`; assert `annotate-conversation!` either throws or
returns a status that reflects the unmet barrier (it currently returns
`:completed :edges-asserted N :epoch-bumped? true`).

**Gate coverage.** None. No gate forces a barrier to time out.

---

## F5 — empty-`:responses` pair counted as a pair, dropped from unpaired, yields zero edges — MED, CONFIRMED

**Where.** `validate-output` accepts a pair with empty (or missing/nil)
`:responses`: step-3 reduce `machine_cut.clj:345-361` runs the inner reduce over
`[]`, producing `{:prompt p :responses []}` in `:accepted`, and marks `p` owned;
`:365` `unpaired (vec (remove owned shown))` then **excludes p from unpaired**.
But `pairs->edge-specs` `:411-413` is `(for [pr pairs, r (:responses pr)] …)` —
empty `:responses` ⇒ **zero edges** for that prompt.

**Failing scenario.** LLM returns `{:prompt "e3" :responses []}` (reachable; G8's
own fixture feeds exactly this). Result: `:counts :pairs` includes e3, e3 is
absent from `:unpaired`, the WAL line's `:mc/pairs`/`:mc/unpaired` record e3 as a
pair-opener — yet no edge names e3. The `:conversation` projection derives pairs
by grouping edges on `to`; with no edge, e3 is served as **unpaired**. So the
driver's receipt/WAL disposition of e3 (a pair, not unpaired) contradicts what
the map can actually serve. `:edges-desired`/`:edges-asserted` also silently omit
it. Honest-ledger claim (§5.1 coverage-verb law: "N events classified, P pairs
asserted") is off — a "pair" that asserts nothing.

**Minimal falsifying test shape.** `validate-output` with
`{:pairs [{:prompt "e1" :responses []}] :unpaired []}`; assert that either e1 is
in `:unpaired` OR `:counts :pairs` counts only edge-bearing pairs. Today e1 is a
counted pair with no edge and not unpaired.

**Gate coverage.** G8 feeds the empty pair but only checks aggregate
edge-asserted/retracted counts, never e3's disposition consistency.

---

## F6 — replay reconcile is address-wide; live reconcile is window-scoped — MED, CONFIRMED asymmetry / PLAUSIBLE harm

**Where.**
- Live: `machine_cut.clj:689-696` filters existing by `in-window-scope?`
  (`:445-451`, both endpoints ∈ window scope-ids) — §5.4: "re-annotating one
  window must not retract edges outside it."
- Replay: `read-machine-cut-edges` `:775-781` filters `machine-cut-edge?` ONLY —
  **no `in-window-scope?`**. `replay-wal!` `:840` feeds this address-wide set to
  `reconcile-plan`, whose `:retracts` = every asserted machine-cut edge under the
  address not in the current line's desired.

**Failing scenario.** Conversation C annotated at two windows (river grew between
runs, or a future multi-page; the WAL line records its window). Replaying a line
for the smaller/earlier window W1 while the address holds asserted edges from W2
(outside W1) → replay retracts the W2 edges (they are not in W1's desired), which
the live path deliberately preserved. Under v0 single-page + monotonic river
growth the incidence is low (last line usually a superset), so harm is PLAUSIBLE;
the code asymmetry is CONFIRMED and is a §5.4 invariant violation waiting on a
multi-window WAL.

**Minimal falsifying test shape.** Fresh cluster; hand-seed an asserted
machine-cut edge for an out-of-window event of address C; a WAL line for C whose
`:mc/edges` covers only the in-window events; `replay-wal!`; assert the
out-of-window edge is untouched. Today it is retracted.

**Gate coverage.** G11 uses one window; asymmetry unpinned.

---

## F7 — `run-one-pending-with-claude!` runs the lowest-sorted pending run, not the submitted one — MED, PLAUSIBLE

**Where.** All machine-cut runs share `:executor-task-id llm/pending-task-id`
= `"local"` (`machine_cut.clj:605`; `llm.clj:36`). `run-one-pending-with-adapter!`
`llm.clj:2219` runs `first-pending-entry` = `(→ read-pending "local" → sort-by
key → first → val)` (`:1793-1800`) — the **lowest run-id**, not necessarily the
run the driver just appended. The driver then feeds that claimed run the
closure `load-bundle` built for **its own** run (`machine_cut.clj:586-595`), which
`verify-bundle-hash`es the claimed run's `context-bundle/id` against the driver's
freshly rebuilt input hash.

**Failing scenario.** A prior pending machine-cut run lingers (same long-lived
process: a crashed/aborted run whose claim never granted, or a concurrent
annotation) with a lower run-id. New annotate call submits run R_new, then
`run-one-pending` claims R_old. `load-bundle(R_old.bundle-id)` recomputes the
hash from R_new's address/window; if R_old is a different conversation/window the
hashes differ → `throw "bundle hash mismatch"` (`machine_cut.clj:590-592`)
escapes `annotate-run!`/`annotate-conversation!` — violating §5.7/§9's "never
throws on a failed/malformed run." Even without the throw, R_new never runs; the
terminal `await-run` (F9) times out → R_new recorded `:failed`; R_new stays
`:pending`, so the next same-input call re-enters `annotate-run!` (neither
`:succeeded` nor terminal) and can loop. Within a clean 1:1-drain process this
never fires; it needs residue, hence PLAUSIBLE.

**Minimal falsifying test shape.** Submit a turn-run intent for a DIFFERENT
address (leave it pending, don't run it), then `annotate-conversation!` for
conv1; assert conv1's own run is the one executed (not the stale one) and no
exception escapes.

**Gate coverage.** None; the suite drains 1:1, one run per launch.

---

## F8 — a transiently-failed deterministic run-id is permanently `:failed`-noop — MED, CONFIRMED

**Where.** `machine_cut.clj:560-564`: `(and existing-run (contains?
llm/terminal-statuses (:status existing-run)))` → `{:status :failed :reason
:prior-run-failed :edges-asserted 0 …}` for any non-`:succeeded` terminal
(`:failed`/`:cancelled`).

**Failing scenario.** A run fails for a transient reason (a real Claude timeout;
a cluster hiccup; the F9 false-`:failed`; a bundle-hash throw folded as failed)
on unchanged input. The deterministic run-id is now terminal-`:failed` forever;
every same-(input,salt) retry returns `:prior-run-failed` with zero writes. The
only escape is a `:salt`, which changes identity and (real Claude) the guess. A
correctable transient becomes a permanent dead spot in the map. Whether this is
intended (single-use, first-request-wins §3) or a durability hole is a judgment
call — but a transient infra failure poisoning a content-derived id with no
same-identity retry is a genuine availability defect.

**Minimal falsifying test shape.** Drive a run to `:failed` (e.g. a canned run
that folds `:failed`); re-invoke same input+salt; observe `:prior-run-failed`
with no retry avenue short of salting.

**Gate coverage.** None; no gate re-invokes over a prior `:failed`.

---

## F9 — terminal `await-run` hardcoded to 2000 ms, decoupled from `:timeout-ms` — MED, PLAUSIBLE

**Where.** `machine_cut.clj:619`
`(llm/await-run llm-rt run-id #(contains? llm/terminal-statuses (:status %)))`
passes **no timeout** → `await-run` default 2000 ms (`llm.clj:2432-2436` →
`await-materialized` `:2415-2426`, which returns the last value on timeout, no
throw). The driver's own `:timeout-ms` (default 5000, `:535`) is passed only to
`run-one-pending` (`:615`), NOT to this terminal await.

**Failing scenario.** `run-one-pending-with-claude!` returns after appending the
final `:claude/result` observation; the microbatch fold to `:succeeded` is async.
If the fold lands >2 s later (real corpus, loaded cluster, G13/G14), `await-run`
returns the non-terminal row → `succeeded? false` (`:623`) → the driver writes a
`:failed` WAL line + zero edges for a run that then succeeds → F1 freezes it on
retry. With the fake 2-line adapter this never fires (PLAUSIBLE, not proven).

**Minimal falsifying test shape.** Stub `await-run` cadence / inject fold delay
>2 s; assert the driver does not record `:failed` for an eventually-`:succeeded`
run. At minimum, thread `:timeout-ms` into the terminal await.

**Gate coverage.** None; fake adapter folds in <2 s.

---

## F10 — boot replay racing a live REPL annotation on a shared relation-id — LOW, PLAUSIBLE

**Where.** `append-wal-line!` `:493` locks a JVM var for FILE writes, but there is
NO coordination between `replay-wal!`'s rk appends and a concurrent live
`annotate-conversation!` on the same relation-id. §6 wires "WAL replay after
attach" and the brief names "dev boot replays in a future while a REPL annotation
runs."

**Failing scenario.** Live wants R retracted (reconcile) while replay wants R
asserted. Interleaving of `assert(mc:R)` vs `retract(mc:R:retract)` at the journal
gate is order-dependent: if R pre-exists, the assert is dropped-as-replay and the
retract wins; if R is new, whichever decision lands first wins and the other is a
same-key no-op or an absent-retract rejection → final status depends on arrival
order. Deterministic-id convergence assumes identical DESIRED state; a genuine
replay/live disagreement is a race. Single-JVM dev, narrow window → LOW.

**Minimal falsifying test shape.** Two threads: one `replay-wal!`, one
`annotate-conversation!` with a conflicting desired set, same relation-id; assert
a deterministic final status. Hard to pin without a barrier/lock; documents the
gap.

**Gate coverage.** None.

---

## F11 — validate-output count conflations — LOW, CONFIRMED

**Where.** `validate-output` step 3, `machine_cut.clj:345-361`.

- **Self-referential prompt** `{:prompt "e1" :responses ["e1" "e2"]}`: the inner
  reduce seeds `owned*` with `(conj owned p)` = `{e1}`, so the self-response `e1`
  hits `contains? owned* e1` → counted as a `:duplicate` and dropped; pair kept as
  `{:prompt e1 :responses [e2]}`. A self-reference is silently swallowed and
  mis-labeled a cross-pair duplicate. Totality still holds.
- **Non-vector `:responses`** `{:prompt "e1" :responses "e2"}`: `(vec "e2")` =
  `[\e \2]` (char seq); `all` = `("e1" \e \2)`; the chars are ∉ shown → the pair
  is rejected with `:unknown-ids += 2` (two garbage "unknown ids" from one
  malformed field). No crash, but the count is meaningless. `:responses` nil or
  missing collapses to the F5 empty-responses path.

Neither throws; both corrupt the honest-ledger counts (§5.1/§5.3) that feed the
WAL and G13 receipt.

**Minimal falsifying test shape.** `validate-output` with a self-referential pair
and with a string `:responses`; assert the `:duplicates`/`:unknown-ids` counts
mean what §5.3 says.

**Gate coverage.** G2 covers clean unknown/dup/non-human/missing/malformed; not
self-reference or non-vector responses.

---

## Gate verdict (does the suite pin this class?)

Traced `test/app/machine_cut_test.clj` in full. The suite pins **happy paths +
one single-direction reconcile move**:
- G1-G3 pure: determinism, validation of the six canned shapes, plan diff
  (including retracted→re-assert **in isolation**, which is exactly the row that
  F2 shows the kernel then silently drops).
- G5-G9 IPC: one assert, one idempotent noop (on a correct edge set), dead-letters
  empty, one A→B salted move, epoch on success/failure.
- G11: replay of that one A→B move + a torn trailing line.

**Not pinned (this class):** A-B-A re-assert (F2/F3); noop-complete over diverged
edge-state (F1); barrier timeout / unmaterialized-edge honesty (F4);
empty-responses pair edge/count consistency (F5); replay window-scope (F6);
wrong-pending-run selection (F7); prior-`:failed` retry (F8); terminal-await race
(F9); replay/live concurrency (F10); self-ref / non-vector responses (F11).

The single most load-bearing gap: **G3 proves the driver PLANS a re-assert of a
retracted row, but no gate proves the kernel HONORS it** — F2 lives exactly in
that seam, and F4's discarded barrier is what makes it silent.
