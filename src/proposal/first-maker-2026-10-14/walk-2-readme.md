# Walk 2: "Decisions of 14 October 2026" in `src-store/README.md`

The walk agent wrote this page, under the conventions of `walk-1-stamp.md`.

## The file

- `src-store/README.md` at 0e6182b9 is the same as at HEAD (blob 3fa3d402): 1,029 lines, 72,062 bytes. The section is lines 984-1029, 3,122 bytes. 0e6182b9 created it, and no commit since touches the file.
- The README links to the section by an anchor made of its heading's words (`src-store/README.md:41`).
- Open: whether a section is named by its anchor, its number or its content. Decisions 2 and 3; lens: fits none.

## Its parts

- A heading, an opening paragraph, and five numbered entries (993, 1013, 1021, 1026, 1028), with five of Sid's quoted lines and one session's.
- The cut I chose (cut A): the file, the section and each entry are entities. Their ids are name-based UUIDs of `path#anchor` and `path#anchor/<number>`. Bullets and quotes stay inside an entry's `:md/text`. The record ids that an entry quotes are facts on it, Sid's apart from a session's.
- Not chosen: each bullet or quote as an entity, and the whole file as one value. It passes parse (act 12); the door's check refuses it `:value-too-large`: its one text is 73,581 bytes (72,062 raw), over 65,536 (`door/declaration.clj:84`).
- Open: decision 2; lens: fits none.

## The acts

- Act 9 holds 36 facts. Parse and the door's check gave `:ok`. Entry 4 in short:

```clojure
{:e #uuid "<…/4>" :k :md/title :v "A thing that arrives with no time is refused."}
{:e #uuid "<…/4>" :k :md/cites-sid :v #{"296a2a7b"}}
```

- K5: act 10 files entry 3's text under the opaque key `:k-7a2e11`. Parse: `:ok`.
- Open: keys are decision 2 (lens: depot record); the session is decision 4.

## What the store answers

This is derived from the code. No cluster ran.

- Sent after walk 1's three acts, act 9 is accepted with stamp 3 (`gate/stamp.clj:47-55`) and writes 36 fact rows.
- No rule reads a plain key's value (`declaration.clj:345`), so nothing checks that ‹296a2a7b› is a line Sid typed.
- Open: who checks that a cited line is real. It belongs to none of the four decisions.

## What a read gives back

- Query 4 asks for entry 3's entity, `:not-signed-in true`, with `:with #{:permission}`. Its check gave `:ok`; everyone sees the base until a fact says otherwise, "Not decided ... kept only so that the build can run" (`first_act.clj:139-140, 146`).
- The answer gives six places, each with `:fact [[:base :microbatch u9] i]`, `:stamp 3` and the permission (`read_path/answer.clj:115-146`).
- Finding a section by its heading needs a marked key, and the base marks none (`read_path/answer.clj:27-30`, `record/rows.clj:56-58`, `gate/first_act.clj:132, 148`). Open: which keys the base marks. Decision 2.

## When the file changes

- The section has no later commit, so I added an instance: a later decision adds a sentence to entry 3. 0e6182b9 did exactly that to three earlier entries (`src-store/README.md:183-184`, `333-335`, `887-888`), so it is this file's real kind of change.
- Act 11 replaces entry 3's `:md/text`, with `:replaces [[:base :microbatch u9] 21]` and a placeholder sentence. Parse and the door's check gave `:ok`.
- Those real edits moved every later line: the 13 October heading went from line 819 to 823 (`git show 0e6182b9`). Identity by line breaks.
- Entry 1 says "It replaces entry 4 of 8 October" (`src-store/README.md:1007`). The store's `:replaces` covers only the same place (`decision.clj:249-257`), so such a link is not a `:replaces`.
- Open: identity is decision 3 (lens: fits none). Whether such links become facts belongs to none of the four decisions.

## Instances added

- **The same heading twice, real.** `.claude/skills/rama/references/unique-ids.md` has "### Key points" at lines 67 and 93, under two parents. An id made from the bare heading makes both one place: the door's check refuses act 13 `:place-named-twice` (`declaration.clj:376`). The heading path keeps them apart.
- **The same bytes at two paths, real.** That file and `.agents/skills/rama/references/unique-ids.md` share blob 7e6c32a9. Identity by content makes them one thing.
- **A section moved to another page, hypothetical.** Under `path#anchor` ids every id changes, as in walk 1's rename.

Why these were added: the walked section has no repeated heading, no copy elsewhere, and no move.

## Questions for Sid

1. This page split into the section and its five numbered entries, and the bullets and quotes stayed inside each entry. **What is needed:** where a markdown page splits into things. A: at sections and entries. B: finer, at each bullet or quote. **Default if you say nothing:** A, marked not decided.
   Source: act 9.
2. One entry says that it replaces an earlier day's entry. **What is needed:** A: that link stays text. B: the road records it as a fact, so that a read can find it. **Default if you say nothing:** A, and nothing can yet ask "what replaced this?".
   Source: `src-store/README.md:1007`.
