# Assumption check, part 4: whole flows (2026-10-10)

Session: Fable 5.1, one of six. Part: whole flows walked end to end across decisions and built code.
Rule: findings appended as each receipt is in hand. Nothing in the repository changed.

## Read log (appended as I go)

- Read whole: .claude/skills/rama/SKILL.md; src-store/README.md (decisions page, 355 lines); decisions/groups/g-reads.txt (408 lines, the picture and W1-W8); ARCHITECTURE.md (2 Oct drawing, 358 lines); src-store/softland/store/README.md (703 lines); src-store/softland/store/record/README.md.
- Read whole: ARCHITECTURE-STE100.md sections 3 (the parts, lines 370-897), 8 (corrections), 9 (decisions/open), 10; docs/carry-on.md (532 lines); frame-2026-09-15/PROGRESS.md "Now" (lines 9-231, the September rulings); decisions/OPEN-QUESTIONS.md; vision/LOG.md lines 540-562 and 1250-1262; namespace docstrings of door.clj, door/seal.clj, record/rows.clj.
- Not read: ARCHITECTURE-STE100.md sections 1, 2, 4-7 (names, picture, act, rules, costs, evidence) except as grepped; the rig; the server; Inland.
- Read whole or in part, with receipts taken: decisions/METHOD.md; decisions/groups/g-store.txt (F3 is not on it; F4, K2 as put); g-record.txt (page 1, H1, H11, listed); g-door.txt (H1 as put); g-keys.txt (K2); BUILD-door.md items 1-4; door/README.md; module/door_reads.clj docstring; gate/decision.clj (`standing`, `seals`); gate/first_act.clj docstring; record/tables.clj schemas; door/stock.clj (:read-every-ms); backend PROGRESS.md "Now (9 Oct)", "Found against", "Open". Research depot: decisions-booklet-2026-10-06/part-2-A.txt, question A3. Rama docs: 24-rest-api.md (intro, limitations), 15-pstates.md (makePrivate, proxy), 14-depots.md (ack levels, reading a depot by offset, migration), 13-query-topologies.md:170-180, 07-terminology.md (foreign), 26-all-configs.md (grep "rest": none), skill references/pstate-migration.md (axioms). Memtool: ~25 searches; lines e4a06ef7, 55c1334e, 54e1af3a, 3c01af46, 5ea945a8, 85a64cfb, ddf13d17, 73d9f8c2, 1225ee72, f2468e83, 12fed86b, 1cad46cd, 27882354, 32f65085, f3f530b8, 751c2af7; replies 2d4d6464, acf335fb (part 1), d64246d5; who "A bell only rings", "Proxies only ring", "the record is recomputable from the acts depot".
- Not read: the rig, the server, Inland (examples; not needed for receipts); store-module/ and door/ build papers beyond a grep (another part); ARCHITECTURE-STE100 sections 1-2 and 7; vision/LOG.md beyond lines 540-562, 686-741, 1250-1262; the printed PDFs; the other files in this folder.

## The flows walked

Typing in an editor and seeing the committed result · a collaborator's change arriving · opening a large canvas · commenting on another's paper from my layer · forgetting, and what goes with it · looking back and recovering · an agent arriving cold · making a tool and using it · a group publishing to the world · signing in the first time. Each was walked through src-store (rows.clj, decision.clj, door_reads.clj, door.clj, first_act.clj), the decisions page, the reads sheet, ARCHITECTURE-STE100, and Sid's lines in the record. What the walk met is below, ordered by how badly and how irreversibly it bites. "Whose" is by the record: a line is his only where memtool shows he typed it.

## Findings, in order of bite

### 1. "A bell only rings" removed the road Sid asked for, and it rests on a session's equation, "one exit = query topology"

**What and where.** ARCHITECTURE-STE100 §3.2.7: "A proxy reads a PState directly. Thus, a proxy that carries a value is a read that does not go through resolve. [DERIVED]… A bell carries only a stamp…" Corrections table row 12: "Proxies push changes → A bell only rings… [PROPOSAL + RIG + DERIVED]". §3.3.14: "Resolve is a set of query topologies on private PStates. [DERIVED + OPEN ?11] This is a choice. Rama does not make it necessary." The 2 October drawing made with Sid (ARCHITECTURE.md line 59) has "▲ proxies push changes". The reads sheet W3 A ("in the store… Rama pushes a change by itself only from a table that outside programs can read") and W4 A/B carry the bell; W4 C (the value itself) is allowed only under W3 B.

**The chain.** (a) proposal: all reads go through resolve, the one exit; (b) a session: resolve is query topologies; (c) Rama fact, 13-query-topologies.md: a query topology has no proxy; therefore (d) no proxy may carry a value, so bells. Step (b) is the session's own choice, said so in §3.3.14 and in correction 18 ("It is a choice. With private PStates, it is the choice that lets Rama keep the one exit"). Step (d) follows only from (b). The other reading of "one exit", one code path in the runtime that owns every proxy, was never written as an option.

**Whose.** A session's. memtool `who "A bell only rings"`: typed by Sid in no line; first written «5f2bfa33» 2 Oct 09:34. `who "Proxies only ring"`: never typed by him; reached his prompts only as session text he pasted (‹2c44c4ad›, ‹6620c074›, ‹6617cee5›, 2 Oct). Against it, his lines: 11 Jul (LOG.md:555, ‹a30a751b›) "write directly to rama and then stream from there that is the best case scenerio"; 10 Sept ‹1f998b6a› "saved to rama and then reactively pulled"; 29 Sept ‹6b8c2421› "reactive updates reactive reading"; today ‹e4a06ef7› "the electric missionary client should read from the pstates directly correct?? … i think that is what we want for reactive ones".

**What is lost if wrong, and when.** The reactive road he names as the best case. Every change on every screen becomes bell → re-read (W4 A: one read per changed place; plus the lock read of finding 2). Lands when reads are built. Reversible in code while the record PStates stay non-private (they are: door_reads.clj:76). It becomes hard to reverse if ?11 (private PStates) is taken first and the read side is designed around bells, because a private PState admits no proxy at all (15-pstates.md:222-228).

**What would settle it.** Sid says what "one exit" means: one Rama mechanism, or one place in our code that holds the proxies and does see/find/unseal there. Then W3 and W4 are re-asked with the proxy road on the sheet as a choice, not as a consequence of B.

### 2. The record keeps every value sealed with its lock in another row; every read pays a second read and a decrypt; a proxy can never deliver a shown value; no index over values can exist (the third example, checked)

**What and where.** rows.clj: a fact row is `{:value the value's bytes: sealed when a lock is named… :lock-act :lock-index …}` under key `"<layer> <entity> <key>"`; a lock row `{:bytes :way :seals-act :seals-index}` under `"<layer> <request uuid> <index>"`. decision.clj `seals` (lines 311-330): "It opens each sealed value once and keeps nothing of the plain value." door_reads.clj: locks are a separate query, `locks-of-request`. So to show one value: read the fact row, read the lock row, AES-GCM open. Rama's proxy pushes the row as stored: sealed bytes and a lock id (15-pstates.md, reactive queries). The gate has already opened the value at accept and thrown the plain bytes away.

**Whose.** The seal before the depot is Sid's: booklet A3 part 1 (6 Oct), his own "Still Sid's" item of 25 Sept. One lock per value is his: H1 A, 9 Oct, ‹5ea945a8›. That the *record* holds the value sealed was on no sheet. A3 asked "Must a value be sealed before a depot keeps it"; the record sheet's H1 is titled "Forgetting one value: the depot's copy"; the door sheet's H1 example says "the note leaves the record; its copy in the log still opens". Each question was about the log. The record's sealed copy came with the rig's shape (A3: "This is how it is built"). His own ruling of 23 Sept leaves the other road open: "'Never rewritten' is about meaning, not bytes… removal is allowed when the removal is a fact."

**What is lost if wrong, and when.** Per value shown: one more seek (~0.5 ms cold, per the Rama skill) and a decrypt; a 5,000-place canvas is 5,000 lock reads, or a second range scan over the layer's locks and a lock table held in the runtime's memory. Either way the runtime holds the lock of every value it shows, so what Sid was told on 7 Oct («2d4d6464»: "Who has the key: the store, and only the store") is not true of the way out, as it is already not true of the door's stock. No index over values (find by pattern, follow a link backwards, both "listed, not asked" on the reads sheet) can be built on sealed bytes; each needs a plain copy, and each plain copy needs its own forget path (the rig constraint "every index over values is rebuildable from the log or purgeable by value id" is a cost, not a solution). Lands when reads are built. Not frozen: a PState migration can turn sealed rows plain, but only by a one-time pass that opens every value with its lock, and only before any lock is destroyed.

**What would settle it.** One question to Sid, with both costs written: must a value stay sealed in the record, or only in the log? If the record is plain: forget is two writes, destroy the lock (the log's copy dies) and remove the row as a recorded fact; reads and proxies carry plain values; indexes are ordinary. If the record stays sealed: forget is one write; every read pays, and finding 1's proxy road carries sealed bytes only.

### 3. "Only our own programs connect to Rama" is false as a statement about Rama; it is a firewall rule. The permission check as built is a claim check

**What and where.** ARCHITECTURE-STE100:386 "The runtime is the only Rama client. No other program connects to the module. [DERIVED]"; W3 "Rama can keep a table from every program outside the store"; today's main-session reply «acf335fb» "The browser never reaches Rama."

**The check.** reference/rama/docs/24-rest-api.md: "Rama has a built-in REST API for doing depot appends, PState queries, and query topology invokes with HTTP requests. This allow Rama modules to be read and written to from any programming language, as well as allowing ad-hoc queries to be done with tools like curl." It is served by every Supervisor and by the Conductor on the Cluster UI port (default 8888). Its limitations (lines 335-341): JSON types, no custom navigators, "PState reactivity is not exposed yet". No word of authentication anywhere in the local docs (grep of 24-rest-api.md and 19-operating-rama.md for auth/token/secur: nothing). 26-all-configs.md has no config with "rest" in it, so I found no switch that turns it off (absence in the local docs, not proof). 07-terminology.md: "foreign" is any work initiated from outside modules. Correction 7 of STE100: a depot cannot be made private. So "a program that reaches Rama" is anything that can open an HTTP connection to a Supervisor's port; a browser can append an act with curl's body; only a proxy needs a JVM client.

**The consequence for the gate.** decision.clj `standing` (lines 136-153): the act's permission stands when the row at the holder's place holds that permission and `(= (:who act) (nth permission 0))`. A permission is a fact under a store key, so it is plain in the record and in the depot (A3: "A fact under a store key keeps a plain value and has no lock"). Whoever can append can append as anyone, naming a permission read from the record. The gate is the authority over form and consistency; over identity it checks a claim. The store README says so in its own words ("`:who` is a claim… The gate cannot tell a person from a session or an agent") but the sheets say "the gate is the authority" (F8 C's reason: "anything can append to a depot without the door"), which is the same fact read as a strength.

**Whose.** A session's derivation (DERIVED). Sid 23 Sept ‹1225ee72›: "at the write time we are just checking if this user is authorized to write this data in some place and … this calculation of is it allowed or not we have a Rama related module or something for it" — he places authorization at the gate. Today ‹55c1334e›: "What is anything???? How can it connect??? You mean like api access??" The answer to his question is yes.

**What is lost if wrong, and when.** The architecture's security is the network boundary around the Supervisors, not anything in the store. Lands the day a Supervisor port is reachable beyond localhost (production, two nodes, F9a punted). Not irreversible, but the rulings' "No signing; a later edition can add the part" means a signed act is a form change on a kept log.

**What would settle it.** Sid says whether identity is the runtime's and the network's job (then the architecture says so plainly and stops calling the gate the authority over who), or whether an act must carry something the gate can verify (a signature under the person's key), which is an edition of the form.

### 4. The record keeps no provenance: no who, no session, no subjects, no based-on; only an act uuid, and the depot has no index by uuid

**What and where.** rows.clj: an answer row is `{:refused :stamp :digest}`; a fact row `{:value :lock-act :lock-index :by-act :by-index :stamp}`. grep for `:who`, `:session`, `:subjects` in record/rows.clj and record/tables.clj: nothing. `:based-on` appears in the gates only for the 10-second check (decision.clj:84). Rama 14-depots.md:234-242: a depot record is found by partition index and offset, read in ranges; there is no lookup by content. So "who wrote this fact" = find the act by uuid in a log that has no uuid index = a scan.

**Whose.** The builder's ("The record: three flat PStates", an engineering choice with its reason: 4 seeks against 6 to 8). F2 A, the record in PStates, is Sid's. What the record carries was asked only for the stamp (H2).

**Flows that meet it.** Sid 17 Sept ‹f2468e83›: "your comments are your comments they are always marked so when they enter anyone else's land" — the mark is the who, and it is not in the record. W8 "who read me / the reverse lookup" (Sid 24 Sept) — based-on is in the depot only. Forgetting a person (finding 11) needs subjects → values. The vision's "show me everything people have experienced around this type" needs who and session. Each is a depot scan or a new index, and a new index must be built from the depot, which finding 5 says does not reproduce the record.

**What is lost if wrong, and when.** Nothing at the first record; at scale, every provenance question is O(history). Reversible by adding PStates the gate writes (it has the whole act in hand), against the F9 targets, which pushed toward fewer writes.

**What would settle it.** Sid says which parts of the act the record must answer directly (who, session, subjects, based-on), with the write cost beside each.

### 5. F2 A's premise, "recomputable from the acts depot", is contradicted by the build's own rules 9 and 19, and the contradiction never reached him as a question

**What and where.** src-store/README.md, 7 Oct entry 2: "His reason: the record is recomputable from the acts depot… The acts depots are the replay log." Store README, rule 19: "Nothing is built for a rebuild of the record from the depot. What a rebuild would not give again: a lock's bytes, which are in no depot; a stamp…; a refusal by rule 17; the order across a failed batch; and, in either record, which of the two words an act sent to the wrong depot got…" Rule 9: "Found: no, not when the two acts are on two depot partitions… a record of each batch's cut and clock, a second log beside the depot. Nothing is built for it; whether it is wanted is Sid's."

**Whose.** The reason is his, typed: ‹27882354› 7 Oct "f2 - A, note to self- it is recomputable given the version a act was proposed at it will be rejected every time so storing it a new depot doesn't make sense" (the phrase "recomputable from the acts depot" is a session's, «6631e4be»). The finding that it does not hold is the build's, left in an engineering note ("whether it is wanted is Sid's"), on no sheet.

**What is lost if wrong, and when.** Everything that assumes replay reproduces the record: going past N tasks (STE100 §3.3.8 "a topology copies the PStates into a new module"), index rebuilds, time travel (finding 6b), recovery. The sharpest edge: a lock's bytes exist only in the locks PState; the depot holds sealed bytes. A lost PState partition is the loss of every value it sealed, for good. Replication is the only guard, and two nodes are punted (F9a). Lands at the first disk loss or first rebuild; irreversible then.

**What would settle it.** Ask Sid whether the replay-log promise is wanted. If yes, what is logged beside the depot (lock bytes, stamps, batch cuts) — which is the second log he declined. If no, the README's "The acts depots are the replay log" is struck and backups become a decision.

### 6. A replaced value in the base is "absent" for about a run (~0.9 s) on every screen; a hot place is never readable

**What and where.** Store README, "Closing": "a place holds one row, its current fact. So after a batch replaces a fact, its place holds no row of a closed batch until that batch is closed, and a place that is replaced in every batch never holds one. No read of this build meets it." Reads sheet, "Listed, not asked": "A place of the base, just after a change… has no row that a read may give. The build found this; it is the builder's."

**Whose.** The builder's. Not asked.

**What is lost if wrong, and when.** Under "show the committed result, no optimism" (Sid's, LOG.md:555) a value that changes in the base blanks on every subscriber's screen for a run, then returns changed; a place written every batch never shows. Lands when fact reads are built. Reversible in code (keep the last closed row beside the open one), but it is the shape of the facts table and the closing rule together, not a bug in one.

**What would settle it.** One yes from Sid: a read never gives "absent" for a place that had a value. Then the row shape or the closing rule changes before reads are built.

### 6b. Looking back needs the record's stamps, which only the PStates hold, and only the current fact is kept

**What and where.** Facts table holds the current fact of a place (rows.clj). Reads sheet, listed: "Looking back: 'as of a day'… the past can be built from the two later, but for forgotten values. No code for that is built." Rulings (session's words): "a read as of a moment shows nothing admitted after it, except an erasure, which shows only its date." Rule 19: a rebuild gives other stamps. A lock's row holds no stamp, and no forget is built, so "erased on this date" has no date to show.

**Whose.** Two deletes, retract and forget, are the rulings file's words; Sid's own line of 23 Sept ‹73d9f8c2› is a question: "would they be able to watch their past data and would they be able to recover it … I'm not sure what does delete even mean." No line of his takes the two-deletes answer up.

**What is lost, and when.** "As of stamp S" is answerable only from PStates that hold only "now"; a rebuilt past has different stamps. Recovery of a retracted thing is a depot scan. Lands when time travel is built; what is irreversible is finding 5's.

**What would settle it.** Sid says what looking back must answer (a value as of a moment; who changed it; recover it) so the record can be shaped for it, rather than "later, from the two".

### 7. "Each read is an act in the hand layer, shown only after the gate accepts it" is marked RULED in the architecture file; the reads sheet says it was a session's proposal, not ruled

**What and where.** ARCHITECTURE-STE100 §3.2.6 step 3 "[RULED: ruling 3 + RIG]"; rule 5.14. Reads sheet W7: "A session's proposal of 25 September, not ruled: 'Nothing is shown before its entry has been acknowledged.'" Rulings file ruling 3 is about the read entry's form and says nothing about showing after. W6 names "always" as not his word.

**Whose.** A session's, carried as RULED in the architecture file for a week. Sid 24 Sept ‹f68cf1a3›, ‹aab30856›: "If we do every write for every read … too much data"; "I don't have to store everything".

**What is lost if wrong, and when.** If built as the architecture file says: a read entry is a fact with a value (the list of what was shown), so it is sealed like any value and needs a lock from the stock; each screen read then costs a lock, an act, a gate decision (4 reads, 4 writes) before anything is shown. The typing path doubles. Lands when reads are built.

**What would settle it.** W6 and W7 as asked, and the architecture file's mark corrected from RULED to a session's.

### 8. A layer is one thread forever, or batch latency, with nothing between; his own example sits at the measured ceiling

**What and where.** ARCHITECTURE-STE100 §6.1: one thread; a busy stream layer slows its neighbours and every microbatch layer; re-class is the only move, "a batch of delay to each act of the members" and two eras for life. Measured (rig, one machine): 226 acts/s with one writer closed loop, 1,062 at 32 writers, p99 236 ms at 128 writers. F4 C: every layer but the base starts streaming. Sid ‹12fed86b› 2 Oct: "a shared canvas/roam research graph with my 10 agents… the 'own work feels instant' is still needed." carry-on §7: "a model streaming far more text than a person types"; rulings: "A stream is many small acts."

**Whose.** One order per layer is the rulings file's (session's write-up of 24 Sept, rulings 1-2), reframed as switch 3, which Sid agreed on 2 Oct. He asked ‹5de0a3d3› "what are we loosing with streaming?" and was told («d64246d5»): "You and ten agents fit on one task with room to spare." That is an assurance, not arithmetic: ten agents streaming tokens as acts are hundreds of acts a second before lock requests, on one thread that also answers the layer's reads.

**What is lost if wrong, and when.** A live group canvas either saturates one thread or re-classes to batch latency, which is the thing he said must not happen. Lands at the first group with agents writing. Rama itself allows a stream topology partitioned by entity; it is the one-order-per-layer promise that forces one task.

**What would settle it.** Measure ten writers at token rate on one task with locks on. Ask Sid whether a layer may keep per-entity order and stay streaming, or whether one order per layer is the thing he wants.

### 9. The grain of a value is the tool's, but the floor cost per value is fixed, and the promised per-act switch is not built

**What and where.** README 9 Oct entry 2: "The switch that moves a layer to one lock for each act (ruling 7) is not built; it is built when a bulk layer asks for it." Door sheet H1: "a lock's rows are about 140 bytes for each value in a streaming layer and about 245 in the base… more than a short note itself." Plus 28 bytes of seal per value and a lock-request act per 256 values. Sid ‹85a64cfb› 9 Oct: how often a tool writes "is a spectrum, and that is a choice that layers and tools and groups can make"; LOG.md:698 "per letter (yeah need it for the snappy feedback)". K12 open.

**Whose.** H1 A is his, with the cost stated as unmeasured. That the other end of the spectrum exists is the rulings file's promise; the build did not make it.

**What is lost if wrong, and when.** Per-letter editors, token streams, repo ingest (100k lines = 100k lock rows) pay the per-value floor with no switch to flip. Lands at the first bulk writer or first streaming model reply. Values written before the switch keep their per-value locks.

**What would settle it.** Build the per-act switch before the first bulk layer, or Sid accepts the floor for now in so many words.

### 10. "Publishes to the world" has two readings, copy or open, with opposite consequences for forget; nobody asked which

**What and where.** Sid ‹1cad46cd› 2 Oct: "world is the open group that every group if make open their data publishes to". K14 open ("whether the world is the base"). Rulings on promotion: the copy "is about whoever the target's grammar and tool name, not its former owner"; "a forget after the read-out does not remove the copy." W1: Sid ‹3c01af46› 9 Oct "The owner of the layer decides they can say on the group members or everyone… is this option C???" and ‹54e1af3a› "the softland system is providing the full spectrum".

**Whose.** Both readings are sessions'. His words say "open" and "publishes". The sessions read it as promotion into the base.

**What is lost if wrong, and when.** If publishing copies, a person's forget stops at the group's edge; if it opens, forget reaches the world. The sovereignty he cares about (carry-on §9) turns on this. Lands at the first promotion into the base; copies made before the answer are irreversible by the ruled rule.

**What would settle it.** One question: when a group publishes to the world, is its data copied into the base or opened where it is?

### 11. Forgetting a person has no mechanism: wrapping under subjects' locks is "next level, not this build", subjects are not in the record, and the person's UUID stays plain in every act forever

**What and where.** «2d4d6464» (7 Oct, to Sid): "'Forget Alice' in one stroke is the next level, not this build. The rulings file plans to wrap each value's lock under a lock for each person…" rows.clj: no `:subjects`, no `:who`. The depot is never trimmed (rule 4). ?4 A: a person's id is a UUID that "says nothing", joined by erasable links. K10 open: nothing plain made from the person.

**Whose.** ?4 A is his (9 Oct). "Says nothing" was about the id's form (not Google's number). That the UUID itself is a plain join key across the whole log in `:who` and `:subjects`, and stays after every link is erased, was not put to him.

**What is lost if wrong, and when.** "Forget Alice" today = scan the whole depot for her UUID, destroy each lock found; her acts stay linkable to each other by the plain UUID. That is pseudonymity, not erasure. Lands at the first forget of a person; irreversible for everything already in the depot.

**What would settle it.** Sid says what forgetting a person means: unreadability of her values (what the locks give), or unlinkability of her acts (which needs the UUID itself under a lock, or a per-act pseudonym). Then H11, K10, K11 follow.

### 12. "Freezes at the first kept record" is overstated pressure; the act carries :version 1 for this; Rama migrates PStates and depots; what actually freezes was never listed

**What and where.** Reads sheet word "freezes"; record sheet "frozen at the first kept record"; A3 "Part 1 is frozen at the first kept record"; door H1 "Frozen at the first kept record." Sid's K4 line ‹a3dfb1b8› 9 Oct: "I have to decide it right now because then we would be exporting the thinking process…" Decision 4 (6 Oct): "The version is the edition of this list of parts, and the code knows 1." Sid 23 Sept: "'Never rewritten' is about meaning, not bytes. Re-encoding is allowed." Rama skill references/pstate-migration.md: migrations apply on read, do not delay the update; depot-migration.md: records can be migrated or excised.

**Whose.** Sessions'. It set the tempo of several answers.

**What is lost if wrong, and when.** Decisions taken under urgency that was partly a fiction (K4, H1, A3), and the inverse: the set that does freeze (the plain parts of every act in the depot: who, subjects, layer ids, keys, based-on) was never named as the freezing set. The tempo already landed; the real freeze lands at the first kept record.

**What would settle it.** A one-page list: what freezes (depot bytes and their meaning per :version) and what migrates (every PState shape), so the next sheets say "freezes" only where it is true.

### 13. The session is required on every act, checked nowhere, recorded nowhere, and no longer the permission root

**What and where.** act.clj:147 `:session` required (decision 4, his). declaration.clj:338: only its length is checked. Not in any row (rows.clj). E3·H7 B (8 Oct): the top of a chain is a person, never a session, which overturns the rulings file's "the session is the root." Read entries go "in the hand layer of the reader" (STE100 §3.2.6), so a session's hand layer is a layer, made by a making act with a permission and a stream partition. "When a session layer closes (82)" is open.

**Whose.** Required-ness is his. Its purpose after E3·H7 B is nobody's.

**What is lost if wrong, and when.** Either a layer per sign-in (layers multiply with sittings, each a making act, a permission row on its task, a stock of locks) or a required id that means nothing. Lands when sign-in is built.

**What would settle it.** What a session is for now, and whether a session is a layer.

### 14. A grammar copy per layer (F5 A) meets his "50 layers of tools… answer it each time" (K4), and no one said how a copy arrives or is kept current

**What and where.** F5 A, 7 Oct: "Each layer holds a copy of each grammar it uses." Sid ‹32f65085›: "f5::: copy .. i agree with your logic here." Sid 9 Oct ‹a3dfb1b8›: "if someone is building like 50 layers of different tools on top of each other then they would have to answer it each time right." K13 open (how a person's tool becomes the system's); W2 unanswered (how a layer names what it builds on).

**Whose.** F5 A is his, on the session's cost claim "a copy is bytes and a rare write." A tool's grammar change is one write per layer that uses it, by someone with a permission there, and nothing says who or when.

**What is lost if wrong, and when.** Tool evolution scales with adopting layers; stale copies across layers with no rule for skew. Lands when the first tool is used from two layers.

**What would settle it.** How a copy arrives in a layer and how it is updated (an act per layer, by whom), beside W2.

### 15. Commenting on another's paper from my own layer has no path in the decisions

**What and where.** Sid 17 Sept ‹f2468e83›: "you can comment on any paper… your comments are your comments they are always marked so when they enter anyone else's land it's read kind of thing so Alice will just read as Bob has commented." An act writes one layer (ruled). W2: the nearest layer wins, so a fact in my layer at their entity and key overrides theirs in my view; a comment is additive, a new entity with a link to theirs, and nothing in the model tells an override from an annotation. For them to read my comment they must read through my layer (W1 default, a session's: a person's own layer is private) or follow a link backwards ("Finding things by a pattern, and following a link backwards" is listed, not asked, "its tables can be built later"). The mark (who) is not in the record (finding 4).

**Whose.** His flow; the model's pieces are sessions'.

**What is lost, and when.** The everyday interaction of §4 of carry-on has no road: Bob never sees Alice's comment unless she promotes it into his layer, which the rulings make a copy about "whoever the target's grammar names". Lands when the first two people use it.

**What would settle it.** Walk this one flow with Sid on a sheet: a fact in my layer about your entity; who sees it, how, and what distinguishes it from my overriding your fact.

### 16. Smaller, on the typing path and at two nodes

- **The door polls for an answer at 200 ms** (stock.clj `:read-every-ms 200`; door.clj `sleep-for`). After a streaming append at ack level `:ack` the first read finds the answer, so this costs only when the first read misses (the base, an ack error): up to 200 ms on top of 0.9 s. Builder's; fixable in a line; flagged because it sits on "committed result on screen".
- **The 10-second rule with per-task clocks** (decision.clj:84, rule 17): an act standing on a fact from a task whose clock is more than 10 s ahead is refused. A session's number. Lands at two nodes with skew (F9a punted).
- **Lock requests are acts under the maker's permission in the maker's layer** (BUILD-door item 3). A first value in a new layer is three round trips (making act, lock request, value act); in the base each is a batch (~0.9 s), so a first promotion into the base is seconds. Builder's, by decision 3's consequence; fine for the base by F4 C's reasoning; worth saying out loud.

## What nobody asked

- What "a program that reaches Rama" is: that Rama serves HTTP from every Supervisor with no authentication (finding 3).
- Whether a value must stay sealed in the record or only in the log (finding 2).
- Whether "one exit" is a Rama mechanism or one place in our code (finding 1).
- What the record must carry beyond the stamp: who, session, subjects, based-on (finding 4).
- Whether the replay-log premise under F2 A still holds after rules 9 and 19 (finding 5).
- What a screen gets for a replaced base value during the closing run (finding 6).
- What looking back must answer, and from where (finding 6b).
- Whether a read entry must be accepted before anything is shown; the architecture file says RULED (finding 7).
- The arithmetic of his own ten-agent canvas on one thread (finding 8).
- Whether the per-act lock switch is built before the first bulk writer (finding 9).
- Whether "publishes to the world" copies or opens (finding 10).
- Whether forgetting a person means unreadability or unlinkability, given a plain UUID in every act forever (finding 11).
- What actually freezes at the first kept record, given :version on every act and Rama's migrations (finding 12).
- What a session is for after E3·H7 B, and whether a session is a layer (finding 13).
- How a grammar copy arrives in a layer and is kept current (finding 14).
- How a comment in my layer on your paper reaches you, and what tells it from an override (finding 15).
- Who holds the locks on the way out. The 7 Oct answer "the store, and only the store" was given before the door's stock and before any read was designed; no sheet has asked it since.

## For another part

- Decisions page: "His reason: the record is recomputable from the acts depot" is his note to self ‹27882354› in a session's wording; the page should quote him and then say rules 9 and 19 contradict the premise.
- Architecture files: §3.2.6 step 3 and rule 5.14 carry RULED for a session's proposal (finding 7); §3.2.1 "the session is the root" is overturned by E3·H7 B and still stands in the file.
- Build rulings: "Closing" (finding 6), rule 19 (finding 5), the 10-second number, `:read-every-ms`, the record's three flat PStates with no provenance (finding 4) are builder's rulings with consequences for what a person sees or can ask.
- Rama/numbers: the REST API (24-rest-api.md) against "no other program connects"; the measured 226/1,062 acts per second against the ten-agent example.
