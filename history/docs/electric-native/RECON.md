# Wire-seam recon — evidence register

Banked 2026-08-13 out of the reopened adopt/adapt sitting; map merged with the
direction session's return-arc settlement. The frame lives in
`docs/electric-native/DIRECTION.md` (road step 3) and `docs/decisions.md` —
this file carries **receipts only**. Gathered by three read-only gatherers +
primary spot-checks on the decision-changing facts; anchors are file:line at
banking time. The store-contract cut boots from here and still reads binding
law primary.

Merged frame in one breath: keyed at every durable stage; the wire is the only
identity-blind seam; adopt/adapt dissolved — Electric's machinery adopted AT
THE WIRE with our unit-ids riding on top; minting staged, not contested.
Demand design center = level-2 view-informed (viewport-residency); collaboration
is a named beneficiary of the keyed wire; presence is a later arc.

## 1 · Durable truth: five deployed modules, all keyed

Deployment receipt: `bin/land:24-29` MODULE_VARS names exactly these five. The
kernel family (text/space/compute/transcript-ingest) exists in source but never
lands durably; llm-module boots in-memory only, for ambient autotag
(`src/app/server_jetty.clj:810-814`).

- **object-container-module** (`src/app/server/rama/object_container.clj:1746`)
  — the material world. Depots: `*object-container-requests-depot` hash-by
  `:partition/key` (:1747); `*transcript-source-line-completions-depot`
  `:disallow`, internal loop-back (:1748). ~25 PStates shaped
  `{object-key → subindexed rows}`, e.g. `$$revision-history-by-container
  {String (map-schema String RevisionRow {:subindex? true})}` with
  `:key-partitioner partition-by-object-key` (:1771-1773); single-row
  `$$containers-by-id {String ObjectContainerRow}` (:1767). Row defrecords at
  :69-168. One universal depot, many producers via
  `ocr/append-object-container-request!`
  (`object_container/runtime.clj:120,141,160`; call sites in episode.clj,
  git_spine.clj, code_atoms.clj, verb_release.clj, material_circulation.clj,
  ingest_watchers.clj).
- **object-container-transcript-ops-module** (`object_container.clj:2608`) —
  ingest control state: 2 depots (:2609-2610); `$$transcript-runs`,
  `$$transcript-file-offsets`, source-lines (:2616-2620); mirrors OC transcript
  pstates (:2611-2614).
- **relation-kernel-module** (`src/app/server/rama/relation_kernel.clj:634`) —
  edges. One depot hash-by `:relation/routing-key` = relation-id, colocating
  request/journal/decision/event/edge (:635-639). Read shape
  `$$relations-by-target {String (map-schema String RelationEdgeRow
  {:subindex? true})}` (:663-664).
- **trail-view-module** (`src/app/server/rama/trail_view.clj:284`) — zero
  depots, zero own pstates; mirrors ~13 pstates (:287-300) plus
  `<<query-topology` reads (context-bundle :306-323). Pure read/assembly layer.
- **face-arsenal-module** (`src/app/server/rama/face_arsenal.clj:166`) — one
  depot hash-by `:face/name` (:167-169); `$$wear-counts-by-face
  {String WearCountRow}` (:179).

Order in Rama is data: sorted submaps + cursor ranges
(`sorted-map-range-from cursor limit`, `object_container/runtime.clj:295-299`).
No positional identity anywhere at rest.

## 2 · The one read artery (and the identity-blind wire)

- Seven identical `e/watch → fv/FacePull → reset!` loops are the whole read
  surface: `src/app/electric_flow.cljc:700-745` (read primary at banking).
  Comment law at :696-699: ONE generic pull — "The Electric surface never
  grows per face."
- Bridge `src/app/file_viewer.cljc:509-512`; server dispatch is a plain map
  lookup, one-shot, `src/app/server/rama/face_projection.clj:1888-1922`.
- Bottom of a ground pull: conversation-projection → `bd/river-page` → ONE
  `foreign-select` (`face_projection.clj:505`;
  `object_container/block_distiller.clj:1413-1421`;
  `object_container/runtime.clj:295-299`).
- No Rama→client streaming: every `Watch*` watches a JVM mirror atom
  (`file_viewer.cljc:124-152`). Verbatim why: "per-key Rama subscription
  (foreign-proxy-async on global PStates) crashes Rama 1.6.0"
  (`src/app/server/rama/util_fns.cljc:97-100`); mirrors quarantined
  `{:kernel-contract? false :durable? false}` (`util_fns.cljc:103-120`).
- Re-pull trigger: one global integer epoch, 1s client debounce
  (`face_wiring.cljs:745-757`); bumpers: `ingest_watchers.clj:199`,
  `episode.clj:961`, `facet_master.clj:220,336`, `machine_cut.clj:619,809`,
  `electric_flow.cljc:103`.
- Demand signals today: `{:face :address :params :epoch}` — never
  viewport/camera. Ground boots load-all, no `:limit`
  (`client/workspace/ground.cljs:4730-4739`). `visible-addresses` (cap 32)
  exists but feeds only the deixis bundle (`scene_store.cljc:611-642`;
  `scene_runtime.cljs:393-418`); LiveEdges addresses are boot-frozen
  (`live_edges.cljc:80-88`).
- Conflation scars of the value-wire: depth-1 edit outbox "by design"
  (`block_edit_wiring.cljs:42-61`); accepted-edit echo is a union map
  `{unit-id → nonce}` capped at 8 because "Electric conflates the request atom
  to its latest VALUE" (:190-204). Writes are ack-blocking with optimistic
  text + refusal revert (`electric_flow.cljc:95-118`;
  `block_edit_wiring.cljs:33, 99-107, 262-270`).

## 3 · Electric's collection machinery: unused (greenfield receipt)

`grep -rn "e/for\|e/diff-by" src/app --include='*.clj*'` → zero real uses;
every hit is a substring false-positive (`:source/format`,
`:space/fork-from-span`, …). The only genuine references in the repo are two
comments in `buffer_pool.cljs:219,433`. Adopting layer-3 machinery is
greenfield, not refactor.

## 4 · The GPU seam is pre-stubbed for exactly this

- `keyed-diff-update-pool!` docstring: "This is the Missionary-side equivalent
  of what e/for-by would do: new ID → allocate-slot! + update-slot! …"
  (`src/app/client/substrate/webgpu/buffer_pool.cljs:214-222`, read primary).
- Dormant section header: "MOUNT CALLBACKS (Phase 6D: bridge between Electric
  e/for-by and GPU pool)" (`buffer_pool.cljs:433`).

## 5 · Load-all scars (why level-1 demand already cliffed)

- "Raised 64 -> 512 at the first used-form break: the genesis episode outgrew
  one page and the serve window cut the newest turns off the ground (the page
  reads from the FRONT)."
  (`object_container/block_distiller.clj:1261-1267`)
- Hard cliff: `conversation-projection-scan-limit` = 100000; exceeding it
  THROWS → degrades to `:projection-read-failed`
  (`block_distiller.clj:1418-1421`).
- Cost docstring: "1 + 4*limit seeks" (:1410) → ~2049 seeks at the 512 cap.
  `serve` logs only >100ms (`face_projection.clj:1915`); real pull latency
  unmeasured.

## 6 · Truth lane is already multi-actor

RelationEdgeRow carries `asserter-actor-id asserter-type … envelope-actor-id
envelope-actor-type` (`relation_kernel.clj:267-273`). Agents already write
through the attributed depot path; collaboration inherits the lane.

## 7 · Keyed at every client stage (the other end of the wire)

- Scene store: `{:slots {vi → slot} :index {address → #{vi}}}`; index
  "maintained incrementally by upsert/remove, NEVER recomputed by scanning"
  (`client/workspace/scene_store.cljc:1-21`); frame derived per change
  (`derive-store-frame`, :321-378).
- Renderer diet: one EDN store-frame of flat per-family op vectors
  (`store-input-keys`, `webgpu/renderer.cljs:3471`); closed 10-family registry
  (:3350-3419); identity-gated prepares + keyed pool diffs
  (`runtime/render.cljs:365, 434, 642-644`). GPU position = allocation
  artifact behind id→slot.
- Faces are already data from Rama: ":assembly-source … the server serves the
  assembly FROM RAMA" (`face_wiring.cljs:121-124`), compiled client-side
  (:154-162); guard: "no list and no symbol anywhere … Arrangement only, no
  logic ever" (`face_assembly.cljc:34-36`). Interpreters are code: ~20-prim
  registry (`face_primitives.cljc:1325`), closed family registry.
- Other keyed stores: `!truth-overlay` by unit-id
  (`block_edit_wiring.cljs:207-212`); ground `!world` `:blocks {unit-id …}`
  (`ground.cljs:94-97`).

## 8 · Three rates share the pipe

| Rate | Examples | Crosses wire? | Today |
|---|---|---|---|
| Frame (60Hz) | camera, drag, caret | never — pointer → scene store → pools | keyed slots + packed arrays; works |
| Truth | edits, ingest, relations | yes | whole-page re-pull per epoch — the contested seam |
| Wear | assemblies, rosters | yes | whole-value pulls; fine |

The 60Hz stream never crosses; its settlement does. The options below apply to
the truth rate only.

## 9 · Options at the truth-rate seam · working hypothesis

1. Harden the status quo — view-informed params on FacePull, keep whole-value
   pulls; every existing scar stays structural.
2. Electric-native keyed wire — server `e/diff-by` over successive projection
   values (works today, no proxy needed), client `e/for-by` per-key branches
   into the stubbed pool bridge.
3. Owned keyed patches — mint at write sites, ship as values; requires a patch
   algebra safe under Electric conflation (a monoid we must prove).

WORKING HYPOTHESIS (merged 2026-08-13, wording frozen): "Electric-native keyed
wire — e/diff-by :unit-id server-side / e/for-by client-side — WORKING
HYPOTHESIS; comparison-minting is the named batch stage, demotion to
write-site minting pre-registered."

## 10 · Named probes (receipts owed before verdicts)

- Per-key branch overhead at real cardinalities — `e/for-by` siting cost at
  hundreds→thousands of units; kill-probe: timed spike of one projection
  through diff-by/for-by into the pool bridge.
- Mount-against-pool contract — branch lifecycle (mount/move/unmount) driving
  keyed pool ops without leak or double-free; the twice-burned family, so
  contract-first.
- Full-ground-pull cost — measure `serve` for `:outline-face`/`:episode`
  (docstring implies ~2049 seeks at cap 512; nothing measured).
- Demotion trigger line — when per-key sources exist (Rama proxy fix or
  topology-emitted deltas) or cardinality receipts cross the proportionality
  line, the comparer demotes to oracle/fence, never deleted.

## Unverified at banking

- `TrailConversation` (cursor+limit bridge, `file_viewer.cljc:251`) has no
  caller — cursor paging is server-ready, unwired (single-definition grep).
- Material/portal projections' `:params` narrowing unread
  (`face_projection.clj:995, 1445, 1772`).
- Genesis-episode row count vs the 100k cliff unknown.

## 11 · Court receipts (2026-08-13 — adversarial audit, two gatherer waves + primary spot-checks)

Banked by the final-court session over the three parallel derivations.
Receipts only; the frame lives in `PROBLEM-SPACE.md` + `docs/ARCHITECTURE.md`.

- **The revision log half-exists.** `RevisionRow [revision-id container-id
  parent-revision-id content-text content-hash order-key created-at-ms
  created-by event-id]` (`object_container.clj:109-111`).
  `$$revision-history-by-container` `{container-id {order-key RevisionRow}}`
  subindexed, order-key = `fixed-width-order-key created-at request-id` —
  TIME-PREFIXED, so a since-cursor read is mechanically supported;
  cursor-range reader exists (`runtime.clj:363`, `sorted-map-range-from`),
  4 callers, no since-watermark reader anywhere yet.
- **Who mints revisions:** 5 import adapters + facet-master bootstrap + ONE
  edit path (`edit-effects`, `object_container.clj:1506`; graduation minted
  in the same effect map :1516, persisted together :2404-2417). Geometry
  does NOT mint revisions — settles land as event-linked hint rows in
  `$$transcript-conversation-projection` (deterministic order-keys
  `geo:unit:<sha8>` / `geo:camera`, last-settle-wins; `episode.clj:274,291`)
  via the OC requests depot (append+await ack).
- **`event-id` rides EVERY row defrecord** (`object_container.clj:90-129`)
  — act linkage is the resting convention of all truth; only motion strips
  it. The two-clock law is comment-enforced at rest ("NEVER read
  :request/time-ms for this: it wall-clock-defaults", :91-95).
- **Epoch topology: TWO counters, 13 sites, identity stripped at all
  sampled.** Global `!ingest-epoch-atom` (12 sites) + separate local
  `!epoch` in `electric_flow.cljc:103`, accept-gated ("ONLY on a real truth
  change — not a replay, not a rejection", :100-104). 4/4 sampled bump
  sites have changed ids in scope, none carry them (`ingest_watchers.clj:199`
  — identity travels in the sibling `on-import` callback one line away;
  `episode.clj:961`; `server_jetty.clj:2304` — "the mint IS an ingest
  (INV-19) — the face re-pull is the committed-echo cross-check channel").
- **Geometry settles bump NOTHING** — only `:deleted?` bumps
  (`server_jetty.clj:2334-36`). Moves/camera settles are durable but
  invisible to other lenses until an unrelated bump fires.
- **Conflation scars verbatim:** cap-64 ("continuations for envelopes
  skipped by Electric conflation never fire; keep the newest 64",
  `block_edit_wiring.cljs:63-68`) · cap-8 ("Capped at 8 entries … the
  clear-all prune below reconciles at the next full pull", :187,198 — the
  oracle's second named duty) · the union map as existence proof that keyed
  payloads survive conflation ("a union map makes conflation lossless —
  the latest value contains every armed unit", `face_projection.clj:1691-95`).
- **"Depot theater" KILLED.** The text-requests round trip is depot-append
  → `$$decisions-by-id` PState query — no JVM atom
  (`text_kernel.clj:562, 579-581`; PState declared :425).
- **Mirror quarantine is test-enforced** ("adding a mirror without
  declaring it … fails loudly", `util_fns.cljc:104-112`). Roster of 9:
  cli-sessions · agent-runs · sidebar-truth · settings-truth · agent-trail
  · workspace-truth · editor-doc · flow-session · ingest-epoch ("a
  counter, not truth … resets on restart (INV-14)").
- **T2 is fixture-fed** — `fixture-specs` hard-coded literal
  (`editing_runtime.cljs:23`); wiring injects font/camera/transform
  providers only (`live_atoms.cljs:361`) — no server data path exists.
- **Registry + modules:** 12 projections in `projection-registry`
  (`face_projection.clj:1815`); 11 modules in source, 5 deployed stands
  (§1); depots — OC: requests + transcript-completions · transcript-ops: 2
  · face-arsenal: 1 · trail-view: 0 · relation-kernel: 1 · text-kernel: 1
  · dogfood transcript-ingest: 4 · transcript: 3 · llm: 4 · compute: 3 ·
  space: 1 (+3 mirrored).
- **Court kill worth remembering:** of ~12 audited citations across the
  three derivations, 11 passed and the one kill was the lone solo dramatic
  claim — variance concentrates where the shared well doesn't constrain;
  `[R]` marks are auditable claims, not facts.
