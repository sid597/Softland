# Starter: the read exit the count needs

Written 25 September 2026, evening, replacing the noon starter, which is in
git at 23a271e3. Sid opens the build session with the prompt below.

---

I'm building Softland's store on Rama. A build of its stream side lives on branch rig-2026-09-25, in the worktree /mnt/data/projects/Softland-rig-2026-09-25, folder src/proposal/rig-2026-09-25/. It began as a throwaway check. It is now a candidate for the store core: build in the form it should be, because it may be kept; its records are not kept, and nothing is kept until edition one is written. Make your own git worktree off rig-2026-09-25 and work only there; other sessions use the worktree above. Load README.md, RIG.md and SPEC.md from the rig folder, then PLAN-stream-store.md "Rig choices proposed", then src/proposal/frame-2026-09-15/PROGRESS.md on main: "Now", ruling 3 and the sharpenings, and "Next". The status notes at the top of the rig's files are current; older text under them that says "throwaway" or lists this morning's options is history.

Where it stands. Phases 0 and 1 are built and tested: 13 tests, 728 checks. The three numbers were measured on slices. Phases 2, 3 and 4 wait; nothing there is on the road to the count.

First, find what rewrote src/rig/store/gate.clj in the rig folder at 08:52 IST on 25 September; the recall tools have the sessions that were running. Tell me, and change nothing because of it.

Then the read exit the count needs, on the stream store as it is, for one-owner layers only:
- Point reads and pattern reads on a layer's home task.
- One exit for reads: query, append the read entry, then answer; nothing shown before its entry is acknowledged.
- The read entry as ruling 3 has it: rows for point reads; one line per pattern read with the pattern, the moment, the role, a keyed fingerprint of what matched, and a complete-or-partial mark; empty pattern reads included. What the fingerprint covers, the ids of what matched or their values, is a rig choice marked pending my ruling; ruling 3 says only "a fingerprint of what matched". Build it as one swappable function. Roles are placeholders: stood on, shown, matched, passed through. Eager in personal and hand layers; agent session layers untouched.
- Values stay unsealed; the open step passes them through. A rig choice.
- Indexes: only what pattern reads need, each purgeable by value id and rebuildable from the log. Index kinds take hints as parameters; there are no grammar facts yet.
- Standing reads: don't build. Write what one would record as a proposal in RIG.md, and stop; round three rules it in its trace.

Then the code that reads a file at a git revision into its passages and functions, with content and position. Give them no store identity: the design session is deciding how a passage at one revision is known to be the same at the next, and will send what its tool needs from the rig; build that next.

Wherever the formal model (src/proposal/formal-model-2026-09-24/ on main) has the same history, compare answers; a difference is a finding, not something to hide. The frontier, the agent-entry default and the stamp's form: don't build, don't pick. Where a ruling is silent on something a kept record would carry, its bytes, a stamp, an id, what an entry holds, stop and ask me; if a placeholder is unavoidable, use the simplest and mark it first-record in RIG.md so edition one reviews it. Pick the simplest thing as a plain rig choice only when it can change later without touching a record.

How it runs: the rama skill's phased process. Every concurrent session gets its own worktree off the branch and merges by commit. Every in-process cluster run waits on one machine-wide lock. Long artifacts are written and committed in parts; last run three plan sessions were cut off mid-write and one left a partial file. Each step ends with its tests run, RIG.md rewritten, and a commit on the branch.

Rules: never read src/app/server/env.clj; nothing under src/app is read or touched. Commit on the branch, never push, no Co-Authored-By or session lines. Ask me before deleting anything. test/rig/store/gate_test.clj is mine; don't open it.

I still owe the thresholds and how big hand-session values are; ask when a number needs them.
