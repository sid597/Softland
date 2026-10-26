# Walk 1: `stamp.clj` before and after 0e6182b9

The walk agent wrote this (Opus, xhigh) on 14 to 15 October 2026, at HEAD c90c7bcc. Every id, key, session and act-id here is an **instance value**, owned by the decision named beside it. The acts are numbered as in `runs/walk-act-maps.edn`. `runs/walk-parse.clj` built them from git; it is a run, not the adapter, and its output is `runs/walk-parse-output.txt`.

**Lens, my reasoning.** Ids, key names, the cut and the grain are values that acts write, and no gate rule reads them. The old values stay in the log (`src-store/README.md:746-751`). I mark these "lens: fits none".

## The file

- `src-store/softland/store/gate/stamp.clj`. At bb6b6cc1, the parent of 0e6182b9: 150 lines, 7,579 bytes, blob c57f8910. At 0e6182b9, unchanged to HEAD: 55 lines, 2,674 bytes, blob 99ab32f4.
- It has three names: the path, the blob and the commit. The path changed in 085bca4a.
- Open: which names are recorded, and which is the identity. Decisions 2 and 3; lens: fits none.

## Its parts

- Before: the `ns` form with its docstring, two `def`s and six `defn`s. After: the `ns` form and `next-stamp` (47-55).
- The cut I chose (cut A): the file, the namespace and each definition are entities. The file's id is a name-based UUID of its path. The other ids are readable keywords such as `:softland.store.gate.stamp/next-stamp`. Each definition carries `:code/in`, `:code/kind`, `:code/doc` and `:code/text`.
- Two cuts not chosen, both refused `:malformed`:
  - The path as the id (act 5): a keyword's name cannot hold `/` (`act.clj:270, 278-279`).
  - Code as data (act 6): symbols and lists are not values (`act.clj:290-291`), so code enters as text.
- Open: which parts become entities, and their ids. Decision 2; lens: fits none.

## The acts

- Act 0 is the base's making act (`gate/first_act.clj:81-122`), needed only if no base exists. Act 1 holds 36 facts. Parse and the door's check gave `:ok` for both.

```clojure
{:version 1 :who :system :layer :base :permission [:system :base :base]
 :session :walk-instance-session
 :facts [{:e :softland.store.gate.stamp/next-stamp :k :code/doc :v "The stamp of one decision. …"} …]}
```

- K5, on that same fact:
  - Under the key `:code/doc` (act 2): `:ok`.
  - Under a UUID key (act 3): `:malformed` (`act.clj:344`).
  - Under the opaque keyword `:k-6f1c0d` (act 4): `:ok`, but the fact that gives it its word needs a key of its own (my reasoning).
- Open: `:who`, `:layer`, `:permission` and `:session` are decision 4 (lens: fits none). K5 is decision 2; a UUID key would need a new act version (lens: depot record).

## What the store answers

This is derived from the code. No cluster ran.

- Act 0 is accepted as a making act (`gate/decision.clj:154-222`), with stamp 0 (`stamp.clj:47-55`). It writes two fact rows keyed `"<layer> <entity> <key>"` (`record/rows.clj:173-179`), an answer row `{:stamp :digest :time}` (`rows.clj:183-193`) and the last stamp.
- Act 1: its permission is found at `:who`'s place (`decision.clj:156-157, 208`); no fact's place is read (`:239-240`). Accepted, stamp 1: 36 fact rows with who, permission and time (`rows.clj:227-250`); none by value, as no key is marked (`rows.clj:56-58`, `first_act.clj:148`).
- Open: who makes the base. Decision 4.

## What a read gives back

- Query 1, `{:asks [:place :softland.store.gate.stamp/next-stamp :code/doc] :asker :system :layers [:base]}`: the check gave `:ok`. Derived, no cluster ran: the answer is `{:holds :value :value "<doc>" :layer :base :fact [[:base :microbatch u1] 26] :stamp 1}` (`read_path/answer.clj:133-146`). That `:fact` is what a later `:replaces` names.
- Who wrote a fact, its permission and its time come back only when `:with` asks (query 2; `answer.clj:115-125`).
- Open: none; lens: the read path costs nothing.

## When the file changes

- Act 7 is the real change, against act 1. Parse and the door's check gave `:ok`. It replaces 4 facts (the blob, the namespace's doc, `next-stamp`'s doc and text) and retracts 28 (the seven deleted definitions, four places each). Each fact names `:replaces [[:base :microbatch u1] i]`.
- The road can know `u1` and `i` in three ways: from act-ids that the door gives ahead (`door.clj:22-26`), by reading the place first, or not at all, in which case the gate writes over the row (`decision.clj:233-247`; not run).
- Derived, no cluster ran: stamp 2. A retracted place then reads `:nothing-here`, not `:no-fact` (`rows.clj:307-320`).
- `next-stamp` stays one thing only because its name stays: its inputs went from three to one, and it moved from line 96 to line 47.
- Open: decision 3 (lens: fits none): identity by name, position or content; how a deletion is said; how old fact ids are known.

## Instances added

- **Rename and move, real (085bca4a).** The file moved into `gate/`, and its namespace changed from `softland.store.stamp` to `softland.store.gate.stamp`. So every cut-A id changes. Act 8 (`:ok`, hypothetical) holds `next-stamp`'s four places only, retracted under the old id and written under the new, with 2 old fact ids in `:based-on`. Whether `:based-on` may carry lineage is decision 3.
- **A false move, real.** 0e6182b9 deleted `stamp/time-of` and added an unrelated `time-of` in `test-store/softland/store/read_path_pure_proof.clj:113-120`. Identity by bare name would call that a move.
- **One act per commit, real.** 0e6182b9 changed 27 files; cut A needs about 422 facts for the 25 that are not `.edn` (approximate), and an act holds 256 (`door/declaration.clj:74`). Sid on grain: "the tool is deciding how much of their different actions are put together inside a single value or inside a single act" (‹85a64cfb›).
- **Not committed, real.** `docs/Untitled.md` is untracked, so it has no commit and no blob. Whether the inlet reads the working tree belongs to none of the four decisions.
- **A whole file deleted, hypothetical.** Either every place of its entities is retracted, as act 7 did for seven definitions, or one fact on the file is retracted. Decision 3.
- **`.cljc`, real.** `src/app/client/engine/value_bytes.cljc` holds 14 reader conditionals (line 7), so one definition can have two bodies. Decision 2.

Why these were added: the real change at 0e6182b9 holds deletions but no rename, no move, no file deleted whole, no working-tree file and no `.cljc`.

## Questions for Sid

1. A function can keep its name while its insides change, as `next-stamp` went from three inputs to one. **What is needed:** how the store knows it is the same thing at the next commit, so that it replaces the old facts and does not start a new thing. A: by name. B: by content. **Default if you say nothing:** by name, marked not decided (a session's proposal).
   Source: «ca81bcf6»; act 7.
2. One commit's 25 code and markdown files need about 422 facts; an act holds 256. **What is needed:** A: one act per file. B: one act per commit, with a larger limit. **Default if you say nothing:** A.
   Source: `declaration.clj:74`.
3. A page you are still writing is not committed. **What is needed:** A: the road reads commits only. B: it reads the working tree too. **Default if you say nothing:** A.
   Source: `git status`.
