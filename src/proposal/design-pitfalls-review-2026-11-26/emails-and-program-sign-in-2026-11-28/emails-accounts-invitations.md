> A note of the handoff (`../HANDOFF-stored-forms-2026-11-28.md`, sections 2 and 4), written after this brief: a builder on Opus started on this brief and was stopped within minutes. Its edits of `reference.clj` and `reference_proof.clj`, half made and never run, are the one commit of the branch `wip/emails-reference-2026-11-28`, on top of `auth-stores-2026-11-28`. Use them or start anew.

You are building one change to what Softland's Auth module stores: emails, accounts and invitations. You are one builder of several that an orchestrating session runs; it holds the whole and has settled the design below. Build it, prove it, commit it, and report. You are a teammate with a stake in this, not ticket service: where the code shows that a settled point is wrong, say so with the grounds and stop that part; do not build around it.

## Where you work

- Worktree: /mnt/data/projects/Softland-auth-stores, branch `auth-stores-2026-11-28`, cut from `main` at de68997d. A builder before you committed three changes there (the second secret of a program that waits, a time that passes ends nothing, who made an agent and whose ending ends it): read `git log --oneline de68997d..HEAD` and those diffs first, since you build on them and in the same files. Work only in this worktree. Never edit, stage or commit in /mnt/data/projects/Softland itself: other sessions work in that checkout.
- A second builder works on the form of an act in another worktree and touches src-store, and three files you may also touch (src-runtime/softland/runtime/sign_in.clj, seam.clj, src-command/softland/command/main.clj). Keep your edits in those three as small and local as the change allows; do not reformat them.

## Rules of this repository that bind you (you load no memory, so they are here)

- Read first: CLAUDE.md at the root; .claude/output-styles/build.md; src-store/CLAUDE.md (its rules for explanations hold for Auth and the runtime too: code is the truth of what happens, a check with its sentence is the truth of what must hold, a docstring does not retell a body, a note that a change makes false is changed in the same commit). Load the Rama skill (.claude/skills/rama/SKILL.md) and rama-pitfalls (.claude/skills/rama-pitfalls/) before you read or write Rama code; this change is a protocol over several tasks, which is where those pitfalls are.
- Never read src/app/server/env.clj or any .env file.
- Commit on the branch by concern: each commit one change, with a message that says what and why, in the plain style of `git log --oneline -15`. No Co-Authored-By, no session link, no attribution line of any kind. Stage paths by name, never `git add -A`. Do not push, open a pull request or merge.
- Delete no file but scratch that you made. Keep nothing you want kept under /tmp.
- Docs change with the code, in the same commit: the check's sentence, docstrings and notes, the README rows of the test folders, and Auth's spec.
- Proofs on a cluster run one at a time on this machine, behind one lock: `flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock clojure -M:… -m …`. Other builders run proofs at the same time, so a wait for the lock can be long: run a long proof in the background and wait for it, do not poll. Pure proofs need no lock.
- The Bash tool's shell is zsh: an unquoted `$var` is not split into words; write a loop over commands as `bash -c '…'`. A proof's JVM loads source when it starts: make no source edit while a sequence of proofs still has one to start. Never open a file under a `runs/` folder whose name begins `raw-`.
- When you ask another agent or model for something, ask for its view and the grounds.

## The pages

- src/proposal/design-pitfalls-review-2026-11-26/DECISIONS-AND-FIXES.md: the review of 26 November; row F18 of section 7.2, O2 of 6.1, E8 of 6.2. Its line numbers are of an older commit. Detail in run-1-who-is-acting.md beside it.
- src/proposal/runtime-2026-11-21/spine/auth/SPEC.md: Auth's spec; it changes with the code. PLAN.md beside it is the plan the module was built from (its section "What can be erased later" and its table of what holds an email); `agreement/` holds the model-checked writing of the steps of `invite`, `enter` and `answer-ask`.
- src-auth/softland/auth/ (schema.clj, module.clj, client.clj, fit.clj); test-auth/softland/auth/ with its README.md: reference.clj is the contract written a second time, and SPEC.md, reference.clj and QUESTIONS.md change together. src-runtime/, src-wire/, src-command/ and test-runtime/ with its README.md.
- .claude/skills/rama/references/depot-migration.md: what Rama can do to records that a depot already holds.

## What Sid said yes to, in his words of 28 November

The question as a session put it to him: "A person has exactly one email claim. Once proven, an email 'names one person for good' and 'never names another'. The same spec also says her email must be erasable. An invitation never ends and cannot be taken back. Three cases: You want your work Google account and your personal one to be the same you. Not possible today. Asha leaves and wants her email erased. The two rules contradict each other. You invite bob@x.com and he never joins. Two years later someone else owns that address, signs in, and enters as the person you invited. Asked, three yes/no: several accounts per person? An erased email becomes free again? Invitations end and can be taken back?" His answer: "Yes to all that i dont explicitly comment below", with no comment on this one.

So he said yes to three things: a person can have several accounts; an erased or unlinked email is free again; an invitation has an end and can be taken back. The third goes with the second: otherwise whoever later holds a freed address enters as the person who was invited. What it takes for an email to be erasable from what Auth keeps was the orchestrator's to work out, and Sid wants to see it. His decision of 9 October, entry 1 (src-store/README.md), stands under this: an account "is added, replaced or removed by its link".

His yes covers each of the three as said. The shapes and names inside are a session's, his to turn, so you list every one you choose.

Not part of this, and not to be built: `:driven-by` leaving Auth (F16); whether a program may act under his own id and the yes page (F14, F15: the next builder); a block on a person; a person's name. If the change needs one of these, leave that part and say so.

## The design, as the orchestrator settled it

### What it takes for an email to be erasable

Three places hold an address today: the invitation (the key of the depot `*invites` and of `$$claims`, and the root's record), the enter (`:email`), and what her provider said (`:says`, in each event of `*enters` and with the link in `$$accounts`). A depot is a log: an ordinary write removes nothing from it.

1. **Auth only ever matches an email; it never reads one. So it is given a hash, never the address.** A claim is `{:by :email :hash H}` or `{:by :code :hash H}`: two kinds of one form. The runtime lower-cases the address (root locale, as today) and hashes it; suggested: SHA-256, as 64 hex characters, of the text `softland/email/` followed by the address. `enter` takes `:email-hash H-or-nil` in place of `:email`. Auth's own rule of form for an email (lower case) goes: the runtime owns the address's form, and every copy of it makes the same hash. After this, an invitation holds no address anywhere in Auth, in its log too: a person who is invited and never comes leaves none.
2. **What Auth keeps to be read is `:says`, with the account's `[provider subject]`.** It sits in one entry of `$$accounts` and in the events of one depot, `*enters`. From the tables, `unlink` (below) removes it: an ordinary write, with its effect at once.
3. **From the events of `*enters`, it takes a depot migration**: a module update whose function clears the `:says`, the subject and the hashes of the records of accounts that were unlinked, which Rama applies on read at once and rewrites in the background, keeping offsets. That is an operator's step and is not built here. What you keep true so that it stays possible: no step of the topology needs to read an old record of `*enters` or `*invites` again after its event is done, and a record that a migration cleared or replaced by a tombstone breaks nothing. Say in the spec's section "What can be erased later" exactly this, in place of "builds none of it".

Left, with the orchestrator's view, for Sid: the hash is plain, so whoever holds the log can test a guessed address against it; a hash keyed by a secret that the runtime holds would close that, at the price of a secret whose loss frees every bound email. And `[provider subject]` could be given as a hash too, since Auth only matches it; then `:says` would be the one readable thing. Neither is built here.

### The rules

- **A person can have several accounts.** A link is still account to person, one person at most for an account. "A person has one claim" goes, and with it `:person-in-use`: a claim may name a person who waits, or one who has entered (that is how she adds an account: an invitation that names herself, proven by her other account). Who may make such an invitation is the runtime's: only the person herself, at a browser, may name herself; nobody may name another person who has entered. Auth does not check `:invited-by`, as today.
- **A claim names one person at a time.** It is free when nothing stands under it: no invitation that waits and is not over, and no link that was made by proving it. "A claim names one person for good … it never names another" goes.
- **An invitation has an end.** `invite` takes `:ends t1`, like an ask. An invitation is over when the `:at` of the event that it is held against is at or after its `:ends`; one whose `:ends` is at or before its own `:at` is taken and never waits. An `enter` proves a claim only when the claim waits and is not over at the enter's `:at`. An `invite` of a claim whose invitation is over at the invite's `:at` finds the claim free and is taken in its place, for the same person or another. Nothing is written when a time passes (the builder before you made that the rule of this module).
- **An invitation can be taken back.** A new write, suggested `take-back {:person P :claim C :at t}`: the invitation of `P` that waits under `C` is removed, whether or not its time has passed, and the claim is free. It gives `{:taken-back P}` whenever, after it, nothing of `P` waits under `C`, so a repeat gives the same; it refuses (suggested `:used`) when a link of `P` stands on `C`, for which `unlink` is the write. It never touches another person's invitation.
- **A link can be removed.** A new write, suggested `unlink {:account [provider subject] :person P :at t}`: the link goes, with its `:says`; the claim that it was made by proving is free; her list of accounts loses it. It gives `{:unlinked P}` whenever, after it, the account is no account of `P`, so a repeat gives the same; it refuses (suggested `:another-persons`) when the account is another person's. It ends no sign-in and no session: those are the person's, not the account's. A person whose last account is unlinked keeps what she has until it ends, and cannot enter again until an invitation names her. Say that in the spec.
- **`invite` gives** `{:invited P}`, or `{:refused :claim-in-use}` when the claim names another person now. The same person under the same claim again, while it waits and is not over, or after she entered by it, changes nothing and gives `{:invited P}`; the `:ends` of a repeat is dropped, as the contract's rule of a repeat says.
- **`enter`** is as it was, with the end held against its `:at`, and with a person who has already entered: her root is left as it is (her own lane and `:making` were fixed by her first enter), and the account becomes one more of hers.
- **Two reads**, for the runtime: the invitation of a claim, `(invitation-of client C)` giving `{:person P :invited-by B :ends t1 :state :waiting | :used}` or nil, one read at the claim's task, so that the runtime can hold who may take it back; and a person's accounts, `(accounts-of client P)` giving `{:accounts [{:account [provider subject] :linked-at t} ...]}` in the order in which they were linked, one read at the person's task of entries that are few; say the bound. No list of the invitations that name a person is kept: an invitation holds no address, it ends by itself in days, and whoever wants one gone names its claim.
- **"Erase her" is not one write.** It is `unlink` for each account that `accounts-of` gives, and `take-back` for a claim that someone names. No word of the runtime for it is built: who may say it is not decided.

### The module

The steps of `invite` and `enter` were written from a model-checked plan, with a reservation at the person (`RootReserving`) and helping between writes, because two things had to be unique at two tasks: the claim's person and the person's claim. The second is gone, so `invite` needs no agreement between the two: it is decided at the claim's task, and the person's task then only notes it. Make the protocol simpler where the rules got simpler; do not carry a step whose reason is gone.

The steps below are the orchestrator's design. Build them as pure step functions in schema.clj, one function for each step, named for it, in the manner of the steps that are there; the module's dataflow only reads, calls the step, writes and hops. If a step cannot be built as written, or you find an order of events that breaks it, stop that write and report the order of events; do not improvise another protocol.

What forces the shape. With `:all-after`, when a record fails, it and every record after it on its depot partition run again, the ones that had already finished too. And the hops of two records interleave: a later record's first step can run before an earlier record's last. Until now a claim, once used, stayed used for good, so running a finished `enter` again changed nothing. Now a claim and a link can be freed, so a finished record that runs again could undo or redo what a later record did. Two small records at the op's own task close that, as `EnterOpFailed` already does for a refused enter.

**The tables.**

- `$$claims[claim-key]`: `ClaimWaiting [person invited-by-kind invited-by-id invited-at ends]`, or `ClaimUsed [person invited-by-kind invited-by-id invited-at used-by-op]`. A claim-key is `e:<hash>` or `c:<hash>`. "Over at t" is `ends <= t`.
- `$$accounts[account-key]`: `AccountLinking` as it is; `AccountLinked [person own-lane making-uuid making-session says says-at claim-key link-op]`, where `claim-key` is the claim that the link was made by proving and `link-op` the op of the enter that made it; and a new `AccountUnlinking [person claim-key link-op unlink-op]`, which holds no `:says`.
- `$$enter-ops[op]`: `EnterOpFailed` as it is; a new `EnterOpLinked`, written by an enter after it used its claim; a new `UnlinkOpDone`.
- `$$root-data`: the root's record appears at her first enter. `RootReserving` and `RootInvited` go: a person who waits is a claim that names her and nothing else. `RootEntered` loses `claim-key`, `claim-kind`, `claim-val`, `linked-provider`, `linked-subject`; it keeps who first invited her and when (three fields that name nobody), her own lane, `:making`, the flag and the two counters. Each account of hers is an entry of its own under her root, `"<P>\0na:<account-key>"` holding `[provider subject linked-at link-op]`.

**`invite`, one step, at the claim's task. No hop.** `InviteEvent` gains `ends`.

- The claim is free (no record, or `ClaimWaiting` of anybody that is over at this invite's `:at`): write `ClaimWaiting` for `P` with this `:at` and `:ends`. Give `{:invited P}`.
- The claim names `P` (`ClaimWaiting` of `P` not over at this `:at`, or `ClaimUsed` of `P`): change nothing. Give `{:invited P}`.
- Else `{:refused :claim-in-use}`.

**`take-back`, one step, at the claim's task. No hop.** A new event on `*invites`, which is hashed by the claim-key.

- `ClaimWaiting` of `P`, over or not: remove it. Give `{:taken-back P}`.
- `ClaimUsed` of `P`: `{:refused :used}`.
- Else change nothing and give `{:taken-back P}`.

**`enter`: the steps that are there, with four changes.**

1. A claim can be chosen and used only while it waits and is not over at the `:at` of the op that it is used for (the target's, when one enter helps another). The probe and the use both hold that. A claim that is gone, over or another's at the use is a lost choice, as "another account took it" is today.
2. Step 4, at the person's task: when she has no root record, make `RootEntered` from what the enter offers, with who invited from the claim (carried from the use); when she has one, leave it. Either way write her entry of this account, with the op as `link-op`.
3. A new step between step 4 and step 5, at the op's own task: write `EnterOpLinked`. Only then step 5, at the account's task, writes `AccountLinked`, with the `claim-key` and the `link-op`.
4. An op whose record is `EnterOpLinked` never uses a claim again. The step that reads the op's record before the use (`enter-step-2c-confirm-choice`) carries that on; the use then goes on only for a claim that is `ClaimUsed` by this same op (a record that failed between the use and the link, running again), and for anything else ends as `{:refused :not-invited}` and clears its `AccountLinking`, as a failed enter does. Without this, a finished enter that runs again after its link was removed and its claim invited anew would link the removed account to whoever the claim names now.

When step 1 of an enter finds `AccountUnlinking`, it first finishes that unlink (its steps U2 to U5 below, which need only what the mark holds) and then starts again at step 1.

**`unlink`**, a new event on `*enters`, which is hashed by the account-key; the client gives it an `op-id` as it does for an enter.

- U1, at the account's task. `AccountUnlinking` whose `unlink-op` is this op: go on to U2. `AccountLinked` of `P`: go to U1a. `AccountLinked` of another person: `{:refused :another-persons}`. Anything else (no record, an `AccountLinking`, an `AccountUnlinking` of another op): change nothing and go to U5, which gives `{:unlinked P}`.
- U1a, at the op's task. `UnlinkOpDone`: this op finished once and is running again; go to U5 and change nothing else. Else back to the account's task, U1b.
- U1b, at the account's task. If the record is still `AccountLinked` of `P`, replace it by `AccountUnlinking [P claim-key link-op unlink-op]`: this write is the decision, and with it the `:says` is gone. Go on to U2. If the record changed meanwhile, judge it as U1 does.
- U2, at the claim's task (`claim-key` of the mark). If the claim is `ClaimUsed` with `used-by-op` equal to the mark's `link-op`, remove it. Else leave it.
- U3, at the person's task. Remove her entry of this account if its `link-op` is the mark's.
- U4, at the unlink op's task. Write `UnlinkOpDone`.
- U5, at the account's task. If the record is `AccountUnlinking` with this `unlink-op`, remove it. Give `{:unlinked P}`.

Why each guard is there: U2 and U3 compare the op so that a link that was made anew meanwhile, by another enter under another claim, is not touched. `UnlinkOpDone` is written before the mark is removed, so that an unlink that finished and runs again after the account was linked anew finds itself done and removes nothing. U5 runs also for a done op, so that a mark left by a failure between U4 and U5 does not stay.

**The reads.** `invitation-of`: the claim's record, at its task. `accounts-of`: her entries, by range, at her task.

**What you check on the cluster**, in the harness's concurrent scenarios: two invitations of one claim for two persons at the same moment; two accounts that prove one claim; an `enter` and a `take-back` of its claim; an `enter` and an `unlink` of the same account; an `unlink` and a new `invite` of the claim that it frees; an `enter` at or after the invitation's end. A record that runs again is not something the generated histories make ("on this module no hop has failed and no record has been replayed in any run", module.clj): hold the two rules of running again (change 4 of `enter`; `UnlinkOpDone`) by calling the step functions directly in a pure check, in the order of events that each rule is for, and say in the check's sentence that it is the steps and not the module that it holds.

At full life: a claim's record is one entry; a person's accounts are few; one record at the op's task for each first link and each unlink, as there is one today for each refused enter; nothing here is watched.

The model check: `spine/auth/agreement/` holds the checked writing of the old steps. Do not rewrite it. Say plainly in module.clj's docstring and in your report that no model check has run the new steps, and which checks hold them.

Left as it is, to say in the spec: a clock far ahead at an `invite` finds a waiting invitation over and replaces it; the invitation that was replaced is gone, and its inviter invites again.

### The runtime, the wire and the command line

The smallest surface that makes the three cases possible through the runtime, each held by a check that drives it end to end:

- The word `:invite`: the runtime puts the end on it, from a number of its own (suggested: seven days, beside its other numbers); it hashes the address; and a person may name herself, to add an account.
- A word to take an invitation back (suggested `:take-back-invite`, with the claim): the runtime reads who invited and takes it back only for that actor.
- A word to remove one of her own accounts (suggested `:unlink-account`), and a word that gives her accounts (suggested `:accounts`), for the person herself.
- Each is a row of the wire's table with who may say it (a person, in a session). Add no page. Add a command to the command line only if it costs a table entry; the next builder takes "as myself" away from programs, after which a command line can say none of these.
- The setup command's invitation of the first person gets an end like any other; a setup that is run again after it is over invites her anew. Say what a deployment's owner sees when she comes late.
- sign_in.clj hands `enter` the hash, and still hands `:says` as it is.

## What "done" is

- The contract, the module, the client, the reference, QUESTIONS.md, SPEC.md (its words, the contract, "What each write does", "What can be erased later", "Not in this build"), the histories and the checks agree, and the runtime, the wire and the command line are brought to the new contract.
- Checks whose sentences failed before: the three cases of the question, each end to end (two accounts enter as one person; after unlinking each account of hers, Auth's tables hold nothing that names her and her address can be invited as another person; an invitation that is over or taken back lets nobody in); and, by reading the tables and the depots in a check, that no record and no key of Auth holds an address after an invitation and that `:says` is in no table after an unlink.
- What you run, by Sid's word: only what holds the thing you changed. The whole battery is run once, at the end, on all the branches stacked, by a cheaper runner, and is not run again in each builder. So run the pure proofs of the trees you touched (Auth's `reference-proof`, the wire's, the runtime's and the command line's pure proofs), Auth's module check and its harness (test-auth/softland/auth/README.md says how), and the one runtime proof that drives the new words. Leave the other runtime proofs, the command proofs, the seven attacks and `full-life-measure` to the final run. In your report keep two lists apart: what you ran with its counts, and what you changed but left to the final run, naming the proofs that should catch a fault of yours.

## Your report, as your last message

It is read by the orchestrator, not by Sid, and goes into a handoff page. Plain words, no preamble:

1. The commits, each with its hash and what it holds.
2. Every change beyond the ask, the small ones too, from `git diff --stat <the commit you started on>..HEAD` and not from memory.
3. What ran, with counts, and which new sentence failed before its change or was held against which broken copy. What did not run.
4. The choices you made, each in a line or two with the other option: every name, every shape, every rule you settled. Mark where you departed from the design above and why.
5. In a short section that can be shown to Sid as it is: what it takes for an email to be erasable from what Auth keeps, what of it is built, and what is an operator's step.
6. The steps of `invite`, `enter`, `take-back` and `unlink` as built, each in a line or two with the task it runs on, and what holds them (which check, whether a model check).
7. What you saw and did not change, and what is left.
