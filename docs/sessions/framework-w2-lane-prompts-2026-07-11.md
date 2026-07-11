# Framework W2 lane prompts — PINNED to CONTRACT v2 §§16–21 (2026-07-11)

Consumed by the W2 orchestrating session's parallel Opus 4.8 subagents.
Every §-ref resolves against `docs/current-mental-model/build/framework/CONTRACT.md`
at v2. Lanes commit NOTHING (the orchestrator owns commits). Each lane ends
with its artifact in `docs/current-mental-model/build/framework/` and a
≤15-line NOW entry APPENDED to `build/framework/NOW.md`.

---

## Lane D — the arsenal (gates G17–G21)

You are lane W2-D of the faces-as-assemblies framework package (Softland).
Binding docs, in order: `docs/current-mental-model/decisions.md` ›
`docs/current-mental-model/build/framework/CONTRACT.md` (v2 — read §§7, 8,
16, 17, 18, 19, 21 in full; §§4–6 for context). The thread file
`build/framework/NOW.md` is a baton, not truth.

**BOOT FIRST, before any code:** the `/rama` skill, then `/rama-pitfalls`.
Non-negotiable.

**Job.** Realize CONTRACT §8 per the §16 placement rulings:
1. `src/app/server/rama/object_container/assembly_adapter.clj` (NEW) — the
   `.edn` assembly source adapter, `markdown_adapter.clj:443-517` as the
   template: object-key `asm:<assembly-name>`, import key
   `imp:asm:<object-key>:<sha-256(source-ref-key:source-hash)>`, family
   `:assembly`; validation at ingest via the SAME `.cljc` compiler + registry
   the client uses (§17, trap T18); malformed `.edn` still ingests as source
   with honest `{:assembly/valid? false :assembly/errors […]}` (R1
   discipline); envelope provenance fields → lineage edges per §17
   (`append-relation-request!`, `relation_kernel.clj:925`; idempotency per
   the `git_spine.clj:224` discipline; EXISTING D-004 kinds only — anything
   else is a stop clause).
2. `src/app/server/rama/face_arsenal.clj` (NEW) — intent-only micro-kernel
   per `kernel.clj` KERNEL-SHAPE: one depot (wear events + face-registered
   events), PStates `$$wear-events-by-face` (append-only),
   `$$wear-counts-by-face`, `$$faces-by-name` (POINTER index — no assembly
   material field, ever; traps T14/T16). `record-wear!` server fn: server
   stamps `worn-at-ms`, appends depot + one WAL line
   (`data/face-wear-log.ednl`, the `git_spine.clj:582-655` /assert
   precedent); duplicate wear-id = journaled no-op. Boot replay fn for the
   WAL (idempotent).
3. `ingest_watchers.clj` (FENCED additive edit, you are sole owner):
   optional `:classify-fn` in cfg (default = current behavior, zero change
   for existing callers); `import-assembly!` through the EXISTING seam
   (`append-object-container-request!` → `await-…-decision`); on accepted
   decision keep the epoch bump AND append the face-registered event to the
   arsenal depot (trap T17 — idempotent by import-key). The assembly branch
   fires ONLY through the faces watcher's own classify-fn (trap T19 —
   `deps.edn` must never ingest as a face).
4. `object_container.clj` — ONE additive `imp:asm:` branch in
   `extract-object-key` (:289-306), mirroring `imp:clj:`. The wave's ONLY
   kernel edit (pre-authorized, §16). Ships WITH the G18 foreign-read gate.
5. `face_projection.clj` — registry gains `:assembly` (wear-time source
   serve: name → `$$faces-by-name` → OC source read → `{:assembly/source
   :assembly/valid? :assembly/errors :assembly/name :face/rendered-at-ms}`)
   and `:face-list` (names + status + wear counts + last-worn). §7 law
   unchanged: server-side dispatch, `serve` total, read-only (trap T15: NO
   write reaches serve). Reads go through the arsenal's own named read fns.
6. `test/app/face_arsenal_test.clj` — IPC suite driving gates G17–G21
   (read each gate's FULL text in §19 and satisfy it literally, carve-outs
   included). Deterministic barriers, never polling; negative invariants get
   physical PState readers; minimize IPC launches (one deftest unless named
   interference); task counts injectable.

**Fence.** You touch ONLY the six files above. `face_assembly.cljc`,
`face_primitives.cljc`, `electric_flow.cljc`, `file_viewer.cljc`, all client
files: READ-ONLY. The faces dir may hold only `outline.edn` while you run
(lane E lands more in parallel) — G17's receipt runs over whatever the dir
holds plus your synthetic fixtures for the edited/malformed/identical-resave
classes; the full-dir receipt re-runs at G26.

**Traps:** cite T14–T19 (and any W1 trap you lean on) by number in code
comments where they bind.

**Stop clauses (§21):** escalate in your artifact + NOW entry, never
improvise — new relation kind or import-key family beyond `imp:asm:`; the
arsenal can't meet G20 without holding material; binding-doc conflict.

**Done =** G17–G21 green in-context (run them), artifact
`build/framework/W2-D-arsenal.md` (what built, gate evidence, judgment calls,
flags for INT), ≤15-line NOW entry appended. Commit nothing. Your final
message: a compact summary + the artifact path + any INT flags.

---

## Lane E — the plurality (gates G22–G23)

You are lane W2-E of the faces-as-assemblies framework package (Softland).
Binding docs, in order: `docs/current-mental-model/decisions.md` ›
`docs/current-mental-model/build/framework/CONTRACT.md` (v2 — read §§4, 5,
6, 16, 17, 19 in full). The thread file `build/framework/NOW.md` is a baton,
not truth.

**Job.** Transcribe the two Sid-picked design-round candidates —
**1e Boxes** and **1f Minimap + Reader** (picked 2026-07-11) — as assemblies
over the REAL vocabulary. Read
`build/framework/design-round/INTENT.md` FIRST (verbatim intent + judge
lines per candidate, plus the transcription notes: interaction verbs are
v0-out `:actions`-class lacks; the two-pane chat-log frame is the
exploration harness, not the face; Minimap+Reader's own strip+reader panes
ARE its anatomy).

**Inputs (read before authoring):**
- The design sources at
  `docs/current-mental-model/build/framework/design-round/` (pulled from the
  claude.ai design round; `Block Views.dc.html` carries each candidate's
  intent + judge notes; `BlockExplorer.dc.html` carries the six modes'
  actual anatomy — find your two modes and extract their band/pane/rail/
  badge/stub/proportionality rules; the CSS is claude-design's medium, the
  LAND re-renders through its own tokens — transcribe STRUCTURE, never
  pixel-port).
- `resources/public/faces/outline.edn` — the worn exemplar (bind paths = the
  §7 data contract: `:turns [{:id :speaker :order :blocks [{:id :kind :text
  :order :time-ms}]}]` + the `:conversation/*` meta keys).
- `src/app/client/workspace/face_primitives.cljc` (16 prims — you own this
  file's ADDITIONS this wave; G7-pinned copies untouched) and
  `face_assembly.cljc` (READ-ONLY unless the ONE named extension fires:
  child `content-w` narrowing, W1-INT Lack 2 — if a transcription genuinely
  needs it, take it minimally + regression test, and flag it).
- `test/app/fixtures/faces/` + `test/app/face_primitives_test.clj` for the
  golden/measure discipline; `build/framework/W1-INT.md` §Lacks for the
  known vocabulary gaps (trailing-gap `:pad-after` is a pre-named candidate
  if your faces need it).

**Rules.** Grammar §4 is closed — arrangement only, the guard bites lists/
symbols; anything wanting logic goes to a primitive (real cljc, gap-fill) or
is a stop clause. New primitives: measure-inside-the-primitive (§6), wrap
ONCE via `wrap-line` where prose (`:text-layout` NOWHERE — dead hook), width
from `(:content-w geom)`, the ONE `fallback-char-width` constant, keyword
`:prim` names in the registry map. Envelope per §17: `:assembly/name`
(kebab, = the `/face` handle), `:assembly/grammar 0`, `:assembly/belief`
(one honest line), `:assembly/status :candidate`, `:assembly/author`.
NO `:assembly/birthed-by` — the design-round conversation is NOT in the OC;
a fake edge would make the map lie; log it as the named provenance lack.

**Deliverables.**
- `resources/public/faces/<name>.edn` × 2.
- Gap-fill primitives in `face_primitives.cljc` (each cited to the design
  element it exists for).
- `test/app/face_transcription_test.clj`: **G22** — each face compiles CLEAN
  against the real registry; golden rt-tree over a committed
  projection-shaped fixture; apply-report zeros; positive content-h; each
  new prim's measured golden (G8 discipline). **G23** — per face, a NAMED
  anatomy checklist extracted from the design file (each item citing the
  design source line/element) asserted against the golden tree (presence +
  relative geometry); every inexpressible design element LOGGED as a named
  lack (D-005), never improvised around.

**Fence.** You touch ONLY: `face_primitives.cljc` (additions),
`resources/public/faces/*.edn` (new files), `test/app/face_transcription_test.clj`,
`test/app/fixtures/faces/*` (new fixtures), and — only if the named
extension fires — `face_assembly.cljc` + its test. Everything else
READ-ONLY.

**Done =** G22–G23 green in-context (run them), artifact
`build/framework/W2-E-plurality.md` (per-face anatomy checklist, lacks
logged, new-prim inventory, judgment calls, INT flags), ≤15-line NOW entry
appended. Commit nothing. Final message: summary + artifact path + INT flags.
