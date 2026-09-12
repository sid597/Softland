# Graph economy

**Duty.** Edges have producers; the graph is an economy, not a flood. Three
strata: *import distillers* (receipt-grade — deterministic, versioned,
idempotent), *ambient basis capture* (structural via lens invocation
receipts — the deposit bridge writes the receipt's cut and basis as the
deposited material's `:based-on` edges at pinned revisions, before the
material becomes visible), *resident sweeps* (silver, gated). Review to gold
happens in the medium where the edge lands, and review mints a durable
disposition: acceptance mints the gold assertion; rejection marks the
proposal rejected and suppresses it for consumers; retraction custody stays
with the asserter (A8). Houses the check-strength / darkness-pricing service
(A4's placement): versioned and receipted; discounts anything standing on
dark ground; the hole-resolution transition is its discount-lift trigger.

**Seams.** Distillers are declared per family
([citizenship-contract](citizenship-contract.md)); the deposit bridge
consumes lens receipts
([read-honesty](../constitution/read-honesty.md)); rows land in the
[relation-kernel](relation-kernel.md). The staleness lens and
[debt-queue](debt-queue.md) consume what the producers mint; re-affirmation
strength inherits the pricing service; publication's dark pricing consumes
it ([workshops-and-publication](workshops-and-publication.md)).

**Existence:** strata embryos; the economy's organs missing.
- *Import distillers: exist.* The block distiller mints `:produced` and
  `:grounds` with the dangling-hole law (`block_distiller.clj:440-531`);
  git-spine mints `:based-on` per commit parent with stable keys, versioned
  asserter, evidence anchors (`git_spine.clj:214`, `:244-294`).
- *Ambient capture: precedent only.* Deictic receipts and gold banking run
  at every utterance; the generic deposit bridge is missing — the current
  `:based-on` writers are explicit importer/assembly paths only (Codex S3:
  `git_spine.clj:244`, `assembly_adapter.clj:436`; no generic lens-deposit
  producer in the bounded census).
- *Resident sweeps: embryo.* The autotag cascade with its validation gate.
- *Strata and review:* silver and gold exist as classifications
  (`material_circulation.clj:83`). **Build-gap:** no disposition transition
  — silver and gold coexist with no join, and a human cannot retract a
  resident's edge (`material_circulation.clj:978-990`,
  `relation_kernel.clj:459-466`; Codex F8 cross-confirmed with Claude's
  sealed F8).
- *Pricing service:* **missing entirely** — check-strength arithmetic has
  zero source presence (sealed F1, grep-confirmed): the porosity law's
  "priced" leg and re-affirmation strength stand on an organ that exists
  nowhere until built. A4 places it here.

**Open** (exchange-05 §7 + Codex rows):
- The deposit bridge: idempotent receipt→`:based-on` production before
  visibility — S3's shape.
- The pricing service: the arithmetic, its owner, minimum closure strength,
  recomputation triggers, the exact discount-lift event.
- The review/disposition service: who may review; late-review ordering;
  whether rejected silver stays historical or live (sealed F8's
  superseded-silver question folds in).

**Law.** [basis-and-staleness](../constitution/basis-and-staleness.md) ·
[checkers-and-depth](../constitution/checkers-and-depth.md) ·
[settlement-publication-custody](../constitution/settlement-publication-custody.md)
(pricing placement) · [read-honesty](../constitution/read-honesty.md) (the
birth-record composition) ·
[mixed-species-leases](../constitution/mixed-species-leases.md) (review is a
human assertion).

**Provenance.** [exchange-02](../provenance/exchange-02-adjudications.md),
one-shot item 4 (M6 — the three strata, each with a running embryo);
[exchange-04](../provenance/exchange-04-meta-syntheses.md) clause 8 and
delta 2; [exchange-05](../provenance/exchange-05-falsification.md) A4/A8,
§2 F1, §5 F4/F8/S3, §7.
