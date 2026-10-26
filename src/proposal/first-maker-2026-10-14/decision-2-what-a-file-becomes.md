# Decision 2: what a file becomes

Decision-2 agent (Opus, xhigh), HEAD c90c7bcc, corrected after `checks/review-d2-report.md`. Nothing here is decided.

Labels:
- **dN** (a row label) and **maps n** (line n): `runs/decision-2-maps-output.txt`
- **detail n**: line n of `runs/decision-2-detail-output.txt`
- **fixes n**: line n of `runs/decision-2-fixes-output.txt`
- **review n**: line n of `checks/review-d2-runs.txt`
- **act n**: the walk's act n
- **d3 run n**: run n in `runs/decision-3-maps-output.txt`

## (a)

When the road reads a .clj or .md file, which parts become entities, what is each one's id, and what is a key?

## (b) The ends

**The lens.** Each end below is a value inside the depot-record forms of `:e :k :v` (`src-store/softland/store/act.clj:169-171, 343-347`). The log keeps that value for ever (`src-store/README.md:746-751`), so each end fits none of the three costs unless it is marked otherwise. A store that is thrown away hardens nothing (`:941-945`).

Fitting none still costs something (my reasoning):
- A fact in another person's layer that points at an old id has to be rewritten by its own writer.
- A reader of history needs a link from old ids to new ones.
- The converter has to keep its old derivation, or it cannot find old things.

So the "72" below counts only the converter's own facts.

**What lands in the base.** The base is copied to every task, and as built it is shown to everyone (`src-store/README.md:911-922`; `gate/first_act.clj:146`).
- My scan takes tracked .clj, .cljc and .md files (`runs/decision-2-maps.clj:28-33`), `history/` (26.9 of 49.0 MB, review 7-8) and the vision log included.
- The repository is closed source (`CLAUDE.md:92`).
- A commit's author, written as a fact, is a person's data, in a log that cannot yet forget (`src-store/README.md:718-719`).

Which layer, then, is a question for decision 4's page.

### Parts

- **A. The whole file as one value.** Too big for 129 files (132 as the store measures; act 12; decision-1; `checks/checker-d1-sizes-as-text-output.txt`).
- **B. The file, the namespace, and each named form; for markdown, each heading** (acts 1, 9).
  - A name used twice is refused `:place-named-twice` (d2-7). `act.clj` declares `conform` before defining it. `read_path.clj` defines `print-method` and `print-dup` once for each of two types (`src-store/softland/store/read_path.clj:313, 316, 332, 335`), so their ids need the dispatch value.
  - 28 code files need more than the 256 facts one act may hold (maps 4). Raising that limit is depot record.
  - B drops the 213 top-level forms that have no name (maps 7), along with comments, blank lines and the order of parts, so the store cannot give a file back.
- **C. Each paragraph too.** A paragraph's id would be its position, or a minted id matched across commits (Id C).
- **D. Parts that tile the file, with an order key** (the reviewer's end). The raw file and its parts are then one set of facts. `stamp.clj`, cut into two tiling parts, passes parse and the door's check (review 153-155). Unnamed parts need ids, and an insertion rewrites the order keys (my reasoning).

In a `.cljc` file, one definition can have two bodies (`walk-1-stamp.md:67`). My reader keeps only one platform's branch (`runs/decision-2-maps.clj:57`), so it would lose the other body (not run).

### The id

- **A. A keyword made from the names.** Every `:<ns>/<name>` has the keyword form (maps 9), but that does not make it unique:
  - 21 such keywords are defined in two or three files each (fixes 8-10). With one act per file, the copies sit in different acts. A fact that replaces nothing is never stale (`gate/decision.clj:237-240`), so the second copy writes over the first one's row, unrefused (d3 run 1). My scan counted repeats only within one file (`decision-2-maps.clj:85`).
  - Headings are names too, but 59 files repeat a title, 18 repeat a whole heading path (maps 15-16), and headings hold spaces, which a keyword cannot (`act.clj:267-270`). A raw path cannot be a keyword either (act 5).
- **B. A UUID computed from the path and the name** (acts 1, 9). No read is needed.
- **C. A random UUID, with the path and the name as facts on it.**
  - A rename changes 3 facts, against 72 under A (d3 run 4). But that run fixed the moved file's ids by hand (`runs/decision-3-maps.clj:165-177`). Without decision 3's rule for a move, C makes new things and as many changes.
  - It costs a read before each write.
  - The path key has to be marked one act before the first file. If both are in the same act, the path goes into no order by value (fixes 5); split into two acts, it does (fixes 2-6).
  - No rule makes the first writer win, so two converters that meet one new file mint two ids (not run).
  - A store started again from git gets new ids.
  - The base's 32 marks (`door/declaration.clj:127-133`) are shared by every system tool, so one shared "part of" key spends one mark where a key for each vocabulary spends several.
- **D. A content hash.** Every edit makes a new id (d3 run 4).
- **E. An id made from the names when a thing first appears, and kept after** (the reviewer's end). It repeats on a rerun like B, and leaves decision 3 open like C. After a rename the id must be read, which needs C's marks. A new thing that takes a renamed thing's old name would compute that thing's id, so every new id has to be checked by a read (my reasoning).

A, B and D also decide when two commits hold the same thing, which is decision 3's question.

**Across layers.** A read takes the nearest layer that holds a fact at a place (`src-store/README.md:533-536`), so one thing needs one id in every layer. Each end meets this differently (my reasoning):
- Under D, an edit in a layer above gets a new id, and the base's version shows through.
- Under C, the layer above has to read the base's registry.
- Under B, and under E until a rename, the layer above can compute the id.

### The key

- **A. A keyword whose namespace names a vocabulary,** such as `:code/doc`. Its word can be a fact on it today (d2-4).
  - "What points at a thing" matches the whole value of a marked key (`record/rows.clj:55-63`; `read_path/sources.clj:203-207`), so a relation has to be one fact whose value is its target.
  - The digest of `#{"296a2a7b"}` is not the digest of `"296a2a7b"` (fixes 11). So the walk's `:md/cites-sid #{…}` (act 9) is never found from the line it cites.
- **B. K5, a UUID key.** It is refused `:malformed` (act 3), so allowing it is depot record.
  - The K5 row prices this change by Rama's migration (`src/proposal/backend-on-rama-2026-10-02/decisions/OPEN-QUESTIONS.md:111`). Since 13 October, though, the tables are made again by running the gates over the depots (`src-store/README.md:842-861`). So the cost is a second act version, and old facts keep their keys (my reasoning).
  - A gate can check a value against its key's grammar only from the act's own layer, or from code. Order is local, and a gate reads no table of the other gate (`src-store/README.md:899-902, 858-861`).

**Missing from the cut:**
- which keys the base marks (under C and E, before the first file);
- a rule for a name used twice;
- whether the raw file is kept.

## (c) What Sid has said

- "how can it be a keyword????? we can have10^x no of ids where will we find the keywords for them???" ‹d4fd70ba›. This is about ids. Code and headings have names, but the names repeat (Id A), and paragraphs have none.
- On a session's form he picked "5 (keys as ids): confirm". He has not ruled K5 (`src-store/README.md:159-164`).
- "the tool is deciding how much of their different actions are put together inside a single value or inside a single act" ‹85a64cfb›. This says who decides the grain, not which grain.
- "we can combine these to make a bigger one but can't decompose it to atomic units later on" ‹e36008e1›. This is a session's point, restated.
- "we keep both the raw but also store it in softland way" ‹b22e6278›. He said it of transcripts.
- His June notes, notes not rulings, from his own depot. `who` confirms only the last as typed (‹86db6d30›); the rest are inside long prompts:
  - "each block is the base layer ... A bunch of block with children is a section" (`vision/LOG.md:126`; `:112`, `:141`). My default stops at headings, which departs from this picture.
  - "the atomic unit is the function"; "The file name is the parent" (`:193-195`).
  - Functions grouped apart from their file, "in differnent ways" (`:197`).
  - "the last commit is its base layer whatever i build on top is in the active layer" (`:146`).
  - "3 identies .... what is the native one and what is derived" (`:170-173`).
  - "we don't have to fatten this" (`:144`).
  - Break it down "on ingest time? or during active layer time?" (`:119`).

## (d) What the examples found

The old server made a markdown part's id from the content hash and the part's position, so any edit gave new ids (`src/app/server/ingest/markdown_adapter.clj:20-23`; `src/app/server/rama/object_container.clj:216-219`).

## (e) Default, not a decision

- **Parts: D, over B's named parts.** A `declare` is folded into its definition. A method's id carries its dispatch value. One act per file, split at 256 facts.
  - Why: the store can give the file back, and it keeps what B drops. His "the side of spectrum that captures more" ‹5ff89890› was said of reads; applying it here is my reading.
- **Id: E, for the trial and the kept store alike.**
  - Why: the trial then runs what the kept store will rely on, and no id changes at the moment of keeping. My first default, B for the trial and C once kept, did neither.
  - Cost: a read before each write, with the path and name keys marked first. A new thing that takes a renamed thing's old name would otherwise get that thing's id. A thing deleted and later re-added under its old name gets its old id back (my reasoning).
- **Keys: A, under `file`, `code` and `md`.**
  - What is carried: path, blob, name, parent, order and text, with each relation as one fact.
  - Docstring, arity and hash come from new read-path code, or from a tool working on what a read gives. A gate reads nothing of a plain value (`door/declaration.clj:273-277`), and a new query word is added in code (`read_path/query.clj:52`).
  - K5 stays open.

## (f) Questions for Sid

1. A code file holds functions, such as `next-stamp`. Each can be a thing of its own that others point at.
   - **What happened:** cut that way, four of the store's own files name one thing twice. `act.clj` announces `conform` and then writes it; `read_path.clj` writes `print-method` for each of two types. The store refused that act.
   - **What is needed:** D, each function and heading is a thing, and the file's other lines are kept beside them in order; or C, each paragraph is a thing too.
   - **Default:** D, with the announcement folded in and the type added to the name. If you say nothing, D.

   Source: d2-7; detail 29-32; review 153-155.
2. Each thing is filed under a label. `next-stamp`'s label can be remade from its name each time, or made from its name once, when it first appears, and then kept.
   - **What happened:** when `stamp.clj` moved folders, labels remade from names changed for the file, its namespace and its eight functions: 72 changes. A kept label needs 3, but only once something has recognised that the moved file is the same file, which is a separate question. A kept or random label does not lower the count by itself.
   - **What is needed:** B, remade every time; or E, made once and kept.
   - **Default:** E, which needs a look-up before each write. If you say nothing, E.

   Source: d3 run 4; `runs/decision-3-maps.clj:165-177`; fixes 2-6.
