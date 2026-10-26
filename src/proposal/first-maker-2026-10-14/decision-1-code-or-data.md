# Decision 1: code or data, on the first road

Decision-1 agent, Opus 5.5, xhigh; HEAD c90c7bcc; revised after `checks/review-d1-report.md`. Nothing here is decided.

**Answered 16 October 2026: E, not this page's default D.** A converter is a record; one grammar reader in code, each format's grammar a record. The decision is in `docs/decisions.md`, "Tools are records over a vocabulary"; the queue's G3 row points there. What a converter writes (decisions 2 and 3) is still open.

**(a) The question.** Of the inlet (opens files, runs git), the adapter (cuts a file into named parts) and the converter (makes acts from them): which is code we ship, which a record an inhabitant can change?

**(b) The ends.** The inlet's crossing and its floor on reading are code no record may lift (my reasoning): values stay plain in a log never cut (`src-store/README.md:685-686, 746`), so a file read by mistake stays. The secrets file is git-ignored (`.gitignore:16`), so reading committed blobs cannot reach it. Its choices (repository, paths, revision, layer, permission) can be records; he asked why it must be code (‹b9b62f9f›). The adapter reads an inside fact, an outside thing first being one (`src-store/README.md:170-174`); yet of 2,186 tracked .clj, .cljc and .md files, 129 exceed a value's 65,536 bytes raw and 132 as the store counts a value's text (`runs/decision-1-sizes-output.txt:2-3`; `checks/checker-d1-sizes-as-text-output.txt`), and the walk's 72,062-byte act 12 was refused (`runs/walk-parse-output.txt:14, 21`).

For the adapter and the converter:

- **A. Code only, in git.** Nothing changes from inside. Class: fits none; a change is a deploy and a reread.
- **B. A record in the store**, run by an evaluator, changed by an act. Class: depot record, but "No rule reads the value of a fact under a plain key" (`src-store/softland/store/door/declaration.clj:345`): the forever falls on the evaluator, not a version of the act; the lens does not separate these. Gains: a person's copy in her layer wins her reads (`src-store/softland/store/read_path.clj:22-24`; through builds-on, `declaration.clj:120-125`); `:based-on` cites its exact version (`src-store/softland/store/act.clj:159`), code only once its version is a fact.
- **C. A declaration compiled with the code**, like the act's shape, "not a fact in the store" (`act.clj:32-36`). Class: fits none. Its reason, "it must hold when each fact is wrong", is the waist's test; an adapter, checked downstream by a gate, fails it (reasoning).
- **D. A floor in code, the rest a record.** For .md the floor splits lines and matches a string pattern (a regex is no value: `runs/decision-1-values-output.txt`); for .clj it reads forms unrun; the record names which. A record cannot mint a random id (a proposal, `src/proposal/frame-2026-09-15/PARTS-4-8.md:78`; the door "makes names at random", `src/proposal/backend-on-rama-2026-10-02/ARCHITECTURE-STE100.md:449-450`): fresh ids come from the floor, or each id is derived and carries its rule. Class: as B.
- **E. One reader of grammars in code**, each format's grammar a record; a leaf per format fails "not bounded by what we do in code ourselves" (‹6b40e2e2›). The page's assembly of his lines: the text shaper below the waist (‹8239c30d›); his question "if i want to have a circle is going to be below the waist as well??" (‹e7203392›); a layer's copy of a key's grammar (`src-store/README.md:236-242`), carried to formats by the page. Class: as B.

**The lens** (my reasoning). Under every end the converter writes what lasts, ids and key names: the old server built ids from the adapter's name (`src/app/server/ingest/markdown_adapter.clj:20-23`). Unnamed costs: ids that other makers' acts cite; who may change a part; a secret, once read, kept. The forever starts at the first real record (`src-store/README.md:941-945`); until then an end can be dropped from the store, not from git, where code steers despite "existing code never answers it by default" (`CLAUDE.md:65`).

**(c) What Sid has said**

- "i use waist as to refer to something that has to exist as code vs what can and should be build on top" ‹89442b46›: the word, not this line.
- Of the converter's function (‹c6afceee›), "Can it be defined in the data?" ‹6b40e2e2›, ‹0ace018c›; "we will provide it has data because uh, uh, I am going to be the first user" ‹450c82ca›; "we should not be doing everything as code" ‹d8f12f84›. A want, not ruled (`src/proposal/backend-on-rama-2026-10-02/decisions/OPEN-QUESTIONS.md:143`).
- Adapters "can be data ... What we might need is like a converter layer" ‹9791265e›: that layer as code is my reading, D's runner.
- The base: "the code that we are writing", "Maybe like ingestors" ‹4123e23c›; "and maybe a few data based layers" ‹1bd5708f›. Who may change a part, and whether it is compiled, are separate (the reviewer's reasoning).

The ruling at `docs/decisions.md:159-184` was made for the 3D kind's input (`vision/LOG.md:1330-1342`; commit 6da31b9f); its reach to converters is a session's (`ARCHITECTURE-STE100.md:1148, 1209`); `src-store/` holds it nowhere.

**(d) Examples.** The old server cut .md with "a line-based subset parser" (`markdown_adapter.clj:5`), .clj with a parser (`src/app/server/ingest/clojure_adapter.clj:3`); Inland's operator code was forced: "no tool can run in the base on the rig as built" (`src-inland/softland/inland/material.clj:1-9`).

**(e) Default, not a decision.** Inlet as in (b). Adapter and converter: D, .md first, on a store not kept; the converter's floor is the runner, declared inputs and outputs, fresh ids. Keys wait on K5 (what a key is), ids on decisions 2 and 3. If the first run must be kept, the adapter turns to A, so its record form does not harden unasked; the converter's keys and ids harden anyway. Reasons: "we should not be doing everything as code" ‹d8f12f84›; D is the ruling's "named steps over a vocabulary of capabilities" (`docs/decisions.md:166`).

**(f) The question for Sid**

A reader cuts a .md file into parts, say "a line starting with # begins a section"; a maker turns each part into facts. Each can be our code, or a record anyone can copy, run by small parts in our code. You asked for these without us, and called the base our code. For both: **A)** our code, this road's instance, not the form; **D)** small parts in code, the rest records; **E)** as D, one reader for all grammars. B (D without a floor) and C (a file outside the store) are set aside. Default D; if you say nothing, D runs on a store thrown away, and nothing is kept.
Source: ‹6b40e2e2›, ‹d8f12f84›, ‹4123e23c›; `src/proposal/backend-on-rama-2026-10-02/decisions/OPEN-QUESTIONS.md:143`.

