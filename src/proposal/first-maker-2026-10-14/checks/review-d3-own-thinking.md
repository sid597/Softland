# Decision 3, the same thing across commits: a reviewer's own thinking

Written by the reviewer of decision 3 (Opus 5.5, max effort) on the night of 14 to 15 October 2026, at HEAD c90c7bcc, before reading the page that another agent is writing on this decision. It is phase one of a two-phase review. Nothing here is a ruling; the decision is Sid's.

How to read the marks. `path:line` is a file at HEAD. A short hash is a commit. ‹id› is a line Sid typed; «id» is a session's text. "Run" means I executed it: pure store code under `clojure -M:store`, or git and grep; no cluster. The runs are in the appendix. "Derived" means read from code and not run. "My reasoning" is mine. "Not checked" says itself. The lines of `vision/LOG.md` that I quote are Sid's notes of June 2026, in his words, thinking aloud; they are not rulings.

## 1. What the question holds

The task's words: how a function or a section is known to be the same thing at the next commit, so that an edit replaces the old fact; and what happens on a rename, a move, a deletion.

My reading is that this bundles six questions. They have different ends, and their costs differ in kind.

1. **What a thing's entity id is made of.** The store keeps one current fact for each place, and a place is `[layer entity key]` (`src-store/softland/store/record/rows.clj:17`, `:173`). So whatever the inlet writes as `:e` decides which later facts edit a place and which make a new one. This is the permanent part. Every act keeps it in the log for good.
2. **Who decides that a thing at the next commit is the same one, when, and on what evidence.** The candidates are a rule of the converter at write, git's own rename detection, an author's or an agent's claim, and a reader at read time.
3. **How an edit is written.** With `:replaces` naming the old fact, which the writer must then know. Or without it, which overwrites.
4. **How continuity across a rename or a move is said, if it is said at all.**
5. **How an ending is said**, for a deleted function and for a deleted file.
6. **What the commit itself is in the store.**

Two meanings of one word need keeping apart before anything else. The store's `:replaces` is mechanical: a new fact at the same place, checked against the row that the place holds now (`src-store/softland/store/gate/decision.clj:249-257`). The README's "It replaces entry 4 of 8 October" (`src-store/README.md:1007`) is semantic: one thing supersedes another, different thing (walk-2 found this). A rename sits between the two: the place is different and the thing is the same. A design that says all three with one word will confuse every reader (my reasoning).

**Sid's frame, and git's.** His note: "it does come out with 3 identies .... what is the native one and what is derived" (`vision/LOG.md:173`). Read against git, my reasoning: code arrives with
- git's content ids for **versions** (blob, tree, commit), which never change and are unique;
- **names** (path, namespace, definition name, heading), which change and are human;
- and the relations that the source declares.

What git does not have is **the thing over time**. Git records snapshots. It finds a renamed file only when asked, by similarity, at the time of the diff. Run: `git show -M --summary 085bca4a` reports 13 file renames at 91 to 99 percent similarity. That git stores no rename and that its default threshold is half similarity is my knowledge of git, not checked in its source. For a function or a section, git has no identity at all.

So decision 3 asks what Softland's own `:e` is for a function or a section. It can be one of git's two natives, the name or the version. Or it can be the third thing, which Softland adds itself, with the names and the versions kept as facts on it.

## 2. What the store does today (checked; parts run)

- **Ids.** An id is a readable keyword or a UUID (`act.clj` leaves; `src-store/README.md:148`, entry 9 of 6 October). Its printed text has 256 characters at most (`door/declaration.clj:113-118`). A keyword's first character cannot be a digit (`act.clj:267-270`). A heading with spaces cannot be a keyword id, which is why walk-2 used name-based UUIDs.
- **Facts and retracts.** A fact is `{:e :k :v :replaces}`. `:replaces` is optional, and it is a fact id `[act-id index]` (`act.clj:166-180`; `src-store/README.md:125-134`). A fact with no `:v` is a retract, which the README marks "kept from today's code, not decided" (`src-store/README.md:133-134`).
- **`:based-on`** is an optional map from a fact id to a carried stamp (`act.clj:159`). No gate reads it. The shape's comment says "as its maker saw it" (`act.clj:192`). Sid's decision of 14 October, entry 2 (`src-store/README.md:1013-1020`; ‹296a2a7b›, ‹f6a39dde›): "That an act came after what it is based on is read from its `:based-on`, by fact id."
- **`:because-of` and `:subjects`.** `:because-of` is an act-id (`act.clj:160`) and `:subjects` a set of ids (`act.clj:162`). No gate reads either beyond its shape and the length of its ids (grep: `act.clj:162`, `door/declaration.clj:499`).
- **When the gate reads a place.** It reads one only for a fact that names `:replaces`, or a fact under `:softland.store/permission` (`gate/decision.clj:233-247`). Run (appendix, cases A to F):
  - **A.** A fact with no `:replaces`, on a place that already holds a fact, needs no row and earns no fault, so it is accepted. `writes` then gives the whole row at the place's key, made with no row before in hand (run). In a cluster that row goes over the old one. That is derived from `decision.clj:541-543`, "Every row is whole, at a fixed key ... so each write needs no read"; the put itself is not run. So the walk's derived claim holds: such a fact is accepted, and it overwrites.
  - **B.** A `:replaces` that names the fact which wrote the row is accepted.
  - **C.** A `:replaces` on a fact of the new id, naming a fact at the old id's place, is refused `:replaced-fact-not-current`. **`:replaces` cannot carry a rename.**
  - **D.** A `:replaces` that names a stale fact is refused.
  - **E.** A retract that names the fact it ends is accepted.
  - **F.** A retract with no `:replaces`, on a place never written, is accepted. It leaves a `:nothing-here` row.
- **Retract rows.** A retract's row stays, with no value, "so that a later fact can replace the retract" (`rows.clj:237`). A read tells `:no-fact` from `:nothing-here` (`rows.clj:307-320`; `read_path/answer.clj:20-41`).
- **No history read.** The tables keep the current fact of each place (`rows.clj:17`). The query's words are `:place`, `:entity`, `:run` and `:points-at` (`read_path/query.clj:9-25`). None reads the history of an entity or a past state. REFINEMENTS.md R3, a session's reading, says the same, and I found no such word.
- **A read hands fact ids.** It hands the fact id of each place that it gives (`answer.clj:49-51`). So a writer can learn what to name in `:replaces`, with one read for each entity or with one `:run`.
- **What a `:run` takes.** It takes neighbouring places, "in the order in which the table keeps its keys: the printed text of the entity, then the printed text of the key", and "two entities are neighbours only if their ids print as neighbours" (`query.clj:30-37`). So the readable keyword ids of one namespace sit together, and one `:run` can take a namespace. UUID ids scatter (derived).
- **`:points-at` needs a marked key.** It finds a place only through a marked key's order (`answer.clj:57-63`). That order "holds every fact under that key whose stamp is later than it, and no other" (`rows.clj:64-67`). So a key marked after its facts were written does not find them, and running the depots again gives the same table. The base marks no key (`gate/first_act.clj:132`, `:148`) (derived).
- **Limits.** An act holds 256 facts at most (`door/declaration.clj:74-77`), and a `:based-on` 1,024 entries (`:79-82`).

## 3. The real instances

They come from the walks and from git at HEAD. The counts are mine, run with git and grep, and approximate where marked.

1. **An edit that keeps its name.** At 0e6182b9, `next-stamp` went from three inputs to one, and from line 96 to line 47. Seven definitions were deleted. Walk-1's act 7: 4 facts replaced, 28 retracted, 0 new (`runs/walk-parse-output.txt`).
2. **A move in bulk, with renamed namespaces.** At 085bca4a (8 October), 13 files moved into `door/`, `gate/`, `module/` and `record/`. Two of them were renamed as well: `record.clj => record/rows.clj` and `record_pstates.clj => record/tables.clj`. Git finds each at 91 to 99 percent similarity (run).
   - The 13 files hold about 190 top-level forms. That is a line-start grep (run), approximate.
   - Only about 24 of those forms changed text in that commit (run: diff hunks mapped to the form above them; approximate). Thirteen of them are likely the `ns` forms, whose names changed; I did not check them form by form.
3. **Renames inside a file.** At c296a459 (9 October), five definitions were renamed: `class-at-making` became `depot-at-making`, `class-place` became `depot-place`, `class-place-of` became `depot-place-of`, `class-read` became `depot-read`, and `gates-class` became `gates-depot` (run: the removed and added definitions; that each pair is one rename is my reading). Git's file rename detection cannot see a rename inside a file.
4. **A false move by bare name.** 0e6182b9 deleted `stamp/time-of` and added an unrelated `time-of` in a test file (walk-1).
5. **A section dissolved into other places.** fe592411 (5 October) removed seven sections of `src-store/README.md` ("Run the proof", "The door's calls", and five more). Its message, "The store tree's explanations, each at its own level", says where the matter went: rewritten into docstrings and other READMEs (run: `git show --stat fe592411`). No rule on names or content can follow that. Only the author knows.
6. **The same bytes at two paths** (blob 7e6c32a9), and **the same heading twice** under two parents (walk-2).
7. **A semantic replacement between two different things** (`src-store/README.md:1007`).
8. **How often.** HEAD has 1,858 commits: 73 merges, 34 commits with a file rename, 60 with a file deleted. Since 1 October there are 149 commits: 2 with a file rename, 4 with a file deleted (run). Git does not count renames inside files, and instance 3 shows that they happen.

## 4. What Sid has said, and what he has not

**Rulings that bound the space**, from `src-store/README.md`. Where an entry rests on a session's reading, the entry says so.
- **`:replaces` is optional.** 6 October, entry 6 (`:125-134`). The answer comes from a session's reading of photographs of his sheet (`:83-90`, «3d4ee664»), and from his line "I think we agree on this" (‹61dbaec7›). So a checked edit and a blind overwrite are both inside his decision.
- **Expected versions** are carried by `:based-on` and `:replaces`, not by a part of their own. 6 October, entry 4 (`:102-107`), with the same provenance.
- **An id is a readable keyword or a UUID.** 6 October, entry 9 (`:148-169`). His question then (‹d4fd70ba›): "how can it be a keyword????? we can have10^x no of ids where will we find the keywords for them???"
- **An outside thing is recorded as a fact first, so its basis is that fact.** 6 October, entry 11 (`:170-178`): a session's proposal for a blank box, which he agreed in chat. A git commit is an outside thing.
- **A person's id says nothing: it is a UUID.** Her accounts are joined to it by links that can be erased. 9 October, entry 1 (`:355-369`). His question first (‹941a29a5›): "i want to replace my google account with a new one ... if we go with uuid that would still be possible????" This ruling is for persons. Its reason, that the natural name changes, has the same structure for code (my reasoning).
- **Every value is plain in the log, the shared logs are never cut, and a delete is a retract.** 12 October, entries 2 and 3 (`:685-751`); his words "forever never trim". Whatever is written as `:e` stays in the log for good.
- **The default is the end that keeps more** (‹5ff89890›): "by default if its not set then we take the side of spectrum that captures more because we are not choosing the destructive side by defualt that can happen even after but the other side cannot."
- **A change in what a gate accepts is a new version of the act**, and the old version's rules stay. 13 October, entry 6 (`:941-951`), in the words of a prompt that a session wrote at his word and that he sent.
- **What `:based-on` says, and time travel.** That an act came after what it is based on is read from `:based-on`, by fact id (14 October, entry 2). "time travel will do later" (entry 5, ‹296a2a7b›).
- **What the base is.** "The base there means like the code that we are writing". At that layer things change "by either me making the changes or a few set of people or agents making the changes to the core" (‹4123e23c›, dictated; `src-store/README.md:864-879`).

**His thinking aloud.** These are from `vision/LOG.md`, June 2026: his notes, not rulings.
- `:173` "it does come out with 3 identies .... what is the native one and what is derived ... this question is tackled at the start only like this will come up with the chat and md later in form of say comments, and already referencing content".
- `:172` The Roam uid and the chat uuid are the identity, "but it does not mean the content inside of it ... the content can similarly be modified mutated in place constantly".
- `:191` "references, searches, pure funciton as a full execution in itself, history etc. So if we want all these then we need metadata and these features define what the actual identity should entail ???"
- `:193` "the path is actually in itself context of where the function is. And I think in code we can say that the atomic unit is the function right???"
- `:195`-`:196` "The file name is the parent ... each function start in a code file is a child of page", and "if we do it like this then we can attach the identity to them".
- `:197` "if we have them independent as the atomic unit in terms of functions then we can group and relate and situate them with other function groups in differnent ways ... preserving their edit, semantic, relational and ofcourse buildable as a cog in wheel property as well."
- `:203` "versioning is its own layer ... we do have the normal versionoing in rama by what time something arrives but that is more like default versionining and history and time travel there can a explicit commited one which we want to do e.g like git does".
- `:146` "say i have some code i want to modify the last commit is its base layer whatever i build on top is in the active layer and then we can contrast, compare".
- `:249` "the fine grained per user versioning is like already done by the depot ... what is the atomic unit for a type of artifact that can be versioned in itself and does have the semantic meaning".
- ‹d048babc› (a long prompt of 23 September; the record cannot tell his typing from a paste, and no session wrote these words before it): "Binding::: ... we are looking at what happens when the pointed-at thing moves, deletes or something .. this is a chain of dependency that is filled out by the user ... different groups have different rules for what happens in each case ... So what we have to provide for this dimension is a default AND proof/compute that the dependency chain will be done. Note that the actions not only involve move/delete it can be notification, recompute my layer over something etc."
- ‹e36008e1› (typed, 23 September): "anything that is 'running answer' is fine not to store because it can be derived, but anything that is 'offer' has to be because it cant". On grain: "we can combine these to make a bigger one but can't decompose it to atomic units later on".
- ‹ff11240d› (a long prompt of 23 September, not safely his words by the record): the store provides the whole spectrum, and "the choosing remains with the user and the toolmaker".

**What nobody has decided.** The identity rule for code or sections. Whether continuity is recorded. How a deletion is said. Whether the commit is recorded.
- The one proposal on record is a session's, «ca81bcf6» (14 October): "by name (namespace plus function; file plus heading path), with the commit as what the act is based on. A rename becomes a new thing plus the old one marked gone." Walk-1 took it as its working cut and its default (walk-1, question 1).
- A session's frame of September (`src/proposal/frame-2026-09-15/PICTURE.md:82`, «8af659ca»; Sid pasted it and did not type it) says: "Minting is a claim with an actor, and continuity between minted things is a claim too, by ingest through name and hash, or by a person overriding in their layer."

**Where his lines point, my reading.**
- The function is a unit of its own, and its path and grouping are context that can change (`:193`, `:197`).
- What identity must serve should decide it (`:191`).
- Versioning is a layer of its own, apart from the history of arrivals (`:203`).
- What happens when a pointed-at thing moves or is deleted is a chain that groups fill, with a default and a guarantee that the chain is walked (‹d048babc›).

These lean toward an identity that is not the name. They are notes, not rulings, and the walk's default leans the other way.

## 5. The ends

### 5.1 What `:e` is made of

**E0. Position is the id** (line or ordinal). This end fails. Every edit above a thing moves it: walk-2 saw the 13 October section go from line 880 to 884. The old server kept positional forms out of its lineage for that reason (`src/app/server/ingest/code_import.clj:160-163`, a finding). Position stays useful only as a tiebreak: for unnamed forms (`comment`, `extend-protocol`), and for two equal headings under one parent.

**E1. The name is the id.** A function is `:ns/name` (walk-1's cut A). A section is a name-based UUID of `path#heading-path` (walk-2). A file is the UUID of its path.
- **What it gives.**
  - The converter keeps no state. One commit's acts are a function of each file's old and new blobs.
  - A second run of the inlet over git gives the same ids.
  - Two writers and two layers agree on an id without asking each other. A person's active layer over the base (`:146`) writes to the same place, because it derives the same name.
  - Keyword ids are readable in the log, and one `:run` takes a namespace (section 2).
- **What it costs.**
  - **A rename or a move is a death and a birth.** For 085bca4a that is about 190 things times 4 places retracted and as many written: about 1,500 facts, in at least six acts. This is my arithmetic, on walk-1's four facts for each definition and the approximate count. `gate/batch.clj` alone holds 48 forms, so its move would need about 384 facts, more than one act holds. A single file's move cannot then be one act, and a reader between the acts sees half a move.
  - **Pointers dangle.** Every Softland-native pointer to an old id (a citation, a claim in a discourse graph, a comment in someone's layer) points at nothing, unless a continuity fact is written and every reader follows it.
  - **Live views see a removal and an addition, not a change.** The render side's "diffs by identity" is his (`vision/LOG.md:1173-1190`).
  - **A reused name is a false merge.** A name used again after a deletion becomes the old thing's continuation, silently, because the retract's row is "there all the same, so that a later fact can replace the retract" (`rows.clj:237`). Over years this is a false merge.
  - **The grouping is inside the id**, which goes against `:197`.
  - **Names collide across sources.** Two repositories that both have `user` or `app.core` collide. In layers that compose, one repository's function would sit at the other's place and win by nearness: a false merge across layers. A UUID of source and name fixes the collision, and loses the readability.
  - **Names cannot be forgotten.** The ids carry every name into every fact, every place key, every by-value key and every pointer, in the log for good. When a forget build comes (the queue's V15), a name cannot be forgotten without rewriting acts. 12 October, entry 2 sets that aside for the shared logs, in a session's reading that he was shown and did not strike: "nothing is cut out of a shared log, by a trim or for a forget" (`src-store/README.md:699-701`).
- **Lens:** fits none of the three costs (section 8).

**E2. The content is the id.** Each blob or each form's text gets a version id, every edit is a new entity, and continuity is a separate relation. This was the old server's road; findings only, not forms:
- ids made from "git-blob:<sha>" and a content hash (`code_import.clj:157-188`);
- a `:supersedes` lineage graded "mech" when the name is equal, and "silver" when the text hash matches exactly across one commit (`:251-321`);
- `--no-renames`, so that a move is seen as a delete and an add (`:91`, `:106`).

What E2 gives and costs:
- **What it gives.** An immutable pointer to one version. Copies are one thing (7e6c32a9). Nothing has to be decided at write.
- **What it costs.**
  - "An edit replaces the old fact" never happens: no fact is ever replaced. The store's one current fact per place then says nothing about the current function, and "next-stamp now" needs a walk of the lineage.
  - The layered read lets the nearest layer win at a place (`answer.clj:5-7`). Under E2 a person's edit in her layer cannot stand over the base's version, because the two versions are two entities.
  - Copies at two paths become one thing, so editing one of them would not be editing "it".
  - A content hash in the log is a fingerprint of the content, for good.
- **What it offers is in the store already.** A fact id `[act-id index]` is an immutable pointer to one value of one place at one moment (`act.clj:179-180`; `answer.clj:49`). A hash can be a fact under a marked key, so that `:points-at` finds copies. My reasoning: E2's goods can be had as facts, without making the content the identity.
- **Lens:** fits none.

**E3. An id given at birth, carried across commits by matching.** The id says nothing. Name, path, namespace, kind, text and hash are facts on it. At each commit the converter matches each thing to an existing id by a ladder of evidence, and gives a new id when it is unsure.

The variants:
- **E3a.** The id is random. To find existing ids, the converter must read the store or keep a map. A second run of the inlet from nothing gives other ids, unless it reads the store first.
- **E3b.** The id is derived once, at birth, never again. It is a UUID over the source, the commit where the thing was born, its name there, and an ordinal for duplicates. A second run of the inlet over the same history, with the same ladder, gives the same ids.
  - Said simply: E1 derives the id from the name every time, and E3b derives it from the name once, the first time, and keeps it.
  - A natural "source" is the repository's root commit (my reasoning, not checked). A fork shares the root and the old births, so a fork's layer lands on the upstream's places. Things born after the fork get their own ids, so there is no false merge.
- **E3c.** A readable birth id, such as `:softland.store.stamp/next-stamp` kept for good after the move, with the current name as a fact. It is readable, but it misleads after a rename, a reused name needs a suffix, and a heading cannot be one.
- **E3d.** The ids are kept in the repository, in a sidecar file or in the source's metadata, so that git carries them. Every tool outside Softland would then have to keep them up. Not costed.

What E3 gives:
- **A move or a rename is a few facts.** For 085bca4a: one path fact and one namespace-name fact for each of the 13 files, and the texts of about 24 forms. That is some 50 facts against about 1,500 (approximate).
- **What others hold survives.** Pointers held by others hold. The history is one entity's. A live view sees a change.
- **It fits his notes and his ruling.** It fits `:193`, `:197` and `:172`. It fits the Roam model he uses in `:195`, where a block keeps its uid when it moves and its parent is a relation. It fits the structure of his ruling on persons' ids.
- **It fits the destination.** An editor inside the medium knows which thing it edits, so the id is what the editor writes to. The git road is the one road that has to infer.
- **The ids say nothing in the log.**

What E3 costs:
- **A ladder, which is judgment** in code, or in data (decision 1). The error that matters is a **false merge**: an unrelated thing's facts replacing another's. By ‹5ff89890› that is the destructive side, so the ladder must say "new" when it is unsure.
- **State in the converter across commits.** It is a fold over the history, rebuilt from the store when the inlet restarts.
- **A lookup.** Finding an id by name needs a key marked before the first facts, or the converter's own map.
- **Two minters can split one thing.** The two minters are a person's tool and the base's inlet. A function born in a person's active layer, and committed later, gets a second id in the base. That happens unless the inlet reads her layer, or a continuity fact joins the two.
  - In the base itself this resolves, if the ladder compares with the base's current facts and not only with the parse of the previous commit. A function that an editor inside the medium wrote into the base is then found by name, when its commit comes back through git.
- **An opaque id costs one more read** for a human or an agent who knows only a name.
- **Lens:** fits none for the ids. The ladder is a maker's rule (section 8).

**E4. No identity across commits in the store.**
- **E4a.** Each commit's things get ids of that commit or of their blob, and "same" is computed at read, the way git computes renames at diff time. The rule costs nothing to change. But the store has no read of history to compute it from (section 2), the layered read has E2's problem, and the base would not hold "the code that we are writing" (‹4123e23c›) as current facts. Lens: the read path, plus a history read that is not built.
- **E4b.** Each commit is a layer that builds on the last. "Same" is then the same place in two layers, and the nearer wins. That is elegant, and his `:146` uses exactly this shape for the working tree over the last commit. But a read goes through 65 layers at most (`query.clj:84`; `max-builds-on` 64 at `declaration.clj:120-125`, "kept and not decided"), and the repository has 1,858 commits. As history it fails that bound; as one active layer over the base, it is his picture. Lens: the bound is checked by a gate rule (`declaration.clj:372`), so raising it is a new version of the act. Here the lens holds.

**Outside examples, from my knowledge and not checked here.**
- **Git:** mutable names, content-addressed versions, continuity inferred at diff time, nothing for functions.
- **Unison:** a definition's identity is the hash of its syntax, with names abstracted. Names are labels that point at hashes. A rename is free, and an edit makes a new definition, which the name is moved to. So the name persists across edits and the hash across renames, and neither persists across both.
- **Datomic:** entity ids are minted. A unique identity attribute lets a write with the same natural key land on the same entity, and a rename is a change of that attribute. That is E3 with a ladder of one rung.
- **Roam:** block uids are minted, a move keeps the uid, and the parent is a relation.

The thing-over-time identity that persists across both edit and rename is Softland's to add, in the sense that git and Unison do not have it.

### 5.2 Who decides sameness, when, and on what evidence

- **The converter, at write, by a ladder.** This is E3; under E1 the ladder is "an equal name". Its rungs, strongest first (my reasoning):
  1. The same kind and name, in the same file or in the file that git says it was renamed from.
  2. The same text hash, in a thing that vanished in the same commit: a move, which was the old server's "silver", a finding.
  3. A name changed in place with the rest close: the case of c296a459. I did not check whether its bodies were otherwise equal.
  4. Otherwise, new.

  A bare-name match across files is refused (the `time-of` instance). The ladder must work over the whole commit, not file by file, or a move across files looks like a death and a birth.
- **Git's rename detection, as evidence and not as a rule of the store.** Its answer depends on git's version and options, so the inlet can record what it used, as facts on the commit (section 5.6). It sees files, not functions.
- **An author or an agent, by a claim.** Some continuity only the author knows: fe592411, or c296a459, whose commit message says that "class" is "depot". Softland has agents. An agent that reads each commit's diff could claim continuity in its own layer, as a fact with its own `:who`. A commit trailer, a line in the message such as "Renamed: X to Y", is another place for such a claim, at the cost of a convention on every commit. This is the many-writers form of his ‹d048babc›: groups fill the chain, and the system gives the default.
- **A reader, at read time** (E4a). It costs nothing in the store, and it needs history reads.

These are not exclusive (my reasoning). The converter's default can stand in the base, claims by others in their own layers, and readers can choose which layers they read through. The store supports every one of them today without a new part, except the history read.

### 5.3 How an edit is written

- **W1, checked.** `:replaces` names the old fact (run B). The writer can learn the old fact id from its own earlier acts. The door gives act-ids ahead of sending (`door.clj:22-26`), so a writer knows the `[act-id index]` of everything it wrote. Or it can make one read per entity, or one `:run` (section 2). A stale fact is refused (run D). That refusal is how the store tells the inlet that someone wrote in between: Sid, a session, an editor in the medium, or another run of the inlet.
- **W2, blind.** With no `:replaces` the fact is accepted and overwrites (run A).
  - The place's history is not lost. A layer's acts are one line (13 October, entries 2 and 3), so the fact before at that place can be found by running the gates again over the depot.
  - What is lost is the writer's claim of what it saw, which no later read can recover. By ‹e36008e1›, such an offer has to be stored, because it cannot be derived.
  - Two writers then lose each other's edits, silently.
- **Lens.** Both are inside act version 1, since `:replaces` is optional. The choice is made act by act and costs nothing to change for later acts.
- **In the base,** the inlet is the only writer of code today, and Sid's own edits inside the medium are where it is going. W1 is the end that keeps more there (my reasoning).

### 5.4 How a rename or a move is said

- **L1, a `:replaces` across places.** It is refused today (run C). To accept it is a change in what a gate accepts, so a new version of the act: the lens holds, as a depot record. It would also make one slot mean two things, the expected version of a place (6 October, entry 4) and the continuity of a thing.
- **L2, the old fact ids in `:based-on`** (walk-1, act 8). It is accepted today, and no gate reads it.
  - But `:based-on` means "what its maker saw" and "came after" (`act.clj:192`; 14 October, entry 2). An act that rewrites function A after reading its caller B is based on B too, and a reader cannot tell "I read this" from "I am this one's continuation".
  - The old fact ids belong in `:based-on` anyway, as the basis; they cannot carry lineage alone.
  - Lens: fits none. It is a meaning put on a depot field that no gate reads, and that meaning conflicts with the one that 14 October gives the field.
- **L3, a fact.** For example `{:e new :k :code/continues :v old}`, written on the newborn, or the converse fact on the old thing. Its key is marked if "what continues X" must be found through `:points-at`. It is accepted today, and anyone can write one in their own layer; the inlet is the default writer in the base. Under E3 it is needed only where the ladder could not see continuity. Under E1 it is needed on every rename and every move. Lens: fits none; it is a value under a key whose word is a convention.
- **L4, nothing recorded, and continuity computed at read**, which is git's way. Lens: the read path. It needs history reads.
- **L5, git's answer kept as facts on the commit**: the renames, with their similarity and the options used. That is evidence that stays when git changes. By ‹e36008e1› it is a running answer, so it is optional. Lens: fits none.

### 5.5 How an ending is said

- **D1, retract every place of the thing**, each retract naming the fact it ends (walk-1, act 7: 28 retracts for 7 definitions).
  - Every place then answers `:nothing-here` on its own, so a read of any one place does not lie.
  - A deleted file of 48 definitions is about 192 retracts, near the 256 of one act. Larger files need several acts, and a reader between them sees half a deletion.
- **D2, D3 and D4.** D2 retracts only the membership (`:code/in`). D3 writes a status fact. D4 leaves the thing out of a list on the file (say `:file/defines`). Each is one fact, or none. But every other place of the thing keeps a current value: a read of `:code/text` alone shows a deleted function's text as current, unless each reader applies the convention. Lens: the read path, when it is the store's own read path that applies it; for the many tools that read, a convention that each must carry.
- **D1 rests on a kept rule.** It rests on "a fact with no value is a retract" (`src-store/README.md:133-134`), which is not decided. That rule is in the act's form, so the lens holds: a change would be a new version, and D1's retracts would keep the old meaning.
- **What a later reuse of the name does.**
  - Under E1, a reused name brings the retracted places back as the same thing (section 5.1).
  - Under E3 the ladder decides. A revert that brings back the same text in a later commit can be matched to the retired id. A new thing with an old name is new.
- **A file moved out of the paths that the inlet reads** looks like a deletion, under every end (my reasoning).

### 5.6 The commit itself

- **Recorded first.** By 6 October, entry 11, an outside thing is recorded as a fact first, and its basis is that fact. So the commit becomes an entity: its hash, its parents, its author, its message, its time, and if wanted git's rename answer. Each act that the converter makes for that commit carries `:based-on` that commit's fact. This is what «ca81bcf6» meant by "the commit as what the act is based on", made to fit the shape of `:based-on`, which takes fact ids, not commit hashes (my reasoning).
- **His `:203`, "an explicit commited one ... like git does".** The commit is the explicit version: the named point that time travel will ask for. "Name a point" is in REFINEMENTS R3's list, a session's reading. Recording the commit costs a few facts for each commit, and it keeps that point nameable for when time travel is designed.
- **The commit's own id** can be content-derived without harm, since a commit never changes: for example a UUID over its hash. A raw hash that begins with a digit is no readable keyword (`act.clj:267-270`).
- **Lens:** fits none; these are values.

## 6. The instances under the three main ends

My reasoning, on the counts of section 3.

| Instance | E1, the name | E3b, a birth id and a ladder | E2, the content |
|---|---|---|---|
| `next-stamp` edited, 0e6182b9 | its changed places replaced (`:code/doc`, `:code/text`); the same thing | the same | a new version entity with all its facts, and a lineage fact |
| 7 definitions deleted | 28 retracts (D1) | 28 retracts | nothing to retract; "gone" must be said another way |
| 13 files moved and namespaces renamed, 085bca4a | about 1,500 facts in six acts or more; one file over 256; pointers dangle unless about 190 continuity facts are written | about 50 facts; pointers hold | a new universe of ids for every changed file (about 190), and lineage |
| 5 renames inside a file, c296a459 | 5 deaths and 5 births, and 5 continuity facts if anyone writes them | the ladder sees a name change in place when the rest is close; otherwise a claim | new versions, and lineage |
| a bare `time-of` deleted here and added there | qualified names make two things (right) | the ladder refuses a bare-name match across files (right) | different texts make two things (right) |
| sections dissolved, fe592411 | deaths and births | deaths and births; only a claim joins them | the same |
| the same bytes at two paths | two things (right) | two things (right) | one thing (wrong for edits) |
| the same heading twice | the heading path keeps them apart; two equal paths need an ordinal, which drifts | the same, with similarity as a tiebreak | different bodies, different ids |
| a name reused after a deletion | the old thing comes back silently (a false merge) | new, unless the ladder sees a revert | new |

## 7. Many writers, many readers, years

My reasoning, unless marked.

1. **Layers.** In the store's model the nearest layer wins at a place, and identity is what makes two layers' facts be about the same place. A person's active layer over the base (`:146`) works under E1 and E3. It fails under E2 and E4a.
2. **Coordination.** E1 needs none: names are the shared coordinate, which is why paths and URLs work. E3 needs one minter for each thing, or a derivation at birth that every minter reaches the same way. Things born in two places get two ids, and a continuity fact has to join them.
3. **Federation.** His line ‹1082f32d› (18 September): "how do we assign ids? so that they never collide even if there are gazzilions of rama??" E1 collides across sources unless it is scoped. E3a never collides. E3b collides only when the source and the birth are the same, which for a fork is the right answer.
4. **Binding.** Pointers to code will be held by others, in layers that the inlet cannot write: claims, comments, citations. Under E1 every rename strands them; under E3 they hold. This is the cost that the lens does not have (section 8), and it grows with use.
5. **Forget.** Ids are plain in the log for good (the queue's V15: "an entity's id ... plain in the log"; 12 October, entry 2). E1 writes names into every fact, E2 fingerprints of the content, and E3 nothing. For this repository that does not matter. For a private repository in someone's layer, it is the difference between a name that can be forgotten later and one that cannot.
6. **History and time travel.** Both are designed later (14 October, entry 5). "Compare two points" for one function across commits needs one id across them, which is E3; under E1 or E2 it needs a join over lineage.
7. **Running the inlet again.** A second run over git, for a new cut or after a fault: E1 and E3b give the same ids. E3a gives others, unless it reads the store first.
8. **The ladder's version.** A ladder changed later decides later commits differently, and its old decisions stay. Its version should be on its acts, as a fact or in the session, so that a reader knows which rule made a continuity. The old server put its deriver's version in each lineage edge (`code_import.clj:281`, `:310`, a finding).
9. **Readers.** Agents read better with readable ids. E3 adds one lookup, which needs a marked key set before the facts that it must find (section 2).
10. **Uniformity.** The spectrum note's test (memory, `project-store-offers-the-spectrum.md`) asks whether two tools can follow different rules here without every reader carrying both for ever.
    - For the same material in layers that compose, they cannot. The identity rule for code is then one convention for every layer that holds code.
    - For different kinds (code, markdown, chat), the rules can differ.

    So the first road's choice becomes the convention that every later code tool inherits: the editor inside the medium, and a person's active layer. The vision's quoted doc said as much ("the next mutable source with native ids (Roam) inherits whatever code decides here", `vision/LOG.md:166`, not his words). His reply was "this question is tackled at the start only" (`:173`).

## 8. The lens, tested

The lens names three costs: a depot record (a rule in the act's form, so a new act version, by 13 October entry 6); a table (made again by running the depots again, with downtime); and the read path (it costs nothing).

**Where it holds.**
- **L1**, a `:replaces` across places: a change in what a gate accepts, so a new version of the act.
- **A new part of a fact**, say a `:continues` slot: the fact as a map is a depot record.
- **E4b**, a layer for each commit: the bound on builds-on is a gate rule (`declaration.clj:372`).
- **The kept rule** "a fact with no value is a retract", on which D1 rests.
- **A history index or a lineage index**, built by a new topology: a table.
- **D2 to D4 and L4**, applied by the store's own read path: they cost nothing.

**Where it bends.**
- **Marked keys**, for a lookup by name or for "what continues X", are a table. But running the depots again gives the same table, because a mark covers only the facts after it (`rows.clj:64-67`). That table is fixed by the order of the first facts. It is changed by writing the facts again, not by a run. Here "made again by running the depots" does not mean "can be changed".
- **"The read path costs nothing"** holds for the store's own read path. A convention that every tool must apply when it reads (D2 to D4, and the meaning of L3) lives in each tool, and it costs once for each tool, every year.

**Where it does not fit.**
- **Entity ids** (E1, E2, E3 and their variants) are values written in every act and read by no gate. They are not an act version, since a keyword and a UUID are both ids today. They are not a table, and they are not the read path.
  - Their cost: they stay in the log for good, and a change of convention splits every long-lived thing into a before and an after. That split is healed by one bridge fact for each thing, and by every reader knowing both conventions.
  - The heaviest part of the cost: others hold pointers to the ids, in layers that the inlet cannot write.

  The walk marked ids "fits none". I agree, and I name the cost that is missing: binding.
- **L2**, lineage in `:based-on`: a depot field that no gate reads. Its meaning lives in the readers, and it conflicts with the meaning that 14 October gives the field.
- **L3 and L5**: values under a key whose word is a convention. The key's shape is a gate rule; its word is not.
- **The ladder, the birth derivation and the cut** are rules of the maker, above the act. The lens names no cost on the maker's side, and their outputs fall under binding.

**A fourth row and a fifth**, which the lens would need; my proposal, as untested as the lens:
- **Fourth:** "a value convention that others point at". It has no gate rule and no table, and it stays in the log. Changing it costs a bridge for each thing, every reader knowing both conventions, and the owners of every pointer.
- **Fifth:** "a maker's rule", whose outputs are fourth-row values.

Decision 3 is almost all fourth and fifth row. Read through the lens as it is written, it looks cheap. It is the most permanent decision of the four.

**Each option, by the lens:**

| Option | Lens |
|---|---|
| E1, E1 scoped, E2, E3a, E3b, E3c | fits none: a value convention (fourth row) |
| E3d, ids kept in the repository | fits none: a convention outside the store, in git |
| E4a, identity at read | the read path, plus a history read that is not built |
| E4b, a layer for each commit | depot record (the bound on builds-on) |
| the converter's ladder | fits none: a maker's rule (fifth row) |
| claims by an agent or a person | fits none: values |
| W1, W2 | none: both are in act version 1, and can change act by act |
| L1 | depot record |
| L2 | fits none |
| L3, L5 | fits none; L3's mark is a table fixed by the first facts |
| L4 | the read path |
| D1 | values, resting on a depot-record rule that is kept and not decided |
| D2, D3, D4 | the read path, or a convention in every tool |
| the commit recorded first | fits none: values, with `:based-on` used as decided |

## 9. What hangs on what

- **Decision 2** (what a file becomes) holds half of this one, because the id it gives a part is the identity rule. Walk-1's cut A is E1 already. If decision 2 settles ids, it settles decision 3's permanent part, and the two pages must not settle it twice, or differently.
- **Decision 1** (code or data). The ladder and the birth derivation are the converter's. Their version must be recordable either way.
- **Decision 4** (who acts, and in which session). A continuity claim by someone else needs its own `:who`, and the inlet's acts name its run.
- **Grain** (walk-1, question 2). Under E1 a move across files spans two files' acts, and a big file's move spans several. Under E3 a move is one fact on the thing, though the ladder still has to see the whole commit.
- **Which keys the base marks** (walk-2). A lookup by name, or a "what continues X", needs keys marked before the first ingest.
- **Which commits are walked.** It could be all of main from the first commit, in first-parent order, or only from now on. The base is one line in arrival order, so history cannot be added behind it later as edits: a backfill would arrive after the present (my reasoning).
- **K5** (what a key is): the words of `:code/continues` and the like.
- **K14** (whether many people write into the base): more writers make W1 and the minting rule matter more.
- **V15, H9 to H11** (forget): which ids carry names.
- **V53** (time travel): "name a point" and "compare two" use the commit's fact and the identity.
- **The paper test of 3 October.** It is a session's, never ruled, and «ca81bcf6» recalls it: "an adapter knows the source and reads nothing from the store, and a converter knows the store but not the source's format". E3 needs a converter that reads the store, or one that keeps its own map.

## 10. A default, marked as a default, and the questions for Sid

**My default, with its reasons. The decision is Sid's.** It is neither the walk's default nor that of «ca81bcf6».
1. **A birth id for each thing.** A function, a section and a file each get an id the first time they appear. The id says nothing, and it is derived once, at birth, from the source, the commit and the name there (E3b), so that a second run of the inlet gives the same ids. Name, path, namespace, kind, text and text hash are facts on it.
   - Why: his notes put a function's grouping and path in its context, not its identity (`:193`, `:197`). A move then costs a few facts, and the pointers that others hold survive. The ids say nothing in a log that is never cut. And it is what an editor inside the medium will write to.
   - What it costs: a ladder, a fold over the history, and marked keys before the first facts.
2. **"Same" only on strong evidence.** The ladder says "same" on strong evidence and "new" when it is unsure, because a false merge is the destructive side (‹5ff89890›), while a continuity it missed can be added later as a fact.
3. **An edit names the fact it replaces** (W1).
4. **Continuity that the ladder cannot see is a fact** (L3). The inlet, Sid, a session or an agent may write one, each in their own layer. `:based-on` keeps its meaning.
5. **A deletion retracts every place**, each retract naming the fact it ends (D1).
6. **Each commit is recorded first as a thing**, and the commit's acts are based on it.

**The strongest case for the other end** (E1, the session's default). It is the least code. It needs no state and no reads, two writers agree without asking each other, and the walk's acts already show it. If Sid takes it, my reasoning says three things go with it:
- scope the names by their source;
- write a continuity fact on every rename and every move;
- and decide now whether a name used again after a deletion is the same thing.

**The questions, in everyday words.**
1. When `stamp.clj` moved into `gate/` on 8 October and its namespace got a new name, are those the same eight definitions in a new place? Or eight gone, and eight new ones? (At that commit the file held its `ns` form and eight definitions, by my count.)
   - "The same" means a function's name and folder are facts about it: about 50 facts for that day's 13 files.
   - "Gone and new" means the name is the function: about 1,500 facts that day, and anything that pointed at the old names then points at nothing.
   - Default if you say nothing: the same.
2. When a function is renamed inside its file, as `class-place` became `depot-place` on 9 October, who says it is the same function? The converter's rule, an agent that reads the change, or nobody until someone says so?
   - Default: the converter's rule when the change is clear. Otherwise it is new, and anyone can add "this is that one" later.
3. When a function is deleted, does every fact about it say "nothing here", or only the fact that it belongs to a file?
   - Default: every fact.
4. Is each commit kept as a thing of its own, so that each change can say which commit it came from?
   - Default: yes.

## 11. Where I am unsure

- **The counts of section 3 are greps**: the 190 forms of 085bca4a, the 24 forms touched, and the facts derived from them under cut A. A real adapter could count otherwise.
- **c296a459's bodies.** I did not check whether its renamed definitions were otherwise equal, so I do not know whether a ladder would catch them.
- **The overwrite in a cluster.** The acceptance and the whole row are run. The put over the old row, in the module, is derived and not run.
- **Git under other options.** What git's rename detection answers under other options or versions is not checked beyond one run.
- **Opaque ids and agents.** How much an opaque id costs the agents who read is not measured.
- **The split between a person's layer and the base.** How often E3 splits a thing that is born in a person's layer and committed later depends on how people will work, which is not known.
- **How much history the base holds.** All of it, or only from now: that is not mine to settle, and it changes what "the next commit" means for the first run.
- **The lens.** My fourth and fifth rows are untested, as the lens is.
- **His notes.** Sid's June notes are thinking aloud. I read them as pointing away from E1; he may read them otherwise.
- **One write outside this folder.** One run's output was written to the session's scratch directory (`/tmp/claude-1000/.../scratchpad/d3-places-run.txt`) before I put it here. Nothing else was written outside `checks/`.

## Appendix: the runs

All runs were made from `/mnt/data/projects/Softland` at c90c7bcc. No cluster was used, and no file under `runs/` whose name begins `raw-` was opened.

**1. The gate's place checks** (`clojure -M:store`, pure code). The case lines below are the output. The setup was:

```clojure
(require '[softland.store.gate.decision :as d] '[softland.store.record.rows :as r])
(def u1 #uuid "a0000000-0000-4000-8000-000000000001")
(def base-act {:version 1 :who :system :layer :base :permission [:system :base :base] :session :s})
(def old-place (r/place-key :base :softland.store.stamp/next-stamp :code/text))
(def new-place (r/place-key :base :softland.store.gate.stamp/next-stamp :code/text))
(def row-by-u1-27 {:by-act u1 :by-index 27 :stamp 1 :time 0 :who ":system"
                   :permission "[:system :base :base]" :value "\"old text\""})
;; then d/places-needed and d/places on six acts, A to F
```

```
A no :replaces, place occupied -> places-needed: []
A places verdict: nil
B same-place :replaces naming the writer -> verdict: nil
C cross-place :replaces (new id names the old id fact) -> places-needed: [:base :softland.store.gate.stamp/next-stamp :code/text]
C verdict: :replaced-fact-not-current
D stale :replaces (row written by index 26) -> verdict: :replaced-fact-not-current
E retract with :replaces -> verdict: nil
F retract with no :replaces on an empty place -> needed: [] verdict: nil
```

C's printed vector holds one key, the string ":base :softland.store.gate.stamp/next-stamp :code/text", which `println` prints without quotes.

**2. The gate's writes for case A** (`clojure -M:store`, pure code):

```clojure
(d/writes [:base :microbatch #uuid "a0000000-0000-4000-8000-000000000002"]
          {:version 1 :who :system :layer :base :permission [:system :base :base] :session :s
           :facts [{:e :softland.store.stamp/next-stamp :k :code/text :v "new" :text "\"new\""}]}
          nil 2 1760000000000 (byte-array 32) {})
```

```
[:fact ":base :softland.store.stamp/next-stamp :code/text" {:by-act #uuid "a0000000-0000-4000-8000-000000000002", :by-index 0, :stamp 2, :time 1760000000000, :who ":system", :permission "[:system :base :base]", :value "\"new\""}]
[:answer ":base a0000000-0000-4000-8000-000000000002" {:stamp 2, :digest #object["[B" ...], :time 1760000000000}]
[:last-stamp nil 2]
```

**3. Git, and the grep counts:**
- `git show -M --summary 085bca4a`: 13 lines that begin "rename", from 91 to 99 percent, among them `{ => gate}/stamp.clj (99%)`, `{record.clj => record/rows.clj} (97%)` and `{record_pstates.clj => record/tables.clj} (95%)`.
- `git show 085bca4a:<path> | grep -c '^(def\|^(ns'` over the 13 new paths: 190 in all (`gate/batch.clj` 48, `door/declaration.clj` 28, `record/rows.clj` 24, ...).
- `git diff -M -U0 085bca4a^ 085bca4a -- <old> <new>`, each hunk mapped to the nearest line above it that begins "(": 24 forms touched in the 13 files.
- `git show c296a459 -U0 -- 'src-store/*.clj'`: the definitions removed and added are `class-at-making`, `class-place`, `class-place-of`, `class-read`, `gates-class` out, and `depot-at-making`, `depot-place`, `depot-place-of`, `depot-read`, `gates-depot` in. Also added: `in-store-namespace?` and `store-namespace`.
- `git show --stat fe592411`: `src-store/README.md` lost 228 lines net, and `door.clj` and the READMEs of the store tree gained. Its `-## ` lines name seven sections removed.
- `git rev-list --count HEAD` gives 1,858, and `--merges` gives 73. `git log -M --diff-filter=R` gives 34 commits, and `--diff-filter=D` gives 60. Since 2026-10-01: 149 commits, 2 with renames (085bca4a, 62344619), 4 with deletions.
