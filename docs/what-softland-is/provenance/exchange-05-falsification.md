# Product-Architecture Exchange — 05 · Falsification Round (non-kin)

Part of the 2026-08-15 exchange. After the three kin sessions converged
(exchange-04, synthesis 2), the span went to a non-kin checker — Codex
(OpenAI) — for a hostile pass BEFORE breakup or signature, per the exchange's
own correlation clause ("three kin checks ≈ one check") and Sid's standing
falsification-review law. Nothing was signed in this round.

Contents: §1 the packet sent to Codex · §2 Claude's own checklist run (sealed
from Codex, delivered in-chat before Codex ran — banked for cross-confirmation)
· §3 Codex's review verbatim · §4 Sid's adjudication lens, verbatim · §5 the
adjudication (per-finding rulings, receipts re-verified) · §6 the candidate
amendment set (unsigned) · §7 candidate register additions · §8 note for the
breakup session.

Fidelity: §3 is Sid's in-chat relay of Codex's output, untouched (one stray
leading character from the paste envelope dropped). §1 is the packet byte-same
with what Sid ran. exchange-04 stays untouched; amendments live here as
candidates until Sid's signature round.

---

## §1 · The packet sent to Codex

MODE: GATE (falsification round)
ALTITUDE: architecture — laws and part decomposition. Implementation detail is in scope only where it falsifies a law; do not review engineering quality.
AUTHORITY: candidate — converged but unsigned. Nothing in the artifact is protected from attack: clauses, the receipts behind them, and the one meta-ruling flagged below are all attackable.

SESSION RULES — these override AGENTS.md's build-session boot for this session:
- Sid opens this scope explicitly. This is a review-only session: no source edits, no commits, no thread files. Load `.agents/skills/review-cross-model-artifact/SKILL.md` before starting.
- Do NOT read: CLAUDE.md's doc pointers, docs/decisions.md, docs/next-prompt.md, docs/BETS.md, any docs/DIRECTION* file, any sense-line-model doc, vision/ (top level), docs/what-softland-is/vision/, or docs/what-softland-is/provenance/what-should-softland-be*.md. The round tests whether the span stands as architecture on receipts alone; those documents would contaminate the test. If a clause is unintelligible without them, that is itself a finding (report as UNDERSPECIFIED — do not go read them).
- HARD RULE: never read src/app/server/env.clj (API keys). Reference as a symbol only.
- ALLOWED sources, exhaustively: the ARTIFACT below; docs/what-softland-is/provenance/exchange-02-adjudications.md and exchange-03-ratification-round.md (the receipt trail — the file:line citations live there); the source tree under src/ (path map below); and, only to disambiguate a term the artifact uses, exchange-00/exchange-01 in the same folder — never as authority for whether a clause is right.

CONTEXT — what you are reviewing and why you:
- The artifact is a twelve-clause product-architecture span for Softland plus its open register: the converged output of a three-session exchange (two working sessions and one meta session, 2026-08-15). The adjudication and ratification rounds are banked verbatim in exchange-02/03; every adoption in those rounds cites running code.
- All three sessions are the same model family (Claude). The exchange's own closing note prices that: three kin checks ≈ one check. You are the commissioned non-kin check, run before the human (Sid) decides whether to sign. Your job is to break clauses, not to polish them.

TASK:
Attack each of the twelve clauses as a stated law. Keep these verdicts distinct:
- WRONG — the clause as stated is false or harmful: an internal contradiction, a contradiction with verified source, or a concrete scenario in which following the law produces the failure the law claims to prevent.
- UNDERSPECIFIED — two materially different architectures both satisfy the words as written; name the divergence and the first build decision where it bites.
- UNPROVEN — sound as stated, but only buildable evidence can settle it. The open register at the end of the artifact already names this kind; re-reporting a register item is noise. Report UNPROVEN only for NEW items, or to upgrade a register item to WRONG.
- HOLDS — survived your attack. Name what you attacked; a HOLDS with no named attack is empty.

Beyond the per-clause pass, attack:
- DECOMPOSITION — do clause 11's kernel parts plus the machinery clauses 1–10 declare actually cover everything the clauses invoke? Name every load-bearing mechanism the span uses but never defines or places in a part.
- CROSS-CLAUSE COMPOSITION — pairs of clauses that hold alone but compose into a contradiction or a forced bad outcome.
- RECEIPT AUDIT — verify in src/ every receipt your verdicts depend on, plus a sample of at least six more from exchange-02/03. A cited receipt that does not hold in source is a finding against every clause standing on it. Label every claim you make: verified (file:line) or inference.
- THE FLAGGED UNSIGNED DELTA — clause 9's "courts demotion" was minted by one session and ruled standing by the meta session without the other session's formal signature (the artifact's delta 3 explains the ruling). It carries no extra protection; attack the ruling if you disagree with it.

CHECK SPECIFICALLY (run per clause where it applies):
1. Ownership — for each object a clause posits (durable address, anchor, occurrence, hole, stale-mark, re-examination question, coverage receipt, resolution record, workshop-scoped act, coined lens): who mints it, who may write it, who clears it. Two writers without a named winner is a finding.
2. Lifecycle — the exact event that clears each object; any object that can stick forever and mask later truth.
3. Consumers — any record a law obliges that nothing is obliged to read.
4. Error paths — refused per-member acts, abandoned candidates, failed publishes: what state remains, who cleans it.
5. Ordering — races the laws permit: preview vs commit, affirmation vs movement, mint vs re-parent, publish vs a basis moving or going dark.
6. Shape — execute the span's structural claims against the actual tree (receipts, indexes, id derivations), never against intent.
7. Closure schedulability — for every closure law (clause 7 especially): can the closing event actually occur under the stated custody, or does the law pressure fabricating it?
8. The kin blind spot — name at least one assumption all three sessions plausibly share by construction (same model family, same codebase) that you would not grant.

OUTPUT (exactly this shape):
1. Per-clause verdict lines, 1–12: verdict + one sentence each.
2. Fatal findings — anything WRONG, or UNDERSPECIFIED at a fork the build hits first. Each: clauses touched · the attack as a concrete scenario or quoted contradiction · evidence (file:line or artifact quote) · smallest amendment that repairs it.
3. Soft findings — same shape, lower stakes.
4. Receipt-audit table — receipt · verified / failed / unverifiable · note.
5. Missing-organ list — machinery invoked but never placed.
6. Overall verdict: READY-TO-SIGN | NEEDS-AMENDMENT | UNSAFE, plus the single highest-leverage amendment.
Rules: every finding names a concrete failure scenario or a contradiction — no style or taste findings. An empty category is a valid result; do not manufacture findings. Do not soften them either — kin convergence is the failure mode this round exists to catch.

SOURCE PATH MAP — the files exchange-02/03 cite, resolved to real paths (line numbers in the exchange files may have drifted slightly; judge the mechanism, not the exact line). The session transcripts named in the provenance headers are not available to you; the exchange files are the record.
- src/app/shared/anatomy_material.cljc — edit-ops, edit-candidate (whole-form candidacy)
- src/app/shared/activation_event.cljc — the publication/decision event, scope vocabulary
- src/app/shared/binding_material.cljc — binding grammar, reservation predicates, claim-chain walk
- src/app/shared/verb_registry.cljc — verb rows incl. the three selection verbs
- src/app/client/workspace/block_edit.cljc — text pending-input echo profile
- src/app/client/workspace/region3d_pointer.cljc — wheel-route custody court
- src/app/client/workspace/selection.cljc — targets-by-address occurrence index
- src/app/client/substrate/region3d_material.cljc — 3D edit-diff op-log
- src/app/client/substrate/scene_tape.cljc — render-side family registration (:draft-settle)
- src/app/client/substrate/region_rungs.cljc + src/app/client/substrate/webgpu/region3d_gpu.cljs — render rung ladder
- src/app/server/rama/object_container.clj — container kernel (visibility field, stale-edit?)
- src/app/server/rama/object_container/block_distiller.clj — distiller, refine-block-path, hole-endpoint-id
- src/app/server/rama/core.clj — request envelope (:causal)
- src/app/server/rama/dogfood/space.clj — frozen context bundles (sha-256)
- src/app/server/rama/git_spine.clj — import-time edge minting, spine-sync! receipts
- src/app/server/rama/relation_kernel.clj — edge rows, asserter/envelope-actor custody
- src/app/server/rama/material_circulation.clj — receipt/silver/gold strata, autotag cascade, activation-as-of
- src/app/server/rama/trail_view.clj — trail contract ("NO as-of", line ~688)
- src/app/server_jetty.clj — :portal-briefing-bytes

ARTIFACT — verbatim from docs/what-softland-is/provenance/exchange-04-meta-syntheses.md, Synthesis 2 (the ratification deltas, the converged span, the open register). Two trailing paragraphs addressed to the human about process were omitted; nothing else is cut.

[The artifact section reproduced exchange-04's "What the final round added" (three
deltas + escape-gauge note), "The converged span" (the twelve clauses), and the
open register, verbatim — see exchange-04, which stays the owner of that text.]

---

## §2 · Claude's sealed checklist run (delivered in-chat before Codex ran; withheld from the packet to avoid anchoring)

**Receipt sample: 10/10 held** (all checked that session, current paths): two-scope vocabulary + "richer scopes wait for a need that names itself" (`activation_event.cljc:59-64`); the two reservation strengths the courts-demotion ruling stands on — camera site-scoped (`binding_material.cljc:134-148`), meta universal (`:150-161`); three medium-shaped selection verbs (`verb_registry.cljc:118-155`); envelope-actor distinct from payload asserter (`relation_kernel.clj:251-257`); "NO as-of" (`trail_view.clj:688`); `hole-endpoint-id` (`block_distiller.clj:495`); refine idempotency — "a re-refine of the same span RESOLVES to the same id" (`block_distiller.clj:997`); `targets-by-address` (`selection.cljc:94-109`); `:portal-briefing-bytes` (`server_jetty.clj:1041`); and `visibility` confirmed as field-plus-projection with no enforcement reader (`object_container.clj:105`, `:1603`) — matching the register's "hook nothing reads."

**F1 — "check-strength arithmetic" is an unplaced organ** [checked]. Zero hits anywhere in `src/`. The silver/gold strata exist (`material_circulation.clj`), but the arithmetic — discounts, "cannot mint strong verification," "discount lifts" — exists nowhere, and no clause defines or places it. Clause 4's porosity teeth and the R2 note "inherits check-strength arithmetic without any new law" stand on it. This is essay vocabulary that leaked through the fence: on the span's own terms it's either a missing thirteenth organ (natural homes: clause 2's checker tiers or clause 8's economy) or porosity's "priced" leg is unproven *and unowned*. My strongest finding.

**F2 — clause 4 × clause 10 compose badly at publish** [derived]. "No public act on silent dark ground" requires computing the act's basis closure at publish; clause 10 legalizes bounded-with-frontier reads. A bounded closure walk lets dark ground past the frontier slip through *silently* — the exact forbidden thing; an exempt walk makes publish cost unbounded on deep graphs. The span's own pattern contains the repair (every act carries basis, so the closure can be maintained incrementally, making the publish check cheap) — but the span doesn't say which reading holds. Underspecified at the first fork the build hits.

**F3 — hole self-resolution has no producer** [derived; hole-id precedent checked]. "When the dark thing publishes, hole → address mechanically" — the matching between a publish event and outstanding holes naming it belongs to no clause: publication act, resident sweep, or relation kernel? Unowned means holes stick and discounts never lift. Sub-case: the dark thing re-parented while dark breaks the deterministic id match entirely.

**F4 — near-miss minting on perceived parts** [derived; refine precedent checked]. Idempotency by (parent-address, region-identity) dedupes only *identical* region-identities. Two lassos of the same perceived face differ by pixels — two addresses, edges split across them — and the lasso is the walkthroughs' common case, not a corner. The region-identity declaration should be obliged to state its normalization, or co-reference named as the duplicate repair.

**F5 — clause 3's "byte-equal across species" over-compresses the law** [checked precedent]. The source law is: same projection result bytes, and packagers extend *what* a mind sees, never mint a machine-only *where*. A drawing's packager hands the mind a census the human never sees — legal under the source law, illegal under the clause's literal words. As worded, clause 3 bans the packagers clause 2 requires. Wording amendment before signature.

**F6 — lens admission custody unstated** [derived]. Clause 2 makes material-declaration admission a floor-tier act (R3(b)'s masquerade argument); clause 3 says an undeclared lens doesn't activate but never says who admits lens declarations. The same masquerade returns one layer up. One line closes it: admission law covers both citizenship forms.

**F7 — staleness depth** [derived, likely fine]. One-hop vs transitive is unstated; the coined-lens receipt ("revision comparison, a closed predicate") implies one-hop with cascade-through-repair. Probably intended — one clarifying word ("direct basis") closes it.

**F8 — superseded silver after gold promotion** [assumed]. Asserter-in-identity means promotion mints a second fact; what demotes the silver original is unstated. Circulation's serving law probably already prefers gold — unverified, soft.

Done-gate honesty, per the checklist's own seventh question: the failure mode this pass cannot rule out is kin correlation — these attacks come from the same model family as all three sessions. That is precisely what the Codex round exists to price.

---

## §3 · Codex's review, verbatim

## 1. Per-clause verdicts

1. **WRONG** — **VERIFIED:** import distillation creates declared-grain parts before any addressing act, so "parts are minted by the act of addressing" contradicts clauses 2/8 and source; only previously unrepresented subparts can truthfully be demand-minted ([block_distiller.clj:412](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:412), [block_distiller.clj:464](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:464)).
2. **UNDERSPECIFIED** — **VERIFIED:** text explicitly uses pending-input plus durable decisions and materialized pull, "not an echo channel," so `echo` permits both that architecture and an echo-driven buffer with different failure behavior ([block_edit.cljc:85](/mnt/data/projects/Softland/src/app/client/workspace/block_edit.cljc:85), [block_edit.cljc:167](/mnt/data/projects/Softland/src/app/client/workspace/block_edit.cljc:167)).
3. **WRONG** — **VERIFIED:** a failed portal lens returns `nil`, the prompt treats that as empty, and the resident still launches, directly falsifying "an undeclared lens doesn't activate" and the claimed structural basis-at-birth guarantee ([face_projection.clj:1994](/mnt/data/projects/Softland/src/app/server/rama/face_projection.clj:1994), [reply_to_block.cljc:82](/mnt/data/projects/Softland/src/app/shared/reply_to_block.cljc:82), [server_jetty.clj:1074](/mnt/data/projects/Softland/src/app/server_jetty.clj:1074)).
4. **WRONG** — **VERIFIED:** the cited hole is deliberately never minted; **INFERENCE from the taken write path:** later material creates a different asserted relation without retracting the hole relation, so hole→address and discount lifting are not mechanical self-healing ([block_distiller.clj:495](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:495), [block_distiller.clj:912](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:912), [relation_kernel.clj:118](/mnt/data/projects/Softland/src/app/server/rama/relation_kernel.clj:118)).
5. **UNDERSPECIFIED** — **INFERENCE:** after partial member refusal, "rollback replays the record" can mean replaying the frozen resolution or compensating only accepted member events, and those first diverge when an unapplied member receives an inverse.
6. **UNDERSPECIFIED** — **VERIFIED:** relation identity is ordered and asserter-scoped, while the law requires symmetric co-reference, leaving canonical grouping, duplicate question minting, and the winning assertion undefined at the first co-reference write ([relation_kernel.clj:118](/mnt/data/projects/Softland/src/app/server/rama/relation_kernel.clj:118)).
7. **UNDERSPECIFIED** — **INFERENCE:** re-affirmation and seam decisions lack an expected-revision comparison and strength winner, so a closure evaluated at revision N can clear a question after an endpoint has already moved to N+1.
8. **UNDERSPECIFIED** — **VERIFIED:** changing a silver proposal's asserter creates a new edge identity and a human cannot retract the resident's original edge, while "review to gold" defines neither accepted nor rejected proposal disposition ([relation_kernel.clj:459](/mnt/data/projects/Softland/src/app/server/rama/relation_kernel.clj:459), [material_circulation.clj:83](/mnt/data/projects/Softland/src/app/server/rama/material_circulation.clj:83)).
9. **UNDERSPECIFIED** — **VERIFIED:** the working resolver is innermost-first, but the final law omits court order, consumption, and receipt lifetime; the flagged unsigned courts demotion itself **HOLDS** because row reservation and resolver machinery are distinct source mechanisms ([binding_material.cljc:134](/mnt/data/projects/Softland/src/app/shared/binding_material.cljc:134), [binding_material.cljc:394](/mnt/data/projects/Softland/src/app/shared/binding_material.cljc:394)).
10. **WRONG** — **VERIFIED:** a heavy instance read can fail into an empty result marked `truncated? false`, after which its packager derives completeness from that false flag, proving that local receipt fields without mandatory composition do not make refusals loud or coverage honest ([face_projection.clj:956](/mnt/data/projects/Softland/src/app/server/rama/face_projection.clj:956), [material_portal.clj:962](/mnt/data/projects/Softland/src/app/server/rama/material_portal.clj:962)).
11. **UNDERSPECIFIED** — **VERIFIED:** source has a common request/event envelope with instance-owned depots and a separate client gesture dispatcher, so "one funnel, one dispatch" does not decide logical protocol versus physical stream or distinguish input dispatch from durable-act routing ([core.clj:5](/mnt/data/projects/Softland/src/app/server/rama/core.clj:5), [ground.cljs:3687](/mnt/data/projects/Softland/src/app/client/workspace/ground.cljs:3687)).
12. **HOLDS** — **ATTACKED:** the recursion attack fails because clauses 2/11 exempt the non-declarable floor, and source likewise excludes binding/verb-floor files from activatable policy while exposing one shared human/agent edit grammar above it ([material_circulation.clj:61](/mnt/data/projects/Softland/src/app/server/rama/material_circulation.clj:61), [anatomy_material.cljc:631](/mnt/data/projects/Softland/src/app/shared/anatomy_material.cljc:631)).

## 2. Fatal findings

### F1. Clauses 1, 2, 8 · Part birth contradicts the graph-production law

- **Attack — INFERENCE:** ingest a transcript before any mind points at it; clause 2's distiller and clause 8's importer require addressable blocks for mechanical edges, but clause 1 says the parts do not exist until an addressing act.
- **Evidence — VERIFIED:** `free-cut-part` derives structural blocks during import, tool blocks are explicitly P1-minted, and mechanical edges target those units ([block_distiller.clj:412](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:412), [block_distiller.clj:464](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:464), [exchange-02:196](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:196)).
- **Smallest amendment — PROPOSAL:** "Native, authored, and declared distiller-grain parts may be minted at birth/import; an addressing act demand-mints only a previously unrepresented subpart, idempotent under its parent and typed region identity."

### F2. Clause 2 · `echo` admits the failure the running text architecture excludes

- **Attack — INFERENCE:** an implementer treats server echoes as the focused buffer's authority; a conflated or lost echo then leaves phantom text or overwrites newer pending input, while a second implementation keeps pending input local and uses decisions plus materialized truth—both satisfy the word `echo`.
- **Evidence — VERIFIED:** source explicitly says the buffer is "pending-INPUT, not an echo channel," accepted decisions retire pending entries, and rejection restores materialized text ([block_edit.cljc:85](/mnt/data/projects/Softland/src/app/client/workspace/block_edit.cljc:85), [block_edit.cljc:167](/mnt/data/projects/Softland/src/app/client/workspace/block_edit.cljc:167)); the exchange nevertheless names this profile "committed echo" ([exchange-02:136](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:136)).
- **Smallest amendment — PROPOSAL:** replace `echo` with `pending-input + durable decision + materialized pull`, or define `echo` as non-authoritative confirmation that can never drive the pending buffer.

### F3. Clauses 3, 8 · Lens activation currently fails open, killing receipt-as-birth-record

- **Attack — VERIFIED taken path:** portal construction throws; `portal-briefing` returns `nil`; `nil` becomes an empty prefix; the resident process still starts and can produce durable output without a portal cut or basis receipt.
- **Evidence — VERIFIED:** the lens wrapper documents "an unbriefed resident, never a blocked summon" and returns `nil` on failure ([face_projection.clj:1994](/mnt/data/projects/Softland/src/app/server/rama/face_projection.clj:1994)); prompt composition treats nil projection as empty ([reply_to_block.cljc:82](/mnt/data/projects/Softland/src/app/shared/reply_to_block.cljc:82)); the server then constructs argv and spawns the process ([server_jetty.clj:1074](/mnt/data/projects/Softland/src/app/server_jetty.clj:1074), [server_jetty.clj:1116](/mnt/data/projects/Softland/src/app/server_jetty.clj:1116)).
- **Smallest amendment — PROPOSAL:** make lens admission a required durable decision: missing/invalid cut, basis, or coverage refuses invocation and deposit; the accepted receipt and deposited material's `:based-on` edges must be written under one idempotent lifecycle.

### F4. Clauses 4, 8 · The porosity receipt proves addressable absence, not self-resolution

- **Attack — INFERENCE from verified writes:** an unpaired tool use first asserts `use → hole`; when its result arrives, replanning asserts `use → result`; because the endpoint participates in relation identity and no retraction is issued, the original hole remains asserted and can keep darkness pricing alive forever.
- **Evidence — VERIFIED:** the hole ID is deliberately never minted ([block_distiller.clj:495](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:495)); pairing substitutes a different endpoint ([block_distiller.clj:503](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:503)); the driver only appends assertions ([block_distiller.clj:912](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:912)); relation identity includes the target ([relation_kernel.clj:118](/mnt/data/projects/Softland/src/app/server/rama/relation_kernel.clj:118)).
- **Smallest amendment — PROPOSAL:** give every hole an explicit resolution record owned by its producer; later publication must atomically resolve or retract the hole edge, bind it to the real address, and trigger every pricing consumer to recompute.

### F5. Clause 5 · Partial failure leaves rollback's target set ambiguous

- **Attack — INFERENCE:** resolve 100 members, accept 98, refuse 2, then roll back; replaying the frozen resolution attempts inverses for two effects that never happened, while replaying accepted member events compensates exactly 98.
- **Evidence — VERIFIED precedent:** the existing batch receipt distinguishes accepted/fresh/converged/rejected/unresolved outcomes rather than collapsing membership into execution ([git_spine.clj:270](/mnt/data/projects/Softland/src/app/server/rama/git_spine.clj:270)); the exchange says both "rollback over the recorded resolution" and "per-member decisions," without choosing the operative set ([exchange-02:180](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:180)).
- **Smallest amendment — PROPOSAL:** rollback references accepted member event IDs, never merely the resolution hash; refused/unresolved member records remain audit evidence but mint no inverse.

### F6. Clauses 6, 8 · Symmetric co-reference has no identity or question winner

- **Attack — INFERENCE:** a human and a resident each assert that A and B co-refer; source-shaped identity makes two ordered, asserter-scoped facts, so movement can mint one endpoint-pair question that improperly clears both assertions or two indistinguishable questions that duplicate the seam.
- **Evidence — VERIFIED:** relation identity includes ordered `from`, ordered `to`, and asserter ([relation_kernel.clj:118](/mnt/data/projects/Softland/src/app/server/rama/relation_kernel.clj:118)); the current closed relation vocabulary contains no co-reference kind or canonical symmetric grouping ([relation_kernel.clj:58](/mnt/data/projects/Softland/src/app/server/rama/relation_kernel.clj:58)).
- **Smallest amendment — PROPOSAL:** define an unordered co-reference group ID separately from per-asserter assertion IDs, and state whether re-examination questions attach to one assertion or to the group with an explicit aggregation winner.

### F7. Clauses 6, 7 · Re-examination closure is not schedulable against concurrent movement

- **Attack — INFERENCE:** a mind examines affirmation pair `[A@2,B@1]`; before its re-affirmation lands, A moves to revision 3; without compare-and-set custody, the late closure clears the question while the edge is already stale again.
- **Evidence — VERIFIED structure:** revisions carry parent and identity ([object_container.clj:109](/mnt/data/projects/Softland/src/app/server/rama/object_container.clj:109)); **artifact evidence:** clause 7 requires only "strength-carrying re-affirmation" and does not require the affirmed pair still to be current.
- **Smallest amendment — PROPOSAL:** every closer cites question ID plus expected endpoint revisions; the funnel accepts closure only if those revisions are still current, otherwise refuses it and leaves or reopens the question; closure appends a resolution record rather than deleting movement evidence.

### F8. Clauses 8, 6 · Review-to-gold has no rejected or superseded state

- **Attack — INFERENCE:** a resident files a false silver edge and Sid rejects it; source custody forbids Sid from retracting the resident's edge, and the span obliges no consumer to read a review decision, so the proposal remains live indefinitely.
- **Evidence — VERIFIED:** only the original asserter may retract an edge ([relation_kernel.clj:459](/mnt/data/projects/Softland/src/app/server/rama/relation_kernel.clj:459)); current composition retains separate silver and gold collections rather than a promotion/disposition transition ([material_circulation.clj:978](/mnt/data/projects/Softland/src/app/server/rama/material_circulation.clj:978)).
- **Smallest amendment — PROPOSAL:** review mints an immutable decision keyed to the proposal relation ID; acceptance may mint a separate gold assertion, rejection marks the proposal rejected, and every semantic consumer must join the disposition before treating silver as live.

### F9. Clauses 9, 11 · Court order and receipt custody disappeared from the final law

- **Attack — INFERENCE:** an overlay contains a Region3D view and both claim the same pointer sample; outer-first and inner-first courts both "adjudicate at every boundary," but land on different owners.
- **Evidence — VERIFIED:** the running resolver requires claims "INNERMOST FIRST" and stops at the first decision ([binding_material.cljc:394](/mnt/data/projects/Softland/src/app/shared/binding_material.cljc:394)); the wheel court likewise grants region custody before ground ([region3d_pointer.cljc:102](/mnt/data/projects/Softland/src/app/client/workspace/region3d_pointer.cljc:102)); ordinary binding decisions are returned values, while only the wheel court has a standing receipt counter ([ground.cljs:3731](/mnt/data/projects/Softland/src/app/client/workspace/ground.cljs:3731), [scroll.cljs:15](/mnt/data/projects/Softland/src/app/client/workspace/runtime/scroll.cljs:15)).
- **Smallest amendment — PROPOSAL:** restore "complete containment chain, innermost-first; a claimed/consumed decision terminates outward travel; one immutable receipt records every visited boundary and terminal owner," and name whether that receipt is ephemeral telemetry or durable material.
- **Unsigned delta — VERIFIED:** courts should remain demoted to machinery: camera/meta reservations reject rows, while `resolve-binding` is the unbindable code-floor decision procedure ([binding_material.cljc:134](/mnt/data/projects/Softland/src/app/shared/binding_material.cljc:134), [binding_material.cljc:394](/mnt/data/projects/Softland/src/app/shared/binding_material.cljc:394)).

### F10. Clauses 3, 10 · Coverage does not compose, so a lens can certify a failed read as complete

- **Attack — VERIFIED structure, INFERENCE outcome:** the instance-tier read throws and returns `{instances {}, instances-truncated? false, instances-error …}`; the portal tests only `instances-truncated?`, so with its other sections complete it emits `truncation/complete? true` while silently dropping the instance error.
- **Evidence — VERIFIED:** the failing branch marks truncation false ([face_projection.clj:956](/mnt/data/projects/Softland/src/app/server/rama/face_projection.clj:956)); the sub-projection wrapper escalates only `:conversation/error`, not `:facet-materials/instances-error` ([material_portal.clj:90](/mnt/data/projects/Softland/src/app/server/rama/material_portal.clj:90)); portal completeness is the conjunction of section truncation flags ([material_portal.cljc:666](/mnt/data/projects/Softland/src/app/shared/material_portal.cljc:666), [material_portal.clj:962](/mnt/data/projects/Softland/src/app/server/rama/material_portal.clj:962)).
- **Smallest amendment — PROPOSAL:** coverage is a mandatory compositional sum type: any child refusal/error makes every enclosing result non-exhaustive or refused, and packagers/lenses must propagate the weakest child status rather than copying bespoke booleans.

### F11. Clauses 9, 11 · "One dispatch" conflates two architecture layers

- **Attack — INFERENCE:** implement "one funnel" as one global durable depot and "one dispatch" as the gesture resolver; alternatively keep the shared envelope, domain-owned depots, and a separate input dispatcher—both satisfy the clause but assign ordering and policy to different owners at the first append.
- **Evidence — VERIFIED:** common core explicitly says instance modules own their depots, topologies, and PStates ([core.clj:5](/mnt/data/projects/Softland/src/app/server/rama/core.clj:5)); client gesture dispatch is independently defined as normalize→pick→resolve→invoke ([ground.cljs:3683](/mnt/data/projects/Softland/src/app/client/workspace/ground.cljs:3683)).
- **Smallest amendment — PROPOSAL:** state "one logical act envelope and decision protocol, with family/kernel-owned physical depots," and separately name the input court/verb dispatcher; define the exact seam where a claimed gesture becomes a durable act request.

## 3. Soft findings

### S1. Clause 4 · `signed` is stronger than its source receipt

- **Attack — INFERENCE:** a downstream reader treats `:activation/actor {:actor/id "sid"}` as authenticated authorship even though the event form only validates a nonblank actor label.
- **Evidence — VERIFIED:** the constructor copies the supplied actor and validation checks only string/keyword shape; there is no signature or verified-principal field in the closed event keys ([activation_event.cljc:74](/mnt/data/projects/Softland/src/app/shared/activation_event.cljc:74), [activation_event.cljc:99](/mnt/data/projects/Softland/src/app/shared/activation_event.cljc:99), [activation_event.cljc:136](/mnt/data/projects/Softland/src/app/shared/activation_event.cljc:136)).
- **Smallest amendment — PROPOSAL:** either rename the shape `actor-attributed`, or define `signed` as an authenticated principal/signature reference validated before publication.

### S2. Clause 3 · `current` has no capture instant

- **Attack — INFERENCE:** a multi-read lens resolves one basis member before and another after a concurrent activation; both invocation-start and per-read "current" satisfy the clause but produce different deposited birth records.
- **Evidence — VERIFIED precedent only:** existing receipt resolution pins by captured time and can mark ambiguous histories ([material_circulation.clj:830](/mnt/data/projects/Softland/src/app/server/rama/material_circulation.clj:830)); **UNPROVEN:** no generic lens transaction freezes a cross-source `current` revision vector.
- **Smallest amendment — PROPOSAL:** `current` must resolve once, at invocation admission, to a recorded revision vector or explicitly non-atomic cut before selection executes.

### S3. Clauses 3, 8 · Generic lens-receipt→`:based-on` production is new and unproven

- **Attack — INFERENCE:** a valid lens receipt is stored beside a deposit, but no edge producer runs; the material is born with metadata yet remains invisible to directed-staleness consumers.
- **Evidence — VERIFIED narrow producers:** current `:based-on` writers are explicit importer paths such as git-spine and assembly provenance ([git_spine.clj:244](/mnt/data/projects/Softland/src/app/server/rama/git_spine.clj:244), [assembly_adapter.clj:436](/mnt/data/projects/Softland/src/app/server/rama/object_container/assembly_adapter.clj:436)); **UNVERIFIABLE:** the allowed source contains no generic lens-deposit producer.
- **Smallest amendment — PROPOSAL:** assign the deposit bridge ownership of idempotently creating all pinned `:based-on` edges before the material becomes visible.

### S4. Clauses 10, 4 · Background sweeps have no completion or obsolescence lifecycle

- **Attack — INFERENCE:** a sweep starts at basis B, B moves, and the sweep later publishes "exhaustive" output without either pinning B or marking the result stale.
- **Evidence — VERIFIED:** local render rungs return admission receipts ([region_rungs.cljc:50](/mnt/data/projects/Softland/src/app/client/substrate/region_rungs.cljc:50)); **UNPROVEN:** the claimed universal background-sweep escalation has no declared owner, idempotency key, cancellation, cut, or publication transition.
- **Smallest amendment — PROPOSAL:** every sweep is keyed by query+cut, publishes only against that pinned cut, and becomes stale/refused—not exhaustive—if its basis cannot be reconstructed.

### S5. Clauses 3, 4, 8, 10 · Kin blind spot

- **Attack — INFERENCE:** all three kin sessions plausibly assumed that emitting an addressable record at one seam also proves its lifecycle and downstream consumption.
- **Evidence — VERIFIED counterexamples:** an addressable hole has no resolving transition (F4), a named instance-read error does not taint parent coverage (F10), and a deterministic portal projection does not prevent fail-open invocation (F3).
- **Smallest amendment — PROPOSAL:** no clause may call a guarantee "structural" merely because a record exists; it must name the producer, transition owner, obligated consumer, and clearing event.

## 4. Receipt-audit table

| Receipt | Status | Note |
|---|---|---|
| Identity refinement by `(surface, span)` and ephemeral occurrences ([exchange-02:31](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:31)) | verified | **VERIFIED narrowly:** refinement reuses exact spans ([block_distiller.clj:1002](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:1002)); occurrences are `{view-instance,address}` session values ([selection.cljc:1](/mnt/data/projects/Softland/src/app/client/workspace/selection.cljc:1)). This does not verify generic re-parenting. |
| Every part born by addressing | failed | **VERIFIED contradiction:** free-cut/import creates declared-grain blocks before addressing ([block_distiller.clj:412](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:412)). |
| Whole-form candidacy ([exchange-02:136](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:136)) | verified | **Extra sample:** one row edit is composed, then the entire candidate is compiled before append ([anatomy_material.cljc:631](/mnt/data/projects/Softland/src/app/shared/anatomy_material.cljc:631)). |
| Text "committed echo" ([exchange-02:136](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:136)) | failed | Source explicitly says pending-input is not an echo channel; decisions and materialized pull own correction ([block_edit.cljc:85](/mnt/data/projects/Softland/src/app/client/workspace/block_edit.cljc:85)). |
| Invertible 3D op-log ([exchange-02:136](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:136)) | verified | **Extra sample:** edit values carry declared op, region/object key, and `before`/`after`, while whole-scene replacement is refused ([region3d_material.cljc:485](/mnt/data/projects/Softland/src/app/client/substrate/region3d_material.cljc:485)). |
| Render-side `:draft-settle` family declaration ([exchange-02:140](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:140)) | verified | **Extra sample:** the render family provenance schema maps `:draft-settle` to scene-entry lifecycle ([scene_tape.cljc:344](/mnt/data/projects/Softland/src/app/client/substrate/scene_tape.cljc:344)). |
| Activation scopes and compensating rollback ([exchange-02:141](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:141)) | verified | **Verified narrowly:** two scopes exist and rollback re-wears rather than deletes ([activation_event.cljc:52](/mnt/data/projects/Softland/src/app/shared/activation_event.cljc:52)); workshop scope is not yet in the form, as the register already admits. |
| Activation is `signed` | failed | Event shape contains actor attribution, not a signature or authenticated-principal proof ([activation_event.cljc:74](/mnt/data/projects/Softland/src/app/shared/activation_event.cljc:74), [activation_event.cljc:136](/mnt/data/projects/Softland/src/app/shared/activation_event.cljc:136)). |
| Portal human/resident projection equality ([exchange-02:59](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:59)) | verified | **Extra sample:** resident briefing embeds canonical EDN of the same result the portal renders ([material_portal.cljc:914](/mnt/data/projects/Softland/src/app/shared/material_portal.cljc:914), [face_projection.clj:1772](/mnt/data/projects/Softland/src/app/server/rama/face_projection.clj:1772)). |
| Undeclared/failed lens cannot activate | failed | Portal failure returns nil and invocation continues unbriefed ([face_projection.clj:1994](/mnt/data/projects/Softland/src/app/server/rama/face_projection.clj:1994), [server_jetty.clj:1074](/mnt/data/projects/Softland/src/app/server_jetty.clj:1074)). |
| Hole is addressable | verified | Deterministic never-minted hole IDs are queryable absence markers ([block_distiller.clj:495](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:495)). |
| Hole is self-resolving and discount lifts ([exchange-03:52](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-03-ratification-round.md:52)) | failed | Later pairing chooses another target/relationship; the inspected producer contains no hole retraction/resolution transition ([block_distiller.clj:503](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:503), [block_distiller.clj:912](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:912)). |
| Causal envelope carries correlation/intent ([exchange-02:180](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:180)) | verified | **Extra sample:** the common envelope carries `:correlation/id` and `:intent/id` ([core.clj:85](/mnt/data/projects/Softland/src/app/server/rama/core.clj:85), [core.clj:150](/mnt/data/projects/Softland/src/app/server/rama/core.clj:150)). |
| Frozen content-hashed context as resolution precedent ([exchange-02:180](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:180)) | verified | **Extra sample, narrow:** Space owns and SHA-256 hashes stable context-bundle material ([space.clj:386](/mnt/data/projects/Softland/src/app/server/rama/dogfood/space.clj:386)); this does not prove the generic resolution-as-claim lifecycle. |
| Per-member stale guard and batch outcome receipt ([exchange-02:180](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:180)) | verified | **Extra sample:** stale edits are rejected by sequence/idempotency state, and spine receipts separate fresh/converged/rejected/unresolved ([object_container.clj:1398](/mnt/data/projects/Softland/src/app/server/rama/object_container.clj:1398), [git_spine.clj:270](/mnt/data/projects/Softland/src/app/server/rama/git_spine.clj:270)). |
| Asserter is inside edge identity; writer custody is distinct ([exchange-02:184](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:184)) | verified | Relation ID includes asserter; event/decision rows separately record the envelope actor ([relation_kernel.clj:118](/mnt/data/projects/Softland/src/app/server/rama/relation_kernel.clj:118), [relation_kernel.clj:251](/mnt/data/projects/Softland/src/app/server/rama/relation_kernel.clj:251)). |
| Import distillers produce graph edges ([exchange-02:196](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:196)) | verified | **Extra sample:** block distiller plans `:produced`/`:grounds`; git-spine writes parent `:based-on` edges with stable keys ([block_distiller.clj:440](/mnt/data/projects/Softland/src/app/server/rama/object_container/block_distiller.clj:440), [git_spine.clj:214](/mnt/data/projects/Softland/src/app/server/rama/git_spine.clj:214)). |
| Lens invocation receipt automatically becomes deposit `:based-on` edges ([exchange-03:92](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-03-ratification-round.md:92)) | unverifiable | Current `:based-on` writers are explicit importer/assembly paths; no generic lens-deposit bridge was found in the bounded source census ([assembly_adapter.clj:436](/mnt/data/projects/Softland/src/app/server/rama/object_container/assembly_adapter.clj:436)). |
| Resident silver review promotes to gold ([exchange-02:198](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:198)) | failed | Source classifies independent silver and gold edges and preserves both; it exposes no generic review/disposition transition ([material_circulation.clj:83](/mnt/data/projects/Softland/src/app/server/rama/material_circulation.clj:83), [material_circulation.clj:978](/mnt/data/projects/Softland/src/app/server/rama/material_circulation.clj:978)). |
| Activation-as-of and trail `NO as-of` ([exchange-02:59](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:59)) | verified | **Extra sample:** activation history resolves a causal prefix, while trail address resolution explicitly reads current truth ([material_circulation.clj:830](/mnt/data/projects/Softland/src/app/server/rama/material_circulation.clj:830), [trail_view.clj:686](/mnt/data/projects/Softland/src/app/server/rama/trail_view.clj:686)). |
| Innermost custody, two reservation strengths, wheel court ([exchange-03:96](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-03-ratification-round.md:96)) | verified | **Extra sample:** source distinguishes site-scoped camera and universal meta reservation, resolves innermost-first, and routes region before ground ([binding_material.cljc:134](/mnt/data/projects/Softland/src/app/shared/binding_material.cljc:134), [binding_material.cljc:394](/mnt/data/projects/Softland/src/app/shared/binding_material.cljc:394), [region3d_pointer.cljc:102](/mnt/data/projects/Softland/src/app/client/workspace/region3d_pointer.cljc:102)). |
| Three family-shaped selection verbs ([exchange-03:98](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-03-ratification-round.md:98)) | verified | **Extra sample:** text, rendered-line, and marquee selection verbs exist with different semantics ([verb_registry.cljc:118](/mnt/data/projects/Softland/src/app/shared/verb_registry.cljc:118)). |
| Render cost rungs and loud refusal ([exchange-02:186](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-02-adjudications.md:186)) | verified | **Extra sample:** divisors 1/2/4/8 are evaluated with receipts, and denied leases select a refusal pipeline ([region_rungs.cljc:3](/mnt/data/projects/Softland/src/app/client/substrate/region_rungs.cljc:3), [region3d_gpu.cljs:1573](/mnt/data/projects/Softland/src/app/client/substrate/webgpu/region3d_gpu.cljs:1573)). |
| Every heavy read/packager returns compositional coverage with loud refusal | failed | A failed instance read returns empty plus `truncated? false`; the portal's completeness calculation consumes that false flag ([face_projection.clj:956](/mnt/data/projects/Softland/src/app/server/rama/face_projection.clj:956), [material_portal.clj:962](/mnt/data/projects/Softland/src/app/server/rama/material_portal.clj:962)). |
| Portal briefing byte receipt ([exchange-03:104](/mnt/data/projects/Softland/docs/what-softland-is/provenance/exchange-03-ratification-round.md:104)) | verified | **Extra sample:** turn-start/durable events carry actual UTF-8 briefing byte counts ([server_jetty.clj:1041](/mnt/data/projects/Softland/src/app/server_jetty.clj:1041), [server_jetty.clj:1110](/mnt/data/projects/Softland/src/app/server_jetty.clj:1110)). |
| Co-reference affirmation/questions and three debt closers | unverifiable | The closed relation set has no co-reference kind, and no question/affirmation/debt lifecycle is present in the allowed source census ([relation_kernel.clj:58](/mnt/data/projects/Softland/src/app/server/rama/relation_kernel.clj:58)). |
| "One funnel, one dispatch" | unverifiable | Source verifies a common envelope plus instance-owned depots and a separate gesture dispatcher, but the artifact does not say which meaning of "one" it intends ([core.clj:5](/mnt/data/projects/Softland/src/app/server/rama/core.clj:5), [ground.cljs:3683](/mnt/data/projects/Softland/src/app/client/workspace/ground.cljs:3683)). |

## 5. Missing-organ list

**INFERENCE from the span's decomposition:** clause 11 places the funnel, dispatch, relation edges, court walk, identity service, and floor, but the following load-bearing organs remain absent or only partially placed.

| Missing organ | Objects/mechanisms it must own | Unanswered custody/lifecycle |
|---|---|---|
| Declaration registry and admission adjudicator | Medium declarations, blast radius, checker tier, packager, settlement profile | Who versions/replaces/retires a declaration; who computes rather than self-declares blast radius; what happens to existing material when anchor or grammar law changes. |
| Address/anchor lifecycle service — identity service is only partial | Durable address, immediate anchor, re-parent, dangling endpoint, hole alias | Who writes re-anchors; how `(parent,address-region)` idempotency transfers across re-parenting; who resolves or retires holes; conflict winner for two re-anchor writers. |
| Revision/cut snapshot service | `current`, pinned revisions, affirmation pairs, background-sweep basis | Exact capture instant; cross-source consistency; who declares ambiguity; what invalidates a cut. |
| Lens registry, executor, and deposit bridge | Coined lens, cut/lacks/basis receipt, selection/shaping interpreter, lens→material birth | Who activates/deactivates/version-controls a lens; failure behavior; atomic receipt and `:based-on` production; who prevents fail-open invocation. |
| Publication/workshop authority | Workshop-scoped act, place cadence, signature, visibility, dark-basis closure | Who authenticates the signer; who applies the place dial; who enforces workshop visibility; what exact transition publishes or refuses; who resolves declared darkness. |
| Set-act coordinator and address-set store | Intent, frozen resolution, universal address-set, per-member decisions, preview, rollback | Who mints/signs/abandons a resolution; accepted-member set; immutable membership identity; partial-failure terminal state; rollback target selection. |
| Consequence/debt engine | Commission, stale-mark, re-examination question | Producer and dedupe key for each; obligated queue/seam consumers; reopen semantics; exact close transition; old evidence retention. |
| Co-reference grouping and closure adjudicator | Symmetric group, per-asserter assertions, affirmation pair, seam decision | Canonical edge/group identity; whether one assertion or group owns a question; compare-and-set closure; strength threshold and winner. |
| Graph review/disposition service | Silver proposal, review record, gold assertion, rejection/supersession | Who may review; how rejection affects consumers without rewriting asserter identity; late-review ordering; whether silver remains historical or live. |
| Coverage algebra and sweep scheduler | Coverage receipt, frontier, refusal, packager degradation, background sweep | Which reads are heavy; parent/child receipt composition; frontier continuation/expiry; sweep idempotency/cancellation; stale publish behavior; obligated consumers. |
| Input-court receipt ledger | Boundary decisions, terminal owner, occurrence mint | Boundary enumeration; innermost-first order; consume/defer semantics; ephemeral versus durable receipt; who reads it. |
| Packager registry and budget owner | Mind-channel packaging, degradation ladder, byte equality | Per-packager budget, fallback ordering, failure/refusal, and how degradation coverage reaches lens activation. |
| Check-strength and darkness-pricing service | Hole discount, verification strength, strength-carrying affirmation | Arithmetic, owner, minimum closure strength, recomputation trigger, and exact event that lifts a discount. |

Workshop visibility and staleness producers are already acknowledged in the open register; they appear here only because the decomposition invokes them without placing their owning organs.

## 6. Overall verdict

**UNSAFE**

**INFERENCE from verified source contradictions:** signing now would canonize four guarantees that the cited paths directly fail—address-only part birth, fail-closed lens activation, self-resolving holes, and compositional coverage—and would leave the first implementation decisions for rollback, co-reference, review, court order, and funnel topology unresolved.

The single highest-leverage amendment is to add a **receipt lifecycle organ** to clause 11:

> Every lens, read, court, review, closure, and publication receipt is an immutable addressable decision carrying producer, pinned cut/basis, coverage, status, and predecessor; parent receipts inherit the weakest child status; activation, deposit, publication, and debt closure refuse without an admitted receipt; holes and reviews resolve only through explicit append-only transitions that every affected consumer is obliged to read.

That one amendment repairs the kin blind spot behind clauses 3, 4, 8, and 10, although the independent clause 1, clause 2, clause 5, clause 6/7, clause 9, and clause 11 amendments remain required before signature.

Method note: the cross-model review discipline kept this at law/part-decomposition altitude, while the investigation fence is why source-established paths are labeled **VERIFIED** and their unexecuted race outcomes remain **INFERENCE**.

---

## §4 · Sid's adjudication lens, verbatim

> ok i would just say that beware of how gpt talks and its on your wise wisdom
> to accept ... NOTE that the architecture is not to be rooted in what is BUT
> what we want it to be and you are the one that will have to keep that very
> very well protected

(Sid, 2026-08-15, on relaying Codex's review.)

---

## §5 · Adjudication

Every ruling below re-verified Codex's load-bearing citations first-hand this
session (**[c]** = read in source now, exact windows). The governing law,
applied throughout, from Sid's lens: **the span is normative — the want, not
the is.** A failed receipt refutes a *descriptive* claim ("this runs today");
against a *normative* clause it reclassifies the gap as build-debt — the open
register's own category. A finding kills a clause only by hitting the want:
internal contradiction, cross-clause contradiction, or a scenario where
following the law produces the harm it exists to prevent. Second-order-mirror
was run before ingesting; the position frozen beforehand is §2, and each
acceptance below is agreement, not accommodation.

### Per-finding rulings

**Codex F1 (part birth) — ACCEPT-WITH-REFRAME.** The contradiction is real in
the clause *text* and never in the design. Exchange-02's own ruling: demand-
minting is "a generalization of my family-declared grain rather than a
replacement: pick grain is the grain a family *offers*; addressing mints parts
at whatever grain a mind actually names" (exchange-02, item 1d). Source runs
both birth modes side by side **[c]**: `free-cut-part` mints declared-grain
blocks at import (`block_distiller.clj:412-434`) while tool-result blocks
"mint lazily on edge-demand" (`:414-415, :464-468`) — the two-tier birth
already practiced. Codex's amendment restores the exchange's own ruling to the
sentence. Verdict on clause 1: **compression defect, wording amendment (A1)**
— not WRONG as want. Claude's sealed F4 (near-miss minting on perceived parts)
is the adjacent gap in the same clause; folded into A1.

**Codex F2 (echo) — ACCEPT as definition; taxonomy preserved.** Confirmed
**[c]**: `block_edit.cljc:86-90` — "The buffer is pending-INPUT, not an echo
channel (BW-T4)… Echoes do NOT drive the buffer — echoes update materialized
truth (the pull)"; decisions retire/revert (`:167-173`). The exchange's word
"echo" names the *pull* (the server echoing accepted acts into materialized
truth), but as written permits the echo-driven buffer BW-T4 exists to forbid.
The three-profile taxonomy is untouched. **Definition amendment (A2);** the
profile's name is Sid's vocabulary call. Receipt-trail correction logged.

**Codex F3 (fail-open lens) — PARTIAL.** The taken path is confirmed **[c]**:
"Any failure yields nil — an unbriefed resident, never a blocked summon"
(`face_projection.clj:1994-1997`), nil→empty prefix
(`reply_to_block.cljc:84-93`), spawn proceeds (`server_jetty.clj:1074-1116`).
Two corrections to Codex's framing: (a) clause 3 is the want — a deliberate,
documented fail-open *today* is a build-gap the signed law converts into a
violation to repair, not proof the law is wrong; verdict WRONG is
re-classified **build-gap (register)**. (b) The failure is not fully silent:
`:portal-briefing-bytes` records 0 at the turn-start event **[c]** — the tree
already refuses silence weakly, at the receipt layer. What survives as
design-work: the enforcement point. Position (Sid closes): the *summon* may
proceed — availability is the source's stated instinct — but the *deposit*
through a failed/undeclared lens cannot silently claim basis: it lands as
dark-based under clause 4's porosity (hole-typed, priced) or is refused. This
composes the two instincts instead of choosing. Codex's admission-lifecycle
proposal is the organ shape (§7). Claude's sealed F6 (lens admission custody)
is the same gap from the custody side — cross-confirmed; amendment A3.

**Codex F4 (hole self-resolution) — ACCEPT; cross-confirmed with Claude's
sealed F3.** Confirmed **[c]**: `hole-endpoint-id` deterministic and never
minted (`:495-501`); a later distill plans the paired edge to a *different*
endpoint (`:503-509`); the driver appends only (`assert-mechanical-edges!`,
`:912-919`); retraction is asserter-locked (`relation_kernel.clj:459-466`).
So the porosity delta's third leg — "self-resolving… hole → address
mechanically" — was an overreach in the ratification round: the *addressable*
leg has receipts, the *resolution transition* has none and no clause owns it,
and the *priced* leg stands on check-strength arithmetic that has zero source
presence (Claude's sealed F1, grep-confirmed). **Receipt-trail correction
logged against exchange-03 R1;** amendment A4 names the resolution owner and
places the pricing service; porosity's refuse-silent-darkness and
addressable-hole legs are preserved as want.

**Codex F5 (rollback set) — ACCEPT.** Claude's sealed pass noted this exact
ambiguity and waved it past; Codex is right that the words permit inverses on
refused members. Amendment A5: rollback replays the *accepted per-member act
record*; the resolution names scope; refused/unresolved members mint no
inverse. Consistent with the spine receipt discipline (fresh/converged/
rejected/unresolved split).

**Codex F6 (symmetric identity) — ACCEPT at organ level.** Confirmed **[c]**:
ordered, asserter-scoped identity (`relation_kernel.clj:118-129`); no
co-reference kind in the closed registry (`:58-75`). Genuine underspec at the
first co-reference write. Position taken in A6: identity of the *group* is
the unordered endpoint pair; per-asserter assertions are strength inputs
within it; the re-examination question and affirmation pair attach to the
group — one seam per subject-pair. Internals (aggregation winner) are
contract-level: breakup session.

**Codex F7 (closure vs concurrent movement) — ACCEPT as explicitness.**
Derivable from clause 1 (basis rides acts) plus the stale-guard precedent,
but law-grade text should say it: closers are acts citing the examined
endpoint revisions; the funnel refuses a stale closure. Amendment A7.

**Codex F8 (review disposition) — ACCEPT; cross-confirmed with Claude's
sealed F8, and grounded deeper by Codex.** Confirmed **[c]**: retraction
forbidden to non-asserters (`relation_kernel.clj:459-466`); silver and gold
coexist with no disposition join (`material_circulation.clj:978-990`).
Amendment A8: review mints a durable disposition; rejection suppresses for
consumers; custody untouched.

**Codex F9 (court order/receipts) — ACCEPT wording restore; and bank the
concurrence.** Innermost-first + first-decision-terminates were in the
exchange receipts and survive in source; the span compressed them away.
Amendment A9 restores them. Receipt lifetime: position — ephemeral-served by
default (the lint pattern), durable only when an act lands; register.
**Codex independently confirms the courts demotion** — the one unsigned delta
now carries a non-kin concurrence on exactly the meta-ruling's ground (two
distinct protection mechanisms in source). Sid's signature still covers it
knowingly, as exchange-04 flagged.

**Codex F10 (coverage composition) — ACCEPT: the round's law-grade
improvement to the want itself.** Confirmed **[c]**: the nil-runtime branch
serves `{instances {}, :instances-truncated? false}`
(`face_projection.clj:965-967`); the sub-wrapper escalates only
`:conversation/error` (`material_portal.clj:90-103`); completeness is a
conjunction over declared entries that consumes the flag
(`material_portal.cljc:666-679`, `material_portal.clj:962-973`). Clause 10 as
written — per-read receipts, no composition law — demonstrably reproduces the
lying map. And the source's own docstrings state the want the error path
betrays: ":truncated? is served, never silently swallowed"
(`face_projection.clj:952-953`); "one place that answers what am I not being
shown" (`material_portal.cljc:667`). Amendment A10. **Separately: this is a
live code defect today** — routed to Sid for the build lane; static structure
verified, runtime outcome not executed (investigation-fence labeling kept).

**Codex F11 (one funnel / one dispatch) — ACCEPT wording.** Confirmed
**[c]**: "Instance modules own their depots, topologies, PStates… This
namespace keeps the common request/event envelope" (`core.clj:5-9`). The
exchange's own receipts used this pattern, so intent was never one physical
depot — but the words permit it. Amendment A11 names the logical/physical
split and the gesture→act seam.

**Codex S1 (signed) — PARTIAL.** Receipt failure confirmed **[c]**:
`valid-actor?` checks a nonblank label (`activation_event.cljc:136-141`);
constructor defaults to "system" (`:107-108`). The word **signed** stays —
it is the want; at n=1 attribution degenerates to signature. Register note:
signature authority (authentication of the principal) is an organ on the
multiplayer horizon, which this exchange deliberately withheld.

**Codex S2 (`current` instant) — ACCEPT.** One resolution instant at
invocation admission, pinned revision vector or declared non-atomic — this is
what makes clause 3's "birth record at pinned revisions" well-defined. Into A3.

**Codex S3 (deposit bridge) / S4 (sweep lifecycle) — ACCEPT → organs +
register.** Both name real unbuilt machinery the span invokes.

**Codex S5 (kin blind spot) — ACCEPT: the crown of the round.** "No clause
may call a guarantee 'structural' merely because a record exists; it must
name the producer, transition owner, obligated consumer, and clearing event."
Claude's sealed pass found the same generator three times (F3/F6/F8) by
running Sid's falsification checklist — ownership, lifecycle, consumer — so
the two model families converged on the meta-law from independent evidence.
This is the checklist ascending into the constitution. Candidate amendment
A12; Codex's "receipt lifecycle organ" is its organ form.

**Missing-organ table — PARTIAL-ACCEPT.** Accepted at organ-existence
altitude; several rows' internals (cancellation keys, aggregation winners,
frontier expiry) are contract-level and DEFER to the breakup/contract
sessions, where the table becomes direct input. Two rows cross-confirm
Claude's sealed findings: "Check-strength and darkness-pricing service"
(sealed F1) and "Lens registry… deposit bridge" (sealed F6).

**Overall verdict UNSAFE — DISPUTE, with citation.** UNSAFE conflates the
two verdict kinds the packet defined. Of the four "canonized guarantees the
paths directly fail": part birth is a wording defect whose fix restores the
design's own ruling (exchange-02 item 1d + `block_distiller.clj:412-434`);
fail-closed lensing and hole resolution are want-vs-is — the register's own
build-truth category, now with named organs; coverage is the one place the
*want as written* was defective, and A10 repairs the law, not the code. With
the amendment set applied and the register grown, no false sentence remains
to sign. **Adjudicated overall: NEEDS-AMENDMENT before signature. The span's
shape — twelve clauses, two-form citizenship, three-species debt, the
unification — survives non-kin attack intact; clause 12 survived Codex's own
recursion attack outright.**

### What the round actually paid for

Cross-confirmed across model families (the strongest evidence this exchange
can produce): the hole-resolution gap, the pricing-service gap, the
lens-admission gap, the review-disposition gap, and the record-exists ≠
guarantee generator. Codex-only genuine finds: the clause-1 compression, the
coverage composition law, the rollback set, the closure CAS, the court-order
omission, the funnel ambiguity, the echo naming, `signed`-as-attribution, the
`current` instant, and a live coverage defect. Claude-only (sealed, standing):
publish-time dark-closure boundedness (folded into A4), near-miss minting
(folded into A1), byte-equal over-compression (folded into A3), staleness
depth (folded into A6). Exchange receipt-trail corrections: "committed echo,"
"signed," porosity's "self-resolving" leg, clause-1's sentence. Codex
re-verified 15+ receipts clean — the trail otherwise holds.

### Preserved (against silent retreat)

The identity triple with single-link anchors and basis-on-acts; two-form
citizenship; three-way publication custody; porosity's refuse-silent-darkness
and addressable-hole legs; the three-grain set-act; the three consequence
modes; the debt genus with three closers; the graph economy's strata; the
verb constitution with the cage-or-silence test and the courts demotion (now
non-kin-confirmed); cost citizenship (strengthened by composition, not
weakened); the kernel and the non-declarable floor; the unification.

---

## §6 · Candidate amendment set (unsigned — for Sid's signature round; exchange-04 untouched)

- **A1 (clause 1):** native, authored, and declared distiller-grain parts are
  born at import/authoring; an addressing act demand-mints a previously
  unrepresented subpart, idempotent by (parent-address, medium-typed
  region-identity). A family's region-identity declaration states its
  normalization — what counts as "the same region"; sameness beyond it is
  co-reference (clause 6), never a second identity.
- **A2 (clause 2):** the echo profile defined: per-edit-quantum committed
  acts; pending input stays local until the durable decision; the
  materialized pull is the only echo; echoes never drive the pending buffer.
  (Name of the profile: Sid's call.)
- **A3 (clause 3):** byte-equal precisely: equal at the shared projection
  surface across species; packagers extend *what* a mind sees, never mint a
  machine-only *where*. Lens declarations are admitted by floor-tier acts,
  as material declarations are. A lens's cut resolves once, at invocation
  admission, to a pinned revision vector (or declares itself non-atomic).
  An act summoned without its lens may proceed, but its deposit cannot
  silently claim basis: dark-based under clause 4, or refused. (Enforcement
  point: Sid closes.)
- **A4 (clause 4):** every hole names its resolution owner; publication of a
  formerly-dark thing closes matching holes by an explicit append-only
  transition that owner runs — that transition is the discount-lift trigger.
  The pricing itself (check-strength arithmetic) is a placed organ of clause
  8's economy, versioned and receipted. The dark-closure check at publish
  runs over incrementally maintained direct holes (acts carry basis at act
  time); publish never walks the transitive graph.
- **A5 (clause 5):** rollback replays the accepted per-member act record —
  the resolution names scope; refused or unresolved members mint no inverse
  and remain audit evidence.
- **A6 (clause 6):** co-reference identity is the unordered endpoint pair
  (the group); per-asserter assertions are strength inputs within it; the
  re-examination question and its affirmation pair attach to the group — one
  seam per subject-pair. Staleness rides *direct* basis.
- **A7 (clause 7):** closers are acts: each cites the question and the
  endpoint revisions it examined; the funnel refuses a closure whose
  endpoints have moved — the question stays open or reopens.
- **A8 (clause 8):** review mints a durable disposition: acceptance mints the
  gold assertion; rejection marks the proposal rejected and suppresses it for
  consumers; retraction custody stays with the asserter.
- **A9 (clause 9):** input custody adjudicated at every containment boundary,
  innermost-first; the first claimed/consumed decision terminates outward
  travel; every walk receipted.
- **A10 (clause 10):** coverage composes: a result that encloses children
  inherits the weakest child status; an error is a refusal, never an empty
  success; packagers propagate, never recompute, child coverage.
- **A11 (clause 11):** one *logical* act envelope and decision protocol —
  physical depots family-owned; input dispatch (the courts) is distinct from
  the act funnel; a claimed gesture becomes a durable act at the funnel's
  mouth.
- **A12 (meta-law; placement Sid's):** no clause calls a guarantee structural
  because a record exists — a structural guarantee names its producer, its
  transition owner, its obligated consumer, and its clearing event.

## §7 · Candidate register additions

Named organs (design work, unowned): lens admission + deposit bridge (the
receipt→`:based-on` writer); hole-resolution transition + check-strength/
darkness-pricing service; co-reference grouping and closure adjudicator;
graph review/disposition service; coverage algebra + background-sweep
lifecycle (pinned cut, stale-not-exhaustive); court-receipt lifetime
(position: ephemeral-served, durable when an act lands); set-act coordinator
internals; declaration-registry versioning/retirement.

Build-gaps (want signed ⇒ violations to repair): resident briefing seam is
fail-open today (`face_projection.clj:1994`; receipted at 0 bytes only);
coverage error path serves `truncated? false` with empty results and portal
completeness consumes it (`face_projection.clj:956-967`,
`material_portal.clj:90-103`, `:962-973`) — also a live defect for the build
lane now, independent of signature.

Horizon notes: `signed` = attribution at n=1; principal authentication is a
multiplayer-horizon organ (scope this exchange deliberately withheld).

## §8 · For the breakup session

Its prompt gains: **also read exchange-05** (this file) — the amendment set
§6 is part of the span it decomposes, the organ list §7 is direct input to
the part inventory, and Codex's missing-organ table (§3.5) is the checklist
its part contracts must answer. Nothing here is signed; Sid's convergence
decision covers exchange-04 + §6 together.
