# What Auth stores, and a program that signs in: what was built on 28 November in the cloud

Written 28 November 2026 by the Claude Code session that ran in the cloud as
an orchestrator over subagents, for Sid and for the session that comes next.
It carries on items 2 to 4 of section 4 of
`HANDOFF-stored-forms-2026-11-28.md` (on `main`), which it did not edit.
Paths are from the repository root. The record could not be reached from the
cloud: what is Sid's here is what that page and its briefs quote of his
answers of 28 November, and his prompt to this session. Every other choice
is a session's and his to turn; section 5 lists them.

The detail is in the folder beside this page,
`emails-and-program-sign-in-2026-11-28/`:

| File | Holds |
|---|---|
| `emails-accounts-invitations.md`, `program-sign-in.md` | The two briefs, as the handoff's session wrote them |
| `emails-settled.md` | What a fresh reader broke in the brief's steps before anything was built, and the rules and steps as settled |
| `report-auth-builder.md`, `report-runtime-builder.md` | What each builder of emails built, chose, ran and saw |
| `review-of-the-auth-branch.md` | What the review of the whole branch broke, what was settled, what was mended and what ran |
| `review-of-program-sign-in.md` | The same for the second branch |

## 1. In short

| Step of the prompt | State |
|---|---|
| 1. A fresh subagent tries to break the steps for emails, accounts and invitations before anything is built | Done. Seven findings (A to G); each settled; the steps were rewritten (`emails-settled.md`) |
| 2. Emails, accounts and invitations built on `auth-stores-2026-11-28` | Done: Auth (`06494ca`), then the runtime, the wire and the setup (`7bccc44`) |
| 3. A fresh review of the whole branch from its diff, and the mending | Done. Eight findings; seven mended as settled, one mended another way after the builder showed the first mend wrong (`6e5a52c`, `9ef9ae4`, `45fabb1`) |
| 4. `program-sign-in-2026-11-28` cut from that head, built, reviewed, mended | Built and reviewed. Six findings: five mended. **One is not mended and is Sid's to settle** (section 6): an agent signed in again with no box ticked keeps what it holds in the store, and the command line cannot find it |

Both branches are pushed. Nothing went to `main` and no pull request is
open. The builders ran only the proofs that hold their own change; **the one
run of every proof has not been made** (section 8).

## 2. Where the work is

| Branch | Holds |
|---|---|
| `auth-stores-2026-11-28` | `de68997d`, then `575ee20` (F30), `b4bb7da` (F6), `93120b4` (F17), which the handoff's session built; then `06494ca` (emails in Auth), `660fb68` (pages), `7bccc44` (emails in the runtime, the wire, the setup), `6e5a52c`, `9ef9ae4`, `45fabb1` (the review's mends), `0f2024f` (pages). Pushed |
| `program-sign-in-2026-11-28` | Cut from `0f2024f`. `44036ec` (the same agent again, and what it is lent), `1fa4148` (WIP saved at the restart), `769531c` (a check fixed), `1062da9` (no program is a person), `fff2b5a` (whose call a thing is), `a3a8709` (pages), `52a53c4`, `22f0e0e`, `289bd14`, `70d685f`, `9c03ea6` (the review's mends), then this page. Pushed |
| `wip/emails-reference-2026-11-28` | Not used: its half-made edits were of the brief's older shapes. It can be removed |

The cloud container: Clojure's command line was installed with the official
Linux installer, Rama's repository answered with no proxy setup, and the
cluster lock was a local file (`flock /home/user/cluster.lock …`). The
container is slow: Auth's module check on a cluster took 32 minutes for 1000
histories, so the builders ran it with 100. The container restarted once,
during step 4, and stopped a builder mid-work; its uncommitted edits were on
disk and were committed as a WIP commit.

## 3. How it was run, and what it cost

By the prompt: Fable for the thinking, Opus for the building, no subagent
above high, a builder runs only what holds its own change, a reviewer does
not rerun proofs. The orchestrator read the handoff's first sections and the
two briefs, settled each finding, and read no source.

| Subagent | Model, effort | Tokens (the harness's count) |
|---|---|---|
| Breaks the brief's steps | Fable, high | 226k |
| Installs Clojure, runs the baseline | Opus, medium | 116k |
| Builds emails in Auth | Opus, high | 544k |
| Brings the runtime to it | Opus, high | 301k |
| Reviews the Auth branch | Fable, high | 312k |
| Mends the review's findings (two rounds) | Opus, high | 366k |
| Builds a program that signs in (stopped by the restart) | Opus, high | not reported |
| Finishes that build | Opus, high | 375k |
| Reviews the second branch | Fable, high | 242k |
| Mends its findings (two rounds) | Opus, high | about 300k |

Fable in subagents: about 780k, in three (one attempt to break a design, two
reviews). Opus in subagents: about 2M. The orchestrator itself was woken
about forty times. Against the session before, whose subagents on Fable
used about 1.6M: half of that, for more of the work. What it cost of Sid's
limit this session could not see.

## 4. Emails, accounts and invitations, as built

Sid said yes to three things (the brief quotes the question and his
answer): a person can have several accounts; an erased or unlinked email is
free again; an invitation has an end and can be taken back.

**What a claim's record is.** `$$claims[claim-key]` holds whom the claim
names, the invitation that waits under it (one or none), and the links that
were made by proving it. The brief had one slot that was either "waiting" or
"used"; that was one slot for two jobs of different count, and it made a
second account with the same address impossible.

**The writes.**

- `invite` (person, claim, inviter, end, op-id): one step at the claim's
  task. Refused `:claim-in-use` when the claim names another person. Beside
  a person's links, only her own invitation is written. It leaves a record
  of its op, so that an invite that Rama runs again cannot bring back an
  invitation that was taken back or a person who was unlinked.
- `take-back` names the invitation by the op that made it; `unlink` names
  the link by the op of the enter that made it. A removal that runs again
  cannot remove a later thing, and needs no record of its own.
- `enter` proves an invitation only when `invited-at <= at < ends`. The
  helping steps got a round on the account's linking mark, and step 5
  releases what a use and step 4 wrote when it cannot link.
- Two reads: `invitation-of` (a claim) and `accounts-of` (a person's
  accounts, each with its link).

**The runtime.** `:invite` hashes the address, puts an end of seven days on
it (`--invitation-ms`), and makes the person's id from the inviter's root,
the word's UUID and the claim; with `:herself` a person at a browser adds an
account to herself. New words, each for a person in a session:
`:take-back-invite`, `:accounts`, `:unlink-account`. The same `:invite` word
sent again after its invitation is over invites the same person anew.

### What it takes for an email to be erasable from what Auth keeps

- **Built.** To match, Auth is given a hash of the address (SHA-256 of
  `softland/email/` and the lower-cased address), never the address. An
  invitation holds no address anywhere in Auth, its log included: a person
  who is invited and never comes leaves a hash and no address.
- **Built.** What Auth keeps to be read is `:says`, what her provider said,
  which can hold her address. It is kept with the link and in each record of
  `*enters`. `unlink` removes it from the tables at once, with the account's
  entry, and frees the claim; `take-back` removes an invitation at once.
- **Checked on a cluster.** After `unlink` of each link that `accounts-of`
  gives and `take-back` of the invitations that are named, no key and no
  value of the claims, the accounts, the invite answers, the enter records or
  her entries holds her hash, her subject or her `:says`, and her address can
  be invited as another person. Her root stays and holds none of the three.
- **Not erased by an ordinary write: the log.** The records of `*enters`
  keep `:says`, the subject and the hash of every sign-in. Taking them out
  is a depot migration, an operator's step that is not built. What was kept
  true for it: no step looks up an old record.
- **Not built.** A list of the invitations that name a person: an invitation
  that nobody names lets its account in until it ends, and its record stays
  after that until the claim is invited again, taken back or swept. A sweep
  of invitations that are over and of the invites' answers.
- **Open, for Sid.** The hash is plain: whoever holds the log can test a
  guessed address against it (a hash keyed by a secret of the runtime would
  close that, and losing the secret would free every bound email). Whether
  `:says` should carry the address at all. Whether the account's
  `[provider subject]` should be given as a hash too.

### What ran

The last run of each, on `45fabb1` unless said: Auth's `reference-proof 42`
233 passed, 0 failed; the pure module check (`42 1000`) 49, 0; the module
check on a cluster with 100 histories 3, 0; the harness modes `reruns` 14, 0
(the steps called by hand, in the orders of events that each rule is for)
and `invitations 10` 11, 0 (on a cluster; it reads the tables and both
depots); `softland.runtime.proof` 60, 0. On `9ef9ae4`: the wire's pure proof
40, 0; the runtime's 28, 0; the command line's 36, 0; all 24 namespaces
under test-runtime load.

No model check has run the new steps; `spine/auth/agreement/` still holds
the checked writing of the old ones, and module.clj says so. The orders of
running again and of late hops are held only by the steps called by hand
(`reruns`), not by the module on a cluster.

## 5. The choices, for Sid to turn

Heaviest first. Each is this session's unless marked.

1. **`invite` keeps a memory of its op**, against the rule recorded as his
   call of 22 November, "a repeat's result comes from what stands, never
   from a memory of calls" (QUESTIONS.md 6 and 35). The grounds: once an
   invitation can be taken back and a link removed, nothing stands under the
   claim that could tell an invite that Rama runs again from a new one, and
   when one record of a partition keeps failing every record after it runs
   again at each retry. The other roads: a mark that stays under the claim
   after a take-back or an unlink (her hash stays in a key, against
   erasing), or to accept that such an invite brings its invitation back.
   The record names nobody, is one for each invite, and nothing sweeps it.
2. **An enter that runs again after its account was unlinked is judged as a
   new enter at its old time** (said in SPEC.md, not closed): if an older
   invitation of her address still waits, the account is linked to the
   person it names without a sign-in, and `:says` is back in a table.
   Closing it takes a record for every sign-in, a mark under the account's
   key after an unlink, or Auth's writes on a topology whose updates are
   exactly-once (a microbatch topology, with results read by op). The last
   would remove this whole class (choice 1, the rule of time, the naming of
   what a removal removes) and is a rebuild of Auth's writes; it is named
   here as the better way and was not weighed further.
3. **The rule of time, `invited-at <= at`.** It trusts the clocks of the
   runtime's copies, as the end of an invitation does. An enter from a copy
   whose clock is behind the inviter's is refused until it passes; an
   invitation made by a clock far ahead lets nobody in until then.
4. **A claim's record holds the invitation and the links apart**; a person
   can have a waiting invitation beside her links; only she writes one
   there. Who may say what is otherwise the runtime's.
5. **`take-back` and `unlink` name what they remove** and so changed form:
   `take-back {:person :claim :invite}` always gives `{:taken-back P}` (the
   brief's `:refused :used` went); `unlink {:account :link :person}` gives
   `{:unlinked P}`, `:another-persons` or `:linking`. `UnlinkOpDone` and
   `EnterOpLinked` of the brief were not built.
6. **Her entry of an account is keyed by its link**, so `accounts-of` can
   show one account twice for a moment.
7. **No list of the invitations that name a person** (the brief's choice,
   kept): erasing her is `unlink` of each link and `take-back` of each
   invitation someone names.
8. **The person's id is made from the inviter's root, the word's UUID and
   the claim.** So an inviter cannot invite one new person under two
   addresses (she adds her second account herself), and nobody can form the
   id of a person another invited. The same word sent again renews an
   invitation that is over; with a new UUID it is another person.
9. **The helping steps of `enter`**: a round on the linking mark; step 5
   releases and starts again; the invitation that a released use took is
   spent and not put back; the code claim is rechecked once when both
   probes fail.
10. Smaller: the end is seven days (`:invitation-ms`); the hash's form;
    `accounts-of` is a query topology; `:find-the-invitation` answers the
    same for "no invitation" and "not yours"; `{:refused :linking}` reaches
    the caller in the envelope of every refusal; no command was added to the
    command line; `setup!` returns `:ends` and `:entered?`.

Questions the review put to Sid:

- With `:herself`, an address that she names is no proof that it is hers:
  whoever's account proves it enters as her. A mistyped address gives a
  stranger her person. Proof by her second account in the same browser
  would close it.
- Adding an account needs a browser; `:unlink-account` does not.
- May the first inviter bring back a person who unlinked everything (the
  same word sent again invites her anew)? A setup run again does the same
  for the first person, which is the owner's way back in.

## 6. A program that signs in, as built

Sid said yes to four things (the brief quotes them): no program acts under
his own id; the same agent can be signed in again; a newly approved program
is lent see and write in his own lane by default, shown on the page and
changeable before the yes; Auth's spec says whose call a thing is.

- **No program is a person.** Auth refuses a yes that names the root as the
  actor, and a `begin-sign-in` of kind `:program` for a person, with
  `:is-a-person`. In the runtime `person?` is true for a caller whose actor
  is its root and whose sign-in is a browser's. A program's sign-in of a
  root that a store kept from before is answered as ended. Every word of
  the wire's column "a person", the four of emails among them, is refused
  to a program with `:do-that-as-an-agent`; `:do-that-at-a-browser` left
  the wire. The checks that signed the command line in "as herself" sign it
  in as an agent and lend it what they need.
- **The yes page** offers: yes, as a new agent with a name; yes, as this
  agent of mine (a list of her live agents); no. Under the first, two
  boxes, to see and to write in her own lane, both ticked; under the
  second both unticked, with "it keeps what it holds".
- **The lend** is her own act, sent by the runtime for her in a session of
  her browser as she posts the page: one permission held by the agent's
  party, allowing what she ticked, covering her lane's default group, given
  by her, under and lent from her first permission. Its id and the act's
  UUID are made from her root and the sign-in that the yes begins, so a
  page posted twice is one act. Auth first, then the lend. After the yes
  the page reads the lend as it stands and says what the agent holds.
- **Whose call a thing is.** In SPEC.md, QUESTIONS.md, Auth's READMEs and
  the sentences that cited them, "Sid's call of 22 November" is "a session's
  default, Sid's to turn", and the opening says what happened. No rule
  changed.

**The one thing the review broke that is not mended.** An agent signed in
again with no box ticked "keeps what it holds", and that is true in the
store, but the command line finds its default permission by computing the
lend of its own sign-in, which nobody wrote. It must be given
`--permission` with an id that the new program cannot compute, or she ticks
the boxes again, which adds a permission each time (nothing ends the
earlier ones; the store refuses a holder's reads above 32). Two mends were
settled and each met a wall that is right to stand: the store takes one
permission at an id for good, so one permission for each agent, changed in
place, cannot be lent again after it was ended; and no read lets an agent
learn what it holds in a lane that it may not see, so a program lent only
"to write" cannot find its lend by reading. What closes it is a rule, and
Sid's: a holder may always read the permissions it holds; or the runtime
tells a program at sign-in what the page has lent its agent; or a yes for
an agent of hers ends the page's earlier lend and lends anew.
`review-of-program-sign-in.md`, finding 1, has the cases.

Other choices of this branch, for Sid to turn: the refusal word
`:is-a-person`; a new agent's id at her yes is made from her root, the
ask's code and its name; the page's 200, 409 and 503 and their words; the
boxes of an agent of hers start unticked and do not show what it holds; a
program can no longer `give` to a person (its lend holds no `:give`); each
lend leaves a session of her browser open for a day; `drive` (the checks'
driver) waits and retries while the runtime paces sign-ins, since the
checks now ask more than 30 times in 10 seconds from one address.

What ran, the last run of each: Auth's `reference-proof 42` 238 passed, 0
failed; the pure module check 51, 0; the module check on a cluster with 100
histories 3, 0; the wire's pure proof 41, 0; the runtime's 30, 0; the
command line's 37, 0; `softland.runtime.proof` 72, 0; `command-proof` 16,
0; `review-faults-proof` 6, 0; `attacks.yes-page` 1, 0; and, before the
mends, `clock-ahead-proof` 4, 0, `command-review-proof` 11, 0,
`line-review-proof` 14, 0.

## 7. Seen and not changed

- `agreement/`, PLAN.md and BUILD.md of `spine/auth/` describe the old
  invite.
- A release can leave a root that a late step 4 made for a person who had
  none; the review found it harmless (her next enter gets ids nobody used).
- A refused enter is not shown linearizable now that claims can come back.
- Setup run without `--person` after an invitation is over makes a second
  person; the first id keeps a base permission that nobody will use.
- `:invite` itself is not restricted to callers in a session.
- Whether Rama keeps the order of two hops between one pair of tasks, and
  whether a rerun can overlap the first attempt's hops, was not settled; the
  round, the release and the entry's key are there so that neither needs an
  answer.

## 8. What is left

1. **Sid's, on section 6:** how a program learns what it holds. Until then
   yes 6 (the same agent again) works in the store and not by default at
   the command line.
2. **The one run of every proof**, by a runner on Opus that only runs and
   reports counts, on `program-sign-in-2026-11-28` (it holds the lower
   branch). No builder ran: `softland.runtime.cluster-proof`,
   `two-lines-proof`, `open-session-proof`, `worker-kill-proof`, the
   attacks `closed-session`, `slow-readers`, `unwatched-standing`,
   `watch-caps`, `lost-replacement`, `sign-in-pace`, Auth's
   `full-life-measure`, the harness mode `slice-b`, and the module check on
   a cluster with its full 1000 histories (32 minutes in the cloud
   container). The commands are in deps.edn's comments and the READMEs of
   test-auth and test-runtime; a cluster proof goes behind one lock.
3. **In the docs:** SPEC.md or the runtime's README does not say yet that
   each yes with a box ticked adds a permission that nothing ends.
   `agreement/`, PLAN.md and BUILD.md of `spine/auth/` describe the old
   invite.
4. **Not started here**, from the handoff's list: the act's form to its end
   and its review (items 1, 5 and 6), the three branches stacked, the
   handoff page brought up to date, and the pull requests, which are Sid's.
   The act's form goes on top of `program-sign-in-2026-11-28`, and changes
   `sign_in.clj`, `seam.clj` and `main.clj`, which both branches here
   changed too.
5. **A model run of the helping steps of `enter` with claims that come
   back.** None has run; the steps are held by the histories on a cluster
   and, for running again and late hops, by the steps called by hand.
