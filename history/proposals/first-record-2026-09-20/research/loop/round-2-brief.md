# Round two — the common body sent to every worker

Kept so that what each worker was given can be read later. A short tailored
paragraph is added per worker at send time and recorded under "Tailored asks"
at the end of this file. Written by the orchestrator session (Claude Fable 5.1,
max effort), 20 September 2026.

---

Round two from the orchestrator: here are the current leans on Sid's table; say which your camp would reject, sharpen, or call the wrong question, each with the one case that decides it.

Your round-one report was written blind. Keep it as it is. Do not edit round one to fit what follows. Append a new section, "Round two", to the same report file.

WHOSE LEANS THESE ARE. They are the current leans of softland-ff, Sid's main session. They are not Sid's rulings. Where a lean reopens something Sid has already held, it is flagged. Sid rules; research never does. Your job is to press, not to bless.

THE LEANS (numbers are Sid's)
(0) the log: never rewritten, as Sid stated, rather than only never lost. This makes (9) and (10) now-or-never.
(2) entity id: long, random, minted by the offerer. An ingest lane derives the id from source plus form. No time inside the id.
(17) first facts: base and the gate are ordinary ids in the first facts. A finite seed, written once by the floor. Seed ids are computed from content so every store derives the same ones.
(3) key: an id too. Its word and its shape are facts about it. The ground given is shape and cost, not loss.
(9) value: stored as bytes under a per-value encryption key. Delete means throw the key away (this reaches backups and rewrites nothing). The fact keeps the value's hash. Only the value, and maybe by-whom, may ever go.
(8) by whom: the door verifies it against the logged-in session. An agent is its own actor. "Acts for" is a grant fact that exists before the agent's first write. [touches Sid's held reading that auth narrows to the crossing]
(11) when: the gate's clock only, never used for order. Based on: every read listed, including reads that only made the tool fire and entries the runtime fills in. Because of: always filled, empty only at a chain's start. No grace period for any of the three.
(16) visibility: base open; personal and session layers private; the gate's answer sits in the layer of the fact it answers.
(10) order: a layer has one home store. Within a store, partition by entity. "As of" is a position per partition (a cut), never one number.
(4) based on: each read is marked depends-on or how-she-got-here, and follow-latest or stay-on-version. The floor guesses; the tool may correct. [light reopen: the stale rule's entries]
(5) based on: write down how far the index had really got, and what was withheld. [light reopen: exactness]
(1) version: point at a fact by its own id, not by the gate's number. The id is computed from the offer's content plus a random salt. [reopens Sid's hold that version is the gate's number. It still is; it stops being the name.]
(6) keep "replacing 25" on the fact. [reopens: an offer and a fact differ by one slot each way]
(7) the gate's yes/no: kept forever, beside the fact, same partition, same layer. Refusals kept in the offerer's session layer.
(12) session start: who, runtime version, kind of machine. Every rebuild is a fact.
(14) the hand: point, select, mode, pan/zoom become facts at a tick. Looking is captured separately from being shown. A person may turn capture down or off by their own fact.
(15) a click: only tools the person enabled may act in their name. Tools in their own layers are on by default. [reopens the miss condition "in one layer"]
(13) storage: plain maps; never trimmed; backups inside the delete plan.

THE THREE ASKS
A. From your camp's view, which leans would they reject or sharpen? For each: who, what they would say, and the ONE case that decides it. Make the case concrete in Sid's world: tens of agents per person at machine rate; hundreds of people and their agents on one problem; a field seeded from about ten million papers with summary layers; the planet with an economy; years of rebuilds of the runtime; one store, with a second institution's store as the rare later case. Skip leans where your camp has nothing sourced to say. A short "stands, and here is who already lives this way" is also worth a line.
B. Which leans would your camp call the wrong question, and what would they ask instead?
C. What would they change above the table? The layers above it: an append-only store of small facts as the one substance; tools, grammars, policies and definitions as facts in the same store; one store for the planet with personal layers instead of personal databases; a fixed nine-part envelope that both ends share forever; running answers never stored, only crossings recorded; every read recorded on every fact; one writer (the gate); the runtime as the one thing that is not a fact.

DISCIPLINE, same as round one
Mark every attributed claim R (reported: they wrote it, give the URL), I (inferred: you are extending their reasoning), or N (institutional: reconstructed from papers or talks about what an organisation did). Carry the resembles/differs line: a famous name's fluency is not evidence for Sid's situation. Depth on the few that matter over coverage. Say who is missing and what you could not source. Refer to Sid as they/them. Disk: only your own report file. No commits.

WHEN DONE
Send the orchestrator at most 300 words: the two or three leans your camp presses hardest and the deciding case for each; any wrong-question call; your strongest change above the table. Then stop; round three (one cross-camp exchange) comes from me.

---

## Tailored asks

(Recorded per worker as sent.)

### research-1 (sent 11:58)

- T1. Which log: offers depot as the permanent record with facts as PState rows declared primary, against the gate re-publishing facts to a facts depot with dedupe by fact id. Each against leans (0), (7), (11), (13); what replay, restore and re-partition do; what a second store reads; which Marz would pick.
- T2. Content ids against the migration door: does the conflict remain if the id is computed once and carried as a stored name nobody re-verifies; what is lost if it is a name and not a proof.
- T3. UUIDv7 against "no time inside the id": why RPL picks v7; whether the reason applies to an id for life; cost of fully random ids in Rama; a hot entity under partition-by-entity.
- T4. The gate's three checks under a stream gate when grammar and policy facts sit on other partitions: the Rama-native options and RPL's own practice.
- T5. One cluster worldwide: known, unknown, and the exact questions for Red Planet Labs.

### research-2 (sent 12:12; told to work from what it has already read, targeted fetches only)

- T1. The unit of saying: redraw Sid's fact the camp's way; what stays on the fact, what moves to the saying; who mints the saying's id; layer on fact or saying; effect on leans (1), (6), (7), (11), (12); what is lost; what Datomic users regret about transaction-level provenance. Test case: one model reply turned into 200 facts.
- T2. XTDB v1 (hashes in the log, documents evicted) against leans (9) and (1): which failures carry over to a per-value key and a salted content id; the low-entropy case; why the authors left that design.
- T3. One order as a scope choice against lean (10): the unit of total order inside one partitioned store (entity, layer, person, problem); two test reads; what Nubank's many databases say about "as of" across them.
- T4. Reading old facts through today's schema: what must be written at admission, and where (fact, verdict, saying).
- T5. "A fixed envelope is place-oriented": what the camp would fix instead, tied to Hickey's own "per datom or dataset, in- or out-of-band" line in the HN thread.
