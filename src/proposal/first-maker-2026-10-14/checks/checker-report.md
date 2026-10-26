# Checker's report on the first-maker pages

The checker (Opus 5.5, max effort) wrote this on 15 October 2026, at HEAD c90c7bcc. I wrote none of the pages. I edited no page and no run file. I opened no `raw-` file and did not read `src/app/server/env.clj` (I asked git only whether that path is ignored and tracked). Messages: two from the orchestrator (decision 2 final; ground check final). No message from any other agent arrived.

How to read this. "Page N" is the page's line N. `path:line` is a file at HEAD c90c7bcc. ‹id› is a line Sid typed, «id» is a session's text, both as `memtool` prints them. "My reasoning" marks what I concluded myself. A claim "holds" when its source says it; a minor fault is a line off, or wording looser or stronger than the source; a major fault is a source that does not support the claim, Sid's word misattributed, or a decision written as made.

My own runs, all printing only, are in this folder:
- `checker-walk-recheck.clj` and `-output.txt`: every act in `runs/walk-act-maps.edn` given again to `softland.store.act/parse`, every act with a door word given again to `softland.store.door.declaration/check`, the walk's queries 1-3 given again to `softland.store.read-path.query/check`, and act 12 rebuilt with the whole `src-store/README.md` from git. All recorded words came out the same (act 12 as stored is elided, as its note says, and gives `:ok`; rebuilt from git it gives `:value-too-large`). I gave query 4 to the check on the command line: `:ok`.
- `checker-d1-sizes-as-text.clj` and `-output.txt`: tracked .clj/.cljc/.md files over 65,536 bytes, raw and as the store measures a value.
- `checker-d3-maps-rerun-output.txt`, `checker-d4-ends-rerun-output.txt`: the pages' own run scripts run again (neither writes a file). Both outputs are identical to the recorded ones (`diff` gave nothing).

---

## 1. `walk-1-stamp.md`

**(1) Verdict: holds with corrections** (six minor, listed below).

**(2) Counts.** Claims on the page: 57. Checked: 57. Hold: 57 (50 cleanly, 7 with a minor fault, in 6 findings). Minor faults: 6 findings. Major faults: 0. Could not check: 0. Claims the page marks as its reasoning I did not count as claims.

Held, among others: the blobs, line and byte counts of `stamp.clj` (`git rev-parse`; `git show ... | wc -l -c`: 150 / 7579 / c57f8910 and 55 / 2674 / 99ab32f4, no commit touches it after 0e6182b9); bb6b6cc1 is 0e6182b9's parent; 085bca4a moved the file and changed the namespace; the parts before (2 `def`s, 6 `defn`s) and after (`next-stamp` at 47-55, inputs `[time-millis last-stamp greatest-carried]` at old line 96 to `[last-stamp]`); the file id is `nameUUIDFromBytes` of the path (recomputed: a452320e-...); every act word (re-run); act 7's 4 replaced and 28 retracted facts (`runs/walk-parse-output.txt:22-54`); every code citation in "What the store answers" and "What a read gives back" (`first_act.clj:81-122`, `decision.clj:154-222`, `rows.clj:173-179`, `:183-193`, `:227-250`, `:307-320`, `answer.clj:115-125`, `:133-146`, `door.clj:22-26`, `act.clj:290-291`, `:344`, `declaration.clj:74`); 27 files in 0e6182b9, all `M`, no `.cljc`; `time-of` at `read_path_pure_proof.clj:113-120`; 14 reader conditionals in `value_bytes.cljc`, the first at line 7; `docs/Untitled.md` untracked; ‹85a64cfb› verbatim and typed by Sid (`memtool who`: "BY THE RECORD: who = sid_typed"); «ca81bcf6» is a session's text proposing "by name", and the page calls it that.

**(3) Findings**, most severe first. All minor.

- **Page 43.** "Act 1: the permission stands, and no place is read (`decision.clj:233-247`)." The source, `places-needed`, supports that no *fact's* place is read: "A fact that names nothing to replace needs no row, and so can never be stale" (decision.clj:239-240). But to find that the permission stands, the gate takes "the row at the place of `:who`'s permission" (`standing`, decision.clj:156-157), and its body compares it: `(record/holds-value? permission-row permission)` (decision.clj:208). A place is read. Minor: wording broader than the source.
- **Page 43.** "and no rows by value (`first_act.clj:148`)". Line 148 is `:softland.store/marked-keys #{}` in `starts-with` ("`#{}`: no key is marked", line 132). The other premise, that rows by value are kept only for marked keys, is at `rows.clj:56-58`: "For a marked key a gate keeps each fact that has a value in this order too". The claim holds; the cited line gives half of it. Minor.
- **Page 64 (and question 2).** "0e6182b9 changed 27 files: about 422 facts under cut A". The run says: "approximate facts under cut A: 422 in 25 files of .clj and .md" (`runs/walk-parse-output.txt:55`). The two files left out are `test-store/softland/store/run_again_fixture/acts.edn` and `tables.edn` (`git show --name-only 0e6182b9`). Minor: the 422 covers 25 of the 27 files.
- **Page 62.** "Act 8 (`:ok`) is hypothetical: it retracts under the old ids and writes under the new ones, with the old fact ids in `:based-on`." It follows "every cut-A id changes", so it reads as every definition. Act 8 holds 8 facts, `next-stamp`'s four places only, and its `:based-on` holds 2 of their 4 old fact ids (`[[:base :microbatch a0...080] 26]` and `27`, the doc and the text), per `runs/walk-act-maps.edn` act 8 and its note: "next-stamp's four places retracted under the old id and written under the new". Minor.
- **Page 18.** "a keyword cannot hold `/` (`act.clj:267-279`)". Those lines say a readable keyword's name and its namespace each match `token-pattern`, which has no `/` (act.clj:270, 278-279). A keyword does hold one `/` as its separator, as `:softland.store.gate.stamp/next-stamp` on the same page does. The walk's own note says it right: "Its name holds '/', which a readable keyword cannot." Minor wording.
- **Pages 48 and 56.** "The answer is `{:holds :value ... :stamp 1}`" and "The result is stamp 2. A retracted place then reads `:nothing-here`". These come from the code; no cluster ran. The page's mark "This is derived from the code. No cluster ran." is at page 40, in the section before; the sections at 46-50 and 52-58 carry none (query 1's *check* did run). Minor.

**(4) Decided language: no.** Every id, key and cut is labelled an instance value (page 3); each question's default says "Default if you say nothing", and question 1's says "marked not decided (a session's proposal)".

**(5) Questions: pass.** Each has an example (`next-stamp`; the 27-file commit; a page still being written), the choices, a default and what silence gives; no ‹id› or row name inside a question (the source lines beneath carry them). Note: question 2 gives the two choices but not what the choice costs (one commit's change landing in many acts); question 3 is outside the four decisions, as page 65 says.

**(6) Size: 1,139 words.** With walk 2, 1,937 words against about 1,200 asked for the two together.

---

## 2. `walk-2-readme.md`

**(1) Verdict: holds with corrections** (one wrong citation, three minor).

**(2) Counts.** Claims: 32. Checked: 32. Hold: 31 as cited, 3 of them with a minor fault; 1 holds in substance but its cited source does not say it. Minor: 3. Major: 1. Could not check: 0 (page 58's "no copy elsewhere" checked only as far as `git grep` of the heading in tracked .md files: five files mention it, none copies the section).

Held, among others: blob 3fa3d402 at 0e6182b9 and HEAD, 1,029 lines, 72,062 bytes, no later commit; the section at 984-1029 is 3,122 bytes (`sed -n '984,1029p' | wc -c`); entries at 993, 1013, 1021, 1026, 1028; five distinct lines of Sid's (‹296a2a7b› five times, ‹2f80019d›, ‹aa212e6b›, ‹f3f530b8›, ‹f6a39dde›) and one session's («6c16b53a»); the anchor link at README:41; the ids are name-based UUIDs of `path#anchor` and `path#anchor/<n>` (recomputed: bfbd279d..., 2027a87a... for entry 3); act 9 is 2 + 4 + 5×6 = 36 facts, entry 3's `:md/text` is its index 21, which act 11 replaces; `:k-7a2e11`; ‹296a2a7b› holds "2) refuse" and is typed by Sid; `declaration.clj:84`, `:345`, `:376`; `decision.clj:249-257`; the three extended entries at README:183-184, 333-335, 887-888 (`git diff -U0 0e6182b9^ 0e6182b9`: hunks `-182 +183,2`, `-331 +333,3`, `-883 +887,2`); README:1007 sits in entry 1; "### Key points" at 67 and 93 under two `##` parents; both `unique-ids.md` files are blob 7e6c32a9.

**(3) Findings.**

- **Major. Page 42.** "Finding a section by its heading needs a marked key, and the base marks none (`gate/decision.clj:375-385`)." Those lines are `reads-marked-keys?`: "Takes an act, or nil. Gives true when a gate that accepts the act needs the row at `marked-keys-place`: the act holds a fact under a plain key, which may be a marked one, or a fact under `:softland.store/marked-keys`..." They say what a gate reads when it accepts an act, nothing about finding a section or the base's marks. The claim is true by other lines: `read_path/answer.clj:27-30` (a `:points-at` read answers `{:layer layer :not-marked true}` for a layer that has not marked the key), `rows.clj:56-58` (by-value order only for a marked key), `first_act.clj:132` and `:148` (no key is marked at the start). The fix is the citation.
- **Minor. Page 48.** "the 13 October section went from line 880 to 884 (`git show 0e6182b9`)". The heading "## Decisions of 13 October 2026 — the store comes out the same when it is run again" is at line 819 before 0e6182b9 and 823 after (`git show 0e6182b9^:src-store/README.md | grep -n`). Lines 880 and 884 are one line inside that section ("What comes with it:", in its entry 1). The shift of four holds.
- **Minor. Page 15.** "since 72,062 bytes is over 65,536 (`door/declaration.clj:84`)". The limit is on "the UTF-8 bytes of the one text of the value in its normal form" (declaration.clj:85-86). As one string value the README is 73,581 bytes (`checker-d1-sizes-as-text-output.txt`); 72,062 is the raw file. The conclusion holds either way.
- **Minor. Page 40.** "the base is seen by everyone while no fact says otherwise (`first_act.clj:146`)". The same docstring says: "Not decided. That a new layer is "members" and the base "everyone" is kept only so that the build can run" (first_act.clj:139-140). The page relays the behaviour without the code's mark.

**(4) Decided language: no.** Page 40 relays a kept, not decided behaviour without saying so (above); it does not say anyone decided it.

**(5) Questions: pass.** Both use everyday words with an example (this page's split; entry 1's "replaces"), give choices, a default and what silence gives; no ids inside.

**(6) Size: 798 words** (walks together 1,937 against about 1,200).

---

## 3. `decision-1-code-or-data.md`

**(1) Verdict: holds with corrections** (five minor).

**(2) Counts.** Claims: 40. Checked: 40. Hold: 40 (35 cleanly, 5 with a minor fault). Minor: 5. Major: 0. Could not check: 0.

Held, among others, every record id and every quoted run, each typed by Sid and verbatim (`memtool line`; `memtool who` gives "who = sid_typed" for all nine quoted runs): ‹89442b46›, ‹6b40e2e2› ("Can it be defined in the data?", "not bounded by what we do in code ourselves"), ‹0ace018c›, ‹c6afceee›, ‹450c82ca›, ‹d8f12f84›, ‹9791265e›, ‹4123e23c›, ‹1bd5708f›, ‹b9b62f9f› ("what do the need do need these need to be as code? Like specifically for the outside inlet"), ‹8239c30d›. Files: README:685-686, 746, 170-174, 941-945; `.gitignore:16` is `src/app/server/env.clj`, untracked (`git ls-files` gives nothing); `act.clj:32-36` verbatim ("in code and not a fact in the store", "it must hold when each fact is wrong"); `act.clj:159`; `runs/decision-1-values-output.txt:5` ("a compiled regex object value? false"); `PARTS-4-8.md:78`; `ARCHITECTURE-STE100.md:449-450` ("the door makes names at random"), `:1148` and `:1209` (both tagged `[SETTLED + PROPOSAL + DERIVED]`); `docs/decisions.md:159-184` and `:166`; `vision/LOG.md:1330` heads "the 3D round closes"; commit 6da31b9f says "the 3D page's Position 9 closed"; no line in `src-store/` or `test-store/` holds the ruling (`grep -rn`); `CLAUDE.md:65`; `markdown_adapter.clj:5`, `:20-23`; `clojure_adapter.clj:3`; `material.clj:1-9`; `OPEN-QUESTIONS.md:143` ("Are adapters and converters data | open").

**(3) Findings.** All minor.

- **Page 7.** "129 of 2,186 files exceed one value's 65,536 bytes (`runs/decision-1-sizes-output.txt:3-7`)". The run counts tracked .clj, .cljc and .md files only (`runs/decision-1-sizes.sh:5`), by raw size; 2,186 is on line 2, 129 on line 3. As the store measures a value (its text, with quotes and escapes; declaration.clj:85-86), 132 exceed (`checker-d1-sizes-as-text-output.txt`). The point holds; the qualifier is missing and 129 is a floor.
- **Page 15.** "His pattern: ... no leaf per shape (‹e7203392›)". His line is a challenge in questions: "❯ why is box below the waist???? can it not be defined above the waist in ecs??? why not? then if i want to have a circle is going to be below the waist as well??" (typed by Sid, 2026-08-25). "No leaf per shape" is the page's reading of it, given as his pattern. Wording stronger than the source.
- **Page 15.** "grammars per layer (`src-store/README.md:236-242`)". README:236: "5. **Each layer holds a copy of each grammar it uses; a gate reads on its own task.** Sid decided (F5 A)." It sits in "Decisions of 7 October 2026 — the store's module" (README:186); a grammar there is a key's (README:160: "word and grammar are facts about them"), not a file format's. Carrying it to format grammars is the page's step, unmarked, inside "His pattern".
- **Page 12.** "Class: depot record no gate reads (`declaration.clj:65-68`)". Those lines: "Not here. Whether a thing's permission stands, and whether a fact that it replaces is current: those need the record... A grammar check is not built." The direct source is declaration.clj:345: "No rule reads the value of a fact under a plain key". Indirect citation.
- **Page 12.** "a person's copy in her layer wins her reads (`read_path.clj:22-24`; `declaration.clj:120-125`)". The first holds: "for each place the nearest layer that holds a fact wins". The second is `max-builds-on` (64 layers), which bears on it only through builds-on. Indirect second citation.

**(4) Decided language: no.** Page 3 says nothing is decided; (e) is headed "Default, not a decision"; page 22 marks his lines "A want, not ruled"; page 26 limits the 3D ruling's reach and says the reach to converters is a session's.

**(5) Questions: pass, with notes.** One example (a line starting with # begins a section), choices, default D, silence ("D runs on a store thrown away, and nothing is kept"), no ids inside. Notes: the letters jump A, D, E (B and C are dropped without a word); "A) our code, an example" does not say what "an example" means; "grammars" is a word of ours.

**(6) Size: 916 words** against about 600.

---

## 4. `decision-4-who-acts.md`

**(1) Verdict: holds with corrections** (one false claim in the examples, two minor).

**(2) Counts.** Claims: 43. Checked: 43. Hold: 42 (39 cleanly, 3 with a minor fault, counted in 2 findings); 1 does not hold. Minor: 2 findings. Major: 1. Could not check: 0.

Held, among others: the run (`runs/decision-4-ends.clj` run again: identical output); `act.clj:155-158`, `:174-176`; `rows.clj:163`, `:173`, `:24-31`; README:911-922, 941-951, 368, 561-562, 293-294 ("The system stands at the top of the base's chain. Sid decided (E1·H8 A): "system" (‹f3f530b8›)"); `read_path/layers.clj:140-141` (`:everyone` gives `:yes` before the signed-in test); `door.clj:72-75`; `first_act.clj:60`; `decision.clj:61-67`, `:236-243`, `:251-258`, `:283-300`, `:302-330`; `declaration.clj:224-230`; `:498` is the only read of `:session` besides the act's shape (`grep -rn ':session\b' src-store`); OPEN-QUESTIONS K7 (113, assumes erasing by destroying a lock, while README:685 records "the store keeps no locks"), K8, K9, K14, V25, E7; the old server's importers as "system" at `clojure_adapter.clj:553`, `:623`, `markdown_adapter.clj:461`, `:531`, and the transcript importer `import:sense-block-v0` typed `:system` (`transcript_import.clj:592-605`). Record ids, typed by Sid and verbatim: ‹60c1bcaa› ("i made the repository closed source"), ‹f3f530b8›, ‹ddf13d17›, ‹4123e23c›, ‹1088472f› (its reply «ca29636f» item 1 is signing: "So your answer would be (a) for now, with the door left open"), ‹bec8ca63› ("we need the sign-in"), ‹ac4db2eb› ("we start the session with what happens when a user logins"), ‹5ff89890› (said of z4, reads). "needs no sign-in yet" is a session's: `memtool who` gives "who = session_pasted_by_sid", first in «9a48928c», pasted in ‹6ac75ce2›; «ca81bcf6» holds "the code and markdown road doesn't need sign-in yet".

**(3) Findings.**

- **Major. Page 40.** "Only the matter room's resident was typed `:agent` (`src/app/server/page/matter_room.cljc:274-284`)." The cited lines do hold a resident typed `:agent`, but it is not the only one: `src/app/server/episode/material_circulation.clj:305-309` reads ";; OC's custody vocabulary names the process as :agent; ... actor {:actor/id autotag-actor-id :actor/type :agent :actor/capabilities #{:object-container/import-material}}". "Only" is false. It sits in the examples and does not move the decision.
- **Minor. Page 11.** "Every end passes the gate unchanged (run)". The run asks `standing`, check (g) and `making-act?`. An act of A, D and E is run (`runs/decision-4-ends-output.txt:3, 10, 11, 13`); for B and C only the making act is (lines 4 and 7); no act of Sid's in his own layer is run. The "(run)" covers less than "every end" and less than "the gate".
- **Minor. Page 15.** "The base starts as "everyone" (`first_act.clj:139-146`)." and "Any see-and-act holder there can change it (`decision.clj:283-300`)." Both cited ranges carry the code's own mark, which the page drops: "Not decided. That a new layer is "members" and the base "everyone" is kept only so that the build can run" (first_act.clj:139-140); "Not decided. Sid's word for who writes a layer's setting is "the owner"... The answer here is kept only so that the build can run" (decision.clj:290-292).

Note, not a fault: page 26 quotes the vision, "the last commit is its base layer whatever i build on top is in the active layer". `memtool who` says: "TYPED BY SID: in no line he typed. ... IN A LONG PROMPT of his (typed or pasted, the record cannot tell; not safely his words): ‹5940b98d› 2026-07-03 19:48 ... BY THE RECORD: who = session_pasted_by_sid." The page says so ("in the record only inside a long prompt, ‹5940b98d›") and cites `vision/LOG.md:146`, filed under "## ~2026-06-06 — raw notes back-fill (pasted by Sid 2026-07-04 ...)" (LOG:101). ‹86db6d30›, typed by Sid, supports the idea ("they would be the base layer and then we would be using them in some way as active layer"), not the words.

**(4) Decided language: no.** Page 3: "Nothing here is decided"; (e) is "Default, not a decision"; page 44 says applying ‹5ff89890› here is the page's own. Page 15 drops two "not decided" marks of the code (above).

**(5) Questions: pass, with notes.** Both carry an example, the choices, a default and what silence gives; no ids inside. Notes on question 1: six choices, partly in words of ours ("under the system"); "the vision log stays out" is not explained anywhere on the page, so its default carries a choice Sid cannot see.

**(6) Size: 984 words** against about 600.

---

## 5. `decision-3-same-thing-across-commits.md`

**(1) Verdict: holds with corrections** (two minor).

**(2) Counts.** Claims: 49. Checked: 49. Hold: 49 (47 cleanly, 2 with a minor fault). Minor: 2. Major: 0. Could not check: 0.

Held, among others: the run (`runs/decision-3-maps.clj` run again: identical output); runs 1, 2, 4 and 6 say what the page says; `decision-3-maps.clj:109` (a map takes the writes) and `:166-175` (the file and namespace ids fixed by hand); 13 renames in 085bca4a at 91 to 99 percent (`git show -M --summary`); 12 of 13 namespaces changed beyond line 1, 11 definitions in 7 files, `stamp.clj` neither, 764 facts (`runs/decision-3-085bca4a-output.txt:1-14`); the eight renames of c296a459 each changed beyond the name (output lines 1-8; spot-checked `class-at-making` in git); act.clj:351 and :400 are the only repeated top-level name in act.clj (`conform`, a `declare` and a `defn-`), and cut A over act.clj is refused `:place-named-twice` (`runs/decision-2-maps-output.txt:26`); `rows.clj:237`; `answer.clj:5-6`, `:127-146`; `act.clj:179-180`, `:192`; `decision.clj:249-257`, `:441-443`; README:90, 125-126, 133-134, 172-173, 600-602, 685, 746, 941, 1007; `declaration.clj:74-77`, `:120-125`, `:372`; V53 lists "name a point"; `PICTURE.md:82` ("or by a person overriding in their layer"), and its header says Sid pasted a session's model (lines 3-6); commit fe592411 ("The limits that were a list in the README sit beside the function they belong to"); `code_import.clj:106` (`--no-renames`), `:170-178`, `:276-316`; ‹1082f32d› ("so that they never collide", typed by Sid). The June lines at `vision/LOG.md:173, 193, 197, 203, 146` are verbatim, and `memtool who` gives "who = unknown ... only inside a long prompt of his (‹5940b98d›)" or (‹ac7f7a11›) for each, as page 54 says.

**(3) Findings.** Both minor.

- **Page 21.** "`gate/batch.clj` alone needs 384, over the 256 of one act". The run gives `gate/batch.clj` "cut-A facts 192" and in its total "'files-over-256': 0" (`runs/decision-3-085bca4a-output.txt:5`, `:14`), counting one side (`decision-3-085bca4a.py:48`, `:52`). 384 is 192 retracts plus 192 new facts in one act, the page's own step, unmarked. A reader who opens the run sees 192 and "0 files over 256".
- **Page 16.** "The prompt's default based acts on the commit («ca81bcf6»)". «ca81bcf6» is a session's reply (14 October, 17:20): "My default: by name ..., with the commit as what the act is based on." The prompt Sid pasted (‹6ac75ce2›: an untraced paste and «9a48928c») does not hold that default; «9a48928c» says only "the same thing at the next commit". A session's text named as "the prompt's"; neither is presented as Sid's.

**(4) Decided language: no.** Page 3: "Nothing here is decided"; (e) is "Default, not a decision"; page 42 calls "a fact with no value is a retract" a kept rule, as README:133-134 does; page 47 marks the June notes "not rulings".

**(5) Questions: pass.** Question 1: the 8 October move as the example, "its own number" for a store id, choices A and E, default E, silence. Question 2: two small examples (someone changed it first; the import ran twice), R and W, default R, silence. No ids inside.

**(6) Size: 1,317 words** against about 600.

---

## 6. `decision-2-what-a-file-becomes.md`

**(1) Verdict: holds with corrections** (five minor).

**(2) Counts.** Claims: 62 (counting the legend). Checked: 62. Hold: 62 (57 cleanly, 5 with a minor fault). Minor: 5. Major: 0. Could not check: 0 (page 57's "two converters ... mint two ids" is marked "not run" by the page and I did not run it).

Held, among others: `act.clj:169-171` (`:e :k :v`) and `:343-347`; README:746-751, 941-945, 911-922, 718-719, 533-536, 159-164 (he picked "5 (keys as ids): confirm"; "He has not ruled between the two"), 842-861, 858-861, 899-902; `first_act.clj:146` ("as built"); `CLAUDE.md:92`; `checks/review-d2-runs.txt:7-8` (49.03 MB, history 26.87 MB) and `:153-155` (two tiling parts of `stamp.clj`, parse `:ok`, door `:ok`); maps output lines 7 (213 unnamed forms), 9, 15-16 (59 and 18), rows d2-4 (line 22) and d2-7 (line 26); `read_path.clj:313, 316, 332, 335`; detail 29-32 (four store files with a repeated name); `decision-2-maps.clj:57` (`:read-cond :allow`) and `:85`; fixes 2-6 and 11; "21 ... in two or three files each": my run gives 12 ids in two files and 9 in three, none in more (`checker-d2-across-files-output.txt`); `decision.clj:237-240`; `act.clj:267-270`; `declaration.clj:127-133` (`max-marked-keys` 32), `:273-277`; `query.clj:52`; `rows.clj:55-63`; `sources.clj:203-207`; the K5 row (OPEN-QUESTIONS:111: "a migration in Rama changes the values of a table and not its keys"); `markdown_adapter.clj:20-23` with `object_container.clj:216-219` (a version-specific key from the source and its content hash). Record ids, typed by Sid and verbatim (`memtool who`: sid_typed for each quoted run): ‹d4fd70ba›, ‹85a64cfb›, ‹e36008e1› (it opens "so i understand it like", so "a session's point, restated" holds), ‹b22e6278› (about transcript capture), ‹5ff89890›. The June quotes at LOG 112, 119, 126, 141, 144, 146, 170-173, 193-195, 197 are verbatim.

**(3) Findings.** All minor.

- **Page 91.** "They come from his own depot, but the record holds them only inside long prompts, so `who` cannot confirm he typed them." For one of the seven, page 98's "on ingest time? or during active layer time?" (LOG:119), the record does confirm it: `memtool who` gives "who = sid_typed. Sid typed it first (‹86db6d30›, 2026-06-06 05:29)". The others hold as the page says: "A bunch of block with children is a section", "The file name is the parent", "we don't have to fatten this" are "who = unknown", only inside a long prompt (‹e784e003› or ‹5940b98d›); "the atomic unit is the function" is first inside ‹5940b98d› (3 July) and only later in session texts (12 July). The page understates its own source in the safe direction.
- **Pages 5-6 (the legend).** "**dN** and **maps n**: line n of `runs/decision-2-maps-output.txt`". The page then cites rows by their labels: d2-7 is line 26, d2-8 line 27, d2-4 line 22. Read by the legend, "d2-7" sends Sid to line 7 (213 unnamed forms). The legend misleads; the labels themselves are right.
- **Page 40.** "28 code files need more than the 256 facts one act may hold (d2-8)." The count is maps line 4: "CODE cut A (file, ns, each top-level form, 4 facts each), one act per file, over 256 facts: 28 ; most: 896". Row d2-8 is one such file (`pure_proof.clj`, 778 facts, `:too-many-facts`). Citation points at an instance, not the count.
- **Page 37.** "129 files are too big for that (act 12; decision-1)." As in decision 1: 129 is raw size over tracked .clj/.cljc/.md files; as the store measures a value, 132 (`checker-d1-sizes-as-text-output.txt`).
- **Page 29.** "My scan takes every tracked file." The scan takes tracked .clj, .cljc and .md files, less `env.clj` and `raw-` files (`runs/decision-2-maps.clj:28-33`). Wording looser than the source.

**(4) Decided language: no.** Page 3: "Nothing here is decided"; (e) is "Default, not a decision"; page 87: "He has not ruled K5"; page 91 calls the June lines "notes and not rulings"; page 107 says applying ‹5ff89890› here is the page's own reading.

**(5) Questions: pass, with a note.** Both questions use everyday words ("announces `conform` and then writes it"; "a label ... remade from its name each time, or made ... once ... and then kept"), an example (`next-stamp`), what happened, two choices, a default and what silence gives; no ids inside. Note: the questions' letters are not the page's. Question 1's A is the page's parts D over B, and its B is the page's C; question 2's A is the page's id B, and its B is the page's id E. Read beside the page, the letters cross.

**(6) Size: 2,007 words** against about 600: more than three times.

---

## 7. `ground-check.md`

**(1) Verdict: holds with corrections** (two minor).

**(2) Counts.** Claims: 50. Checked: 49. Hold: 49 (46 cleanly, 3 with a minor fault, in 2 findings). Minor: 2 findings. Major: 0. Could not check: 1 (page 24, "The 52 were already listed by my first `git worktree list` tonight, which I did not save": no record of it). Checked only in part: page 8, the content of the checkout, which is removed; `runs/ground-1-run-stdout.txt:1-4` shows it at c90c7bcc with exactly the three fixture files differing, the script's header and `runs/ground-1-summary.txt:1` name 0d24b03e, and `git log` shows 0d24b03e is those files' last commit before 0e6182b9.

Held, among others:
- The derivation (`run_again_fixture/README.md:63-76`), summarized faithfully, including its own "derived ... not run".
- Every number against `runs/ground-1-compare-output.txt`: the 475 rows (lines 2-3: 140+58+22+14 and 167+61+6+7); answers (line 7: 135, 3, 1, 1; line 249: 162, 3, 1, 1; together 297, 6, 2, 2 of 307); facts (150: 57+1; 419: 60+1; 119, two with `:level-since`); facts by value (211 and 213: 22 paired; 483 and 485: 6 paired); last stamps and strays (237: 14 rows, 6 equal; 240-247: eight differ; 492: 7 rows, 6 equal; 495: one differs); the total (498: "equal: 12 ... store only, unpaired: 3 | fixture rows that differ ...: 463"); the two acts (496-497: `:carried-stamp-too-far-ahead` to `:accepted`, one per gate) and their three rows (210, 236, 482, the three "store only" rows).
- The summary: line 5 ("this proof prints none"), lines 8-25 (six FAIL lines, all in the 4-task and 8-task fixture tests, each with `(not (zero? 494))` for `apart`), 65 (no OUT OF ITS LINE), 214-215 (`{:test 5, :pass 40, :fail 6, :error 0}`, exit 1). The proof's docstring names "Checks 2 and 3, the fixture ... with 4 tasks and then with 8" (`run_again_proof.clj:48`, `:59-60`). 463 + 28 + 3 = 494.
- The worktree list (53 lines, none first-maker; my own `git worktree list` now: 53, none first-maker; the checkout directory does not exist).
- **The twenty runs, counted by me** from `runs/ground-2-read-path-20-runs.txt`: 17 PASSED and 3 FAILED on "through three layers"; the failures are runs 07, 09 and 11, at lines 41, 61 and 81; each failing run `{:test 28, :pass 552, :fail 1, :error 0}`, exit 1; each passing run `{:test 28, :pass 553, :fail 0, :error 0}`, exit 0; no other FAILED or ERROR line in any run. The SHOWN line says "gave 1 whole answers anew" in the 17 passing runs and "gave 2" in the 3 failing ones. The quoted FAILED line (file line 41) and the two SHOWN lines (lines 42 and 6) are verbatim.
- `read_path_proof.clj:1285-1287` (the check, its four parts) and `:1303` (`(count erin-later)`); `runs/ground-2-fail-parts.txt:1-3`. Without opening a raw file, the cut file's own "actual:" lines (48, 68, 88, whole at 1,126 characters) show `true` in the expected vector and `false` in the observed one at that part.
- `RECEIPT.md:60-65` ("A session's reading, not verified by repeated runs: it rests on the order in which Rama's pushes arrive").
- Section 3, every citation: `module/walk.clj:295` (`decision/time-on`), `:361`, `:397` (the last stamp per layer in each gate's task state); `record/rows.clj:192`, `:248`, `:446` (`:time` as a row value; `(long time-millis)` throws on nil); `gate/decision.clj:217-218`, `:457`, `:553-555`; `gate/stamp.clj:26-28`, `:50`; `door/declaration.clj:330` (`[:int {:min 0}]`, `:no-door-time`); probe lines 2, 5, 6, 7, 12, 28-33, 36-37.
- "What these runs changed": the one tracked change in the tree, `src/app/server/README.md`, dates from 29 September; the lock file is empty and dates from 12 October; `/tmp/claude-1000/gr1-said` does not exist; `/tmp/clojure-13426758051935281907.edn` exists (14 October, 23:40; I did not open it).
- «3c021c3a» is a session's text; Sid pasted 5,378 characters of it in ‹be879d88›; it holds the ground-check task ("feed the fixture as it was before 14 October (git, 0d24b03e) through today's store ...").

**(3) Findings.** Both minor.

- **Page 14.** "In 6 only the time: refusals already counted 0 or 1. In 2 only the stamp: `:no-door-time`." The cited lines (:7, :249) give only the counts by class. The refusal words are not on the rows in that file either (lines 24, 117, 118, 251, 252, 279 and 25, 320 end at the store's time). I looked the eight act ids up in the old fixture (`git show 0d24b03e:test-store/softland/store/run_again_fixture/tables.edn`): the six are refusals (`:layer-not-made` twice, `:layer-belongs-to-the-other-gate` four times) at stamps 0 or 1, and the two are `:no-door-time`. The claim holds; its source is not on the page.
- **Page 22.** "the run agrees with the derivation on every class, and disagrees nowhere." The derivation names one act: "The second making's act that carried a stamp an hour ahead of its time was refused; it would be accepted, and write a fact's row in each gate" (README:73-75). The run accepted two acts, one per gate (compare output 496-497). The rows match exactly; the act count is reconciled only by the page's own reasoning at page 18 ("the hand's one case played in each gate"), which the verdict line does not repeat. Wording stronger than the source.

**(4) Decided language: no.** The page reports runs; its one default ("nothing changes until you answer") is marked.

**(5) Questions: pass.** Everyday words (Erin, "what points at canvas-1", let into the nearest layer); what happened in numbers (17 and 3); two choices, each with what changes; a default and silence ("nothing changes until you answer"); no ids. "At least one of them partial" is the page's reasoning (page 38 marks it so) stated plainly in the question; "her screen still ended right" rests on the third part being equal (fail-parts 1-3).

**(6) Size: 944 words** against about 600.

---

## Table

| page | claims checked | hold | minor | major | verdict | words |
|---|---|---|---|---|---|---|
| walk-1-stamp.md | 57 | 57 | 6 | 0 | holds with corrections | 1,139 |
| walk-2-readme.md | 32 | 31 (+1 true, wrongly cited) | 3 | 1 | holds with corrections | 798 |
| decision-1-code-or-data.md | 40 | 40 | 5 | 0 | holds with corrections | 916 |
| decision-4-who-acts.md | 43 | 42 | 2 | 1 | holds with corrections | 984 |
| decision-3-same-thing-across-commits.md | 49 | 49 | 2 | 0 | holds with corrections | 1,317 |
| decision-2-what-a-file-becomes.md | 62 | 62 | 5 | 0 | holds with corrections | 2,007 |
| ground-check.md | 49 (of 50) | 49 | 2 | 0 | holds with corrections | 944 |

"Hold" counts claims whose source says them, including those with a minor fault. The two majors: walk 2, page 42 (a true claim whose cited lines do not say it) and decision 4, page 40 (a false "only"). Sizes against the asks: the walks together 1,937 against about 1,200; decisions 1 to 4 at 916, 2,007, 1,317 and 984 against about 600 each; the ground check 944 against about 600. Every page is over its size.

## What I could not check and why

- Ground check, page 24: the agent's first `git worktree list` of the night was not saved.
- Ground check, page 8: the checkout's files; the checkout is removed. I checked what the run's stdout, the script header and `git log` say of it.
- Ground check, page 60: that the marker was made; I can see only that it is gone.
- Ground check, section 2: fail-parts reads the "actual:" line that its author took by grep from the raw files. I opened no raw file; I confirmed the differing part from the cut file's whole "actual:" lines, and took the equality of the other three parts from fail-parts' own output.
- Whether Sid typed the June notes in `vision/LOG.md`: the record holds most of them only inside long prompts, "typed or pasted, the record cannot tell". I report what `memtool who` says for each.
- What a page marks as its own reasoning, I did not re-derive, as asked; where it was cheap I checked the arithmetic (494 = 463 + 28 + 3; 384 = 2 × 192; 307 = 297 + 6 + 2 + 2; 36 = 2 + 4 + 5 × 6).
- Results the pages derive from code without a cluster (walk 1's answer to query 1, stamps 0 to 3, `:nothing-here` after a retract; decision 3's statement that the system can write the mark, "standing not run"): I checked them against the code they cite, not by a run.

## Three places Sid should look first

1. **The vision line under option F, and the pictures beside it.** Decision 4, page 26 (also decision 3, page 52, and decision 2, page 95): "the last commit is its base layer whatever i build on top is in the active layer". `memtool who` on it: "TYPED BY SID: in no line he typed ... IN A LONG PROMPT of his ...: ‹5940b98d› ... BY THE RECORD: who = session_pasted_by_sid." The pages say it is only inside a long prompt, and `vision/LOG.md:146` files it under "raw notes back-fill (pasted by Sid 2026-07-04 ...)" (LOG:101); but end F of decision 4 and a cost of decision 3's default rest on it. Look at: `memtool who "the last commit is its base layer"`, `vision/LOG.md:101` and `:146`.
2. **Walk 2, page 42.** "Finding a section by its heading needs a marked key, and the base marks none (`gate/decision.clj:375-385`)." The cited lines are `reads-marked-keys?`, a gate's read on accept; they do not say it. Look at `read_path/answer.clj:27-30`, `record/rows.clj:56-58` and `gate/first_act.clj:132, 148`, where it is said.
3. **Decision 1, page 15: "His pattern".** "the text shaper below the waist (‹8239c30d›), no leaf per shape (‹e7203392›), grammars per layer (`src-store/README.md:236-242`)". The first holds. The second is a line of questions ("why is box below the waist???? ... if i want to have a circle is going to be below the waist as well??"). The third is his decision on where a gate keeps a key's grammar (README:186, 236), not on file formats. The pattern is the page's assembly, presented as his. Look at `memtool line e7203392` and README:186 and 236.

Beyond these three, the one false claim on any page: decision 4, page 40, "Only the matter room's resident was typed `:agent`": `src/app/server/episode/material_circulation.clj:305-309` holds a second (`autotag-actor-id`, `:actor/type :agent`).
