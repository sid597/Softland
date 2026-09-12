# first-light A — PHASE_0 (fresh-context re-derivation)

2026-07-17 · derived from CONTRACT.md + DIRECTION.md + RECON.md +
decisions.md §first-light/§Durable-ground, no prior package context (the
point of this layer: surface drift/ambiguity before P1). A derivation
layer, not a code review. RECON receipts are trusted as ground truth;
two load-bearing ones (relation allowlist, revision-id determinism) were
spot-verified in source and are cited inline.

Redlines seen (§9, Sid's calls, not P0's to resolve): (1) boot-to-ground
default vs flag-staged; (2) `:references` as the wish edge vs a named Wish
kind now; (3) candidate naming `asm:<name>--wish-<n>`. Flagged on read.

---

## A. Primitives → plane map (P0's named artifact)

DIRECTION §primitives (line 89), all 15. Plane · existing organ (receipt)
· active-in-A? · smallest missing surface (the minimal concrete thing that
must exist for the primitive to be REAL in A).

| # | Primitive | Plane | Existing organ (receipt) | A? | Smallest missing surface |
|---|-----------|-------|--------------------------|----|--------------------------|
| 1 | address | identity | OC object-keys `asm:<name>`/`du:…:sense-block-v0:<path>`/`imp:…` (framework CONTRACT:363-372,831-833; RECON B, C:110); scene-store per-block index over `[:data :address]` (scene_store.cljc:27-41) | **Active** | (a) flip applies per-block `stamp-block-addresses` (scene_store.cljc:224-244) on the main-face slot, not just apply-assembly's ROOT stamp (face_assembly.cljc:511-524) — T7; (b) new `imp:ep:` episode key prefix (§5.1) |
| 2 | containment | identity/structure | OC container + conversation container; scene-store per-vi container registry (scene_runtime.cljs:34,55-72) | **Active** | main face gets its OWN dedicated store container (render.cljs:210-246 gate+container — RECON A) + episode = conversation container minted at genesis birth (§3) |
| 3 | selection | gesture | scene pick + context-bundle (scene_runtime.cljs:188-218,369-393) | **Active** | main face flipped INTO the store so pick walks its slots — today nil because main face lives outside the store (mouse.cljs:413-421; RECON A:28,58-60) — P1/G2 |
| 4 | arrangement | material | pure-EDN assemblies + total never-throws compiler + revisioned source (face_assembly.cljc; face_projection.clj:421-469) | **Active** | string-source assembly import entry (§5.2, RECON B gap 1) + client compile-by-name second artery for preview (RECON B gap 3; spawn-by-name stop-clause scene_runtime.cljs:370-378) |
| 5 | reference | relation | relation kernel `:references` kind (relation_kernel.clj:58-79) | **Active** | the wish→target `:references` coupling edge via /api/relation/assert (server_jetty.clj:809-854) — §4 · **see Flag A** on target-kind |
| 6 | mark | relation/gesture | same `:references` edge; DIRECTION line 40 "mark ≠ write" | **Active (late-bound onto reference)** | none new — A serves mark AND reference with one `:references` edge; a dedicated Wish/mark kind waits for recurrence (§4; redline §9.2) |
| 7 | actor | identity/provenance | `asserted-by` in relation payload (relation_kernel.clj:118-125); `:assembly/author` | **Active** | utterance minted `asserted-by: sid` at utterance time (§5.1; today argv-only non-durable, RECON C:111-120); candidate carries `:assembly/author` = agent, visibly silver (§4) |
| 8 | provenance | cross-cutting | two clocks; `:assembly/src-path` stamped at apply (face_assembly.cljc:395-412); asserted-by | **Active** | pick returns `src-path` recorded on the wish (P1/G2) + the "Why is this here?" trail projection over existing reads (§4 explain, P5/G8) |
| 9 | proposal | lifecycle state | `:assembly/status :candidate` (framework CONTRACT:376-379) | **Active** | candidate fork `asm:<name>--wish-<n>` w/ `:candidate` + `based-on` + author, via §5.2 string-source import (P4); + second compile artery for preview |
| 10 | ratification | lifecycle event | accept = import onto original name → new revision (§4; OC chain object_container.clj:235-237) | **Active — LOCAL accept form only** | accept semantics = import candidate source onto ORIGINAL name + lineage `produced`/`supersedes` (D-004 kinds), P5/G7. The constitutional SCOPED-PROMOTION ratification (gold, blast-radius, human scopes — DIRECTION §axes) is **deferred to B** |
| 11 | scope | cross-cutting | DIRECTION §axes "gold stays narrow" | **Deferred (B)** | — A is single local scope by construction; scope-widening is inheritance |
| 12 | inheritance | lifecycle | — | **Deferred (B)** | — explicit non-goal (§1 non-goals; DIRECTION package boundary) |
| 13 | branch | version-control | assembly fork via based-on/new-direction (framework CONTRACT:363-372) | **Active** | candidate forks under its OWN name w/ `based-on` edge (§4, T3) — rides §5.2 import + existing based-on kind |
| 14 | diff | version-control | per-slot `update-nodes-by-address` — ZERO callers (F8; RECON A:38) | **Deferred** | — copy-echo + per-slot echo diff staged (§1 non-goals); "preview may full-rebuild"; the explain-trail is provenance, not a content diff |
| 15 | reversal | version-control | OC append-only revision chain (object_container.clj:235-237) | **Active** | reverse re-imports the prior revision's content-text onto the name (§4; RECON B:421-469) — **see Flag B** (content-addressed revision-id may dedup this to a no-op) |

**Active-in-A (12):** address · containment · selection · arrangement ·
reference · mark · actor · provenance · proposal · ratification (local
form) · branch · reversal. **Deferred (3):** scope · inheritance · diff.

---

## B. Derived requirements, phase by phase (placement inherited verbatim)

Global placement (§2, binds every requirement below): **zero new Rama
modules**; **kernel edits only where §5 names them** (else §8 stop clause);
server code in existing product ns (`face_projection.clj` projections,
`assembly_adapter.clj` string-source entry, `file_viewer.cljc` boot) + AT
MOST one new ns for the episode seam (`app.server.episode`, P2's call);
**client = the scene-store plurality path PROMOTED to product** (committed,
keyboard/command-reachable, no console — the `window.sceneFaces` dev
affordance retired).

### P1 — the P3c-minimum seam (§7 P1; §2 client placement; RECON A)
- **Op:** flip the main face from the singleton `<face-assembly` builder
  (editor_compute.cljs:485-534) into a store slot in its OWN dedicated
  container (render.cljs:210-246; scene_runtime register/refresh for
  `:face-main`). No durable/kernel write in P1 — client store + pick only.
- **Inv (T5):** overlay threaded — `overlay-face-context` (block_edit.cljc:
  271-301, watching !edit-state + !truth-overlay) feeds the per-vi store
  build (`build-face-tree` runs apply-assembly on the RAW projection today,
  scene_store.cljc:250-271) so caret/pending-input/refusal survive the flip.
- **Inv (T6):** legacy face-mode consumers silenced the same frame
  (combined_text.cljs:301-302; mouse.cljs:424-437 fallback retired) — no
  double-draw.
- **Inv (T7):** BOTH stamps on the flipped slot — apply-assembly ROOT
  (face_assembly.cljc:511-524) AND per-block `stamp-block-addresses`
  (scene_store.cljc:224-244).
- **G1:** block-write typing drill on the FLIPPED face — caret/pending-
  input/refusal visible; narrow-echo p95 ≤ the P5d cluster baseline (52ms
  browser-path — **see Flag E**); zero visual regression on the worn
  conversation. **G2:** browser pick on a conversation block returns real
  `{vi, address, src-path}` (today nil, RECON A:28,58-60).

### P2 — the ground + the episode (§3, §5.1, §7 P2; RECON C)
- **Op (T9, redline §9.1):** boot wears the conversation face over the
  living episode instead of the file workspace (file_viewer.cljc boot;
  today electric_flow.cljc:534 loads its own source → `:file-workspace`,
  RECON C:139-143). File workspace stays one keystroke away (unprojected
  world); world-state-zero blankness is birth-only.
- **Op:** genesis episode = conversation container + trail; conversation-id
  minted at birth; NO new ontology (§3). Seam in AT MOST one new ns
  `app.server.episode` (§2; recorded in NOW).
- **Entity×write — utterance (§5.1 kernel deliverable):** Sid's typed
  utterance lands in OC as addressable material `asserted-by: sid` BEFORE
  the agent sees it, riding the import family via a NEW `imp:ep:` prefix =
  extract-object-key routing branch + foreign-read routing gate. **See
  Flags C, D and §8 stop clause.**
- **Op:** resident agent = the EXISTING CLI lane (subscription auth, NO API
  keys — hard rule, RECON C:121-125); summon on that lane (server_jetty.clj).
- **Entity×write — responses (T8):** post-turn harvest+distill of the
  episode = the proven transcript organ (block_distiller.clj:39-71) now
  triggered at turn end (**see Flag F** on trigger), idempotent by import
  keys `imp:tr:`. No new durable chat pipeline.
- **G3:** typed utterance readable via cluster foreign read with sid
  provenance BEFORE the agent replies; kill dev server mid-episode → reboot
  → episode resumes with the utterance present. **G4:** an agent turn's
  material lands via post-turn distill; re-run = receipted no-op (T8); the
  §5.1 foreign-read routing gate passes.

### P3 — the wish (§4, §7 P3)
- **Entity×write — wish unit (T1):** an OC unit IN the episode — durable,
  addressable, `asserted-by` its wisher — minted through the episode
  material path (same create question as the utterance, §5.1 machinery).
- **Entity×write — coupling (redline §9.2):** a `:references` relation edge
  (EXISTING kind, zero enum growth) from the wish unit to its target, via
  /api/relation/assert (server_jetty.clj:809-854). **See Flag A** (the
  target-kind allowlist has no block/unit kind).
- **Inv (T2):** wish-ness lives in the material + projection, NEVER the edge
  kind or a transport field. The wish records BOTH the material address AND
  the face `src-path` from a REAL pick (P1's seam, scene_runtime.cljs:
  188-218).
- **Op:** visible projection coupling the wish to its target in the face —
  a new projection in `face_projection.clj` (§2 server placement).
- **Carve-out (§4 last bullet; G9/T10):** a behavioral (non-arrangement)
  friction still lands as material + coupling; the land says so; it routes
  to the code lane; the arrangement gate keeps waiting.
- **G5:** wish unit + edge readable (kernel foreign reads); the wish renders
  coupled to its target in the face; the wish records address + src-path
  from a REAL pick.

### P4 — proposal + membrane (§4, §5.2, §7 P4)
- **Entity×write — candidate (T3, redline §9.3):** agent proposal lands as
  assembly source under its OWN name `asm:<name>--wish-<n>`, `:assembly/
  status :candidate`, `based-on` edge to the original, `:assembly/author` =
  agent (visibly silver) — entering via the §5.2 string-source import entry
  (non-file caller of `assembly-source-import-request`, adapter-level in
  `assembly_adapter.clj`; likely zero topology change). Fork-under-own-name
  is what keeps the original's `current-revision-id` from auto-advancing.
- **Op (T4):** preview = the draft-face membrane on the plurality path —
  the candidate compiles CLIENT-SIDE BY NAME (the SECOND assembly artery,
  P3c staged item pulled in; spawn-by-name stop-clause today scene_runtime.
  cljs:370-378) and renders as its OWN view-instance against the LIVE
  conversation projection; the worn face untouched. This artery sits BESIDE
  `!assembly-request` and must never clobber `!face-compiled`.
- **G6:** while the preview is visible, a cluster read shows the ACTIVE
  face's `current-revision-id` UNCHANGED (T3); the preview view-instance
  renders the real conversation material (T12); killing the preview leaves
  the worn face untouched.

### P5 — accept · reject · reverse · explain (§4, §7 P5)
- **Op ACCEPT:** import the candidate's source onto the ORIGINAL
  `asm:<name>` (new revision through the existing chain; current-revision-id
  advances; the Step-7A loop re-renders the inhabited face) via the §5.2
  entry; PLUS lineage `produced`/`supersedes` edges recording wish →
  proposal → decision → revision (D-004 kinds ONLY; a new kind = §8 stop).
- **Op REJECT:** the fork stays in the trail as `:candidate` (or a
  `dead-end` edge — Sid's word); the active face untouched.
- **Op REVERSE:** re-import the prior revision's content onto the name
  (history append-only; nothing deletes). **See Flag B.**
- **Op EXPLAIN:** "Why is this here?" at point of use — pick the changed
  region → the trail chain (wish material → proposal material → decision →
  revision) served as a PROJECTION over existing reads (new projection,
  `face_projection.clj`). First lived form of the inspect verb.
- **G7:** accept → new revision on the original name + lineage edges + the
  inhabited face re-renders; reject → active unchanged, fork in the trail.
  **G8:** reverse restores the prior revision (content match via cluster
  read) with history intact; "Why is this here?" answers with the chain at
  point of use.

### P6 — metabolism (§7 P6; DIRECTION §the-first-repair; T10)
- **Op:** Sid wears the conversation for REAL work until a REAL arrangement
  friction appears, then the §4 loop runs end to end. No deadline, no
  fabrication — a staged specimen fails BY DEFINITION; the metabolism gate
  is not drillable. No code deliverable (P6 cannot be delegated, §11).
- **G9:** DIRECTION measures recorded — staged wish-to-worn latency (wish →
  visible request → first candidate → functioning preview → accepted
  revision → truth echoed) and terminal-escape count ZERO for the
  arrangement repair. G9 closes the package; until it fires it sits
  OPEN-WAITING with G1–G8 green.

---

## C. Ambiguity / conflict flags (flag, do NOT resolve)

**Flag A — the wish `:references` edge target-kind.** §4 couples the wish
"to its target by a `:references` relation edge" and the wish "records the
material address" from a pick that returns a conversation BLOCK address
(`du:…:sense-block-v0:<path>`, RECON C:110). But the route-side allowlist
is `#{:container :source :doc-file :conversation}` (VERIFIED
server_jetty.clj:813-825) — no block/unit kind. Two implementable readings:
(a) the edge targets the CONVERSATION (`:conversation`) or the block's
container (`:container`) coarsely, with block-precision living ONLY in the
wish material; (b) the edge targets the block itself, requiring an allowlist
addition (a route-level edit beyond §5's two deliverables → §8 stop-clause-
adjacent). Collides: CONTRACT §4 × RECON C:126-133 (allowlist).

**Flag B — reverse under content-addressed revision-id + no active
pointer.** §4/G8: "Reverse = re-import the prior revision's content …
restores the prior revision." But `import-revision-id` = `rev:<object-key>:
<sha256 target-id>:<sha256 import-key>` and import-key is a content sha
(VERIFIED object_container.clj:113-115; RECON B:831-833), so re-importing
identical prior content yields the SAME revision-id that already exists in
the chain, and RECON B gap 2 states there is "no active-vs-latest pointer,
no accept semantics." Two readings: (a) the duplicate import is a journaled
no-op → current-revision-id STAYS at the changed revision and reverse
silently does nothing; (b) reverse must explicitly re-point
current-revision-id to the prior revision-id — a pointer move RECON says
does not exist. Collides: CONTRACT §4/G8 × RECON B:831-833 + object_container
content-determinism.

**Flag C — utterance durability "riding the import family" vs no
create-from-blank path.** §3/§5.1/G3: the utterance is durable at utterance
time "riding the import family" adapter-level. RECON C:119-120: "no
`:object/create` from blank ground"; block-edit targets EXISTING units
(`:target/not-found` rejected); the demonstrated import entries are the
file-watcher (assembly) and BATCH transcript distill (conversation) — no
single-utterance native create is shown. Two readings: (a) `imp:ep:` is a
purely adapter-level caller minting one unit through existing import
topology (no topology change → proceed); (b) minting a net-new durable unit
from blank ground at utterance time needs a create path that does not exist
→ topology change → §8 STOP. The contract half-anticipates this (§8 third
stop clause) but does not establish that an adapter-level single-utterance
import entry EXISTS. Collides: CONTRACT §3/§5.1 × RECON C:119-120.

**Flag D — the utterance is minted twice (imp:ep: and imp:tr:).** §3 mints
Sid's utterance at utterance time under `imp:ep:` (native episode), AND §3
distills "the episode" post-turn via the transcript organ under `imp:tr:`
(`du:…:sense-block-v0`). The CLI jsonl the distiller harvests contains the
user turn too (RECON C:116). T8 idempotence is deterministic WITHIN a
prefix/organ; it does not reconcile the SAME utterance across two prefixes
and two object-key schemes. Two readings: (a) post-turn distill mints only
AGENT material, skipping utterances already durable → dedup across paths;
(b) it re-mints the whole episode including the utterance → the utterance
exists as two distinct units. Collides: CONTRACT §3 (utterance path) ×
CONTRACT §3 (post-turn distill) × T8.

**Flag E — G1's "p95 52ms browser-path" P5d baseline (soft; not a
contradiction).** G1 thresholds narrow-echo against "the P5d cluster
baseline (p95 52ms browser-path)." The provided inputs (decisions.md:61,83;
memory) carry only the on-cluster echo p95 7.86ms and stream echo p95
7.66ms — different measurement segments (server↔cluster vs full browser
round-trip). Not a hard contradiction, but the 52ms figure's provenance is
not corroborated within the P0 input set; the implementer should confirm the
baseline source before treating G1 as pass/fail.

**Flag F — post-turn distill auto-trigger vs "ingest explicit" (soft).** §3
triggers the distiller "at turn end." decisions.md:90 states "Boot-time
ingest is OFF the startup path (`bin/land ingest` explicit)." Turn-end ≠
boot-time, so not forbidden — but it introduces a NEW automatic ingest
trigger where the durable-ground stance is explicit invocation. Two
readings: (a) a sanctioned new trigger (boot path stays clean); (b) friction
with the "ingest explicit" discipline. Surfaced, not resolved.

(No conflict found between the contract's placement/traps and DIRECTION's
law or the ratification wording; A/B/C are the internal-contract-vs-RECON
seams, D is intra-contract, E/F touch the number/ops ledger.)

---

## D. Trap cross-check (binds which phase · enforced or merely mentioned)

- **T1 (message = the grave).** Binds P2. G3/G4 **enforce** addressability +
  actor provenance via foreign read; "causal parents" / instance→type guard
  is **mentioned only** — no gate asserts causal-parent linkage. *Partial.*
- **T2 (semantics never leak into transport).** Binds P3, P5. **Mentioned,
  not gate-enforced** — G5 checks the wish/edge readable, but no gate asserts
  ActionRequest/relation envelope fields stay clean. *Weak.*
- **T3 (candidate onto active name = silent flip).** Binds P4, P5. **Enforced
  — G6** directly reads that the ACTIVE face's current-revision-id is
  UNCHANGED while the preview is visible. *Strong.*
- **T4 (worn-face compile guard intact / second artery).** Binds P4. G6
  **enforces** "worn face untouched / killing preview leaves it untouched";
  the specific "never clobber `!face-compiled`" publish-guard claim is
  **mentioned**, not directly asserted. *Partial.*
- **T5 (overlay across the flip).** Binds P1. **Enforced — G1** requires
  caret/pending-input/refusal visible on the flipped face. *Strong.*
- **T6 (double-draw).** Binds P1. **Enforced — G1** "zero visual regression
  on the worn conversation" catches a double-paint. *Strong.*
- **T7 (stamp BOTH identities).** Binds P1 (enables P3/G2). **Enforced — G2**
  a block-level pick only resolves if the per-block stamp is applied.
  *Strong.*
- **T8 (post-turn re-distill idempotent).** Binds P2. **Enforced — G4**
  "re-running the distill is a receipted no-op." *Strong* (but within-path;
  cross-path double-mint is Flag D, uncovered).
- **T9 (boot must not orphan the file workspace).** Binds P2. **Mentioned,
  not gate-enforced** — G3 checks episode durability/resume; no gate asserts
  the file workspace stays reachable. *Weak.*
- **T10 (no fabricated friction).** Binds P6. **Enforced structurally — G9**
  is non-drillable; a staged specimen fails by definition. *Strong.*
- **T11 (serve-path totality under cluster hiccups).** Binds P3–P5.
  **Deliberately not gated** — contract states "non-blocking to drill,
  load-bearing to design for." *Weak by design.*
- **T12 (wear the real corpus).** Binds P3–P6. **Enforced** — G3/G5/G6/G9
  name the MIGRATED conversation + episode material / a REAL pick / the real
  conversation material, not fixtures. *Reasonable.*

Weakest enforcement: **T2** (no gate), **T9** (no reachability gate), **T11**
(un-gated by design); partials **T1** (causal parents) and **T4** (publish-
guard specifics).
