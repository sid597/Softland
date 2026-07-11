# Framework package RETRO — faces-as-assemblies (Waves 1 + 2)

2026-07-11 · close session (Opus, `framework-close-retro`), `/work-package`
close steps 3–5. Written from the full trail: `NOW.md` (per-session log) ·
`CONTRACT.md` v2 (with the four dated in-place amendments) · `PROBE.md` ·
lane artifacts `W1-A/B/C`, `W2-D/E` · `W1-INT`/`W1-GATE` · `W2-INT`/`W2-GATE` ·
**and the source itself** (every scorecard claim below was spot-verified at
file:line against the two code commits `c84ebfa` (W1) / `1725f55` (W2), not
trusted from the artifacts). The adversarial recheck (step 4) is appended at
the foot of this file after a fresh subagent re-ran the suite and re-verified
each claim; where the recheck and this body disagree, **the recheck is the
corrected reading** (the cycle-1 rule: retros are written by the same process
they judge).

## Verdict

**Package CLOSED. Both waves gate-passed** (G16, G26 — Fable falsification
pass each). Suite at committed HEAD: **46 tests / 804 assertions / 0f / 0e**
across 8 namespaces (7 `face_*` + `clojure_adapter_test`, the
`object_container.clj` regression guard). No suite reads git HEAD dynamically,
so the code-atom close-protocol Step-2 trap (a package's own closing commit
moving HEAD off a pinned specimen) **does not apply here** — the G7
source-form diff reads the working tree, which is clean, so disk == committed.

One open **evidence** item, not a defect: both new faces want `turn ⊃
user|response` pair structure the v0 projection cannot serve (double-confirmed,
W1-INT + W2-E lack 1 + G25). That lack is THE machine-cut package's ordering
evidence (ROAD Step 5; CONTRACT §20) — it is Sid's call, not this package's.

## 1 · QC-layer scorecard — what each layer caught, missed, and cost

The package ran **seven** distinguishable QC layers (the five-layer model plus
the Step-0 probe above it and the live wearing below the suite). The headline
of the scorecard: **the two most expensive layers to run — the probe and the
live wearing — each returned findings no cheaper layer could have.**

| Layer | Ran as | Caught (highest-value) | Missed / cost |
|---|---|---|---|
| **0 · Step-0 probe** | 1 fresh Opus subagent, JVM shape-proxy vs the real render path | The `:text-layout` **dead hook**: not an rt-node ctor param (`rect_tree.cljc:45`), attached by nothing shipped, `resolve-text-layout` has *never run in production*; riding it **doubles** wrap cost (measured ≈2×). → the one-wrap `:text-run` law (§6/G8). Also: extraction is cljs→cljc **porting** not reference (ui_primitives is cljs-only); "measure in primitive, arrange in engine" is load-bearing law; corrected ROAD's "sub-millisecond" → low-single-digit ms / 12–25 ms @1000 nodes. | Cost: 1 subagent + uncommitted probe code (later DISCARDED by lane A — different builder iface). Did NOT mount the dev-hook (>fence) — substituted a JVM flatten through the real `tree->rects`/`tree->text-ops`. A scoping judgment, not a miss. |
| **1 · Contract** | Fable authoring, every source claim re-verified at file:line | §8 schema check found **zero enum edits needed** (`supersedes` already registered, `relation_kernel.clj:58-72`); the `imp:asm:` **named-deliverable + foreign-read gate** rule (G18) killed the twice-fired latent class at birth. | **The §17 contract-TEXT error** (import-key as a 4th idempotency component) — the contract's own prose was wrong; every downstream layer that trusted it inherited the bug. Caught only at the W2 falsification pass (W2-F3). This confirms the pre-registered "every Fable contract so far carries ≥1 contract-text error" ledger (handoff §14). Also: G14(b)'s premise ("the sidebar list through an assembly") embedded a factual mismatch — the sidebar predates the vocabulary (caught at INT, honest negative). |
| **2 · In-lane gates** | Gates green in-context per lane (G1–G13, G17–G23) | Empirical de-risk *before* code: lane C probed the real corpus (247 events, `river-page` 64-block hard cap, no time-carrying API) and lane D ran the `/rama-pitfalls` protocol before writing — both went first-run green. | By construction sees nothing the fresh-context layers see (same context as the work). Zero cost beyond the lane run; this is the model working, not a gap. |
| **3 · Serial suite** | 46t/804a at close; every fixed finding got a **biting regression** (`face_gate_fixes_test`) | The regression namespace encodes each kill: F9 root-`:each`; W2-F2 4 poison shapes; W2-F3 two-import-keys→one-key; W2-F4 register→unregister→gone. | JVM-only: **blind to the two live-only lifecycle defects** (below). No cost; the correct scope. |
| **4 · Falsification-by-class** | 5 fresh Opus subagents/wave, default-fail, class-hunting | W1: 10 findings (4 MAJOR). W2: 14 findings, **3 CONFIRMED HIGH** — the poison-event **topology wedge** (F2: non-numeric `worn-at-ms` → `(long …)` throws inside the topology → `:retry-mode :all-after` wedges the partition / hangs boot replay); the **§17 contract-text error** (F3); the **rename-ghost** roster entry (F4: register never removed → `/face old-name` served an orphan). Plus the boot-Delay totality bomb (F7). | **Cost: ~503k subagent tokens (W2 alone).** Under Sid's mid-wave token flag, the 14 fixes were applied **by the orchestrating session at the gate** — no further subagent spend (see §7, the D-006 note). |
| **5 · Fable pass** | CLAUDE.md falsification protocol | Consolidated writers/readers/clearers per changed state; open doubts recorded **with named falsifiers** (W2-F5 two-files-one-name; the hardcoded `human:local` wearer; the receipt-at-HEAD via IPC). | Cheap; its value is the recorded-doubt discipline, not new kills. |
| **6 · Live wearing** | G15 (W1) · G24/G25 (W2), dev app over real `7c80ce2a` | **Caught a real defect no suite could, in BOTH waves** — see §2. Also: the name-vs-stem lesson; the minimap's absent `:reader-turn` rendering as an honest error card (the totality law, live). | Cost: the puppeteer/CDP harness + the **GPU-capture environment lack** — the Vulkan swapchain never composites into CDP on this box, so visual evidence is a labeled CPU rasterization of the live `!face-scene`. A native-session screenshot is still owed. |
| **7 · Daily use (D-001)** | **Not yet** — the loop's real close | — | Sid wears the three faces going forward (dev app left running); the wearing log is now the live desire-path instrument that decides which face earns primitive investment. |

## 2 · The two cross-cutting findings (the scorecard's spine)

**(a) The live wearing is a QC layer, not an acceptance demo.** It found, in
*both* waves, a runtime-integration lifecycle defect invisible to every JVM
layer above it:

- **W1 — the epoch-bump defect** (`W1-INT.md` G15, source in `file_viewer.cljc`):
  the first-light distill did NOT bump `util-fns/!ingest-epoch-atom` (only
  `ingest_watchers.clj:113` did), so INV-19 never re-pulled; re-issuing an
  identical `/face outline` couldn't re-fire the Electric watch (equal value →
  no propagation) — the face stayed **stuck empty, masking now-present truth**
  (the exact "state stuck masking future truth" class the falsification
  protocol hunts). Found by wearing; fix = the distill bumps the epoch like
  every watcher import; re-verified on a fresh JVM (auto-fill to 35/64, zero
  re-requests).
- **W2 — the projection-routing gap** (`W2-GATE.md` W2-F1, fix at
  `face_projection.clj:331` `resolve-projection-kind`): the W1 static
  `face->projection-kind` map knew only W1 names, so `/face boxes-face` served
  `:unknown-projection` → **empty scene**. Found by the first live run of the
  new faces.

Both are integration bugs — a write→render→reconcile lifecycle gap and a
data-binding gap — structurally beyond a unit golden's reach. This is D-001
("proves itself only in use") turned into a *gate*: the wearing is where the
seam between two green subsystems is actually exercised.

**(b) The contract-text-error class is real and only a fresh reader catches it.**
W2-F3 was not a code bug the code lane introduced — it was the contract's §17
*prose* naming `import-key` as a 4th idempotency-key component. Because
import-key changes on every EDITED save, embedding it mints a fresh key per
edit and **defeats the relation-kernel journal**, leaving only a racy read-back
as defense. Every layer that read §17 as *instruction* inherited the error; the
falsification pass, reading §17 as a *claim to falsify against the code*, is the
only layer positioned to catch it. §17 was amended in place (dated), the fix is
in `edge-specs` (`assembly_adapter.clj:469`, stable key; import-key → `:note`),
and a biting regression pins it. **This is the third consecutive package to
carry exactly one contract-text error** (code-atom, block-kernel, framework) —
the handoff's pre-registered prediction, now a confirmed pattern.

**(c) Two fully-parallel lanes, disjoint fences, zero collisions.** W2-D
(arsenal: `assembly_adapter.clj`, `face_arsenal.clj`, the `ingest_watchers`
classify-fn, the ONE `object_container.clj` `imp:asm:` branch, the projection
registry entries) and W2-E (plurality: `face_primitives.cljc` additions, the
ONE `face_assembly.cljc` `:child-w` edit, the two `.edn` faces) ran as
concurrent Opus subagents over disjoint file sets. The only crossing was a W1
test (`g13-serve-dispatch` pinning the registry to `[:conversation]`), resolved
at INT in one line — not a lane collision. W2-E ran the full suite over D's
in-tree edits and got 32t/529a green. The §2/§16 fence design (new files only;
named single-owner additive edits; shared runtime touched only at INT) held
under real concurrency — the strongest evidence yet for the
one-phase-per-fresh-CONTEXT amendment (2026-07-05): **both waves closed the
same day** because the lanes never blocked each other.

## 3 · What the next contract should do differently

Each rule traces to a concrete failure in THIS package — no speculative
hardening.

1. **A schema that fixes an object's IDENTITY must also fix how that object
   RESOLVES TO ITS DATA.** Failure: §8 fixed assembly identity/provenance but
   left the face→projection binding as a static code map (`face->projection-kind`);
   W2's new faces served `:unknown-projection` → empty scenes, found only live
   (W2-F1). The fix made the **roster** the resolver (a registered face routes
   to `:conversation`; the static map shrinks instead of growing), with
   `:assembly/projection` as the pre-named per-face override. The next contract
   that mints a kind of object with a data need names the resolution rule
   alongside the identity rule, in the schema section — not in a code map a
   later wave silently outgrows.

2. **Any lazily-booted runtime that feeds the render path must yield a
   poisoned-but-TOTAL value on failure, never a cached throw.** Failure: W2-F7 —
   the runtime `Delay`'s throw site sat outside every totality guard; a `Delay`
   **caches its throw and re-throws on every deref**, and W2's install-time
   roster pull forces it for every client → one boot failure = permanent black
   screen for ALL clients (L13: no `try` upstream). This generalizes L13 from
   "no `try` in `e/defn`" to "no un-guarded `delay` on the boot path." A gate
   class (`delay`-totality) belongs in any contract with a lazy runtime handle.

3. **The addressing key a user TYPES is part of the identity contract and must
   be pinned where the command is specified.** Failure: the name-vs-stem lesson —
   §17 said "identity on the NAME," but W1's `/face outline` command was
   filename-based; W2's Rama serve resolves by envelope name (`outline-face`),
   so `/face outline` now correctly error-cards. The mismatch surfaced only at
   the first live run. When a schema pins identity to a field, the command
   surface that lets a human address the object must pin to the same field in
   the same wave.

4. **An evidence-capture harness names the op-shape contract it consumes; a
   blank capture is a HARNESS bug until the op counts disagree.** Failure: the
   rasterizer's first two runs produced blank PNGs — rect ops carry FLAT
   `r g b a` keys while text ops nest per-node; the harness mis-mapped. The op
   counts always matched the render, so the render was never in doubt. A
   visual-evidence gate should assert op-count parity first and treat a blank
   frame as a mapping error, not a render failure.

## 4 · Mechanisms that earned their keep (do not drop)

- **The live wearing as a named gate layer** (§2a). The single highest-value
  mechanism of the package — it caught what nothing above it could, twice.
  Promote it in the skill from "acceptance gate" to "QC layer 6."
- **Roster-as-registry resolution** (W2-F1 fix). The face object knows its own
  data need; the static dispatch map shrinks per face instead of growing. The
  §8-consistent direction, and it dissolved the routing gap rather than patching
  it.
- **WAL-first ordering** (W2-D judgment 8; the `git_spine.clj:582-655`
  `/assert` precedent). The WAL line is written BEFORE the depot append — an
  append failure after the line converges at boot replay; the reverse order
  silently loses an acked wear. The wear log is the desire-path instrument;
  losing a wear defeats it. WAL-first + a wear-id journal is the cheapest honest
  durability for an in-memory dev kernel.
- **`imp:asm:` routing gate as a named deliverable** (G18). The twice-fired
  latent class (code-atom G-F2 `imp:clj:`, block-kernel F2 `imp:sense-block:`)
  was killed at birth this wave because the skill rule forced it into the
  contract as a deliverable *with* its foreign-read gate. The rule works; keep
  it.
- **Empirical de-risk before code** (lanes C, D). Probe the real corpus / run
  the pitfalls protocol first → first-run green. The green first run is the
  upstream layers' receipt, not luck.
- **One `.cljc` compiler, two call sites** (T18). Server ingest-validation and
  client wear-time compile call the SAME `face-assembly/compile-assembly` over
  the SAME `face-primitives/registry` — verdicts equal by construction, drift
  impossible while both exist. The G7 source-form diff pins the copies inside
  the shared namespace.
- **One-liner-carries-every-carve-out gate discipline.** G12 (read-source
  added), G20 (WAL first-line canonical), §8 (arsenal carve-out) — each amended
  in place with a dated note, the short gate text kept honest against its
  authorizing section.

## 5 · Residue for the NEXT contract (non-blocking; falsifiers named)

Ordered by how much they constrain the next package. Items 1–2 are
consumer/importer disciplines that MUST travel into the next contract (the
cycle-1 lesson: the importer-timestamp discipline was dropped and only the
recheck restored it).

1. **Pair structure = THE machine-cut evidence item** (double-confirmed). Both
   new faces want `turn ⊃ user|response` pair structure; v0 `:turns` are
   single-speaker with flat `:blocks`, so Boxes' 4-layer russian doll flattens
   to 3 and the reader loses its user/response split. This is the ordering
   evidence for the machine-cut package (ROAD Step 5; §20) — the single most
   important residue. It is Sid's call to open that package.
2. **Name-conflict row field** (W2-F5, mitigated + recorded). Two files claiming
   one `:assembly/name` silently merge (the native-claim branch compares neither
   source-ref nor import-key); today warn-only, last-import-wins. **Falsifier:
   two files, one name → pointer/revision flap by import order.** Fix = a
   per-row `:name-conflict` field (a `$$faces-by-name` row-schema change). Carry
   into the arsenal's next contract.
3. **Reader focus control** (`:actions`-era, W2-INT lack 2). The data serve
   landed (`:reader-turn`, `:params {:focus-turn}`-aware); picking the focused
   turn by click/scroll is D-008 item-5 gesture work.
4. **Paging** (W1 lack 1, carried). `river-page` has no cursor; faces wear the
   first ≤64-block page of a 247-event conversation, surfaced honestly as
   `:conversation/paging-lack`. Full paging is block-kernel CONTRACT §10 scale
   work, D-001-gated. First data-work item.
5. **Dev-boot relation-kernel attach** (W2-INT lack 5). The dev wearing boot
   carries no rk runtime → envelope lineage edges skip (honest T17-class gap;
   G19 proves them in-suite). Pre-named extension.
6. **fsync boundary** (W2-GATE recorded). No fsync on the WAL (matches the
   `/assert` precedent; dev in-memory scope). The honest durability boundary;
   a durable-future item, not a dev-scope defect.
7. **Post-wave harmonization** (CONTRACT §2's pre-named extension point). Rename
   `ui_primitives.cljs` → `.cljc`, delete the G7-pinned copies, keep the
   callers — entered only after assemblies wear in (D-001 pacing). Folds in a
   real observation from this retro: the G7-pinned copies still carry literal
   `0.56`s (`face_primitives.cljc:139/140/142/168/299`) because G7 forbids
   editing the copy to match the originals; the `fallback-char-width` constant
   (`:403`) governs only NEW face code. At harmonization those literals collapse
   to the shared advance — the copies stop existing.
8. **Importer disciplines that must be contract-clauses next time**: (a) any new
   `imp:` prefix names its `extract-object-key` branch + a foreign-read routing
   gate (the twice-then-thrice-fired class); (b) WAL-first + wear-id-journal
   idempotency for any append-only usage log; (c) the roster-reconcile-on-every-
   accepted-import discipline (W2-F4 — a register that never unregisters lies
   about wearability on rename); (d) the one-`.cljc`-compiler-two-call-sites
   parity rule (T18) for any client/server verdict pair.

## 6 · Traps that fired vs. traps that held

- **Held (cited by number in code, never fired):** T1 (Electric-native faces —
  the seam law held everywhere pushed), T5 (fn-values persisted — persisted =
  keyword+address only), T8 (per-face transport — ONE generic artery, no face
  dispatch in Electric), T11 (assemblies as a new row type — rode OC), T12
  (projection scope creep — read-only by construction, G12/G21 mechanical
  scans), T14 (face list from the fs — reads Rama), T15 (wear fused into serve —
  separate outbox + write e/defn; scrub re-pulls append NOTHING, asserted live),
  T18 (one compiler), T19 (`.edn` by bare extension — per-watcher classify-fn).
- **Fired and was caught by a QC layer (the ledger working):** T3/wrong-layer
  validation (F9 root-`:each` compiled clean, error-carded only at apply →
  compile-time root-buildable check). T13 hash-compare skip (F8/W2-F11 — a hash
  leaked into a node id / a sibling flow still hash-gated → value-compare). T6
  index-fallback ids (counted honestly in the apply-report). T17 dual-append
  non-atomicity (the honest wearable-but-unlisted window, converged by the next
  event/sweep).
- **The one real DESIGN finding inside a seam** (lane D, no kernel edit): OC's
  native-identity law rejects content change on a stable container, so the face
  document container is materialized as an **IDENTITY ANCHOR** (nil inline
  content; bytes ride the revision chain via `read-current-revision`). Resolved
  in-seam by materialization shape — the stop-clause discipline working (fix,
  cite the precedent, move on; no contract change).

## 7 · D-006 evaluation notes (→ decisions.md)

- **Criterion 1 (traps ledger as a live constraint):** MET, strongly. T1–T19
  are cited by number in code comments and each fresh gate checks them; §6
  ("fired and was caught") is the ledger doing its job. The `imp:asm:`
  named-deliverable rule (skill-derived) killed the thrice-fired latent class at
  birth.
- **Criterion 2 (the counterfactual reproduction probe):** was **WAIVED
  2026-07-11** under the pre-registered reopen (commit `ea68ec8`) — no new data
  from this package changes that.
- **The in-session-fixes-at-gate pattern under a token flag (new D-006 data
  point):** W2's 14 falsification findings were all fixed by the ORCHESTRATING
  session at the gate — no re-dispatch to subagents — on Sid's explicit mid-wave
  token flag (~503k already spent on the falsification pass). This flexed the
  "authoring phase applies fixes, validation does not re-run" rule under a cost
  signal **without losing the kill record**: every fix got a biting regression
  in `face_gate_fixes_test`, the suite re-ran green after each. Evidence that the
  package pattern responds to cost pressure by moving the fix-authoring inline,
  not by dropping verification.
- **The two-waves-in-one-day cadence:** both waves closed 2026-07-11. The
  one-phase-per-fresh-CONTEXT amendment (2026-07-05) is what enabled it —
  subagent lanes in parallel, judgment held in the orchestrating Fable context,
  QC consolidated at one end-gate per wave. §2c (zero collisions under real
  concurrency) is the falsifier that did NOT fire. This is the amendment's
  strongest confirmation to date.

## 8 · Routing (step 5)

- **`memory/implementation-quirks.md`** ← the coding gotchas (§below in the
  routing commit): the `Delay`-caches-its-throw boot-totality rule; the rect-ops-
  flat vs text-ops-nested rasterizer mapping; the bundled-Chromium-has-no-WebGPU
  driver requirement; `imp:asm:` two-segment routing (leading-object-key
  truncates at `"asm"`); the identity-anchor container shape for stable-identity
  OC objects.
- **`/work-package` SKILL.md** ← PROPOSED amendments (Fable signs; the
  canon-authorship rule): promote the **live wearing to a named QC layer**;
  add the **schema-fixes-identity-must-fix-data-resolution** rule; add the
  **delay-totality gate class**; add the **evidence-harness op-shape** rule;
  record the **in-session-fixes-at-gate-under-token-flag** adaptation. Filed as
  PROPOSED — not applied to the skill this session.
- **`decisions.md` D-006** ← the §7 evaluation notes.
- **Board thread 8** ← pruned to a one-line done-pointer; next-up set.

---

## Adversarial recheck (step 4) — appended after the fresh-subagent pass

_[pending — a fresh-context subagent re-runs the 8-namespace suite at committed
HEAD and re-verifies every scorecard claim against source + git, default-fail.
Its corrections land here; where it disagrees with the body above, it wins.]_
