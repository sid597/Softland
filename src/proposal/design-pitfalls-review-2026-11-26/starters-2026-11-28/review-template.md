> A note of the handoff (`../HANDOFF-stored-forms-2026-11-28.md`): a template. Fill each `{{…}}`; the scratch paths under `/tmp` were the writing session's, so name a folder of your own.

Review one build of Softland from its diff and try to break it. You have seen none of the building, and that is the point: the job of a review here is to break the change, not to confirm it. You change nothing that is committed: your findings are your last message.

## The build

{{WHAT}}

- Worktree: {{WORKTREE}}, branch `{{BRANCH}}`. The build is `{{BASE}}..{{HEAD}}`: read `git log --oneline {{BASE}}..{{HEAD}}` and each commit's diff. Judge the diff, not a telling of it.
- The builder is done and will mend what you break after you report. While you review, nobody else writes in this worktree. Never edit a tracked file, never stage or commit. A probe or a test of your own goes in /tmp/claude-1000/-mnt-data-projects-Softland/26482499-eeca-42a6-8599-d2fddfc4d456/scratchpad/{{SCRATCH}}/, or as an untracked file in the worktree that you delete before you finish. Never touch /mnt/data/projects/Softland itself: other sessions work in that checkout.

## What was asked, and what was settled

What Sid said yes to, in the words put to him:

{{ASK}}

What the orchestrating session settled inside that, which is a session's and Sid's to turn. A settled point that you think is wrong is a finding too, with its grounds:

{{DESIGN}}

What was not part of the build, so its absence is no finding: {{NOT}}

## Rules of this repository that bind you (you load no memory, so they are here)

- Read first: CLAUDE.md at the root; .claude/skills/staff-product-architect.md, which gives the perspective of a review here; src-store/CLAUDE.md, whose rules for what is written beside code hold for every tree this build touches. Load the Rama skill (.claude/skills/rama/SKILL.md) and rama-pitfalls (.claude/skills/rama-pitfalls/) before you read or explain Rama code, and .claude/skills/design-pitfalls/ for the faults of a stored form: one id, key, word or record doing two jobs; a thing fixed at write that could be drawn at read; a fix that grew into new building.
- Never read src/app/server/env.clj or any .env file.
- Proofs on a cluster run one at a time on this machine, behind one lock: `flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock clojure -M:… -m …`. Other agents run proofs at the same time, so a wait for the lock can be long: run a long proof in the background and wait for it, do not poll. Pure proofs need no lock. The commands are in deps.edn's comments and in the READMEs of the test folders.
- The Bash tool's shell is zsh: an unquoted `$var` is not split into words; write a loop over commands as `bash -c '…'`. Never open a file under a `runs/` folder whose name begins `raw-`.
- Keep apart what you read in source, what an existing check says, what a run of yours printed, and what you infer. A finding names its case: the input or the order of events, what the code does with it (file and line), and what it should do. Say how sure you are and what would settle it. Do not report style.

## What to try to break

{{ATTACK}}

For every part: a claim of the build's docs, docstrings or check sentences that no check holds; a sentence that passes for a reason other than the one it states; a page or a docstring that the build made false and did not change; a stored form that fixes at write what could be drawn at read, or lets one field do two jobs; a path that the builder did not bring to the new form (grep every tree that compiles against what changed: src-store, src-auth, src-wire, src-runtime, src-command, src-server, src-server-browser, src/app, and the tests of each). What you run, by Sid's word: do not rerun the build's proofs to see them pass. The whole battery is run once, at the end, by a cheaper runner. Run a small probe of your own where a finding rests on what it prints, and a whole proof only when nothing smaller settles the point. A finding that a probe printed is worth more than one that you read; a finding that only a long run could settle is reported as one, with the exact command, and left to that runner.

## Your last message

It goes to the orchestrator, who has the builder mend what breaks. Plain words, no preamble:

1. What breaks: each finding with its case, file and line, what happens and what should, how you know (read, ran, inferred), and how heavy it is: must be mended before the pull request, should be, or a note. Heaviest first. For each, the smallest probe or check that shows it, as code or as a command.
2. What holds: what you tried to break and could not, in a line each, with what you tried.
3. What is still open: what you could not settle, and what you did not look at.

Also write the same text to /tmp/claude-1000/-mnt-data-projects-Softland/26482499-eeca-42a6-8599-d2fddfc4d456/scratchpad/{{SCRATCH}}/FINDINGS.md, so that it survives if your message is cut.
