# Auth checks

[Parent: softland](../README.md) · [Auth's spec](../../../src/proposal/runtime-2026-11-21/spine/auth/SPEC.md)

This folder holds the reference of Auth's contract, the proof that holds it to
the spec, and the run that holds a module against it. The reference is the
contract written a second time, in plain Clojure, from the spec alone. A build
of the module, or of the runtime that calls it, can be held against it, call by
call. It reads no clock, opens no cluster and requires nothing outside Clojure.

| File | What it is | State that it changes |
|---|---|---|
| [reference.clj](reference.clj) | The twelve writes as a pure state machine, the pure reads of a state, a thing that stands for a cluster in which Auth is launched under the names it is given, and the eighteen functions of the contract, `open` among them, so that a program can stand on it. | The atoms of a cluster, in memory, one for each name: the state, the watches, which clients are closed. Every client opened under one name shares its atom, as copies of the runtime share one Auth. |
| [reference_proof.clj](reference_proof.clj) | The checks of the reference: by cases, by generated histories, by broken copies. | None outside its process. |
| [histories.clj](histories.clj) | The generator of the histories that both checks send: random sequences of calls made to meet the state often, with repeats, inputs that do not fit, and a clock that now and then goes back. | None: a world is a value, with a random source of its own. |
| [module_check.clj](module_check.clj) | The run that holds a module against the reference through the contract alone: every result, every read and what every watch came to, compared call by call. | It writes to the module that it is given. Run on the reference, nothing outside its process. |
| [QUESTIONS.md](QUESTIONS.md) | Each place where the spec does not say what happens, or says two things, with the reading that the reference takes, and which of them Sid's calls of 22 November settled. | None. |

The docstrings of `reference.clj` and the sentences of `reference_proof.clj`
cite `QUESTIONS.md` where a rule or a check stands on one of its readings. A
turn of a question changes the three together.

## reference_proof.clj

```sh
clojure -M:auth -m softland.auth.reference-proof [seed]
```

**What it checks.** One check for each sentence of the spec that says what a
call gives or refuses, and the clients that `open` gives over a cluster of the
reference, with races of two copies of the runtime among them. Then generated
histories, from the seed that the run prints: random sequences of calls with
repeats, inputs that do not fit, and times that pass and now and then go back.
A rule of its own is checked after every step of every history. Last, each
broken copy of the rules (`breaks` in `reference.clj`) is run through the same
histories, which must catch it.

**What a pass shows.** Each sentence holds for the cases written. Every rule of
the histories held after every step of every history of that seed. Each broken
copy was caught, and not only because it threw. Run with no seed, it takes the
clock's, so each run makes histories of its own. Given the printed seed, it
makes the same ones again.

**What it changes outside the code that it checks.** Nothing.

**What no check reaches.**

- What a read costs, the bound of what one task watches, and that no task is
  one that every person's calls go through. Those belong to the module and its
  plan, not to a state machine.
- `:softland.auth.client/not-reached`: the reference never gives it.
- In this proof, `watch-root` is checked by cases, and `:too-deep` by a case.
  The run of `module_check.clj` holds watches open through its histories and
  makes 32 agents in a line in a quarter of them, against the reference too.
- The client's lock is checked by three races of two threads, a hundred times
  each. No race of many threads is written.
- Where no read of the contract shows a fact (which account is linked to whom,
  which claim names whom, a link's `:says`), the histories read the
  reference's state map, whose form `reference.clj` gives. A check of a module
  can read those facts only through results, and `:says` not at all.
- Erasing, which the spine does not build.

## module_check.clj

```sh
clojure -M:auth -m softland.auth.module-check [seed [histories [module]]]
```

**What it checks.** It opens two clients of the module with the contract's
`open`, as two copies of the runtime, and sends generated histories through
them and through the reference, 1,000 unless told otherwise. A step is a write,
a repeat of a write that was taken, a write whose input does not fit, a read,
or a watch registered or stopped. Each write goes through one client or the
other. After it the run makes `under`, and `who-is` of each of the history's
sign-ins and sessions, for every root that the write could change, and
`ask-of` of each of its codes after an ask or an answer. It compares each
result and each read with the reference's, and what each watch came to with
what is live under its root in the reference. It reads nothing of the module
but what the eighteen functions give.

What a real module needs of it, and what the run does:

- A watch's calls come later, on another thread: after a change under a watched
  root the run waits, a bound at most, until the watch comes to the reference's
  value. Values that it passed through are not compared.
- A `:could-not` from a write, but `:read-the-event`, says nothing of whether it
  was taken: the run sends the write again, a bound of tries at most. A read is
  tried again so too. A `:could-not` is compared by its word alone.
- One Auth for all the histories of a run, since a launch for each would take
  hours: each history's ids, accounts, emails, hashes and codes are its own, and
  each history's reference starts empty. A run needs a module that nothing else
  has written to.
- A quarter of the histories make 32 agents in a line, then one more under the
  deepest, which is `:too-deep`, and a sign-in for the deepest. Two rules of
  Sid's calls are met in every run: after an `own-layer-made`, a copy of the
  person's first enter is sent again, now and then; and after a replacement
  whose new hash is the one before the present one, the replacement before it.
- A watch that says `:softland.auth.client/not-reached` differs: in a run on one
  machine nothing is lost.

Without a module, it holds the reference itself three ways: as it is; with its
watches called up to 3 ms late, each on a thread of its own; and losing the
answer of one call in 20, half before the call is made and half after. Then
each broken copy of the reference and four broken clients over it: one that
never calls its watches, one that calls them for every second change alone, one
that says not-reached, and one whose reads are one write behind.

**What a pass shows.** For the printed seed, the module gave the reference's
result for every write and every read of every history, and each watch came to
the reference's value after each change under its root. `open` of a name that
is not there gave `:find-the-module`, and a closed client gave `:closed`, as
the contract says. Run without a module, it shows that the run passes the
reference however late its watches and however often its answers are lost,
and that it catches each broken copy and broken client, but for the one that no
call of the contract can show.

**What it changes outside the code that it checks.** It writes to the module
that it is given, and closes its clients at the end.

**How a module is held to it.** Write a function of no argument that launches
the module, in an in-process cluster for instance, and gives
`(module-check/implementation (module-check/calls-of 'softland.auth.client) cluster module-name :close close-fn)`,
with `:watch-bound-ms`, `:settle-ms`, `:tries` and `:retry-pause-ms` if the
defaults (10 s, 200 ms, 8, 100 ms) do not fit. Give its full name as the third
argument.

**What no check reaches.**

- A real module: none is built. The defaults of the waits and tries are set for
  a module on one machine and are not measured.
- A link's `:says`: no result and no read of the contract gives it, so the
  broken copy `:says-of-every-enter` is not caught, and no run through the
  contract can catch it.
- Calls at one moment. The two clients take turns: each call returns before the
  next is sent. Whether one order explains the results of calls made at once
  is a check of its own, not written here.
- The values that a watch passes through on its way to the reference's value.
- Inputs that a careless client could hang on, an infinite sequence or a map
  whose order throws: the reference's proof sends them to the reference, and
  this run sends none to a module.
- What a read costs, and the bounds of the module's tasks.

## The build of 22 November 2026

Asked for in a prompt that Sid pasted: bring Auth's spec and its reference to
his two calls of 22 November, then a third on question 12, and write the run
that holds a module against the reference. Three commits:
`234960bd` (the spec), `7cebbac2` (the spec and the reference brought to the
calls, with question 12), and the commit that holds this section (the run).

### What changed

From the commit of the reference that this build starts from, `20c1efda`, to
the end of the build (`git diff --stat 20c1efda`, over the paths of this build):

```text
 src/proposal/runtime-2026-11-21/spine/auth/SPEC.md | 149 +++++---
 test-auth/README.md                                |   8 +-
 test-auth/softland/auth/QUESTIONS.md               | 185 +++++++---
 test-auth/softland/auth/README.md                  | 293 ++++++++++++++--
 test-auth/softland/auth/histories.clj              | 383 ++++++++++++++++++++
 test-auth/softland/auth/module_check.clj           | 770 +++++++++++++++++++++++++++++++++++++++++
 test-auth/softland/auth/reference.clj              | 368 +++++++++++++-------
 test-auth/softland/auth/reference_proof.clj        | 615 +++++++++-----------------------
 8 files changed, 2067 insertions(+), 704 deletions(-)
```

### What was run

The reference's proof, with the seed of the earlier runs and with the clock's:

```text
$ clojure -M:auth -m softland.auth.reference-proof 42
190 passed, 0 failed.          (the same output, line for line, as before the generator moved)
$ clojure -M:auth -m softland.auth.reference-proof
Seed of the histories: 1795327401244
...
190 passed, 0 failed.
```

The run, without a module, with the clock's seed and 1,000 histories:

```text
$ clojure -M:auth -m softland.auth.module-check
Seed of the histories: 1795327693487
The module: the reference itself, three ways, and then each broken copy of it.
PASSED The module, the reference itself, gave what the reference gives, through the contract alone, in 1000 generated histories of 81374 steps: every result of 71943 writes (8922 of them repeats, 4243 whose input does not fit), every one of 546688 reads of who-is, ask-of and under, and what each of 2711 watches came to after each of the 3903 changes under its root; with 250 deep segments of 32 agents in a line, 307 times :too-deep, and 0 calls sent again after a :could-not that said nothing.
(The histories took 2.0 s.)
PASSED open of a name that the cluster does not hold, and of a thing that is no cluster, gives {:could-not :find-the-module}.
PASSED After close!, each of the sixteen calls on that client gives {:could-not :closed}, watch-root's in place of the function; close! gives nil, also again; no function that it watched with is called again; and the other client goes on.
PASSED The module, the reference itself, with its watches called up to 3 ms late, on threads of their own, gave what the reference gives, through the contract alone, in 1000 generated histories of 81374 steps: every result of 71943 writes (8922 of them repeats, 4243 whose input does not fit), every one of 546688 reads of who-is, ask-of and under, and what each of 2711 watches came to after each of the 3903 changes under its root; with 250 deep segments of 32 agents in a line, 307 times :too-deep, and 0 calls sent again after a :could-not that said nothing.
(The histories with late watches took 19.9 s.)
PASSED The module, the reference itself, which loses the answer of one call in 20, made or not, gave what the reference gives, through the contract alone, in 1000 generated histories of 81374 steps: every result of 71943 writes (8922 of them repeats, 4243 whose input does not fit), every one of 546688 reads of who-is, ask-of and under, and what each of 2711 watches came to after each of the 3903 changes under its root; with 250 deep segments of 32 agents in a line, 307 times :too-deep, and 32628 calls sent again after a :could-not that said nothing.
(The histories with lost answers took 1.5 s.)

Each broken copy of the reference, as the module:
PASSED Through the contract alone, the run catches the copy of the rules in which an answer whose time is the ask's end is taken: history 13, step 31, a-result-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which an answer to an ask that has an answer, unless it is the same answer, meets the refusals first, :over among them, and only then is told what stands: history 21, step 74, a-result-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which a sign-in or a session whose end is at or before the time of the event that makes it is live: history 0, step 15, a-read-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which an ask under a code that is bound to another hash takes the code: history 0, step 31, a-result-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which an email with a capital letter fits, and is compared as given: history 3, step 19, a-result-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which a repeat of open-session opens a closed session again: history 14, step 37, a-watch-came-to-another-value.
PASSED Through the contract alone, the run catches the copy of the rules in which a thing whose end is the time of a write is still live at that write: history 1, step 49, a-read-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which end-actor ends the actors and their sign-ins, and leaves the sessions of those open: history 52, step 21, a-read-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which end-sign-in ends the sign-in and leaves its sessions open: history 24, step 36, a-read-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which end-actor ends the agent and the agents that it made itself, and none further down: history 27, step 70, a-read-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which a repeat of begin-sign-in makes an ended sign-in live again: history 0, step 26, a-watch-came-to-another-value.
PASSED Through the contract alone, the run catches the copy of the rules in which an enter that is a copy of one that its link decided gives what that one gave, its :own-layer-made? too: history 0, step 29, a-result-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which a replacement that arrives after a newer one is taken as a repeat: history 4, step 18, a-result-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which a later answer to an ask takes the place of the answer that stands: history 10, step 40, a-result-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which an enter of an account that has a link, which proves another invitation that waits, moves the link to that invitation's person: history 2, step 21, a-result-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which a label of more than 256 characters fits: history 0, step 13, a-result-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which a part that the contract names may be left out, and reads as nil, and a part that it does not name is dropped: history 2, step 13, a-result-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which the parts of a no that it does not judge may be anything: history 20, step 85, a-result-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which a provider's name of more than 256 characters, and an :own-layer that is no UUID, fit: history 6, step 12, a-result-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which a write that is refused takes nothing out of what is live: history 0, step 21, a-watch-came-to-another-value.
PASSED Through the contract alone, the run catches the copy of the rules in which a repeat of begin-sign-in takes its own until and label in place of the first ones: history 0, step 26, a-read-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which a repeat of open-session takes its own driven-by: history 43, step 63, a-read-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which a sign-in keeps each replacement that it took, and refuses one that it took before, also when that one's old hash has come back and is the hash now: history 50, step 40, a-result-is-not-the-reference's.
NOT CAUGHT, as no run through the contract can: the copy in which every enter of an account that has a link keeps its own :says, whatever its :at; no result and no read of the contract gives :says, so a link's :says can be held against the spec only by reading the module's tables.
PASSED Through the contract alone, the run catches the copy of the rules in which a person who has a claim is invited under another, which takes the place of the first: history 2, step 12, a-result-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which a sign-in's time is the later of its present secret's and the secret's before it: history 1, step 49, a-read-is-not-the-reference's.
PASSED Through the contract alone, the run catches the copy of the rules in which an input that does not fit, but names a root and a time, takes out what has passed under that root: history 20, step 42, a-watch-came-to-another-value.
PASSED Through the contract alone, the run catches the copy of the rules in which a claim that an account has used up is bound to another person by a new invite: history 7, step 10, a-result-is-not-the-reference's.
(The broken copies took 5.9 s.)

Each broken client over the reference, as the module:
PASSED Through the contract alone, the run catches a client over the reference that never calls its watches: history 0, step 21, a-watch-came-to-another-value.
PASSED Through the contract alone, the run catches a client over the reference that calls its watches for every second change alone: history 0, step 24, a-watch-came-to-another-value.
PASSED Through the contract alone, the run catches a client over the reference that says :softland.auth.client/not-reached at a watch's first change: history 0, step 21, a-watch-said-not-reached.
PASSED Through the contract alone, the run catches a client over the reference that returns from a write before its effect can be read: its reads are one write behind: history 0, step 9, a-read-is-not-the-reference's.
(The broken clients took 0.1 s.)

36 passed, 0 failed.
```

Outside the commits, a scratch run over six seeds (42, 7, 1234567, 99999,
31337 and the clock's) caught each copy that the contract can show by history
219 at the latest, `:replacements-remembered` being the last. Each of the 20
copies that were there before this build is caught through the contract
alone. Of the eight that it adds, seven are, and `:says-of-every-enter` cannot
be, since no result and no read gives `:says`.

### What is open

- No module exists yet. The run has been held against the reference, the
  reference with late watches and with lost answers, 28 broken copies and 4
  broken clients; never against a module built from the plan.
- `:says-of-every-enter` cannot be caught through the contract (above).
- Calls at one moment, through two clients at once, are not in the run.
- Question 17 bears on the runtime. Another session's review of the runtime's
  spine (`177d252a`) says: "the seam reads a repeat's {:open S} as moved on; a
  closed session is then kept only when its close is never pushed, up to half a
  span." The reference keeps question 17 as Sid let it stand.
- Question 28 of `QUESTIONS.md`, named and not changed: `:sign-in-order`
  keeps every sign-in that ever began, which only a read of live ones uses, and
  a cluster's atom keeps in `:closed` every client that was ever closed.
- Questions 8, 9, 11, 13 to 16, 18, 19, 21, 22 and 25 to 28 are still Sid's to
  turn, and 10, 17 and 24 stand as the reference took them and stay his to turn.
- The wording of the calls is a session's, in prompts that Sid pasted (by the
  record, first written at 03:57 UTC that day). The spec marks them as his
  calls, in the prompt that he pasted.

### Choices of this session, for Sid to turn

In the spec:

- The calls are written where each belongs, with the date, and the readings of
  questions 3, 4, 5, 10, 17, 23 and 24 are now said in the spec's words.
- "What the runtime does, and Auth does not check" keeps the runtime's order
  of calls and how often it calls; the order of refusals went to the rules for
  every call, a session's id that is another sign-in's to open-session, the
  forms to "What fits", `watch-root` and `close!` on a closed client to the
  words of `:could-not`, the 20,000 watched roots to full life.
- `open` given a thing that stands for no cluster gives `:find-the-module`, as
  a name that is not there does: the contract gives `open` that word alone.

In the reference and its proof:

- A provider's name is counted with its namespace: `:g/x` is three characters.
- Eight broken copies more, each taking a reading that the calls set aside or
  let stand against another (listed in `breaks`). Two keep a memory of calls
  that the reference no longer keeps, so that a module which keeps it is caught.
- The generator of histories moved out of the proof into `histories.clj`,
  shared with the run; for seed 42 the proof gives the same output as before
  the move. The histories now and then bring the hash before the present one
  back, so that question 12's rule is met.
- The check of lower-case emails takes one case more: an enter of an account
  that has a link, with a capital letter, does not fit (question 3, which
  stands).

In the run:

- One Auth for the histories of a run, each history with pools of its own; so
  a run needs a fresh module.
- A history stops at its first difference; the run prints ten at most.
- Reads after every write, of every root that it could change: about seven
  reads a write, which a module over Rama pays for in time.
- A watch is compared by what it came to; a call at its registration is taken
  when it holds the value that was live then.
- Not-reached is a difference, since nothing is lost on one machine.
- The defaults: a watch's bound 10 s, a pause of 200 ms at a history's end,
  8 tries, 100 ms between tries, growing.
- The two follow-ups that meet question 6's and question 12's rules in every
  run, and the four broken clients, beyond the 28 broken copies.
