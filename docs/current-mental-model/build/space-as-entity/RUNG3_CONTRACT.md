# CONTRACT — space-as-entity, rung 3: the G10 lift

Status: BINDING (authored 2026-07-26, Fable, staging session; every trap
below verified against the on-disk code in-session). Precedence: this
contract + `decisions.md` outrank `RUNG3_NOW.md` and every phase artifact.
Rungs 1+2 are CLOSED at `43a57a0` + `f7945fd`; their `CONTRACT.md` (beside
this file) remains the binding record of the fence and traps T1–T9 — cited
here as standing law, never re-litigated.

## Purpose

Per-space instance deviations become legal: the space gains the instance
tier the other facets already have — deviate, pin, release on THE space,
served and reversible, without minting a shared `fm:space` revision. The
lift is OWNER-SCOPED, not wholesale: `:space/ground` opens for the space
only, through the same three G10 lanes that refuse it today, with the
camera fence intact at every lane. With no space instance anywhere,
behavior is identical at the cut — proven, not assumed.

## Standing law (preserved, not delivered — a diff that breaks any line
here fails review on sight)

- **The fence's three lanes**: the reservation set stays ONE var
  (`binding-material/camera-gesture-reserved?`) read by the fm:space
  grammar (`space_material.cljc:61-67`), the console seam
  (`ground.cljs:2245`), and the served-instance extraction
  (`ground.cljs:2302-2306`). Rung 3 adds NO fourth call site — the write
  lane's refusal is INHERITED (trap T-R4; gate R3-G5a proves it).
- Camera verbs stay floor-reserved forever; `verb_registry.cljc` is
  READ-ONLY; zero new verbs.
- Frozen grammar v1/v2 semantics untouched (rungs-1+2 T4): the lift
  changes NO grammar validator. Legality lives in the lanes, outside every
  versioned grammar.
- One chain-builder, `into`-append (T1/T7): the claim chain is untouched.
- T5: no new state atom, no new per-gesture read path.
- T9 `:form-validators` seam at both validation sites: untouched;
  R3-G5c proves the instance lane inherits it at write time.

## Consumers, in order

1. Sid's own drive — unchanged on day one (no space instance exists; the
   extraction contributes nothing; all 12 probes byte-identical).
2. THE space's first deviation/pin: zoom clamp or tap/marquee meaning
   deviated for this space only, or the space PINNED to a revision so
   later shared activations don't move it — the package's felt payoff.
3. Embedded canvases (the nesting package): each space its own subject;
   the owner-aware legality and the subject bridge generalize; the fence
   is their safety floor.

## Non-goals (each refusal an extension point)

- NO nested/embedded spaces, NO per-space camera, NO cross-container pick.
- NO instance-candidate preview membrane — preview is master-lane
  machinery; a space instance candidate refuses or activates. Instance
  preview is its own later cut.
- NO registry redesign: the per-conversation instance index stays;
  cross-conversation visibility of the space deviation rides the serve
  request's `:subjects` line (placement 4), whose docstring already
  blesses exactly this ("a subject passed in explicitly is honored even
  when the index has not caught up", `material_truth.clj:148-153`).
- NO reaction-declaration (next package; nothing here pre-builds it),
  NO zoom bands.

## Placement rulings

1. **Dispatch identity vs durable identity.** The claim subject stays the
   KEYWORD `:space` (`binding_material.cljc:432-437`) — a collision-proof
   sentinel; the dispatch law is untouched. The DURABLE subject is the
   string constant `space-material/space-subject` = `"space"` — durable
   lanes stringify by construction (`facet_material.cljc:319`,
   `facet_master.clj:446-450`). The two spellings meet at EXACTLY TWO
   bridge points, both client-side, both commented with T-R1:
   (a) the served-instance extraction keys rows under facet `:space` by
   the claim subject (`[:space :space/ground]`); (b) the console
   `:instance` handler coerces the subject to `:space` when the
   normalized site is `:space/ground`. Reversal cost: one constant + two
   commented sites.
2. **Owner-aware legality, one place.** `instance-site-legal?` gains a
   2-arity `[site owner]`: block sites legal for any owner;
   `:space/ground` legal iff owner = `:space`. Owner is the parent FACET
   at the write lane and the extraction, and the claim SUBJECT at the
   console — the `:space` facet-name/subject-name pun is BY DESIGN
   (`space-facet` and the space claim's subject are the same name) and the
   docstring declares it. The 1-arity keeps its old block-sites meaning so
   any un-swept caller fails CLOSED. `instance-legal-sites` (the var)
   keeps the block-sites enumeration for refusal cards.
3. **The clamp reads the space's own wear.** `:camera/zoom-at-pointer`
   moves from `(:space (current-material-wears))` (`ground.cljs:2676`) to
   the same lookup through `wears-for` keyed by `space-subject` — same
   cache, same preview membrane, literal floor fallback unchanged (T5:
   no new atom, no new read path).
4. **The serve request names the space.** `material-request!`
   (`face_wiring.cljs:444-449`) adds `:subjects [space-subject]` to the
   params — container existence is the truth, so the space deviation is
   visible from every conversation's serve regardless of which registry
   indexed it. Cost: ≤7 instance-state probes per DEBOUNCED pull (≥1s
   apart, epoch-driven — `face_wiring.cljs:524-544`).

## Traps ledger (cite trap numbers in code comments; each verified
against disk this session)

- **T-R1 — the subject schism.** The claim subject is the keyword
  `:space`; every durable lane stringifies (`(str subject-uid)` at
  `facet_material.cljc:319`; `subject-digest` at
  `facet_master.clj:446-450`). Dispatch looks rows up by
  `(get instance-rows [subject site])` (`binding_material.cljc:306`), so
  a durable space instance served under any string key would serve,
  resolve wear, and SILENTLY NEVER FIRE — no error card anywhere.
  Ruling: placement 1's two bridge points; R3-G3 proves the end-to-end.
- **T-R2 — the clamp reads the shared tier.** `ground.cljs:2676` reads
  `(:space (current-material-wears))`. Lift without moving it and a
  per-space zoom deviation is served and worn — and never FELT. Silent
  wrong, worse than an error. Ruling: placement 3; R3-G3's felt-clamp
  receipt.
- **T-R3 — a wholesale site-lift mints dead durable rows.** Adding
  `:space/ground` to the site set with no owner check lets a NON-space
  facet's instance carry rows there as valid durable material that no
  claim can ever pair with (block claims never carry the space site) —
  exactly the silently-vanish class the write-time G10 check exists to
  refuse (`facet_master.clj:545-548`, gate P6 finding F2). Ruling:
  owner-aware legality (placement 2); R3-G5b.
- **T-R4 — the write-lane fence is inherited, never added.**
  `instance-grammars` (`facet_material.cljc:284-298`) passes the parent's
  `:facet-master/bindings` validator through — for fm:space that is
  `valid-master-bindings?` WITH the fence (`space_material.cljc:61-67`),
  so a camera row in a space instance candidate refuses at
  `compile-form`, before any append. A fourth explicit fence call at the
  write lane would break the one-place census for zero coverage. Ruling:
  no new fence call site; R3-G5a PROVES the inherited refusal instead.
- **T-R5 — every deviation snapshot carries bindings.** `instance-form`
  snapshots ALL parent material keys (`facet_material.cljc:304-321`), and
  grammar v1's keys include `:facet-master/bindings` — so even a
  zoom-only space deviation serves an instance wear whose bindings are
  the inherited tap/marquee rows, and those two probes flip
  `:master → :instance` the moment ANY space deviation activates.
  EXPECTED tier transition, declared here (the rungs-1+2 G6 lesson);
  camera probes report `:floor` forever.
- **T-R6 — the pins that move (T8 class).**
  `material_truth_test.clj:493-500` pins
  `(false? (instance-site-legal? :space/ground))` ·
  `binding_dispatch_test.clj:905-910` pins the chain grep ·
  the G10 "forever, until built" prose lives in THREE files
  (`binding_material.cljc:102-115` · `ground.cljs:2249-2254` · the fence
  comment `ground.cljs:2242-2244` "before rung 3 lifts") and sweeps with
  the lift · the client table's instance label is
  `(str "instance:" subject)` (`ground.cljs:2431-2446`), which renders
  `"instance::space"` for a keyword — normalize to `"instance:space"`
  and pin the exact string (R3-G7). Grep the test tree for pins over
  every edited file before calling suite selection done; every moved pin
  is LISTED in the phase artifact.

## Deliverables (all Phase P1)

1. `binding_material.cljc` — owner-aware `instance-site-legal?`
   (placement 2) + the G10 docstring sweep. Fence var untouched.
2. `ground.cljs` — console seam: legality by claim subject + the
   `:instance` JS-handler subject coercion (placement 1b); extraction:
   legality by facet + the claim-subject key bridge (placement 1a); the
   clamp read (placement 3); table label normalization; docstring sweep
   (including the 2242-2244 fence comment, which anticipated this lift).
3. `facet_master.clj` — the write-lane `illegal-sites` check becomes
   owner-aware (parent facet as owner).
4. `space_material.cljc` — the `space-subject` constant.
5. `face_wiring.cljs` — the `:subjects` line (placement 4).
6. Suite coverage for the instance lane at the space: `drill-report` with
   injected `[:space :space/ground]` rows (tap reports `:instance`;
   camera probes report `:floor` with the same rows present). The LIVE
   probe list stays at 12 — probes are stateless by design; the live
   instance-lane drill is R3-G2's install→drill→clear cycle.
7. The T-R6 sweep, listed in the phase artifact.

## Acceptance gates — R3-G1…R3-G8 (numbered; each executable; live
receipts are server-read, never a client cache)

- **R3-G1 — behavior-identical at the cut.** With NO space instance
  anywhere (served or console): all 12 drill probes byte-identical to the
  rungs-1+2 baseline, suite AND live console agreeing; the interaction
  table carries zero instance rows; wheel saturates at exactly 8.0/0.1
  (the served base).
- **R3-G2 — the console lane, suite + live.** (a) install a LEGAL row at
  `[:space :space/ground]` (tap → `:selection/marquee-begin`) →
  `:installed`; drill: tap-empty-space reports tier `:instance`; clear →
  restored. (b) a camera row there → refused
  `:binding/camera-gesture-reserved` (the fence check stays ORDERED
  BEFORE site legality, as today, so the refusal names the deeper law).
  (c) a non-space subject at `:space/ground` → refused site-illegal.
  (d) block-site installs behave exactly as before the lift.
- **R3-G3 — durable end-to-end, live cluster, fresh request ids.**
  `material-truth/deviate!` on the space (`zoom-max 3.0`) → the serve
  carries facet `:space` / subject `"space"` → client extraction keys
  `[:space :space/ground]` → trusted wheel clamps at exactly 3.0 WHILE
  the shared fm:space master still serves 8.0 (both receipts, server-
  read) → declared T-R5 transition: tap/marquee probes report
  `:instance` during the deviation → `release-deviation!` → probes and
  clamp restore (`:master` tier, 8.0). Re-import mints byte-identical
  revision ids (content-addressed law).
- **R3-G4 — pin the space.** Pin the space to the base revision →
  activate a shared fm:space revision with `zoom-max 2.0` → the space
  still wears 8.0 (pin holds; felt clamp receipt) → unpin → wears 2.0 →
  roll the shared pointer back to base. Server-read receipts throughout.
- **R3-G5 — write-lane refusals, server-side, nothing appended.**
  (a) a space instance candidate carrying a camera row at
  `:space/ground` → `:accepted? false` via the INHERITED grammar fence
  (T-R4), error names `:facet-master/bindings-invalid`; (b) a NON-space
  facet's instance candidate carrying `:space/ground` rows →
  `:accepted? false`, site-refused (T-R3 — the no-silent-vanish law
  preserved); (c) a space deviation with `zoom-min ≥ zoom-max` →
  `:accepted? false` via the inherited T9 `:form-validators` seam
  (`:space/zoom-clamp-invalid`).
- **R3-G6 — one-place censuses (style gate; stops at the named sets).**
  The legality predicate is defined in exactly ONE place in
  `binding_material.cljc`; all three G10 lanes read it with their owner
  (three call sites cited in the phase artifact). The FENCE census is
  re-derived and expected UNCHANGED at three product call sites — the
  write lane proven inherited (R3-G5a), not added. `verb_registry.cljc`
  zero diff; frozen v1/v2 validators byte-unchanged; no new state atom;
  `git diff --check` clean.
- **R3-G7 — label/table coherence.** The client table's space-instance
  rows carry exactly `"instance:space"`; the server interaction table
  carries no instance tier — an EXISTING P6 asymmetry
  (`face_projection.clj:1047` builds tiers from shared masters only),
  asserted here so it is a declared fact, not an oversight; floor-label
  client/server agreement (`code-floor:fm:space:v0`) unchanged.
- **R3-G8 — the sweep receipt.** Every pinned enumeration this package
  moves (T-R6's list plus anything the pre-code grep finds) is updated in
  the same change and LISTED in the phase artifact; the affected suites
  and the fail-closed fast lane run green.

## Gate partition + owners (sum-check rule, work-package skill 2026-07-26)

- Partition: **Phase P1 runs ALL of R3-G1…R3-G8.** One phase, eight
  gates; the partition sums to the contract's full gate list with zero
  remainder. The Fable gate review re-runs its selection independently
  (it owns no gate exclusively).
- Owners: every gate is machine-runnable on this box by the implementing
  session (live cluster + dev app + headless browser). NO gate is bound
  to Sid or a special environment. Grounds for staging no echo-bar/felt
  gate: rung 3 adds no new per-event work class (read plan below). If
  the falsification finder or the gate review falsifies that claim, a
  G7-class echo gate is staged in the same breath with Sid named as its
  owner.

## Read plan (the one performance promise)

The wheel path gains ONE cached map probe over today (`wears-for`
by-subject hit) — no new pick, no new atom, no per-event resolution. The
first wheel event after a served-identity change pays one instance-wear
resolution; that is the class every deviant block already pays, and P6's
echo receipt held with a deviant + pin in the serve (0/120 over the 52
bar) — precedent cited as INFERENCE; the structural claim (no new
per-event work class) is what R3-G6 asserts against the diff. The serve
gains ≤7 instance-state probes per debounced material pull (≥1s apart).

## Stop clauses (beyond the standard: contract unbuildable /
binding-doc conflict)

- Making the durable lane fire would require changing the claim chain's
  shape or its subject identity — that is an identity fork; escalate.
- The subject bridge cannot stay at exactly two commented client sites —
  the ruling is wrong; escalate, never scatter a third.
- The fence one-place law or frozen grammar versions (T4) cannot be
  preserved.
- Any need to touch `verb_registry.cljc` or mint a verb.

Escalation per the work-package skill: verbatim citations + options +
recommendation into `decisions.md` Open Questions; no improvisation.

## Input manifest (re-verify every line against disk before code —
lines drift)

- `src/app/shared/binding_material.cljc` — sites 77-83 ·
  instance-legal-sites + docstring 102-115 · instance-site-legal? 117-119
  · camera-gesture-reserved? 121-135 · candidates-at 298-315 ·
  space floor + space-claim 400-437 · probes 449-489.
- `src/app/client/workspace/ground.cljs` — current-material-wears 262-273
  · wears-for 275-322 · floor-binding-rows 2205-2218 ·
  set-instance-bindings! 2224-2267 · served-instance-binding-rows
  2275-2316 · instance-binding-rows 2318-2324 · claim-chain ~2360-2371 ·
  binding-tables 2431-2469 · zoom clamp 2668-2685 · console `:instance`
  handler 3196-3207 · drill worlds ~3140-3183.
- `src/app/shared/facet_material.cljc` — instance-validators 273-282 ·
  instance-grammars 284-298 · instance-form 304-321 (the `(str
  subject-uid)` at 319) · wear-for-subject 370-459. READ-ONLY.
- `src/app/server/rama/object_container/facet_master.clj` —
  subject-digest 446-450 · write-instance-revision! 517-609
  (illegal-sites 549-550; "Refuses BEFORE any append").
- `src/app/server/rama/material_truth.clj` — register!/deviate!/
  release-deviation!/pin!/unpin! 32-75 · served-instance 118-146 ·
  served-instances 148-172. READ-ONLY.
- `src/app/server/rama/face_projection.clj` — served-instance-tier
  911-957 · interaction-table 1034-1064 (tiers from shared masters
  only). READ-ONLY.
- `src/app/client/workspace/face_wiring.cljs` — material-request!
  444-449 · the epoch debounce 524-544.
- `src/app/shared/space_material.cljc` — the whole file (118 lines).
- Tests: `material_truth_test.clj:485-505` · `binding_dispatch_test.clj`
  (chain grep-gates ~900-911) · grep the tree per T-R6 before suite
  selection is done.

## Process

- Implementer: fresh build session (cheaper-model lane per the role
  split), booted from THIS file + `RUNG3_NOW.md` STANDING. ONE phase
  (P1) running all eight gates green in-context; every manifest claim
  re-checked against the on-disk file before code.
- Falsification batch: ONE finder, aimed at the genuinely-new machinery —
  the subject bridge (both spellings, both bridge points), owner-aware
  legality (all three lanes), and the clamp's tier move.
- Gate review: Fable, sized slim per the settled sizing ruling: drive
  R3-G2/G3/G4 live under fresh request ids, re-run the censuses and the
  package's suites + fast lane; full re-derivation not required.
- After green: Sid's commit word → HEAD-dynamic suites at committed HEAD
  → close + retro per the skill. Then reaction-declaration stages per the
  board order — NOT in this package.
