# Decision 2, thought out on my own: what a file becomes

The reviewer of decision 2 wrote this (Opus 5.5, max effort), in phase one, in the night of 14 to 15 October 2026, at HEAD c90c7bcc. I wrote it before reading the page under review. It prepares the decision and makes none of it; the decision is Sid's.

How to read the marks. A `path:line` is at HEAD. ‹id› is a line that Sid typed, opened with `memtool line`. «id» is a session's text. A line of `vision/LOG.md` is his own note, as the log keeps it, except the lines that begin with `>`, which quote a document he was reading. "My reasoning" is mine. "Not checked" says what I could not check. "Run A" to "Run D" are in `checks/review-d2-runs.txt`, with each command as it was run and what it printed. Runs B and C count with line heuristics and not with a reader; each says which.

## 0. The question, and the five choices in it

The question is a session's composition that Sid pasted (`memtool who "what a file becomes"`: first in «9a48928c»): "What a file becomes. Which parts become entities, which keys they carry, and what a key is."

It holds five choices that pull on each other (my reasoning):

1. **The cut.** Which parts of a .clj or .md file become entities, and at how many levels.
2. **The ids.** What form a part's id has, and what it is made from. Decision 3, the same thing across commits, says what happens when those inputs change. The id rule decides what decision 3 can do cheaply, so I cannot think the ids without saying what they hand to decision 3.
3. **The keys.** Which keys each entity carries; one key for each attribute, or one for each part; how a relation is written; and what a key is (the queue's K5).
4. **The values.** Code as text or as data; whether the raw file is kept, and how.
5. **The act's grain.** How many facts one file gives, against the store's bounds. The cut decides this in practice.

## 1. What the store fixes, and what it leaves open

All of this is read in the code; no cluster ran.

**The act's form.**
- A fact is `{:e :id, :k :key, :v :value, :replaces :fact-id}`; `:v` and `:replaces` are optional (`src-store/softland/store/act.clj:164-171`).
- An id is a readable keyword or a UUID; a key is a readable keyword only (`act.clj:343-344`). Sid decided the half on an id. The half on a key is a session's, and K5 is open (`src-store/README.md:148-170, 470-472`).
- The name of a readable keyword cannot hold `/` (`act.clj:266-280`), so a path is no keyword id: the walk refused its act 5 `:malformed`. A symbol and a list are no value (`act.clj:286-311`), so code goes in as text, or as data under an encoding: the walk refused its act 6 `:malformed`.

**The door's declaration, which each gate checks.**
- The bounds are 65,536 bytes for the one text of one value under a plain key, 256 facts for one act, 256 characters for the printed text of an id or a key, and 32 marked keys for one layer (`src-store/softland/store/door/declaration.clj:74-77, 84-91, 113-118, 127-134`). Each docstring says "a number of the build, kept and not decided".
- An act that names one place twice is refused `:place-named-twice` (`declaration.clj:376`; the walk's act 13).
- A gate reads nothing of a value under a plain key: it keeps the value's text, and `:v` becomes nil (`declaration.clj:273-277, 663-668`). "A grammar check is not built" (`declaration.clj:68`).
- The store owns the namespace `softland.store`, exactly, and any other key is a tool's (`declaration.clj:180-187, 281-289`; Sid's K4 B and his "yes", ‹1a5ec25a›, `README.md:422-466`). Whether `softland.store.x` is the store's too is K15, and it is open.

**The gate and the record.**
- "A fact that names nothing to replace needs no row, and so can never be stale" (`src-store/softland/store/gate/decision.clj:233-240`). So such a fact writes over whatever is at its place. Two parts that get one id are refused when they are in one act. When they are in two acts, the second silently takes the place of the first (my reasoning from that line and from `writes`, `decision.clj:500-530`; not run).
- The key of a place's row is `"<layer> <entity> <key>"`, made of printed texts (`src-store/softland/store/record/rows.clj:173-180`). So a key's spelling is inside a table key.
- For a marked key, a gate keeps a second order of the facts, keyed by the SHA-256 of the whole value's text (`rows.clj:55-63, 400-411`). That order holds only the facts written after the mark (`rows.clj:63-66`). What happens to facts written before a mark is "not designed" (`README.md:600-602`).

**The read path.**
- It reads a place, an entity, a run of neighbouring places, and what points at a thing (`src-store/softland/store/read_path/query.clj:10-24`). A run is in the order of "the printed text of the entity, then the printed text of the key", and "two entities are neighbours only if their ids print as neighbours" (`query.clj:29-37`).
- What points at a thing reads a marked key only (`read_path/sources.clj:203-207`), and it matches the whole value.

**The base.**
- Its rows are on every task: "a write to it is a write on each task" (13 October, entry 4, `README.md:905-922`; the cost is a session's, which he was shown).
- As built, everyone sees it, a person who is not signed in too (`src-store/softland/store/gate/first_act.clj:146`; "kept only so that the build can run", `README.md:516-524`).
- Its log is never trimmed, nothing is cut out of it, and the store can forget nothing until a later build (12 October, entries 2 and 3, `README.md:685-751`, and 718-719; his "forever never trim").
- Order is local to a layer (13 October, entry 2, `README.md:899-903`). A session found, and he has not seen it, that "a gate can read nothing whose order no depot holds", so a gate reads no table of the other gate (`README.md:858-861`).

## 2. The repository, measured

Everything here is at HEAD.

- **Size.** There are 2,186 tracked .clj, .cljc and .md files, 49.0 MB in all. `history/` holds 976 of them and 26.9 MB, `src/` 11.6 MB, and `snapshots/` 4.3 MB (Run A).
- **History.** There are 1,858 commits, with 8,389 changes of a .clj, .cljc or .md file across them (Run A). That is the number of file readings if the road replays the history.
- **Files over the bound.** 129 files are over 65,536 bytes (Run B; decision 1's page found the same). The largest is 6,275,453 bytes.
- **Code (Run B).** The heuristic takes a top-level form to start at a line that begins with `(` or `#?(`; it is not a reader.
  - There are 414 code files and 10,059 top-level forms, with at most 224 in one file.
  - The largest form is 53,173 bytes, and no form is over 65,536.
  - 29 files have more than 63 forms, 20 more than 85, and none more than 255.
- **Names (Run B).**
  - 9,131 forms have a head that is one of 13 def words. 22 code files have no `ns` form.
  - Each ns/name gives a readable keyword of at most 256 characters. The run lists 29 failures, but they are an artifact of the heuristic: it read `(ns ^{:doc "…` in `reference/electric/source/incseq.cljc` as a namespace named `"`.
- **Names that are not one thing (Run B, confirmed with `git grep`).**
  - 20 ns/name pairs are defined in two or three files. All are in the probe projects under `src/proposal/backend-on-rama-2026-10-02/`, whose namespaces `probe.cost`, `probe.fail`, `probe.gate`, `probe.order` and `probe.util` are each declared in two or three files.
  - In one file, `src-store/softland/store/read_path.clj:313, 316, 332, 335` holds two `defmethod print-method` and two `defmethod print-dup`.
- **Markdown (Run C).** The heuristic takes a heading to be a line of one to six `#` outside a fence.
  - There are 1,772 files; 217 have no heading. There are 23,247 headings, with at most 702 in one file.
  - 35 files have more than 63 headings, 5 more than 127, and 2 more than 255.
- **Markdown over the bound (Run C).** 106 markdown files are over 65,536 bytes. Of these, 15 still have a piece over the bound when cut at every heading: in 13 a section is over, in 2 the text before the first heading.
- **Repeated headings (Run C).** 59 markdown files repeat a heading's title; in 18 a whole heading path repeats.
- **One over-bound piece (Run D).** `history/docs/what-softland-is/provenance/exchange-00-commission-and-round0-outputs.md` has a section of 108,594 bytes, and the door's check refuses it `:value-too-large`.
- **A tiling cut (Run D).** I cut `stamp.clj` at HEAD before each top-level form and kept the lines before a form with that form. That gives two parts whose texts, in order, are the file exactly. An act of 11 facts in that cut passes `parse` and the door's check: on the file, its path, blob and parts; on each part, its parent, order, text and name; ids that are name-based UUIDs. The same act with one fact under a UUID key is refused `:malformed`.

## 3. The cost lens, tested on this decision

The lens is a session's, of tonight. It came to Sid in a pasted prompt («cca75ac4» says it "replaces the 'Rama can't migrate keys' argument"), and no run has tested it. Its three costs are these:
- A choice in a depot record keeps its rule in the code for ever, as a version of the act (decision 6 of 13 October, `README.md:941-951`).
- A choice only in a table is made again by running the depots again, with downtime.
- A choice only in the read path costs nothing.

**Where it holds for decision 2.**
- **The first cost.** It covers what a gate accepts: the leaf `:key` (`act.clj:344`), the bound on one value, the bound on the facts of an act, and the bound on marked keys. Each is code that a gate runs. After the first real record, a change to any of them is a new version of the act (decision 6).
- **The second cost.** It covers the text of a place's key and of a key in the order by value (`rows.clj:173-180, 400-411`).
  - Since 13 October, the tables are made by running the gates again over the two depots (`README.md:842-861`, and entry 5). So the table side of K5 costs a second run, with downtime.
  - The queue's K5 row argues from Rama: a migration changes a table's values and not its keys (`src/proposal/backend-on-rama-2026-10-02/decisions/OPEN-QUESTIONS.md:111`). Under 13 October that argument is moot. Nobody migrates a table in place; a second run fills a new one.
- **The third cost.** It covers how a read shows a key's word, and which words a query has. These are free for the store. They are not free for the tools: each word of a query is a contract with every tool built on it (my reasoning).

**Where it does not fit.** Most of decision 2 is none of the three (my reasoning).

- **Content in the log that no gate rule reads.** This is the ids of parts, the names of plain keys, the cut, how code is encoded, and the order of parts. An act writes each of them into a depot. No gate rule reads them. So no version of the act is due when they change, and a second run of the gates gives them back as they were. Their cost is of another kind:
  - They stay in the log for ever: nothing is cut out of a shared log, and the store forgets nothing yet (`README.md:685-751`).
  - A change is made with more acts, which retract the old places and write the new ones. That migration becomes history too.
  - Everyone who points at an old id or key must change their own facts. Those facts are in other layers, and other people write them. This cost grows with the number of writers and with the years.
  - A reader of history sees both vocabularies, and needs a link from the old to the new to read across the change.
  - The maker's rule must stay known. A converter that derives ids must keep the old way of deriving them, or it cannot find the old things again. So the "old rules stay in the code" of decision 6 moves from the gate to the maker. The difference is that a second run replays the gates and not the converters. So a change of a converter never changes the past; it only forks the future.
- **Content that a gate reads.** This is a permission, a setting, a layer's marked keys, and a key's grammar if that check is ever built. The rule is code, so the first cost. The content is changed by an act and needs no version. A second run gives the same only if the gate reads that content from the act's own layer, because order is local and a gate reads no table of the other gate (`README.md:858-861, 899-903`). That puts a limit on grammars as facts (section 4.3).
- **An item that sits in two classes.** The time on the act is one. Its presence is checked: a thing with no time is refused (14 October, entry 4). So its presence has the first cost. Its value is a label that no rule reads (14 October, entry 1), so its value is content.

**When the forever starts.** Decision 6 says "This starts with the first real record" (`README.md:941-951`). For content, the forever starts when the log is kept. If the first road writes into a store that is thrown away, each choice below can be tried and changed. The risk is a trial store that becomes the kept one (my reasoning).

## 4. The ends

### 4.1 The cut: which parts become entities

**E1. The file only.** A file carries its path, its blob and its text.
- Gives: the smallest converter. No id is needed below the file, and decision 3 is the file's path alone.
- Costs:
  - 129 files cannot hold their text in one value (Run B), so E1 needs a split anyway, or a higher bound.
  - Nothing below the file can be pointed at, except by a line range, which moves with every edit.
  - Each edit replaces up to 64 KB.
- Lens: content.
- Sid's words lean away from it: "I think in code we can say that the atomic unit is the function right???" (`vision/LOG.md:193`).

**E2. The file and its top-level parts.** For code, each top-level form; for markdown, each heading's section. The walk's cut A is of this family (`walk-1-stamp.md:16`; `walk-2-readme.md:14`).
- Gives:
  - It is the unit that Sid named for code, with its file as the parent: "The file name is the parent then if we come to line no. from a file where the function starts we can mark that as the parent's child X" (`LOG.md:195`).
  - Each code form fits in one value; the largest is 53,173 bytes (Run B).
  - Another fact can point at a definition or a section.
  - An edit replaces only the parts that changed. The walk's act 7 replaced 4 facts and retracted 28 (`runs/walk-parse-output.txt`, "ACT 7 FACTS").
- Costs:
  - Every part needs a name for its id (4.2). About 900 top-level forms have no def head (Run B: 10,059 forms, of which 9,131 are named). A defmethod shares its name with the others of its multimethod (2 pairs in `read_path.clj`). There are 79 top-level reader conditionals. 59 markdown files repeat a heading title, and 18 a whole heading path (Run C).
  - 15 markdown files keep a section over the bound (Run C), so the cut needs a rule below the heading for them.
  - At about four keys a part, 29 code files and 35 markdown files give more than 256 facts in one act (Runs B and C).
- Lens: content.

**E3. Finer: blocks.** For markdown, the paragraphs, list items, tables and fences under a section; for code, the forms inside a form. Lines are a variant: blame works by line, but a line's identity is its number or its content, and both move.
- Gives:
  - It is the grain that Sid drew for text: "If I use an outliner say roam then each block is the base layer the block which can contain text. A block with children is a paragraph. A bunch of block with children is a section. A group of sections is a page." (`LOG.md:126`). Also: "text-artifact is correct for me in my mind this maps to the smallest text unit that we are going to store, edit, modify, versioning, owner, creator, etc." (`LOG.md:141`).
  - A decision entry of the store's page can be pointed at as itself.
  - The old server cut markdown this way, into headings, list items and paragraphs (`src/app/server/ingest/markdown_adapter.clj:93-97`).
  - A numbered entry has a name of its own, its number in its section, which is how the store's page is cited ("entry 6 of 13 October"). The walk made the entries entities (`walk-2-readme.md:14`).
- Costs:
  - A block that has no number has no name, so its id is its position or its content, and both move with each edit (4.2). The code-ingestor document that Sid quoted says that markdown "has no native identity" (`LOG.md:159`, a `>` line, not his words).
  - There are many more entities and facts (not measured), and each fact of the base is written on every task.
- Lens: content.
- Sid names no unit finer than the function for code.

**E4. Each version is an entity.** This is the old server's way. A unit's id is made from a hash of the file's source and content, and from its position, so an edited file becomes all new things, and continuity is a separate relation (`src/app/server/rama/object_container.clj:216-219`; `markdown_adapter.clj:20-23`; `src/app/server/ingest/code_import.clj:251-300`).
- Gives: a thing never changes, and one version can be pointed at exactly.
- Costs:
  - It breaks the layers (my reasoning). Sid's picture is "say i have some code i want to modify the last commit is its base layer whatever i build on top is in the active layer" (`LOG.md:146`). A read takes the nearest layer that holds a fact at a place (`README.md:535`). A layer above can override the base only at the same place, so only with the same entity id. An id made from content changes with the edit, so the edit in the layer above is another thing, and the base's version still shows through.
  - What is "current" needs a walk along the lineage.
  - One text at two paths becomes one thing. The walk's case: `.claude/skills/...` and `.agents/skills/...` share a blob (`walk-2-readme.md:55`).
- Lens: content.
- The store already names a version without an entity for it (my reasoning). A fact id `[act-id index]` is one version of one place (`act.clj:177-179`), and `:based-on` names fact ids (`act.clj:159`).

**E5. The raw file now, the parts later, in another layer.** The base holds the file, and another tool in another layer cuts it.
- Gives:
  - Sid: "we don't have to fatten this we can add another layer that adds the missing data for different layer" (`LOG.md:144`).
  - It is his own open question: "Do we break down the data on ingest time? or during active layer time?" (`LOG.md:119`).
  - Different tools can cut differently: "the tool is deciding how much of their different actions are put together inside a single value or inside a single act" (‹85a64cfb›).
- Costs:
  - The raw is over the bound for 129 files (4.4).
  - The parts' ids then live in a layer that each reader of parts must read through.

**Four smaller choices inside each end.**
- **The commit as an entity.** It makes "this code at that commit" a fact. But a commit's author and e-mail are a person's data, in a log that cannot forget yet (`README.md:718-719`), in a base that everyone sees as built. A commit's sha alone holds no person (my reasoning).
- **The repository as an entity.** It is needed once two repositories are read: both can hold a `README.md` (my reasoning).
- **The namespace beside the file.** 22 code files have no `ns` form (Run B), and a .cljc file holds two platforms.
- **The text between forms.** Comments and blank lines can go with the next part, or be parts of their own. The old server kept comments as spans of their own (`src/app/server/ingest/clojure_adapter.clj:1-8`).

### 4.2 The ids: their form, and what they are made from

**What an id can be made from.**
- **The repository.** It is needed when there is more than one. It can be a name, or the repository's first commit. With the first commit, a fork gets the same ids as its origin and can sit as a layer above it (my reasoning).
- **The path.** It names one file at one commit in one repository. A later, unrelated file can use the same path again.
- **A form's namespace and name.** These are not one thing in this repository: 20 ns/name pairs are in two or three files (Run B). Each probe project has its own classpath, so `probe.util/within` is a function of two different programs. A form's id must therefore hold its file, or its project, or be minted.
- **The heading path,** with an ordinal for a sibling of the same title (18 files repeat a whole path, Run C). Or the anchor, which is what links use (`README.md:41`).
- **The position.** It moves with each insertion. The old server left unnamed forms out of its lineage for that reason (`code_import.clj:158-163`).

**A line of Sid's pulls against all of these:** "if we have them independent as the atomic unit in terms of functions then we can group and relate and situate them with other function groups in differnent ways ... preserving their edit, semantic, relational and ofcourse buildable as a cog in wheel property as well." (`LOG.md:197`). A function that does not depend on its grouping needs an id that does not hold its file. In Clojure, a function moved to another file changes its namespace, so no name in the source frees a function from its situation (my reasoning). Only an id that is minted once and then kept does.

**I1. A readable keyword made from names.** Cut A's `:softland.store.gate.stamp/next-stamp` is one.
- Gives:
  - A person can read the id in rows, logs and answers, and no lookup is needed.
  - Keywords with one prefix print as neighbours, so one run can read a namespace's definitions, if its ends are chosen with care (`query.clj:29-37`; my reasoning, not run).
- Costs:
  - A path is no keyword (act 5). A heading must be made into a word, which loses letters and collides.
  - The 20 pairs above would become 20 places that two programs share, silently, across acts (`decision.clj:233-240`).
  - An id that reads as a name invites a reader to take meaning from it. The rig held that "nothing here reads meaning out of an id" (`src/proposal/rig-2026-09-25/src/rig/material.clj:47-49`).
  - Sid, of keyword ids for many things: "how can it be a keyword????? we can have10^x no of ids where will we find the keywords for them???" (‹d4fd70ba›).
- Lens: content. The form allows it (Sid's half of entry 9).

**I2. A UUID made from the names** (version 3 or 5).
- Gives:
  - Nothing in the id can be read, and its width is fixed.
  - The converter stays a function of git and reads nothing from the store.
  - A layer above, a person's work or a fork, makes the same id for the same thing, so the layers work.
  - Two stores that read one repository agree on its ids without asking each other. Sid asked: "how do we assign ids? so that they never collide even if there are gazzilions of rama??" (‹1082f32d›).
- Costs:
  - The id is the name. A rename or a move gives a new id, unless decision 3 writes a link.
  - Once kept, the way of deriving is for ever: its UUID namespace and the exact bytes of its inputs. A change forks every id. The old server found the same of its asserter id: "versioning it would fork every edge AND lose retraction rights" (`code_import.clj:26-30`).
  - Anyone who knows a path can make its id and test whether it is there. The rig keyed its derivation so that "an id does not confirm a guessed path" (`material.clj:38-49`). That does not matter in a base that everyone sees; it matters in a private layer.
- Lens: content, and the maker's rule.

**I3. A random UUID, found again through a registry.**
- Gives:
  - The id is free of every name. So it can live through a rename and a move, when decision 3 says that the thing is the same.
  - Nothing can be guessed, and nothing collides anywhere.
- Costs:
  - The converter must read before it writes, and a fresh store gets other ids.
  - A registry needs no new machinery: mark `:file/path`, and "what points at this path" gives the file (`sources.clj:203-207`). But no check of a gate makes the first writer win. A fact that names nothing to replace overwrites (`decision.clj:233-240`), so two converters that meet a new file at the same time make two ids (my reasoning; not run).
  - A layer above must read the base's registry to name the base's things.
- Lens: content. The mark is content that a gate reads.

**I4. An id made from the content.** It is E4's id, and it breaks the layers in the same way.

**I5. Made from the name at birth, and kept after.** The converter makes a part's id from its names the first time it sees the part. From then on, it finds the id by reading the previous reading.
- Gives:
  - Under the rule "the same name is the same thing", I5 gives the same ids as I2.
  - When decision 3 later keeps a thing through a rename or a move, no id rule changes: the id stays the one the thing was born with.
  - A replay of the history in order, under the same rules, gives the same ids (my reasoning).
  - Reading the previous reading also gives the fact ids that `:replaces` must name, so each place keeps its chain of versions. The walk lists three ways to know those ids (`walk-1-stamp.md:55`).
- Costs:
  - The converter reads before it writes.
  - A store that started from a snapshot can differ from one that started from the first commit, for each thing that moved between the two.
- Lens: content, and the maker's rule.

### 4.3 The keys

**How many keys a part carries.**
- **One key for each attribute.** The walk used `:code/in`, `:code/kind`, `:code/doc` and `:code/text`.
  - Gives: an edit replaces only what changed. A relation on its own key can be marked. A layer above can override one attribute.
  - Costs: there are more facts, so the 256 bound comes sooner. A docstring under its own key repeats bytes of the text.
- **One key for each part, with a map as its value.** The rig used one `:material/form` for each form, holding the name, kind, head, lines and digest in one map (`material.clj:451-460`).
  - Gives: one fact a part. So one act holds every code file (at most 224 forms) and all but 2 markdown files (Runs B and C).
  - Costs: each change writes the whole map again. Nothing inside the map can be found by what points at a thing, which matches the whole value. A layer above must restate the whole map to change one field, and from then on its copy hides each later change to the base's map.
- **Only what the store must carry.** The rest is worked out at read, or added by another layer (Sid, `LOG.md:144`).

**How a relation is written.** What points at a thing matches the whole value of a marked key (`rows.clj:55-63`; `sources.clj:203-207`). So a relation can be found from its target only when it is one fact whose value is the target:
- A part's `:part/of`, with its parent's id as the value, can be found.
- A parent's list of its parts cannot, and neither can a set of cited ids. The walk's act 9 wrote `:md/cites-sid #{"296a2a7b"}` (`walk-2-readme.md:24`), which "what points at 296a2a7b" will never find.
- An entity for each relation, with its two ends, can be found, at the cost of more facts.

**The order of the parts.** It is needed when the parts are to give the file back (4.4).
- **A list on the parent.** It is one fact, written again whole at each change. A layer above that overrides the list hides each later change to the base's list (my reasoning).
- **A number on each part.** An insertion renumbers every part after it. Each is a new fact in a log that is kept for ever, and in the base each is written on every task.
- **A text on each part that sorts between its neighbours.** An insertion writes one fact, and a layer above can move one part. The converter must keep the texts that it gave, so it reads the previous reading (my reasoning; not run).

**The marks.**
- The base can mark 32 keys (`declaration.clj:127-134`), and the base holds every tool of the system. Sid on 9 October: "a few layers that are built on top of it, kind of libraries", the "converters or importers, ingesters" (entry 3, `README.md:401-410`, from ‹6d5e8c3c›, a long prompt that the record cannot tell from a paste). On 13 October: "Maybe like ingestors" (‹4123e23c›).
- So one key for "is a part of", shared by code, markdown and later readers, spends one mark, where a key for each vocabulary spends several (my reasoning).
- The marks must be written before the road's first act, because facts written before a mark are not in its order (`rows.clj:63-66`), and what to do with them is not designed (`README.md:600-602`).

**The vocabulary.** The names of the converter's keys are content.
- Sid: "different groups can have their different schemas" (‹6b8c2421›). So the keys of different owners must not collide.
  - Bare words collide.
  - A namespace that names the vocabulary keeps them apart. It also groups a vocabulary's keys in an entity's run, which is ordered by entity and then by key (`query.clj:29-33`).
- A short namespace such as `:file/` can collide with another group's keys later. A namespace under `softland.store` is a risk while K15 is open (`declaration.clj:281-289`).
- A version in a key or an id forks all of them at the next version.
  - The old server put its adapter's name in every unit id, as `"du:" object-key ":" "markdown-block-v0" ":" path` (`markdown_adapter.clj:13, 20-23`).
  - A version belongs on the act, or on its session (my reasoning).

**What a key is (K5).** It is three questions in one (my reasoning).
- **Its form: may a key be a UUID?** `act.clj:344` is what a gate accepts, so this is the first cost after the first real record. To widen keyword-only to keyword-or-UUID keeps every act that the narrow form took. To narrow it later refuses old acts under a new version.
- **Its meaning: its spelling, or facts about it?**
  - The rulings file says "Keys are ids; word and grammar are facts about them" (`src/proposal/frame-2026-09-15/PROGRESS.md:65, 87`), in a session's words. Sid's mark on it is a pick on a session's form, the option that the form marked as its own lean (`PROGRESS.md:333-344`).
  - Under entry 9, a readable keyword is already an id. So `:code/doc` can carry its word and grammar as facts, and "keys are ids" does not need UUID keys. The walk's acts 4 and 10 gave an opaque keyword key its word as a fact, and both passed `parse`.
  - The meaning side costs three things. A reader looks up a key's word. A writer must know the key for a word. And the facts that give a key its word need keys of their own, a small seed (the walk's note on act 4).
- **Its grammar: who checks a value against it?**
  - Today nobody does (`declaration.clj:68`).
  - If a gate is to check it, the grammar must be in the act's own layer or in code, since order is local and a gate reads no table of the other gate (`README.md:858-861, 899-903`). A key whose grammar is stated once in the base cannot be checked by the gate of a person's layer and still give the same on a second run.
  - So a grammar as a fact is checked by each layer that states it again, or at read, or by tools (my reasoning).
- **What the road needs of K5.**
  - Readable keyword keys are good under either answer on the form, and they leave "keys are ids" open, since a keyword is an id.
  - A UUID key needs K5 decided first: the walk's act 3 was refused.
  - So the road need not wait for K5, if it uses readable keywords and puts a key's meaning, when it wants one, in facts about the key (my reasoning).

### 4.4 The values

**Code as text or as data.**
- Text is the exact bytes, a piece of the blob. The old server kept `:text (subs source start end)`, marked "== blob substring" (`clojure_adapter.clj:198`).
- Data needs one of two things, because a symbol and a list are no values:
  - a wider form, which is the first cost, and the one text of a value must then print symbols and lists; or
  - an encoding in maps, vectors and strings, which is content. Such an encoding loses comments, spacing and reader forms, unless it is designed to keep them.
- The code-ingestor document named a destination, code as "self-modifying world substrate" (`LOG.md:175`, the document's words). That is where data matters. Data can be added later, in another layer, beside the text (my reasoning).

**The raw file.** Sid: "we keep both the raw but also store it in softland way" (‹b22e6278›, 6 June). Also: "all these parts should be stored in both raw but also then native or view forward way in rama" (`LOG.md:257`). The ends:
- **R1. One value for each file.** 129 files are over the bound (Run B), and the walk's act 12 was refused `:value-too-large`.
- **R2. The bound raised.** This is the first cost, and it is free before the first real record. The largest file is 6,275,453 bytes. What Rama does with rows of megabytes, written on every task in the base, is not checked.
- **R3. Chunks.** They are a second copy of every byte beside the parts. Fixed-size chunks move with each edit; chunks cut where the content says (a rolling hash) move less (my reasoning).
- **R4. The parts tile the file.** Each byte is in exactly one part: comments and blank lines go with the next form, and the text before the first heading is a part.
  - The parts in order are the file, and the git blob on the file checks it. Run D gave two parts, the file exactly, and `:ok` from `parse` and from the door's check.
  - It fits every code file cut at top-level forms. 15 markdown files need a cut below the heading (Run C).
  - So R4 makes the raw and the native one set of facts, with no second copy (my reasoning).
- **R5. The blob only.** The raw stays in git, outside the medium. This is cheap, but the read path cannot give the file back.

### 4.5 The act's grain

- **One act for each reading of a file.** A file lands whole. At about four keys a part, 29 code files and 35 markdown files exceed 256 facts (Runs B and C).
- **One act for each commit.** A commit lands whole. The walk counted about 422 facts for 0e6182b9 (`walk-1-stamp.md:64`).
- **A series of acts for a large file.** The door gives act-ids ahead (`walk-1-stamp.md:55`, citing `door.clj:22-26`). A reader can then see half a file, since the store has no act that spans acts.
- **The bound raised.** This is the first cost, and it is free before the first real record. The bound limits the rows of one event (`declaration.clj:74-77`), and in the base each row is written on every task. What Rama does with large events there is not checked.
- Sid: the tool decides what goes into one act (‹85a64cfb›). The bound is the store's.

## 5. Consequences that cross decisions

- **One thing, one id, in every layer.** The base is the last commit, and a person's work is a layer above it (`LOG.md:146`). A read takes the nearest layer that has a fact at a place (`README.md:535`). So ids must be ones that a layer above can make or find: from names (I2, I5) or from a registry (I3), and never from content (I4) (my reasoning).
- **What the road writes into the base is copied and shown.** It is written on every task (`README.md:905-922`). As built, everyone can read it, a person who is not signed in too (`first_act.clj:146`). The repository is closed source: the project's `CLAUDE.md:92` says "the repo is closed source (Sid, 2026-08-10)". And K14, whether the base is the world, is open. Whether the road reads all of `history/` (26.9 MB, Run A) and the vision log into a base that everyone may see sits beside decision 2, as decision 1's inlet or decision 4's layer. Nobody has decided it.
- **Who wrote a commit is a person's data.** The store forgets nothing yet, and a delete is a retract (`README.md:718-719`).
- **The grain can change later; the ids cannot.**
  - Git keeps every version and who wrote each line. So a coarse cut can be made finer later, from git, or from the raw kept in the store. A new level under an existing part leaves that part's id alone.
  - What cannot change later is an id that others point at, because each pointer is a fact in someone else's layer.
  - Sid's grain line holds here for the data that other makers attach to a part: "we can combine these to make a bigger one but can't decompose it to atomic units later on becuaes the additional data that is attached is not there" (‹e36008e1›). That data attaches to whatever ids exist at the time (my reasoning).
  - His default is the side "that captures more" (‹5ff89890›). A coarse cut with the raw kept drops nothing (my reasoning). Another reading of his line puts blocks first; I name it in section 10.
- **The three identities that Sid saw.** "it does come out with 3 identies .... what is the native one and what is derived" (`LOG.md:173`). In the store they can be three things (my reasoning): the entity id, for the thing; the git blob, a native version, as a fact on the file; and the fact id, the store's version of one place.
- **Decision 1 bears on the ids.** If the cut is a record that an inhabitant can change (decision 1's ends B or D), ids made from the cut's own output change when the cut changes. Ids made from the source's names, such as the path, ns/name and heading, do not (my reasoning).

## 6. What hangs on what

- **The ids on the cut.** Only a part that the cut makes needs a name.
- **Decision 3 on the ids.** Under I5, decision 3 can start with "the same name is the same thing" and keep things through renames later, with no change to the id rule. Under I2, keeping a thing through a rename turns the scheme into I5 anyway.
- **The act's grain on the cut and the keys.** It is their product. The bound on one value hangs on the cut and on how the raw is kept.
- **What points at a thing on the relations.** It needs single-valued relations, and marks written before the first act.
- **K5 on the road.** It matters only for a UUID key, or for a grammar that a gate checks.
- **Decision 4 on the commit.** If the commit is no entity, the commit at which a fact was read is a fact on the file, or it is the act's session. A read gives who wrote a fact, its permission and its time, and not its session: the whole act behind a fact is not decided (`query.clj:21-22`; `README.md`, 11 and 12 October, entry 7).
- **Store questions it touches, none decided:**
  - K5;
  - K14, who sees the base;
  - K15, `softland.store.x`;
  - the three bounds (65,536, 256 and 32);
  - the facts written before a mark;
  - who may write the base's marks. As built, anyone who may act in the base can (`decision.clj:283`, `who-may-write-fault`).

## 7. What Sid has said

**His words.** These are lines that he typed, and his own notes in the vision log.
- **The unit of code.** "I think in code we can say that the atomic unit is the function right??? unlike block strings which can be "" and still be meaningful." (`LOG.md:193`). Also "The file name is the parent ..." (`:195`), and "if we have them independent as the atomic unit in terms of functions then we can group and relate and situate them ..." (`:197`). All three are notes of 11 June.
- **Code's identities.** "the native unit of code is like path, but also git level changes so it has many default identities what do we import and then save as in the container?" (`LOG.md:170`). Also "it does come out with 3 identies .... what is the native one and what is derived" (`:173`), and "So if we want all these then we need metadata and these features define what the actual identity should entail ???" (`:191`).
- **The unit of text.** "so in roam its a block" (`LOG.md:112`), the outliner (`:126`), and the text-artifact as "the smallest text unit" (`:141`). These are notes of 6 June.
- **Thin records.** "we don't have to fatten this we can add another layer that adds the missing data for different layer" (`LOG.md:144`).
- **When to cut.** "Do we break down the data on ingest time? or during active layer time?" (`LOG.md:119`). This is his own open question.
- **Base and active layer.** "the last commit is its base layer whatever i build on top is in the active layer" (`LOG.md:146`).
- **Raw and native.** ‹b22e6278› (6 June) and `LOG.md:257` (4 July).
- **The unit and the levels above it.** "What is the [unit of code] that we should store and version ... the question is not only about unit of code but the higher levels as well." (`LOG.md:240-241`). The log keeps this as a model's transcription of his handwriting.
- **Grain.** ‹e36008e1› (23 September). **Defaults.** ‹5ff89890› (12 October). **The tool decides the grain.** ‹85a64cfb› (9 October, dictated).
- **Ids.** At scale, ‹d4fd70ba› (6 October). Across many stores, ‹1082f32d› (18 September).
- **Schemas by group.** ‹6b8c2421› (29 September).
- **The base.** "The base there means like the code that we are writing", and "Maybe like ingestors" (‹4123e23c›, 13 October, dictated).
- **The spectrum.** "as i have said many times the softland system is providing the full spectrum" (11 and 12 October, entry 1, `README.md:503-509`).

**His picks on sessions' forms.**
- "5 (keys as ids): confirm" (24 September). It is one paste of a session's "Copy answers" text (‹c985696f›). Each pick was the session's own lean (`PROGRESS.md:333-344`).
- K4 B, the namespace of the store's keys, and his word "softland.store" (`README.md:422-466`).

**His questions, which are not positions.**
- ‹ac8e2611› and ‹a5718452› (24 September) ask what keys mean, how a key differs from an entity, and which id is meant.
- ‹22d80d6c› (30 September): "what is the key and value why is there there only one key".
- ‹26f4cfab› (30 September): "the act can have one factor or many and uh, this depends on what tool's or group's act is this correct??"
- ‹e164cb14› (15 September) asked what a session's "A thing is an id. Nothing about it is intrinsic, and there is no is-a on things" means. His next line was "hmmm its both a bit scary but also so powerful" (‹9cc5b6c4›).

**A session's thinking that he quoted.** In ‹2b56c120› he quoted, as more readable than the session's answer, a paragraph that his line presents as the session's own thoughts: "the pointer only works if its target is stable under that name forever ... (gate version numbers, partition offsets, unversioned grammar keys, content-derived entity ids)". The record holds that paragraph only in his line. He praised how it read, not what it said.

**Sessions' words that are easy to read as his.**
- "Keywords were the rig's choice", and the split "a key is a keyword" («a78296bc», 6 October).
- "Keys are ids; word and grammar are facts about them" (the rulings file).
- "a function and a markdown section are each an entity, and keys are readable words such as `:name`, `:doc`, `:text`, `:in-file`" («ca81bcf6», tonight).
- The cost lens («cca75ac4»), and the question itself («9a48928c»).

## 8. What the examples found

These are findings only.
- **The old server's markdown.**
  - It was "a line-based subset parser" (`markdown_adapter.clj:5`) that cut headings, list items and paragraphs (`:93-97`).
  - Its ids were made from a hash of the file's source and content, the adapter's name, and a positional path (`:20-23`; `object_container.clj:216-219`). So an edited file got all new ids.
- **The old server's Clojure.**
  - It cut top-level forms and named each by its name; a second form of one name got `~2` (`clojure_adapter.clj:144-158`).
  - A defmethod's name took its dispatch value (`:185-190`), and unnamed forms were named by position (`:160-171`).
  - It kept comments as spans, and kept the text as the blob's own piece (`:198`).
- **The old server's history.**
  - Continuity was a separate `:supersedes` relation: the same name in the same path, or the same hash across files. Unnamed forms were left out (`code_import.clj:158-163, 251-300`).
  - It kept a version out of an id on purpose (`code_import.clj:26-30`).
- **The rig.**
  - A file is a thing and a named form is a thing, and "a file at a revision is a fact on its file's thing, never a thing of its own" (`material.clj:12-14`).
  - It derived ids under a key, so that an id does not confirm a guessed path (`:38-49`).
  - It wrote one fact for each form, with a map, and the file's whole text as one fact (`:451-460`).
- **The paper test.** Sid declined it on 3 October. It laid out the rig's reading as the repository's road, and it is "NOT IN USE" (`src/proposal/backend-on-rama-2026-10-02/CONTRACT-above-the-act.md:3-5, 96-103`).

## 9. My default for the first road

This is a default, not a decision.

1. **The cut: the file and its top-level parts, cut so that the parts tile the file (E2 with R4).**
   - For code, each top-level form is a part, with the comments and blank lines before it. For markdown, the text before the first heading is a part, and so is each heading's own text up to the next heading of any level; a section links to its parent section.
   - A part over 65,536 bytes is cut again at blank lines, into parts of its own (15 markdown files).
   - No block below a heading is an entity on this road, and no commit and no person is one.
   - Reasons:
     - It is Sid's unit for code, with the file as its parent (`LOG.md:193-197`).
     - The raw and the native become one set of facts (‹b22e6278›, `LOG.md:257`).
     - Every code form, and all but 15 markdown pieces, fit in one value (Runs B and C).
     - A finer level can come later under these parts without changing their ids (my reasoning).
   - Costs:
     - A paragraph or a decision entry cannot be pointed at as itself, and an edit of one paragraph replaces its whole section's text.
     - It departs from Sid's own picture of text, in which the block is the unit (`LOG.md:112, 126, 141`). I would name that departure to him, not hide it.
2. **The ids: UUIDs made from the source's names when a part is born, and kept after (I5).**
   - The file's id is made from the repository and the path.
   - A named form's id is made from its file, its kind and its name, with a defmethod's dispatch value.
   - An unnamed form's id is made from its file, its head and its ordinal among the unnamed forms with that head.
   - A section's id is made from its file, its heading path, and its ordinal among siblings of one title.
   - The converter reads the previous reading of a file before it writes.
   - Reasons:
     - Sid asks how keywords could serve 10^x ids (‹d4fd70ba›).
     - Two programs share 20 ns/name pairs in this repository (Run B), and a path is no keyword (act 5).
     - The id must be the same in every layer (`LOG.md:146`; `README.md:535`).
     - A function that can be moved and regrouped, and stays itself (`LOG.md:197`), needs an id that does not change when its name or file changes. I5 allows that and I2 does not.
     - The converter's read also gives `:replaces` its fact ids.
   - Costs:
     - The converter is not a function of git alone.
     - Once kept, the way of deriving the ids is for ever.
     - Anyone who knows a path can make the id of a part in the base.
3. **The keys: readable keywords, in a namespace that names the vocabulary, and none under `softland.store`.**
   - There is one key for each attribute, and few of them.
     - A file carries its path, its blob, and the commit it was read at.
     - A part carries the part it belongs to, its order (a text that sorts between its neighbours), its text, its kind and its name. The first key is single-valued, shared by code and markdown, and marked in the base before the first act.
   - No docstring, arglist or require key goes in on this road; another layer can add them (`LOG.md:144`).
   - K5 stays open: there is no UUID key, and a key's word or grammar, when wanted, is a fact about the keyword.
   - Reasons: section 4.3.
   - Cost: five facts a part and three on the file, so about 50 parts in one act under today's bound.
4. **The values.** Code and markdown go in as text, the exact bytes. The bound stays as built.
5. **The act.** One act for each reading of a file. A file whose parts need more than 256 facts goes as a series of acts, named ahead. Sid can rather raise the bound, so that each file lands whole; that is a question for him.

The questions I would put to him, in his words' order:
- **The unit of text.** A markdown page can be cut at its headings, or down to each paragraph and list item. Your Roam picture makes the block the unit. Should the road cut at headings now and add blocks under them later, or cut blocks from the start? My default: headings now.
- **A moved function.** When `next-stamp` moves into another file, is it still the same function? If yes, the reader of the repository must remember each id: it reads what it wrote before it writes. If no, each id is made from the name, and nothing is remembered. My default: yes.
- **Keys as words.** Keys stay readable words such as `:part/text`. That works whichever way you later decide what a key is. This is a default I would tell him, not ask him.

## 10. Where I am unsure, and what I did not check

- **The unit of text.** I am not sure whether Sid wants the block now. His notes draw it as the unit (`LOG.md:112, 126, 141`), and his grain line pushes to the finer side (‹e36008e1›). My default goes coarser, for the reasons in section 5. That is a judgment of mine, and he may weigh it otherwise.
- **Whether an id should survive a move.** Should it outlive a rename and a move (I3, I5), or be made again from the source alone (I2)? This is the core that decision 2 shares with decision 3. I lean to I5. A future in which many stores read the same repositories from snapshots pulls toward I2.
- **The first road's store.** I do not know whether it is kept. If it is not, almost all of this can be tried first.
- **Rama.** I did not check how Rama handles many rows in one event, and rows of megabytes, in the base's microbatch with a copy on every task.
- **My counts.** They come from line heuristics, not from a Clojure reader or a markdown parser. A docstring line that begins with `(` at its first column makes a false cut. Underlined headings are not counted. The roughly 900 forms without a def head include custom def macros.
- **The lens.** The fourth and fifth kinds of cost in section 3 are my reasoning; no run tested them.
- **Grammars.** The limit on grammars as facts rests on a session's finding that Sid has not seen (`README.md:858-861`), together with his entry 2 of 13 October.
- **What the base should hold.** Whether this closed-source repository, its history and its vision log should go into a base that everyone sees is not decision 2's question, and nobody has decided it.
- **The repository's identity.** I have not thought it through: its name, or its first commit.
- **What I did not read.** I did not read decision 4's page, Inland's material reader, or the sessions' texts of 2 and 3 October on adapters, beyond what decision 1's page cites.
