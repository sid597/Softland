> A note of the handoff (`../HANDOFF-stored-forms-2026-11-28.md`, section 4), written after this brief: the branch `program-sign-in-2026-11-28` is not cut yet. Cut it from the head of `auth-stores-2026-11-28` once emails, accounts and invitations are built and reviewed there.

You are building one change to how a program signs in to Softland, in four parts. You are one builder of several that an orchestrating session runs; it holds the whole and has settled the design below. Build it, prove it, commit it, and report. You are a teammate with a stake in this, not ticket service: where the code shows that a settled point is wrong, say so with the grounds and stop that part; do not build around it.

## Where you work

- Worktree: /mnt/data/projects/Softland-auth-stores, branch `program-sign-in-2026-11-28`, which the orchestrator cut from the head of `auth-stores-2026-11-28` for you. That lower branch holds four changes to what Auth stores, built and reviewed just before you: read `git log --oneline de68997d..HEAD` and the diffs first, since you build on them and in the same files. Work only in this worktree. Never edit, stage or commit in /mnt/data/projects/Softland itself: other sessions work in that checkout.
- A second builder works on the form of an act in another worktree and touches src-store, and three files you will also touch (src-runtime/softland/runtime/sign_in.clj, seam.clj, src-command/softland/command/main.clj). Its branch goes on top of yours later. Keep your edits in those three as local as the change allows; do not reformat them.

## Rules of this repository that bind you (you load no memory, so they are here)

- Read first: CLAUDE.md at the root; .claude/output-styles/build.md; src-store/CLAUDE.md (its rules for explanations hold for Auth and the runtime too: code is the truth of what happens, a check with its sentence is the truth of what must hold, a docstring does not retell a body, a note that a change makes false is changed in the same commit). Load the Rama skill (.claude/skills/rama/SKILL.md) and rama-pitfalls (.claude/skills/rama-pitfalls/) before you read or write Rama code.
- Never read src/app/server/env.clj or any .env file.
- Commit on the branch by concern: each commit one change, with a message that says what and why, in the plain style of `git log --oneline -15`. No Co-Authored-By, no session link, no attribution line of any kind. Stage paths by name, never `git add -A`. Do not push, open a pull request or merge.
- Delete no file but scratch that you made. Keep nothing you want kept under /tmp.
- Docs change with the code, in the same commit: the check's sentence, docstrings and notes, the README rows of the test folders, and Auth's spec.
- Proofs on a cluster run one at a time on this machine, behind one lock: `flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock clojure -M:… -m …`. Other builders run proofs at the same time, so a wait for the lock can be long: run a long proof in the background and wait for it, do not poll. Pure proofs need no lock.
- The Bash tool's shell is zsh: an unquoted `$var` is not split into words; write a loop over commands as `bash -c '…'`. A proof's JVM loads source when it starts: make no source edit while a sequence of proofs still has one to start. Never open a file under a `runs/` folder whose name begins `raw-`.
- When you ask another agent or model for something, ask for its view and the grounds.

## The pages

- src/proposal/design-pitfalls-review-2026-11-26/DECISIONS-AND-FIXES.md: the review of 26 November; rows F2, F14, F15 of section 7, O1 of 6.1, E9 of 6.2. Its line numbers are of an older commit. Detail in run-1-who-is-acting.md, run-5-pages-that-say-decided.md (breaks 2, 4 and 6) and merge.md (finding 2) beside it.
- src/proposal/runtime-2026-11-21/spine/auth/SPEC.md: Auth's spec; it changes with the code. test-auth/softland/auth/QUESTIONS.md beside the reference.
- src/proposal/permissions-2026-11-27/PERMISSIONS.md (section 6 holds Sid's replies) and CONTRACT.md (the form of a permission, section 2.3): the permissions rebuild, which is built and whose rules stay.
- src-runtime/softland/runtime/ (http.clj holds the yes page, sign_in.clj the answer, seam.clj the caller and `person?`), src-wire/softland/wire/words.clj (the table of words and who may say each), src-command/, test-runtime/ with its README.md and attacks/.

## What Sid said yes to, in his words of 28 November

He answered these as a session put them to him; his line was "Yes to all that i dont explicitly comment below", with no comment on these. His yes covers each change as said here. The shapes and names inside are a session's, his to turn, so you list every one you choose.

5. No program acts under his own id (F15). The question as put: "when the command line signs in, your browser shows three choices: 'yes, as myself', 'yes, as a new agent', 'no'. With 'as myself', every act the program writes is stored as written by you, and it can do everything you can, such as invite people or end your other sign-ins. You cannot give it less. … A. Remove 'as myself' for programs; only you at a browser are you." He took A. So "as myself" goes for programs: only he at a browser is he, and `person?` tests what its name says. After this a command line can say none of invite, list my agents and end a sign-in; whether those can be lent to a program is not decided and is not part of this.
6. The same agent can be signed in again (F14). The yes page gets "yes, as this agent of mine".
7. What a newly approved program can do. The question as put: "you approve the command line as a new agent. At that moment it holds nothing. … I say a default of see and write in your own lane, shown on the page and changeable before you say yes." So by default it is lent see and write in his own lane; the yes page shows that, and he can change it before he says yes.
8. Auth's spec says whose call a thing is (F2, the spec's part). The entries of SPEC.md that say "Sid's call of 22 November", or that stand under his go without a line of his ("writing Auth first" among them), are marked "a session's default, Sid's to turn". The rules stay as they are built.

Not part of this, and not to be built: `:driven-by` leaving Auth (F16); `:session` leaving the act and the run (F28); whether invite, list my agents and end a sign-in can be lent to a program (O1). If a part needs one of these, leave that part and say so.

## The design, as the orchestrator settled it

### No program is a person (5)

- **In Auth, so that it is a stored guarantee and not a habit of the runtime:** a yes names an agent, new or live, never the person: `answer-ask` whose yes names the root itself as the actor is refused (suggested `:is-a-person`, the word that `end-actor` already says), and `begin-sign-in` of kind `:program` for a person is refused the same. A sign-in of kind `:program` is then never a person's. The spec's "ask, answer-ask" loses "yes, as herself", and the reference and QUESTIONS.md (question 24) follow.
- **In the runtime:** `person?` (seam.clj) is true for a caller whose actor is its root and whose sign-in is a browser's. Find each place that asked "not an agent" by another road and bring it to `person?` or to the wire table's column.
- **The yes page** offers: yes, as a new agent (with a name); yes, as this agent of mine (below); no.
- **The command line:** the commands that only a person may say (`invite`, the list of her agents, the end of a sign-in, and what the builder before you added) stay in its table, and now get the runtime's own refusal: whether a program may be lent them is open (O1), and taking the commands out would answer it. Their notes say that today only a person at a browser may say them. If one of them now reads worse than a plain refusal, say which.
- **The checks:** much of test-runtime signs the command line in "as herself". Each such check now signs it in as an agent and lends it what the check needs, or drives the browser for what only a person may say. Keep each sentence's meaning; where a sentence was about a program acting as the person, it becomes its opposite, a sentence that it cannot.

### The same agent again (6)

- The yes page lists her live agents (from Auth's `under`, by label) and she picks one; `answer-ask` already takes a live agent of her root.
- The sign-in that begins is a new sign-in of that agent; its others stand.

### What it is lent (7)

- **The yes page shows what the program will be lent, and she can change it before she says yes.** In this build the choice is over her own lane alone: see, and write, each a box. For a new agent both are ticked. For an agent of hers that exists both start unticked, and the page says that it keeps what it holds; she may tick them. With nothing ticked the program is signed in and holds nothing.
- **The lend is her own act**, sent by the runtime for her as she posts the page, in the session of her browser: its `:who` is her party, its lane her own lane, under her first permission in it, with one fact, a permission (CONTRACT.md, section 2.3) whose holder is the agent's party, which allows what she ticked, covers her lane's default group, is given by her, and is lent from her first permission. No program wrote it and no program acts as her.
- **Sent twice, it is one act:** the act's UUID and the permission's id are made from the sign-in that the yes begins (or the ask's code), so that a page posted twice, or a copy of the runtime that was cut off, sends the same act.
- **The order: Auth first, then the lend**, as sign-in already does for her own lane. A lend that fails after the yes leaves a program that is signed in and holds nothing; the page says so, and she can lend it later (`give`). Say what the page shows in each case.
- What the program is told when it is approved says nothing new: it reads what it holds like anyone.

### Whose call a thing is (8)

- In SPEC.md, each mark "Sid's call of 22 November" becomes "a session's default, Sid's to turn", and the opening paragraphs say plainly what happened: a session proposed the calls and wrote them into a prompt in his first person, which he pasted; that is no line of his. The entries that stand under his go of 21 November without a line of his, "a break between Auth and the store is met by writing Auth first" among them, are marked the same. Run 5's breaks 2, 4 and 6 say which sentences; mark those and no others. What he did decide (the entries of the runtime's page that carry his lines) stays marked as his.
- QUESTIONS.md and the docstrings and sentences that cite "Sid's calls of 22 November" follow.
- No rule changes: the code and the checks are as built.

## What "done" is

- Auth's contract, module, client, reference, QUESTIONS.md and SPEC.md agree; the runtime, the wire's table, the command line and the checks are brought to it.
- Checks whose sentences failed before: a yes that names the person for a program is refused, at the page and in Auth; a program approved as a new agent writes under the agent's party and can do in her lane what she ticked and nothing else; with a box unticked it cannot do that thing; the same agent signed in a second time holds what it held; the page posted twice lends once; a command line says none of the words that only a person may say.
- What you run, by Sid's word: only what holds the thing you changed. The whole battery is run once, at the end, on all the branches stacked, by a cheaper runner, and is not run again in each builder. So run the pure proofs of the trees you touched (Auth's `reference-proof`, the wire's, the runtime's and the command line's pure proofs), Auth's module check if you changed the module, and the runtime proofs whose sentences you rewrote, each once when you think it whole, not after each edit. Leave what you did not touch to the final run. In your report keep two lists apart: what you ran with its counts, and what you changed but left to the final run, naming the proofs that should catch a fault of yours.

## Your report, as your last message

It is read by the orchestrator, not by Sid, and goes into a handoff page. Plain words, no preamble:

1. The commits, each with its hash and what it holds.
2. Every change beyond the four parts, the small ones too, from `git diff --stat <the commit you started on>..HEAD` and not from memory.
3. What ran, with counts, and which new sentence failed before its change or was held against which broken copy. What did not run.
4. The choices you made, each in a line or two with the other option: every name, every shape, every rule you settled. Mark where you departed from the design above and why.
5. The yes page as it now reads, in words: each choice, each box, and what it shows after.
6. Which sentences of the existing checks changed their meaning and how.
7. What you saw and did not change, and what is left.
