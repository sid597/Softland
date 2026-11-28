# The review of `program-sign-in-2026-11-28`: what it broke, and what is mended

Written 28 November 2026 by the orchestrating session. A fresh subagent
(Fable, high) that had seen none of the building read the branch from its
diff, `0f2024f..fff2b5a`, with the job of breaking it. It ran one probe of
its own (one in-process cluster that drives the yes page) and no proof of the
build. "Ran" below is what that probe printed. Its findings, shortened, each
with what the orchestrating session settled. Sections 1 and 2 are the brief
of the builder that mends.

Nothing settled here is Sid's; each is his to turn.

## 1. What breaks, and the mend that is settled

**1. The lend's id was made from the sign-in, and the lend is held by the
agent: one id, two jobs.**
- Ran: an agent lent both boxes, signed in again with no box. The command
  line's default permission (`main.clj`, `lent-at-yes` of *this* sign-in)
  names a permission that nobody wrote: its write is refused; under the
  first lend it is accepted. The new program cannot compute the first
  lend's id. So "the same agent again holds what it held" was true in the
  store and of no use at the command line, the main client of that yes; the
  fix `769531c` made the check true only by handing it the first sign-in.
- Read, inferred: to get a default that works she must tick boxes at every
  sign-in; each adds a permission to the same holder and nothing ends the
  old ones. Above 32 in one holder's run the store answers that agent's
  reads `:too-many-permissions` (read_path/lanes.clj).

*Settled.* What the yes page lends is one thing for each agent, not one for
each sign-in:

- **The permission's id is made from her root and the agent.** The act's
  UUID stays made from the sign-in that the yes begins, so a page posted
  twice is one act. `lent-at-yes` gives both; the command line's default
  permission is the one of its own root and agent, the same at every
  sign-in.
- **A yes with a box ticked sets that one permission to what she ticked**
  (CONTRACT.md 2.3 lets a permission be changed in place by its giver). A
  yes with nothing ticked writes nothing: the agent keeps what it holds. So
  for an agent of hers the page says: tick nothing and it keeps what it
  holds; what you tick replaces what this page lent it before. After the
  yes it says what the agent is lent now. For a new agent nothing changes:
  both boxes ticked by default.
- If the store does not take a write to that id after she ended the
  permission (so that a later tick cannot lend again), stop this mend at
  that point and report the case with what the gate answers; do not build
  around it.
- Checks: "the same agent signed in a second time holds what it held" is
  driven through the command line's own default, with nothing handed to
  it; a new sentence, "a later yes with one box ticked sets what the agent
  is lent to that box"; and "however often she ticks, the agent holds one
  permission from the page" (read the permissions of the holder).

**2. The page judged the lend by the send's result, not by what stands, and
said false things.** `http.clj`, the answer after a yes.
- Ran A: post "see only", 200; the same page again with both boxes: 503,
  "could not be lent: it holds nothing of it. Send this page again". It
  holds see; write can never arrive that way (the same act UUID with other
  content is answered `:not-mine`).
- Ran B: a second post from her other browser sign-in: 503 "holds nothing"
  while it holds both (the lend's session is made from root and act and
  belongs to the first browser sign-in).
- Inferred: a lend that a gate refused replays that refusal under the same
  UUID for good, so "send this page again" cannot work; a post again after
  she ended the lend replays `:accepted` and says "it is lent". "Lend it
  later" named nothing she can do.

*Settled.* **The page says what stands.** After the send, whatever it
answered, the page reads the permission at its id and says what the agent
is lent now. When that is what she ticked: 200. When it is not: if the send
could not be made (a `:could-not`, a timeout), 503, with "send this page
again; if it is still not lent, start the program's sign-in again as this
agent and tick again"; if the send was refused or was not this act
(`:refused`, `:not-mine`, a session in use), 409, with what it holds and
"to change it, start the program's sign-in again as this agent". No
sentence says "lend it later".

**3. A sign-in begun before the change still acts as her.** Read: Auth
refuses only new begins and new yeses; a stored sign-in of kind `:program`
whose actor is the root is still taken by `who-is`, `replace-secret` and
`open-session`, and the runtime then makes a caller whose who is her
person, so it writes under her id with her first permission and renews
itself. The pure proof's new sentence asserted exactly that ("answered as
an agent" was nil at `:send`). *Settled:* the runtime makes no caller of a
sign-in of kind `:program` whose actor is its root: it is answered as a
sign-in that is ended, at every word. SPEC.md says what is true: Auth
makes none since this change; one that a store kept from before is refused
by the runtime; ending them in Auth is a step for a store that holds one,
and is not built (nothing is deployed). The pure proof's sentence becomes
its opposite.

**4. `new?` matched the standing actor by its label.** Ran: yes as her
agent p-a with no box, then the form "new agent, p-a" with both boxes: 200
"lent to see and to write", a lend written and no agent made. *Settled:*
compare with the actor that Auth returned for this answer, not with a
name.

**5. Part 8 missed three marks**: QUESTIONS.md:600 ("his call of 22
November"), :690 ("his call on question 12"), test-auth README.md:153-154
("his two calls of 22 November, then a third"). No mark landed on a line
of his and no rule changed. *Settled:* mark the three, and mend the
sentences that the replacement left unreadable ("a session's default,
Sid's to turn, let the reference's reading stand" and its like), meaning
kept.

**6. Small.**
- A yes by hand as an ended or a foreign agent got 409 "Start the
  program's sign-in again" though the ask still waits. *Mend:* say that
  this agent cannot be signed in and that she can choose again.
- GET of the yes page when `:under` fails said "You have no agent yet".
  *Mend:* say that her agents could not be read.
- `give` to a person from the command line is now always refused by a
  gate (an agent's lend holds no `:give`, and no page lends it): it follows
  from Sid's yes 5. *Mend:* the command line's docs say it.
- Each lend leaves a session of her browser open for 24 hours, counted
  against the 30 opened for a sign-in in a minute. *Not mended:* the same
  act sent again needs the same session. Said in section 3.

## 2. What the mending builder runs

The wire's, the runtime's and the command line's pure proofs;
`softland.runtime.proof`; `command-proof`; `review-faults-proof`;
`attacks.yes-page`. Auth's `reference-proof` only if a file under src-auth
or test-auth changed beyond words. Every namespace under test-runtime
loads. Nothing else.

## 3. For Sid, from the review

- What the page lends an agent is one permission, set by the last yes that
  ticked a box. The page's words for an agent of hers changed from "it is
  lent besides" to "what you tick replaces what this page lent it before".
  The other road was a permission for each sign-in, with the command line
  reading what it holds to find one.
- The page shows the boxes of an agent of hers unticked; it does not show
  what the agent holds now.
- A program can no longer `give` to a person: its lend holds no `:give`.
- Each lend leaves a session of her browser open for a day.

## 4. What the review tried to break and could not

- Auth's refusal (`:is-a-person`) is in the plan functions that the module
  runs on the root's task, in the event of its reads, after
  `:no-such-actor`; the reference and the module agree; the ask still
  waits after it.
- No caller of `begin-sign-in` or `answer-ask` but an agent's
  (seam.clj) and her browser's (sign_in.clj); setup signs nobody in.
- A yes by hand: as herself 400; her own id as the agent 409 from Auth;
  another person's agent or an ended one refused by Auth; both forms at
  once, the unused field is dropped; no after yes and yes after no, 409.
- No posted field reaches the lane, the holder, the group or what is
  allowed beyond the two boxes; lane and first permission come from the
  cookie's caller, the holder from Auth's answer. The act's who is her
  party, in a session of her browser; the permission's value is CONTRACT
  2.3's, under and lent from her first permission.
- The same post twice, in turn or on two copies: one act.
- Every word of the table's column "a person" passes through `word-fault`
  alone; `invite-now` is reached only by her, so `invited-by` is her id
  only on her own invite. Finding 1 of the lower branch's review is not
  broken.
- `:do-that-at-a-browser` is referenced nowhere; `:do-that-as-an-agent`
  reads true for every caller that Auth can now make.
- drive's wait hides no refusal that a check is about: `sign_in_pace`
  posts raw and is unchanged; no other check asserts a 429.
- The sentences of the unticked boxes test a real refusal; the sentence of
  the person's words is read off the table; the command proof's three
  refusals are real processes.

## 5. Still open after the review

- Two posts of the page at one moment: read only.
- "Posted twice lends once" checks two 200s and one write, not that one
  act exists; by construction it is one.
- Not looked at: `cluster_proof`, `worker_kill_proof`, `two_lines_proof`,
  `open_session_proof`, the attacks `closed_session`, `slow_readers`,
  `unwatched_standing`, `watch_caps`, `lost_replacement`; `src/app`.
- Whether any kept store holds a sign-in of kind `:program` for a person.
