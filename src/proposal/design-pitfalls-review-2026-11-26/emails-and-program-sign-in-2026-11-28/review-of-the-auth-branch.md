# The review of `auth-stores-2026-11-28`: what it broke, and what is mended

Written 28 November 2026 by the orchestrating session. A fresh subagent
(Fable, high) that had seen none of the building read the branch from its
diff, `de68997d..7bccc44`, with the job of breaking it. It ran one probe of
its own (the step functions called by hand, no cluster) and no proof of the
build. Its findings are below, shortened, each with what the orchestrating
session settled. Sections 1 and 2 are the brief of the builder that mends.

Nothing settled here is Sid's; each is his to turn.

## 1. What breaks, and the mend that is settled

**1. The wire's `:invite` answered from a memory for good, and a person who
waits could not be invited again.** seam.clj's `invite-now` made the op's id
from root, UUID, person and claim. So the same word, after its invitation
was over or taken back, met `InviteOpDone` and gave `{:invited P}` while
nothing waited; a `:claim-in-use` likewise stayed after the claim was free.
A new UUID names another person, so no road of the wire renewed an
invitation for the same person, and what was given to the first id was
orphaned. And "nobody names another person who has entered" rested on
`$$invite-ops` never being swept: one record with two jobs of different
lifetime, a retry's memory and the lock of a rule. checks.clj asserted the
memory's answer.

*Settled.* The lock goes where the fact is, in Auth's `invite` step, which
sees the links under the claim:

- **An invitation beside links is written only when the person herself
  invites.** When links of P stand under the claim and no invitation of
  hers waits that is not over: if the invite is her own, write it (as
  now); if not, change nothing and give `{:invited P}` (she is in by this
  claim; adding an account to a person who has entered is her act alone).
  "Her own" is drawn from what the event already holds if it can be: the
  invite's `invited-by` is P herself. If the runtime's `invited-by` for a
  person's own act is not her person id, say what it is and carry one
  field instead; say which you took.
- **The op's id is fresh for each call that reaches the runtime** (a
  random UUID made where the runtime takes the word; the same event, and
  so the same op, when the client or Rama runs that one write again). It
  is not made from the word's parts. The same holds for the setup command.
- The person's id stays made from root, UUID, inviter and claim: no caller
  can form the id of a person whom another invited, or of one under
  another claim. The docstring says that, and no longer says that the rule
  leans on Auth's memory of answers.
- So the same word sent again: while her invitation waits, nothing changes;
  after it is over or was taken back, she is invited anew under the same
  id; after she entered by it, nothing changes; after she unlinked every
  account, she is invited anew (for Sid: whether the first inviter may
  bring back a person who unlinked everything).
- Checks: the sentence that asserted the memory's answer becomes "an
  invitation that is over is renewed by the same word, for the same
  person, and she enters as that person"; "cannot name another person who
  has entered" is held in Auth (reference, reference-proof with a broken
  copy, module check) and end to end in the runtime's proof; QUESTIONS.md
  35 and client.clj's note are true again.

**2. Setup run again with `--person` after she had entered opened a door
for seven days** that nobody could close (`invited-by` is `:system`).
*Settled:* closed by the rule of 1: setup's invite is not her own, so it
writes nothing beside her links. Setup reads `invitation-of` as it does and
says "she has entered" when nothing waits and links stand. A setup run
again after she unlinked everything invites her anew, which is the owner's
way back in.

**3. A record that runs again after an unlink can link the account to
another person.** A entered as P by a code. Its email's claim holds an
invitation of Q made before that enter. P unlinks A. The partition replays:
an enter of A, run again, finds no record, proves Q's invitation
(`invited-at <= at` holds, it is older), and links A to Q with its `:says`;
nobody signed in. The rule of time covers only invitations made later.
*Settled: said, not closed.* What the replay does is what A's next sign-in
would do (her address is invited as Q); what is wrong is that it happens
without a sign-in and brings `:says` back after an unlink. Closing it
takes one of three things, each Sid's: a record for every sign-in; a mark
that stays under the account's key after an unlink (her subject stays in a
key); or Auth's writes on a topology whose updates are exactly-once, with
results read by op. A failed mark under the linking enter's op, which the
reviewer proposed, closes one of the two cases and is not built, for the
reason `EnterOpLinked` was not. SPEC.md says the rule as it is: **an enter
that runs again after its account was unlinked is judged as a new enter
at its old `:at`: only an invitation made at or before it, that still
waits, lets it in.** SPEC.md's "no step reads an old event again" becomes
what is true: no step looks up an old record; Rama runs a record again
from the log after a failure, and a record that a migration cleared must
still be one the topology can take.

**4. A late step 4 could remove the entry of a newer link.** A helper's
hop from the use to step 4 lands after the account was unlinked and linked
anew to the same person: step 4 overwrites her entry, the release deletes
it, and the account is linked while `accounts-of` gives none. It needs two
hops between one pair of tasks to change order. *Settled first:* step 4
leaves an entry of another link whose `linked-at` is later than the
target's `:at`. The mending builder stopped that mend with the case that
breaks it: with a clock behind, the newer link's `:at` is before the late
step's, so the late step overwrites after all. *Settled then, and built:*
the fault was in the key. Her entry was one slot for each (person,
account), and what it records is a link, of which one account can have a
late one and a new one at once. An entry is keyed by its link,
`"<P>\0na:<account-key>\0<link-op>"`: step 4 writes the target link's
entry and touches no other; the release, U3 and the step that finishes a
mark remove their own link's key. No time is compared. For a moment
`accounts-of` can give one account twice, under two links; SPEC.md says
so.

**5. Checks and pages.**
- No check reads a depot, though "done" asked for the tables and the
  depots. *Mend:* the harness's check of "no address after an invitation"
  reads the records of `*invites` too, and the check of erasing says in
  its sentence what it reads (the tables) and what it does not (the log of
  `*enters`, which keeps `:says` until a migration).
- The harness's sentence of erasing was vacuous for "her address": its
  `:says` held none. *Mend:* the harness's `:says` holds an address as a
  provider's does, and the check looks for it.
- `every-record` said its 64 keys meet all 4 partitions and did not check.
  *Mend:* check it or do not say it.
- "Auth is given a hash, never the address" is not so: the same enter
  hands `:says` with the provider's `email` as it came, kept with the link
  and in every record of `*enters`. *Mend:* the words, in SPEC.md, seam.clj
  and sign_in.clj: to match, Auth is given a hash; an invitation holds no
  address anywhere; what the provider said, `:says`, can hold it, with the
  link until `unlink` and in the log until a migration. (For Sid: whether
  `:says` should carry the address at all.)
- The commit of the pages said it added the brief and did not (a copy
  failed). The two briefs are in the folder now.

**6. The probe's recheck of the code claim.** Its trigger missed an order:
the email's invitation taken back while a code invitation is made, and the
enter refused though one of the two let it in at every moment. *Settled:*
the code is rechecked once whenever both probes failed: one read more, on
a refusal only.

**7. Step 5 linked a linking that was `:failing`.** E is told refused, the
account is linked under E's op, `EnterOpFailed` stands. It needs a
re-invite by a clock behind. *Settled:* at step 5 a linking that is
`:failing` is released like the other cases.

**8. One op given again under another claim or person.** The module
answers from memory when both claims share a task and judges afresh
otherwise; the reference always answers from memory. *Settled:* SPEC.md
says it is not defined; the runtime cannot send it.

## 2. What the mending builder runs

Auth's `reference-proof`; Auth's module check, pure and on a cluster with
100 histories; the harness modes `reruns` and `invitations` and whatever
mode holds the depot read; the wire's, the runtime's and the command
line's pure proofs; `softland.runtime.proof`. Nothing else.

## 3. For Sid, from the review

- With `:herself`, an address that she names is no proof that it is hers:
  whoever's account proves it enters as her, and she reads its subject in
  `:accounts`. A mistyped address gives a stranger her person. Proof by
  her second account in the same browser would close it; not built.
- Adding an account needs a browser; `:unlink-account` does not.
- May the first inviter bring back a person who unlinked everything?

## 4. What the review tried to break and could not

- module.clj: each old enter step reads and writes the table and key that
  it did at `93120b4`; each hop goes to the act's key; the new steps read
  and write on the task they hop to; a hop's state carries only fixed
  fields of the target.
- `InviteOpDone` is read and written only in the invite's own event.
- The two later commits touch no root plan and no ask step; no break of
  F6, F17 or F30 found by reading the steps and `beneath?`.
- The root that a release leaves is harmless: her next enter gets ids that
  nobody used, and `enter!` makes the lane from what Auth returns.
- The ids made from the claim: the join is unambiguous, and no choice of
  UUID or claim names a person of another root or under another claim.
- `:take-back-invite` takes back by the op it read; `:unlink-account` with
  another person's link is refused, and with a wrong link removes nothing.
- No old form is left in src or the tests of auth, wire, runtime, command,
  server or app, outside the reference's broken copies.
- By hand, with hops in order: an unlink's mark against a late step 5;
  late U2, U3 and U5 after a relink; two chains at step 5; a take-back
  between probe and use; an enter finishing a mark while the unlink
  finishes. No stuck state.

## 5. Still open after the review

- Whether Rama keeps the order of two hops between one pair of tasks, and
  whether a rerun overlaps the first attempt's hops.
- No cluster proof was run by the reviewer. Nobody has run
  `full-life-measure` or the harness mode `slice-b` since their inputs
  changed.
- Not read by the reviewer: reference_proof.clj and module_check.clj
  sentence by sentence, standing.clj, clock_ahead_proof.clj, the READMEs
  of src-auth and test-auth.

## 6. What the mending builder built and ran (Opus, high)

- `6e5a52c`: findings 1, 2 and 8. In `schema/invite-step` and the
  reference's `write-invite`, an invite beside her links whose `invited-by`
  is not P writes nothing and gives `{:invited P}`; "her own" is drawn from
  the event as it is (for `:herself` the runtime's `invited-by` is her id).
  `seam/invite-now` makes the op with `UUID/randomUUID`. `setup!` returns
  `:entered?`. QUESTIONS.md 35 is true again; 41 is new. Broken copy
  `:anyone-invites-beside-links`.
- `9ef9ae4`: findings 3, 5, 6 and 7. SPEC.md states the rule of an enter
  that runs again after its unlink (QUESTIONS.md 42 is new). The harness's
  `:says` holds an address; a new check reads the records of `*invites` and
  `*enters`; `partitions-met` checks that the 64 keys meet every partition.
  The code is rechecked once whenever both probes failed. Step 5 releases a
  linking that is `:failing`.
- `45fabb1`: finding 4, her entries keyed by their link.

The last run of each, on `45fabb1`, passed and failed: `reference-proof 42`
233, 0; the pure module check (`42 1000`) 49, 0 (`:says-of-every-enter` not
caught, as before); the module check on a cluster with 100 histories 3, 0;
harness `reruns` 14, 0; harness `invitations 10` 11, 0;
`softland.runtime.proof` 60, 0. On `9ef9ae4`: the wire's pure proof 40, 0;
the runtime's 28, 0; the command line's 36, 0; all 24 namespaces under
test-runtime load.

Shown to fail first: `reruns` before the mends of 6 and 7 (the two new
cases failed); finding 4's order on the old key; `runtime.proof` with the
old op and the old invite step, 58 passed and 2 failed (the renewal and
the setup after she entered).

Changed and left to the final run of every proof: `cluster_proof` (it runs
`checks.clj`), `full-life-measure`, the harness mode `slice-b`.

Seen and not changed: fit.clj:116 and schema.clj:76 say "never the
address" of claims, where it is true; `agreement/`, PLAN.md and BUILD.md
describe the old invite.
