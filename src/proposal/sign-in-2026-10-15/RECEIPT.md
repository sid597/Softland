# Sign-in: the receipt

**16 October 2026.** The code this receipt describes was taken off `main` by
3a41c91d: the store, its checks and `deps.edn` are as at 59b2a703, and
`src-runtime/` and `test-runtime/` are gone. The build went past what Sid asked
for, "sign-in gives us a bunch of data and we just store that data in rama"
(‹3165b020›). This page and `PROGRESS.md` stay as the record of what was built
and taken off; the 17 rows of the queue that `BUILD.md` pointed at itself have
their 14 October text again. Neither page is a source for the next build.

The build of `BUILD.md`, 15 October 2026, by the build session (Opus 5.5)
and its builders. The working, step by step, is in `PROGRESS.md`; this page
says what exists now, what was run, what was not reached, and what waits for
Sid. Nothing here is a decision of Sid's unless it cites one.

## What sign-in gives now, and the evidence

Every act and read that goes through the runtime carries a name that was
proven, not claimed: a person by Google's ID token, an agent or a program by a
token that a person made for it, the system by the runtime's own steps.

Proven on Rama's in-process cluster with a stand-in for Google (a key pair of
the proofs' own that signs ID tokens as Google does), through the service and
over HTTP:

- a first sign-in by invitation, and a later one; a stranger with no
  invitation refused; a second account linked while signed in;
- an agent made, given a right in its person's layer, its token made, an act
  in its run accepted and read back named as the agent, beneath its person;
  the token ended and the agent's next act refused;
- a token asked for by an agent and approved in the person's browser;
- a client that claims another's name refused;
- a read by someone not signed in sees the base and not a members layer; a
  layer for anyone signed in is seen by a signed-in person only;
- a right ended, and the agent's next act refused; a key the right does not
  list refused; an agent under "see and act" refused when it gives a right or
  changes a setting;
- a blocked sender stopped at the runtime and counted, also through a program
  that a blocked person made; limits of acts and bytes a minute;
- a name claimed, changed, refused while taken, and taken again after its
  wait;
- a first sign-in cut after each of its acts, and two first sign-ins of one
  account at the same moment (ten rounds): one person each time.

Not yet proven: Google itself, in a browser. That is Part 3, below.

## The commits

- `12621884` — the store's four changes (a right can end and lists its keys;
  "anyone signed in"; one write for a place), the run-again fixture made again,
  and the decisions of 15 October in `src-store/README.md`.
- `2af6e6d6` — the store after its review: a bound of 8,192 bytes on a
  permission's text; a reader who is not signed in told less; wording.
- `7f35472c` — the runtime, `src-runtime/` and `test-runtime/`, with the
  aliases `:runtime` and `:runtime-module`.

## The fixture of the proof that runs the gates again

Made again on purpose, as 13 October entry 6 asks when a rule changes. The
first making decided exactly as the old fixture had (the same rows in each
table, the same count of every refusal word in both gates): no act in it
reached the new rules. So a scenario was added, `rights-and-claims!`, that
reaches each one in both gates (a right listing keys, a key refused, a give
over the giver's, an end, an end from below refused, a right beneath an ended
one, the same right given again above, "see" raised by an end and a give, a
one-write key and two claims of one place), and the fixture was made again:
390 acts, 566 rows. The run-again proof passes on it with no row that differs.
The bound on a permission's size, added after the review, reaches no act of
the fixture; the fixture was not made a third time.

## What was run, on the final source

| Proof | Count |
|---|---|
| act | 176 |
| pure (store) | 765 |
| read path, pure | 234 |
| module | 469 |
| door | 277 |
| read path | 578, in two runs of three (below) |
| run again | 46 |
| runtime, pure | 88 |
| runtime service | 43 |
| runtime HTTP | 23 |

None failed but one check of the read path, in one run of three: the check of
the question below, which fails now and then by a race that was found, traced
and forced. Its failing run ends with the right screen.

## The review at the end

Two falsification passes, one on the store, one on the runtime. Neither found
a right that stands when it should not. What they found was fixed before the
last two commits; `PROGRESS.md` lists each finding and its fix. The worst were
in the runtime: a blocked person could write through a program she made; an
agent given a right with no key list could give rights and change her layer's
settings; a blocked person could sign in again and again; the invitation code
reached Google in the Referer.

## Where the build departs from its page

- **An ended right also stops seeing.** The page's change a named the gate.
  The read path read "she holds a permission row", and an end is a row, so an
  agent whose right was ended would have gone on reading a members layer. One
  rule of a chain (`gate/chain.clj`) serves both.
- **A malformed act from a signed-in actor goes to the log** with her name on
  it, and the gate refuses it (12 October, entry 7). Only a thing that is no
  map is refused at the runtime.
- **Six system layers** where the page named two: identities, ways in, names,
  invitations, rules, and the system's own record.

## For Sid

**A question.** Erin watches a list drawn from three notebooks; Alice lets her
into the nearest one. Usually her screen gets one message, the whole new list.
Sometimes it gets the whole new list and then a small repeat of something the
list already shows; the screen ends the same. (3 of 20 runs on 14 October;
0 of 39 traced runs; 3 of 3 when one message is held back on purpose; the
evidence is in `runs/`.) Is the repeat fine, and the check changes (A)? Or must
it be exactly one message, and the read path changes (B)? B cannot be held for
a right that ends without the read path keeping what it last told her, a value
kept between changes, which his ruling of 11 October ("i also don't consider it
a cache") and the caching rule rule out. The build session's view: A. Until he
answers, nothing changes.

**Smaller questions.**
- Whether a replace under a one-write key is limited, say to the row's own
  writer: today any writer whose right allows the key can replace a claim by
  naming it. The runtime's names are safe: only the system writes them.
- Ending a right does not block the person: another branch of a chain can give
  her a right again at once. A person's own block in her layer has no form yet.
- The runtime reads a person's own layer as her (at sign-in, and for the
  version of an agent's description in a run). Once reads are kept, those
  reads would be recorded as hers.
- A second making act of a layer already made is refused with a word that
  reads wrongly in the log; "already made" would say what happened.

**The defaults to strike, each a session's.** The words (`:signed-in`,
`:softland.store/one-write-keys`, the patterns `:*` and `:ns/*`, every refusal
word, the system's layer ids, the runtime's keys); an account's id is a
name-based UUID of its kind and number, the number kept beside it as given;
Google's email and name kept in her record layer; a random provisional name at
the first sign-in; 600 acts and 64 MiB a minute for each kind of actor, a rule
on one id before its kind's; a sitting lasts 7 days; a freed name waits 30
days; 8 blocked attempts kept whole; a block on a person reaches what she made;
the cookie `softland-sitting`, the header `Softland-Run`, the admin token in
`~/.config/softland/runtime-admin-token`.

## Part 3, when Sid gives the client id

1. Sid makes the Google client (`BUILD.md`, "The Google client, for Sid") and
   gives its id.
2. `clojure -M:runtime-module -m softland.runtime.main --google-client-id <id>`
   starts the cluster, the store, the runtime and the page at
   `http://localhost:8400`, and prints how to make the first invitation:
   `clojure -M:runtime-module -m softland.runtime.command invite --base see-and-act`,
   whose link carries the code after `#`.
3. He opens the link and signs in with Google.
4. On the page he makes an agent, gives it a right in his own layer, and makes
   its token. The agent, with its token and a run, writes a note into his layer
   and reads it back, named as the agent, in its run, beneath him.
5. He ends the token, and the agent's next act is refused.

Everything is thrown away when the process ends.
