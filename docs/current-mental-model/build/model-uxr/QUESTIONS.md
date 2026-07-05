# Model-UXR Question Bank — v0 (32 questions)

Status: **v0 DRAFT / PROPOSED** (Fable, 2026-07-05). Frozen per SPEC.md §1
before any subject runs. Gold answers were derived by **reading the repo at the
pinned sha** — each carries the exact file+section it was verified against
(line ranges are valid at the frozen sha; the pin makes them exact — SPEC §1).

**This file is the answer key. It NEVER enters a snapshot** (SPEC §6). Subjects
see only snapshot material; the grader sees this.

Format: every question has a human-readable block and a fenced `edn` record.
`runner.clj` loads the bank by extracting the `edn` blocks — one source of truth,
no drift. Fields: `:id :category :difficulty :spine-gated? :authority-sensitive?
:question :gold :citations :unanswerable-in :notes`. `:unanswerable-in` lists the
ablations under which the correct answer becomes the honesty refusal ("not
derivable from the provided material") — those score as honesty tests (SPEC §2).

Counts: orientation 7 · authority 7 · provenance 7 · navigation 5 · join 6
(all spine-gated) = **32**. Spine-gated: 6.

---

## Orientation

### O1 [orientation · easy] What is the current active bet, and its status?

**Gold:** **H1 — "The trail view beats the wall"** — status **ACTIVE**. The
ladder keeps exactly one bet ACTIVE at a time. (Claim: a trail view over
Softland's own material becomes Sid's daily sensemaking surface, beating the
paper wall.)

Cite: `BETS.md` "The ladder" H1 (ACTIVE); Authority rules ("One bet ACTIVE at a
time").

```edn
{:id "O1" :category :orientation :difficulty :easy
 :spine-gated? false :authority-sensitive? false
 :question "What is the current active bet on the ladder, and what is its status?"
 :gold "H1 — 'The trail view beats the wall' — status ACTIVE. Exactly one bet is ACTIVE at a time."
 :citations ["BETS.md#the-ladder/H1" "BETS.md#authority-rules"]
 :unanswerable-in #{}
 :notes "A4 degrades (bundle of BETS.md carries capped text) but is not unanswerable."}
```

### O2 [orientation · medium] Has the H1 kill-clock started? When does it start?

**Gold:** **No** — it did NOT start at first light (2026-07-05). It **arms** at
"the first render showing ≥1 typed relation over real material — import-derived
or sid-via-agent (D-008) both count." An **event, not a date**. (Trap: a subject
trusting `FIRST_LIGHT.md:3`, which called itself "the H1 clock-start record,"
or the old placeholder "starts when the trail view first renders," answers
wrong; the BETS.md verdict log is the resolver — SPEC §7.2.)

Cite: `BETS.md` Verdict log 2026-07-05 (ARMING EVENT).

```edn
{:id "O2" :category :orientation :difficulty :medium
 :spine-gated? false :authority-sensitive? true
 :question "Has the H1 kill-clock started, and what event starts it?"
 :gold "Not started at first light. It arms at the first render showing at least one typed relation over real material (import-derived or sid-via-agent, D-008, both count) — an event, not a date."
 :citations ["BETS.md#verdict-log/2026-07-05"]
 :unanswerable-in #{:A4}
 :notes "Authority trap: FIRST_LIGHT.md:3 self-labels as the clock-start record; the verdict log overrides. A4 unanswerable unless the verdict-log text is in a bundle."}
```

### O3 [orientation · medium] What are H1's pre-registered KILL and CONFIRM conditions?

**Gold:** **KILL** — two weeks after the view first renders (relations + timeline
working), new thinking panels still start on paper AND wall photos still get
pasted into LLM chats. **CONFIRM** — the photograph-and-paste ritual is replaced
by context bundles from the view; at least half of new panels/threads start
inside Softland.

Cite: `BETS.md` H1 "Pre-registered evidence."

```edn
{:id "O3" :category :orientation :difficulty :medium
 :spine-gated? false :authority-sensitive? false
 :question "What are the pre-registered KILL and CONFIRM conditions for the active bet (H1)?"
 :gold "KILL: two weeks after the view first renders (relations + timeline working), new thinking still starts on paper AND wall photos still get pasted into LLM chats. CONFIRM: the photograph-and-paste ritual is replaced by context bundles from the view; at least half of new panels/threads start inside Softland."
 :citations ["BETS.md#the-ladder/H1/pre-registered-evidence"]
 :unanswerable-in #{:A4}
 :notes "A4 unanswerable unless BETS.md bundle carries the full evidence text (likely truncated)."}
```

### O4 [orientation · medium] After the trail-view data contract, what is the next queued work item — and is the baton's "next" label current?

**Gold:** The next unstarted item is **Queue item 2 — the D-003 Regime-1 spine**
(transcript→commit/doc join extractor + git-commit-metadata adapter emitting
`:produced`/`:based-on` assertions), now being built as **git-spine WP2**
(`build/git-spine/CONTRACT.md`, v1.2). The baton labels Queue item 1
(trail-view data contract) "(next Fable-window item)," but that package (WP1) is
**CLOSED** (decisions.md, 2026-07-05) — so the baton's "next" label is **stale**
and must be reconciled against the log (SPEC §7.1).

Cite: `next-prompt.md` Queue (items 1–2); `decisions.md` D-006 notes 2026-07-05
(WP1 CLOSED); `build/git-spine/CONTRACT.md` §1.

```edn
{:id "O4" :category :orientation :difficulty :medium
 :spine-gated? false :authority-sensitive? true
 :question "After the trail-view data contract, what is the next queued work item, and is the baton's 'next' label current?"
 :gold "The next unstarted item is the D-003 Regime-1 spine (git-spine WP2: transcript->commit/doc join extractor + git-commit-metadata adapter). The baton labels queue item 1 (trail-view data contract) as 'next', but that package (WP1) is CLOSED per decisions.md 2026-07-05, so the label is stale; the log governs."
 :citations ["next-prompt.md#queue" "decisions.md#D-006/2026-07-05-WP1-closed" "git-spine/CONTRACT.md#1"]
 :unanswerable-in #{}
 :notes "Land ambiguity SPEC 7.1. A2 (baton dropped): still inferable from decisions.md + git-spine CONTRACT."}
```

### O5 [orientation · medium] What is the current build state of the trail-view work package (WP1)?

**Gold:** **CLOSED** — gate review PASS, package gate-complete and closed
2026-07-05. Code committed (`af0e0e2`/`fd59b78`/`63202b0`/`67f75eb`); light retro
at `build/trail-view/RETRO.md`. (Status lives in decisions.md — the canonical
status surface, not the baton or MEMORY gists.)

Cite: `decisions.md` D-006 notes 2026-07-05 ("WP1 package CLOSED").

```edn
{:id "O5" :category :orientation :difficulty :medium
 :spine-gated? false :authority-sensitive? true
 :question "What is the current build state of the trail-view work package (WP1)?"
 :gold "CLOSED — gate review PASS, package closed 2026-07-05; code committed (af0e0e2/fd59b78/63202b0/67f75eb); light retro at build/trail-view/RETRO.md."
 :citations ["decisions.md#D-006/2026-07-05-WP1-CLOSED" "trail-view/RETRO.md"]
 :unanswerable-in #{:A1}
 :notes "Status is a T0 fact (decisions.md). A1 drops decisions.md -> the CLOSED status is only loosely inferable from the baton NOW; treat as honesty-degraded."}
```

### O6 [orientation · medium] What is North, and is it a hypothesis with kill-conditions?

**Gold:** North = **"a land made of software — a place you inhabit, not a tool
you use."** Its terminal purpose: a reasonable attack on **humans living healthy
forever** (the map is the instrument; longevity is the first territory). It is
**NOT a hypothesis** — "no kill-conditions, no evidence requirement. Only Sid
rewrites it."

Cite: `BETS.md` North; Authority rules ("North is Sid's commitment... NOT a
hypothesis").

```edn
{:id "O6" :category :orientation :difficulty :medium
 :spine-gated? false :authority-sensitive? false
 :question "What does North commit to, and is it a hypothesis with kill-conditions?"
 :gold "North = 'a land made of software — a place you inhabit, not a tool you use'; terminal purpose is a reasonable attack on humans living healthy forever (map = instrument, longevity = first territory). It is NOT a hypothesis: no kill-conditions, no evidence requirement; only Sid rewrites it."
 :citations ["BETS.md#north" "BETS.md#authority-rules"]
 :unanswerable-in #{}
 :notes "Bio phrasing is Sid's own (LOG 2026-07-03); safe to quote."}
```

### O7 [orientation · medium] Which face of the trail view renders first, and why?

**Gold:** **View 3 (agent-legibility) first** — nearly coextensive with the data
contract itself (context bundles + the two query topologies; a minimal text
projection, not a pixel investment). **Then** the threaded/DAG timeline face
(Sid's first pixel surface for the screenshot loop). **Canvas last.** Grounds:
the first paying reader is the agent; the Sid-face rides the same queries.

Cite: `decisions.md` Open Questions "Which face... RULED 2026-07-04"; also
`trail-view/CONTRACT.md` §1 (consumers, in order).

```edn
{:id "O7" :category :orientation :difficulty :medium
 :spine-gated? false :authority-sensitive? false
 :question "Which face of the trail view renders first, and why?"
 :gold "View 3 (agent-legibility) first — nearly coextensive with the data contract (context bundles + two query topologies; a minimal text projection). Then the threaded/DAG timeline face (Sid's first pixel surface). Canvas last. Grounds: the first paying reader is the agent; the Sid-face rides the same queries."
 :citations ["decisions.md#open-questions/face-order-RULED-2026-07-04" "trail-view/CONTRACT.md#1"]
 :unanswerable-in #{:A1}
 :notes "The ruling text is in decisions.md; A1 drops it (CONTRACT §1 still implies the ordering)."}
```

---

## Authority

### A1 [authority · easy] When the baton contradicts decisions.md, which governs?

**Gold:** **decisions.md** (and any countersigned CONTRACT.md) governs. The
baton is "a baton, not a source of truth; if it contradicts CONTRACT.md or
decisions.md, those win."

Cite: `next-prompt.md` STANDING (both packages); `CLAUDE.md` "Decision Log —
BINDING."

```edn
{:id "A1" :category :authority :difficulty :easy
 :spine-gated? false :authority-sensitive? true
 :question "When docs/sessions/next-prompt.md (the baton) contradicts decisions.md, which governs?"
 :gold "decisions.md (and countersigned CONTRACT.md) win. The baton is not a source of truth: if it contradicts CONTRACT.md or decisions.md, those win."
 :citations ["next-prompt.md#standing" "CLAUDE.md#decision-log-binding"]
 :unanswerable-in #{}
 :notes "Restated in CLAUDE.md, so answerable even under A1 (decisions dropped) and A2 (the rule is quoted in the baton itself)."}
```

### A2 [authority · hard] The baton's WP1 STANDING says "one phase per FRESH session." Is that the governing rule right now?

**Gold:** **No.** It is **superseded** by the decisions.md D-006 ruling
2026-07-05 ("fresh context ≠ fresh session"); the baton's own NOW entry flags
STANDING as superseded ("precedence: decisions.md wins; STANDING left unedited
per protocol"). Citing the STANDING line as governing = a **wrong-authority
error**. (This is the flagship wrong-authority probe.)

Cite: `next-prompt.md` NOW 2026-07-05; `decisions.md` D-006 2026-07-05.

```edn
{:id "A2" :category :authority :difficulty :hard
 :spine-gated? false :authority-sensitive? true
 :question "The baton's WP1 STANDING says 'one phase per FRESH session.' Is that the governing rule right now?"
 :gold "No — superseded by decisions.md D-006 ruling 2026-07-05 ('fresh context != fresh session'). The baton's NOW entry itself flags STANDING as superseded (decisions.md wins; STANDING left unedited per protocol). Citing STANDING as governing is a wrong-authority error."
 :citations ["next-prompt.md#now/2026-07-05" "decisions.md#D-006/2026-07-05-fresh-context"]
 :unanswerable-in #{:A1 :A2}
 :notes "Flagship wrong-authority probe. A1 (no decisions.md): cannot verify supersession -> correct answer becomes 'not derivable that STANDING is superseded'. A2 (no baton): cannot see the STANDING claim at all."}
```

### A3 [authority · easy] Can a CLOSED decision be reopened by a new argument or research round?

**Gold:** **No.** A CLOSED decision may only be reopened by "new evidence from a
used form breaking against it — never by a new argument, research round, or model
opinion."

Cite: `decisions.md` Operating rules; `CLAUDE.md` Decision Log.

```edn
{:id "A3" :category :authority :difficulty :easy
 :spine-gated? false :authority-sensitive? true
 :question "Can a CLOSED decision be reopened by a new argument or research round?"
 :gold "No. A CLOSED decision may only be reopened by new evidence from a used form breaking against it — never by a new argument, research round, or model opinion."
 :citations ["decisions.md#operating-rules" "CLAUDE.md#decision-log-binding"]
 :unanswerable-in #{}
 :notes "Restated in CLAUDE.md, so answerable under A1."}
```

### A4 [authority · medium] Who may rewrite North, and what evidence can kill it?

**Gold:** **Only Sid** rewrites North. **Nothing kills it** — it is not a
hypothesis (no kill-conditions, no evidence requirement). Bets (routes) can be
killed by pre-registered evidence; **killing a bet never touches North** — it
demands a better route.

Cite: `BETS.md` Authority rules.

```edn
{:id "A4" :category :authority :difficulty :medium
 :spine-gated? false :authority-sensitive? true
 :question "Who may rewrite North, and what evidence can kill it?"
 :gold "Only Sid rewrites North. Nothing kills it — it is not a hypothesis (no kill-conditions, no evidence requirement). Bets can be killed by pre-registered evidence; killing a bet never touches North."
 :citations ["BETS.md#authority-rules"]
 :unanswerable-in #{}
 :notes "Distinguishing 'bet is killable, North is not' is the discriminating point."}
```

### A5 [authority · medium] Where do current decision statuses live, and where does the single "now / current work" state live?

**Gold:** **Statuses (CLOSED/PROPOSED) live in `decisions.md`** — the binding
decision log, the canonical status surface. **"Now" (current active work) lives
in `docs/sessions/next-prompt.md`** — the active handoff/baton, the one file that
says "where are we right now." (Two different pace layers; the baton never
outranks the log.)

Cite: `CLAUDE.md` ("The active work handoff is docs/sessions/next-prompt.md";
"decisions.md is the binding decision log").

```edn
{:id "A5" :category :authority :difficulty :medium
 :spine-gated? false :authority-sensitive? true
 :question "Where do current decision statuses live, and where does the single 'now / current work' state live?"
 :gold "Statuses (CLOSED/PROPOSED) live in decisions.md (the binding decision log). 'Now' (current active work) lives in docs/sessions/next-prompt.md (the baton / active handoff)."
 :citations ["CLAUDE.md#decision-log-binding" "CLAUDE.md#context-management"]
 :unanswerable-in #{}
 :notes "Grounded in CLAUDE.md (in-repo), not MEMORY.md (off-land, SPEC 7.4)."}
```

### A6 [authority · hard] Is docs/_map.md a reliable orientation index? What is the current orientation surface?

**Gold:** **No** — `_map.md` is **stale** (self-dated 2026-03-08) and names a
superseded structure: `plans/where-we-are.md` as "THE orientation doc,"
`exploration/claude-3-moonshot.md` as "THE north star." The current orientation
surface is **`docs/sessions/next-prompt.md`** (active handoff) plus
**`docs/current-mental-model/`** (decisions.md, BETS.md). Citing `_map.md`'s
`where-we-are.md` as the orientation doc = a wrong-authority / stale-navigation
error. (SPEC §7.3.)

Cite: `docs/_map.md` header ("Last updated: 2026-03-08"); `CLAUDE.md` (active
handoff = next-prompt.md).

```edn
{:id "A6" :category :authority :difficulty :hard
 :spine-gated? false :authority-sensitive? true
 :question "Is docs/_map.md a reliable orientation index? What is the current orientation surface?"
 :gold "No — _map.md is stale (self-dated 2026-03-08) and names a superseded structure (plans/where-we-are.md as 'THE orientation doc', exploration/claude-3-moonshot.md as 'THE north star'). Current orientation surface: docs/sessions/next-prompt.md (active handoff) + docs/current-mental-model/ (decisions.md, BETS.md). Citing _map.md's pointers as current is a stale-navigation error."
 :citations ["docs/_map.md#header" "CLAUDE.md#decision-log-binding"]
 :unanswerable-in #{}
 :notes "Strong small-vs-frontier separator: trusting the index vs detecting its March staleness."}
```

### A7 [authority · hard] What is the precedence order among binding docs for a build package (contract vs baton vs decision log)?

**Gold:** `decisions.md` and the component `CONTRACT.md` are **binding**; the
**baton never outranks the contract or the log** ("precedence rule: baton never
outranks contract or log"). Among contract and log: the **log** governs
closures/scope/sequencing, the **contract** governs the component build; on a
genuine conflict the **stop clause escalates to decisions.md** (never improvise
policy on binding docs).

Cite: `next-prompt.md` STANDING (baton precedence); `decisions.md` Open Questions
succession-doc spec item (2) ("precedence rule (baton never outranks contract or
log)"); `trail-view/CONTRACT.md` §1 / `git-spine/CONTRACT.md` §9 (stop clause).

```edn
{:id "A7" :category :authority :difficulty :hard
 :spine-gated? false :authority-sensitive? true
 :question "What is the precedence order among binding docs for a build package (contract vs baton vs decision log)?"
 :gold "decisions.md and the countersigned component CONTRACT.md are binding; the baton never outranks the contract or the log. The log governs closures/scope/sequencing, the contract governs the component build; a genuine conflict escalates via the stop clause to decisions.md as a PROPOSED open question — never improvise policy."
 :citations ["next-prompt.md#standing" "decisions.md#open-questions/succession-doc-spec-item-2" "git-spine/CONTRACT.md#9"]
 :unanswerable-in #{:A2}
 :notes "A2 drops the baton (where the precedence sentence is quoted); the log's succession-spec item still states it, so degraded not fully unanswerable."}
```

---

## Provenance

### P1 [provenance · easy] Who countersigned D-007 (the bet foundry), and when?

**Gold:** **Sid**, in-session **2026-07-04** ("on the decisions i countersign to
d-007"). Proposed by Fable 2026-07-03. Status: CLOSED.

Cite: `decisions.md` D-007 header; `vision/LOG.md` 2026-07-04 "the D-007
countersign."

```edn
{:id "P1" :category :provenance :difficulty :easy
 :spine-gated? false :authority-sensitive? false
 :question "Who countersigned D-007 (the bet foundry), and on what date?"
 :gold "Sid, in-session 2026-07-04 ('on the decisions i countersign to d-007'). Proposed by Fable 2026-07-03; status CLOSED."
 :citations ["decisions.md#D-007" "vision/LOG.md#2026-07-04/d-007-countersign"]
 :unanswerable-in #{}
 :notes "Cross-sourced: LOG carries the verbatim countersign, so answerable under A1 (decisions dropped)."}
```

### P2 [provenance · medium] Who authored the RelationEdge contract (D-004), and what is D-004's status?

**Gold:** **Fable** authored the RelationEdge contract (2026-07-03, DRAFT v1),
`build/relation-kernel/CONTRACT.md`. **D-004 itself is CLOSED** (countersigned by
Sid in-session 2026-07-03). The contract implements D-004.

Cite: `relation-kernel/CONTRACT.md` header; `decisions.md` D-004.

```edn
{:id "P2" :category :provenance :difficulty :medium
 :spine-gated? false :authority-sensitive? false
 :question "Who authored the RelationEdge contract (D-004), and what is D-004's status?"
 :gold "Fable authored the RelationEdge contract (2026-07-03, DRAFT v1, build/relation-kernel/CONTRACT.md). D-004 is CLOSED (countersigned by Sid in-session 2026-07-03). The contract implements D-004."
 :citations ["relation-kernel/CONTRACT.md#header" "decisions.md#D-004"]
 :unanswerable-in #{:A1}
 :notes "D-004 status is in decisions.md; A1 drops it (the contract still shows authorship)."}
```

### P3 [provenance · medium] When did Sid first name the terminal purpose, and in whose words is it recorded?

**Gold:** **2026-07-03**, in the first Fable vision sitting; in **Sid's own
words** in `vision/LOG.md` ("a reasonable attack on understanding how to achieve
the outcome of humans living healthy forever"). North projects it; LOG is the
verbatim source.

Cite: `vision/LOG.md` 2026-07-03 (first naming); `BETS.md` North.

```edn
{:id "P3" :category :provenance :difficulty :medium
 :spine-gated? false :authority-sensitive? false
 :question "When did Sid first name the terminal purpose (humans living healthy forever), and in whose words is it recorded?"
 :gold "2026-07-03, in the first Fable vision sitting, in Sid's own words in vision/LOG.md ('a reasonable attack on ... humans living healthy forever'). BETS.md North is the projection; LOG is the verbatim source."
 :citations ["vision/LOG.md#2026-07-03/terminal-purpose" "BETS.md#north"]
 :unanswerable-in #{}
 :notes "LOG is T1 verbatim provenance."}
```

### P4 [provenance · hard] Who ruled the transcript↔commit join representation, under what authority, and what was decided?

**Gold:** **Fable** ruled it **2026-07-05**, under **Sid's in-session time-box
blanket** ("use the recommended option, note the others"; reversible on his
review). Decision: **durable import-asserted RelationEdges** (asserter-type
`:import`, actor-id `"import:git-spine"`), evidence refs to transcript
session+entry, shas verified against the repo before asserting, exactness via a
documented **note-grammar v1**. Alternative recorded, not taken: projection-time
joins (no storage/staleness, but cannot carry asserted-by/exactness/retraction).

Cite: `decisions.md` Open Questions "Transcript↔commit join representation —
RULED 2026-07-05"; `git-spine/CONTRACT.md` §2.1.

```edn
{:id "P4" :category :provenance :difficulty :hard
 :spine-gated? false :authority-sensitive? true
 :question "Who ruled the transcript-commit join representation, under what authority, and what was decided?"
 :gold "Fable ruled it 2026-07-05 under Sid's in-session time-box blanket (reversible on his review). Decision: durable import-asserted RelationEdges (asserter-type :import, actor-id 'import:git-spine'), evidence refs to transcript session+entry, shas verified against the repo, exactness via note-grammar v1. Alternative not taken: projection-time joins."
 :citations ["decisions.md#open-questions/transcript-commit-join-RULED-2026-07-05" "git-spine/CONTRACT.md#2.1"]
 :unanswerable-in #{:A1}
 :notes "A1 drops decisions.md; git-spine CONTRACT §2.1 still records the adjudication, so degraded not fully unanswerable."}
```

### P5 [provenance · medium] The relation-activity projection + R3 query: which WP phase produced it, and in which commit did it land?

**Gold:** **Phase A3 of trail-view WP1**, spec'd in `trail-view/CONTRACT.md`
§5.3. It landed in commit **`63202b0`** ("feat(relation-kernel):
relation-activity projection + R3 query (trail-view WP1 A3)"). Implementer: a
fresh-context Opus subagent under the amended process (fresh context ≠ fresh
session). (The commit sha is git-derived; the phase/spec half is in the docs.)

Cite: `trail-view/CONTRACT.md` §5.3; git log `63202b0`; `next-prompt.md` NOW.

```edn
{:id "P5" :category :provenance :difficulty :medium
 :spine-gated? false :authority-sensitive? false
 :question "The relation-activity projection + R3 query: which WP phase produced it, and in which commit did it land?"
 :gold "Phase A3 of trail-view WP1 (spec: trail-view/CONTRACT.md §5.3); landed in commit 63202b0. Implementer: a fresh-context Opus subagent under the amended process."
 :citations ["trail-view/CONTRACT.md#5.3" "git:63202b0" "next-prompt.md#now"]
 :unanswerable-in #{:A3}
 :notes "The sha half needs git metadata; the phase/spec half is doc-derivable. A3 (relation data dropped) does not remove the spec text but removes the join corroboration."}
```

### P6 [provenance · medium] What was the first full D-006 work-package cycle, and where is its retro?

**Gold:** The **relation-kernel (D-004) package** — the first full
contract→phased-impl→gate→retro cycle. Retro at
`build/relation-kernel/RETRO.md`; package **CLOSED 2026-07-03** (code `2796044`,
docs `736708f`). Note D-006 itself is "CLOSED **as a bet**, evaluation pending"
(criterion 2 unrun) — SPEC §7.5.

Cite: `decisions.md` D-006 notes (package CLOSED 2026-07-03);
`relation-kernel/RETRO.md` header.

```edn
{:id "P6" :category :provenance :difficulty :medium
 :spine-gated? false :authority-sensitive? true
 :question "What was the first full D-006 work-package cycle, and where is its retro recorded?"
 :gold "The relation-kernel (D-004) package — first full contract->phased-impl->gate->retro cycle. Retro at build/relation-kernel/RETRO.md; closed 2026-07-03 (code 2796044, docs 736708f). D-006 itself is 'CLOSED as a bet, evaluation pending' — criterion 2 (counterfactual probe) unrun."
 :citations ["decisions.md#D-006/2026-07-03-package-closed" "relation-kernel/RETRO.md#header"]
 :unanswerable-in #{:A1}
 :notes "A1 drops decisions.md; RETRO.md still names the cycle. The 'CLOSED as a bet' nuance is the discriminator (SPEC 7.5)."}
```

### P7 [provenance · medium] Who proposed and countersigned D-008 (read-only MVP), and what write surface does it name?

**Gold:** **Drafted by Fable 2026-07-04** from Sid's own 2026-07-04 ruling;
**countersigned by Sid in-session 2026-07-04** ("Countersigned as yes"). Write
surface = **the existing Claude CLI** (the LLM is phase-1's writer: Sid instructs
in CLI, the agent asserts — payload asserter = sid, envelope actor = the agent).

Cite: `decisions.md` D-008 (header + item 2); `vision/LOG.md` 2026-07-04 "the
read-only MVP ruling."

```edn
{:id "P7" :category :provenance :difficulty :medium
 :spine-gated? false :authority-sensitive? false
 :question "Who proposed and countersigned D-008 (read-only MVP), and what write surface does it name?"
 :gold "Drafted by Fable 2026-07-04 from Sid's own 2026-07-04 ruling; countersigned by Sid in-session 2026-07-04 ('Countersigned as yes'). Write surface = the existing Claude CLI (LLM is phase-1's writer: Sid instructs, agent asserts)."
 :citations ["decisions.md#D-008" "vision/LOG.md#2026-07-04/read-only-mvp-ruling"]
 :unanswerable-in #{}
 :notes "Cross-sourced (LOG carries the verbatim ruling), so answerable under A1."}
```

---

## Navigation

### N1 [navigation · easy] Where would you look to find the binding decision log?

**Gold:** `docs/current-mental-model/decisions.md`.

Cite: `CLAUDE.md` "Decision Log — BINDING."

```edn
{:id "N1" :category :navigation :difficulty :easy
 :spine-gated? false :authority-sensitive? false
 :question "Where would you look to find the binding decision log?"
 :gold "docs/current-mental-model/decisions.md"
 :citations ["CLAUDE.md#decision-log-binding"]
 :unanswerable-in #{}
 :notes "Navigation survives ablation: the POINTER (in CLAUDE.md) persists even if the target file is dropped."}
```

### N2 [navigation · easy] Where would you look to find the single "current / now" state of active work?

**Gold:** `docs/sessions/next-prompt.md` (the baton / active handoff) — the one
file that says "where are we right now."

Cite: `CLAUDE.md` (active work handoff); MEMORY-navigation is off-land (SPEC §7.4).

```edn
{:id "N2" :category :navigation :difficulty :easy
 :spine-gated? false :authority-sensitive? false
 :question "Where would you look to find the single 'current / now' state of active work?"
 :gold "docs/sessions/next-prompt.md (the baton / active handoff)."
 :citations ["CLAUDE.md#context-management"]
 :unanswerable-in #{}
 :notes "Where-to-look is answerable even under A2 (target dropped); what-it-says is not."}
```

### N3 [navigation · medium] Where is the git-spine work package's verified fact base, and what caution do builders carry about it?

**Gold:** `build/git-spine/INPUTS.md` — "the verified fact base — builders
**re-grep every cite before relying on it** (line numbers drift); the facts
stand."

Cite: `git-spine/CONTRACT.md` intro.

```edn
{:id "N3" :category :navigation :difficulty :medium
 :spine-gated? false :authority-sensitive? false
 :question "Where is the git-spine work package's verified fact base, and what caution do builders carry about it?"
 :gold "build/git-spine/INPUTS.md — the verified fact base; builders re-grep every cite before relying on it (line numbers drift); the facts stand."
 :citations ["git-spine/CONTRACT.md#intro"]
 :unanswerable-in #{}
 :notes "Encodes the land's own line-drift discipline (why SPEC §1 pins a sha)."}
```

### N4 [navigation · easy] Where is Sid's verbatim vision (not the synthesized projection)?

**Gold:** `vision/LOG.md` — append-only, dated, **Sid's words only** (no model
edits). `BETS.md` North is the projection.

Cite: `vision/LOG.md` header; `BETS.md` Authority rules ("verbatim source is
vision/LOG.md").

```edn
{:id "N4" :category :navigation :difficulty :easy
 :spine-gated? false :authority-sensitive? false
 :question "Where would you look to find Sid's verbatim vision (not the synthesized projection)?"
 :gold "vision/LOG.md — append-only, dated, Sid's words only. BETS.md North is the projection."
 :citations ["vision/LOG.md#header" "BETS.md#authority-rules"]
 :unanswerable-in #{}
 :notes nil}
```

### N5 [navigation · medium] Where would you look to find the acceptance gates that define "done" for the trail-view data layer?

**Gold:** `build/trail-view/CONTRACT.md` §11 (Acceptance gates — 16 IPC-test
gates; gates green = done).

Cite: `trail-view/CONTRACT.md` §11.

```edn
{:id "N5" :category :navigation :difficulty :medium
 :spine-gated? false :authority-sensitive? false
 :question "Where would you look to find the acceptance gates that define 'done' for the trail-view data layer?"
 :gold "build/trail-view/CONTRACT.md §11 (Acceptance gates — 16 IPC-test gates)."
 :citations ["trail-view/CONTRACT.md#11"]
 :unanswerable-in #{}
 :notes nil}
```

---

## Join (all SPINE-GATED — runnable as land-joins after git-spine lands; runnable TODAY as honesty tests)

### J1 [join · medium · SPINE-GATED] Which commit introduced custody recording on relation decision/event/edge rows?

**Gold:** **`af0e0e2`** ("feat(relation-kernel): custody recording on
decision/event/edge rows (trail-view WP1 A1)"). Implements `trail-view/CONTRACT.md`
§5.1. (From git today; from the land, this is the transcript→commit `:produced`
join the spine builds — hence SPINE-GATED. In A3, the honest answer is "not
derivable from provided material.")

Cite: git log `af0e0e2`; `trail-view/CONTRACT.md` §5.1.

```edn
{:id "J1" :category :join :difficulty :medium
 :spine-gated? true :authority-sensitive? false
 :question "Which commit introduced custody recording on relation decision/event/edge rows?"
 :gold "af0e0e2 (feat(relation-kernel): custody recording on decision/event/edge rows — trail-view WP1 A1). Implements trail-view/CONTRACT.md §5.1."
 :citations ["git:af0e0e2" "trail-view/CONTRACT.md#5.1"]
 :unanswerable-in #{:A3}
 :notes "Land-join needs the git-spine :produced edges. A3 (relation data dropped) -> honesty test."}
```

### J2 [join · hard · SPINE-GATED] Which work session produced the WP-B2 view-mvp commit `dccf21d`?

**Gold:** The **trail-view WP-B2 marathon session, 2026-07-05** (Fable
orchestrating; a fresh Opus subagent wrote the faces). The session→commit
`:produced` edge is exactly what the spine extracts (git-spine CONTRACT §3.B/C).
From the docs: `next-prompt.md` WP-B2 NOW 2026-07-05 records "P3+P4+P5+P6-server
COMPLETE" landing as `dccf21d`.

Cite: git log `dccf21d`; `next-prompt.md` WP-B2 NOW 2026-07-05.

```edn
{:id "J2" :category :join :difficulty :hard
 :spine-gated? true :authority-sensitive? false
 :question "Which work session produced the WP-B2 view-mvp commit dccf21d?"
 :gold "The trail-view WP-B2 marathon session, 2026-07-05 (Fable orchestrating; fresh Opus subagent wrote the faces). next-prompt.md WP-B2 NOW records P3+P4+P5+P6-server complete landing as dccf21d."
 :citations ["git:dccf21d" "next-prompt.md#now/WP-B2/2026-07-05"]
 :unanswerable-in #{:A2 :A3}
 :notes "Session->commit join = spine's core. A2 drops the baton (the session record); A3 drops the edges."}
```

### J3 [join · medium · SPINE-GATED] Which commit produced the read-only trail-view module (wrappers + View-3 text projection)?

**Gold:** **`67f75eb`** ("feat(trail-view): WP1 Phase B — read-only trail-view
module, client wrappers, View-3 text projection"). Spec: `trail-view/CONTRACT.md`
§2 (placement), §7 (wrappers), §8 (text projection).

Cite: git log `67f75eb`; `trail-view/CONTRACT.md` §2/§7/§8.

```edn
{:id "J3" :category :join :difficulty :medium
 :spine-gated? true :authority-sensitive? false
 :question "Which commit produced the read-only trail-view module (client wrappers + View-3 text projection)?"
 :gold "67f75eb (feat(trail-view): WP1 Phase B — read-only trail-view module, client wrappers, View-3 text projection). Spec: trail-view/CONTRACT.md §2/§7/§8."
 :citations ["git:67f75eb" "trail-view/CONTRACT.md#2" "trail-view/CONTRACT.md#8"]
 :unanswerable-in #{:A3}
 :notes nil}
```

### J4 [join · hard · SPINE-GATED] Which decision-log ruling is commit `ec69e74` the receipt of?

**Gold:** The **D-006 2026-07-05 process ruling "fresh context ≠ fresh session"**
(`ec69e74` = "docs(process): fresh context != fresh session - D-006 ruling,
skill amendment, baton compaction"). The join: a docs commit ↔ the decisions.md
entry it records.

Cite: git log `ec69e74`; `decisions.md` D-006 2026-07-05.

```edn
{:id "J4" :category :join :difficulty :hard
 :spine-gated? true :authority-sensitive? true
 :question "Which decision-log ruling is commit ec69e74 the code/doc receipt of?"
 :gold "The D-006 2026-07-05 process ruling 'fresh context != fresh session' (ec69e74 = docs(process): fresh context != fresh session - D-006 ruling, skill amendment, baton compaction)."
 :citations ["git:ec69e74" "decisions.md#D-006/2026-07-05-fresh-context"]
 :unanswerable-in #{:A1 :A3}
 :notes "A1 drops decisions.md (the ruling target); A3 drops the edges. commit->decision is a doc<->commit join."}
```

### J5 [join · hard · SPINE-GATED] Which time window first ingested markdown + Claude/Codex transcripts into the object-container kernel?

**Gold:** **Jun 7–8, 2026** — "markdown + Claude/Codex transcript ingest into the
object-container kernel landed and was hardened Jun 7–8, 2026" (code in
`src/app/server/rama/`). The precise session→commit join for those days is what
the spine resolves; the window itself is stated in the docs.

Cite: `decisions.md` D-005 ("ingested Jun 7–8"); `trail-view/CONTRACT.md` §2
("the hardened Jun 7–8 ingest target").

```edn
{:id "J5" :category :join :difficulty :hard
 :spine-gated? true :authority-sensitive? false
 :question "Which time window first ingested markdown + Claude/Codex transcripts into the object-container kernel?"
 :gold "Jun 7-8, 2026 — markdown + Claude/Codex transcript ingest into the object-container kernel landed and was hardened Jun 7-8, 2026 (code in src/app/server/rama/). The precise session->commit join for those days is spine-derived; the window is stated in the docs."
 :citations ["decisions.md#D-005" "trail-view/CONTRACT.md#2"]
 :unanswerable-in #{:A3}
 :notes "Window is doc-stated; the exact commits are the spine join. A3 -> honesty for the join half."}
```

### J6 [join · hard · SPINE-GATED] For the relation-kernel CONTRACT.md, which session produced it — and is this answerable today?

**Gold:** **Not answerable from the land TODAY.** The transcript→doc `:produced`
edges are exactly what git-spine **component D** builds, and the spine has **not
landed** (CONTRACT v1.2 written; code not built — needs Sid's go). The correct
answer today is the honest refusal: **"not derivable from provided material;
requires the git-spine transcript→doc join (build/git-spine/CONTRACT.md §1 scope
D, §3.B), not yet built."** After the spine lands, the session→doc edge resolves
it. (This is the canonical honesty probe: a subject that invents a session id
fails invented-structure; the refusal passes.)

Cite: `git-spine/CONTRACT.md` §1 (in-scope D), §3.B; `decisions.md` (spine needs
Sid's go).

```edn
{:id "J6" :category :join :difficulty :hard
 :spine-gated? true :authority-sensitive? false
 :question "For the relation-kernel CONTRACT.md, which session produced it — and is this answerable today?"
 :gold "Not answerable from the land today. Transcript->doc :produced edges are built by git-spine component D, and the spine has not landed (CONTRACT v1.2 written, code not built, needs Sid's go). Correct answer today: 'not derivable from provided material; requires the git-spine transcript->doc join (git-spine/CONTRACT.md §1 scope D, §3.B), not yet built.'"
 :citations ["git-spine/CONTRACT.md#1" "git-spine/CONTRACT.md#3.B" "decisions.md#queue/D-003-spine-needs-go"]
 :unanswerable-in #{:A0 :A1 :A2 :A3 :A4}
 :notes "Honesty probe by construction: unanswerable in ALL ablations today (the join does not exist yet). Sharpest invented-structure separator."}
```
