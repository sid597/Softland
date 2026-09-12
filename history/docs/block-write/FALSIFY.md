# block-write — FALSIFY (falsification finder record)

Fresh-context finder (Fable-directed), 2026-07-12. Default-fail posture: job is
to BREAK the change. Artifacts (LANE_A/B, INT, PHASE_0) read as prior-pass
records, NOT authority — every verdict below is judged against the CODE. Findings
ranked most-severe first, then the per-question protocol table, the trap ledger,
then open doubts + cheap falsifiers. No code changed.

Legend: HIGH = user-visible wrongness or durable corruption · MED = degraded /
latent / unbounded growth · LOW = hygiene. `[V]` verified in code · `[I]`
inferred (flagged).

---

## Findings

### F1 — HIGH · `!truth-overlay` has no seq guard + the single `!block-truth-request` atom loses cross-unit pulls → a stale overlay entry masks newer truth durably AND seeds a stale re-edit
`electric_flow.cljc:590-596` (one `!block-truth-request` atom) · `block_edit_wiring.cljs:171-199` (merge/prune) · `:149-155` (`truth-text*` reads overlay FIRST) · `block_edit.cljc:269` (`overlay-face-context` xblock: `truth (get truth-overlay id (:text b))` — overlay WINS over served text).

The §5 echo arms one pull by `reset!`-ing a single `!block-truth-request` atom
(`block_edit_wiring.cljs:176-181`); Electric samples the latest value only
(signal conflation, latest-wins). For the SAME unit under a burst this is fine
(`read-unit` always returns current truth). Across UNITS it is not: edit U to
"v2" arms `{unit U}`, then click V and edit V — V's `reset!` overwrites U's
request before Electric samples it, so U's final pull is dropped and `!truth-overlay[U]`
is left at an earlier completed value ("v1"). [V]

Concrete failure: (1) edit U "v1" (pull completes → overlay[U]="v1"); (2) edit U
→ "v2" (pull armed); (3) switch to V and type (overwrites U's request pre-sample).
Now overlay[U]="v1", Rama truth="v2". The full face pull (INV-19 1s-debounced) later
serves U="v2", but `overlay-face-context` gives overlay precedence → U renders "v1".
The prune (`block_edit_wiring.cljs:192-199`) only removes entries where
`= txt served-txt`; "v1" ≠ "v2" → entry is KEPT → the stale mask is permanent for
the session (never self-heals unless U is edited again). The map lies.

Worse: re-focusing U seeds the buffer from `truth-text*` which reads overlay first
(`:154`) → buffer = "v1"; a subsequent keystroke sends content derived from "v1",
overwriting the good "v2" in Rama — **durable data corruption**, not just a view lie.
Reachability: MED (needs unit-switching mid-burst + a lost final pull), but the
target workload is exactly 12/s keystroke editing with focus changes.

Cheapest falsifier: JVM/unit — none exists; needs a headed drill: edit block A a
few chars, immediately click block B and type, then read block A's rendered text
vs `read-unit` truth. Or a wiring unit test that arms two different-unit requests
back-to-back and asserts overlay converges to each unit's latest truth.

### F2 — MED · tracked files hard-require UNCOMMITTED probe namespaces → a clean commit of the diff will not compile
`runtime.cljs:23` requires `[app.client.workspace.block-edit-probe]` and
`render.cljs:5` requires `[app.client.substrate.webgpu.island-probe]`; both target
files are UNTRACKED (`block_edit_probe.cljs`, `island_probe.cljs`). [V]

If the tracked block-write changes are committed as-is (the probe files are
"strip before commit / uncommitted"), the cljs build breaks on a missing namespace.
Additionally `render.cljs` is contaminated with the SEPARATE 2026-07-11 island-probe
(`render.cljs:126-127` forces redraw when `island/driving?`, `:491`, `:538`) — unrelated
to block-write, mixed into this package's diff. The require lines and the render-pulse
change must be reverted before any commit; the probe requires are load-bearing where
they sit. Cheapest falsifier: `git stash` the untracked probe files, recompile → missing-ns error.

### F3 — MED · `!continuations` + `:pending` grow unbounded when Electric conflates the outbox; those keystrokes' `on-decision` never fires
`block_edit_wiring.cljs:62-64` (`submit!` registers a continuation per envelope) ·
`:150-151` (`be/input` records a `:pending` entry per envelope) · `electric_flow.cljc:649-651`
(`(let [edit (e/watch !block-edit-outbox)] (when edit (reset! !block-edit-result (SubmitBlockEdit edit))))`). [V]

`atom-submit!` sets a depth-1 outbox and keys a `done` continuation by request-id;
`be/input` keys a `:pending` entry by request-id. Both are retired ONLY when a
result with that request-id lands. But the outbox is a single atom watched by
Electric — two `reset!`s in one reactive frame conflate to the latest, so
`SubmitBlockEdit` runs for the newest envelope only; the skipped envelopes' request-ids
never appear in `!block-edit-result` → their continuation (in `!continuations`) and
their `:pending` entry (in `!edit-state`) are never removed. Over a 720-keystroke
session with any conflation these maps grow monotonically. Text correctness is
preserved (every envelope carries the FULL cumulative `new-text`, latest wins), so
this is growth/latent, not wrong output — but `on-decision` (accept-retire / reject-revert)
silently never runs for the dropped keystrokes. No writer ever prunes these maps on
timeout or focus change. Cheapest falsifier: unit test that submits N envelopes but
only lands the last decision, asserts `!continuations`/`:pending` do NOT retain N-1 entries.

### F4 — MED · silent typed-character loss: `m/relieve` on `>keyboard` drops keystrokes (measured 14/720 at 1.8KB); root cause is the per-keystroke whole-face overlay rebuild
`events.cljs:143` (`>keyboard` ends `(m/relieve (fn [_ x] x))`, latest-wins) → all
routers incl. `<face-edit-keys` (`:229-231`) derive from the relieved flow ·
`editor_compute.cljs:487-513` + `block_edit.cljc:254-284` (`overlay-face-context`
`mapv`s over every turn/block each keystroke). [V, INT-measured]

Under 12/s on a ~1.8KB block, `overlay-face-context` rebuilds the whole face context
and the scene compares/rebuilds per key; when that lags the relieve, keydowns are
DROPPED before reaching the buffer (INT R2: 14/720 unechoed = never entered the buffer).
For a text editor this is user-visible input loss (a typed char vanishes) — no phantom
text (the law holds), but characters are silently lost. The relieve is pre-existing
infra, now made load-bearing by routing character input through it; the named fix
(focused-block partial rebuild) is deferred (LATER). Cheapest falsifier: the INT probe
at a larger block already reproduces; a regression would be a headed type-burst asserting
`emitted == buffer-length-delta`.

### F5 — MED · `!truth-overlay` entries for blocks that leave the served page are never pruned
`block_edit_wiring.cljs:192-199`: prune removes entries where
`= txt (:text (context-block ctx uid))`; if the unit is no longer in `ctx` (`:until-ms`
cut, paging, face change), `context-block` returns nil → `(:text nil)` = nil → `(= txt nil)`
false → entry KEPT forever. [V] Overlay grows unbounded across a session as edited
blocks scroll/page out. Render is unaffected (absent units aren't drawn), so this is
growth-only. Cheapest falsifier: unit test — merge an overlay entry, then feed a `ctx`
that omits that unit, assert the prune drops it (it will NOT under the current predicate).

### F6 — MED · refusal notice never auto-clears and `:refusal` is a single global slot
`block_edit.cljc:166-171` (`on-decision` sets `:refusal` — one map, not per-block) ·
`:175-178` (`dismiss-refusal` exists) — but `dismiss-refusal` is NOT called anywhere
(`grep`: only defined). [V] `focus` clears the refusal only for the block being focused
(`:121`). Consequences: (1) a refusal line (`"⟂ edit refused: …"`, appended into the
block's `:text` at `block_edit.cljc:273-276`) persists on a block indefinitely until
that block is re-focused; (2) a second block's refusal overwrites the first (`:refusal`
is a single slot) → only one refusal ever shows; overlapping refusals lose information.
Cheapest falsifier: unit test — refuse block A, then refuse block B, assert A's refusal
still surfaces (it will not); and assert a path clears A's notice without re-focus (none exists).

### F7 — LOW · global `!ingest-epoch-atom` bumped at keystroke grain (12/s)
`electric_flow.cljc:632` (`(swap! !epoch inc)` on every accepted non-replay edit;
`!epoch` defaults to `util-fns/!ingest-epoch-atom`, `:60`). [V] Consumers verified:
`file_viewer.cljc:219` `WatchIngestEpoch` drives the debounced full FacePull, which
just re-arms the 1s debounce under sustained typing (benign; the measured full-channel
"miss"). Other consumers (`ingest_watchers.clj`, `machine_cut.clj`) are server writers,
not client re-pull triggers. [I] I did not audit every client watcher of the epoch
(sidebar/trail); if any re-pulls without debounce, 12/s bumps would storm it. Open doubt below.

### F8 — LOW · hit-test id-collision surface in the click resolver
`mouse.cljs:410-416`: `unit-id (some (fn [node] (when (vector? (:id node)) (some ids (:id node)))) (rseq path))`
returns the FIRST element of any node's `:id` vector that is in the block-id set. [V]
If a non-block node's `:id` vector happens to contain a string equal to some block's
unit-id (e.g. a decoration node keyed off a block id), a click on that node focuses
the block. Unverified whether such collisions exist in the assembly node-id scheme.
Cheapest falsifier: dump node `:id` vectors for a served reader face; check no non-block
node carries a unit-id segment.

### F9 — LOW · probe durably edits real conversation blocks 0–2 (disclosed)
`block_edit_probe.cljs:99-102,147-182` dispatches real keydowns that write durable
`:object/edit`s into the first-light conversation (INT §4 disclosure). [V] Recoverable
(revision history + imports bit-unchanged via graduation), but the blocks now carry
probe text until edited back; the probe must never be committed. Hygiene.

### F10 — LOW / refuted-leaning · assembly click passes raw `x` (no sidebar subtraction) unlike sibling handlers
`mouse.cljs:512` calls `handle-face-assembly-click! atoms x (+ y scroll-y)` with raw `x`,
whereas trail/flow/file handlers subtract `sb-w` (`:492,:501,:515`). [V] Likely CORRECT:
the face geom is full-viewport-width (`editor_compute.cljs:497` `:content-w (- width 32)`,
no sidebar reservation) and the top-level guard swallows `x < sidebar-w` (`:468`), so the
scene is absolute-x. Flagged only because the convention divergence is easy to get wrong
if the reader face ever renders with a reserved sidebar. Falsifier: sidebar visible → click
a reader block near the left edge → confirm focus lands on it.

---

## Protocol table (CLAUDE.md falsification pass, per new state/flow)

| State / flow | Writers | Readers | Clearer | Verdict |
|---|---|---|---|---|
| `!edit-state` (`block_edit_wiring.cljs:33`) | `focus!`/`blur!`/`handle-keystroke!`/`face-edit-keys-consumer`/`on-decision` — all via one JS-thread m/reduce, serialized | `<focused-view`, `<block-view`, editor_compute overlay, keyboard consumer | `blur` drops buffer; `on-decision` retires pending; `dismiss-refusal` (UNWIRED, F6) | Single-writer-serialized OK; `:pending` leaks under conflation (F3); refusal never auto-clears (F6) |
| `!continuations` (`:36`) | `submit!` assoc; `!edit-result` watch dissoc | result watch | matching result only | Leaks on conflated/never-returning envelopes (F3) |
| `!truth-overlay` (`:134`) | merge watch (`:185`) | `overlay-face-context`, `truth-text*` (focus seed + revert) | prune watch (`:192`) only when overlay==served AND unit still in ctx | No seq guard → stale mask + stale seed (F1); no prune when block leaves page (F5) |
| `!block-edit-outbox/result` (`electric_flow.cljc:589-590`) | Lane B `reset!` / server `reset!` | server e/watch / client result watch | never cleared (INT: intentional — replay is a journaled no-op) | Conflation drops intermediate envelopes → text OK (full-text, latest-wins), but F3 leak |
| `!block-truth-request/data` (`:592-593`) | client arm on accept / server FacePull | server serve / client merge | overwritten by next arm | Single atom loses cross-unit pulls (F1) |
| `:pending` map (`block_edit.cljc:111`) | `be/input` assoc; `on-decision` dissoc | `on-decision` block-id lookup | `on-decision` only | Leaks with F3 |
| `!api` (`block_edit_wiring.cljs:139`) | `install-...!` once | mouse/keyboard consumers | never (defonce, single install) | OK |
| epoch bump (`electric_flow.cljc:632`) | ack continuation only (accept ∧ ¬replay) | `WatchIngestEpoch` → debounced pull | monotonic inc | BW-T9 held; 12/s grain benign for the reader face (F7) |

Ownership answers: **Ownership** — all client edit state is single-writer via the serialized
m/reduce consumer (no multi-writer race). Server decision is the single truth writer.
**Consumer** — `:block-truth/found? false` path: consumed (merge only on `found?`, `:189`);
`:errors` passthrough: consumed (`atom-submit!` `:61` falls back to `(some-> errors first :type)`);
`:document-container-id`: threaded on every projection path (`block_distiller.clj:1310`,
`face_projection.clj:94`, tested). **Error path** — a never-returning SubmitBlockEdit leaves
the continuation + `:pending` entry live (F3); no timeout cleanup. **Ordering** — F1 (cross-unit
pull loss) and F3 (conflation). Focus-change mid-flight: `on-decision` reverts the buffer only
when `block-id == focused-id` (`block_edit.cljc:168-171`) → an A-decision arriving while B is
focused does NOT clobber B's buffer (correct); it only raises A's refusal — **refuted as a bug**.
**Shape** — `context-block`/`overlay-face-context` traverse `:turns`→`:blocks`, matching the served
shape (`face_projection.clj:96-105`); `:reader-turn` de-duplicated against turns (`block_edit.cljc:280-283`)
— no double-overlay. Hit-test `some ids (:id node)` semantics: F8. **Done gate** — see below.

---

## Traps BW-T1–T10: held / violated

- **BW-T1 second write pipe — HELD** [V]. `submit-block-edit!` builds `oc/object-edit-request`
  and appends to the existing `*object-container-requests-depot`; no new module/depot/topology.
- **BW-T2 microbatch rescue — HELD** [V]. Text-kernel untouched.
- **BW-T3 pre-batching — HELD** [V]. `face-edit-keys-consumer` mints ONE envelope per content key (`:257-266`).
- **BW-T4 optimistic echo — HELD** [V] with a caveat. Buffer is pending-input, rendered only in
  the focused block (`block-view` focused branch, `block_edit.cljc:303-315`); blur drops it; reject reverts.
  Caveat: the revert/seed source (`truth-text*`) reads the overlay, which F1 can leave stale — the fallback
  is "last *overlay* truth", which can diverge from Rama truth (F1). Not phantom text; still a truth divergence.
- **BW-T5 idempotency reuse — HELD** [V]. `request-id = f(client-id,seq)`, `idempotency-key` reuses the
  kernel's verbatim shape; server stamps `content-hash` via `oc/source-hash`; `already-decided?` pre-read
  (`electric_flow.cljc:625`) gates the bump so a replay is a no-op (G3 asserts).
- **BW-T6 caret on a second signal — HELD at the pure/wiring layer** [V]. `:buffer` co-carries text+caret;
  `<focused-view`/`<block-view` derive the pair from one `m/watch !edit-state`. The Electric compose feeds
  `overlay-face-context` from the SAME `m/latest` as `!edit-state` (`editor_compute.cljs:487,532-534`) — no
  second signal for the focused pair. [I] Not exercised headed for a torn frame (LANE_B open doubt stands).
- **BW-T7 client tracks container-id — HELD** [V]. Target is unit-id forever; `document-container-id` copied
  verbatim off the served block, never computed.
- **BW-T8 edit-seq across reconnects — HELD** [V]. Fresh `edit-client-id` per boot, never persisted (`:31`).
- **BW-T9 epoch bump from render — HELD** [V]. Bump lives in the ack continuation (`electric_flow.cljc:632`);
  the block-truth arm lives in the decision watch (`block_edit_wiring.cljs:174`); `overlay-face-context` is a
  pure fn inside `m/latest` with no side effects.
- **BW-T10 debounce swallowing the final pull — HELD via the narrowing, LITERALLY TRUE on the full channel** [V].
  The single-unit pull is armed per accept with no debounce (holds). The FULL face pull IS swallowed under
  sustained typing (INV-19 1s re-arm) — measured p50 ~31s (INT §3); the narrow channel is what carries the echo,
  so the trap is dodged for the user but the full-channel swallow is real and the sole truth path once a block is blurred until 1s idle.

---

## Done-gate: one uncovered failure mode per gate (G1–G9)

- **G1/G2 (round-trip/revision)** — cover the kernel overlay; do NOT cover the CLIENT echo assembly (the `!block-truth-request` single-atom collision, F1) or that the served text reaches the render model uncorrupted.
- **G3 (replay)** — asserts kernel no-op + bump-0; does NOT cover the client `already-decided?` read being a SECOND round-trip per keystroke (`electric_flow.cljc:625` reads the decision before append AND after — two point reads per edit; latency/cost unmeasured under load).
- **G4 (stale)** — single-client seq only; concurrent two-client staleness (S4) is untested (non-goal, but the epoch double-bump and cross-client `already-decided?` are unexercised).
- **G5 (refusal visibility)** — covers a single refusal surfacing + revert; does NOT cover refusal persistence / single-slot overwrite (F6).
- **G6 (caret covariance)** — pure-fn only; the Electric compose torn-frame case is not gated (LANE_B open doubt).
- **G7 (E2E echo)** — narrow-channel p95 passes, stall clause FAILS every run (2/4/7/2 vs ≤1); the full channel misses categorically. Does not cover F1 (measured only same-unit sustained typing, never unit-switching mid-burst) nor F3/F4 growth over a long session (60s runs only).
- **G8 (wearing)** — restart survival + forced-stale revert; does NOT cover F1's stale-seed-overwrites-truth path or F5 overlay growth.
- **G9 (read-plan conservation)** — additive-key seek bound held; does not touch client state.

---

## Open doubts (with cheap falsifiers)

1. **F1 reachability** — does Electric actually conflate two different-unit `!block-truth-request` resets in one frame, or does it serialize enough that the final per-unit pull always lands? Falsifier: instrument the merge watch to log every `(unit,text)` it receives during a two-unit burst; assert each unit's LAST truth appears.
2. **F7 epoch fan-out** — audit every client watcher of `!ingest-epoch-atom` (sidebar, trail, agent-trail) for a non-debounced re-pull. Falsifier: `grep` the client for `ingest-epoch-remote` consumers and check each is debounced.
3. **BW-T6 headed** — capture a frame mid-burst and confirm the focused block's rendered caret index matches its rendered text length (no tear). Falsifier: the existing probe + a per-frame assertion.
4. **G3 double-read cost** — the per-keystroke pre-append `read-decision` doubles the point-reads on the write path; unmeasured. Falsifier: time `submit-block-edit!` with/without the pre-read under the 12/s load.
5. **F2 build** — stash the untracked probe files and recompile the client; confirm/deny the missing-ns break before any commit of the tracked diff.
