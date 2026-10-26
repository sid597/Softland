# Review of decision 1, code or data: the page against my own thinking

Reviewer: Claude Opus 5.5, max effort; phase two, one pass. I read the page `src/proposal/first-maker-2026-10-14/decision-1-code-or-data.md` once. I also read its runs (`runs/decision-1-values.clj`, `runs/decision-1-values-output.txt`, `runs/decision-1-sizes.sh`, `runs/decision-1-sizes-output.txt`), and checked each line it cites. My own thinking from before I saw the page is `checks/review-d1-own-thinking.md`.

"page:N" is a line of the page. Other paths are at HEAD c90c7bcc. ‹id› is a line Sid typed; «id» is a session's text. "(Mine)" marks my reasoning.

## What the page misses

- **The converter's default is never put to Sid, and the build goes ahead without him.** page:33 makes the converter "code marked an example" and says it "departs from his lean in ‹6b40e2e2›". But the question in (f) asks only about the rule that cuts a .md file (page:37), and adds "silence builds B". So the one choice the page itself says goes against him would be built without his word. His lean is said more than once: "we should not be doing everything as code" (‹d8f12f84›); "anything above the outside inlet is an adapter that the user would define" (‹450c82ca›); and "since adapters and converters can be data, we are not going to touch them" (‹06df0468›, a long prompt; no session wrote these words before it). (Mine.)
- **What the store already gives a record and not code.** On the page, a record's whole gain is "a user writes a reader by an act" (page:10). The store gives two more things.
  - Copies per layer. The read path makes "the nearest layer that holds a fact" win (`src-store/softland/store/read_path.clj:22-24`), and layers build on layers (`src-store/softland/store/door/declaration.clj:120-125`). So a person's copy of a converter in her own layer is the one her reads find. That is his import-and-fork picture (‹d8f12f84›), already built.
  - Provenance. `:based-on` cites the exact version of a record by fact id (`src-store/softland/store/act.clj:159`). A converter in code is cited only if its version is first written as a fact. The old server put that version in a note (`src/app/server/ingest/code_import.clj:25-32`).

  Without these, the comparison sells records short. (Mine.)
- **What is kept of what arrived, and the input that the adapter gets.**
  - Entry 11 of 6 October records an outside thing as a fact first (`src-store/README.md:170-176`; ‹61dbaec7›: "I think we agree on this"). So the adapter reads an inside fact; G1 (`decisions/OPEN-QUESTIONS.md:141`) becomes "yes".
  - If the text is kept, a record adapter, or a person's copy of it, can cut again from inside. If only the hash is kept, every new cut goes back to the host's disk.
  - The page's own runs show that a whole file is often too big for one value. 129 files are over 65,536 bytes, three of them in `src-store` (`runs/decision-1-sizes-output.txt:3-7`). The walk's act 12 was refused `:value-too-large` for the store's README at 72,062 bytes (`runs/walk-parse-output.txt`, act 12 and its "VALUE BYTES" line). The page's text uses neither result.
  - Sid's own lines on this: `vision/LOG.md:257` (raw and native both kept), `:119` (cut at ingest or later), and ‹5ff89890› (a default takes "the side of spectrum that captures more").

  (Mine.)
- **A wrong read cannot be undone.** The page cites the old server's deny list (page:7). It does not say that every value is plain in the log and that the log is never cut (`src-store/README.md:685-700`, `:746`). So a file read by mistake stays in the log forever. The secrets file is in the working tree and git ignores it (`.gitignore:16`), so reading committed blobs from git cannot reach it. The floor on what may be read must be one that no record can lift. (Mine.)
- **One general reader is missing from the ends.** Under D, ".clj, read forms without running them" (page:12) is a leaf that knows one format. Each new format (Python, YAML) is then new code, and that fails "not bounded by what we do in code ourselves" (‹6b40e2e2›) for formats. Sid's own pattern argues for one general reader. "text in position of glyph out", the text shaper, belongs below the waist (‹8239c30d›). Of the box, a single shape, he asked: "then if i want to have a circle is going to be below the waist as well??" (‹e7203392›). Grammars are already layer data in his decision 5 of 7 October (`src-store/README.md:236-242`) and in ‹ed9b8edc›. One reader of grammars, with the .md and .clj grammars as records, is not among the ends. (Mine.)
- **Sid's lines that bear directly on the decision are not cited.**
  - ‹06df0468› (long prompt): adapters and converters "can be data, we are not going to touch them".
  - ‹450c82ca›: base functions are provided "as data because ... I am going to be the first user".
  - ‹b9b62f9f›: he asked why even the inlet has to be code.
  - ‹d8f12f84›: his answer to security is review and proof ("Bend or Lean"), not code.
  - `vision/LOG.md:1316`: above the waist is data, "without getting into git merge deadlocks".
- **The tension about the base is left as "Open: which" (page:24), without what informs it.**
  - ‹1bd5708f›: "the code that we are writing and maybe a few data based layers".
  - ‹6d5e8c3c› (long prompt): the ingesters are "layers that are built on top of it, kind of libraries", and "the other people's becomes the system one".
  - Who may change a thing (the system only) and whether it is compiled are separate questions. A converter can be held and shipped by the system and still be a record. (Mine.)
- **The reach of the settled ruling.** `docs/decisions.md:159-184` was ruled for the input of the 3D kind (`vision/LOG.md:1330-1343`; commit 6da31b9f). That it governs converters is a session's extension (`src/proposal/backend-on-rama-2026-10-02/ARCHITECTURE-STE100.md:1148`, `:1209`). The page leans on it for D (page:33) and against C (page:11) without saying this. (Mine.)
- **The way back.** Editing code in the store and writing it back to a file needs a cut that keeps every byte (`vision/LOG.md:228`; `docs/carry-on.md:232`). That choice is made with the first kept fact, because a reading that lost bytes stays that way in history. The page does not mention it. (Mine.)
- **Who may run a record, and under which permission.** B and D say only that "a user writes a reader by an act" (page:10).
  - Until sign-in is built, any sender can make any layer for any holder (`src-store/softland/store/gate/first_act.clj:30-33`), and the gate cannot check `:who` (`src-store/softland/store/README.md:216-220`).
  - The Inland file the page cites says the same obstacle: "no tool can run in the base on the rig as built" (`src-inland/softland/inland/material.clj:3-5`).

  A record converter that runs in the base needs the system's permission. (Mine.)
- **Where fresh ids come from.** A record converter cannot make random ids itself: "No leaf may read a clock, a random number" (`src/proposal/frame-2026-09-15/PARTS-4-8.md:78`, a proposal), and the door "makes names at random" (`ARCHITECTURE-STE100.md:450`). So the fixed part must supply fresh ids. Otherwise ids are derived, and each one carries the rule, which is the hazard the page itself names at page:14. (Mine.)
- **The costs that decision 1 mostly carries.** The page rightly says that the lens does not separate B's permanence (page:10). It does not name three other costs:
  - ids that other makers' acts cite, which no one may rewrite;
  - who may change a part, which is what "not bounded by what we do in code ourselves" is about;
  - a secret that, once read, stays in the log.

  (Mine; my own thinking, section 10.)

## What the page gets wrong

- **The converter default contradicts the ground the page cites.**
  - page:33 grounds D in "named steps over a vocabulary of capabilities" (`docs/decisions.md:166`). The same ruling says "A tool is a record and never enters git" (`:169`).
  - page:11 holds that line against C, a file outside the store. It does not hold it against A, or against its own default of a converter in code, which also lives in git.
  - The reason given, "its ids waiting on K5", does not favour code. A record converter waits on K5 just as well, and its key names would then change by an act, not by a commit.

  (Mine.)
- **D's precedent is a precedent for C.** page:12 gives "as the act's shape is code declared as data (`act.clj:32-36`)". Those lines say the shape is in code "and not a fact in the store", compiled with the store. That is the page's C (a declaration outside the store), not a record in the store. Their reason, "it must hold when each fact is wrong", is the test for what goes below the waist. Applied to an adapter, which a gate checks downstream, it argues that the adapter need not be code. (Mine.)
- **The letters collide.** In (b), B is "a record in the store, run by an evaluator" and D is "a floor in code, the rest a record" (page:10, page:12). In (f), "B" is "a record over small steps in code", which is (b)'s D (page:37). An answer of "B" can be read as either. (Mine.)
- **The Inland evidence is a forced placeholder, not a finding.** page:31 gives "Inland read files in operator code" as a precedent. The cited file calls it "placeholder 21: no tool can run in the base on the rig as built, since a tool's lease is sealed under the layer's person owner" (`src-inland/softland/inland/material.clj:1-9`). So Inland did not choose code. Its permission design forced it. (Mine.)
- **"Until then any end can be tried and dropped" (page:14) holds for the store, not for code.** A throwaway store goes. Code written into git for the road stays and steers every later session. The project's rule is that "existing code never answers it by default" (`CLAUDE.md:64-65`), and the old server is the example of this. So trying A is not free in the way that trying a record is. (Mine.)
- **"The inlet is code under every end" (page:7) is too coarse.** The crossing and the floor on reading must be code. But the inlet's choices are records under the page's own logic for D ("The record names which pattern, which forms", page:12): which repository, which paths, which revision, which layer, under which permission. Sid asked exactly this on 5 October (‹b9b62f9f›). The floor must still be one that no record can lift (see "What the page misses"). (Mine.)
- **A borrows a clause written for capabilities.** page:9 labels A, "Code only", with "added once, with receipts" (`docs/decisions.md:169-171`). That clause describes a capability, a leaf under records. It does not describe a whole adapter or converter, which the same ruling makes a record. So the quote gives A a backing from the settled ground that it does not have. (Mine.)
- **"So no vocabulary hardens unasked" (page:33) overstates.** If the first run is kept, the converter's keys and ids harden under A too; the page says so itself: "Under every end the converter writes what lasts" (page:14). The sentence holds only for the adapter record's own form. (Mine.)
- **"Its ids waiting on K5" (page:33) mixes two questions.** K5 asks what a key is (`decisions/OPEN-QUESTIONS.md:111`). Entity ids belong to decisions 2 and 3. (Mine.)
- **An ambiguous attribution.** page:21 reads "Of the converter's function (‹c6afceee›), "Can it be defined in the data?"". ‹c6afceee› holds only the line about running a shape through a function. "Can it be defined in the data?" is in ‹6b40e2e2› and ‹0ace018c›.

## What the page has that I did not think of

- **End C, a declaration in a file outside the store.** This is an end I did not list (page:11). I would add one point (mine): the page's run shows that such a rule map is already a store value (`runs/decision-1-values-output.txt:1-2`). So C can be a step toward a record: the same value moves into the store unchanged, and only who may change it, and how, moves with it.
- **A reading of ‹9791265e› that I missed.** "What we might need is like a converter layer that uh, has some kind of input output attached to it" sits inside "what needs to be built in code" (page:22). I read the line only as "adapters can be data". My refinement (mine): the code there could be the machinery, a runner and declared inputs and outputs, while the converters themselves stay records. ‹06df0468› (a long prompt, two days later) says converters "can be data".
- **Tying the adapter's form to whether the first run is kept.** If the first run is kept before he has seen it, a record language hardens unasked; choosing A then avoids that (page:33). I had the runner's language versions as a cost. I did not tie the choice to whether the first run is kept.
- **Concrete evidence that a new adapter version makes new ids.** The old markdown unit id contains the adapter's own name, "markdown-block-v0" (`src/app/server/ingest/markdown_adapter.clj:13`, `:20-23`; page:14). I had the general cost of ids that others cite, but not this direct evidence that the adapter's version sits inside the id.
- **A compiled regex is not a store value** (`runs/decision-1-values-output.txt:5`; page:16). So a pattern in a record is a string, and the leaf that compiles it brings Java regex semantics below the waist. I would add (mine): such a leaf needs a budget against runaway matching.
- **A file path is not an id** (page:16, the walk's act 5, `runs/walk-parse-output.txt`). Identity for a file must be made, not taken from its path. I had the limit of 256 characters, but not that a path is not a readable id at all.

## Does the page write any decision as if it were made?

No. page:3 says "Nothing here is decided", and page:33 says "Default, not a decision". No line puts a choice in Sid's mouth.

But two lines together let one choice take effect without him. page:33 sets the converter to code. page:37 asks him only about the .md rule, and says "silence builds B". If he says nothing, the converter is built as code, the end that the page itself says departs from his lean, and it was never put to him.
