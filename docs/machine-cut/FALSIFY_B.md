# FALSIFY_B — machine-cut Lane B (serve + face) adversarial pass

Fresh-context falsifier, class = **fixture-vs-live drift + the projection's pair
serve** (the kill class that fired in two prior waves). DEFAULT-FAIL, hunt by
class. Report only — no fixes.

Working tree state: the machine-cut package is **uncommitted** (untracked
`machine_cut.clj`, tests, fixtures, face `.edn`, golden; modified
`face_projection.clj` + `file_viewer.cljc`). "Pre-package" face_projection.clj =
git HEAD (`6700bab`), diffed below for MC-T8.

## Verdict headline

The **pair-serve core is sound** — I could not break the pairing logic,
the additive law, the edge direction, or the target-id resolution (details in
"Verified sound" so the gate knows they were probed, not skipped). The named
kill class **did fire**, but only in **cosmetic turn-label VALUES** (block
`:kind` vocabulary + turn `:speaker`), which are out of the pair-structure path,
inherited from W1, and one is documented. No HIGH. Findings are the drift +
three latent/robustness items.

## Finding count by severity
- HIGH / CONFIRMED break: **0**
- MEDIUM: **1** (F1)
- LOW: **4** (F2, F3, F4, F5)

---

## F1 — Fixture block `:kind` vocabulary ≠ live `:unit-kind`; golden dot-colors + labels diverge from live · MEDIUM · CONFIRMED

- **Class:** fixture-vs-live drift (the kill class).
- **Where:** fixture `test/app/fixtures/faces/machine-cut-paired-conversation.edn`
  blocks use `:kind "human-message" | "heading" | "text" | "code"` (strings).
  Live: `face_projection.clj:85` `blocks->turns` sets block `:kind (:form b)`;
  `:form` = `(:unit-kind unit)` (`block_distiller.clj:1296`); `:unit-kind` is a
  **keyword** from a **different vocabulary** — `:prose-para :header :list-item
  :code-fence :human-sub :blockquote :table` (`block_distiller.clj:353-370`,
  `sub-form`/`map-md-form`). The colour map `sliver-palette`
  (`face_primitives.cljc:575-581`) keys on `"human-message"/"heading"/"text"/
  "code"/…`; `kind-color` (`:583-586`) coerces TYPE (`(name (keyword kind))`) but
  keys on VOCABULARY.
- **Concrete failing scenario:** wear `boxes-paired-face` over a real
  conversation. Every block card's kind → `(name (keyword :prose-para))` =
  `"prose-para"` → **not** a palette key → `kind-color` returns nil → the header
  **dot falls to the neutral/absent default** and the label reads `prose-para`
  instead of the golden's `text`. The golden
  (`boxes-paired-face.golden.edn:117-135`, dot `:bg [0.373 0.69 0.918 1.0]` for
  `"human-message"`; `:391-412` teal for `"heading"`) shows the design's
  kind-coded colours; **live shows all-neutral dots + different label text.** The
  visible kind colour-coding is lost live.
- **Why no test notices:** the golden is generated from the string-kind fixture
  and `paired-face-compiles-clean-and-golden` asserts only `golden ==
  apply(fixture)`. No JVM test compares fixture `:kind` to a live `river-page`
  block. `g4-composition-cut-then-group` in the SAME test file uses keyword
  `:form :human-message` for its synthetic blocks — the author's own two fixtures
  already disagree on kind type, and neither matches the live vocabulary.
- **Attribution / severity:** inherited from W1 (`boxes-face.edn:61` binds
  `:kind` identically; `plurality-conversation.edn` uses the same strings) and
  partially documented (`sliver-palette` docstring `:569-572` — though even that
  docstring is wrong about the live vocab: it says the worn projection emits
  `:human-message`; it actually emits `:human-sub`/`:prose-para`/…). Not a
  machine-cut regression and NOT in the pair-structure path (structure-line,
  pair/user/response headers do not bind `:kind`). MEDIUM for the visible
  golden↔live divergence; the gate should decide whether an inherited,
  documented, out-of-scope cosmetic gap blocks close.
- **Minimal falsifying test shape:** assert `(some? (fp-primitives/kind-color
  :prose-para))` — currently nil; or a golden built from a `river-page`-shaped
  block (`:kind :prose-para`) would show a neutral dot, not the committed
  coloured one.

## F2 — Fixture turn `:speaker` values ≠ live `resolve-actor` output · LOW · CONFIRMED

- **Class:** fixture-vs-live drift.
- **Where:** fixture turns use `:speaker "sid" | "claude"`
  (`machine-cut-paired-conversation.edn:16,20,28`). Live: `blocks->turns` sets
  `:speaker (:actor b)` (`face_projection.clj:97`); `:actor` = `resolve-actor`
  (`block_distiller.clj:187-211`) → user = `"human:<userType>"` (default
  `"human:external"`), assistant = the **model-id string**
  (`(get-in parsed [:message :model])`).
- **Concrete failing scenario:** the user/response/unpaired frame headers bind
  `:sub {:bind [:user-turn :speaker]}` / `[:speaker]`
  (`boxes-paired-face.edn:67,101,139`). Golden renders `"sid"` / `"claude"`
  (`golden:96-101, 327-332, 964-969`); live renders `"human:external"` and a long
  model-id string. Label-only; no pairing/geometry effect (pairing keys on edge
  target-ids, not speaker).
- **Attribution:** inherited from `plurality-conversation.edn`. No test bridges
  fixture speaker to live. LOW.
- **Minimal falsifying shape:** feed `blocks->turns` a block with `:actor
  "human:external"` and assert the served turn `:speaker` — diverges from the
  fixture's `"sid"`.

## F3 — Count-scope asymmetry in `:conversation/structure` when serve `:limit` < annotation window · LOW · PLAUSIBLE

- **Class:** projection pair serve / annotated-window vs served-page drift
  (hunt item 6).
- **Where:** `read-machine-cut-edges` (`face_projection.clj:334-351`) reads ALL
  `:pairs-with` edges under the conversation key — **unbounded by the serve
  limit**. `derive-pair-structure` (`:262-318`) resolves them against
  `tid->turn-id` built only from the **served** turns. `:pairs-count` /
  `:unpaired-count` are therefore window-scoped, but `:edges-read (count
  mc-edges)`, `:conflicts`, and `:foreign-asserter-edges` count the **whole
  conversation's** edges.
- **Concrete scenario:** annotator ran at default limit 64; a face request passes
  `:params {:limit 5}`. `river-page` serves 5 turns; the projection reads (say)
  40 pair edges. The served page has 2 pairs, but `:conversation/structure`
  reports `:edges-read 40 :pairs-count 2`. Off-page prompts → their edges drop
  (prompt resolves nil → filtered from `usable`, `:268`); off-page responses →
  empty `:responses` — pairing itself stays **prefix-consistent and honest**
  (same mechanism as until-ms). Only the count map mixes scopes.
- **Why latent:** in the default/aligned case (serve limit == annotation window
  == 64, §5.1) the counts agree; faces today pass no `:limit`. It is honest-ish
  ("edges read" ≠ "pairs formed") but the mixed scope can mislead a consumer.
- **Minimal falsifying shape:** `derive-pair-structure` over `[t1 t2]` (2 served)
  with 5 machine-cut edges whose prompts are t1,t3,t4,t5,t6 → `:edges-read 5`,
  `:pairs-count 1`.

## F4 — Chain / self-loop edges silently drop a pairing (no `:conflicts` bump) · LOW · PLAUSIBLE

- **Class:** projection pair serve / conflict-dedup determinism (hunt item 6).
- **Where:** `derive-pair-structure:275-278` — a response is claimed only if
  `(not (contains? prompt-ids resp))`, i.e. a turn that is BOTH a prompt-head and
  a response is never nested (the "defensive chain rule"). An edge whose `from ==
  to` (self-loop) makes the turn a lone empty-responses pair.
- **Concrete scenario:** rk holds edges `(from t2 → to t1)` and `(from t3 → to
  t2)` for the machine-cut actor. `prompt-ids = {t1, t2}`. The t2→t1 "responds"
  edge is dropped (t2 is a head), so **t1 becomes a pair head with empty
  `:responses`** and t3 nests under t2. The face shows "pair · turn 1" with an
  empty well even though an edge asserts t2 answered t1 — a silent loss, with NO
  `:conflicts` increment and no honest count.
- **Reachability:** the driver's closed-world validation (CONTRACT §5.3, "each
  event in at most one pair") prevents a well-formed run from emitting this. But
  the projection reads rk **directly** — a bad reconcile, a hand-seeded edge, or
  a future asserter under the same actor id could produce it, and the serve would
  not surface it. Defensive-but-silent, not defensive-and-named.
- **Minimal falsifying shape:** `derive-pair-structure [t1 t2 t3]` with edges
  `(mk-edge a "t2" "t1")` + `(mk-edge a "t3" "t2")` → expect either an honest
  drop-count or t2 under t1; got t1 with `:responses []`.

## F5 — Live re-render depends on driver and projection sharing ONE rk-rt cluster · LOW · PLAUSIBLE

- **Class:** live-serve integration the fixtures cannot catch (adjacent to the
  drift class).
- **Where:** `file_viewer.cljc` boot (diff `:279-316`) mints the projection's
  rk-rt with its **own** `rk/start-relation-runtime!` (a fresh in-process
  cluster) and replays the WAL into it; `face-ctx` (`:376-388`) hands that rk-rt
  to `serve`. The driver `annotate-conversation!` writes to whatever `:rk-rt` ITS
  ctx carries (`machine_cut.clj:515,725`) and bumps the epoch (`:740`) on THAT
  cluster.
- **Concrete scenario:** a REPL annotate that constructs its own rk-rt (not
  `(:rk-rt @face-projection-runtime)`) lands edges in a different cluster. The
  epoch bump fires, INV-19 re-pulls, but `conversation-projection` reads the
  file_viewer cluster → **no new pairs in-session**; they appear only after a
  reboot + WAL replay. G14's "re-annotate (salted), face re-renders without a
  re-wear" holds ONLY if the caller threads the projection's rk-rt.
- **Note:** this is INT/G14 responsibility, not a Lane B fixture defect, but it
  is a live-serve footgun no pure/IPC gate exercises (G10 seeds edges into the
  same rt it reads; the two-cluster split only exists in the boot path). Flagging
  so the gate confirms the wearing uses the shared handle.

---

## Verified SOUND (probed, did not break — recorded so the gate knows)

- **MC-T8 additive law (hunt item 3):** `git diff` of `face_projection.clj`
  leaves `shape-conversation`, `blocks->turns`, `apply-until-ms`
  byte-unchanged. Success path returns `(merge dc structure)`; the four additive
  keys (`:pairs :unpaired :conversation/structure :conversation/structure-line`)
  **do not collide** with any of shape-conversation's 14 keys. nil-address + error
  paths use `(merge none-structure {…explicit…})` — none-structure's four keys
  also collide with nothing, and the explicit map would win on collision anyway.
  `:turns` and every pre-existing key are byte-identical. `g12-mc-t8-*` pins it.
  **HOLDS.**
- **Edge direction (fixture↔live driver):** driver `pairs->edge-specs`
  (`machine_cut.clj:414-416`) writes `from = response`, `to = prompt`; projection
  resolves `:resp ← (:target-id (:from e))`, `:prompt ← (:target-id (:to e))`
  (`face_projection.clj:262-265`). Match — no swap. Fixture `mk-edge`
  (`machine_cut_serve_test.clj:46-60`) and `seed-edge!` use the same convention.
- **target-id resolution (hunt item 2):** the projection builds ids **forward**
  — `(tid/chat-message-id address (:id t))` per served turn as the map key
  (`:250-252`) — and matches raw edge `:target-id`s by string equality. It
  **never parses** `oc:chat-message:<ok>:<uuid>`, so object-keys (`chat:<sha>`,
  which themselves contain `:`) and any `:`-bearing uuid are safe. No split-based
  parse bug exists.
- **Live RelationEdgeRow fidelity (hunt item 1):** the value in
  `$$relations-by-target` is a `RelationEdgeRow` record (`transition-row`,
  `relation_kernel.clj:379-382`) with `:from`/`:to` as `RelationTargetRef`
  records. The projection reads only `:relation-id`, `:asserter-actor-id`,
  `:from`→`:target-id`, `:to`→`:target-id` — all present on the live row; keyword
  access is identical on a record or an IPC-round-tripped map. Fixture `mk-edge`
  is a faithful **subset** (adds `:relation-status`, omits fields the pure fn
  never reads) — no invented field. `g10-foreign-asserter-isolation-ipc`
  exercises the REAL `relations-for-targets` query and derives correctly, so the
  edge-row shape IS bridged fixture→live (only the TURN shape is not — see F1/F2).
- **Actor constant (T18 parity):** `machine-cut/machine-cut-actor-id` =
  `"llm:machine-cut/v1"` (`machine_cut.clj:53-57`); projection alias
  (`face_projection.clj:200`), fixture (`actor`), and driver all reference the one
  def. No drift.
- **MC-T14 dedup:** the live query already dedups by relation-id
  (`relations-pairs->map`, `relation_kernel.clj:607`); the projection's
  `dedup-edges-by-relation-id` is redundant-but-correct for the both-copies
  fixture. Both endpoint copies colocate under the conversation key (§4.2; g10
  asserts `(contains? result address)`).
- **Golden↔derive bridge (hunt item 5):** `golden == apply(fixture)`
  (`paired-face-compiles-clean-and-golden`) AND `fixture{:pairs :unpaired
  :structure :structure-line} == derive(fixture :turns, one edge)`
  (`g4-fixture-parity`) → the golden transitively reflects `derive-pair-structure`
  for the pair STRUCTURE. Every face bind path (`:conversation/structure-line`,
  `:pairs`, `:user-turn/:order/:speaker/:blocks`, `:responses`, `:unpaired`,
  `:conversation/paging-lack`) resolves to a served key — **no W2-F1 hollow
  bind** in the pair path. The drift (F1/F2) is only in served VALUES the bridge
  doesn't check against live.
- **until-ms interplay (hunt item 4):** pairs derived AFTER `apply-until-ms` over
  the post-cut `:turns` (`conversation-projection` diff, and
  `g4-until-ms-*`/`g4-composition-cut-then-group`). Cut prompt → responses
  stranded to `:unpaired`; cut responses → empty `:responses`; totality holds
  over served turns. reader-turn is an independent additive key with no pair
  interaction. Prefix-consistent.
