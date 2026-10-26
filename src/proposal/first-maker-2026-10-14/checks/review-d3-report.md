# Review of the page on decision 3: what it misses, what it gets wrong, and what it has that I did not think of

The reviewer of decision 3 (Opus 5.5, max effort), phase two. I read the page once: `src/proposal/first-maker-2026-10-14/decision-3-same-thing-across-commits.md`, 39 lines and 859 words (`wc -w`). I also read its run and the run's output (`runs/decision-3-maps.clj`, `runs/decision-3-maps-output.txt`). My own thinking, written before I saw the page, is `checks/review-d3-own-thinking.md`.

How to read the marks. "Page N" is the page's line N. `path:line` is a file at HEAD c90c7bcc. ‹id› is a line Sid typed, and «id» is a session's text. "My reasoning" is mine. I made one new run for this review. It is described inside the first finding of "What the page gets wrong", and it used git only.

## What the page misses

1. **Sid's own notes on the identity of code.**
   - What the page cites. Page 24-28 cites ‹d4fd70ba› and `src-store/README.md:125-126`, and reports no line since September that rules on sameness. The page cites nothing of `vision/LOG.md`, although the task's sources name lines 152-260 of it as the identity thread of the code ingestor.
   - What those lines hold:
     - "it does come out with 3 identies .... what is the native one and what is derived" (`vision/LOG.md:173`);
     - "the path is actually in itself context of where the function is" (`:193`);
     - functions "independent as the atomic unit ... group and relate and situate them with other function groups in differnent ways" (`:197`);
     - "versioning is its own layer ... there can a explicit commited one which we want to do e.g like git does" (`:203`);
     - "the last commit is its base layer whatever i build on top is in the active layer" (`:146`).
   - Why it matters. These are his notes of June, not rulings. But they are his framing of this exact question, and they bear on end A against end E. Under A a function's grouping is inside its id, and `:197` asks for functions that can be grouped again. Without these lines, Sid gets the question without his own words on it. My reasoning.
2. **How a file is known to be the same file after a move.**
   - What the page has. The page's rule matches parts by qualified name, or by a body that has not changed (page 32). It says nothing of files. Page 30 notes that the old server turned git's rename detection off (`src/app/server/ingest/code_import.clj:106`), and does not weigh that detection for the road.
   - What git gives. Git finds each of the 13 moves of 085bca4a, at 91 to 99 percent similarity (`git show -M --summary 085bca4a`, run in phase one).
   - What a ladder could do with it. It could first match each file by git's answer, and then each definition by its bare name inside the matched file. Such a ladder keeps the namespaces and the definitions that the page's rule retracts (findings 1 and 2 under "What the page gets wrong"). My reasoning.
3. **Sections.**
   - The decision is about "a function or a section". The page's ends, runs, default and questions are all about Clojure definitions. Sections appear only in end B (page 17) and in run 5.
   - The page does not say how its default applies to a section:
     - what a section's "qualified name" is;
     - what a retitled heading does;
     - what two equal headings under one parent do;
     - what happens to sections dissolved into docstrings, as at fe592411 (`git show --stat fe592411`);
     - and what to do with a line like "It replaces entry 4 of 8 October" (`src-store/README.md:1007`). That line is a supersession between two different things, which the store's `:replaces` cannot say (walk-2).
   - My reasoning.
4. **The commit itself.**
   - The page does not say what a commit becomes in the store.
   - The session default that page 32 cites as end A, «ca81bcf6», included "with the commit as what the act is based on".
   - 6 October, entry 11 says that an outside thing is recorded as a fact first, and that its basis is that fact (`src-store/README.md:170-178`). And `:based-on` takes fact ids, not hashes (`src-store/softland/store/act.clj:159`, `:179-180`).
   - Two other things need the commit as a thing: Sid's "explicit commited one" (`vision/LOG.md:203`), and "name a point" for time travel (REFINEMENTS R3, a session's reading; the queue's V53).
   - The page drops this part of the question without saying so.
5. **When the choice stops being reversible.**
   - Page 36 says "If you say nothing, B is built on a store that is thrown away." That holds until the first real record: 13 October, entry 6 says of its rule "This starts with the first real record" (`src-store/README.md:941-951`).
   - From then on, the ids are in a log that is never cut (12 October, entries 2 and 3, `:685-751`), and others hold pointers to them in layers that the inlet cannot write. A change then splits every long-lived thing into a before and an after.
   - The page does not say this, so its question reads as low-stakes. My reasoning.
6. **Where the lens holds.**
   - The task asked which of the three costs each option is, and where the lens holds and where it does not. Page 22 says that every end fits none, and that E's mark adds a table.
   - It does not say where the lens holds:
     - a `:replaces` across places would be a change in what a gate accepts, so a new version of the act (`gate/decision.clj:249-257`; 13 October, entry 6);
     - a layer for each commit would meet the gate's bound on builds-on (`door/declaration.clj:120-125`, `:372`);
     - a deletion by retracts rests on the kept rule "a fact with no value is a retract" (`src-store/README.md:133-134`).
   - Nor does it place the options that are not ends: an edit with `:replaces` or a blind write (both inside act version 1), lineage in `:based-on`, the forms of a deletion, and the commit as a fact.
   - My phase-one text, section 8.
7. **Continuity claimed by others.**
   - Page 12: "Here the converter judges." In a store of layers, an agent or a person can claim continuity, or deny it, in their own layer, as a fact with their own `:who`. The page leaves no place for such a claim.
   - A session's frame of September says so: "continuity between minted things is a claim too, by ingest through name and hash, or by a person overriding in their layer" (`src/proposal/frame-2026-09-15/PICTURE.md:82`, «8af659ca»; Sid pasted it and did not type it).
   - His line on binding gives "what happens when the pointed-at thing moves, deletes" to groups, with "a default AND proof/compute that the dependency chain will be done" (‹d048babc›, a long prompt; the record cannot tell his typing from a paste).
   - Under E, a thing born in a person's active layer (`vision/LOG.md:146`) and committed later gets a second id in the base, unless something joins the two. My reasoning.
8. **How E's id is made.**
   - The run uses instance UUIDs (`runs/decision-3-maps.clj:166-167`), and the page does not say whether a real id is random or derived.
   - A random id makes a second run of the inlet, a second store, or a fork mint other ids, unless each reads the store first.
   - An id derived once, at birth, from the source, the birth commit and the birth name gives the same ids every time. A fork then lands on its upstream's places.
   - This bears on federation (‹1082f32d›: ids that "never collide even if there are gazzilions of rama"), and on the page's own question 2, "if the import ran twice". My reasoning.
9. **A name used again after a deletion, and the reason for "new when unsure".**
   - The page's rule makes a new part when it is unsure (page 32). That is the side that ‹5ff89890› picks: "we are not choosing the destructive side by defualt". The page does not give that reason.
   - Nor does it say that end A is not wholly the safe side. Under A, a name used again after a deletion brings back the retracted places as the same thing, because a retract's row stays "so that a later fact can replace the retract" (`src-store/softland/store/record/rows.clj:237`). Over years, that is a silent false merge. My reasoning.
10. **The size of the real commit against one act.**
    - The page's 72 and 3 (page 16, 20, 32, 36) are for one file. 085bca4a moved 13 files, with about 190 top-level forms (phase one, a grep).
    - Under A that is about 1,500 facts, in six acts or more. `gate/batch.clj` alone needs about 384, more than the 256 of one act (`door/declaration.clj:74-77`). So one file's move cannot be one act, and a reader between two acts sees half a move.
    - My arithmetic, on walk-1's cut A.
11. **The inlet can know its old fact ids without a read.**
    - Page 32 counts "a read before each write" as a cost of the default, and page 10 names only a read as the way to get an old fact id.
    - The door gives act-ids ahead of sending (`src-store/softland/store/door.clj:22-26`). So an inlet that keeps the `[act-id index]` of each fact it wrote needs a read only after a restart, or when someone else has written. My reasoning.
12. **One refusal word for two situations.**
    - Runs 1 and 2 give `:replaced-fact-not-current` both when someone else wrote first and when the import ran twice (`runs/decision-3-maps-output.txt:4`, `:6`).
    - The inlet can tell the two apart only by reading the row's writer (`:by-act`, `rows.clj:227-250`) against the act-ids that it holds. My reasoning.
13. **Content ids against the layered read.**
    - Page 18 judges end C only by "every edit needs a link". Its larger cost is with layers.
    - A read takes the nearest layer that holds a fact at a place (`src-store/softland/store/read_path/answer.clj:5-6`). Under content ids, a person's edit in her own layer is another entity, so it cannot stand over the base's version.
    - The store's one current fact for each place then says nothing about the current function.
    - Meanwhile a fact id already pins a version (`answer.clj:49-51`). My reasoning.
14. **Forget.**
    - Under A, names are in every fact, every place key and every pointer, in shared logs that are never cut (12 October, entry 2; the queue's V15, "an entity's id ... plain in the log").
    - For this repository that does not matter. For a private repository in someone's layer, it decides whether a name can ever be forgotten. My reasoning.

## What the page gets wrong

1. **The default rule is tested on the one file where it works, and that file is given as the rename.**
   - Page 14 says: "The real rename is 085bca4a. Only the `ns` line changed, and its eight definitions stayed byte-identical." That is true of `gate/stamp.clj` alone. The commit moved 13 files.
   - This review's run, approximate: `git diff -M -U0 085bca4a^ 085bca4a -- <old> <new>`, with each hunk set against each file's second top-level form.
     - In 12 of the 13 files, the `ns` form changed beyond its first line, by 1 to 15 lines: the `:require`s and the docstring.
     - 7 files had changes inside definitions, 11 hunks in all.
     - Only `stamp.clj` had 0 and 0.
   - Under the rule of page 32, a namespace whose name and body both changed is "retracted, and a new part is made", and so is each definition whose qualified name and text both changed. On the page's own example commit, the default would split 12 namespaces and the definitions that changed.
   - "3 facts instead of 72" holds for one file. My reasoning, on the run.
2. **The rule splits ordinary renames.**
   - The rule's second rung (page 32) needs a body that has not changed. c296a459 renamed five definitions in place, among them `class-place` to `depot-place`. In each, the docstring changed with the name: "the place of its layer's class" became "the place of its layer's depot". I checked this in this review with `git show c296a459`; no file moved in that commit.
   - Under the rule, each of the five is retracted and made new, with nothing joining the old to the new.
   - A rename usually changes the text that names the thing, so the rung catches the rarest case. My reasoning.
3. **E's 3 facts rest on matches that the run makes by hand.**
   - Page 20 says "The rename becomes 3 replaced facts (run 4)"; page 36 says "the move is 3 changes".
   - The run fixes `file-id` and `ns-id` to the same UUIDs before and after (`runs/decision-3-maps.clj:166-175`).
   - No rule on the page says how a file whose path and bytes both changed is the same file, or whether a namespace's "body" includes its name.
   - So the 3 facts are the cost after the matching, not the cost of a rule that does it.
4. **The default's costs are understated.**
   - Page 32: "The costs: a read before each write, and a rule for a name defined twice in one file."
   - It leaves out the costs that carry risk:
     - the judgment of the matching, and its false merges;
     - the mark that must come before the first facts, which page 20 states but the default does not count;
     - one more lookup for anyone who knows only a name;
     - a second id for a thing born in a person's layer.
   - And "a read before each write" can be avoided (finding 11 under "What the page misses"). My reasoning.
5. **The lens line understates a change of mind.**
   - Page 22: "a change of mind is paid in later acts."
   - The inlet's later acts pay for the inlet's own facts. They do not reach the pointers that others hold in their own layers. And every reader must then know both conventions, in a log that is never cut (12 October, entries 2 and 3).
   - The cost that the lens lacks is binding, and the page's sentence hides it. My reasoning; my phase-one text, section 8.
6. **Question 1's numbers are one file's.**
   - Page 36: "On 8 October a file moved into a folder ... 72 changes ... 3 changes." That is true of that one file.
   - The day's move was 13 files. That is about 1,500 facts under A. Under the page's own rule it is not 13 times 3, which is 39, but a few hundred, with false splits (findings 1 and 3).
   - Sid would decide on a difference of the wrong size. My reasoning.
7. **Two meanings of "B".**
   - Page 17's end B is "the id is the position". Page 36's "Default: B" is "a function has a number", which is end E.
   - A reader of "Default: B" can take it for the end of position. My reasoning.
8. **"This confirms the walk's claim" goes one step past the run.**
   - Page 10. In run 1 the overwrite is the run's own stand-in for the module: `(assoc t k row)` over a map (`runs/decision-3-maps.clj:109`).
   - What the run confirms: the act is accepted with no read, and `writes` gives a whole row at the place. The module's put over the old row is not run. Small.

## What the page has that I did not think of

1. **A forward declaration breaks names as ids, in the store's own `act.clj`.** Page 16. `(declare conform)` (`src-store/softland/store/act.clj:351`) and `(defn- conform` (`:400`) name one place twice under ids by namespace and name. Decision 2's run refuses `act.clj` as one act, `:place-named-twice` (`runs/decision-2-maps-output.txt:26`; I checked it). I had duplicate names only in the abstract (the old adapter's `~2`) and I counted `declare` forms, but I did not see that a `declare` and its `defn` collide inside one act. Any rule that matches by name needs a rule for this, as page 32 says.
2. **`:replaces` also guards against the import running twice.** Page 10, run 2 (`runs/decision-3-maps-output.txt:6-7`). With `:replaces`, the same replacement sent again is refused; without it, it is accepted twice. I had `:replaces` as the guard against someone else writing in between, not against the inlet's own second run.
3. **The decisions page already lists the late mark as not designed.** Page 20 cites `src-store/README.md:600-602`: "Not designed, and the builder's to propose: ... the facts of a key that were written before it was marked". I derived that behaviour from the code, but I did not find that it is a named open item.
4. **For "`:replaces` is optional", the record holds only a session's reading.** Page 27: that entry rests on a session's reading of his ticks (`src-store/README.md:90`, «3d4ee664»). My phase-one text tied his "I think we agree on this" (‹61dbaec7›) to it. The README ties that line to 6b, "a fact has no layer of its own" (`:127-129`). The page is right, and my text is loose there.
5. **Under content ids, the copies must share one place.** Run 5 (`runs/decision-3-maps-output.txt:17-19`). Two paths of one blob, as two facts on the content's entity under one key, are refused `:place-named-twice`; only one fact holding a set of paths passes. So the membership of a copy is one set at one place, and every writer of either path rewrites it. I said that copies become one thing; I did not see this consequence at the level of the store.
6. **The system can write the mark today.** Run 6 (`runs/decision-3-maps-output.txt:20-21`). A system act that marks `:code/name` in the base passes the parse, the door's check and the checks of the places; its standing is not run. I said a mark must come before the first facts, but I did not check who can write it.
7. **A plain question.** Page 36: "If each function has its own number, with its name written on it". That is the end in everyday words with one example, as the memory note `feedback-a-question-to-sid-carries-its-own-context.md` asks of a question to Sid. My own questions used more of our words.

## Does the page write any decision as if it were made?

**No.**
- Page 3 says "Nothing here is decided". Page 32's heading is "Default, not a decision". Pages 36 and 38 mark each default, and say what is built if Sid says nothing.
- The nearest thing to a ruling in tone is page 12, "Here the converter judges". It is stated flatly, but it sits in a paragraph marked "(my reasoning)", so it is a position and not a decision.
- Page 32 gives «ca81bcf6» correctly, as a session's default.
