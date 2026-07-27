# matter-room — PLAN VALIDATION · ROUND 2 — FAIL (preserved verbatim)

2026-07-27 · fresh-context default-fail validator (Opus subagent; a
DIFFERENT fresh context from R1's) · inputs: PLAN_VALIDATION_R1.md +
CONTRACT.md (amended post-R1) + PLAN.md (reworked post-R1) + source
spot-reads. Scope: (A) verify each R1 finding resolved; (B) hunt NEW
breakage introduced by the fixes. Never overwritten; the resolution
round and final verdict live in `PLAN_VALIDATION.md`.

## VERDICT: fail

Two PASS-blockers. One is in the amended CONTRACT, not just the plan:
G9's new pin makes the escape gauge permanently `:ambiguous` by
construction, which kills the detector the package's own pre-registered
falsifier depends on. The other is that P2's stated resident-birth
mechanism does not produce machine residents — it names fields that
nothing reads.

## R1 RESOLUTION TABLE (validator's text, verbatim)

| # | Status | Evidence |
|---|---|---|
| F1 | RESOLVED | §11 allowlists episode.clj (P2-scoped); G5's grep extended to new import-request builders. Achievable without touching callers (0-arity `utterance-actor`; all four callers 0-arity). *(But see NEW-2: the plan parameterizes the wrong set of builders.)* |
| F2 | RESOLVED | L5 "Preview is :pure-projection"; G7 re-cut; ground.cljs named in P3; the client lane verified (`__bindings.preview`, ground.cljs:3246-3248). |
| F3 | RESOLVED | face_projection.clj in §11 + P3; G6 positive assertion. Defect confirmed real and localized (explicit 5-key map, face_projection.clj:1689-1705). |
| F4 | PARTIAL | `:blast/counted-over` is minted per master inside `blast-radius` (material_truth.clj:218, :229) and reaches the portal via `:blast/by-master` — the plan never says where the override lands; the rendered card untouched. See NEW-8. |
| F5 | RESOLVED (shape) | Lifecycle pinned concretely; the new activate-and-reopen test added. *The chosen refresh scheme introduces NEW-5, NEW-6.* |
| F6 | RESOLVED | G7 states the v2+/v1 honest scope + the served-active-bindings assertion; residue carried. |
| F7 | RESOLVED | G1 re-cut with teeth. Tautology confirmed real (`safe-get-in` + sect-fallback totality). |
| F8 | PARTIAL | PLAN adds G10m to P1–P4 ✓ but CONTRACT §6 was not swept — per-phase gate lists + sum-check still the old partition, contradicting §8 G10's own carve-out. |
| F9 | RESOLVED (policy) | L7 ON-DEMAND + face timeout; G9 re-cut; the no-git-in-open assertion. *Implementability caveat: NEW-9.* |
| F10 | RESOLVED | G8 re-cut: injectable row source; dark row fixture-injected. Contradiction confirmed real (no seam in `declared-rows`). |
| F11 | PARTIAL | Disclosure half resolved. `subject-uid` still missing from required-args — the exact arg-starved class T10 closed. See NEW-11. |
| F12 | RESOLVED | T6 amended: in-code reverse table + served `:portal/room`, no durable row. |
| F13 | RESOLVED (data) | Sentinel mandated. *Truthy — one reader boolean-branches on it: NEW-3.* |
| F14 | PARTIAL | Address half fixed (no require cycle — material_portal.clj already requires episode). Second half dropped: room resident ids in `:material-ids` (CONTRACT L6's own clause). |
| F15 | RESOLVED | ground.cljs + face_wiring.cljs named; the drill-URL MVP stated with felt cost; residue carried. |
| F16 | PARTIAL | G1 amended ✓ but §6 P1 still says "cross-JVM byte determinism" — the exact claim G1 retired. Unswept. |
| F17 | RESOLVED | `:entity/id` = master-id pinned + asserted in G1. |
| F18 | RESOLVED (repo-root) / UNRESOLVED (trail) | repo-root derivation fine. The activation-trail pin is worse than unresolved — it pins the wrong read and breaks the gauge. See NEW-1. |

## NEW FINDINGS (validator's text, verbatim)

1. **[HIGH][G9/L7/P4] The pinned gauge read makes `terminal-escape-report` permanently `:ambiguous`, killing the package's own pre-registered falsifier.** G9 + PLAN pin "read-revision-history over active-pointer-container-id **per priced master**". `terminal-escape-report` takes ONE activation-events vector and `analyze-activation-history` sets `linear?` only when there is exactly ONE tip (material_circulation.clj:751-768). Seven masters' histories concatenated ⇒ seven tips ⇒ `:ambiguous-activation-history` with `:count`/`:escapes` nil, forever, on clean data. The shipped precedent reads exactly ONE master (cluster.clj:537, provenance). §8's falsifier says "the gauge is the detector; no one has to remember" — wired this way it never fires. **Fix:** repin to the single provenance master verbatim; per-master gauges are a re-cut of L7, not a pin.

2. **[HIGH][P2/L3] `{:actor/type :machine}` + a machine part-type do not make a resident render or behave as a machine block; the one field that does is in a builder the plan never names.** Machine classification: `machine? (not= (str (:speaker b)) "sid")` (ground.cljs:1337) ← `:speaker (:actor b)` (face_projection.clj:127) ← `:actor (:role row)` (block_distiller.clj:1385) ← the `role` slot hard-coded `utterance-actor-id` at episode.clj:207 inside `utterance-projection-hint` — a THIRD builder. Followed literally, every room resident is born as Sid's own human block (user hit-area, editable body, caret). The "distilled-reply precedent" does not transfer (that lane resolves actor via `resolve-actor` in the river, block_distiller.clj:1313). **Fix:** name `utterance-projection-hint` in P2 + §11; update the `episode_test.clj:139` role pin; add a G5 clause asserting the served turn's `:speaker` for a resident is the room actor, never "sid".

3. **[MED-HIGH][P1/G2] The `:not-applicable-at-anchor` sentinel is truthy, so every master on the anchor card floor renders "· PINNED".** `material_portal.cljc:596`: `(when (get v :master/pinned-here?) " · PINNED")` — a keyword is truthy. **Fix:** `true?`-guard the reader (file already in P1's list); assert the rendered anchor card contains no "PINNED".

4. **[MED-HIGH][P3] Adding three `:durable-via-request` verbs breaks an exact-set pin whose stated law the matter verbs violate.** `binding_dispatch_test.clj:153-158` pins the durable-via-request set exactly, labeled "arms the settle lane" — the matter verbs arm no settle lane. **Fix:** disclose the pin in P3, update the set, re-cut the label to name the two durable lanes. (`well-formed-registry?` itself is satisfied; release-ref optional.)

5. **[MED][P2] The refresh scheme's "seq/lineage derived from (unit-id, content-hash)" collides with `stale-edit?`.** `stale-edit?` (object_container.clj:1398-1405) rejects `(<= seq last-seq)` per `[lineage-key edit-client-id]`, raised in `edit-effects`, NOT in `edit-request-validation-errors` — outside the plan's stated verification duty. A hash-derived seq is not monotone ⇒ ~half of refreshes silently rejected `:edit/stale`. A content-hash lineage-key avoids rejection only by voiding ordering and growing `$$edit-order-by-target` unboundedly. No actor/capability constraint refuses an imported machine-actor unit, but the actor must carry `:object/edit` capability — never stated. **Fix:** lineage-key = unit-id (constant); seq = strictly monotone from durable truth; name the edit actor + capability; gate: two refreshes in the "wrong" hash order both land.

6. **[MED][P2] "a machine part-type" names nothing in `free-cut-part`, and the choice decides whether a resident is one block or many.** Part-types: `:thinking :tool-use :text :human-message :tool-result :image :material` (block_distiller.clj:412-434). `:text` → N markdown blocks; `:material` → exactly one. The refresh lane + the "exactly ONE head resident" test are true only under a whole-block part. **Fix:** pin `:material`; state the `seed-noise-kinds` elision (face_projection.clj:1630) and whether intended.

7. **[MED][P1/P3] `matter_room.cljc` is a shared `.cljc` the client will require, and `nameUUIDFromBytes` is JVM-only.** The narrowing law runs both sides (reply_to_block precedent, required by ground.cljs:51); a cljs compile of `java.util.UUID/nameUUIDFromBytes` fails. **Fix:** reader-conditional the derivation; the client reads the served `:portal/room`.

8. **[MED][P1/G2] The blast fix lands in the wrong place and never reaches the surface Sid reads.** Section-level nil leaves every per-master map still saying 0; `card-rows`' blast case prints per-master counts with no basis row. **Fix:** post-process each per-master map; add the basis row to the card; G2 asserts the CARD.

9. **[MED][L7/G9] "Timeout-wrapped AT THE FACE, no git_spine.clj edit" cannot actually interrupt the git call.** `git-log-bytes` blocks in `.readAllBytes`/`.waitFor` (no timeout) in a private fn; a face-level future+deref abandons — thread + subprocess leak per expiry on a re-hittable endpoint. **Fix:** scoped git_spine edit under a disclosed conditional, OR state the leak and bound it (single-flight + cached last report).

10. **[MED][G4] "Identically to ground blocks" is false by construction for machine residents.** attention v3: tap→place-caret at user-hit-area but tap→release at machine-hit-area; reply binds only at user-hit-area; only the positioned drag row is on both. **Fix:** G4 → site-matched; scope L3's clause to the verbs machine blocks actually carry.

11. **[LOW][P3] Two contract clauses quietly dropped by the plan.** (a) deviate's required-args omit `subject-uid` (deviate! cannot run without it — the registry docstring's own definition). (b) §5/§6 require release refs; the plan lists none. Also `declaration-rows` will publish `:verb/bindable? true` for verbs the docstrings call GRAMMAR-UNBINDABLE. **Fix:** declare `#{:master-id :subject-uid}`; add release refs or amend §5/§6 to drop them; state in G7 what `:verb/bindable?` means here.

12. **[LOW][§6 text] Contract §6 was not swept with the amendments and now contradicts §8 in three places.** (a) §6 P1 "cross-JVM byte determinism" vs amended G1. (b) §6's per-phase gate lists + sum-check vs §8 G10's "(P2 + every serve-widening phase)". (c) §6 P4 lacks the on-demand qualifier vs amended L7. **Fix:** sweep §6; restate the sum-check with G10m.
