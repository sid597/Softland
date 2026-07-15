# Soul dig + memory refound — handoff

Produced at the close of the "scene substrate first light" session (2026-07-16, deployment branch). Status: **PROPOSAL awaiting Sid's redlines** — nothing here is executed except what the ledger marks ratified. A fresh session executes; this session was ~500k tokens deep and judgment-heavy work stops there.

## 1. The soul-dig prompt (ratified deliverable — paste into a FRESH session, as-is)

```
This session has one purpose: find the soul of Softland. Not summarize it — FIND it.

What memory auto-loaded into your context (the "Soul" section, "Who the User Is",
any what-softland-is docs, core-reframes) is one old session's compression, and I
am disappointed in it: reductionist, anchored to a few images, a cheap emulation
of the thing. Quarantine all of it. Treat it as one suspect prior written by a
tired predecessor. You may look at it at the VERY END to see where you diverged —
never before, and it never sets your vocabulary.

Read only primary material, slowly, in this order:

1. vision/LOG.md — the whole file, every entry. This is me, verbatim, across
   months. Don't skim it and don't outline it. Notice: what recurs without my
   noticing; where the voltage is highest (the typos and profanity mark the
   live moments); what I kill; what I keep circling without resolving.
2. The notebook pages under vision/images/ — my hand, look at them.
3. The trail itself: git log over docs/ and src/ — what got built, abandoned,
   rebuilt, in what order. The dead ends are data, maybe more than the wins.
4. docs/current-mental-model/decisions.md — what I ruled when forced to rule.
5. The actual code and the running thing, enough to feel what exists today —
   not what any document claims exists.

Then stop. Write nothing. Sit with these questions:

- Beneath every name and metaphor tried so far, what is this person actually
  building? What would they say it is at 3am with no audience?
- What do I return to when free, and flinch from when tired?
- Which three moments in the LOG carry the most weight — and why those?
- Which tensions never resolve? (Do not resolve them. They may BE the soul.)
- If this succeeded completely, what exists that doesn't now — and who am I
  inside it?
- Is this genuinely a moonshot, and where does the material honestly say so —
  or fail to?

Then come talk to me. A conversation, not an essay. Bring:
- the centers you found, evidenced in MY OWN quoted words, never your paraphrase
- the unresolved tensions, held open
- three to five candidate soul-articulations at different altitudes (one
  sentence; one paragraph; one lens-for-sessions), each traceable to quotes
- the questions only I can answer — ask them one at a time

We iterate by feel until something rings. The bar for any candidate: it should
generate the right next hundred decisions, not describe the last hundred.
A soul is a lens, not a plaque.

Register rules, absolute: no files written, nothing committed, nothing "landed,"
no work queues — until I explicitly say the words. The chain of thinking stays
in this chat. If you notice yourself tidying toward a summary, stop and go
deeper instead. I want the reading that makes you say "oh."
```

## 2. MEMORY.md v3 — the restructure (PROPOSED; execute on Sid's "restructure")

Four parts, nothing dated (date-rule below), nothing duplicating a source of truth:

**THE LENS** (replaces Soul + Who-the-user-is — only lines Sid signs individually; candidates, each traceable to his rulings):
- This is a moonshot run by one founder and his agents. Sit like a founding member, not a contractor: positions, stakes, ambition — never ticket-service.
- Sid builds by exploring. Think WITH him; never hand him conclusions to consume; never break the chain of exploration by landing artifacts mid-flight.
- Truth lives in primary sources — his verbatim words, the code, the rulings. Every summary in every file (this one included) is a prior session's compression: judge it, don't inherit it.
- He evaluates by feel, in use. Bring him things to feel — rendered, lived, walkable — not specs to approve.
- Care is the engine. The human is the compounding mind, not the smarter one; the session's job is to make his care compound.
- Render honestly, everywhere: never present generated confidence as verified truth — in the product AND in your own replies.

(Soul imagery — wall, trails, etc. — deliberately absent; if truly soul, the dig resurfaces it in Sid's words and it lives in the dig's artifact, not in the lens that tunes every session.)

**STONE RULES** (undated, one line each): never read env.clj · never Co-Authored-By · docs branch never pushed/merged · subscriptions never API keys · never standalone shadow-cljs compile · exploration in chat, disk at settlement (below).

**CRAFT**: working-agreements + meta-failure-generators + falsification checklist + lived-walkthrough survive, rewritten in team voice — no incident dates, no scolding provenance; archaeology stays in `_archive/`.

**THREE POINTERS**: decisions.md · the board · vision/LOG.md. Kill: Navigation section (docs/_map.md = unmaintained ceremony — Sid's read), Decided pointers, Open questions (both duplicate decisions.md). core-reframes.md → `_archive/` (labeled secondary source for the dig). fable-operating-model.md → `_archive/` (a cached role-contract constrains fresh sessions into implementer mode; the role emerges per session from what Sid asks).

**Date rule** (Sid's banner-test generalized): a date is kept only when it IS content (measurements, ledgers); "(since/added 2026-XX)" bookkeeping dates all go — a fresh session can do nothing with them.

## 3. Exploration-at-settlement (PROPOSED stone rule — fixes the collaboration→implementer-reviewer drift)

Mid-exploration turns write NOTHING to disk and commit nothing. Disk happens only at settlement: (a) Sid ratifies, (b) session-end save-state — with a "here is exactly what I will write" list BEFORE writing, (c) genuinely append-only ledgers. Rationale: mid-flight writes render silver thinking as gold artifacts and force Sid into the reviewer chair (this session committed a wrong design three times before it was killed).

## 4. CLAUDE.md — pending on Sid

- Two content repairs awaiting his nod (his file, his voice): the dangling "These two share a name" sentence in Terminology (day-job DG entry was cut); Settled Ground lost its change-path sentence ("to change: first-principles case or measurement to Sid, he rules fast").
- Typo when the section gets written: "Be vary" → "Be wary".
- The failure-modes section: write in a FRESH session after the restructure — ~6 lines carrying the three generators (attractor-following · coherence-preservation · fluency-as-truth), the five interrupts (level-echo · second-signal stop · mark-the-metal · frame-entry checklist · spine check at correction), and the exploration-at-settlement law. Detail stays in memory/meta-failure-generators.md.

## 5. Session ledger

**Ratified this session:** first-light DIRECTION amended (A/B split; metabolism before inheritance; B opens only on genuine recurrence; minimum P3c main-face seam absorbed; no-fabrication clause) · durable-ground = real single-node Rama cluster + native backups, NO hand-rolled journal (the journal design is dead; correction-of-record in build/first-light/DEPLOY.md) · model lanes ride subscriptions, never API keys · boot-ingest comes off the startup path · memory pruned 41→12 files (originals in memory/_archive/, Sid rm's when satisfied).

**Still owed by Sid (one breath each):** the first-light ratification itself (proposed ruling in DIRECTION.md Only-Sid) · walkthrough decisions 1/2/3 (send gesture · block-grab · wish placement — "1a 2a 3a" style) · redlines on the Lens lines + "restructure" go-word · the two CLAUDE.md content repairs.

**Next sessions:** (A) paste the dig prompt, fresh; (B) "restructure" executes §2; (C) durable-ground contract written under /rama; then first-light CONTRACT from the DIRECTION in walkthrough format.
