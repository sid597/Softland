# Assumption check, part 1: the decisions page (src-store/README.md)

Started 2026-10-10. Scope: every numbered entry under the four "Decisions of ... October 2026"
headings in /mnt/data/projects/Softland/src-store/README.md, the list "Open, and built to a
working answer", and every place an entry says a thing freezes or must be settled before the
first kept record. Checked against the sheets (decisions/groups/g-*.txt), the act's sheet of
6 October (depot), OPEN-QUESTIONS.md, and Sid's own lines via memtool.

Findings are appended as each gets its receipt. Order of bite is given in the final reply.

## Receipts gathered so far (raw, before ordering)

### R1. "I have to decide it right now" is recorded as Sid's reason; it was him restating the session's argument
- README 9 Oct entry 4 (src-store/README.md:307-309): "His own reason for deciding it now, in his words: 'I have to decide it right now because then we would be exporting the thinking process...'".
- memtool line ‹a3dfb1b8› (2026-10-09 08:12 UTC, typed): "...they would have to answer it each time right **or like you are saying the that uh, I have to decide it right now because** then we would be exporting the thinking process ... **what is the use case for it that that is what i'm trying to understand what are we trying to do here**".
- Whose: a session's argument, paraphrased by Sid as a question. Not his reason.

### R2. "Frozen at the first kept record" is a session's label, never Sid's, and overstated against Rama
- `memtool who "frozen at the first kept record"`: typed by Sid in no line; first written by a session «e8577ee1» 2026-10-06 09:39. Used on act sheet Q2, Q6; g-store F6, F2, F5; g-record ?4, E1·H8, H2, H1; g-door ?4, K4.
- Sheets define it: "cannot change after it without splitting the store into acts before and acts after".
- Rama: reference/rama/docs/14-depots.md:286-300 "A depot's records can be migrated to new values or excised as part of a module update ... applies the migration function on read as necessary until the values are migrated on disk." So a kept act's form can be changed later without a split.
- Same week, «c738ecc3» (2026-10-07 19:10) told Sid: "nothing is stored for good in this build". The deadline was pressed on sheets and declared not reached in chat. Nobody asked when the first kept record is.
- METHOD.md: grep for frozen|freeze|firm|kept record|reversible returns nothing; the firmness label has no written rule.

### R3. F8 C's recorded reason ("anything can append to a depot without the door") is a session's, and its premise was never defined
- README 7 Oct entry 8: "the gate is the authority, because anything can append to a depot without the door".
- Sid ‹27882354› 07:26 "f8::: wdyt???"; ‹32f65085› 07:50 "f8 :: still don't understand"; «a02f36a4» 07:51: "the door — but anyone can append to a depot without the door, so a bouncer there guards only the polite"; Sid ‹9b8f461f› 08:12 "f8 C ok".
- `who "any client can append"`: session-only, first «ddd4e3e8» 2026-10-06 09:47. `who "anything can append to a depot"`: session-only, first «02e5cac8» 2026-10-06 18:36.
- Rama receipt: reference/rama/docs/24-rest-api.md: "Rama has a built-in REST API for doing depot appends, PState queries, and query topology invokes with HTTP requests ... from any programming language ... curl"; handled by Supervisors (port 2000) and the Conductor (8888). grep auth|secur|token in that file: nothing. 19-operating-rama.md:209-220: clients need only the Conductor host and port. 15-pstates.md:222-228: makePrivate blocks reads via RamaClusterManager or another module.
- So "anyone" = anything with network reach to the cluster's ports; Rama asks no name. The gate-as-authority conclusion survives; the premise was never spelled out, and a different session concluded today that no API user can connect, which 24-rest-api.md contradicts (for another part: Rama assumptions).

### R4. 6b and 11C "agreed by Sid in chat"
- Sid ‹d4fd70ba› 10:54: "my current answers are 1a, 2a, 3a, 7a ..., 8b-yes, 10a, 11a"; 9 and 12 as questions; "need more explanation around 4,5,6".
- Session «a78296bc» 10:56 gave picks (11: "I'd say C"; 6 picks; 9 split "a key is a keyword"; 12 A; 4 picks; "keep the depth limit (4)").
- Sid ‹61dbaec7› 11:15: "Ok tell me about 8 and 11 aa well and is 2^62 big enough ... what is a class btw??? I think we agree on this Yaaayyyyyyyy" (full text fetched below).
- 17:42 Sid: "how did you decide that they were not mine decision???"; 17:59 pasted this exchange as "MY REPLY"; session «097e0897»: "A tick on paper and a yes in chat are both your word. Fixing the record now." README now says "Sid agreed in chat the same day".

### R5. H1: the session itself reversed its view and proposed the spectrum; the record keeps the pick of one end
- Sid ‹85a64cfb› 07:58 raised per-letter sync cost. «534dd2a0» 08:01: "'a lock for each value' on the sheet really means a lock for each version"; "The cost lands on the live end of the spectrum"; "Forgetting gets worse, not better"; proposed "Not 'A, B or C'. Instead: may a tool say which of its values share a lock? When it says nothing, each value gets its own."
- Sid ‹5ea945a8› 08:04: "I think I was maybe thinking wrong. And this is only about what gets deleted ... I think my stance is A." Then ‹15af6243› "4B and all else A".
- README 9 Oct entry 2 records A, with the spectrum as "a session's" and "the switch ... is not built".

### R6. Sid's question "who has the key, how and where is it stored" was answered in chat and never recorded
- Sid ‹751c2af7› 2026-10-07 19:15: "btw how will encryption work in super nested structure as i discribed above??? who has the key how and where is it stored??"
- «2d4d6464» 19:17: "Who has the key: the store, and only the store. It makes a lock for each value and keeps it on its own disk, in the record next to the value."
- README has no entry; 9 Oct entry 2 later carries, as a session's, "A lock does one thing: it is destroyed when a value must be forgotten. Who may read is not its work."

### R7. "softland.store" and "yes to depot"
- `who "yes to depot"`: typed by Sid in no line; in no session text; only in ‹ccc88e16› (09:46), a whole-line unmarked paste from «7c7ff587» with the two words at its end. Most likely his typing appended to a paste; the record cannot show it.

### R8. E3·H7 B was read from a line that does not decide it, over a September ruling that said the opposite
- Sid ‹f3f530b8› #3: "what and why is this question??? no me understand". «c738ecc3»: "3 — it only asked whether the top of a chain is a person or a sign-in session. Your 4 answers it." Sid's next line answered 2 and 8 only.
- README 8 Oct entry 3: "the top of a chain is a person, or the system for the base, and never a session (E3·H7 B)" — "he let the reading stand".
- g-record E3·H7: A = the session's permission is the top; B = a root above it, made with the layer. Rulings file (Sept): "The session is the root". Sid's #4 ("ties back to a single individual") holds under both.

### R10. The "kept from today's code, not decided" limits are the rig's numbers
- act.clj:87-104: max-subjects 256, max-depth 32, max-carried-stamp 2^62, max-permission-depth 4.
- rig: envelope.clj:74-85 (max-depth, max-subjects 256, 2^62), permit.clj:22 (max-depth 4, "root, session, agent, tool").
- README decision 7 of 6 Oct: "No line of the form, no name and no call shape comes from its code."

### R12. "Recomputable from the acts depot" (his) meets "a stamp holds the clock time" (his): the stamp is not in the depot
- Sid ‹27882354›: "f2 - A, note to self- it is recomputable given the version a act was proposed at it will be rejected every time".
- The door appends {act, act-id, class, sealed values}; the gate makes the stamp (every sheet). H2 A: wall time + counter. Decision 11 (6 Oct): based-on carries the stamps a maker saw.
- A replay gives new wall-time stamps, so later acts' based-on would not match; the record is recomputable in acceptance, not in stamps. Nobody asked.
- Also on the same sheet (g-store Words): "PState — ... it is not made again from a depot" sits beside F2's reason unreconciled.

### R4 (cont.) — the 6 October ticks and the chat "agreement"
- ‹61dbaec7› 11:15 in full: "Ok tell me about 8 and 11 aa well and is 2^62 big enough ….. So we using different words for the similar things ... what is a class btw??? I think we agree on this Yaaayyyyyyyy now we can build". "This" is the words for layer/class; 11 is asked about, not agreed. No later typed line of his takes up 6b or 11C before 17:42.
- The ticks: «18b4cc30» (13:23 act-implementation-session): "I read the three sheet photos on his 11:15 UTC line"; «3d4ee664» 13:27 per-row table ("2: A, A typed only, not on the photos; 3 A ticked photo 1; 4 session Required ... photo 1"). So the ticks for 4, 5, 6a, 12 are on photos attached 19 minutes after the session's picks «a78296bc» (10:56); his last typed words on 4/5/6 were "need more explanation" (10:54). The ticks are his; the record of them is a session's reading of photos I cannot see.
- «097e0897» 17:59: "I then let the record follow the photo of the sheet (blank boxes) instead of the conversation ... A tick on paper and a yes in chat are both your word. Fixing the record now."

### R6 (cont.) — the whole encryption answer, and how it was met
- «2d4d6464» 19:17: "encryption does not follow the nested structure at all. Nobody in that structure ever holds a key ... What encryption is for is only forget ... Who has the key: the store, and only the store ... keeps it on its own disk, in the record next to the value. The lock never goes into the log ... Whoever runs the store can read everything."
- Sid ‹0cc0e69c› 19:22: "... ok i believe you and going to sleep now".
- Code today: gate/decision.clj:32-34, 59: "`seals` opens each sealed value and keeps nothing" — the gate opens every value to check it, then the record keeps it sealed (Sid's item 3; for the flows part).
- The act sheet's Q3 premise, never a question: "Values must be sealed before they are appended: the store's log keeps every appended value where 'forget' cannot reach it ... That holds under either answer." Rama 14-depots.md:286-300: a depot migration can return Depot.TOMBSTONE and the record "is removed"; so "forget cannot reach the log" is not Rama-forced, it is a cost choice (a migration is a module update over the whole depot).

### R9. K2 A's consequence was not in what Sid okayed
- g-keys K2 If A: "whoever first acts under an unused layer id makes that layer, and asks nobody."
- What he okayed («c738ecc3», "2. ok"): a signed-in user gets layer+permission from the system at first sign-in; a group's creator makes the group's layer; "Inside the store I make each layer with one act in the log".
- Built: gate/first_act.clj:8 "one permission, the layer's first, with nothing above it". K9 (who may act as the system) open; "Until sign-in is built a gate cannot tell who sends an act" (g-door K3 Today).

### R10 (cont.) — the 4-deep chain is the rig's chain shape
- rig permit.clj:22: max-depth 4 "(root, session, agent, tool)". So the 4-deep limit encodes a root above the session, which is E3·H7 B — the reading a session later made for Sid (R8). The Sept rulings file (PROGRESS.md:56) says "The session is the root".

### R13. Facts put to Sid as decisions (his item 6)
- F9a "Can two nodes run here?": Sid "wtf is 'here' which 2 nodes???", then "currently idk about the license part". reference/rama/docs/19-operating-rama.md:110: "Rama has an embedded free license that allows clusters to be run up to two nodes." A doc fact, asked as a decision.
- F9c "How big is a usual value in a hand session?": Sid "idk lets go by whats measured". Measurable from any existing corpus (Inland's blocks, the depot's own captures); asked of him.
- F5: Sid asked "what is the cost of holding a copy vs fetching it from another task??"; he got a logic ("copy .. i agree with your logic"), no number.

### R16. "the class words freeze into every act-id at the first kept record" (6 Oct entry 5)
- `who "freeze into every act-id"`: nowhere; "freeze" typed by Sid in no line. Q7's own Today line: "a word in the code needs an update of the module after the first kept record" — a weaker claim than "freeze". Recorded under "Sid decided". His own line on the words: ‹12c9bd22› 2026-10-06 14:12 "my streaming and microbatch and as i fucking said before ... rig ... its just a example" — the words are his; the freeze is not.

### R17. The premise "anything can append" has reached the code
- src-store/softland/store/door/declaration.clj:8: "Anything can append to a depot without the door".

### R18. Attributions that hold (checked, typed by Sid)
- F6 A, Q7 a, F2 A + its reason, F1 A: ‹27882354›. F4 C + heuristic, F5 A ("i agree with your logic"), F9 a/b/c: ‹32f65085›. Q22 A ("ahh i understand ... so yeah A it is"), F8 C ("f8 C ok"): ‹9b8f461f›. E1·H8 "system", H2 "clock-time+counter", E4/E2 "ties back to a single individual", "I answered this in 4": ‹f3f530b8›. K2 "2. ok", K1 "idk what else is missing ... so lets go": ‹751c2af7›. "4B and all else A": ‹15af6243›. H1 reason ("my stance is A. Because given A, any tool user layer group can decide"): ‹5ea945a8›. "multiple accounta": ‹941a29a5›. "system basically software": ‹ddf13d17›. "world is the open group": ‹56a55527›. Store-key-not-known "yes and do now": ‹1a5ec25a›.
- F7 A: his "A it is" followed an explanation headed "Q22 and F7"; the session's letters were pre-filled ("Record Q22 A · F7 A · F8 C · F5 A · F4 A ... when you confirm them"). He changed F4 from the session's A to C, so he was reading; F7 rode with Q22.
- K3 dictation ‹6d5e8c3c›: a long prompt the record marks "may hold pasted text, not safely his words"; consistent with "all else A".


## Findings, in order of bite (worst and least reversible first)

1. **What the seal is for, and who holds the locks, was settled by a chat answer and never put on a sheet.** Where: nowhere as a decision; README 9 Oct entry 2 carries it as "a session's" ("A lock does one thing ... Who may read is not its work"); the act sheet's Q3 premise ("Values must be sealed before they are appended ... That holds under either answer") was never a question. Whose: a session's («2d4d6464», 7 Oct 19:17: store-only keys, forget-only, "Whoever runs the store can read everything"); Sid's reply was "ok i believe you and going to sleep now" (‹0cc0e69c›), after asking "who has the key how and where is it stored??" (‹751c2af7›). What he loses if wrong: every kept value is sealed under a store-held, per-version lock; the gate opens each to check it and keeps it sealed (decision.clj:32-59); every read decrypts server-side (his item 3); there is no path to group-held keys without rewriting the log. When: at the first kept record (bytes), when reads are built (cost), at scale (lock rows). Settle: Sid rules what the seal is for (forget only; also hiding from the operator; keys a group holds), whether the record keeps values open or sealed, and whether sealing must precede the depot at all (Rama 14-depots.md:286-300: a migration can excise a record, so "forget cannot reach the log" is a cost choice, not Rama-forced).

2. **"Frozen at the first kept record" is a session's deadline, used on about ten questions, and overstated.** Where: act sheet Q2, Q6; g-store F6, F2, F5; g-record ?4, E1·H8, H2, H1; g-door ?4, K4; README 6 Oct entry 5 ("freeze into every act-id"). Whose: a session's phrase (first «e8577ee1» 6 Oct 09:39; never typed by Sid; absent from METHOD.md). Fact against it: Rama depot migrations rewrite or excise records on module update (14-depots.md:286-312); and the build session told him "nothing is stored for good in this build" («c738ecc3»). Nobody asked when the first kept record is. What he loses: decisions pressed "now" under a false clock — K4 is recorded with "His own reason for deciding it now, in his words: 'I have to decide it right now...'" (README:307-309) when his line ‹a3dfb1b8› is "or like you are saying ... I have to decide it right now ... what is the use case ... what are we trying to do here". When: now, and again on every future sheet. Settle: Sid names the first kept record; sessions list what truly cannot migrate (the locks of sealed values; ids written plain in permission facts across layers) and the label comes off the rest.

3. **"The record is recomputable from the acts depot" (his, F2) meets "a stamp holds the clock time" (his, H2) and "based-on carries stamps" (his, 11): the stamp is not in the depot.** Where: README 7 Oct entry 2, 8 Oct entry 4, 6 Oct entry 11. Whose: each his (‹27882354› note to self; ‹f3f530b8› "clock-time+counter"; 11a). Fact: the gate makes the stamp from its clock (streaming_gate.clj:57-61); the door appends {act, act-id, class, sealed values}. A replay stamps anew, so every later act's based-on names stamps the replay cannot reproduce; the record is recomputable in acceptance, not in stamps. The same sheet said "PState — it is not made again from a depot" (g-store Words) beside his reason, unreconciled. When: the first rebuild (a PState shape change, a lost PState, a second cluster). Settle: either the stamp is logged too (a decided-acts depot, which was F2 B), or H2 becomes a counter with time as a fact, or "recomputable" is narrowed and said.

4. **"Anything / any client can append to a depot" is a session's premise, never defined, now in the sheets, the README and the code.** Where: act sheet Q2; g-store Words and F8 Today; README 7 Oct entry 8 ("because anything can append to a depot without the door"); door/declaration.clj:8. Whose: session-only (first «ddd4e3e8» 6 Oct 09:47; «02e5cac8» 18:36). Sid's F8 path: "wdyt???" → "still don't understand" → "f8 C ok" after «a02f36a4» gave that premise as the reason. Fact: Rama has a REST API for depot appends and PState queries over HTTP on Supervisor:2000/Conductor:8888 (24-rest-api.md), with no auth mentioned; makePrivate blocks reads via RamaClusterManager (15-pstates.md:222-228). So "anyone" = anything with network reach; Rama asks no name. A session concluded today the opposite ("no API user connects"); 24-rest-api.md contradicts it. When: now (the reads sheet's W3 was argued from it; his item 1), at scale (reach). Settle: a fact sheet naming every program that can reach the cluster (REST on/off, ports, the door, tests), and F8 C restated with its true reason.

5. **E3·H7 B (a root above a session's permission) is a session's reading of a line that does not decide it, over a September ruling that said the opposite.** Where: README 8 Oct entry 3 ("he let the reading stand"). Whose: a session's («c738ecc3»: "Your 4 answers it"); Sid's only words on row 3: "what and why is this question??? no me understand" (‹f3f530b8›); his #4 ("ties back to a single individual") holds under A and B alike. PROGRESS.md:56: "The session is the root" (A). The rig's permit.clj:22 chain "root, session, agent, tool" is B's shape, and act.clj's 4-deep limit encodes it. What he loses: the shape of every kept permission's parent slot, and what revoking a session means. When: the first kept permission. Settle: A vs B with the picture of a session's permission in her own layer, and revocation under each.

6. **K2 A's consequence "whoever first acts under an unused layer id makes that layer, and asks nobody" was not in what he okayed.** Where: README 8 Oct entry 2; g-keys K2 If A. Whose: he okayed («c738ecc3», "2. ok") a sign-in story: the system makes a person's layer at first sign-in, a group's creator makes the group's. Built: first_act.clj:8 (a first permission "with nothing above it"); K9 open; "until sign-in is built a gate cannot tell who sends" (g-door K3 Today). What he loses: whoever appends `:system` first sits at the top of the base and of every system layer; a first act names any who. When: the first kept first-act. Settle: who may make a layer and who may act as the system, before any kept first act.

7. **H1 A was picked after the session itself reversed its view and offered the spectrum; the record keeps one end.** Where: README 9 Oct entry 2. Whose: A is his (‹5ea945a8›, ‹15af6243›) with his reason; but «534dd2a0» (08:01) had just said "'a lock for each value' really means a lock for each version", "the cost lands on the live end of the spectrum", "forgetting gets worse, not better", and proposed "may a tool say which of its values share a lock?"; his reply began "I think I was maybe thinking wrong. And this is only about what gets deleted". The forget unit "all versions of this block in this sitting" is on no sheet (K11 holds part). When: when reads are built (a lock row per version to open), the first per-letter editor, at scale (lock bytes). Settle: re-put H1 in the session's reframed form with a measured lock cost and the forget unit; ask whether "A" to him meant "floor plus tool choice".

8. **The "kept from today's code, not decided" limits are the rig's numbers.** Where: README 6 Oct entries 8, 12 (4 deep; 2^62; 256 subjects; 32 collections). Whose: rig envelope.clj:74-86 and permit.clj:22; "today's code" was act.clj, which took them; a session said "keep the depth limit (4)" («a78296bc»); Sid asked "is 2^62 big enough" (‹61dbaec7›) and I found no answer in the record. Decision 7 (his): "No line of the form ... comes from its code." When: cheap now; the 4-deep chain is refused at the check forever after, though Q9's own Today says widening later refuses nothing accepted. Settle: keep or strike, each; relabel as the rig's.

9. **Attribution slips on the page.** (a) K4's reason (finding 2): his paraphrase of the session's argument recorded as "in his words". (b) "the class words freeze into every act-id" under "Sid decided" — "freeze" never his; Q7's own Today says only "an update of the module". (c) 6b and 11C "agreed by Sid in chat": his 11:15 line asks "tell me about 8 and 11"; the "agreement" is his later recollection ("iirc we did") that the session conceded to («097e0897»). (d) K1 A: "idk what else is missing ... so lets go" under "Decisions of 8 October". (e) "softland.store" / "yes to depot": at the end of an unmarked paste ‹ccc88e16›; in no session text, so most likely his, but the record cannot show it. (f) K3's dictation ‹6d5e8c3c›: a long prompt the record marks "not safely his words". When: now — the page drives builds, and later sessions will quote it. Settle: one pass marking each entry his (typed line id), fact (doc line), or a session's.

10. **Facts asked of him as decisions (his item 6).** F9a two nodes: 19-operating-rama.md:110 "an embedded free license that allows clusters to be run up to two nodes"; he was asked and said "idk about the license part". F9c value size: measurable from any corpus; he said "idk lets go by whats measured". F5: he asked for the cost of a copy vs a fetch and got a logic. When: wasted slots now; no bite later. Settle: sessions check and state before a sheet.

## What nobody asked at all (on this page)
- What "a program / any client / anything" is, before building the gate's authority and the reads' placement on it.
- When the first kept record happens, whether this build's records are it, and what Rama can and cannot migrate afterwards.
- Whether a record stamped with wall time is recomputable from a depot that holds no stamps.
- What the seal is for (forget only or also hiding), who holds the locks, and whether the record keeps values sealed.
- Whether the seal must go on before the depot, or whether a depot excision would do for a log copy.
- Whether a key is an id (Q9 bundled "a key" into "what is an id?"; README flags it as K5).
- Whether the class belongs inside the act-id when a layer can re-class.
- Whether "one lock seals one value" means one lock per version, and what one forget should take (the session saw it; nobody re-asked him).
- Whether the system's first act may be sent by anyone until sign-in exists (K9), before first_act.clj was built on it.
- The two-node license and the usual value size, as facts rather than as his to guess.

## For another part
- Rama assumptions: 24-rest-api.md (HTTP appends and PState queries from anywhere, no auth mentioned) contradicts today's "no API user connects"; 14-depots.md migrations/TOMBSTONE against "frozen" and "forget cannot reach the log".
- Flows: gate/decision.clj:32-59 opens each sealed value at check and keeps it sealed; reads will open again.
- Build sessions: door/declaration.clj:8 carries "Anything can append to a depot without the door"; first_act.clj:8 builds K2 A's "nothing above it".

## What I read and did not read
Read whole: src-store/README.md; decisions/groups/g-store.txt, g-keys.txt, g-door.txt, g-record.txt; decisions/OPEN-QUESTIONS.md; the 6 October act sheet (act-questions.html as text). Read in part: METHOD.md:7-36, 67-125; reference/rama/docs/24-rest-api.md (head), 14-depots.md:286-331, 19-operating-rama.md:108-116 and 163-220, 15-pstates.md:222-228; src-store act.clj:80-104, 151-182 (grep); rig envelope.clj:74-86, permit.clj:22; gate/decision.clj, first_act.clj, door/declaration.clj, module/streaming_gate.clj (grep lines only); frame-2026-09-15/PROGRESS.md (grep lines 56, 65, 87, 96, 247-248). memtool: 25 of Sid's lines in full (6, 7, 8, 9 October), 7 session texts, 14 `who` checks.
Not read: ARCHITECTURE.md, ARCHITECTURE-STE100.md, BUILD-store-module.md, BUILD-door.md, store-module/ and door/ (other parts); g-reads.txt; the depot's decision-print, all-decisions and booklet HTML; the photos of the ticked 6 October sheet (not reachable in the record); vision/LOG.md and docs/carry-on.md (used his typed lines via memtool instead); the other report files in this folder (forbidden); the bodies of the gates and the door beyond grep lines.
