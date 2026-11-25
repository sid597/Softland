# Starter: finish the prose cut

For a fresh Build session. Everything it needs is in this folder,
`src/proposal/lane-2026-11-25/` (W below). Paths are from the repository root.

## What Sid decided, 25 November 2026

- The store's unit is the **lane**: the acts one gate decides one after another,
  shown to its members at birth. "Layer" is Sid's word for a group of acts at any
  size; the store does not use it for its unit and defines no meaning for it.
- Code is the truth of what happens, and a check is the truth of what must hold.
  Prose beside the code stays only where it is the one home of something: a why,
  a trap, a limit, what a run found. Whose call a thing is becomes data.

## State

Commit 9d08cf53 renamed the unit, layer to lane, by script (`W/rename.py`), with no
change of behaviour. `W/proofs-before.txt` and `W/proofs-after-the-rename.txt`
hold what each proof gave on the head before it and on the renamed tree.

## Do, in order

1. Copy `W/exemplar_first_act.clj` over
   `src-store/softland/store/gate/first_act.clj`. It is the pattern for the cut.
2. Cut the docstrings and comments of the 45 files in `W/fork-lists.txt`: six
   lists, one fresh subagent per list, in parallel. Each follows
   `W/cut-rules.md`, writes its entries to `W/calls/<list-name>.edn`, and runs
   `python3 -I W/same_code.py HEAD <its files>` until every file prints
   "same code". Commit nothing until all six are done: the checker reads HEAD.
3. Assemble `W/calls/*.edn` into one `whose-call.edn` per tree, beside that
   tree's code.
4. Replace `src-store/CLAUDE.md` with `W/store-CLAUDE-draft.md`. It is Sid's to
   turn: show its text in the report.
5. Run the suite once, in the background, behind the machine's lock:
   `flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock bash W/run_proofs.sh "$PWD" <a folder for the output>`
   Then the three server proofs (`clojure -M:server -m softland.server.pure-proof`,
   `seam-proof`, `http-proof`) and the two probe runs whose code was renamed and
   never run: `clojure -M:run` in
   `src/proposal/runtime-2026-11-21/store-piece-3/statements` and in
   `src/proposal/runtime-2026-11-21/gate-histories`.
   If this machine has no Clojure, or cannot fetch Rama, say so plainly in the
   report: the cut is then held by `same_code.py` alone and the suite is owed.
6. Commit by concern. Report from `git diff --stat`. Draft, and do not write,
   the dated entry for the Decisions sections of `src-store/README.md`.

## Out of scope

Pages (`.md`) beyond what the rename changed, the checks' prose, deleting any
file, pieces 5 to 8, groups.

## Cost

Use fresh subagents, never forks. Run long commands in the background and wait
for the notice; do not poll them from the main thread.
