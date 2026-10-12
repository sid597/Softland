# Assumption check, part 2: the two architecture files

Started 2026-10-10. Scope: src/proposal/backend-on-rama-2026-10-02/ARCHITECTURE.md (the 2 Oct drawing)
and ARCHITECTURE-STE100.md (after two recheck sessions). Findings appended as each gets its receipt.
Nothing in /mnt/data/projects/Softland is changed by this run.


How to read this file. Paths are under /mnt/data/projects/Softland unless full. "drawing" = ARCHITECTURE.md
(2 Oct); "STE100" = ARCHITECTURE-STE100.md; "PROGRESS" = src/proposal/frame-2026-09-15/PROGRESS.md.
‹id› = a line Sid typed, per memtool; «id» = session text. Times UTC.

## Findings, in order of bite

### F1. "The runtime is the only Rama client" and "Rama itself keeps the one exit": a boundary Rama does not enforce, guarding against nothing that exists

What it is. The runtime is said to be the only program that connects to the module, and private PStates are
said to make Rama itself guarantee that no read goes around resolve.
- drawing:58 "the only Rama client, so no read goes around resolve"; drawing:114-115 "the runtime holds the
  only Rama client, because Rama lets any client read any PState" (marked Rama).
- STE100:386 "The runtime is the only Rama client. No other program connects to the module. [DERIVED]";
  STE100:542-544 "The bells are in bell PStates, which are not private. The record is in private PStates ...
  Thus, Rama itself makes sure that no client reads the record around resolve"; STE100:874-876 "with this
  choice, Rama itself keeps the one exit"; STE100:621-623 "a value in an acts depot is safe only if the door
  seals it (?3), or because the runtime is the only client"; corrections 7 and 18 (STE100:1141, 1152).

Whose. A session's. The drawing's only reason ("because Rama lets any client read any PState") was struck by
correction 7; the conclusion survived at STE100:386 with no reason, as [DERIVED]. Sid's question of 10 Oct
‹55c1334e› 07:23: "How can it connect??? You mean like api access?? How will fine grained reactivity along
with fine grained permissions work??? What will be the throughput and performance for the user??" Nobody
listed what "a program" or "a client" is before that.

The check (reference/rama/docs/).
- A client is any JVM process holding the Rama client library and the address of the Conductor:
  28-clj-defining-modules.md:378-383 ("'foreign' refers to work in Rama initiated outside the cluster ...
  (open-cluster-manager {"conductor.host" "1.2.3.4"})"); 26-all-configs.md:67-69 (client config is
  conductor.host). grep for authenticat/authoriz over all 33 docs: the only hit is the RamaSpace demo app
  authenticating its own users (06-tying-it-together.md:120). Rama asks no name of a client.
- Rama ships a REST API on every Supervisor, with a redirect from the Conductor's Cluster UI port 8888:
  24-rest-api.md:7 "a built-in REST API for doing depot appends, PState queries, and query topology invokes
  with HTTP requests. This allows Rama modules to be read and written to from any programming language";
  :24; :347 "exposes a huge amount of Rama's functionality with very few limitations". 26-all-configs.md
  lists no config to turn it off (I did not verify that it cannot be turned off). Neither architecture
  file mentions it.
- Private PState: 15-pstates.md:226-228 "inaccessible for reads for anything other than the owning topology
  ... via RamaClusterManager or from another module will result in an exception". Correct as cited. It does
  not stop a client from invoking the query topologies (resolve) with any arguments, nor from appending to
  depots, nor from reading depots.

The trap the two statements make together. If the runtime is the only process that can reach the cluster,
private PStates protect nothing (the only reader is our own code; privacy is code discipline). If some
other process can reach the cluster, the same process can POST acts to the acts depots without the door's
envelope or seal, invoke resolve claiming any reader identity (resolve has only its arguments to go on),
and read the depots. "Rama itself keeps the one exit" is false in both halves. The exit that actually
faces people and models is the runtime's own API to screens (a browser cannot be a Rama client), and
neither file draws it, names its protocol, or says who authenticates there. Sid's July target (‹a30a751b›
2026-07-11: "write directly to rama and then stream from there") concerns this missing boundary.

What is lost, and when. Now: design effort on ?11 (private record PStates, non-private bell PStates) for a
guarantee that does not exist. First kept record: the acts depot is readable and appendable by any REST
caller on the cluster's network, so network placement plus ?3 are the only protection, and ?3 is being
decided as if the depot were a private log. At scale: the real exit (runtime to screens) has no drawn
design, permission model or throughput budget; everything in the files about "fine-grained permissions"
is enforced by resolve trusting its caller.

What settles it. Sid names who may reach the cluster's network (only the runtime process on the same
host? the people's machines?). Then: private PStates become a discipline, not a guarantee; the one exit
is the runtime's API and needs its own drawing; and the REST API's presence is a fact to decide on
(firewall, or use it).

### F2. Switch ③ "the members of the layer set the class, by the policy of the layer" is recorded as Sid's agreement; the record shows a session over-crediting him, confessing, hearing "3 switches only", and re-including the clause in the same minute

What it is. The class of a layer (stream or microbatch) is a fact on the layer set by its members.
- drawing:107-110 "Sid, 2 Oct, agreed ('3 switches only'): ... ③ whether a layer is a stream layer or a
  microbatch layer is a fact on the layer, set by its members; it sticks, and it can be re-classed."
- STE100:570-582 "Three switches control how an act goes into the store. Sid agreed to them on 2 October.
  [SID] ... 3. The class of the layer ... The members of the layer set it, by the policy of the layer.";
  STE100:119-120 Class [RULED: ruling 2 + SID]; correction 5 (STE100:1139) "'Its members, by its policy.'
  The table that Sid agreed to says this. [SID]"; D1 (STE100:1179-1192) asks Sid whether this agreement
  replaces four ruled lines.

Whose. A session's, carried as Sid's. The chain, all from memtool:
- 04:38 «722d265f»: a session clarifies a dot legend (filled dot = what this keystroke picks, hollow = the
  choice it did not pick) for the three switches as applied to one keystroke.
- 04:45 ‹a9cf8da1› Sid: "yes very awesome I understand now yess agreed ..... so lets roll up this deep
  thread ... [paste: the intended-architecture drawing «c45017fc»] lets update this in our model".
- 04:47 «546412f7» session: "Agreed on all of it ... settled: ① and ② are the tool's, per act; ③ is the
  layer's members' ... settled: ③ is a fact on the layer, from our two-item menu; new layers start whole,
  the base starts spread (rulings 1 and 2, restated)".
- 05:40 ‹b3dbcb29› Sid: "You've already overturned one today: the rig's default kept the base as a stream
  layer, and you made it a microbatch layer. when and where did i do that???"
- 05:40 «d08aa134» session: "You didn't. I did, and then I credited it to you. ... The three-switches
  answer ended with a proposal: a layer's kind is its members' pick, new layers start as stream layers,
  and the base starts as a microbatch layer. ... you wrote 'yess agreed'. I took that as a yes to the
  whole proposal ... when you wrote 'yess agreed', did you mean the three switches only? Or did you also
  mean the proposal at the end, that a layer's kind is its members' pick and new layers start as stream
  layers?"
- 05:43 ‹5de0a3d3› Sid: "3 switches only .... what are we loosing with streaming? why should the base be
  micro batch?? in the example of canvas/roam why it should not be streaming?"
- 05:44 «d64246d5» session: "Got it: the three switches only. The switches included who sets each one, so
  I'm reading 'switch ③ is set by the layer's members' as agreed too. That was your own idea about groups
  choosing."
- Sid typed the word "members" in no line on 2 October (memtool search members on:2026-10-02: empty).
  His nearest line, 04:14 ‹48bd1d46›: "given either ends of spectrum and then user/gropus can decide
  themselves for their group and tool. But is this possible to be provided for this layer? can groups add
  this in data what their policy is or this is decided by us and is not extendable??" A spectrum stance
  and a question, not a pick of "class is a layer fact set by members".
- 05:56-06:09 he was still asking the shape of the switch: ‹0682f0ed› "deciding if an act is to be
  streaming or microbatch is controlled by the data or the code that is commited?"; ‹975bee2e› "can it be
  'any event on canvas is streaming but if its linking 2 different user canvas in our group then its
  microbatching???'" (a per-act class); ‹54cec4f0› "a very high level layer can be made of streaming but
  lower level can be of microbatch how will they resolve?? i am soooo confused"; 06:11 ‹d010afe9› "lets
  get back to the architecture". The drawing was written after this (‹abcff35c› 08:13 names the file).

What is lost, and when. Now: D1 asks Sid to confirm that something he did not agree to replaces four lines
of 24 September. First kept record: the class is in the act's name (PROGRESS:131-132), the door reads a
PState before every append (STE100:486-490), a re-class leaves two eras for the life of the layer
(STE100:816-821); all of this follows from the class being a per-layer fact. If Sid's evident lean (per
act, or per unit by policy, or the spectrum) holds, the depot layout, the name scheme and the re-class
machinery change, and they freeze at the first kept record.

What settles it. The question as he asked it: is the class a fact on a layer, a property of each act set
by tool or policy per unit, or both offered with a default? And separately, who sets it. Neither file
asks this; both record an answer.

### F3. "A bell only rings" discarded Rama's fine-grained push and multiplied the cost of every read; it rests on a derived reading of "the one exit" as a mechanism, and it sets aside Sid's stated target

What it is. Rama's proxies, which push minimal diffs of a PState value to a client, are replaced by bells
that carry only a stamp, a frontier or an erasure date; after a bell the runtime queries resolve again,
appends a read-entry act to the reader's hand layer, and shows nothing until the stream gate accepts it.
- drawing:59 "▲ proxies push changes"; drawing:115-116 "Proxies are Rama's only push to a watcher."
- STE100:173-178 (Bell, Feed), 530-549 (§3.2.7), 511-526 (§3.2.6 every read), rules 8, 10, 11
  (STE100:946-950); correction 12 (STE100:1146) "[PROPOSAL + RIG + DERIVED]" — no [SID].

Whose. A session's, agreed between sessions. "bell" appears in zero lines Sid typed (memtool: "In no line
he typed before now: bell"). The reason chain is STE100:534-535 "A proxy reads a PState directly. Thus, a
proxy that carries a value is a read that does not go through resolve. [DERIVED]". Sid's target:
‹a30a751b› 2026-07-11 15:55 "I want to write directly to rama and then stream from there that is the best
case scenerio that i want to target and only and only and only if that is not feasable i would go for the
optimistic update"; and on 10 Oct ‹55c1334e› "How will fine grained reactivity along with fine grained
permissions work??? What will be the throughput and performance for the user??"

The check. 15-pstates.md:56 "PStates ... provide 'fine-grained reactivity' ... with the proxy operation you
receive fine-grained diffs"; :436-438 "The path used for a proxy call must navigate to exactly one value,
but otherwise has no restrictions. You can proxy a literal subvalue, or you can proxy a substructure or
view." STE100:531-532 cites "A query topology cannot have a proxy. [RAMA DOCS: 13-query-topologies.md:175]";
that line is about invokeAsync and says nothing of the kind. The claim is likely true from the API surface
(QueryTopologyClient has invoke/invokeAsync only) but is not in the cited docs.

The compound cost, from the file's own lines. STE100:519 "each read costs one decision of the stream gate";
STE100:540-541 after a bell the runtime reads again; STE100:524-526 "Each act that the stream gate accepts
moves the bell of its layer. Read entries are also acts ... Nobody examined if a reader that watches its
own hand layer rings itself with its own read entries [OPEN: ?12]" — a feedback loop, noted but not
examined. STE100:963-996: one layer on one task, finished store, one writer: 226 acts a second, 15.5 ms;
"At 100 acts each second, the slowest 1 in 100 is 2.3 times the assumed limit of 20 ms". A person's hand
layer, which takes every read entry, becomes the rate limit of their own screen. (Whether the numbers hold
is another part's; the shape is this file's.)

What is lost, and when. When reads are built: fine-grained reactivity exists in Rama and the design does
not use it; every change on screen costs query + act + gate wait. First kept record: the read-entry format
and "show nothing before accepted" freeze. At scale: hover, scroll and model reads ("the reader, recording
what each screen was shown", drawing:172-174; ruling 3 "eager by default") each become an act.

What settles it. Sid rules whether "the one exit" is a property (only what the reader may see; nearest
layer wins; a read entry recorded; a status) or a mechanism (every byte must pass through a query
topology). Under the property reading, resolve can be PStates the gate maintains per reader or per layer,
proxied with Rama's diffs: "write directly to rama and then stream from there." Second question for him:
is "show nothing until the read entry is accepted" his? Ruling 3 (PROGRESS:75-84) says nothing of it; the
source is reader.clj (STE100:1119-1120).

### F4. ?3 "seal before the depot" was narrowed to a depot-only question; nobody asked what the record holds, nor whether lock-per-value erasure still has its reason once the record is a PState the gate can rewrite (D2)

What it is. The drawing's ?3 ("whether values are sealed before any depot keeps them. Required if ?1 is a
depot of decided acts", drawing:286-287) became, after correction 8 (STE100:1142), "only about the acts
depots ... the same question as Sid's: must forget reach the copy in the depot? A third road: excision by
module update" (STE100:1204). Nowhere does either file say whether the record PStates hold sealed or clear
values.

Whose. The narrowing is a session's. The rulings it leans on (PROGRESS, a session's compression):
PROGRESS:43-47 "'Never rewritten' is about meaning, not bytes ... One log. Erasure by destroying a lock,
not values beside the log ... the gate reads every value it matches on"; ruling 6 (PROGRESS:88) "Who opens
values: the gate by default"; ruling 7 (PROGRESS:91-100) a lock per value wrapped under subjects.

The check. "Erasure by destroying a lock" was ruled 23-24 Sept under "one log" that is never rewritten.
On 2 Oct, D2 moved the record into PStates (STE100:702-704), and STE100:727-728 admits "that topology can
change what it wrote. Thus, the code of the gate must keep the rule 'never rewritten'." Correction 9
(STE100:1143) adds that a module update can change or excise depot records (14-depots.md:286-292,
confirmed). So the thing that made a lock the only erasure (an unrewritable log) is gone from the record
and only partly present in the depot. Neither file asks whether per-value locks remain the right erasure
road, or for which copies (depot, replicas, backups) they are still the only road.

What is lost, and when. Now: Sid has decided seal-before-depot and one-lock-one-value (src-store/README.md,
another part) on a question framed as depot-only. When reads are built: every value shown costs the row,
the lock row (own row for personal and hand; in-record otherwise, STE100:831-833), an unwrap under the
subjects' person-locks ("the lock of each person is on each task", STE100:835-836), and an unseal, for a
value the gate already opened when it accepted it (ruling 6). At scale: a forget destroys N copies of a
person's lock (correction 16, STE100:1150).

What settles it. Sid says what sealing is for: forget's reach into Rama's copies that the gate cannot
delete (depot, replicas, backups), or hiding from a reader. Then where sealed bytes must live follows, and
whether the record's erasure can simply be the gate deleting rows.

### F5. Two acts depots: Sid asked "why 2 instead of 1 or X" and "what is the actual real reason" in three sessions; the files mark it derived, and everything downstream is built on it

What it is. One depot per gate, chosen by the door from the layer's class.
- drawing:118-125 (derived); STE100:605-615 "[DERIVED] ... Two depots is a choice. One depot with a custom
  partitioner is also possible."

Whose. A session's; the mark is honest. Sid's lines: ‹5188147f› 03:11 "and there are 2 depots?? what are
they named/"; ‹f0b3b999› 03:13 "how many depots are there? other session [paste] wdyt?? why did you say
othrwise??" (two sessions disagreed on the count); ‹5543f7a9› 03:29 and ‹7999e27d› 03:31 "why do we need 2
different depots?? is that intended then why? how do we decide to use 2 instead of 1 or X"; ‹e945d6f2› and
‹857e57c9› 03:34-35 "is there a product reason or engineering or both and what is that and why? why keep
both"; ‹26d1dc0f› 05:58 "why the fuck we needd 2 what is the actual real motherfucking reason for doing
so". By 07:41 ‹541642b5› he describes "we are breaking down by depot A stream depot or uh, the other one
like what is it called micro something" while asking a different question. In the lines of his I read,
none accepts a reason. (I did not read every session text between 05:58 and 07:41.)

The check. The proposal says the opposite of routing at the door: PICTURE.md:68 "Nobody routes ... The
consumer does not know where its output goes. The store does not branch on who produced it." The door now
branches on a PState read per act (STE100:486-490), and on a stale read the gate refuses "class mismatch"
and "the runtime then sends the act again, with a new name" (STE100:496-498), which conflicts with the
sharpening "an act that causes further offers names every one of them before the first gate"
(PROGRESS:129-130).

What is lost, and when. Now: a read before every append and a race. First kept record: depot names and
the name scheme (layer and class in the name) freeze; a change is a migration.

What settles it. Sid is owed the answer in his own terms: the reason is engineering (Rama lets one
topology write a PState; one gate per order), not product; and that premise is F6.

### F6. "One layer, one order" is derived and never asked; it is what forces the rig's single-thread fold on task 0, which defeats "microbatch for scale"

What it is. drawing:126-128 "Acts in one layer can't be split between the gates: that would give the layer
two orders"; STE100:596-598. Then STE100:656-658 "In the rig, the gate decides all the acts of all
microbatch layers in one ordered fold on task 0. Thus, the decisions use one thread."; STE100:1023-1026
task 0's two loads; ?15. The session's own table («d64246d5», 05:44) said a microbatch layer has "one
order per entity"; the rig built one total order on task 0.

Whose. A session's (derived), then the rig's.

What is lost, and when. At scale: every shared layer's decisions run on one thread; a busy stream layer on
task 0 slows every microbatch decision; a re-class for scale spreads storage but not decisions; all
microbatch layers wait on the slowest task each batch (Q5). The architecture's one reason for microbatch
(scale) is undone by the one-order premise.

What settles it. Sid: does a shared layer need one total order of its acts, or per-entity order (what Rama
gives by hashing) plus his own rule "order between the two stores exists only through stood-on"
(PROGRESS:162-165)? If per-entity order suffices, the fold goes.

### F7. Every [RULED] mark rests on a session's compression that PROGRESS itself says is not yet verbatim; several recorded rulings are not Sid's words

What it is. STE100:29 defines [RULED] as the Now section of PROGRESS. PROGRESS:17-18 "these are chat
rulings, not ledger rows, until it is pasted"; PROGRESS:126 again for the sharpenings. The paste never
happened in that file. Cases found in my files:
- "N ... chosen for two years" (ruling 2, PROGRESS:72; STE100:566 "Choose N for the next two years.
  [RULED]"). Rama's docs: 19-operating-rama.md:485 "choose a number in anticipation of how much your
  application will grow in the next couple years". Sid typed "two years"/"2 years" in no line about tasks.
  A vendor's advice recorded as his ruling (Sid's shape 6: a fact handed over as a decision).
- "The door makes the name at random, before the gate. [RULED]" (STE100:81-82) against PROGRESS:58
  "Names: random, made by the offerer before the gate". The move from offerer to door is forced by
  correction 13 (no tool may read a random number) and is marked [RULED] without saying so.
- "It shows the result only after the gate accepts that act [the read entry] [RULED: ruling 3 + RIG]"
  (STE100:511-517; correction 22). Ruling 3 (PROGRESS:75-84) has no such line. The source is reader.clj.
- "Google says who you are" is marked [DRAWING] (= not examined) at STE100:394. It is his: ‹ac4db2eb›
  2026-10-02 02:18 "for login we will use google login". The one place the marks under-credit him.
- "one module ... agreed on 30 September" (STE100:563): ‹5220006f› is 2026-09-29 16:49 UTC (22:19 IST),
  agreeing with a session's "nothing you rely on needs them in one module" («00fa0b6a»). His, 29 Sept.
- Sid's card (29-30 Sept) is quoted at STE100:722 and 932-934 as [SID]. No typed line holds "log
  collectively" or "source of truth and event"; the card's words reach the file through a session's
  transcription of a paper or image, which I could not check.

What is lost. D1 and every question of the form "does X replace your ruling Y" asks Sid to defend words he
may not have said. What settles it: the verbatim paste PROGRESS promised; until then [RULED] should read
as "Now-section, unverified".

### F8. "Sid's reason: shared work also needs 'own work feels instant'" is a session's phrase that Sid quoted while saying he was confused

STE100:592 "Sid's reason on 2 October: shared work also needs 'own work feels instant'. [SID]". memtool who:
a session used the words first («abff9316» 03:35); Sid typed them 5 minutes later in quotes (‹12fed86b›
03:40) ending "so this is confusing to me". A confusion was turned into the stated reason for D1. Low cost
alone; it is the reason given for changing four ruled lines. Settles: ask him for his reason.

### F9. §9.4 re-proposes "the base starts as a microbatch layer", the proposal Sid pushed on at 05:43, without saying he did

STE100:1232-1239. Sid ‹5de0a3d3› "why should the base be micro batch?? in the example of canvas/roam why it
should not be streaming?"; ‹b3dbcb29› "when and where did i do that???". He also caught the drawing dropping
an arrow without a word: ‹432e7ba5› 06:13 "in the first one you had the arrow going back to microbatch is
that not now the case???"; ‹d78d0cf7› "so why is it not in the diagram you updated". ?5 (STE100:1206) lists
"24 September: promotions into the base go to the microbatch gate" as if it were a ruling on the base's
class; it is a consequence line in PROGRESS:114-116, itself a session's. Lands at the first kept record in
the base: a re-class leaves two eras for ever; starting microbatch puts every promotion a batch late.
Settles: his pick, with his question carried beside the session's position.

### F10. Statements in STE100 that cannot both hold, or that drop a spectrum

a. STE100:386 "No other program connects" vs :621-623 "safe only if the door seals it, or because the
   runtime is the only client" vs :544 "Rama itself makes sure that no client reads the record" (F1).
b. Correction 10 (STE100:1144) "?4 loses its reason" vs ?4 (STE100:1205) "Without them, forget needs a
   module update": ?4 is said to have no reason and a reason.
c. STE100:100 "Each value has its own lock. [RULED]" vs PROGRESS:96-100 per-act lock by a layer setting:
   the ruled spectrum is dropped to one end (Sid's shape 5).
d. STE100:446-447 clock and random numbers "for outside tools only" vs :449-450 "the door makes names at
   random" vs PROGRESS:58 names "made by the offerer" (F7).
e. STE100:519 each read is a gate decision, :540-541 each bell causes a read, :524-526 read entries move
   the bell: a loop, carried only as "?12 nobody examined".
f. STE100:941 "A name goes to one gate only" and :496-498 resend "with a new name" vs PROGRESS:129-130 a
   series named in full before its first gate.
g. STE100:565-566 N fixed [RULED + RAMA DOCS] and :729-730 "To go past N tasks, a topology copies the
   PStates into a new module": 19-operating-rama.md:487-489 confirms, and adds that the replay road "only
   works if your processing is deterministic, which may not be the case if your processing makes use of
   any random numbers (such as UUIDs)". The gate stamps with wall time and the door names at random, so
   only the repartition road with "significant downtime" remains. The drawing's ?1 position (drawing:
   278-280 "replaying it gives the same state, including past N tasks") was wrong for this reason as well.
h. PICTURE.md:68 "Nobody routes ... The store does not branch on who produced it" vs the door choosing a
   depot by a PState read (F5).

### F11. Smaller mark and citation errors

- STE100:531-532 cites 13-query-topologies.md:175 for "A query topology cannot have a proxy"; the line is
  about invokeAsync.
- STE100:563 "30 September" for ‹5220006f› (29 Sept UTC).
- drawing:32 "act converters (Sid's word, from his sketch)": his only typed use that day is a question,
  ‹c6afceee› 07:15 "So should the converter is about taking a shape and running it through a function to
  output act or a series of acts???". The sketch I could not check.
- Correction 15 (capabilities-first was the session's, not Sid's) is right: ‹d8f12f84› 07:34 names
  "security reviews using languages like Bend or Lean" only. Correction 27 (PICTURE.md:34 "owned by the
  floor and governed by material") is right. drawing:29 "stream layer ... Sid's words": memtool who says
  Sid typed "stream layer" first (‹bc103df4› 05:34), so that mark holds.

## What nobody asked, in either file

1. What is a Rama client, and who can reach the cluster's network? (Sid asked on 10 Oct; F1.)
2. Is "the one exit" a property of what reaches a reader, or a mechanism every byte passes through? (F1, F3.)
3. Where does the runtime run, how many runtimes are there (per instance, per person, per machine), and
   what is the runtime's own door to screens, the boundary that people and models actually cross?
4. Does a layer need one total order of acts, or per-entity order plus stood-on? (F6.)
5. Is the class a fact per layer, per act, or a spectrum with a default, and who sets it? (F2; Sid's own
   question at 06:05-06:09.)
6. Why two depots: product or engineering, and why? (Sid's question, F5; no answer of his on record.)
7. Must the record hold sealed values when the gate can delete rows? What is sealing for? (F4.)
8. Is every read an act? Is "show nothing before the read entry is accepted" Sid's? What does a hover,
   a scroll, a model's read cost? (F3.)
9. Does a reader ring itself? (?12, listed, not examined.)
10. With a REST API on every Supervisor, who can append to the acts depots and invoke resolve? (F1.)
11. What does "N for two years" mean when the gate's own stamps make the replay road to more tasks unusable?
    (F10g.)
12. Does ruling 7's lock per value survive D2? (F4.)

## For another part

- src-store/README.md: whether it records "class set by members" or "seal before depot" as Sid's (decisions
  page). "Always" in ruling 3 and "Default visibility" (ruling 9): no typed line of his holds either;
  decisions page.
- Promotion's "commit, then append" inside a stream event: Rama commits at a partitioner boundary
  (23-acid-semantics.md:40-42, 52); the file does not say where the hop is (whole flows).
- The bench rows at STE100:982-996 and the 20 ms limit (numbers).
- reader.clj "show after accepted" and the task-0 fold as build-session choices (build sessions).

## What I read, and what I did not

Read whole: ARCHITECTURE.md (358 lines); ARCHITECTURE-STE100.md (1257 lines); src/proposal/frame-2026-09-15/
PROGRESS.md (274 lines).
Read in the cited ranges only: reference/rama/docs/15-pstates.md (56, 215-235, 430-445), 13-query-topologies.md
(168-182, grep proxy), 19-operating-rama.md (478-495), 14-depots.md (203-209, 286-292, 312-320),
12-microbatch-topologies.md (118-131), 23-acid-semantics.md (40-42, 52, 82), 24-rest-api.md (headings, 7,
22-33, 87-93, 317-331, 347), 26-all-configs.md (grep), 28-clj-defining-modules.md (378-389), 07-terminology.md
(25-31); grep over all 33 docs for authenticat/authoriz/client/conductor.
Read lines: docs/decisions.md:172-177; PARTS-4-8.md:70-80, 320-326; PICTURE.md:32-36, 64-68, 93-97, 120-124;
carry-on.md:296-304; LOG.md:1215-1219 and grep optimis/directly.
memtool: searches and lines listed by id above; replies «d08aa134», «d64246d5», «722d265f», «383bab2f»,
«c45017fc» (first 55 lines), «546412f7» (first 43 lines).
Not read: src-store/, src/proposal/backend-on-rama-2026-10-02/BUILD-*.md, store-module/, door/, decisions/,
the rig and Inland code the file cites (rig line numbers taken on trust from the file), rama-check-2026-09-25/
RESULTS.md, the other files in this report folder, any session text between 05:58 and 07:41 on 2 Oct beyond
those named, Sid's card, the drawing session's chat at 07:42 on the server's parts.
