# matter-room — PLAN VALIDATION · FINAL RECORD

2026-07-27 · three rounds, per-round files preserved (`_R1.md` FAIL 18
findings · `_R2.md` FAIL 12 findings + R1 resolution table · this file =
the resolution round + closure). All validators fresh-context,
default-fail, Opus subagents orchestrated by the Fable staging session;
R2.5 continued the R2 validator's context (fresh w.r.t. the author) for
a scoped resolution check.

## Round history

- **R1 — FAIL** (18 findings, 5 HIGH): the contract/plan's first cut
  broke on real code — no machine-actor birth path; preview is
  client-only by P6's own law; `portal-briefing` drops unknown params;
  blast radius lies confidently at the anchor; the resident-id scheme
  self-contradicted. Contract amended (L5, L7, T6, G1, G2, G5, G6, G7,
  G8, G9, §11) + plan fully reworked, every fix tagged `[F<n>]`.
- **R2 — FAIL** (13/18 R1 fixes confirmed; 12 new findings, 2 HIGH —
  both in R1 fixes): the gauge repin would have made the escape detector
  permanently `:ambiguous` (dead §8 falsifier); the resident-birth
  parameterization missed `utterance-projection-hint`'s `role` slot —
  the ONE field machine classification reads. Contract + plan amended
  again (G9 repin to the single provenance master; the four-builder
  episode scope; sentinel truthiness; per-master blast landing;
  edit-lane monotone-seq law; part-type `:material`; cljc reader
  conditional; single-flight gauge; site-matched G4; pinned-surface
  disclosures; §6 sweep).
- **R2.5 — RESOLUTION CHECK: minor-fail** (verbatim below). No fix
  unsound; four one-line sweeps enumerated. Per the skill's law
  (minor-fail → the authoring phase applies the enumerated fixes;
  validation is NOT re-run), all four were applied same-session:
  1. §6 P2's "distilled-reply precedent" corrected → the §11 render
     chain (the distilled lane's `resolve-actor` does not transfer).
  2. Builder-set unified across CONTRACT §11 + PLAN P2: all FOUR
     builders named (`utterance-import-request` · `utterance-rows` ·
     `utterance-actor` GAINS a 1-arity, 0-arity callers untouched ·
     `utterance-projection-hint`).
  3. PLAN P2's leftover unqualified "every first-light verb rides
     unchanged" swept to the site-matched form (L3/G4 as amended).
  4. PLAN P2's same-content-replay sentence corrected: identical
     idempotency key short-circuits `stale-edit?` → journaled no-op,
     never `:edit/stale` (do not write a test expecting a rejection).

## VERDICT: VALIDATED — P1 may open source

Both R2.5 spot-checks held against source: the G9 repin wording matches
`analyze-activation-history`'s one-tip law (material_circulation.clj:
751-768) and the shipped single-master precedent (cluster.clj:537); the
§11 render-chain citation is correct at every hop (ground.cljs:1337 ←
face_projection.clj:127 ← block_distiller.clj:1385 ← episode.clj:207).
R2.5 also verified NEW-5's checkable claim: `$$edit-order-by-target` is
foreign-exposed (runtime.clj:77, declared cluster.clj:126) — the pinned
durable read of a unit's current edit row is real.

## R2.5 RESOLUTION TABLE (validator's text, verbatim)

| # | Status | Where |
|---|---|---|
| NEW-1 | RESOLVED | G9 repins to `(active-pointer-container-id provenance-material/spec)`, states the one-tip mechanism, routes per-master gauges to the F4-residue LATER re-cut. PLAN matches in all three places; §6 P4 + L7 carry no contradiction. |
| NEW-2 | PARTIAL → fixed | Core landed (§11 chain + G5 `:speaker` proof + PLAN P2 + test). Missing halves = R2.5 fixes 1 + 2, applied. |
| NEW-3 | RESOLVED | G2 truthiness-proof clause; PLAN cites material_portal.cljc:596 verbatim + the no-"PINNED" assertion. |
| NEW-4 | RESOLVED | G7 + PLAN P3 + cross-phase pinned-surface list carry the binding_dispatch_test pin + label re-cut. |
| NEW-5 | RESOLVED (one factual slip → fixed) | Lineage-key = unit-id, monotone seq from durable read (verified readable), never a hash; `stale-edit?` cited with its true firing site. Slip = R2.5 fix 4, applied. |
| NEW-6 | RESOLVED | Part-type `:material` pinned with the `:text` fan-out failure + the `seed-noise-kinds` elision declared INTENDED with reason. |
| NEW-7 | RESOLVED | `room-id` reader-conditional; client reads served `:portal/room`. |
| NEW-8 | RESOLVED | Override ON each per-master map + basis row on the rendered card, "asserted on the CARD". |
| NEW-9 | RESOLVED | Leak stated honestly (abandons, never interrupts), bounded by single-flight + cached last report; scoped git_spine edit named as LATER escalation. |
| NEW-10 | PARTIAL → fixed | G4/L3 re-cut ✓; the leftover PLAN sentence = R2.5 fix 3, applied. |
| NEW-11 | RESOLVED | deviate `#{:master-id :subject-uid}`; §5 amended to NO release chains with the named substitute; `:verb/bindable?` legibility stated in both docs. |
| NEW-12 | RESOLVED | §6 swept: in-JVM determinism wording, G10m on every phase, sum-check restated, ON-DEMAND qualifier. |
| F4 | RESOLVED | Subsumed by NEW-8. |
| F8 | RESOLVED | §6 swept; sum-check restated with G10m, cross-referenced to §8's carve-out. |
| F11 | RESOLVED | G7 + PLAN P3. |
| F14 (2nd half) | RESOLVED | G2: from P2 on `:material-ids` includes the room's resident unit-ids; PLAN P2 with the mark-surfaces test. |
| F16 | RESOLVED | §6 P1 swept to match G1. |
| F18 (trail) | RESOLVED | Same repin as NEW-1; repo-root half already clear. |

## What the layers caught (for the batch retro's scorecard)

R1+R2 caught, pre-code: 3 silent-wrong-behavior classes (residents born
human · dead escape detector · silently-dropped refreshes), 2 contract
self-contradictions (G8's dark row · §6-vs-§8 partition), 4
scheduled-false-stops (allowlist gaps: jetty, face_projection, episode,
the import-prefix gate), 2 dishonest-surface classes (confident-zero
blast · truthy sentinel rendering "PINNED"), 2 platform-law violations
(preview server-mint · cljc JVM-only derivation), and 2 pinned-surface
breaks (episode_test role pin · binding_dispatch exact-set pin). Zero
source files were touched at any point in the loop.
