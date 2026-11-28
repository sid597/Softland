# The form of an act, finished and reviewed: what was built, what ran, the choices, what is left

Written 28 November 2026 by a Claude Code session in the cloud, for Sid and
for the session that puts this branch on top of the other two. It is item 1
and item 5 of section 4 of `HANDOFF-stored-forms-2026-11-28.md` (on `main`,
not on this branch), and it does not repeat that page. Paths are from the
repository root.

What is Sid's here: his lines of 28 November as that page and its starters
quote them, and the prompt of this session. The record could not be reached
from the cloud, so nothing was held against it. Every other choice is a
session's and his to turn; they are listed in section 4.

## 1. In short

The piece is built, reviewed by a fresh subagent that saw none of the
building, and mended. The branch is `act-form-2026-11-28`, pushed, with
`de68997d` as its base: the seven commits that were there, and thirteen of
this session's builder, and this page.

- The review found no fault in the source that had to be mended. It found
  three holes in sentences and pages, which are mended, and notes, of which
  some are mended and some are for Sid (section 5).
- Everything that holds this branch's own change ran in the cloud and passed
  (section 3). The mends were not reviewed a second time.
- Not done, as the prompt said: this branch on top of the other two, and the
  one run of all the proofs.

## 2. What was built

By one builder on Opus at high effort, from a brief that this session wrote
after it had read the diffs of the three commits that came with no report.

Those three commits, as read:

- `18b605cf` (the door, module and read-path proofs): mends of the proofs
  only; each fault was the proof's. It stands.
- `620cf968` (the seam's series through `door/send-acts`): it stands as
  built. What it moves is in section 4.
- `4ad925e5` (WIP): its fixture held a fault of the proof's hand, and
  `run-again-proof` failed on that tree (check 4). Both are mended below.

The builder's commits, first pass:

| Commit | What |
|---|---|
| `506a037` | The run-again fixture made again. The WIP fixture's "stray of the old form" in the base was `[:base uuid]`, a well-formed id, so the base took it as an act sent twice; it is now a three-slot id. `uuid-of` read only three-slot ids. "Five words" of a stray became four |
| `a31ad64` | The judge's `reference.clj`, which `fix` loads, holds an act in the new form |
| `5aac40b` | The server's sign-in (`src-server/softland/server/sign_in.clj`) writes no `:subjects` |
| `0dcfa73` | Entry 3 of "Decisions of 28 November 2026 — the form of an act", pointers to it from the entries that describe the old id, and sentences of four pages made true |
| `51f2aff` | F2: 6 October entries 4 and 5 and 13 October entry 6 are marked "a session's default, Sid's to turn" |
| `cc3faa9` | The runtime's pure proof holds the seam's two sending calls: it had failed 1 of 25 since `620cf968`, because its pattern also matched `send-acts` |
| `9aa2bf9` | Check 4 of the run-again proof took the uuid from the third slot, so none of its planned failures fired |

After the review:

| Commit | What |
|---|---|
| `cc2766a` | The decisions page: entry 3 is headed by what he said yes to; entry 2 says what is so (section 5, findings 1, 6, 9, 10, 13) |
| `864542a` | A sentence of the door's proof that a send goes by the lane's depot fact and not by where the lane starts |
| `cdbb6a5` | Sentences for each place where a series stops, in the seam and in the door; `send-all-now`'s docstring says what an interrupt does |
| `136838a` | The server's `send-all` sends a series through `door/send-acts` too |
| `e374415` | The fixture, twelfth making (415 records, 1,807 rows): facts under a UUID key in each gate, and things carrying `:depot`, `:subjects` and `:claimed-when`, each refused |
| `105b483` | The judge's page and aliases say that `-M:record` and `-M:run today` hold the old form |

The seven items of the starter are done. What was left in the old form, on
purpose:

- The judge's `record.clj`, `walk.clj`, `today.clj` and `piece_2/`: `fix`
  loads none of them, and they were out of step with the store before this
  build.
- Recorded outputs of old runs inside pages
  (`test-runtime/softland/runtime/attacks/README.md:82`, and what is under
  `runs/` folders).
- No digest of an act is written down as a constant outside the fixture, so
  item 5 changed nothing by hand.

What a send costs now: one act is one read of the lane's depot fact and one
append; a series of n acts through the runtime's seam or the server's is one
read for each lane and n appends, then the answer reads.

What this branch changes in the three files that the Auth branch also
changes, against `de68997d`: `main.clj` one line (F24); `sign_in.clj` the
`:subjects` removals in `making-act`, `name!` and `setup!` (21 lines
touched); `seam.clj` one block, `act-id-of` to `send-all-now` (61 lines
touched).

## 3. What ran

In the cloud container, each once after the last edit of what it loads, one
cluster proof at a time. Counts are passed; none failed at the end.

| Proof | Result |
|---|---|
| Store `act-proof`, `pure-proof`, `permission-proof`, `read-path-pure-proof` | 157, 779, 72, 289 (before the mends; the mends changed no source of the store) |
| Wire `pure-proof`; `command-pure-proof` | 39; 35 (before the mends) |
| Runtime `pure-proof` | 32, after the mends |
| Runtime `softland.runtime.proof`, which sends a series through the real seam and door | 50, after `620cf968` and before the mends; the mends changed only the docstring of `send-all-now` |
| Server `pure-proof`; `seam-proof` | 68; 43, after the mends |
| `door-proof` | 292, after the mends |
| `module-proof`; `read-path-proof` | 511; 758 (before the mends; the sentence that fails one run in three on `main` did not fail) |
| `run-again-proof`, on the twelfth making | 49 |
| The judge, `fix 2000 1` | green, 124,861 things |

Held against a broken copy: the new door sentence failed, and only it, with
`door.clj:158` changed to `depot-at-making`.

Sentences that existed and were changed to pass, and which side was wrong:
the proof's in every case but one. Runtime pure proof "the seam calls the
door's send-act in one place" (its pattern); run-again check 4 (read the
third slot); run-again "five words" (there are four); door proof "as the
base's own depot fact says" (it could not tell the fact from where the base
starts, and now says so). The server's pure proof "send-act in one place"
changed because the source changed on purpose.

Not run, and left to the full run: the runtime's other cluster proofs
(`two-lines-proof`, which F24 changed, among them), the command proofs, the
attacks, the measures. In the full run a fault of this branch should show in
the store's four pure proofs, `door-proof`, `module-proof`,
`read-path-proof`, `run-again-proof`, the runtime's `pure-proof` and `proof`,
the server's `pure-proof` and `seam-proof`, `two-lines-proof`,
`command-proof` and the judge's `fix`.

## 4. The choices, for Sid to turn

Of how it was run:

- **The branch that was pushed is `act-form-2026-11-28`**, by "check out the
  branch by its name" and "push the branch you work on". Nothing went to
  `main`, and no pull request is open.
- **The builder was on Opus at high effort, the reviewer on Fable at high.**
  The review is the one place besides this session where Fable was used. By
  the harness's counts of subagent tokens: the builder 367k for the build and
  478k at the end of the mends (it is one subagent, sent on; whether the
  second count holds the first is not known); the reviewer 339k.
- **The mends went to the same builder, and were not reviewed again.**
- **The commits of this session are authored "Claude" with a no-reply
  address**, the cloud container's git identity; the seven before them are
  his. No message holds an attribution line. The identity was left as the
  container had it and no pushed commit was rewritten. When the branch is put
  on top of the other two on his machine, `git rebase --reset-author` in that
  step makes them his.

Of the build:

- **The seam's series (`620cf968`) stands.** It checks and rations every act
  and makes its id before the first is sent, and reads the answers after the
  last send. What moved against the code before:
  - An interrupt in the middle of a series can let one act more be appended,
    and the receipts before it come back with `{:could-not :read-the-answer}`
    under `:answer`. Before, nothing further was sent. The interrupt comes
    from a closed connection, so nobody reads that result, and a repeat under
    the same UUIDs is taken once. Held with a stand-in; whether Rama's real
    append goes through in that state is known only from the door's proof.
  - No answer is read between two acts. In one lane the depot's order is the
    order of the appends, and a gate decides an act against its own lane
    alone, so no act of a series rests on another lane's answer (the
    reviewer read this in the source).
  - When pacing is built, `ration` will see a whole series in one moment and
    count acts that a word of the door then keeps from being sent. It gives
    nil today.
- **The server's `send-all` was brought onto `door/send-acts` as well**,
  though the starter named only the runtime's seam: this change had made it
  n reads for n acts.
- **Entry 3 is headed by his answer and not by the mechanism**: "A lane's
  speed is to be changeable after the lane is made. Not built." The question
  4c is retold and not quoted, and the entry says so and says which session
  text holds it («59efa1cb»): a session with the record puts the words in.
  That his yes covers the depot word leaving the id is written there as what
  the starter of this work says, not as read from the question.
- **Entry 2 was corrected in what a session had written under it**: six
  places wrote `:subjects`, not four (the server's sign-in wrote it twice),
  and the declaration's bound on the length of an id did read it. Its
  conclusion holds for all six.
- **The fixture is the builder's making, not the WIP commit's**, and it now
  holds this change: a second run of a UUID key's rows and of the three
  refusals is seen by "the tables stand".
- **Pointers to entry 3 were put on three entries more than the starter
  named** (6 October entry 2, 9 October entry 4, 13 October entry 1), since
  each describes the old id or what the door adds.
- **F2 marks one sentence more in 6 October entry 5**, "The door makes
  act-ids", because break 4 of run-5 names it.

## 5. What the review found

By a fresh subagent on Fable at high effort, from the diff `de68997d` to
`9aa2bf9`. It ran no proof of the build; it ran three probes of its own. Its
verdict: no fault in the code that must be mended before the pull request.

Mended:

1. Entry 3 was headed by a session's mechanism where his decision belongs;
   and "he ruled: both" stood for an answer that was a yes to "confirm
   both?". (`cc2766a`)
2. No check told "the door goes by the lane's depot fact" from "the door
   goes by where the lane starts": no act can make the fact say another
   word. (`864542a`, for the door. For the read by act-id nothing could be
   added: only a stored fact that no act can write would show it. The pure
   sentence on `depot-as-said` is all that holds it.)
3. The seam's rewritten series had one sentence, the happy path, and none
   for any of its stops. (`cdbb6a5`)
5. The server's `send-all` still looped `send-act`. (`136838a`)
8. The fixture held no fact under a UUID key and no thing carrying a part
   that left. (`e374415`)
10. Entry 2's count of writers. 11. The judge's other modes carried no line
    that they are out of step. 13. Two wordings of the F2 marks, which now
    say only what run-5 says. (`cc2766a`, `105b483`)

For Sid, not mended:

- **4. An interrupt in the middle of a series** (section 4). The docstring
  now says it; the behaviour is as built.
- **6. Two rows under one answer key have a consequence that nobody had
  named.** The same thing, bit for bit, can now be answered by both gates
  under one key with one digest (before, the copy on the other gate's depot
  had another id and so another digest), and the door tells whose answer a
  row is by digest alone. And the read by act-id picks its record by the
  lane's depot as the fact says it now. So on the day a lane's speed
  changes, every earlier act's answer is looked for in the other record, and
  where a copy was put there the maker reads
  `:lane-belongs-to-the-other-gate` as the answer to an act that was
  accepted. Nothing reaches this today. It is written into entry 3's list of
  what a change of speed must still build, and it weighs for the stray rule
  that is already on his list (making such a thing a stray in `walk/begin`).
- **7. Rationing before the sends** (section 4).
- **9. The fact that stands in for `:subjects` does not find a person's
  acts.** `{:e <the act's uuid> :k :subjects :v #{…}}` is found by the act's
  id; by a person's id only when the value is that one id, since a read of
  what points at a thing matches the whole value; and the lane must mark the
  key first. He asked "how would this work?", and the page's answer was
  wrong on this. Entry 2 now says what is found and that the form is open.
  No code writes such a fact.
- **12. The author of the commits** (section 4).

What the reviewer tried to break and could not: a UUID as a key through the
check, the digest, the gate, the rows and the read path's query and range
order (no unguarded `namespace`, `name`, `sort` or `compare` on a raw key in
any tree); who may write a lane's depot fact (only a making act, only the
starting word, so the door, the gates and the read path cannot disagree
today); a third slot left anywhere in source; the count and order of a
series' results against the wire's words; the digest of a thing with no
`:depot`; his quoted lines, character for character; the three shared files.

What it left open: no cluster proof and no mutation was run by it; entries 1
and 2's questions and answers could not be held against the record; the
diffs of the proofs were read only where a finding needed a sentence.

## 6. What is left

- This branch on top of `auth-stores-2026-11-28` and
  `program-sign-in-2026-11-28` (item 6 of the handoff), and the one run of
  all the proofs.
- The question 4c in its own words, into entry 3, by a session with the
  record; and entries 1 and 2 held against the record.
- The read by act-id going by the lane's fact has no sentence of its own on
  a cluster.
- `MODEL.md` still gives a permission as `[who lane granted-in]`: that is of
  the permissions change, not of this one, and was left.
- The handoff page on `main` was not edited: its section 1 and section 4,
  items 1 and 5, are answered by this page.
