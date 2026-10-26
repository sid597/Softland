# Decision 1, code or data: a reviewer's own thinking, before the page

Written by Claude Opus 5.5 at max effort. The orchestrating session (Claude Fable) started me as a reviewer in the night of 14 to 15 October 2026, at Sid's word. This is phase one: I had the question and its sources, and not the page that another agent is writing on it. No agent but the orchestrator sent me anything. Nothing here is Sid's decision. Where I lean, the lean is marked "default" or "my position", with its reasons.

How to read the marks. ‹id› is a line Sid typed, opened with `memtool line`; where its words matter I ran `memtool who`. "‹id›, long prompt" is a line that the record cannot tell typed from pasted; I say whether a session wrote its words before it. «id» is a session's text, never his words. `path:line` is at HEAD c90c7bcc unless it says "working tree". "Run" is something I ran tonight. "My reasoning" is mine. "Not checked" is what I could not check. The question's own words ("Code or data. Which parts of the inlet, the adapters and the converter are code ...") are a session's composition («3c021c3a», "Session 2's prompt"), which Sid pasted (‹be879d88›, paste #1). They are not his.

## 0. Where this lands

This is here so that the working below can be read against it. The working is the substance.

1. The line between code and data does not fall between the inlet and the adapters. It runs through the inlet and through the adapter. For this road, these must be code: the crossings (read git objects at a revision, read a file, watch a folder); the inlet's own account of what arrived; a floor on what may be read that no record can lift; a few leaves (decode, hash, compare, and one parse primitive); and the runner. The runner runs records under a budget, reads only through the read path, records what it read, and gets fresh ids from the fixed part. The door, the gates and the read path are already built.
2. These can be data, as records in a layer that the system holds and that a person can copy into her own layer: what to read (repository, paths, revision, layer, permission); the markdown and Clojure grammars or tables; which parts are named things; the identity and continuity rules, as claims; the mapping from parts to facts and keys; and how many acts a reading makes.
3. Today I cannot draw the line cleanly at one place: the parse primitive. One general grammar reader, with the markdown and Clojure grammars as records, follows Sid's own pattern of "the shaper, not the box" (‹8239c30d›, ‹e7203392›). Two format leaves in code (a Clojure reader and a markdown block splitter) start the road sooner, but leave a debt that the gauge counts. Default: the grammar reader. The two leaves are the cheaper form for now; choosing them is his call. If he chooses them, the record shell stays the form, so nothing above the leaf changes when the reader arrives.
4. The store as built already gives what makes converters worth being records. A person's copy in her own layer is what reads from her layer find (`src-store/softland/store/read_path.clj:22-24`). An act cites the exact version of a record it read, by fact id (`src-store/softland/store/act.clj:159`). Code gives neither without extra conventions.
5. The cost lens fits this decision only at its edges: what a value can be, what a key is, and the keys of the arrival facts. The main costs of decision 1 are of kinds that the lens does not name (section 10): ids that other makers' acts cite, the runner's language versions, who may change a part, and a secret leaked into a log that is never cut.
6. Three things hang on decision 1 that the four decisions do not name. First, whether the full text of what arrived is kept (the second half of the 5 October question 5). Second, the way back from the store to git, which needs a cut that keeps every byte. Third, authority over records rests on sign-in, which is not built.

## 1. What the question asks

### 1.1 Two readings of "below the waist for code and markdown"

(i) The machinery. Which parts of the inlet, the adapters and the converter must be compiled code, and which can be records that an inhabitant changes by an act. The prompt means this reading: its decision 1 sits beside "what a file becomes", "the same thing across commits" and "who acts".

(ii) The material. The road brings this repository's own .clj and .md files into the store, and part of that is the fixed part's own source (`src-store/`). After the road, the store holds the text of the code that runs the store. A changed version of that text can be written as an act, but that changes nothing that runs. The frame says "Its behaviour cannot be produced from inside" (`src/proposal/frame-2026-09-15/PARTS-4-8.md:262-263`, a proposal that Sid held and did not rule, `PARTS-4-8.md:7-8`). Carry-on gives the other half: "Being implemented in code does not by itself make something permanently outside the land" (`docs/carry-on.md:232`). So for code as material, the waist is an enactment boundary: from the store to text, to git, to a build, to a run. The road builds only the first step. Reading (ii) bears on reading (i): if the cut loses bytes, the way back stays closed for every revision that was cut that way (section 8, P13). My reasoning.

There is a small loop here too (my reasoning). If the adapters are code, they live in this repository, and the road ingests its own adapters as material. If they are records, the repository holds only the runner, and the adapters are already in the store.

### 1.2 The road has more than three parts

The prompt names three parts. My decomposition finds at least fourteen:

- P1 the crossing: bytes from git or from the working tree.
- P2 the inlet's account of what arrived: which ask, path, revision, blob hash, size, when.
- P3 the read scope: which paths may be read, and a floor that nothing may lift.
- P4 what is kept of what arrived: the text, or only its hash.
- P5 the cut: text into parts (forms, sections).
- P6 which parts are named things, and of which kind (ns, defn, a heading).
- P7 identity and continuity: ids for files, forms and sections, and how a part is known to be the same thing at the next commit.
- P8 the mapping from parts to facts: entities, keys, values, and what each fact replaces.
- P9 the grain: how many acts a reading makes, and none for an unchanged file.
- P10 the source record and the trigger: which repository, which paths, which layer, which permission, and when to run.
- P11 the runner, which finds and runs P5 to P9 when they are records.
- P12 the vocabulary: the keys that the facts carry, and what a key is.
- P13 the way back: parts to text, so that an edit made in the store can reach a file.
- P14 provenance: which version of which rule made an act.

Q3, Q4 and Q5 ask where the inlet ends and the adapter begins (`src/proposal/backend-on-rama-2026-10-02/decisions/OPEN-QUESTIONS.md:130-132`). That is the question of which of these parts belong to which name. I use these groups: the inlet is P1 to P3; the adapter is P5 and P6; the converter is P7 to P9; P4 and P10 to P14 stand beside them. Whether P7 belongs to the adapter or to the converter is Q4 itself.

### 1.3 "Code or data" is four questions at once

- Privilege: can the part bypass a guarantee? This is the waist proper.
- Authority: who may change the canonical version? The system only, or anyone in her own layer?
- Form: is it compiled code in git, a record over a vocabulary, or foreign code held as a value?
- Locality: where does it run? On the machine that has the files, on a server, in a browser?

Sid's lines touch different questions:

- "has to exist as code" (‹89442b46›) is about privilege and necessity.
- "Anything above the waist is stored as data, is collaborative editable, createable, agents and humans can build freely over it without getting into git merge deadlocks" (`vision/LOG.md:1316`, 2 September) is about form.
- "so that it is not bounded by what we do in code ourselves" (‹6b40e2e2›) is about authority.
- The Linux line, "we can like uh, basically draw a line of what we mean by what is being shipped by Linux and what is not being shipped by it" (‹6d5e8c3c›, long prompt), is about authority and distribution.
- "only going to change by either me making the changes or a few set of people or agents" (‹4123e23c›) is about authority.
- "anything that we need to read from a local system we need to have the user's permission ... Mac, OS, Windows and Linux" (‹06df0468›, long prompt) is about locality.

In Linux, the line between kernel and userland is a line of privilege. Userland is code too: anyone writes it, and distributions ship it. That is my reading of his analogy, not his. If the two lines are kept apart, a converter can be above the waist (unprivileged, a record) and still be "shipped by the system" (held in a layer that the system holds, and changed only by Sid). Much of the apparent tension in his words (section 2.4) comes from reading the two lines as one. My reasoning.

## 2. What Sid has said

### 2.1 What the waist is

- ‹89442b46› (19 August): "i use waist as to refer to something that has to exist as code vs what can and should be build on top ecs or whatever style".
- ‹1fc6c9b6› (19 August), on the old dogfood ingest: "the goal of it was to bring all the softland in softland by ingesting it"; and "is this how it should be, think deep, broad from the waist pov, do we genuenly need something below the waist and why".
- ‹8239c30d› (25 August): "my principle i am working with is anything that is equivalent to "text in position of gylph out" like the text shaper for the other rendering ones .. because this is something that needs to exist below the waist and then we can build on top of it".
- ‹e7203392› (25 August): "why is box below the waist???? can it not be defined above the waist in ecs??? why not? then if i want to have a circle is going to be below the waist as well??"
- `vision/LOG.md:1316` and `:1318` (2 September, verbatim in the log): "Anything above the waist is stored as data, is collaborative editable, createable, agents and humans can build freely over it without getting into git merge deadlocks"; and "Problem is what needs to be coded with the assumption that we are saying anything that is not here can be done on ecs layer".
- `vision/LOG.md:1334` (6 September, dictated, verbatim in the log): "anything that is below is set in code and cannot be changed"; "I think, basically, the layers are infinite"; "what should exist on the code level that should be going into get and we are maintaining it?"; and of shaders, "since data is code, it will run. I'm not sure if this will work out". Then `:1338` (also ‹2b2b149b›, typed): "I just said that it seems right as I was reading it. Why are you putting the decision on me?"
- ‹75476084› (3 September), his method for finding the waist: "If we start from this point, what should exist to make this? Then go to that layer and then ask the same questions."

### 2.2 Adapters and converters as data

- ‹c6afceee› (2 October): "So should the converter is about taking a shape and running it through a function to output act or a series of acts???"
- ‹6b40e2e2› (2 October): "So who is defining the function? Can it be defined in the data? Currently in server we encoded it in the code but I don't think we do need to do that. Can compute code be written down? I think it can ... so a new user can come and describe the adapter for whatever external data that they have so that it is not bounded by what we do in code ourselves because we cannot do it we need the extensibility". He sent it again in ‹0ace018c› (3 October).
- ‹d8f12f84› (2 October): "we should not be doing everything as code because ... I can be the first user of Softland and uh, make these the canonical ways of uh, providing computation on this on the Softland but as data and then multiple users can do the same it's just like uh, how open source works uh, you can write your library and other people can import it ... we need to figure out what is below the waist what has to be written as code uh, security is a big issue I yeah I agree with that and like we can solve that by having security reviews using languages like Bend or Lean to do formal verification". His answer to security is review and proof, not "keep it in code".
- ‹450c82ca› (2 October): "can we just say anything above the outside inlet is an adapter that the user would define ... what we provide are some base functions and these are the uh, functions that uh, we will provide it has data because uh, uh, I am going to be the first user ... similar to how we are defining uh, pointer as data". In this line, the base functions are data too.
- ‹589cf8ba› (3 October): "Data sources (outside, inside inlet), adapters, converters, act, depots (streaming, microbatch)". The same line holds the warning that shapes how this page is laid out: "one we are making the better abstractions and then the instances they appy to in the same diagram and same layer it is bound to fail every time".
- ‹9791265e› (3 October, dictated): "currently like we have adapters and as we were discussing like that can be data. Uh, so we don't need to port that code over. What we might need is like a converter layer that uh, has some kind of input output attached to it."
- ‹b9b62f9f› (5 October): "what do the need do need these need to be as code? Like specifically for the outside inlet." He asked why even the inlet is code.
- ‹06df0468› (5 October, long prompt; `who` finds no session text that holds "since adapters and converters can be data" before it, and sessions quoted it from 18:58 that day): "since adapters and converters can be data, we are not going to touch them. So it uh, basically comes down to inlets, acts, or depots"; and "the instance and server code are just like examples".

What the sessions did with this. On 2 October a session proposed "everything is data, except three things": the evaluator, raw contact with the machine, and the guarantees («8691beaf»). He asked for its recommendation and for how it changes the architecture (‹541642b5›), and the next text recommended opening the designed total language («dc0b2f80»). His next line redirected the session without a ruling (‹381414f6›: "no let's not pollute this session by printing in the waste kind of thing"). The session then wrote "your position, not yet ruled: converters and adapters are data, you write the canonical base functions, code only for primitives, the evaluator and the guarantees" («63d7b89c»), and he answered "write down" (‹ee9b4a2a›). The backend page records it so: "That the adapters and converters are data is Sid's position of 2 October. It is not ruled." (`src/proposal/backend-on-rama-2026-10-02/PROGRESS.md:302-305`). The queue holds it as G3, "Are adapters and converters data", open (`OPEN-QUESTIONS.md:143`). Its source row in RS reads: "Your position of 2 Oct. Not ruled ... You declined the test on 3 Oct." (`/mnt/data/projects/research/softland/all-decisions-rest-2026-10-06/make_rest.py:91-93`).

### 2.3 The first user, the libraries, Linux, the base

- ‹6d5e8c3c› (9 October, long prompt). Its first paragraph is only in this line, and no session wrote it before; its second paragraph is his typed ‹a3dfb1b8›. It says: "what I will be providing in Softland is like code and then like a few layers that are built on top of it, kind of libraries or something like that. And like we already have a concept for this, which we are saying as converters or importers, ingesters kind of thing. So those are like the default things that I will be making. And similarly, any other person can make their own. ... other people can use either theirs or the system ones, or maybe it turns out like the other people's becomes the system one on some point." The store quotes this line as his (`src-store/README.md:400-412`, entry 3 of 9 October).
- ‹1bd5708f› (13 October): "What I meant by base is like the code that we are writing and maybe a few data based layers or something."
- ‹4123e23c› (13 October, dictated): "The base there means like the code that we are writing, anything thad can be written in code. And then every other person can use to build on top by having data as code."; "basic admin provided things that they can do. Maybe like ingestors or some other stuff."; and "at that layer things are only going to change by either me making the changes or a few set of people or agents making the changes to the core of uh, system. And no one else would have access to it. And it would be a thing that will affect everyone in Softland world."
- The notebook of 28 June (`vision/LOG.md:241`, a model's transcription of his handwriting; the photographs are the source, `:221`): "softland is like ... a ... truly distributed runtime that ships with default capabilities but ... users (AI, humans) can expand it to build on top of it."
- ‹ff11240d› (23 September, long prompt; no session text holds the words before it): "the choosing remains with the user and the toolmaker". And ‹5ff89890› (12 October, typed), his rule for a default: "by default if its not set then we take the side of spectrum that captures more because we are not choosing the destructive side by defualt that can happen even after but the other side cannot." He said it of reads; that it reaches other defaults is a session's reading.

### 2.4 A tension to show him, not to settle

On 2 October, the system's own canonical libraries are data, and he writes them "as the first user" (‹d8f12f84›, ‹450c82ca›). On 13 October, "the base" is "the code that we are writing, anything thad can be written in code"; everyone else builds on top "by having data as code"; and ingestors are an example of what the base provides (‹4123e23c›). If the two lines are read together without care, the 13 October line puts the ingestors in code.

There are two readings:

- R1. He moved by 13 October: what the system provides, the ingestors included, is code, and data is for everyone else.
- R2. On 13 October he was correcting what "base" means: not a layer with many writers (`src-store/README.md:864-878`). "Code" there stands for what the system provides, compiled or held as system data. The same morning he said "the code that we are writing and maybe a few data based layers" (‹1bd5708f›). On 9 October he spoke of libraries on top of code, the ingesters among them, and of "the other people's becomes the system one" (‹6d5e8c3c›). That last part works smoothly only if the system's ingesters and everyone else's are the same kind of thing.

I lean to R2 (my reading). The topic of 13 October was partitioning, and "data as code" for everyone else reads as one medium for all. But this is his to say, and it decides whether "ingestors as code" is a choice that he has already made. With the two lines of 1.3 kept apart, both of his statements hold at once: the system "ships" the ingesters (authority), and the ingesters are records (form).

### 2.5 The test he declined, and the road he asked for

- On 3 October a session wrote a one-page test (`src/proposal/backend-on-rama-2026-10-02/CONTRACT-above-the-act.md`). A new session was to write the adapter and the converter for the repository and for a second source as data, run nothing, and count what had to stay code. He declined it: "No. I don't want to do this test. This is not a priority to test out." (‹279000ef›; `who`: typed by him.)
- On 14 October: "but to do that we need the sign-in, inlets, adapter, converter atlest for the code and md part" (‹09aa5d93›).

What he declined was a paper prediction, on a day when the work was below the act. The store is now built, and the road that he asks for is that test made real (my reading of the two lines). A session said the same on 14 October: "whether an adapter can be data, G3, as the test you declined on 3 October, now real" («5fdf2c62»).

A divergence to note. The road's text says "this road needs no sign-in yet" (the prompt, a session's words). His own line lists sign-in among what the road needs (‹09aa5d93›). That belongs to decision 4; it touches decision 1 in section 4, item 9.

### 2.6 Shapes are grammars, and grammars have versions

- ‹ed9b8edc› (16 September): ""A value" is a ______ it has a "shape" which is a "grammar", the grammar is itself versioned. So to talk about a "shape" of a "value" we need to get its grammar-at-version".
- Decision 5 of 7 October (F5 A): "Each layer holds a copy of each grammar it uses; a gate reads on its own task" (`src-store/README.md:236-242`). His decisions already treat grammars as layer data, for the gate.
- The frame that he pasted as "our latest conceptual model we are thinking through" (a session's text, `src/proposal/frame-2026-09-15/PICTURE.md:3-13`) says: "A shape is a grammar, and grammars are facts with revisions. The floor interprets grammars; inhabitants write them." (`PICTURE.md:30`)

### 2.7 The vision log on ingest

- `vision/LOG.md:119` (June): "Do we break down the data on ingest time? or during active layer time?" Carry-on keeps the question open: "When to break imported material into its useful units—during ingestion or while preparing it for active work—is explicitly part of the inquiry." (`docs/carry-on.md:99`)
- `vision/LOG.md:257` (4 July): "all these parts should be stored in both raw but also then native or view forward way in rama". Carry-on: "Ingestion should retain the raw artifact as well as the form that makes it usable inside Softland." (`docs/carry-on.md:97`)
- `vision/LOG.md:193` and `:197` (June): "in code we can say that the atomic unit is the function right???"; and "we can play around with the idea of situating and grouping functions in different ways and preserving their edit, semantic, relational and ofcourse buildable as a cog in wheel property as well."
- `vision/LOG.md:189`: the "Who am I" question of a code file. The quoted sections around it (`:155-188`) are a document that he was reading and questioning, not his words (`docs/carry-on.md:445`).
- `vision/LOG.md:228` (the notebook, transcribed): "Import → work with llm on code → check what maps to what and then accept or reject code changes."

## 3. The settled ground, and whether it reached the store

`docs/decisions.md:159-184`, "Tools are records over a vocabulary", was settled on 6 September (commit 6da31b9f). His line "okay. Let's do b." is at `vision/LOG.md:1342`. `memtool who` finds those words only in session texts, such as «34c290d8». The record holds no line of his between 12:10 and 18:04 UTC that day (`memtool around fde75a74`). So the record lacks the line, while the log and the commit hold it. I did not check further. The ruling has these parts:

- "A tool is a record and never enters git. A capability is code below the waist, added once, with receipts." (`docs/decisions.md:169-171`)
- "Nothing in a record is a program. Foreign code enters the store only as a value in a slot with a declared output, for the runner whose language it is" (`:172-175`).
- The exit: "if a record provably cannot say a needed tool twice over, the leaves gain a designed total language, never runtime code, asked of Sid then." (`:176-178`)
- The why, "Sid's own constraints": rows merge, version and authorize per row, so there are no merge deadlocks; agents author records from a one-page contract; a record is total, with a work budget, so "throughput is a compiled runner and not a language change"; and the layers are infinite (`:179-184`).

Did the ruling reach the store? No. I searched `src-store/` and `test-store/README.md` for "vocabulary", "records over", "nothing in a record", "never enters git" and "waist", and found nothing (run). The store's decisions of 6 to 14 October (`src-store/README.md:72-1029`) never mention tools as records. The backend architecture marks the ruling "[SETTLED]" and carries it to converters through ?8 (`src/proposal/backend-on-rama-2026-10-02/ARCHITECTURE-STE100.md:1148`, `:1209`). He made the ruling for the input of the 3D kind (Position 9, `vision/LOG.md:1330-1343`). That it governs converters is a session's extension. The extension is reasonable, but it is not his ruling, and the page should say so.

If the ruling governs converters, it says four things for this decision (my reading):

1. A converter is a record, and never in git.
2. Its steps call capabilities, which are code with receipts.
3. An adapter body written as Clojure text is allowed only through the foreign-code slot. The ruling's own why ("rows merge ... per row") argues against using that slot for tools that we author ourselves.
4. A parser as data needs either "a grammar that one primitive reads" (`ARCHITECTURE-STE100.md:1209`), or the exit, which first needs its proof. "The repo converter is the test" (`:1209`).

The 2 October session said that his position "reopens" the line about programs («8691beaf»); he did not rule on it (2.2). The queue says of ?8: "listed on RS as not for a ruling yet: needs a proof" (`OPEN-QUESTIONS.md:140`).

## 4. What the store as built forces on this decision

I checked each item in the source, and ran one.

1. The store's own waist test. "Why the shape is in code and not a fact in the store. Each shape that a tool gives enters the store as a fact, inside an act. So the shape of the act itself is the floor: it must hold when each fact is wrong." (`act.clj:32-36`; again in `src-store/softland/store/README.md:210-212`.) A gate examines a converter's output, and the converter's errors are attributable to whoever made the act. So nothing about a converter has to hold when every fact is wrong. Two things on the road do: the inlet's account of what arrived, and the floor on what may be read (item 8 and section 6).
2. What a value can be. `value?` takes nil, booleans, strings, readable keywords, UUIDs, integers, finite doubles, decimals, and plain maps, vectors and sets, inside 32 collections at most. "A ratio, a Java float, a list and a record are not values" (`act.clj:286-310`). I ran it tonight on `-M:store`. A Clojure form as itself, `'(defn f [x] x)`, gives false. A symbol gives false. The form's text gives true. The same form written as vectors of keywords and strings gives true. So a parsed form enters the store as text or in another encoding, and any record language for adapters and converters must be written without symbols and without lists. That grammar of values is in every depot record. Under the lens it is therefore a version of the act, and the record language inherits its permanence.
3. Limits, each marked "kept and not decided" in the code: 256 facts per act (`src-store/softland/store/door/declaration.clj:74-77`); 65,536 bytes per value under a plain key (`:84-90`); 18 MiB of text per act (`:92-111`); 256 characters per id or key (`:113-118`); and 32 nested collections (`act.clj`, `max-depth`). Measured on the working tree tonight: 2,426 tracked .clj, .cljc, .cljs, .edn and .md files hold 50.5 MB (19.3 MB without `history/` and `snapshots/`). 130 of the .clj, .cljc, .cljs and .md files are over 65,536 bytes. The .clj file with the most lines that start with "(" has 224 (`src/app/server/rama/object_container.clj`; a rough count of top-level forms). Some .md files in `history/` have more than 256 heading lines, and one has 628. So a whole file is often not one value, and one reading is sometimes more than one act. The converter must then make a series of acts, whose act-ids "are given on request, ahead of sending" (`src-store/README.md:114-117`).
4. Keys. A fact's key is a readable keyword (`act.clj:344`, the `:key` leaf). The store owns the namespace `softland.store` and refuses a key in it that it does not know (`src-store/README.md:422-468`; `door/declaration.clj:44-51`). So the road's keys are the tools' own. What a key is stays open (K5, `OPEN-QUESTIONS.md:111`).
5. Provenance. `:based-on` is a map from fact ids to carried stamps (`act.clj:159`), so only facts can be cited. A converter that is a record is cited by its fact id at no extra cost. A converter in code can be cited only if its version is first written as a fact. The old server carried its rule's version in the note of a relation (`src/app/server/ingest/code_import.clj:25-32`), which shows that the need is real.
6. Copies in a nearer layer. The read path puts layers together: "for each place the nearest layer that holds a fact wins" (`read_path.clj:22-24`). A layer names the layers that it builds on (`:softland.store/builds-on`, `door/declaration.clj:120-125`). So when a person copies a system converter into her own layer, reads from her layer find her copy. Import and fork, his picture of open source (‹d8f12f84›), are behaviour of the read path that is already built for any record. (My reading of the built code; not run for converters.)
7. "A grammar check is not built." (`door/declaration.clj:68`) Decision 5 of 7 October presumes grammars as layer data (2.6).
8. What is read cannot be unread. "Every value is plain in the log, the store keeps no locks, and the shared logs are never cut" (`src-store/README.md:685-700`, his decision of 11 and 12 October). The working tree holds `src/app/server/env.clj`, which git ignores (`.gitignore:16`; `git ls-files` lists no such file; I did not open it). An inlet that read the working tree with a wide pattern would put API keys into a log that is never cut. An inlet that reads committed blobs from git cannot see that file. Of everything on the road, this is the plainest case of a part that must hold when a record is wrong.
9. Authority over records rests on sign-in. "Until sign-in is built, any sender can make any layer that is not made, for any holder." (`src-store/softland/store/gate/first_act.clj:30-33`) The gate cannot check `:who` (`src-store/softland/store/README.md:216-220`). So today, "only the system may change the canonical converter" protects a record no better than git protects code: both rest on access to his machine (my reasoning). That is a fact about now. It is not a reason to choose code.
10. `system` and `base` are the two ids that the form knows, and the system can hold other layers (`first_act.clj:24-28`, `:45-58`; entry 3 of 9 October). So a library layer that the system holds beside the base, for converters and grammars, is possible today.
11. Outside things are recorded as facts first. Entry 11 of 6 October: "an outside thing is recorded as a fact first, so its basis is that fact" (`src-store/README.md:170-176`). A session proposed it for a blank box, and Sid agreed in chat: "I think we agree on this" (‹61dbaec7›). That answers the first half of the 5 October question, "Is what arrived handed on as a value, or recorded as a fact first?" (`src/proposal/backend-on-rama-2026-10-02/PROGRESS.md:566`). It also means that the adapter's input is an inside thing (section 11).

## 5. What the three examples found

These are evidence only. No form, name or call shape is taken from them.

- The old server put everything in code:
  - a git process with a byte-safe read (`code_import.clj:44-65`);
  - a top-level cutter that does not evaluate, with rewrite-clj, comment spans and source anchors (`src/app/server/ingest/README.md:12`);
  - ids made from the blob hash and the content hash (`code_import.clj:159-184`);
  - a lineage law: the same name with a different hash means "supersedes", and the same hash in another place means a move (`:251-320`);
  - a deny list that fails closed, by path (`:205-214`);
  - the rule's version in a note (`:25-32`).

  Its sync drivers were never wired into its ingest (`:12-13`). Its adapters and converters are about 8,150 lines (`backend PROGRESS.md:435-437`; an estimate from a mapping, not a count of function bodies).
- The rig made a tool a fact, found by matching, with at most 16 steps, each run once, and formulas at most 8 deep (`src/proposal/rig-2026-09-25/src/rig/store/recipe.clj:45`, `:60-62`). But its reading step "knows the source" (`CONTRACT-above-the-act.md:104-105`; `src/proposal/rig-2026-09-25/src/rig/material.clj:461-470`). Its ids are derived, as an HMAC over the repository, line, path and name, and its own docstring says "The frame's direction is a random id plus a registry cell and same-as" (`material.clj:41-58`). The rule for "the same function" is a parameter of a code step, `:name-in-file` only (`:461-470`). One reading is one act: the reading, the text, one fact for each named form, and one "gone" fact for each name that left (`:402-460`). A session's finding from the rig: "making a tool a fact is not the same as making its function data. The grain of the leaves decides that." («f259f02c»)
- Inland runs authored rules in a total language with seven kinds of step and a fixed table of leaves. It has no host eval, and it checks sizes after evaluation (`src-inland/softland/inland/total.cljc:1-8`, `:14-28`). It has 64 seed records and 4 citation tools as facts (`backend PROGRESS.md:439-440`). A session read its expression language as having no repeat over a list (`CONTRACT-above-the-act.md:124`). But it has a bounded `:range` leaf, "so a view can repeat over a file's line numbers" (`total.cljc:26-27`), and activities that repeat a step (`total.cljc:218-233`), so the claim is narrower than it reads. The finding that holds: a total language with fixed leaves can carry real authored behaviour, and as it stands it does not cut a text.

## 6. The abstraction: what must be code

This section is kept apart from the instance, as ‹589cf8ba› asks.

### 6.1 Tests

- T1, his: "has to exist as code" (‹89442b46›), and "do we genuenly need something below the waist and why" (‹1fc6c9b6›).
- T2, the store's: it must hold when each fact is wrong (`act.clj:32-36`).
- T3, a session's of 2 October: something that runs data; raw contact with the machine; the guarantees that others rely on («8691beaf»).
- T4, a session's of 5 October: it reads or acts outside; it runs the data; it must hold when every fact is wrong or hostile; or a measurement or a guarantee demands compiled code. "No reason on that list names a source." (`backend PROGRESS.md:367-370`)
- T5, mine: the part whose work it reports must not be able to forge it. The account of what arrived is the basis of everything after it. If an adapter could write that account, an adapter could claim anything. «3e13e026» says the same, as a session's position.
- A negative test, mine, from his box line (‹e7203392›): if putting X below the waist means that the next X' must go below too ("then if i want to have a circle"), then X belongs above, and a general mechanism belongs below.

### 6.2 His analogies, applied

The text shaper ("text in position of glyph out") is below the waist (‹8239c30d›), and the fonts that it reads are data. The box is one specific shape, and he asked for it to be above (‹e7203392›). Applied to the road (my reasoning): a Clojure reader in code is a box, and a markdown splitter in code is a second box. Then if I want Python, YAML or CSV, does each go below the waist as well? One reader of grammars is the shaper, and the Clojure and markdown grammars are its fonts.

### 6.3 "Data" has rungs

This framing is mine:

- R0: code in git.
- R1: parameters to code. A record sets the tables and choices for a function that is fixed in code (the kinds of form, which names count, which key to use).
- R2: a closed declarative form that one interpreter in code reads: a grammar that recognizes and captures, or a mapping with a bounded "for each" and reads.
- R3: a designed total language (?8).
- R4: foreign code held as a value, run by a runner for its language (the settled exit).

Each rung up moves more of the function into what can change from inside. It also needs more design and makes the interpreter's versions live longer (section 10, kind 5). R4 is extensible at once, but its body is text in one value, so editing and merging by row are lost. That goes against the settled why (`docs/decisions.md:179-181`) and against his definition of 2 September (`vision/LOG.md:1316`). The rig was R1 and R2 over capabilities in R0; Inland is near R3. His "Can compute code be written down? I think it can" (‹6b40e2e2›) points at R3. His shader thought (`vision/LOG.md:1334`) pointed at R4, and the ruling that he took keeps R4 for posted foreign code.

## 7. Working back from the ceilings, his way

His method (‹75476084›) is to start at the ceiling, ask what must exist to make it, go a layer down, and ask again. These are the ceilings for code and markdown, in his words:

- C1. Onboard a codebase, work on it with a model, see what maps to what, accept or reject changes, then commit, push and deploy (`vision/LOG.md:228-231`, transcribed).
- C2. Store and version the unit of code, "but the higher levels as well" (`vision/LOG.md:240-241`, transcribed).
- C3. Group functions in other ways and keep them "buildable as a cog in wheel" (`vision/LOG.md:197`).
- C4. A code file has several identities (path, git, names), and one of them is native while the others are derived (`vision/LOG.md:173`, `:189`).
- C5. A newcomer brings any source and describes its adapter (‹6b40e2e2›).
- C6. All of Softland in Softland, by ingesting it (‹1fc6c9b6›).
- C7. Raw and native are both kept (`vision/LOG.md:257`).
- C8. Whether to cut at ingest or at active-layer time stays open (`vision/LOG.md:119`).

Working back (my reasoning):

- From C5: whatever is specific to a source or a format must be something that a newcomer can write, and it must run in the newcomer's runtime, where her files are. A record travels to any runtime that has the crossings; code has to be deployed to each runtime. So adapters are records (R2 at least), and the crossings belong to each host.
- From C3, C4 and C8: the cut and the identity rules will change with use. Whoever changes them should not need a commit, and both versions should stay in history. So P5 to P7 are records, and the text is kept (C7), so that a new cut can be made from inside.
- From C1 and C6: the way back must exist, from the store to text, to a file, to git. That needs a cut that keeps every byte (comments and blank lines too), and a reverse step from parts to text. The cut must have this property from the first kept fact.
- From C2: the higher levels (a namespace, a folder, the folder's README) are things too. So the table of kinds includes containers, not only forms.

For all of this, these must exist below the waist: narrow crossings; the account of what arrived; the floor on reading; one general parse primitive; the runner; and fresh ids from the fixed part. A leaf may not read randomness: "No leaf may read a clock, a random number, or anything mutable outside" (`PARTS-4-8.md:78`, a proposal), and "the door makes names at random" (`ARCHITECTURE-STE100.md:450`). Later there must also be a crossing out that writes a file; the first road does not need it.

## 8. The instance, part by part, for markdown and Clojure

For each part: what it does here, the ends, which cost it carries (lens kinds 1 to 3, or kinds 4 to 7 of section 10), and my default.

**P1, the crossing.** The ends:

- (a) Narrow, named crossings: read git objects at a revision (resolve a revision, list a tree, read a blob, list the paths that a commit changed), read a working-tree file, watch a folder.
- (b) A general "run a program with these arguments" crossing, with "git log --raw" written as data in the ask (the reading in «3e13e026»).
- (c) Git's object format itself as data, over a crossing that only reads bytes (the extreme in «8691beaf»).

(a) is small and safe. It knows git, which is a kind of contact, not a source: one primitive "for each kind of contact, never one for each source" («f259f02c», a session's). (b) is wider, and it is arbitrary execution, a capability that only the system could hold. (c) would mean writing zlib and pack parsing in a record language. Cost: none of the three lens kinds, because this part is above the door. Default: (a), in code. The working-tree read waits for the floor of P3, or it reads tracked files only.

**P2, the account of what arrived.** It says which ask, path, revision, blob hash, size and time. It must be code, by T2 and T5. By entry 11 of 6 October, an outside thing is recorded as a fact first (`src-store/README.md:170-176`). So the account is a set of facts that the inlet writes, and the adapter's acts cite them in `:based-on`. Its keys can be the inlet's own plain keys (kind 4), or new store keys in `softland.store` (kind 1, and his to name, as he named the namespace). Default: the inlet's own plain keys.

**P3, the read scope.** A floor in code that nothing can lift: never a path that git ignores, never a fixed list (env.clj, .env files), and fail closed when in doubt. The source record (P10) narrows the scope inside the floor, and never widens it past the floor. Cost: kind 7 if it is wrong. Default: code, and the first road reads committed blobs only.

**P4, what is kept of what arrived.** The ends:

- the text, cut into chunks under 65,536 bytes;
- only the hash, with the text left on the host;
- only the parts, if the cut keeps every byte, so that the parts together are the text.

With the text kept, any adapter can cut the text again from inside, with no access to the host: a newer version of the adapter, or a person's copy in her own layer. With only the hash, every new cut goes back to the host's disk, which only the system's runtime on his machine can reach. The cost is the text of every distinct blob, plain and never cut. That is about 50 MB at the head of this repository (working tree, measured), and more over its 1,858 commits (not measured). If the text is keyed by blob hash, an unchanged file at a new commit adds nothing (my reasoning). Cost kind: 4 (in depot records under the inlet's keys; no gate reads it). Default: keep the text, keyed by blob hash. That is the end that keeps more, by his rule for defaults (‹5ff89890›).

**P5, the cut.** The ends:

- (a) Two format leaves in code: a reader of Clojure top-level forms and a markdown block splitter (R0).
- (b) One grammar reader in code, with the Clojure grammar and the markdown grammar as records (R2).
- (c) A parser written in a designed total language (R3, ?8).
- (d) A parser as Clojure text in a record, run by a sandboxed interpreter (R4).
- (e) No cut at ingest. Keep the text; mint ids only for what is named or pointed at; let a tool cut finer at read time. This is his June question (`vision/LOG.md:119`), and the frame's "A thing gets an id when some consumer needs to refer to it" (`PICTURE.md:82`, a proposal).

Section 9 compares the whole roads. For the cut alone:

- (a) is fast and robust, but each new format is a new leaf.
- (b) is the shaper. It needs a grammar format that fits the grammar of values (no symbols, no lists), with budgets and a way to report a miss. The Clojure grammar must handle strings, characters, comments, regex literals, reader conditionals, metadata, discard and tagged literals. The markdown grammar must handle headings, fenced code (a "#" inside a fence is not a heading), lists, paragraphs and tables.
- (c) waits for the proof.
- (d) is fast but opaque, and it makes the sandbox a guarantee.
- (e) is a different axis: it says when to cut, and it combines with (a) or (b).

Cost kinds: (a) is kind 6 (each new format is a commit); (b) is kind 5, or kind 1 if the gate's grammar check later shares its language; (d) is kind 5. Default: (b), with the grammar kept a recognizer with captures and no actions, so that it does not grow into a programming language. The fallback is (a). For (e), my default is the middle: mint ids at ingest for named things (forms by name, headings), and cut anything finer on demand.

**P6, kinds and names.** Which heads make named things (ns, def, defn, defmacro, defmulti, defmethod, deftest, defonce, and the Rama definitions that this repository uses); which heading levels make sections; whether a list item is a thing. R1, a table in a record. Cost kind 4: the kind words become values that others read. Default: data.

**P7, identity and continuity.** Ids can be derived (a hash over source, path and name: deterministic and replayable, but every id carries the rule) or random (minted by the fixed part on request, like act-ids, and kept in a registry fact under the path and the name). Continuity rules: the same name in the same file; the same hash elsewhere is a move; a rename found by likeness. Each rule is a claim that a person can override in her own layer (`PICTURE.md:82`). The rules are records (R1 or R2); fresh randomness belongs to the fixed part, because a leaf may not read it. Cost: kind 4, and the dearest one. Ids end up in other makers' acts, such as a note or a relation that cites a function by its id. A change of rule cannot rewrite their acts; it can only add same-as claims, which are resolved at read time. This is decision 3's ground. Decision 1 says only where the rule lives and who makes fresh ids.

**P8, parts to facts.** Entities, keys, values, and `:replaces` (the latest fact of that entity and key). R2: a mapping record with a bounded "for each" and reads through the read path; the reads become the basis. Cost: the keys are in depot records. What a key is, is kind 1 (K5); which keys a tool uses is kind 4. Default: data.

**P9, the grain.** One act per file, one act per changed part, or a series; at most 256 facts per act; and none for an unchanged file. That last point answers Q6 for this road ("Can a converter give no act", `OPEN-QUESTIONS.md:133`). If the converter runs twice on an unchanged repository, it must give no act, or it writes the same facts again (my reasoning). R1. Default: data.

**P10, the source record and the trigger.** Which repository, which paths, which revision, which adapter and converter records, which layer, under which permission; and when to run: at each commit, on demand, or on a watch. A session drew such a record on 3 October: "a fact that a person writes: which outside thing, which adapter reads it, which layer its acts go into, with which permission" (`CONTRACT-above-the-act.md:44-45`). R1. Default: data, which the runner reads.

**P11, the runner.** Code, by T1, T3 and T4: it runs the data. It finds records through the read path, so the copy in the nearest layer runs. It runs them under a budget, and records every read as the basis. It asks the fixed part for fresh ids, names a series before its first act, and hands the claims to the door. It gives the guarantee that makes unreviewed records safe: a record can make acts only under its own permission, and can read only through the read path (the session's position at `backend PROGRESS.md:385-393`). Cost: kind 5, the versions of its language.

**P12, the vocabulary.** The keys are keywords as built, or ids with their word and grammar as facts (K5). If converters are records, their keys are data in either case. What a key is belongs to decision 2, and is kind 1.

**P13, the way back.** Parts to text is a concatenation in order, if the cut keeps every byte: a leaf and a record. The crossing out (writing a file, making a commit) is code. It is not on the first road. But whether the cut keeps every byte is chosen with the first kept fact, because a reading that lost bytes stays that way in history. Default: keep every byte. I did not check whether the old server's cut does.

**P14, provenance.** A record is cited by fact id in `:based-on`, at no extra cost. Code needs a version fact that the system writes (for example, "the Clojure converter at commit c90c7bcc"), which the acts then cite. No lens cost: it is a value.

**One more part (mine): this repository's conventions.** Each folder has a README that maps its files, and a namespace docstring maps its functions (`src-store/CLAUDE.md`, "Read by level"). Relating a folder's README to its namespaces is not a fact of Clojure or of markdown. It is this project's custom: it changes, and another repository has other customs. A rule like that has to be a record, or the road becomes code that only fits Softland. My reasoning.

## 9. The whole-road ends

**A. Everything in code.** The inlet, the adapters and the converter are written in Clojure, in git, outside the store's pure core.

- Gives: the fastest working road; mature parsers; easy debugging; no language to design.
- Costs:
  - It goes against what he said each time the question came up (‹6b40e2e2›, ‹d8f12f84›, ‹9791265e›, ‹06df0468› long prompt).
  - Each new source or format needs a commit by the core team, and every other runtime must deploy it.
  - The cut and the identity rules will change with use (C3, C4, C8), and they change only by a commit.
  - There is no copy per layer, and provenance needs a version fact.
  - The road's code becomes the form. A session proposed writing "the first adapter as code and marked an instance, not the form" («f9be15b2»). But a working instance is what every later session reads first, and the project's guidance is that "existing code never answers it by default" (`CLAUDE.md:64-65`).
- Before the first act: nothing to design but the code. At scale: the old server's 8,150 lines again (`backend PROGRESS.md:435-437`).
- Lens: only its outputs are in depot records (kind 4); the rest is kind 6.

**B. A code floor, with everything that is specific to a source or a format as records.** One grammar reader does the cut (R2). The converter is a mapping record (R2). The language grows past R2 only where the road shows that it must.

- Gives: what he asked for; the settled ground (tools as records, grammars as facts); one parse primitive; copies per layer and provenance from the store as built. A newcomer can bring a format as a grammar. The road is the test that ?8 says nobody has made (`ARCHITECTURE-STE100.md:1209`).
- Costs: before the first act, three forms must be designed (the source record, the grammar record with its reader, the mapping record), and the runner built. Two grammars must be written, and there must be a way to report a miss ("A miss is a fact with a subject and a time", `PICTURE.md:133`). Interpretation is slower; the settled answer to that is a compiled runner later (`docs/decisions.md:182`). A session named the risk: "a language is where a project like this can disappear for a year" («dc0b2f80»). Keeping the grammar a recognizer with captures and no actions bounds that risk (my reasoning).
- At scale: many writers bring formats without the core team; many readers read the canonical records or their own copies. Over years, the reader must keep old grammars readable, or the grammars are migrated by acts.
- Lens: kind 5 for the grammar language. If the gate's future grammar check shares that language (one interpreter used in two directions, «f259f02c»'s idea), it becomes kind 1. That coupling makes the text grammar as permanent as the act's own rules; keeping two languages costs a second interpreter. That is his to weigh.

**C. Records whose body is foreign code.** Clojure text, run by a sandboxed interpreter, over the same floor.

- Gives: records by place (in the store, versioned, citable, copyable, and they travel), with the full reach of Clojure, today.
- Costs: the body is text in one value, so editing and merging by row are lost, against the settled why and his definition of 2 September. Reads inside the body are not traced unless the runner hands the body only declared inputs. The sandbox becomes a guarantee (I did not check the sandbox of any interpreter). The settled ground admits foreign code "in a slot with a declared output, for the runner whose language it is"; that was written for posted foreign code (the shader), not for our own tools. Kind 5.

**D. Records as the shell, with the cut done by two format leaves in code.** Everything but the cut is R1 and R2; the two leaves are counted on the gauge.

- Gives: the outer form is right from the first act (source records, identity rules as claims, the mapping as data, copies per layer, provenance). The road starts without a grammar design, and the gauge records exactly what is owed.
- Costs: until the grammar reader exists, a new format is a new leaf. That fails C5 for formats, though not for new sources of a known format. The leaves risk never leaving. When they leave, the records that name them keep working only if the runner keeps the leaves (kind 5), or if the records are migrated.
- Lens: kinds 5 and 6.

**E. Cut late.** Keep the text; mint ids at ingest only for named things; cut finer at read time.

- Gives: the cheapest end under the lens, because the cut lives on the read side. Each layer can cut its own way. His question "ingest time or active layer time" stays open by construction.
- Costs: ids for anything finer than a name are minted at the first reference, and continuity must be solved for each such id. There is a cut on each read; a cache of it would need the adversarial review that the project requires before any caching (`CLAUDE.md`, "Systemic repair and caching"). Until a tool cuts it, the read path gives text, not structure.
- E is about when to cut, and it combines with B or D.

## 10. The cost lens, tested

Where the lens holds: for what a gate interprets in a depot record. That is the act's parts, the fact as a map, the permission tuple, the store's keys, what a key is, and (from item 2 of section 4) what a value can be. A change there is a new version of the act, and the old rule stays (`src-store/README.md:941-952`, decision 6 of 13 October). A table is made again from the depots. The read path costs nothing.

Where the lens does not fit decision 1:

- Kind 4: meaning in other makers' records. A converter's outputs are values under tool keys, and no gate interprets them. Changing a converter changes only later acts. But other makers' acts (notes, relations) cite the ids, kind words and keys that the converter made. Those acts belong to their makers, no one may rewrite them, and the log is never cut. The only bridge is new claims (same-as, `:replaces`), read at read time. That is not a version of the act, not a table, and not free.
- Kind 5: the runner's language. If converters are records, running an old one again (for an audit, a person's copy, or a new cut of kept text) needs the runner to keep the old meaning, or the records to be migrated by acts. It is as permanent as kind 1, but it binds the runner and not the gate, and it bites only when old records run again.
- Kind 6: who may change it. Either a commit and a restart by the core team, or an act by whoever holds the permission. The lens measures permanence, and decision 1 is mostly about openness. His "not bounded by what we do in code ourselves" (‹6b40e2e2›) is not an argument about permanence.
- Kind 7: what cannot be undone. A wrong read scope puts a secret into a plain log that is never cut (`src-store/README.md:685-700`). No version, rebuild or migration repairs it.
- A trap: if the adapters' text grammars and the gate's future value grammars become one language, the adapter language moves from kind 5 into kind 1.

So for decision 1, use the lens for what a value is, what a key is, and the keys of the arrival facts, and use kinds 4 to 7 for the rest. My reasoning.

## 11. What hangs on what

- **Entry 11 of 6 October** (an outside thing is recorded as a fact first). After the inlet writes what arrived, the adapter reads facts, so its input is an inside thing. G1 ("Does an inside thing ever pass through an adapter", `OPEN-QUESTIONS.md:141`) then becomes "yes, always, on the outside road". The guess of 5 October, "An inside thing has a shape already, so it skips the adapter" (`backend PROGRESS.md:341`), does not survive this. The entry also makes pure record adapters the natural form. My reasoning.
- **The second half of the 5 October question 5** (keep the bytes, or only the hash; `backend PROGRESS.md:566-567`). It decides whether a record adapter can run again from inside. With only the hash, every new cut goes to the host.
- **?8** (`OPEN-QUESTIONS.md:140`). B needs R2, not R3; R3 waits for the proof. D needs nothing from ?8. C uses the settled exit instead.
- **G2** (one body language grown from Inland's, `:142`). Rule 5 forbids taking Inland's form. Its finding says that a converter needs a bounded repeat over a list, which Inland's expression language does not have as a general step (section 5).
- **Q3, Q4 and Q5** (`:130-132`).
  - Q3: the inlet is P1 to P3, in code; its asks (P10) are data.
  - Q4: if the adapter ends at "a shape", P7 belongs to the converter, and the adapter reads nothing from the store. That is rule 1 of the 3 October contract, a session's: "An adapter knows the source. It reads nothing from the store." (`CONTRACT-above-the-act.md:61-62`) My lean: the adapter is pure (P5, P6); the converter reads (P7 to P9).
  - Q5: "under" (the ladder drawn by flow: source, inlet, adapter) and "above" (his "anything above the outside inlet is an adapter", ‹450c82ca›, a stack drawn by privilege) may be one claim drawn two ways. That is my reading; I have not checked it with him.
- **Q6** (`:133`): yes for this road (P9).
- **Q17 and Q18** (`:137-138`; LP `/mnt/data/projects/research/softland/decision-print-2026-10-06/make_decision_print.py:40-42`, `:124-127`). The 6 October print proposed base functions with the converters, and the primitives as the inlet. It also proposed that no body touches the outside: a body asks by an act. That fits entry 11 and end B.
- **Decision 2 and K5**: the keys that the converter writes; kind 1.
- **Decision 3**: the identity rule and the continuity claims. Decision 1 says where they live and who makes fresh ids.
- **Decision 4, K9 and K14**: which layer holds the canonical records (the base, or a library layer that the system holds and that the base builds on); who may act as the system; and the fact that authority over records rests on sign-in (section 4, item 9). Also question 7 of the 5 October list, whose session an ingester runs in (`backend PROGRESS.md:569`): the system's session for the canonical records, while a person's copy runs under her own session and writes into her own layers.

## 12. My position, as a default

This is my position, with its reasons. The decision is Sid's.

1. Code: the narrow git crossings (P1); the account of what arrived, written as facts (P2); the floor on reading (P3); the leaves (decode, hash, compare, and one grammar reader); the runner (P11); and fresh ids from the fixed part. Reasons: T1 to T5, and section 4, items 1, 8 and 11.
2. Records, in a layer that the system holds, which any person can copy into her own layer: the markdown and Clojure grammars (P5), the kinds (P6), the identity and continuity rules (P7, with fresh ids from the fixed part), the mapping (P8), the grain (P9), the source record (P10), and this repository's conventions. Reasons: his position each time it came up (2.2); the settled ground (section 3); the copies and provenance that the store gives records (section 4, items 5 and 6); and the ceilings (section 7).
3. Keep the text of what arrived, keyed by blob hash, and a cut that keeps every byte. Reasons: C1, C6 and C7, and his rule that a default keeps more (‹5ff89890›).
4. The two format leaves of D are the cheaper form for now. If he wants the road sooner than the grammar format can be designed, choosing them is his call. The records stay the same, and the leaves stand in for the grammar reader, counted as owed.
5. The road's receipt carries the gauge: every code unit that the road added, and for each one, whether it knows a source, a format, or this project. The 3 October contract had such a count (`CONTRACT-above-the-act.md:113-127`).

What would change my mind:

- If the grammar reader cannot handle markdown's fences or Clojure's reader macros without actions, the grammar is turning into a programming language. Then: stop, go to D, and put ?8 to him with that grammar as the example.
- If he means R1 in 2.4 (the system's ingestors are code), then A for the system's ingestors and B or D for everyone else's. That contradicts "the other people's becomes the system one" (‹6d5e8c3c›): a person's record would have to be rewritten as code to become the system's.
- The fact that authority over records rests on sign-in today (section 4, item 9) is not a reason for code. It is a reason to bring sign-in in early, as his ‹09aa5d93› already asks.

## 13. The questions for Sid, prepared

In everyday words, with one example each and the default told.

1. Someone brings a kind of file that Softland has never read, for example Python. Should she be able to make Softland read it without us writing code? Example: she writes, as a record, what a Python function looks like, and Softland cuts her files by that description. Default: yes. We write one general reader of such descriptions. The Clojure and markdown descriptions are its first two, written as records that you hold.
2. Should Softland keep the full text of every file that it reads, or only a fingerprint and the pieces that it cut? Example: next month you decide that a markdown section should include its subsections. With the text kept, Softland cuts the old files again from inside. With only the fingerprint, it has to go back to your disk. Default: keep the text.
3. The ingesters that you make as the system: are they code that we write, or records that you write first, which others can copy and change? On 2 October you said "as data ... I can be the first user". On 13 October you said "the base ... the code that we are writing ... Maybe like ingestors". Default: records in a layer that only you change. Anyone can copy them into their own layer, and a copy can later become the system's.
4. Must a file come back out exactly as it went in? Example: you change one function in Softland and want the file on disk to change by that function only. Default: yes. Every byte belongs to one piece, comments and blank lines too.
5. Told, not asked: the reader reads only the files that git tracks, at a commit. It never reads a file that git ignores, such as the one with the API keys, and no record can change that. A value in the log is plain and is never cut, so a secret that is read once stays in the log.

## 14. Where I am unsure, and what I did not check

- I did not read the page that is being written.
- I did not read the bodies of the old server's `clojure_adapter.clj` or `markdown_adapter.clj`. So whether their cut keeps every byte is not checked.
- That a recognizer grammar written in the store's grammar of values can handle Clojure's top-level forms and markdown's blocks is my reasoning, not a run. How long the design for end B takes is not measured.
- The sandbox of any Clojure interpreter, for end C: not checked.
- The size of all distinct blobs over the 1,858 commits: not measured.
- "okay. Let's do b." is in `vision/LOG.md:1342` and in commit 6da31b9f. The record holds no line of his with those words; that is a gap that I did not check further.
- ‹6d5e8c3c›, ‹06df0468› and ‹ff11240d› are long prompts that the record cannot confirm as typed. No session wrote their key words before them.
- The counts of forms per file are rough (lines that start with "(").
- Where "the base" holds these records belongs to decision 4. I took no position on it here.
