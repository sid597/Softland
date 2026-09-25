# Starter: the next session on the rig and the real store

Written 25 September 2026, around noon, by the session that built the rig
so far. Sid opens the new session with this file.

## Where things are

The rig lives on branch `rig-2026-09-25`, in the worktree
`/mnt/data/projects/Softland-rig-2026-09-25`, folder
`src/proposal/rig-2026-09-25/`. Work in that worktree. The main tree at
`/mnt/data/projects/Softland` stays on `main`.

Read, in this order:

1. `README.md` in the rig folder: what the rig is, why, the questions and
   what each decides, the numbers so far, what is open for Sid.
2. `RIG.md`: the running record. Its "Status", "Numbers so far" and "What
   is next" sections are enough to start; the rest is there when needed.
3. The plans (`PLAN-*.md`) only when a question needs them.

Words: Sid's phases are 0 to 8 of the rig. The rama skill's numbered parts
inside a phase are "steps". A fact's key is a "key"; an encryption key is a
"lock".

## State

- Phases 0 and 1 are done and committed. Phase 1's suite passes: 13 tests,
  728 checks.
- Phases 2 and 3 have plans, not validated and not built. Phases 4 to 8 are
  not started.
- All three numbers are measured and committed (`README.md`, "The
  numbers so far"). Lock growth was not rerun by a second session; the
  measuring session's own runs repeated exactly.
- `test/rig/store/gate_test.clj` is a partial file from an interrupted
  write. It is Sid's to decide on; do not open, change or delete it.

## What to do first

1. Rerun one point of each number yourself and compare, as a check. Only
   one in-process cluster can run on this machine at a time.
2. Ask Sid for what only he can give:
   - the three thresholds, which are the old session's assumptions
   - how big hand-session values usually are, which decides whether a lock
     row per value is an affordable default
   - the stamp's unit: milliseconds let a busy task's stamps run ahead of
     real time, and the choice freezes at the first kept record
   - the direction: the old session's proposal is to take the three numbers
     and then start the real store, with the count as its first milestone
     and the model's fixed histories as its acceptance tests
3. The next question is where the real store lives: a new module beside
   the server, or the server's existing Rama modules evolved toward the
   rulings. Another session has mapped the rulings onto `src/app/server`;
   ask Sid for it. Put that map beside the rig: for each part of the
   rulings, mark the server as matching, conflicting or missing, and note
   whether kept records depend on each conflict. Take a position with
   reasons; Sid decides.

## Rules carried over

- Never read `src/app/server/env.clj`.
- Commit on the branch as work settles; never push. No Co-Authored-By
  line and no session link in commit messages.
- Ask Sid before deleting anything.
- Keep exploration in the conversation; write to disk once something
  settles, as the project's CLAUDE.md says.
