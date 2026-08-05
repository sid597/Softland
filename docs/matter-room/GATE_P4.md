# matter-room — P4 slim gate

2026-07-28 · fresh Fable gate over the final uncommitted P4 worktree
(`/mnt/data/projects/Softland-matter-room-p4`, branch `codex/matter-room-p4`,
base `620c021` — the P3 code commit).

## Verdict

**PASS — P4 is closed at the slim tier.**

G8, G9, and G10-machine are green: the focused suite was independently
re-derived and re-run, the full five-file diff was read against every
contract-named trap, and the contract-critical receipts were independently
re-driven live on an isolated current-worktree server. Five non-blocking
findings/observations are recorded below with their cheap falsifiers.
Staging and BOTH commit decisions remain Sid's; this verdict commits
nothing. The package's phases end at P4; the §8 package falsifier arms at
Sid's wear.

## Scope and fence — machine-verified

- Worktree HEAD re-verified at open and close: `620c021e…`; index empty
  throughout; nothing staged, committed, or pushed in either tree.
- `git status --porcelain --untracked-files=all`: EXACTLY the five PLAN §P4
  files (`face_projection.clj` · server `material_portal.clj` ·
  `matter_room.cljc` · `face_projection_test.clj` ·
  `material_portal_test.clj`); zero untracked files, so no new test
  namespace exists anywhere. Diffstat: 5 files, +578/−23 —
  matching the receipt exactly. `git diff --check` clean.
- Zero diff verified by path on: `cascade.clj`, `cluster.clj`,
  `material_circulation.clj`, `object_container/facet_master.clj`, shared
  `material_portal.cljc`, `binding_material.cljc`. No stashes.
- `env.clj` was NEVER read: the worktree boot used a symlink to the main
  tree's file, created blind and removed at teardown (verified gone), same
  for `node_modules`.
- Main tree confirmed at `604e6a8` with the foreign write-set (multi-cascade
  docs, decisions.md, next-prompt.md, vision/LOG.md, probe-block-anatomy.*)
  untouched and unabsorbed; this gate's only write is this file.

## Independent gate evidence

### Focused suite — selection re-derived, re-run this session

Selection re-derived per the cross-phase machine-cut rule: grepped the test
tree for every edited file's path. Pinning scans found: `face_projection.clj`
slurp-scanned by `face_projection_test` (:171), `face_arsenal_test` (:196),
`provenance_material_test` (:558); `matter_room.cljc` slurp-scanned by
`material_portal_test` (:470). (`face_integration_test`'s hit is a comment,
not a scan — its exclusion is legitimate.) Union run = the receipt's exact
six namespaces: the two edited test namespaces + face-arsenal +
provenance-material + the conservative material-circulation and
cascade-table pins:

```text
62 tests · 1,171 assertions · 0 failures · 0 errors
```

— exactly the receipt's numbers. CLJS `:dev` compile re-run from the
worktree: 272 files, 0 compiled (artifacts already current), **0 warnings**.

### G8 — PASS

- Diff read whole: the production `:cascade-rows` two-arity reads exactly
  `cascade/rows` and serves `:cascade/read-only? true`, labels
  `[:code-owned :in-process]`, ownership `:code-owned`, source
  `'app.server.cascade/rows` (a SYMBOL — production rows carry the handler
  as a symbol, so enumeration can never resolve or invoke it), plus the
  source-swap note. The injectable three-arity labels itself
  `:fixture-injected` / `[:fixture-injected :in-process]` — the falsifier
  repair is real in the source and PINNED by the suite
  (`(not= :code-owned (:cascade/ownership injected))`).
- The dark-row inertness proof runs on a fixture-injected
  `(constantly [dark-row])` source with a call-recording handler asserted
  never invoked — never on a row in the real serve; production rows are
  separately asserted `= (cascade/rows)`.
- `cascade.clj` zero diff (machine-verified by path). The new
  `:portal/cascade` section rides the standard `sect` boundary (fallback +
  `:section/error` into `:portal/errors` — no silent confident-zero).
- Mechanical sweeps over the whole diff: zero
  write/topology/depot/import-request constructors, zero mtime/filesystem
  inference. The pre-existing read-only slurp-scans (G12/G21) pass
  UNWEAKENED — the gauge's only OC read is `ocr/read-revision-history`,
  already in the scans' exact allowed set.
- LIVE (isolated `:8096`, this gate): the anchor portal served ONE cascade
  row (`:cascade/material-autotag`) with the exact production labels.

### G9 — PASS

- ON-DEMAND proven three ways: (1) the suite's throwing-spy test —
  standard portal open with `git-spine/read-commits` redefined to throw,
  asserted ZERO calls (re-run green); (2) the live anchor open embedded no
  `:portal/escape-gauge` and its query plan listed `cascade-rows` but NOT
  `escape-gauge`; (3) the portal links the face:
  `{:face :escape-gauge :params {:master-id …} :on-demand? true
  :embedded? false}` — verified in source, suite, and live.
- Activation read PINNED in source and observed live: exactly one
  `read-revision-history` over `(active-pointer-container-id
  provenance-material/spec)` = `oc:block:fm:provenance:active-pointer`,
  cursor `""`, limit 100000 — the single provenance master; no alternate
  provenance truth source exists in the diff. Repo root =
  `(System/getProperty "user.dir")`, derived locally.
- Timeout at the FACE: owner `deref`s the flight with the request timeout;
  expiry returns a poisoned report carrying the real report's FULL 13-key
  shape plus a named `:error` (poisoned-but-total verified against
  `terminal-escape-report`'s key list). Single-flight via CAS on one
  process-wide atom; concurrent/post-expiry callers get the cached last
  report; the abandoned worker completes naturally and only ITS real
  report lands in the cache (the timeout poison never does). All
  suite-pinned (blocked-git fixture: one git call, cached sibling, natural
  completion) and re-run green.
- Honest ambiguity: suite pins two-tip history → `:ambiguous-activation-
  history`, `nil` count, `nil` escapes, no inferred zero in the resident
  text. LIVE on this gate's unseeded runtime: EMPTY provenance history
  (zero events → zero tips) rendered `:ambiguous-activation-history` with
  `count nil` — honest, never a confident zero (see observation 5).
- `:measured` on IPC fixtures per G9's letter: the suite's clean
  single-tip fixture (provenance seeded via `ensure-master!`) asserts
  `:measured`, re-run green this session.
- Live gauge drive through the PRODUCTION dispatcher in the live server
  JVM: real git subprocess scanned 802 commits; all seven
  `default-material-policy-paths` listed; three sequential asks were three
  clean owner computes (real git returns faster than a 150ms stagger, so
  the cached path wasn't observable live — the suite's deterministic
  blocked-git proof covers it).
- Truncation entries for `:cascade` AND `:escape-gauge` present (suite +
  live); exactly 17 questions live; the 17-question floor is structurally
  unwidenable here — shared `material_portal.cljc` is zero-diff.
- The room still serves with the gauge resident among the residents
  (live: head, bindings, gauge — all birthed accepted; trail absent only
  because the fresh runtime has no pointer revisions; the suite pins trail
  composition deterministically).

### G10-machine — PASS

Environment attestation FIRST, on the app page of the isolated
current-worktree `:8096` server (`LAND_CLUSTER=0 LAND_PINNED=1`,
fresh-profile headless Chrome): WebGPU available, adapter vendor `google`,
architecture `swiftshader`, `isFallbackAdapter true`,
HeadlessChrome/150.0.0.0 — the CPU rasterizer, conservative direction, and
the SAME adapter identity the implementer attested.

Real ground edit echo with the room serves live and the gauge driven to
completion before sampling (the receipt's own ordering):

```text
warm runtime: n=45 · p50=16.4 · p95=20.4 · p99=22.5 · max=22.5
slow echoes >52ms=[] · client stalls=[] · inflight=0
bar: p95 < 52ms — observed 20.4ms
```

Honestly recorded: the FIRST sampling pass overlapped the server's
first-ever serves (faces sweep, WAL replays, a 172ms cold
`:material-portal` serve) plus a boot-time `WEBGPU/DEVICE-LOST` event and
breached the bar (n=38, p95=56.1, outliers 56/74/56 clustered in the
cold window). The warm pass on the same rig is the receipt; the cold
distribution is boot transient, not steady-state echo (the same class as
the implementer's honestly-visible 65.1ms cold echo).

Teardown verified: `:8096` and the `:9223` debug port free, gate Chrome
processes gone, the shared `:8080` server and Rama cluster never touched,
the conductor never started, symlinks removed.

## Findings — non-blocking, cheap falsifiers named

1. **Timeout envelope's resident is refreshable with the poison.** On
   owner expiry, `gauge-resident` composes from the poisoned TIMEOUT
   report with `:resident/refreshable? true` (`(map? report)`); a driver
   routing it through the edit lane would land a timeout poison durably as
   "LAST COMPUTED REPORT (verbatim EDN)" over a previously measured one —
   while the in-process cache correctly never holds the poison. Latent
   (no production driver exists — finding 2). Falsifier: blocked-git +
   `:timeout-ms 1`, feed the returned resident to `matter-room-refresh!`,
   read the unit. Cheap fix: refreshable only when the report has no
   `:terminal-escape/error`, or rename the durable line honestly.
2. **No production driver lands the computed report durably.** The
   on-demand face composes and SERVES the gauge resident, and the test
   proves the P2 edit lane lands it; but no production code path calls
   `matter-room-refresh!` with it — the durable resident stays at the
   placeholder until some driver (client wiring or an act endpoint, next
   contract's call) exists. L7's serve/refresh machinery is real and
   proven; the standing-durable half currently requires the test-shaped
   call. Falsifier: drive the gauge live, re-open the room, read the
   durable gauge unit — placeholder text remains.
3. **Pre-P4 rooms get a gauge/trail overlap (derived from code, not
   verified live).** Rooms born at P2/P3 (the durable cluster's
   fm:attention room) placed trail #1 at ordinal 2 / y=520; the gauge now
   births at exactly that ordinal and position, and existing residents'
   positions settle as truth — so old rooms show two residents at y=520
   and an order-key tie. New rooms are consistent. Falsifier: compose a
   P3-shaped room in IPC, then run P4 `residents` and diff positions; or
   look at the durable room at wear. Verifying live here would have meant
   touching the durable cluster — forbidden in this gate.
4. **Uncapped caller-supplied `:timeout-ms`.** Any positive number is
   accepted; a huge value parks that session's serve thread on the deref
   while git hangs (single-flight still bounds subprocesses to one; only
   the caller's own session stalls). Cheap fix: an upper cap at the face.
5. **Unseeded dev runtimes read ambiguous, honestly.** `LAND_CLUSTER=0`
   boots do not seed masters (GATE_P3 finding 2's territory), so the
   provenance history is empty → zero tips → `:ambiguous-activation-
   history`. Recorded so a dev-boot ambiguous is not misread as the F4
   residue firing; the durable cluster has real history.

Residue carried: the F4 `:ambiguous` keying residue itself (LATER at P8,
unchanged); Sid's headed G10 half completes at wear; the boot-time
`WEBGPU/DEVICE-LOST` under headless SwiftShader (recovered both runs, zero
stalls in the warm sampling window; P4's diff touches no client/render
code); kondo remains unavailable in this environment.

## Receipt coherence

`P4.md`, this gate, the source/tests, and the live output agree on: the
five-file fence and +578/−23; suite 62t/1,171a (exact); CLJS 272 files /
0 warnings; the falsifier repair (`:fixture-injected` envelope separation)
present in source and pinned; the live anchor portal shape (one cascade
row, production labels, on-demand non-embedded gauge link, both truncation
entries, 17 questions, plan excludes escape-gauge); room id `a983e774-…`
(P1's derivation, stable across all four phases); adapter attestation
identical (google/swiftshader/fallback-true, headless Chrome 150); G10
under the bar on both rigs (theirs 16.4ms, this gate 20.4ms warm).
Explainable divergences, not contradictions: their live gauge read
`:measured` and their room held four residents (their runtime carried
provenance history and a pointer revision); this gate's fresh runtime
read an honest `:ambiguous` on empty history and a three-resident room —
both behaviors deterministically pinned by the shared suite. Their
single-flight was observed "completed, no in-flight worker" pre-sampling;
this gate observed three clean sequential computes and relies on the
suite's deterministic proof for the concurrent path. No receipt claims
the durable cluster was driven; it never was, by either side.

## Close

P4 is proven at CONTRACT §8's slim tier. The package's four phases are
complete pending Sid's commit ruling: staging and both commit decisions
remain his, and this verdict does not commit, stage, or push anything.
The §8 pre-registered package falsifier (the first material-policy change
routes through the room; the gauge is the detector) arms at his wear.
