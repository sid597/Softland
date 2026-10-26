# Decision 3: the same thing across commits

Decision-3 agent (Opus, xhigh), HEAD c90c7bcc. Nothing here is decided. A "run" is a `runs/decision-3-*` file. `decision-3-maps.clj` runs the store's pure checks with no cluster, and the permission's standing is not run.

**(a)** How does the road know that a function or a section is the same thing at the next commit, so that its new fact replaces the old one? What do a rename, a move and a deletion become?

**What the store does today**

- `:replaces` names a fact at the same layer, entity and key only (`src-store/softland/store/gate/decision.clj:249-257`). So the entity id that the converter writes settles sameness.
- A fact with no `:replaces` is accepted with no read, and `writes` gives a whole row at its place. In run 1 a map stands in for the module's put (`decision-3-maps.clj:109`). `:replaces` adds two refusals: a write after someone else's (run 1), and act 7 sent twice (run 2).
- The old fact id comes back from a read (`src-store/softland/store/read_path/answer.clj:127-146`). It can also come from the inlet's own record: the door gives act-ids ahead of sending (`src-store/softland/store/door.clj:22-26`), so the inlet needs a read only after a restart, or when someone else has written (my reasoning).

**Missing from the cut** (my reasoning)

- Two questions are inside this one: what the store holds after a rename, and who judges that it was one. Git records no "renamed" for a function, so the converter judges here. A person can claim or deny sameness in her own layer (a session's frame that Sid pasted, `src/proposal/frame-2026-09-15/PICTURE.md:82`).
- The commit must be a fact before anything can be based on it (`src-store/README.md:172-173`; `src-store/softland/store/act.clj:159`, `:179-180`). A session's reply based acts on the commit («ca81bcf6»), and time travel's "name a point" needs it (the queue's V53).

**(b) The ends.** The real move is 085bca4a. Git finds all 13 of its file moves at 91 to 99 percent (`decision-3-085bca4a-renames.txt`). In 12 of the 13 files the `ns` form changed beyond its first line, and 7 files had 11 changed definitions; `stamp.clj` had neither (`decision-3-085bca4a-output.txt`, approximate).

- **A. The id is the name.**
  - The day's move is about 764 retracts and 764 new facts. `gate/batch.clj`'s move, 2×192 (run; my doubling), exceeds one act's 256 (`src-store/softland/store/door/declaration.clj:74-77`), so a reader between two acts sees half a move.
  - A name defined twice in one file is refused (`act.clj:351`, `:400`; `decision-2-maps-output.txt:26`).
  - A name used again after a deletion brings back the old thing, because a retract's row stays (`src-store/softland/store/record/rows.clj:237`). The merge is silent.
- **B. The id is the position.** One insertion renumbers every later part (walk 2).
- **C. The id is the content.** Moving `stamp.clj` keeps its eight ids, but every edit makes a new id that needs a link (run 4). A person's edit in her own layer is then another entity, and it cannot stand over the base's version (`answer.clj:5-6`).
- **D. A link fact** (`:code/was`) passes (run 4). The walk's act 8 put the link in `:based-on` instead. That part holds what the maker saw (`act.clj:192`), so a link there looks like a read.
- **E, missing from the cut: the store's own id, with the name as a fact on it.**
  - Moving `stamp.clj` takes 3 replaced facts once the file and the namespace are matched. The run matches them by hand (`decision-3-maps.clj:166-175`).
  - Finding a part by its name needs `:code/name` marked before the first file is written (`decision.clj:441-443`; `src-store/README.md:600-602`). The system can write the mark (run 6).
  - Open: the id is random, so a second run or a fork mints other ids; or it is derived at birth, so it is the same every time (‹1082f32d›: ids that "never collide").

A refinement for A, C or E is a ladder. Match each file by git's rename detection, which the old server turned off (`src/app/server/ingest/code_import.clj:106`), then match each definition by its bare name inside that file. On 085bca4a the ladder avoids every split (my reasoning).

**Sections.** A section's name is its file and heading path.
- A retitled heading is a new name.
- Two equal headings under one parent collide.
- Sections that dissolved into docstrings at fe592411 are retracted, with nothing joining them to the docstrings.
- "It replaces entry 4 of 8 October" (`src-store/README.md:1007`) joins two different things, which `:replaces` cannot say.

**The lens.** Ids, link keys and marks are values that no gate rule reads, so they fit none of the three costs.
- Later acts pay for the inlet's own facts, not for the pointers that others hold. After a change, every reader knows both conventions, in logs that are never cut (`src-store/README.md:685`, `:746`).
- The lens holds in three places. A `:replaces` across places would be a new version of the act (`:941`). Deletion by retracts rests on the kept rule "a fact with no value is a retract" (`:133-134`). E's mark adds a table.
- A layer for each commit runs into the limit on builds-on (`declaration.clj:120-125`, `:372`).

**(c) What Sid said**

- June notes, not rulings:
  - "it does come out with 3 identies .... what is the native one and what is derived" (`vision/LOG.md:173`);
  - "the path is actually in itself context of where the function is" (`:193`);
  - functions "independent as the atomic unit" (`:197`);
  - "versioning is its own layer ... there can a explicit commited one" (`:203`);
  - "the last commit is its base layer whatever i build on top is in the active layer" (`:146`).

  The log is his depot, but the record has these lines only inside long prompts (‹ac7f7a11›, ‹5940b98d›), so `who` cannot confirm that he typed them.
- `:replaces` is optional (`src-store/README.md:125-126`). For that entry the record holds only a session's reading of his ticks (`:90`).

**(d) What the examples found.** The old server gave each edit new ids. It joined versions by an equal name in one path, or by an exact hash (`code_import.clj:170-178`, `:276-316`).

**(e) Default, not a decision.** E, judged first by qualified name, then by an unchanged body.
- This rule splits 12 namespaces and 11 definitions of 085bca4a. The ladder avoids those splits.
- It also splits the eight in-place renames of c296a459, from class to depot, because each one changed beyond its name (`decision-3-c296a459-output.txt`). The unchanged-body rung catches the rarest case.
- Reasons: a reference survives a rename, and a move costs a few facts.
- Costs:
  - the judgment of the matching, and its false merges;
  - the mark, which must come before the first facts;
  - one more lookup for anyone who knows only a name;
  - a rule for a name defined twice;
  - a second id for a thing born in a person's layer and committed later, unless something joins the two (`vision/LOG.md:146`).

**(f) Questions for Sid**

1. On 8 October, 13 files moved into folders, and their functions stayed the same functions. If a function is its name, the move deletes each one and makes it again. That is about 1,500 changes in several acts, so a reader in between sees half a move. If each function has its own number, with its name written on it, each file costs a few changes. Until real records are kept, either choice can be thrown away. After that, others point at these functions, and a change splits every long-lived thing in two. **Needed:** A) a function is its name; E) it has its own number. **Default:** E. If you say nothing, E is built on a store that is thrown away.
   Source: `decision-3-085bca4a-output.txt`; `src-store/README.md:941`.
2. When the importer changes a function, it can name the text it replaces, or write over it. Naming it lets the store refuse the change if someone else changed the function first, or if the import ran twice. **Needed:** R) name it; W) write over it. **Default:** R. If you say nothing, R is built.
   Source: runs 1 and 2.
