# DIFF_FALSIFICATION_R1 — batched build 2026-07-05 (spine · route · names · render)

Fresh-context adversarial pass over the entire uncommitted working-tree code
diff, per CONTRACT v1.2 §6 (Final) + trail-room CONTRACT §3 and CLAUDE.md's
falsification protocol. Job: BREAK the changes. Report only — nothing changed,
nothing committed. Authorities read first: git-spine CONTRACT v1.2, PHASE_P1P2,
PHASE_P3, trail-room CONTRACT v1 + PHASE_R1.

**Verdicts (detail below):**
- **spine** — SOUND on the happy path; SHOULD-FIX replay/extract per-item fragility; DOUBTs (reader field-pinning, charset, doc-edge path match).
- **route** — SOUND on the happy path; SHOULD-FIX (asserter-id unvalidated → lying 200; retry idempotency; `:git-commit` allowlist dangles); DOUBTs (optimistic 200, unbounded log).
- **names** — SOUND.
- **render** — SOUND for kraft/rim/address; **SHOULD-FIX**: removing the outer expansion `clip?` stripped the card-width text clip from info/relations/holes/omissions (only material got a replacement), uncaught by tests.

No BLOCKING findings — every gated happy path is real and tested. `file(1)`
reports text for all 15 touched files (matrix at end); zero NUL bytes.

---

## Architecture

The spine is a pure adapter/driver/extractor + a replay fn over the existing OC
and relation-kernel public APIs, hooked into the one boot future in
`file_viewer.cljc` (`run-git-spine-boot!`: replay → spine-sync → extract, each
try/catch-isolated). The write shim is an HTTP route in `server_jetty.clj` that
validates route-side, write-aheads one plain-map EDN line to a shared log, then
appends the envelope to the same trail runtime the spine drives. The
serialization boundary (SF-R2.1) is deliberately two independent
implementations with zero shared code: `server_jetty/envelope->plain-map` +
`append-assert-log-line!` write; `git_spine/stored-map->request` +
`replay-assert-log!` read. I verified the trail runtime bundles
`:relation-request-depot`, `:relation-detail-query`, and
`:object-container-requests-depot` (`trail_view.clj:753`+), and that
`ocr/append-object-container-request!` reads exactly `:object-container-requests-depot`
(`object_container/runtime.clj:94`) — so the production runtime shape matches the
one the spine gate tests drive (`git_spine_test.clj:182,245` use the same
`start-trail-view-runtime!`).

The render change (trail-room R-1) is paint-only: `:relation-transition` feed
entries become kraft marks/lines (never box-cards), the face address moves off
the card into a four-slot rim stored in scene `:data` and rendered by
`combined_text.cljs` inside the 24px strip, and the closed card keeps its address
as node DATA. The load-bearing subtlety is the clip model: **production walks the
trail scene with `(tree->rects scene)` and `(tree->text-ops trail-face-scene)` at
arity-1 — clip-bounds `nil`, no scroll offset — and applies scroll as a GPU
camera pan** (`editor_compute.cljs:386,346`; `combined_text.cljs:284`). The R-1
fix's rationale (negative-y bg rects when an expansion is "scrolled above the
viewport") describes the gate-6 TEST model (`tree->rects s 0 (- offset) clip`,
`trail_face_test.clj:238`), not this production path — which reframes both the
fix and its one real regression.

---

## Failure modes attempted (scenario → held / BROKE)

### Seam 1 — Serialization drift (SF-R2.1, the analytical G8 pair)

The route (production writer) and `replay-assert-log!` (production reader) are
never exercised together by any test: the route test round-trips its line only
through `edn/read-string` (`relation_assert_route_test.clj:67`), and the spine
replay test writes with git_spine's OWN `envelope->log-line` and reads with its
own reader (`git_spine_test.clj:356,364`). So I executed the pair analytically.

- **Envelope shape** = `envelope` (`relation_kernel.clj:853-870`): 7 top-level
  keys (`:relation/routing-key :request/id :request/type :request/sent-at-ms
  :idempotency/key :actor :payload`); payload = `RelationMutationPayload` 10
  fields (`:215-222`); from/to = `RelationTargetRef` 3 fields.
- **Writer** `envelope->plain-map` (`server_jetty.clj`): `(into {} payload)` +
  `(into {} from)` / `(into {} to)` — preserves ALL fields; `pr-str` under
  `*print-namespace-maps* false`.
- **Reader** `plain-map->payload` (`git_spine.clj:512`): `select-keys` of the 8
  scalar payload fields + reconstructed from/to (3 fields each) via
  `map->RelationMutationPayload` / `map->RelationTargetRef`; envelope keys kept
  verbatim.
- **Round-trip → HELD** for the *current* record shapes: reader's select-keys
  lists total to exactly the 10 payload + 3 target fields; ids travel verbatim;
  `:request/type :relation/assert` survives (replays as an assert, not a mint);
  nil `:evidence-*`/`:note` survive (records carry them as map entries); `:actor`
  submap survives (`edn/read-string` reads both `{:actor/id …}` and `#:actor{…}`).
  `append-relation-request!` accepts it (routing-key present, `:917-926`).
- **Namespaced-map / `*print-namespace-maps*`** → HELD. Writer forces `false`;
  even without it, `clojure.edn` reads `#:actor{…}`. The only uniform-namespace
  submap is `:actor`; the top-level and payload maps are mixed/plain and never
  print as namespace maps.
- **Non-ASCII `:note`** → **DOUBT (see Open doubts).** Both sides use
  `io/writer`/`io/reader` with the JVM default charset (no `:encoding`), unlike
  the git path which pins `StandardCharsets/UTF_8`. Same-host round-trips; a
  fresh-cluster replay on a different `file.encoding` (or across the Java-17→18
  JEP-400 default flip) can mis-decode non-ASCII notes/ids.
- **Future field added to either record** → **BROKE (latent).** Writer's
  `into {}` auto-preserves a new field; reader's hard-coded `select-keys` silently
  drops it. No test pins the two select-keys lists to the record fields, and G8
  (the cross-writer pair) does not exist.

### Seam 2 — Boot ordering

- **Sequential replay→sync→extract, per-stage isolation** → HELD.
  `run-git-spine-boot!` wraps each stage in try/catch (`ingest_watchers.clj`), so
  one stage's throw is logged and the next runs. Runs AFTER
  `start-ingest-watchers!` inside the existing future; concurrent watcher writes
  and spine appends both land on the serializing depot.
- **Commit-target edges before their objects exist** → HELD. `spine-sync!`
  awaits ALL OC decisions (`git_spine.clj:245`) before returning; `extract`
  (next stage) asserts commit-target `:produced` edges only after. Edges
  themselves are fire-and-forget (async materialization) — acceptable, journal
  converges.
- **Double-boot on hot reload** → HELD. `trail-view-runtime` is a
  `defonce`-wrapped `delay` (`file_viewer.cljc:146,156`); re-eval doesn't
  redefine; the future runs once when first forced. Even a forced re-run
  converges (stable keys + journal).
- **Replay hitting an unbooted relation module** → HELD. `rt` is created
  synchronously in the delay body BEFORE the future is spawned; the future's
  `cfg` closes over that live `rt`.

### Seam 3 — Idempotency split

- **curl RETRY after a timeout** → **BROKE (SHOULD-FIX).** The route mints a
  per-POST `request-id (UUID)` and `idempotency-key "assert:<uuid>"`
  (`server_jetty.clj` `assert-relation-handler`). The relation-id is
  deterministic so the EDGE converges, but the journal keys on the idempotency
  key — a retry with a fresh UUID is NOT dropped → a second accepted decision +
  event + activity row for the same edge. This is exactly trap 4 ("activity
  buckets fill with phantom writes"), which the IMPORT path was hardened against
  (stable `"spine:"+relation-id+":"+basis`, `git_spine.clj:58`) and the route
  was not.
- **spine-sync after a route-assert of the same semantic relation** →
  HELD / intended. `relation-id-for` includes the asserter
  (`relation_kernel.clj:104-109`); route asserter (curl param) ≠ import
  `import:git-spine` → two relation-ids → two edges, by design (§2.4, trap 5).

### Seam 4 — Cursor

- **Corrupt / delete mid-run** → HELD. `read-cursor` catches → `{}` → full
  reprocess (cost only, journal is truth, G7). Written once at end
  (`git_spine.clj:470`); mid-run deletion is a no-op.
- **jsonl SHRINKS (truncation)** → HELD. `file-sig {:mtime :size}` changes on any
  size delta → reprocess. (Edges are never retracted on source shrink — inherent
  to the append-only edge model, not a cursor bug.)
- **mtime-equal-but-content-changed** → HELD in practice. Transcripts are
  append-only, so size always grows on new content → sig differs. A same-size,
  same-mtime in-place rewrite would be missed, but jsonl never does that.

### Seam 5 — R-1 `:material-clip` / negative scroll / zero-height / editor byte-identical

- **Editor strip byte-identical** → HELD. The else-branch wraps the two ORIGINAL
  op maps in `(concat […] […])` (`combined_text.cljs`); by concat associativity
  the outer op sequence is identical to the pre-change
  `cmd-lines ++ agent-lines ++ [left] ++ [right]`. The two maps are unchanged
  byte-for-byte.
- **`m/latest` 11 watches, no `m/ap`** → HELD (CLAUDE.md pattern respected).
- **Zero-height card** → HELD. `preview-h 0` → material-clip `:h 0` → material
  `visible?` false (`rect_tree.cljc:224` `(< abs-y (+ cy 0))` false) → nothing
  emitted, no divide-by-zero (`truncate-op` guards `(pos? cw)`).
- **bg rects at negative scroll (the reported catch)** → HELD **in the test
  model, moot in production.** In the gate-6 test (`tree->rects s 0 (- offset)
  clip`, `trail_face_test.clj:235-241`) the fix is correct: non-clip expansion →
  holes/omissions clamp to the viewport clip, not the expansion's negative
  bounds. But production walks `(tree->rects scene)` clip-bounds `nil` +
  camera-pan scroll (`editor_compute.cljs:386,346`), so the scene never carries
  negative-y at the data level and the described bug cannot arise there. The fix
  is test-correct and production-neutral for bg.
- **info / relations / holes / omissions text horizontal clip** → **BROKE
  (SHOULD-FIX).** The OLD expansion `:clip? true` clipped ALL its text children to
  `card-w` (in `tree->text-ops`, a clip? node sets child-clip = its own bounds →
  `truncate-op` at card-right). R-1 removed the outer clip? and gave ONLY the
  material preview a replacement (`:material-clip`, `scene.cljc:359`). With
  production's nil clip-bounds, info/relation/omission/hole text now has no
  clip-right → `truncate-op` is a no-op. Relation lines are
  `"-> based-on oc:doc:<40-hex> by import:git-spine"` (`scene.cljc:294-296`) — far
  wider than the 320px card — so they now overflow horizontally (bounded only by
  the GPU viewport). No test asserts non-material expansion text is card-clipped
  (gate-6 checks bg RECTS; the material test checks the material node only).

### Seam 6 — P3's strengthened fixture gate

- **Vacuous-pass hole?** → mostly HELD; one DOUBT. The new
  `fixture-fidelity-test` adds a non-vacuousness guard forcing BOTH
  `:relation-transition` and `:source-ingested` live entries to exist and be
  compared (two kinds × three exact `shape-keys=` checks), which is strictly
  stronger than the old single arbitrary first-vs-first pair. The
  `:transcript-file-updated` clause is guarded `:when live-entry` and
  `$$transcript-file-offsets` is never driven by `build-fixture!`, so that kind
  is NEVER compared → its hand-fixture `:entry/target` shape
  (`{:id :kind :display-name}`) is never validated against live truth. Latent
  hole, but shape-keys= compares KEY SETS and the live `file-activity-entry`
  emits the same key set, so no current divergence hides. DOUBT (minor).

### Seam 7 — Ownership / lifecycle / paths

- **assert-log path base (route literal vs replay cfg)** → HELD. Route:
  `(str (System/getProperty "user.dir") "/" "data/relation-assert-log.ednl")`.
  Replay cfg `:assert-log-path (str dir "/data/relation-assert-log.ednl")` where
  `dir (System/getProperty "user.dir")` (`file_viewer.cljc:158,173`). Both resolve
  to the identical absolute string; `user.dir` is immutable post-launch.
- **assert-log lock** → HELD. `(Object.)` monitor; `locking` releases on any exit;
  the line is built OFF the lock, only the append is inside.
- **assert-log growth** → **DOUBT.** No rotation/compaction; every valid POST (and
  every curl retry, and every depot-rejected poison line — see route) appends
  forever, and each boot re-replays the WHOLE file (O(all-asserts-ever)).
- **asserter-id NOT validated** → **BROKE (SHOULD-FIX).** `validate-assert-params`
  guards kind/from-kind/to-kind/from-id/to-id but NOT `asserter-id`. A POST
  without it builds `:actor {:actor/id nil}`, yields a non-blank routing key, so
  `append-relation-request!` does NOT throw → the route write-aheads a log line
  and returns 200 `{:ok true :relation-id …}`, but the depot REJECTS it
  (`:actor/id-missing`, `relation_kernel.clj:323`). The edge never materializes
  and the poison line re-fails on every replay.

---

## Writers / readers / clearers (changed state)

| State | Writer(s) | Reader(s) | Clearer | Risk |
|---|---|---|---|---|
| `data/relation-assert-log.ednl` | route `append-assert-log-line!` (per valid POST) | `replay-assert-log!` (boot) | **none** — no rotation/compaction | Unbounded; poison + retry-dup lines accumulate; O(n) boot replay |
| `assert-log-lock` (Object) | `append-assert-log-line!` via `locking` | — | `locking` auto-release (exception-safe) | HELD |
| `data/git-spine-cursor.edn` | `write-cursor!` (end of extract) | `read-cursor` (start) | delete → full reprocess (G7) | Cost-only; corruption → `{}` |
| relation depot | route(assert), spine-sync(based-on), extract(produced), replay | topology / `read-relation-detail` | idempotency journal on `:idempotency/key` | Route UUID key ≠ import stable key → retry dups |
| `!trail-face-scene` atom | `editor_compute` `<trail-face` `reset!` | `combined_text` (rim + text ops), mouse hit-test | `reset! nil` when `(:face trail-state)` nil (`editor_compute.cljs:356`) | HELD; value-compared not hash (`:348`) |
| `!last-trail-struct` atom | `editor_compute` (memo guard) | same | reset on every rebuild | HELD — compares VALUE, avoids stale-scene freeze |
| `:sidebar-visible` (derived) | `workspace_actions` `(and sidebar-visible (not trail-face?))` | layout readers | recomputed each derive | Guard is report-only; `<layout>` reads the ATOM directly (recorded debt R-1 #1) |

---

## Async ordering risks

1. **Route 200 is optimistic.** `append-relation-request!` uses `:append-ack`
   (durably queued, NOT decided). The route returns 200 before the depot
   decision, so a depot-REJECTED request (missing actor now; any future
   validation) still gets a 200 + a durable log line. No write→decision
   reconciliation exists.
2. **Concurrent POSTs**: the lock serializes only the FILE append, not the depot
   append, so log order and depot order can diverge across two POSTs. Benign —
   edges are independent and replay is order-insensitive.
3. **Boot vs route**: a /trail pull forces the delay (spawns the boot future) and
   makes the route see a booted runtime; the route appends independently of the
   boot future's still-running edge-building. HELD.
4. **A-starts / B-starts / A-finishes-after-B** on the depot: resolved by
   deterministic relation-id + journal for identical idempotency keys; the ONE
   gap is the route's per-POST key (retry ordering yields duplicate rows).

---

## Error-path cleanup

- `run-git-spine-boot!` — per-stage try/catch; one stage's throw logged, others
  proceed. HELD.
- **`replay-assert-log!` — SHOULD-FIX.** The `reduce` over lines skips only BLANK
  lines; a malformed/torn non-blank line throws `edn/read-string`, aborting the
  reduce. Lines AFTER it never replay this boot, and — because the bad line
  persists — never on any future boot either (durability silently degraded,
  logged only as "threw; boot survives"). A crash between `.write line` and
  `.write "\n"` leaves exactly such a torn last line.
- **`extract-session-joins!` — SHOULD-FIX.** `process-jsonl-file!` is not wrapped;
  one unreadable transcript (permissions, mid-read delete) throws the whole pass
  → cursor unwritten → full reprocess next boot, but no edges from later files
  this run.
- `resolve-trail-runtime-or-503` — `future-cancel` on timeout AND on exception;
  never forces an unrealized delay. HELD (the 503 test proves no-boot,
  `relation_assert_route_test.clj:125`).
- Route outer try/catch → 500 on any exception; invalid → 400 with NO file/depot
  write (short-circuits before the runtime is even used, so a nil runtime is
  safe). HELD.

---

## Open doubts (with named falsifiers)

1. **Reader field-pinning (SF-R2.1 residual).** Add a field to
   `RelationMutationPayload`/`RelationTargetRef`; the route persists it, replay
   drops it. *Falsifier:* a unit test asserting
   `(set (keys (into {} payload))) == ` the reader's select-keys ∪ `#{:from :to}`;
   it does not exist. Fix cheaply by reconstructing from ALL stored keys, or add
   the missing G8 cross-writer pair test.
2. **assert-log charset.** *Falsifier:* POST `:note "café ☕"`, then replay under
   a JVM with `-Dfile.encoding=ISO-8859-1` (or a Java-17→21 host change) → the
   note mis-decodes. Pin `StandardCharsets/UTF_8` on both `io/writer` and
   `io/reader`, matching the git path.
3. **Doc `:produced` edge path-string match.** `doc-document-id` hashes and keys
   on the transcript's RAW `:file_path` string (`git_spine.clj:344`, returns
   `file-path` not the canonical path), which must EXACTLY equal the watcher's
   `.getPath` source-ref to join. *Falsifier:* a transcript that Edited a doc via
   `/home/sid/projects/Softland/…` while the server runs under `user.dir =
   /mnt/data/projects/Softland` (both are live working dirs) → the edge targets a
   non-ingested object-key and dangles. Duty §8.6 flagged this; the dual-path env
   makes it concrete.
4. **`:git-commit` in the route allowlist re-opens B1.** `relation-target-kind-allowlist`
   includes `:git-commit`, whose `->target-ref` target-key is the BARE sha
   (`relation_kernel.clj:844`), while the rendered commit object's key is
   `extract-object-key "oc:doc:<ok>" = <ok>`. *Falsifier:* POST a `:based-on` with
   `:to-kind :git-commit :to-id <sha>` (exactly the route test's own shape,
   `relation_assert_route_test.clj:40`) → the edge never surfaces on the rendered
   commit container — the identical non-join v1.1 removed from the import path,
   re-exposed on the manual path.
5. **Info/relation horizontal overflow (render).** *Falsifier:* expand a card
   whose `:relations :this` carries a full `oc:doc:<hash>` target — the
   `-> based-on oc:doc:… by …` line exceeds card-w and, with the outer expansion
   clip? gone, bleeds past the card. Add a text-op test asserting expansion
   non-material ops satisfy `(<= (:x op) (+ card-left card-w))`.
6. **Optimistic 200 (route).** *Falsifier:* POST a request the depot will reject
   (missing asserter-id) → 200 `{:ok true :relation-id …}` while
   `read-relation-detail` never returns a row. The response cannot distinguish
   "queued" from "asserted."

---

## Verdicts per package

- **spine (git_spine.clj + git_spine_test + ingest_watchers + file_viewer):**
  **SOUND** on the tested happy path (serialization pair round-trips analytically
  for current shapes; boot ordering, path base, runtime bundling all HELD).
  **SHOULD-FIX:** replay per-line and extract per-file lack try/catch — one bad
  line/file silently degrades durability/coverage. **DOUBT:** reader field-pinning
  (1), charset (2), doc-edge path match (3).
- **route (server_jetty + relation_assert_route_test):** **SOUND** happy path.
  **SHOULD-FIX:** asserter-id unvalidated → lying 200 + poison log line;
  per-POST-UUID idempotency → curl-retry duplicate decision/event/activity rows
  (reintroduces trap 4); `:git-commit` allowlist → dangling edges (4).
  **DOUBT:** optimistic 200 (6); unbounded log + O(n) boot replay.
- **names (trail_view.clj + trail_view_test):** **SOUND.** `basename` is nil-safe
  (blank-output doubt falsified: `str/split "/" #"/"` → `[]` → nil), `subject-line`
  first-match is the header by §3.A order, `bundle-display-name` falls back
  honestly, the `== <tid>` anchor is untouched. **DOUBT (minor):**
  `:transcript-file-updated` parity never actually exercised (6/Seam-6).
- **render (cards + scene + workspace_actions + combined_text + trail_face_test):**
  **SOUND** for kraft marks (never vanish), rim (four slots), address-off-face +
  address-as-data, editor byte-identity, sidebar guard. **SHOULD-FIX:** the outer
  expansion `clip?` removal stripped card-width text clipping from
  info/relations/holes/omissions (only material replaced), a real production
  overflow uncaught by tests (Seam 5 / doubt 5); the fix's negative-y-bg rationale
  targets the gate-6 TEST scroll model, not the production camera-pan path.

## `file(1)` matrix (G12 / G6)

All 15 touched source/test/fixture files:
`git_spine.clj, server_jetty.clj, ingest_watchers.clj, file_viewer.cljc,
trail_view.clj, cards.cljc, scene.cljc, workspace_actions.cljs, combined_text.cljs,
git_spine_test.clj, relation_assert_route_test.clj, missionary_claims_test.clj,
trail_view_test.clj, trail_face_test.clj` → "Clojure module source, Unicode text,
UTF-8 text"; `feed.edn` → "ASCII text". Zero NUL bytes (grep `\x00` empty). PASS.
