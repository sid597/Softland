# Orchestrator notes — working file, not a ruling

Written by the orchestrator session (Claude Fable 5.1, max effort). Working
notes that must survive a context squeeze. Nothing here is decided; Sid rules.
R = reported (the words are in the source), I = my inference.

## 1. My blind reading of the Kay / Hickey exchange

Formed 20 September 2026, 11:30, from the primary text in
`sources/hn-11945722-kay-hickey.md`, before any worker report or summary had
arrived, and before testing softland-ff's from-memory reading. Kept separate so
it can be compared with research-3's reading afterwards.

### What set it off (R)

Kay's opening line answers a commenter whose first prediction for the next
paradigm was "focus on data processing rather than imperative way of thinking
(esp. functional programming)". Kay: "What if 'data' is a really bad idea?"

### What Kay says (R, his words)

- "What is 'data' without an interpreter (and when we send 'data' somewhere,
  how can we send it so its meaning is preserved?)"
- "you were able to find the interpreter of the sentence and ask a question,
  but the two were still separated. For important negotiations we don't send
  telegrams, we send ambassadors."
- "Bundling an interpreter for messages doesn't prevent the message from being
  submitted for other possible interpretations, but there simply has to be a
  *process* that can extract signal from noise."
- To Hickey's "interpreters are secondary, and perhaps essentially, varied":
  "Please think especially hard about what you are taking for granted in your
  last sentence."
- Scale is the driver: "ambassadors that can negotiate with other objects
  they've never seen. Think about this as one of the consequences of massive
  scaling"; "Intergalactic Network"; Lincos; "It's not 'Big Data' but 'Big
  Meaning'".
- Time is the other driver: the 1978 Smalltalk image came back to life "because
  it was already virtualized 'for eternity'"; "you could make a universal
  computer in software that would be smaller than almost any media made in it".
- On the seismometer: "How do they know they are even bits? How do they know
  the bits are supposed to be numbers? What kind of numbers? Relating to what?"
- "Welcome to Claude Shannon! It's not about the message but about the
  receiver ..."
- The constructive form of his question: "what is the smallest thing that could
  be universal that will allow everything else to happen?" TCP/IP worked
  "partly because it is small and simple, partly because it doesn't try to
  define structures on the actual messages, but only minimal ones on the
  'envelopes'. And partly because of the '/' which does not force a single
  theory." Then: "What is the minimal 'stuff' ... that could allow 'meanings'
  to be sent, not just bits -- and what assumptions need to be made on the
  receiving end to guarantee the safety of a transmitted meaning?"

### What Hickey says (R, his words)

- Data is "records of things known/uttered at a point in time". "Some metadata
  indicated to me that you said it (should I trust that?), and when."
- "there is a notion of sufficiency ('42' isn't data)". "you must at least
  grant it sufficient formatting and metadata to satisfy that definition."
- Interpretation is separate and downstream: the seismometer records; someone
  else, "given the numbers and the provenance alone", says an earthquake is
  coming. "There is no negotiation or direct connection between the source and
  the interpretation."
- Regress: "richer messages will just bottom out at more data ... Ditto ... any
  accompanying interpreter (e.g. bytecode? - more data needing
  interpretation/execution)."
- Trust: "You can of course instead send me a program that (better?) explains
  yourself, but I don't trust you enough to run it :)". With an ambassador "one
  must always fear that some question you ask will start a war. Science
  couldn't have happened if consuming and reasoning about data had that
  irreproducibility and risk."
- Three aspects of data: a recording of observations at a point in time; it
  "doesn't and can't *do* anything, i.e. have effects"; "it does not change".
- Fact versus derivation: "putting facts behind a *dynamic* interpreter (one
  that could answer the same question differently at different times, mix facts
  with opinions/derivations or have effects) certainly exceeds (and breaks) the
  idea of data." "'date-of-birth' is data and 'age' (unless
  temporally-qualified, 'as-of') is not." "the king is dead" versus "there may
  be a revolt".
- The open design question, in his words: "What constitutes minimal sufficiency
  of 'data' is a useful and interesting question. E.g. should data always
  incorporate time, what are the tradeoffs of labeling being in- or
  out-of-band, per datom or dataset, how to handle provenance etc."
- "'Data' is not a *universal* idea ... Sometimes we want the facts, and other
  times we want someone to discuss them with. That's why there is more than one
  good idea."

### My reading (I)

1. They never resolve it, and they are not arguing about the same thing.
   Hickey defends data as the *record*: what happened, fixed, inert. Kay
   attacks data as the *message*: what a stranger far away or long after can
   take it to mean. Hickey says so himself ("I thought we were talking about
   data, not objects. I don't think there is a conflict").
2. What Kay asks Hickey to notice in "something given ... interpreters are
   secondary" is, I think, that a thing is only ever given *as* something to a
   receiver that already has the interpreter. Senses come with no explanations
   because evolution built the reader. Kay leaves this unsaid; it is my
   inference from "it's not about the message but about the receiver".
3. Where they agree is the part that matters for Sid. Both say the live
   question is the *minimum every record must carry*. Hickey: "minimal
   sufficiency ... in- or out-of-band, per datom or dataset ... provenance".
   Kay: "minimal [structures] on the 'envelopes' ... does not force a single
   theory". That is Sid's first-record question from two ends. Hickey's end
   says: whatever is not recorded is gone, because the record never changes.
   Kay's end says: whatever the envelope fixes, every reader for decades must
   share, so fix little and do not force one theory.
4. So the exchange sits under Sid's question in its *agreement*, not in its
   *disagreement*. The disagreement (meaning with the reader or with the
   record) sits under a smaller set: key as id or word, grammars, tools as
   facts, the runtime outside the substance, the first facts.
5. The frame is already Hickey-shaped at the record, and it answers one of his
   sharpest lines directly. His "age is not data unless as-of" is the frame's
   split between a running answer and a fact, and "what was shown, with its
   reads" is the "as-of" that turns a derivation into a record. His "the king
   is dead" versus "there may be a revolt" is the one-truth ruling: an opinion
   is stored as the fact that someone said it, marked as theirs, in their layer.
6. The frame answers Kay halfway. Every interpreter it has (grammar, tool,
   definition of stale, policy) is a fact at a version, and each fact points at
   the versions it stood on. That is "lead to its interpreter", done with
   data, which also meets Hickey's regress point. What is left over is what is
   not a fact: the nine-slot envelope, the matching rule, the gate's procedure,
   the leaf set that walks a tool body. A recorded runtime version number is a
   Hickey-style provenance tag. It names an interpreter that lives outside the
   store. Kay's 1978 image came back because the interpreter was small enough
   to travel with the media. The question this puts to layer 0 ("only the
   runtime is not"): is the floor's meaning written down, inside the store,
   small enough to rebuild from, or only named?
7. Kay's envelope test, applied to the nine slots: layer, based on, because of
   and version each carry a theory (of context, of provenance, of cause, of
   replacement). TCP/IP's envelope carries almost none. The ledger records the
   envelope as Sid's *lean*, not a ruling ("this seems right as of now, but I
   cannot say for future"). So the most exposed convention may be the envelope
   itself: what happens to year-one facts when a tenth slot is needed, or one
   of the nine turns out to be the wrong theory.
8. Hickey's "per datom or dataset" names a fork the table does not yet face.
   Datomic hangs who, when and why on the transaction, once, and the datoms in
   it share it. The frame hangs them on every fact. The Codex pass graded
   "bundle boundaries" as lost. To check with research-2.
9. Trust crosses the axes. Hickey's "I don't trust you enough to run it" and
   Kay's "guarantee the safety of a transmitted meaning" are the same problem:
   an interpreter is code, and running a stranger's code is an effect. In the
   frame this is the click lean (only tools the person enabled may act in their
   name) and the fact that bodies are data walked by fixed leaves. So the
   meaning axis and the authority axis are joined at the tool. The capability
   camp (research-3) is the one that has worked this join.
10. What neither had in view in 2016, and the frame has: a receiver that is a
    model. It reads under-labelled data well, which eases Kay's worry today,
    and it cannot be replayed, which sharpens Hickey's. The frame's rule that a
    model's output is an offer, and that what the model was given is recorded,
    is the Hickey answer to a Kay-style receiver.

### softland-ff's from-memory reading, tested against the text

- "Kay: data presumes both ends share how to read it; fails among strangers
  over decades; a message should carry or lead to its interpreter." Holds (R),
  though Kay's weight is on the receiver's minimal assumptions more than on
  bundling.
- "Hickey: data is recorded observation; readers interpret, differently over
  time; shipping an interpreter only moves the problem." Holds (R).
- "Unresolved: the minimal shared ground." Holds, and both say so in their own
  words.
- Missing from it: Hickey's fact-versus-derivation line (the closest match to
  the frame's own split), his trust line, his "per datom or dataset" fork, and
  Kay's envelope test from TCP/IP. These four are where the exchange touches
  the table most directly.
- "Under about a third of the table": I read it as wider in its agreement
  (the whole first-record question) and about that size in its disagreement.
  Order across partitions and erasure are outside it. Hickey's "it does not
  change" is in plain tension with erasure, and the exchange does not touch it.

## 2. Roster and loop state

Seven workers since 11:40 on 20 September (softland-ff's update; Sid started
two more). Report files sit beside this one.

| worker | file | group |
|---|---|---|
| research-1 | rama-marz.md | Rama's own reference per question; Marz; Kreps as the dissent. Only worker allowed into `reference/`. |
| research-2 | facts-datalog.md | Hickey, Halloway, Datomic, Nubank; XTDB v1 to v2; DataScript, Datalevin, Instant |
| research-3 | meaning-objects-substrates.md | the HN thread; Kay and VPRI; Miller, Lemmer-Webber; Varda; Unison; RDF, PROV, nanopublications; Wikidata; IPLD; Realtalk, Folk, Webstrates, Linda, Edwards, Xanadu. Owns the challenge above the table. Heavy: watch for overload. |
| research-4 | log-as-truth.md (or order-time-identity.md if already begun) | narrowed: Helland, Kleppmann, Young, CORFU/Tango/Delos, Hyder, Aurora, Lamport, Certificate Transparency |
| research-5 | sync-versioning-defaults.md | narrowed: sync and multiplayer incl. AT Protocol, Nostr, SSB; versioning: Git, Dolt, Irmin, Pijul |
| research-6 | clocks-ids-determinism.md | FoundationDB, TigerBeetle, Calvin, Spanner/F1, Bigtable to Megastore to Spanner, Chubby, Firestore, Zanzibar, Temporal, Restate, Kingsbury, Newcombe |
| "research 7" (name has a space) | frontiers-views.md and defaults-skeptics-bigtech.md | McSherry/Naiad, differential dataflow, Materialize, DBSP/Feldera, Brandon, Noria, 3DF, Alvaro, Hellerstein; then the industry default per question, Stonebraker, Hellerstein, Pavlo, McKinley, Amazon, TAO, Orleans, Hyrum's Law, Builders' Library |

Round-three fault lines, as mapped by softland-ff:

1. single writer and one total order versus partitioned logs: research-2 against research-1
2. names versus ids for keys: research-2 and research-5 against research-3
3. policy keyed on identity versus capabilities: research-6 (Zanzibar) against research-3 (Miller)
4. meaning with the reader versus with the record: research-2 and research-3, both sides of the thread
5. ids computed from content versus assigned, and what each did about deletion: research-3 and research-5 against research-2 and research-6
6. recording every read versus its cost: research-5 (Convex) and "research 7" (lineage, view maintenance) against "research 7"'s skeptics section

Load note: research-3 appears in four of the six lines. If it is near its limit
after round two, lines 2 and 5 can be carried by research-5 alone on the ids
side, and Sid can be asked for one more session.

Loop state is kept in `loop/state.md`.

## 3. What I hold of the frame, and where it is

Pointers only. The verbatim wins over anything here.

- Layer −1 and layer 0 with Sid's words: `../../frame-2026-09-15/What-is-this-proposal-about.md` lines 1 to 136.
- Weights: `../../frame-2026-09-15/LEDGER.md`. The envelope ("what a fact carries") is a LEAN: "this seems right as of now, but I cannot say for future or if this will hold for everything". "A thing is an id, for life", "grammars are facts with versions", "a version on every fact", "version as the gate's number per fact" and "one live row per thing, attribute, layer" are rulings.
- The nine things fixed for life, the gauge, the nine demonstrations, and the six items left open: `../../frame-2026-09-15/PARTS-4-8.md` lines 378 to 470. Two of the six left open "to the first one" are what the first-record question turns into now-or-never: "Migration when a grammar or the envelope changes" and "Erasure as an enactment, or a per-person log".
- Sid's governing question, 18 September: "what i want first. the conventions that would paint me in a corner". Order: one Rama first, then many.

## 4. Primary sources I opened myself

Only claims that could change a lean. "Opened" means I fetched the source and
read the words in context, not that I trust the worker's mark.

| claim | source | result |
|---|---|---|
| Hickey: provenance goes "on the transaction (which can have an open set of attributes) ... substantially more efficient than replicating this on many facts (and IMO, correct, as the 'saying' of it *is* the transaction)" | Datomic Google Group, 20 May 2016, https://datomic.narkive.com/Rn7jWmvv/modelling-a-graph-using-reified-transactions | Holds, word for word. Context: he is answering why reified transactions replace many uses of reified edges; the example is "Lucy said that Fred likes Ethel", so the provenance he means is who said it and when. He does not speak of recorded reads there. His premise: "Lucy probably said everything in the transaction". |
| The Kay / Hickey thread | `sources/hn-11945722-kay-hickey.md` | Read in full; see section 1. |

## 5. Threads to carry into the synthesis (running list)

Each is a thought of mine (I) unless marked. Not rulings.

1. **Which log is the log** (from research-1). Offers depot or facts. Not on the table; changes what lean (0) even covers.
2. **What never-rewrite is for.** Asked Sid at 12:05. Honesty to insiders lets representation move; proof to outsiders needs transparency-log methods.
3. **The unit of saying** (research-2, and my blind reading point 8). Two camps press the envelope from opposite sides: Kay's "minimal structures on the envelopes" and Hickey's "per datom or dataset". research-2's test: a slot belongs in the envelope only if every fact always has it and it is never empty. Entity, key, value, version, layer pass; by-whom and when are shared across one act; based-on and because-of are sometimes empty. An open set of attributes on the saying can grow by accretion, so "a tenth slot" stops being a migration.
4. **Convergence on the name of a fact.** Rama camp: never a log position. Datomic camp: facts have no ids; sayings do. Lean (1)'s "hash of the offer's content plus salt" is already an id of the saying. The offerer-minted saying id may be the one name all three accept.
5. **The cost of naming a cut is the hinge between order and provenance.** Recording reads at pattern grain needs "as of" to be cheap to write. research 7 reports the move that does it: the asker gets one number; the store records once, as sidecar data, which per-partition positions that number stands for (McSherry's reclocking, Feldera's log). Facts admitted before such a map exists never get an exact as-of, so the map is a first-record item. In Rama the microbatch id is such a number; whether a stream gate can be given one is a question for research-1.
6. **Internal consistency** (research 7, Brandon). A running answer that reads two indexes at different frontiers can show a state that never existed. By the frame's own rule (the map must not lie) that must be either prevented (compute at one cut) or painted (inexact).
7. **Valid time** (research-2). The "when" slot is the gate's time. A field seeded from papers has "true from" built into its subject. Datomic: an attribute. XTDB: an axis on every record. Not on the table.
8. **Sameness, not only uniqueness** (research-2). Random ids stop clashes; they also make the same paper ingested twice into two entities.
9. **What may never enter in the clear** (research-2). Decided on day one or never for earlier facts.
10. **XTDB v1 lived lean (9)'s shape** (hashes in the log, erasable documents beside it) and left it: replay blocked on evicted documents, tombstones were needed, and a resurrection race is still open because identical content is the same id. To verify before it touches the lean.
