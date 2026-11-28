# Emails, accounts and invitations: what the attempt to break the steps found, and the steps as settled

Written 28 November 2026 by the orchestrating session in the cloud. It stands
over the brief beside it, `emails-accounts-invitations.md`: where the two
differ, this page holds. The brief's sections "What Sid said yes to", "The
design … What it takes for an email to be erasable", "The runtime, the wire
and the command line", "What 'done' is" and "Your report" stand as amended
here; its section "The module" is replaced by section 3 below, and its
section "The rules" is amended by section 2.

Nothing here is Sid's but his three yeses in the brief. Every rule, shape and
name below is this session's and his to turn.

## 1. What a fresh reader broke in the steps as written

A subagent that had seen none of the designing read the brief and the code
(schema.clj, module.clj at `93120b4`) and ran nothing. Its findings, each with
the order of events, and what was settled. Tasks in brackets: [A] account,
[C] claim, [P] person, [op] an op's own task.

**A. A finished sign-in of an unlinked account, run again, links that account
to whoever the claim names now.** A is P's. A later sign-in x of A finds
`AccountLinked` and answers P; x leaves no op record (only a first link would
have). A is unlinked; C is free; C is invited for Q. A record before x on A's
`*enters` partition fails, `:all-after` runs x again: x finds no record,
writes `AccountLinking(x)`, the probe finds C waiting, the use takes it, and A
is Q's. Every sign-in sends the email hash, so every returning enter is such a
record. *Settled:* a rule, **an enter proves an invitation only when the
invitation's `invited-at` is at or before the enter's `:at`** (and the enter's
`:at` is before its `:ends`). It stores nothing. With it, `EnterOpLinked` (the
brief's change 4) is not built: the rule closes the same order of events for
the linking enter too, and a record for one of the two cases with a rule for
the other is two mechanisms for one job. The price: it trusts the clocks of
the runtime's copies, as the end of an invitation already does; an honest
enter from a copy whose clock is behind the inviter's is refused
`:not-invited` until its clock passes `invited-at`, and an invitation made by
a clock far ahead lets nobody in until then (its inviter takes it back and
invites again).

**B. "Erase her" leaves invitations that name her.** She invites herself
under C2 to add an account and does not finish; `accounts-of` gives A;
`unlink` A. `$$claims[C2]` still names her, nobody but she knows C2, no read
lists it; until `:ends` the C2 account enters as her, and after `:ends` the
record stays (nothing is written when a time passes). The same holds for
every invitation that nobody answered. And `invited-by` is a person's id, so
"three fields that name nobody" was wrong. *Settled:* no list of the
invitations that name a person is built (it brings back the hop to the
person's task that the design removed; it is Sid's call, listed for him). The
spec says what is true: erasing her is `unlink` of each link that
`accounts-of` gives and `take-back` of each invitation whose claim someone
names; an invitation that names her and that nobody names lets its account in
until it ends; the record of an invitation that is over stays until the claim
is invited again, taken back, or swept by an operator's step that is not
built; `invited-by` is the inviter's id and stays where it is written. The
check's sentence is reworded to that (section 5).

**C. A second account with the same address as her first could not be added,
and `invite` said `{:invited P}` when nothing waited.** P entered by C with
her Google account. She invites herself under C: the slot is `ClaimUsed` of
P, nothing changes. Her GitHub account with the same address finds the claim
used by another op: `:not-invited`. Worse: told invited, she unlinks A; C is
free, nothing waits, she cannot enter. `$$claims[C]` was one slot for two
jobs of different count: whom the claim names (one person), and what stands
under it (at most one invitation that waits, and the links made by proving
it, which can be several). *Settled:* the claim's record holds the two apart:
a person, an invitation that waits or none, and the links (section 3). An
invitation of P can wait beside P's links.

**D. The step of a lost choice could undo a use: a claim left used with no
link.** The helping steps were checked for claims that never come back; a
claim now can. Three enters of A in flight, E the target (op e), helpers H1
and H2:
1. [A] status `:chosen C`; E and H1 hop to the use.
2. [C] `take-back`.
3. [C] E and H1 use: gone, both hop to step 3b.
4. [A] E's 3b resets to `:probing`. [C] E probes: nothing, hops to 2b (slow).
5. [C] `invite` again.
6. H2: [C] probe waits, [A] `:chosen C`, [C] use for e, hops to P.
7. [A] H1's late 3b: chosen equals the failed claim, so it resets to
   `:probing` (schema.clj:823-834).
8. [A] E's late 2b with nothing probed sets `:failing` (746-750). [e]
   `EnterOpFailed`. [A] `AccountLinking` cleared. E is refused.
9. H2: [P] entry, [A] step 5 finds no record, writes nothing and answers
   `{:person P}` (874-900).
End: the claim used by e for good, an entry for an account that is not hers,
a person given without a link. A second order, found while settling: with two
uses of one round in flight, the first is lost (claim taken back), the target
re-probes, finds nothing, fails and is cleared; the claim is invited again;
the second use lands and takes the new invitation for an op that was already
refused. *Settled:* two things. A **round** on `AccountLinking`, raised at
each reset to `:probing` and carried in the hop state: steps 2b and 3b change
the status only in their own round. And **step 5 releases** what a use and
step 4 wrote when it cannot link (section 3, enter, change 5). The helping
steps with claims that come back have had no model run; that is said in
module.clj and left.

**E. `invite` and `take-back`, run again, redid what a later record undid.**
(i) [C] invite P, take-back, both run again: between the two reruns the
invitation waits again and an enter gets in by an invitation that was taken
back. (ii) invite P, enter, unlink, then the invite runs again: P is invited
again after she was erased. When one record of a partition keeps failing,
every record after it runs again at each retry, so this is not a narrow
window. *Settled:* `invite` carries an op-id and leaves a record of its op at
the claim's task, in the same event as its write; an invite whose op has a
record changes nothing and gives the answer it gave. And **a write that
removes names what it removes, by the op that made it**: `take-back` names
the invitation (the invite's op), `unlink` names the link (the linking
enter's op). A removal that runs again cannot remove a later thing, and needs
no record of its own.

**F. Two answers of `unlink`.** (i) It fails after its mark; an enter
finishes it and links A to Q; the rerun finds `AccountLinked` of another
person and refuses an unlink that was carried out. (ii) Step 4 writes her
entry two hops before the link: `accounts-of` shows A, `unlink` finds
`AccountLinking`, says `{:unlinked P}`, then the link lands. *Settled:* (i)
goes with `unlink` naming the link: another link on the account means this
link is gone, `{:unlinked P}`. (ii) `unlink` of a link that is being made is
refused `{:refused :linking}`; the caller says it again.

**G. What the brief left without a source.** `invitation-of` gave `:ends`
for a used claim that had none: the read is redefined (section 3). What
mark-failed does on `EnterOpLinked`, and what a helper does at change 4:
both gone with `EnterOpLinked`. The brief's line that the person's task
"notes" an invite: wrong, `invite` has no hop. SPEC.md's "run again gives
the person it gave" is turned: a finished enter that runs again after its
link was removed is refused; the spec says so.

**The simpler `unlink`** that the reader proposed is taken: with the link
named, `U1a`, `U4`, `unlink-op` and `UnlinkOpDone` go.

What the reader tried and could not break: any two first runs (enter with
take-back, enter with an invite that replaces one that is over, enter with
unlink, unlink with invite, two accounts on one claim, two first enters of
one new person); `unlink` failed between any two steps with another write in
between; an enter finishing a foreign unlink; the removed root fields (read
only by the invite steps and step 4); "over" held against `:at` gives the
same answer on a rerun over the same table; after unlink no `:says`, subject
or hash in a table; no step reads an old depot record.

What it could not settle from reading: whether Rama can start a rerun while
the first attempt's hops still land; whether two events from one task to
another keep their order; how long after it finished a record can run again;
whether any deployment holds old forms (assumed: none, nothing is deployed).
The round and the release of step 5 are there so that the first two need no
answer.

## 2. The rules, as amended

The brief's rules stand with these changes.

- **A claim's record holds whom it names, the invitation that waits under it
  (one or none), and the links that were made by proving it (few).** It names
  its person while a link stands under it, or an invitation waits that is not
  over. It is free otherwise.
- **An invitation lets in an enter whose `:at` is at or after its
  `invited-at` and before its `:ends`.** Both ends are held against the `:at`
  of the op that the claim is used for.
- **`invite`** `{:person P :claim C :invited-by B :at t :ends t1 :op-id I}`
  gives `{:invited P}` or `{:refused :claim-in-use}`. The claim names another
  person at `t`: refused. An invitation of P waits and is not over at `t`:
  nothing changes (the `:ends` of a repeat is dropped). Else P's invitation is
  written, beside her links if she has some. The op-id is fresh for each
  call, the setup command's too (a setup run again after its invitation is
  over must invite anew, so its op-id is not drawn from its arguments).
- **`take-back`** `{:person P :claim C :invite I :at t}` gives
  `{:taken-back P}` always: after it, the invitation that op `I` made does
  not wait under C. It touches no link and no other invitation. The
  `:refused :used` of the brief goes: a link is `unlink`'s.
- **`unlink`** `{:account [provider subject] :link L :person P :at t}` gives
  `{:unlinked P}` whenever, after it, link `L` does not stand on the account;
  `{:refused :another-persons}` when link `L` stands there for another
  person; `{:refused :linking}` when the enter `L` is still making it. It has
  no op-id.
- **`invitation-of`** `(invitation-of client C)` gives nil when the claim has
  no record, else `{:person P :invitation {:invite I :invited-by B
  :invited-at t0 :ends t1} | nil :links n}`. It says nothing of "over": the
  caller holds `:ends` against its own time.
- **`accounts-of`** gives `{:accounts [{:account [provider subject] :link L
  :linked-at t} ...]}`.
- **Erasing her** is as B says above.

## 3. The module

Build the steps as pure step functions in schema.clj, one for each step,
named for it, in the manner of the steps that are there; the dataflow only
reads, calls the step, writes and hops. The step names below (`2b`, `3b`,
step 4, step 5) are the reader's names for the steps that are in schema.clj
now: fit each change to the step as it is in the code. If a step cannot be
built as written, or you find an order of events that breaks it, stop that
write and report the order of events; do not improvise another protocol.

**The tables.**

- `$$claims[claim-key]`: one record, `Claim [person waiting links]`.
  `waiting` is nil or `[invite-op invited-by-kind invited-by-id invited-at
  ends]`. `links` is a map from the op of a linking enter to
  `[invited-by-kind invited-by-id invited-at]`, the invitation that the link
  was made by (step 4 needs it for the root on a run again). The record is
  removed when `waiting` is nil and `links` is empty. A claim-key is
  `e:<hash>` or `c:<hash>`. "Over at t" is `ends <= t`.
- `$$invite-ops[op]`: `InviteOpDone [answer at]`, `answer` being `:invited`
  or `:claim-in-use`. It is read and written only at the claim's task, in
  the event of its own invite: it is never looked up by op alone. It names
  nobody; `at` is there so that a later sweep can drop old ones.
- `$$accounts[account-key]`: `AccountLinking` as it is, with a `round`;
  `AccountLinked [person own-lane making-uuid making-session says says-at
  claim-key link-op]`; `AccountUnlinking [person claim-key link-op]`, which
  holds no `:says`.
- `$$enter-ops[op]`: `EnterOpFailed` as it is. Nothing new.
- `$$root-data`: as the brief says. `RootReserving` and `RootInvited` go.
  `RootEntered` loses the five fields and keeps who first invited her and
  when (the inviter's kind and id, and the time), her own lane, `:making`,
  the flag and the two counters. Each account of hers is an entry
  `"<P>\0na:<account-key>"` holding `[provider subject linked-at link-op]`.

**`invite`, one step at the claim's task, no hop.** `InviteEvent` gains
`ends` and `op-id`.

1. `$$invite-ops[op]` has a record: change nothing, give its answer.
2. The claim names another person at `:at` (its links are another's, or
   another's invitation waits and is not over): `{:refused :claim-in-use}`.
3. An invitation of P waits and is not over at `:at`: change nothing,
   `{:invited P}`.
4. Else write the record with person P, `waiting` this invitation, and the
   links as they are (they can only be P's here). `{:invited P}`.

In every case but 1, write `InviteOpDone` in the same event.

**`take-back`, one step at the claim's task, no hop.** A new event on
`*invites`. If `waiting` is the invitation of op `I`, set it to nil (and
remove the record if no link stands). Give `{:taken-back P}`.

**`enter`: the steps that are there, with these changes.**

1. The probe and the use hold the rule of section 2: an invitation waits,
   its person's, with `invited-at <= at < ends`, `at` being the target
   op's. A claim that fails this at the use is a lost choice, as "another
   account took it" is today.
2. The use, at the claim's task. The target op is in `links`: go on, with
   the person and what `links` holds for it (a record that failed after the
   use, running again; or a second use of one round). Else the invitation
   can be proven: set `waiting` to nil, put the target op in `links` with
   who invited and when, go on. Else a lost choice.
3. Step 4, at the person's task: when she has no root record, make
   `RootEntered` from what the enter offers, with who invited from the use;
   when she has one, leave it. Either way write her entry of this account
   with the target op as `link-op`.
4. `AccountLinking` holds a `round`, 0 at first, raised by each reset to
   `:probing`, and each hop carries the round it left in. The steps at the
   account's task that change the status for a probe that found nothing or
   a choice that was lost (`2b`, `3b`) act only when the record is
   `AccountLinking` of the same target op in the same round. A chain whose
   round has passed starts again at step 1 for its own op.
5. Step 5, at the account's task. The record is `AccountLinking` of the
   target op, in any round: write `AccountLinked` with the claim-key and
   the target op as `link-op`. It is `AccountLinked` by the target op:
   answer its person. Anything else (no record, a mark of an unlink, a link
   or a linking of another op): **release**, then start again at step 1 for
   this chain's own op. Release is `unlink`'s U2 and U3 for the target op:
   at the claim's task take the target op out of `links`, at the person's
   task remove her entry of this account if its `link-op` is the target op.
   The invitation that the use took is spent and is not put back (putting
   it back could bring back one that was taken back meanwhile); say so in
   the spec.
6. Step 1 that finds `AccountUnlinking` first finishes that unlink (U2, U3,
   U5, which need only the mark) and then starts again at step 1.

Step 5's "no record: answer the person" of today goes with change 5.

**`unlink`**, a new event on `*enters`, which is hashed by the account-key.

- U1, at the account's task. `AccountLinked` with `link-op` L: of P,
  replace it by `AccountUnlinking [P claim-key L]` (this write is the
  decision, and with it the `:says` is gone) and go to U2; of another
  person, `{:refused :another-persons}`. `AccountUnlinking` with `link-op`
  L: go to U2. `AccountLinking` whose target op is L:
  `{:refused :linking}`. Anything else: go to U3 and then give
  `{:unlinked P}` (U3 there removes an entry that a fault left; it changes
  nothing otherwise).
- U2, at the claim's task (the mark's `claim-key`): take L out of `links`;
  remove the record if nothing is left.
- U3, at the person's task: remove her entry of this account if its
  `link-op` is L.
- U5, at the account's task: if the record is `AccountUnlinking` with
  `link-op` L, remove it. Give `{:unlinked P}`.

**The reads** are as section 2 says: `invitation-of` at the claim's task,
one record; `accounts-of` at the person's task, her entries by range, in
the order of `linked-at`; say the bound.

## 4. The runtime, the wire and the command line

As the brief says, with these changes: the runtime gives each `invite` a
fresh op-id; `:take-back-invite` reads `invitation-of`, and takes back the
invitation it read (its `:invite`) only when the actor is its `:invited-by`;
`:accounts` gives each account with its link, and `:unlink-account` names
the account and the link. `{:refused :linking}` reaches the caller as it is.

## 5. The checks

The brief's "What 'done' is" stands, with the sentence of erasing reworded:
after `unlink` of each link that `accounts-of` gives and `take-back` of the
invitations that are named, no key and no value of `$$claims`, `$$accounts`,
`$$invite-ops`, `$$enter-ops` or her entries holds her hash, her subject or
her `:says`, and her address can be invited as another person; her root
stays, and holds none of the three.

Held by calling the step functions directly in a pure check, in the order
of events that each is for, with a sentence that says it is the steps and
not the module that it holds:

- A: a returning enter that runs again after its account was unlinked and
  its claim invited for another person is refused and links nothing.
- E(i) and E(ii): an invite that runs again after its invitation was taken
  back, or after its link was removed, changes nothing and gives the answer
  it gave.
- A take-back that runs again after the same person was invited anew under
  the claim leaves the new invitation.
- An unlink that runs again after the account was linked anew leaves the
  new link, her new entry and the claim's new link.
- F(i): the unlink that an enter finished, running again, gives
  `{:unlinked P}`. F(ii): an unlink of a link that is being made is refused
  `:linking`.
- D, the nine steps above: with rounds, step 7 changes nothing, the target
  is linked, the claim holds its link.
- D's second order: the use that lands after its target was refused is
  released at step 5: the claim holds no link of that op, she has no entry
  of the account, nobody is given as a person.
- C: she invites herself under the claim of her first account, enters with
  a second account of the same address, unlinks the first: the claim still
  names her and the second account still enters; after the second is
  unlinked the claim is free.

On the cluster, in the harness's concurrent scenarios, as the brief lists
them.
