# The changes to stored forms of 28 November: what was built, and what is left

Written 28 November 2026 by the Claude Code session on Sid's machine that ran
as an orchestrator over subagents, for Sid and for the session that comes
next. It follows `HANDOFF-fixes-2026-11-28.md` and assumes nothing of that
conversation. Paths are from the repository root.

Sid stopped the session before the work was whole ("This session has been too
long i think its time to handoff now"). So this page is of a build that is
half made: read section 1 before anything else.

What is Sid's here: his answers of 28 November, ‹bfa41fde› ("Part a yes to
all") and ‹b8e1bdaf› ("Yes to all that i dont explicitly comment below", "4b
yes but then to get what we are removing how would this work? Ok we can do it
as ordinary fact as you mention", "4c yes should be controllable"), each to a
question that a session put to him in plain words («59efa1cb», «91f1705d»);
and his words on cost during this session, in section 3. Every other choice is
a session's and his to turn.

## 1. In short

The prompt asked for twelve changes in three stacked pull requests, each
reviewed by a fresh subagent, and a review of the two builds that were done.

| Piece | State |
|---|---|
| The review of the builds of 27 and 28 November | Done. Findings below. Nothing mended |
| Auth: the second secret (F6), a time that passes ends nothing (F17), who made an agent and whose ending ends it (F30) | Built, three commits, the builder's own proofs passed. Not reviewed |
| Auth: emails, accounts and invitations (F18) | Designed, with the steps written out. Not built |
| A program that signs in (F14, F15, what it is lent, Auth's spec's marks) | Designed. Not built |
| The act's form: a key may be an id (F23), `:subjects` and `:claimed-when` leave (F24), the depot word leaves the id (F22) | Built in the source; the store's pure and cluster proofs passed by the builders' word. The fixture, the judge and the pages are an unverified WIP commit. Not reviewed |
| The store's README marks (F2) | Not started |
| The three branches stacked, the one full run of the proofs, the pull requests | Not done |

Nothing is pushed and no pull request is open.

## 2. Where the work is

The session ran on Sid's machine, not in the cloud as its prompt assumed.
Local `main` was 12 commits ahead of `origin/main` (the build of 28 November),
the repository is public, and the push is Sid's, so the session pushed
nothing: the work is on local branches, each in a worktree outside the shared
checkout.

| Branch | Worktree | Holds |
|---|---|---|
| `auth-stores-2026-11-28` | `/mnt/data/projects/Softland-auth-stores` | `main` at `de68997d` plus `575ee201` (F30), `b4bb7da7` (F6), `93120b41` (F17). Clean, whole |
| `wip/emails-reference-2026-11-28` | none (same repository) | One commit on top of the above, `15ad2f4d`: an Opus builder's first minutes on F18 in `reference.clj` and its proof. Half made, never run. Use it or drop it |
| `act-form-2026-11-28` | `/mnt/data/projects/Softland-act-form` | `de68997d` plus `34a88494` (F24), `567018b8` (F23), `ea6cbb06` (F22, source and pure proofs), `37a5be43` (WIP: F22 in the cluster proofs), `18b605cf` ("the door, module and read-path proofs pass"), `620cf968` (the seam sends a series through `door/send-acts`), `4ad925e5` (WIP: the fixture remade, the judge, three pages: unverified) |
| (detached) | `/mnt/data/projects/Softland-review-done` | `de68997d`, the reviewer's. Can be removed |

The last three commits of `act-form-2026-11-28` are an Opus builder's that was
stopped before it reported. Their messages are its only word: nobody has read
the diffs of `18b605cf` and `620cf968`, and for `4ad925e5` it is not known
whether `run-again-proof` or the judge ran on that tree.

When the work is whole, the commands that are Sid's:

```sh
git push origin main
git push -u origin auth-stores-2026-11-28
gh pr create --base main --head auth-stores-2026-11-28
git push -u origin program-sign-in-2026-11-28      # cut from the head of the first
gh pr create --base auth-stores-2026-11-28 --head program-sign-in-2026-11-28
git push -u origin act-form-2026-11-28             # after it is put on top of the second
gh pr create --base program-sign-in-2026-11-28 --head act-form-2026-11-28
```

## 3. How it was run, and Sid's words on cost

The prompt, which a session wrote and Sid pasted, said each piece goes to a
subagent on Fable at high effort. The session did that for the review of the
finished builds and for the first two builders, and the session had used 8% of
Sid's Fable limit when he asked. His words, as typed in this session (their ids
were not looked up):

- "Ok also be aware of.not wasting tokens on random tests that can be run by
  opus and all that and one trst for all the changes instead of one for eaxch
  in each subagent and i hope the subagents are fable high not xhigh and not
  max"
- "why do you need fable then why dont we only use fable for tinking and then
  opus is for implementation???"
- "Like i suspected that that aince we know the bugs now we can fix rhem
  directly or no? Like each requores first thinking what is we need to do and
  only then do"

What the session changed on them, and what the next session should start
with:

- Fable settles the design and reviews; a builder is on Opus and is handed
  the settled design in a file. The two Fable builders were told to wrap up;
  two Opus builders took over and were stopped at the handoff.
- A builder runs only the proofs that hold what it changed. All the proofs
  are run once, at the end, on the branches stacked, by a runner on Opus that
  only runs and reports counts.
- A reviewer does not rerun proofs to see them pass.

What it cost, by the harness's counts of subagent tokens: the review of the
finished builds about 345k (Fable); the builder of Auth's three changes about
580k (Fable); the first builder of the act's form about 680k (Fable); the
runner of `read-path-proof` about 80k (Opus).

## 4. What is left, in order

The starters are in `starters-2026-11-28/` beside this page: each is a whole
brief for a subagent, with the rules of the repository that a subagent does
not load, what Sid said yes to, and the design as settled. Their scratch paths
under `/tmp` are this session's and are gone after a restart.

1. **The act's form, to its end** (`act-form-finish.md`). First read the
   diffs of `18b605cf`, `620cf968` and `4ad925e5`, since no report came with
   them. Then items 3 to 7 of the starter as far as they are not done: the
   fixture and `run-again-proof`, the judge's `fix` mode, digests written as
   constants, the pages, the new entry 3 of "Decisions of 28 November 2026 —
   the form of an act", and the marks of F2.
2. **Emails, accounts and invitations** (`emails-accounts-invitations.md`), on
   `auth-stores-2026-11-28`. The steps of each write are in the starter's
   section "The module"; they are this session's design and nobody has tried
   to break them.
3. **A review of `auth-stores-2026-11-28`** by a fresh subagent
   (`review-template.md`), and the mending. The points to push on are in
   "Auth's first three changes" below, under the choices.
4. **A program that signs in** (`program-sign-in.md`), on a branch
   `program-sign-in-2026-11-28` cut from the head of the Auth branch. Then
   its review.
5. **A review of the act's form.**
6. **The act's form on top of the other two** (it changes `sign_in.clj` in
   `making-act`, `name!` and `setup!`, `seam.clj` in `act-id-of` and
   `send-all-now`, and one line of `main.clj` in `give!`), then all the proofs
   once, by a runner on Opus, and the mending.
7. **This page brought up to date**, and the push and the pull requests,
   which are Sid's.

## 5. The three things Sid wanted to see

- **What replaces the take-out (F17): built.** In "Auth's first three
  changes" below, as the builder wrote it for him.
- **Where the door finds a lane's depot (F22): built in the source.** In the
  lane's own fact, `[lane :softland.store/depot]`, which the door already
  read when it made an id. The door now reads it for each send: one act costs
  a read and an append whether or not its id was made ahead; a series costs
  one read and n appends through the new `door/send-acts`. The read of an
  act's answer finds the record by the same fact, in the same turn on the
  lane's task, at one seek more. Where a lane starts stays a pure function of
  its id (the base on the microbatch depot, every other lane on streaming,
  the default): that is what lets both gates and the door agree on which gate
  makes a lane with no read and no race. A maker choosing another speed is the
  same step as changing a lane's speed later, a handing over from one gate to
  the other, and is not built. What a change of speed would still have to
  build: the handing over itself; the read path, which still picks tables by
  where a lane starts; and what to do with a refusal that the other gate
  already holds under the same id (below).
- **What it takes for an email to be erasable (F18): designed, not built.**
  Auth only ever matches an email, so it is given a hash and never the
  address: a claim is `{:by :email :hash H}`, and invitations then hold no
  address anywhere in Auth, its log included. What Auth keeps to be read is
  what her provider said (`:says`), with the account's `[provider subject]`,
  in one table and one depot: a new write, `unlink`, removes it from the
  table, and removing it from the log takes a Rama depot migration at a
  module update, which is an operator's step and not part of the design's
  build. Left with the session's view: the hash is plain, so whoever holds
  the log can test a guessed address; a hash keyed by a secret of the
  runtime would close that, at the price of a secret whose loss frees every
  bound email. `[provider subject]` could be given as a hash too. And `:says`
  has no reader in the contract today.

## 6. The act's new form

As `act.clj` declares it on `act-form-2026-11-28`:

```clojure
{:act
 [:parts
  [:version      [:enum version]                    {:reason :unknown-version :edition true}]
  [:who          :author-party                      {:reason :who-not-a-party}]
  [:lane        :id]
  [:permission   :uuid                              {:optional true :reason :permission-not-an-id}]
  [:session      :id                                {:absent :no-session}]
  [:based-on     [:map-of :fact-id :carried-stamp]  {:optional true :default {}}]
  [:because-of   :act-id                            {:optional true}]
  [:facts        [:seq-of :fact]                    {:empty :empty-act}]]
 :fact
 [:parts
  [:e        :id]
  [:k        :id]
  [:v        :value   {:optional true}]
  [:replaces :fact-id {:optional true}]]
 :act-id  [:tuple :id :uuid]
 :fact-id [:tuple :act-id [:int {:min 0}]]}
```

What arrives on a depot is `:act-id`, `:door-time`, then those eight parts:
the door's `:depot` addition is gone too. The names and shapes that the
builder chose inside this are listed in `starters-2026-11-28/act-form-finish.md`,
under "Decisions already taken".

## 7. Choices that reach across pieces, for Sid to turn

- **No push and no pull request from this session**, though the prompt asked
  for three. Section 2 has the commands.
- **Pieces not built keep their designs in the starters**; a yes of his
  covers each change as it was put to him, not these designs:
  - F18: the email as a hash; `invite` with an end (the runtime's number,
    seven days suggested); `take-back` and `unlink` as new writes;
    `:person-in-use` gone; a person's accounts as entries under her; no list
    of the invitations that name a person; "erase her" as `unlink` for each
    account, with no word of the runtime for it; unlinking ends no sign-in.
  - The program's sign-in: Auth itself refuses a yes that names the person
    and a `:program` sign-in for a person; `person?` is "her actor is her
    root and her sign-in is a browser's"; the commands that only a person may
    say stay in the command line's table and get the runtime's refusal; for a
    new agent the two boxes (see, write, in her own lane) start ticked, for
    an agent that exists they start unticked; the lend is her own act, sent
    after Auth's yes, under ids made from the sign-in so that a page posted
    twice lends once.
- **A thing on the other gate's depot** (found in the act's form): see "The
  act's form: what the first builder did and found" below. Left as it was.
- **The healing of what is watched after a clock far ahead** (found in F17):
  not built; the builder thinks it cheaper than the session weighed it.

## 8. Auth's first three changes (branch `auth-stores-2026-11-28`)

Built by a subagent on Fable at high effort, from the design the orchestrator
settled. 32 files, +1727 −716 against `de68997d`.

| What | Commit |
|---|---|
| F30: who made an agent and whose ending ends it are stored apart | `575ee201` |
| F6: a program that waits asks with a secret of its own for asking | `b4bb7da7` |
| F17: a time that passes ends nothing, and what is watched is no longer what stands | `93120b41` |

### What replaces the take-out (for Sid, as the builder wrote it)

Auth still takes a sign-in or a session out of what is watched at a write, when
its time is at or before that write's time. That keeps what is watched from
growing for ever, and it is now all that the take-out does. It no longer writes
"ended" on anything: "ended" is written only when someone ends the thing.

A sign-in that was taken out of what is watched still stands while its "good
until" has not passed. `who-is` says so. Each copy of the runtime holds the time
against its own clock at each read. Each write is judged against its own time,
so a write that a wrong clock got refused is taken when a right clock sends it.
The sign-in comes back into what is watched when its secret is next replaced; a
session, when its time is next moved on.

Because being out of what is watched is no longer an end, the watched value now
carries a count of the ends under the person, so that every real end is still
pushed to every copy. A copy that holds a connection whose sign-in is not in
the watched value reads Auth and decides by that read.

A server whose clock is far ahead now empties what is watched under the people
it writes under, and refuses things by its own clock. It ends nothing, and the
other servers carry on.

What is left over: until its secret is next replaced, a sign-in that still
stands is missing from the list of her sign-ins (`under`). She cannot see it
there to end it, though ending it by its id works.

One cost: `who-is` reads one record more for an agent's sign-in, because an
agent's end is read from the agent's record and no longer copied onto each of
its sign-ins.

### The choices, for Sid to turn

Where the builder departed from the settled design, or the design was silent:

- **An end reaches what is not watched, by reading (F17).** `end-actor` finds an
  agent's sign-ins only by scanning the watched set, so a sign-in that had left
  it would have kept standing after its agent ended, and a session after its
  sign-in ended. Built: the cascade still writes `ended?` and `closed?` on what
  it finds watched; `who-is` and each write read the rest from the record of
  the sign-in and of its actor (`signin-ended?`). The contract shows none of
  this (question 32). Cost: `who-is` 3 seeks with a session for an agent's
  sign-in (was 2), 4 when the session's sign-in is another; `replace-secret`,
  `end-sign-in`, `open-session` one seek more for an agent's sign-in;
  `close-session` one or two more. The other options: an index of what is not
  watched; or stop writing the cascade and always read, which is simpler (an
  end is now stated in two ways) but departs from "written by end-actor with
  what it ends".
- **Each thing leaves what is watched by its own time (F17).** A session stays
  watched while its sign-in is not (question 34), so thinning needs no scan.
  `LiveSession` holds its sign-in's `actor` so that `end-actor` finds it: a
  stored field beyond the three changes.
- **`:ends` counts writes, not things (F17).** A repeat, or an end of a thing
  already ended with one above it, counts nothing (question 33). It is the
  inner key `"ends"` of the root's map in `$$live`; the value always holds it
  (`:ends 0`).
- **The runtime refuses an ask whose two hashes are one (F6)**
  (`:read-the-word`): without it the credential's secret would be a valid
  asking secret. Auth does not compare the two.

Settled inside the design:

- F30: the field is `ends-with`; it starts as the maker and no input sets it.
  `end-actor` follows it and reads `made-by` for nothing; `under` gives both.
  The runtime's rule of who may end an agent (`beneath?`) reads `:ends-with`.
  The depth bound stays on `made-by`. The scan from the actor's `seq` still
  relies on "what ends with an agent was made after it", with a note at that
  site.
- F6: stored `:asking-hash` on the ask's event and its four states; `ask-of`
  gives it; on the wire `:asking-hash` and `:asking-secret` (the poll's field
  was `:secret`; a poll in the old form is refused for its form). Two asks are
  the same write when both hashes are the same; the same hash with another
  asking hash is `:in-use` (question 31). The yes page shows and checks
  neither. The library's `waited` takes what `ask` gave (`[url asked]`).
- F17: a sign-in or session whose time is at or before the `:at` of its own
  making is taken, not watched and not ended (question 9, turned). A repeat of
  `open-session` moves on only when the session's time and its sign-in's have
  not passed at the repeat's `:at` and nobody ended either. `:not-live` still
  comes before `:wrong-secret`. An ask that is over when made stays over
  (question 19). Questions 10 and 17 turned; 32 to 34 new. The ops are `thin`
  and `thin-watched-on-root!`.
- The runtime needed no code change for F17 beyond docstrings: `standing`
  already reads `who-is` whenever the caller is not in the pushed value.

### What ran (the builder's own runs)

| Proof | Result |
|---|---|
| Auth `reference-proof 42` | 192/0 at F30, 196/0 at F6, 206/0 at F17 |
| Auth `module-check` with no module | 36/0, 37/0, 40/0 |
| Auth `module-check` on the real module, 200 histories | 3/0 at each of the three; at the head 13,737 writes, 104,531 reads, 553 watches |
| `module-harness ends-with`; `step3 1` | passed; all passed, carbon's 9 batches with 0 failures |
| wire `pure-proof`; runtime `pure-proof`; `command-pure-proof` | 38/0; 26/0; 36/0 |
| runtime `proof`; the new `clock-ahead-proof` | 52/0; 4/0 |

Held against a broken copy: F30's sentences against a reference and a plan that
follow `made-by`; F6's "crosses no wire before her yes" against a library that
polls with the credential's secret; F17's three broken copies of the rules
(`:time-ends`, `:end-of-unwatched-not-counted`, `:passed-session-moves-on`), a
module that does not read the actor's record, and `clock-ahead-proof` over a
client that hands on no count of ends. The runtime-level sentences were not run
against the code of before F17.

Left to the full run: `two-lines-proof`, `open-session-proof`,
`review-faults-proof`, `command-proof`, `command-review-proof`,
`line-review-proof`, the seven attacks (`yes_page` and `sign_in_pace` have
changed forms), `full-life-measure` (its expected seeks changed), the harness
modes `slice-a`, `slice-b`, `slice-c`, `late-open`, `partial-outage`,
`unfit-record`. The cluster proof and the worker-kill proof were not run. The
model check of the agreement was not run: the steps of `answer-ask` are
unchanged, what they store changed.

### Seen and not changed

- **The healing alternative is cheaper than it looks** (the builder's view).
  Keep every `exp:` entry of what nobody ended, and per root the `:at` of its
  latest write, T. A write at `at'` reads only the entries between T and `at'`:
  it thins them when `at'` is later and puts them back when it is earlier. The
  first write from a right clock after one far ahead restores everything, and
  `under` is then exact. Cost: one small index entry per sign-in and session
  that nobody ended, one small write per root write, a point read of each
  record put back. Not built.
- `Caller.until` is stale after a replacement: true. `:who-am-i` on the same
  connection gives the old `:until`. No connection is ended or kept wrongly.
- No migration is written. A cluster that holds Auth's data of the old form
  must be made anew: a sign-in that the old take-out marked ended cannot be
  told from one that someone ended.
- `PLAN.md`, `BUILD.md`, `IMPLICIT_SPEC.md`, `PLAN_VALIDATION.md` under
  `spine/auth` and the agreement's model still describe take-out as an end and
  an ask with one hash; they are the record of that build.
- `opened-in` tells a `:not-live` from `open-session` as `{:why :ended}`, also
  when the cause is a time that passed.

## 9. The act's form: what the first builder (Fable, high) did and found

Branch `act-form-2026-11-28`. Commits: `34a88494` (F24), `567018b8` (F23),
`ea6cbb06` (F22, source and pure proofs), `37a5be43` (WIP: F22 in the cluster
proofs, edited, never run). It was stopped on Sid's word (Fable thinks, Opus
builds) with the source whole and the pure proofs passing; an Opus builder
finishes. The decisions it took are listed in
`starters-2026-11-28/act-form-finish.md`, under "Decisions already taken".

What it found that the design did not foresee, for Sid:

- **Two rows under one act-id are now possible.** The same thing appended to
  both depots (only an appender other than the door can do it) is accepted by
  the lane's gate and refused by the other, under the same answer key in two
  records. The door's read shows only the lane's own. A later change of speed
  would find the refusal standing under an answer key of the lane, with a
  last-stamp count and line rows already there. Making such a thing a stray in
  `walk/begin` would prevent it in a few lines; that changes a store rule, so
  it is left for Sid's word. Kept as today: refused
  `:lane-belongs-to-the-other-gate` in that gate's own answers, with a stamp of
  that gate's own count.
- **F24: no `:subjects` fact was added anywhere.** In all four places the act's
  own facts already name her: the making act and the two gives as `:holder` of
  the permission they write, the name act as `:e`. A plain `:subjects` fact
  would also have been readable by whoever may see the lane (anyone, in the
  base), where the part was read only with the act whole, and it would need
  `:write`, so a holder who may give but not write would have her give refused.
- **A given act-id no longer saves a read.** One act costs a read and an append
  either way. A series of n costs 1 + n through the new `door/send-acts`; the
  seam still sent one act at a time (2n) when the builder stopped.
- **The answer read costs one seek more** on the lane's task, for the depot
  fact.
- **The read path still picks tables by `depot-at-making`** (sources.clj,
  acts.clj, lanes.clj): a change of speed must change that too.
- `digest.clj` held the id's form too (`under-some-permission`,
  `replaced-uuids`).
- `line-statements` does not load on `:store-module` (it needs
  `piece3_statements`); not caused by this change.
- F24's and F23's new sentences each failed against a scratch copy with the
  earlier act.clj; F22 was not held against a broken copy.

## 10. The review of the two finished builds

By a fresh subagent (Fable, high) that saw none of the building, at `de68997d`. It
changed nothing. Each finding says how it is known: **ran** (a probe of the
reviewer's printed it), **read** (source only), **inferred**. The fixes of 27
November were reviewed as they stand at `de68997d`, since the command line was
rewritten into one table on 28 November. Nothing here is mended: these are for
Sid.

### The build of 27 November

What holds:

- F3: no `:own-layer` is left in Auth's spec (read).
- F7, first half: each command with one argument too few or too many ends with
  2 before anything is connected (ran, 45 typed lines).
- F8, the main case: an act edited in place, acts in another order, and one act
  twice in a file each keep the right UUIDs (ran `journal-of`).
- F9, the cases the handoff lists (ran `status-of`).
- F10, the three decisions: no line of `main.clj` reads a status any more
  (read).
- F10, F11, the line's own refusals: a word that the check refuses gets
  `{:id n :gives …}`, a `:watch` too (ran). A number in use and the two limits
  were read, not run.
- The library hands on the word of a refused opening and of a `:bye` (read).

What breaks:

- **F8: the same act still gets a new UUID in four cases, so the store would
  take it twice.** Medium: this is the harm F8 was to remove.
  - A journal that does not read, or reads as something that is no map, counts
    as empty: every act gets a new UUID and the journal is written over without
    a word (`main.clj:98-102`, `427-430`). Ran. It should end as a "could not".
  - The journal keeps only the entries of the run that wrote it. She sends the
    file, an agent sends it with `--as`, she sends it again: her entries are
    gone and her acts go under new UUIDs (`main.clj:407-413`). Ran.
  - An act taken out of the file, sent, and put back gets a new UUID (ran).
    Arguable.
  - Two `send`s of one file at once: the journal is written by `keep!` with no
    lock, though `keep!`'s docstring says the caller holds the file
    (`main.clj:430`). Read, not run.
- **F11: a watch that is not opened still answers two ways.** Medium for a
  client that speaks the wire itself; the library folds both into `{:ended}`.
  The line's own refusals answer `{:id n :gives word}`; a watch that the seam
  refuses answers `{:id n :ended word}` (`seam.clj:985-992`, `998-999`;
  `line.clj:173-175`): a query that names another asker, the cap for each
  person (`:too-many-watches`), a query that the read path cannot read. Ran: a
  visitor's watch with no query gave `{:id 0 :gives …}`; one with an `:asker`
  gave `{:id 1 :ended {:could-not :send-as-another}}`. No sentence of
  `line_review_proof` holds a watch that the seam refused.
- **F9: a "could not" inside `:answer` ends with 0.** Low to medium. When the
  read of an act's answer throws, the seam puts the door's
  `{:could-not :read-the-answer}` under `:answer` of the receipt
  (`seam.clj:588`); `status-of` looks for `:could-not` only at the top of each
  result (`main.clj:217-221`). Ran: a `write` with that result gives 0; a
  `send` with one refusal and one such result gives 3, where the handoff says
  1.

Still open:

- A refused act is pinned to its UUID (inferred, not run): the journal gives a
  refused act its old UUID, and the record answers a repeat with the first
  answer, so a file sent again after the cause is mended would get the old
  refusal until she edits the act or deletes the journal. This is E4. One run
  on a cluster settles it: refuse, mend, send again.
- A file copied or renamed loses its journal, since the journal is beside the
  path.
- Not looked at: the texts of F4, F5 and F6 beyond their diffs; the four points
  of that handoff's 3.6.

### The build of 28 November

What holds:

- F13: an act with an unknown part and version 2, no version or a nil one is
  refused `:unknown-version`, at the maker's check and at the gate's; with
  version 1 it is `:unknown-part` (ran).
- F33, F34: no reader of `:no-fact`, of `[:run …]` or of the old reason words
  is left in src-server, src-server-browser, src-wire, src-command,
  src-runtime, src-cli, src/app or their tests (grep). `[:run a b]` is now
  refused `:asks-no-word-known` (ran).
- F12: held word by word against the old `call-now`: the table gives the same
  answer for all sixteen words (read). Not held on a real line, as the handoff
  says.
- The library reads its own frame with the runtime's function on the same
  text, so the verdicts are the same (read). Size is the stated exception.
- A word that ends its own connection: the result comes before the last word
  when the end arrives during the word, also when the word throws and when a
  second such word waits behind it (ran, staged).
- The table and the flags: a flag of another command, a flag twice, a flag with
  no value, `--lane=x` and `-lane x` each end with 2 and are not run (ran).
- F1: the diff keeps the old sentence, the question and his answer. ‹e17932a7›
  was not looked up.

What breaks:

- **F7, second half: counting numbers refuses everyday text and still lets
  number slips through** (`begins-as-data?`, `main.clj:190-195`). Medium as
  behaviour; no harm to data, since a refusal is loud. All ran.
  - Refused, though a person means text: `3rd`, `1st place`, `555-1234`,
    `12:30`, `10.0.0.1`, `2026-11-28`, `100%`, `1,000`, `9lives`, `:)`,
    `#hashtag`, `#1`.
  - Still stored without a word, and not as typed: `017` becomes 15, `0x1F`
    becomes 31, `.5` becomes the text ".5", `5 ;c` becomes 5, `(maybe)` becomes
    a list.
  - Uneven: `1 2` is refused, while `nil x` and `true false` are text.
  - The reviewer's view: take the narrower reading, where the seven opening
    characters are data and a text that begins with a digit and does not read
    as a number stays text. It loses only `12O` and gives back dates, times and
    phone numbers.
- **After `:sign-out` has given its result, words sent behind it on the same
  connection are still done.** Low to medium. Ran: `:sign-out` answered at 13
  ms, then `:who-am-i` and `:send` each got a result, and the last word came at
  412 ms, when the standing said the end. The line clears its mark after the
  result and takes the next word (`line.clj:214-228`). This was so before the
  build, but the build made the line know which words end their connection and
  chose to hold the standing's end, not to stop at the word's own success.
  After such a word succeeds, no further word should be done. The wait for the
  held end has no bound (the handoff says so). Ran: with a call of Auth that
  never returns, the connection of an ended sign-in stays open.
- **`give who lane bogus` connects before it is refused.** Low. The third
  argument is tested inside `with-line` (`main.clj:473-485`), so the session
  can be written to the file, and the secret replaced, before the command ends
  with 2.
- **The `:why` names the wrong fault for a flag that is not known and stands
  last.** Low. `who --help` and `bogus --lane` say `:flag-with-no-value` (ran;
  `main.clj:722`).
- **F13's run-again claim shows no regress, not the fix.** The fixture holds no
  thing with an unknown part and another version, so "the tables stand" cannot
  see the change; the fix is held by the pure sentences only. Two side effects,
  both ran: a map that is no act at all (`{"a" 1}`) now says
  `:unknown-version`; a thing of version 2 with no door time says
  `:no-door-time`, but with an unknown part added it says `:unknown-version`.
- **One page made stale.** `test-runtime/softland/runtime/attacks/README.md:142`
  shows `:holds :no-fact` in a recorded output.

Still open:

- F12 and the order of the end have no sentence on a real line beyond what the
  handoff names.
- A frame nested so deep that one JVM's reader runs out of stack and the
  other's does not: not looked at.
- `scripts/fire` was read, not run. The cluster proofs of the runtime and of
  the command line were not rerun.

### The sentence of `read-path-proof`

**It fails on both commits, so it is not of the build of 28 November.** The
reviewer's own runs: on `90cea545` 3 runs, 1 failed; on `de68997d` 2 runs, 1
failed. Then eight runs by a runner on Opus, alternating the two commits, each
about 63 seconds once it had the cluster lock, none thrown or timed out: on
`90cea545` 4 runs, 2 failed (runs 3 and 4); on `de68997d` 4 runs, 1 failed (run
1). Together: 3 of 7 on `90cea545`, 2 of 6 on `de68997d`. Every failure is the
same sentence at the same place (`read_path_proof.clj:132`, in
`what-shapes-a-read-changes-under-it`), with the same one difference. The
runner's runs print 244 checks where the handoff of 28 November says 745: the
two count different things, and nobody reconciled them.

In both failures one thing differed: `(every? :anew erin-later)` was false. The
places, the screen and the read asked once were equal, and she had two messages
after the first where a passing run has one.

The reviewer's view, inferred from the code (no failing run printed the
messages): **a fault of the sentence, not of the read path.**

- When the nearest lane joins, one step joins four pushes at once: the lane's
  order and three checks of the farther places (`read_path.clj:790-795`,
  `sources.clj:307-343`).
- The whole answer is told by the first arrival step that finds every wanted
  push given (`read_path.clj:452-458`, `554-565`). The arrival steps of the
  other pushes of that join run after it (`read_path.clj:643-653`).
- Each of those that is a check then tells its place again as `{:changed …}`:
  `after-arrival` says "a check that arrives tells its place, with no test"
  (`answer.clj:471-472`, `495-497`, `516-517`).
- So the extra message repeats what the whole answer said. It is the one place
  in the proof where a lane joins with checks, which fits that only this
  sentence fails.

The sentence asks for more than the read path promises: that every later
message is a whole answer. It should hold that a whole answer comes with her
own place in it, and that the screen ends at what a read asked once gives. If
Sid wants no repeated place after a whole answer, that is a change of the read
path (an arrival that tells nothing when the whole answer already held the
push), not a mend of this proof.

## 11. Not done, and not checked

- **No build of this session is reviewed.** The prompt asked that each be
  reviewed by a fresh subagent before its pull request. The Auth branch's
  three commits and the act's form are as their builders left them.
- **All the proofs together were not run.** Each builder ran what holds its
  own change; the lists of what it left to the one full run are in its
  section. The runtime's cluster proofs, the command line's proofs and the
  seven attacks ran on neither branch after its changes, but for the
  runtime's `proof` on the Auth branch.
- **The steps of emails, accounts and invitations are design that nobody has
  tried to break.** They rest on this session's reading of how Rama runs a
  record again under `:all-after`; the starter says where.
- **No model check ran on anything.** `spine/auth/agreement/` and carbon's
  `step1` scenarios still hold the old contract (take-out as an end, an ask
  with one hash).
- **No migration of Auth's stored forms is written.** A cluster that holds
  Auth's data of the old form must be made anew.
- **The record** was opened for his two lines of 28 November and the two
  session texts they answered, and for nothing else.
- **Pages out of step, not edited:** `DECISIONS-AND-FIXES.md`, section 7,
  still lists as to do what is built; `FIRE.edn` beside this page says
  nothing of this work.
- The findings of the review of the two finished builds are mended nowhere.
