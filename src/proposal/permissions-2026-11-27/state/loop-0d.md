status:     DONE
commit:     fa9b7256+loop0d
verified:   3 Rama 1.6.0 in-process probes run (gathered push 6ms->50ms at 900 viewers vs plain push 6-9ms; 1-turn query 835 stream + 33,611 microbatch turns with 0 half-acts); read-set stamp validation adopted in CONTRACT.md §2.4
next:       Run Loop 2A Part 2 (Cloud Claude Fable xhigh) IN PARALLEL WITH Loop 3 (Local Fable xhigh or Carbon High /goal)

# Loop 0d Receipt: Read-Set Validated Snapshot Adopted (Invariants 1–19)

## Empirical Findings (Rama 1.6.0 In-Process)
1. **Two plain pushes across a 1-act swap:** `29/60` evaluations saw an intermediate state.
2. **Gathered multi-path push:** `0` intermediate states, **but** every write in scope re-evaluated all open gathered pushes on the gate's task (`~6ms` append latency jumped to `22ms` at 300 open pushes and `45–50ms` at 900 open pushes, vs. `6–9ms` for 900 plain run pushes).
3. **Single-turn query topology across two PStates:** `835` streaming turns = `0` half-acts (vs. `99/556` skew on 2 client selects); `33,611` microbatch turns across 6 homes = `0` half-acts.

## Required Invariants for `Loop 2A Part 2` & `Loop 3`
1. **Gate (`decision.clj` + `walk`):** Every grant and in-place `change` of a `[:default-group lane]` permission writes `[:holder-permission key stamp]` with **that decision's new stamp** (`nil` on `end`), copied behind `(|all)` on `:base`.
2. **Query (`lane-rights`):** Takes `[*routing-id *lane *question *asker *session :> *snapshot]` (`*question = :see | :acts`), runs in one non-yielding turn between `(|hash *routing-id)` and `(|origin)`, bounded on holder-scan and `:lent-from` chain (`<= 32`, returning `:too-many-permissions` word if cut at bound, never a false `:may-not-see`), reads `:anyone` first on `:see` so public viewers share pushes, and returns `{:standing ... :read-set ...}` containing every row and holder-prefix run read (including `:anyone-signed-in` when signed in and all climbed `:lent-from` rows) with its stamp(s).
3. **Pure read-path & `read_path.clj`:**
   - Standing is `(:standing *snapshot)` **if and only if** a held answer's `:read-set` equals the current values/stamps of the read's open Rama pushes entry-for-entry; otherwise `:not-known-yet` (while `:not-known-yet`, tell nothing of the lane and owe a whole answer).
   - Re-check `:read-set` equality on every `settle!` / step before emitting anything (closes `[t_query, t_open_watch]`, out-of-order answers, and swallowed callback errors).
   - Async single-flight query per `(read, lane)` when no answer matches and a rights push arrived after the last ask; never block Rama callback threads; on failed/unmatched limit end the read with an error word (never stale standing or false `:may-not-see`).
   - Answer lives inside its open read entry (never shared across reads, routing-ids, questions, or `read-once`); `read-once` asks `lane-rights` **after** the lane's last fact select returns.
