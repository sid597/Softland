# studio — fresh contract validation, round 2

**VERDICT: MINOR-FAIL — REPAIRED IN PLACE. P0 IS OPEN FOR THE NEXT FRESH
CONTEXT.** No same-context re-run was performed. No P0 probe or product-code act
followed this verdict.

This was the one fresh default-fail round over the whole 2026-07-31 recut, not
an audit limited to round 1's repairs. The package shape, S1 ownership question,
phase count, file fence, and gate tier survived. Three local source/proof
ambiguities were repaired in `CONTRACT.md`: the stale scalar-only description
of instance material, the canonical guard's durable applicability discriminator,
and P0's status as the pre-build spike rather than a product delivery phase
under brief law 11.

## Subject, role, and custody

- Mode: GATE; altitude: executable contract against current code; authority:
  canonical candidate.
- Recut bytes validated at boot: `CONTRACT.md` SHA-256
  `5dee17bd80065c2da811a4187f511c9305fc0b1299882fde5b676cd2eb5dd7a1`.
- Repaired bytes released by this round: `CONTRACT.md` SHA-256
  `f16ed4d0345ef96b28cbedbf60a2ba66769f04a764ac375589d5da9dcb247454`.
- Docs checkout: `/mnt/data/projects/Softland`, branch
  `docs/current-mental-model-local`, opening HEAD
  `fcc6a98e6cb51f3c1ac943ce99bdea4f1580787c`.
- Authoritative code checkout: `/mnt/data/projects/Softland-smalltalk-ui-vm-p2`,
  branch `codex/smalltalk-ui-vm-p2`, clean HEAD
  `685f2fc6c937225503d48e9f9654ff5179600b68`.
- Current code outranks the P2 receipts' older custody language: P2 is now a
  clean commit at that HEAD. No source conflict was hidden by the docs.
- `BRIEF.md` was consumed once at boot. Its eleven laws were not reopened.
- Round-1 `VALIDATION.md` was consulted only after the cold whole-contract
  traces, for custody and non-duplication. It remains unchanged history.
- Pre-existing untracked `build/smalltalk-ui-vm/PODCAST_SCRIPT.md` stayed
  outside scope and untouched. `env.clj` was never read.

## Large trace 1 — first stroke → draft trail → candidate → guarded default → reverse

The recut now asks S1 to price the complete transition, not merely choose a
storage tier: birth identity, edit target, candidate container, preview scope,
promotion, durable validation report, canonical guarded activation, target
pointer, and exact reverse (`CONTRACT.md` §4 S1). It also stops if both routes
are cutover-class or if report/guard ownership needs a new depot, PState,
import-key family, or protected-kernel change.

Current disk makes those questions real:

1. The instance path synthesizes a master from a registered parent
   (`object_container/facet_master.clj:457-460`). It snapshots and validates the
   parent's complete material key set, not merely scalar values
   (`facet_material.cljc:304-347`; `facet_master.clj:517-639`). The contract's
   tentative “VALUES” description was therefore stale against code.
2. A shared activation rejects a candidate from another container
   (`object_container/facet_master.clj:252-273`), while the public master registry
   is a closed code value (`facet_masters.cljc:16-47`). An instance revision
   cannot be relabeled, and a draft-master route cannot pretend dynamic registry
   support exists.
3. Current `facet-master/activate!` validates candidate existence, container,
   and grammar, then writes the pointer without reading a validation report
   (`object_container/facet_master.clj:252-342`). The recut correctly treats the
   durable report/read/guard route as P0 proof-or-STOP, never as an existing
   implementation claim.
4. Current activation grounds are closed to experience, deviation, and conflict
   (`activation_event.cljc:66-83,127-134`). S1 correctly requires P0 to prove an
   honest encoding backed by a queryable fact; a report id string alone is not
   evidence.

The round-1 F1/F2 repairs therefore hold: P0 has an executable investigation and
an explicit stop rather than license to improvise a source family.

### Finding R2-F1 — MINOR: S1 carried a stale scalar-only code premise

Claim in the recut: the per-instance candidate was described as a new STRUCTURE
tier because the substrate today carried instance-tier “VALUES.”

Concern: current `instance-spec` widens every parent grammar over the complete
parent material key set, and `instance-form` selects/merges those keys. For
`fm:anatomy`, that includes parts and defs. The uncertain work is the actual
durable write/serve/preview/promotion chain—especially the cross-container
promotion—not schema expressibility.

Smallest correction applied: S1 now records structure as schema-legal and tells
P0 to verify the real end-to-end route rather than inherit a scalar-only
assumption. The two-route comparison and all promotion/STOP obligations remain.

Why minor: current code settled the premise; no route was selected and no new
transition or file was authorized.

### Finding R2-F2 — MINOR: canonical guard applicability was under-specified

Claim in the recut: the lowest canonical activation owner must refuse a Studio
accept with no matching PASS while the anatomy/x-ray remains behavior-identical.

Concern: the owner also serves existing bootstrap, migration, instance,
Workshop, ordinary activation, and rollback calls
(`facet_master.clj:344-395,591-620`; `server_jetty.clj:1415-1474`;
`ground.cljs:701-765`). A blanket “all activate calls require a Studio report”
breaks those paths. A UI wrapper or a check activated only by caller-supplied
actor/provenance/grounds remains bypassable when the caller omits that metadata.

Smallest correction applied: S1 now requires the durable, non-caller-asserted
discriminator by which the canonical owner recognizes a test-gated Studio
promotion while existing bootstrap/migration/rollback/x-ray paths remain
unchanged. Absence is a STOP. T3 and G6 now attack both sides: a required Studio
activation cannot bypass the report, and an existing exempt activation plus
rollback cannot be accidentally blanket-gated.

Why minor: this chooses no discriminator, changes no owner or file fence, and
adds no phase. It makes the already-required P0 proof and direct-bypass test
complete.

## Large trace 2 — ordinary canvas → drawing compiler → existing behavior → restart

This trace remains executable at P0 rather than speculative product design:

1. The primitive inventory is a plain registered value
   (`face_primitives.cljc:1252-1295`); `:text-run`, `:box`, and the block-specific
   primitives are enumerable. The interpreter compiles against that value and
   applies the same graph per instance (`face_assembly.cljc:330-347,572-601`).
   S2 can name what exists and the smallest missing rect primitive without a
   second renderer.
2. The shared anatomy editor exposes exactly seven operations and validates the
   whole candidate before append (`anatomy_material.cljc:613-708`). This supports
   §3c's “drawing hand is a compiler” rule; T5 forbids a direct scene writer.
3. The existing block root emits a real material claim at the user/machine hit
   sites (`face_primitives.cljc:644-668`). The binding grammar names a closed site
   and gesture set (`binding_material.cljc:52-106`), and its one camera
   reservation predicate protects naked ground drag/wheel
   (`binding_material.cljc:134-148`). S3 must pick an actually unreserved entry
   gesture rather than invent one.
4. The verb registry is enumerable and separates bindable verbs from
   floor-reserved camera/meta behavior (`verb_registry.cljc:317-388`). S5 must
   pick one whose current implementation can observe the draft subject; the
   contract does not authorize a new verb or a no-op registry declaration.
5. The warm echo instrument and 52ms family exist in the ordinary ground path
   (`ground.cljs:3893-3939`); S4 must re-attest the actual device before reusing
   the bar. T8 correctly makes attestation the first receipt field.

Law checks survive this trace: world-not-mode; durable material from first
stroke; visible provisionality; existing behavior in-arc; no x-ray dependency;
Sid-language with recoverable detail; agent parity through the shared controller;
and ink left open as a future part in the same vocabulary. Margin thinking is
kept inside the arc as P2, not silently erased from it.

## Large trace 3 — three real instances → PASS fact → bypass attack → lived gate

The recut's test-gated accept is now contract-grade even though P0 may prove its
route unavailable:

- The report has a closed demanded shape: exact candidate, tested instance ids
  and shapes, checks, result, actor, honest time.
- S1 must name the first physical durable request, same-id replay,
  conflicting-id refusal, and the product read that makes PASS queryable before
  pointer mutation.
- T3 attacks no report, FAIL, wrong candidate, wrong instance set, conflicting
  id reuse, and—after R2-F2—blanket-gating of existing activation/rollback.
- Preview remains non-durable and ends before activation; recovery is refreshed
  before rollback (`ground.cljs:3578-3645,701-765`). T3/T4 preserve those
  substrate laws.
- G1–G5 and G8 are implementer-owned; G6 is one fresh default-fail finder over
  the two genuinely new mechanisms; G7 belongs only to Sid, is cold and
  one-shot, and runs after the independent gate. Green receipts cannot consume
  the lived instrument or authorize a commit.
- The gate tier escalates to FULL if any durable draft/promotion/report owner or
  canonical activation owner changes. SLIM is allowed only if P0 proves the
  complete S1 chain already exists and P1 merely calls it.

No “helper returned success” is accepted as report truth: the guard must read
product-queryable durable state before the default pointer moves. That closes
the Rama retro failure mode the recut was designed to catch.

## Large trace 4 — eleven laws → phase boundary → fence → custody

The refusal/fence story is machine-checkable after round 1's recut: whole-file
hashes protect the named constitutional files, while the shared `ground.cljs`
x-ray controller is protected by P0-pinned symbol spans plus projection/golden
receipts. Ordinary-canvas Studio integration is not falsely blocked by a whole-
file hash, and an x-ray drift cannot hide inside a shared host file.

### Finding R2-F3 — MINOR: P0 contradicted the literal every-phase gate law

Claim in the recut: brief law 11 is binding; “two gates every phase.” The same
contract called P0 a phase but stated that P0 owns no gates.

Concern: a no-product-code spike has no lived surface for Sid to gate, so adding
a fake lived gate would violate the law's purpose; leaving the wording unchanged
would instead make the contract internally contradictory.

Smallest correction applied: §8 and §9 now say explicitly that P0 is the
pre-build spike, not a product delivery phase under law 11. P1 is the one
delivery phase and owns the receipt plus lived families G1–G8. The P0 deliverable
and fresh-context/no-product-code fence are unchanged.

Why minor: this records the contract's existing operational shape; it neither
removes a gate nor creates a new phase.

## Preserve and do not reopen

- All eleven brief laws and the P1 soul: the pencil is real, it does something,
  and one block completes the gated trust loop.
- P0-before-P1; no product code in P0; no separate `PLAN.md`.
- S1's complete transition and hard stop clauses; S2–S6 as machine-pinned slots.
- The seven-op shared controller, zero-new-verb/module/depot/PState/topology
  fence, camera reservation, x-ray behavior identity, and ink extension point.
- One independent post-P1 finder; Sid-owned one-shot G7 last; separate code/docs
  custody; no push.

## Disposition

This is repaired `minor-fail`, not substantive `fail`: the three findings remove
one stale source premise and add missing proof boundaries around already-settled
requirements. They do not change the transition map, select an S1 route,
introduce a durable owner, alter the allowlist, or reopen any law. Per the
contract, fixes landed in place and this validation was not re-run.

P0 is therefore **OPEN**, but §9 requires it to begin in a fresh context. Its
only authorized deliverable is `P0.md`: S1–S6 machine-verified, protected hashes,
manifest locators, x-ray span/golden pins, and a gate-tier recommendation. This
round did not start that work.
