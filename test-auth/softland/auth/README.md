# Auth checks

[Parent: softland](../README.md) · [Auth's spec](../../../src/proposal/runtime-2026-11-21/spine/auth/SPEC.md)

This folder holds the reference of Auth's contract and the proof that holds it to
the spec. The reference is the contract written a second time, in plain Clojure,
from the spec alone. A build of the module, or of the runtime that calls it, can
be held against it, call by call. It reads no clock, opens no cluster and
requires nothing outside Clojure.

| File | What it is | State that it changes |
|---|---|---|
| [reference.clj](reference.clj) | The twelve writes as a pure state machine, the pure reads of a state, and a client over one atom with the eighteen functions of the contract, so that a program can stand on it. | A client's atom, in memory: the state, the watches, which clients are closed. A client opened from another shares its atom, as two copies of the runtime share one Auth. |
| [reference_proof.clj](reference_proof.clj) | The checks of the reference: by cases, by generated histories, by broken copies. | None outside its process. |
| [QUESTIONS.md](QUESTIONS.md) | Each place where the spec does not say what happens, or says two things, with the reading that the reference takes. | None. |

The docstrings of `reference.clj` and the sentences of `reference_proof.clj`
cite `QUESTIONS.md` where a rule or a check stands on one of its readings. A
turn of a question changes the three together.

## reference_proof.clj

```sh
clojure -M:auth -m softland.auth.reference-proof [seed]
```

**What it checks.** One check for each sentence of the spec that says what a
call gives or refuses, and the client over one atom, with races of two copies of
the runtime among them. Then generated histories, from the seed that the run
prints: random sequences of calls with repeats, inputs that do not fit, and
times that pass and now and then go back. A rule of its own is checked after
every step of every history. Last, each broken copy of the rules (`breaks` in
`reference.clj`) is run through the same histories, which must catch it.

**What a pass shows.** Each sentence holds for the cases written. Every rule of
the histories held after every step of every history of that seed. Each broken
copy was caught, and not only because it threw. Run with no seed, it takes the
clock's, so each run makes histories of its own. Given the printed seed, it
makes the same ones again.

**What it changes outside the code that it checks.** Nothing.

**What no check reaches.**

- The module, which is not built. No check runs one call through both the
  module's client and the reference and compares what each gives. That is the
  check that the reference is for: the same generated histories, call by
  call, with every result and every read compared.
- What a read costs, the bound of what one task watches, and that no task is
  one that every person's calls go through. Those belong to the module and its
  plan, not to a state machine.
- `:reach-the-module` and `:softland.auth.client/not-reached`: the reference
  never gives them, so nothing checks a caller's way with them.
- `watch-root` is checked by cases alone. A history that holds watches open and
  checks each call that a watch receives against `live-under` is not written.
- `:too-deep` is reached by a case alone. The histories never make 32 agents in
  a line.
- The client's lock is checked by three races of two threads, a hundred times
  each. No race of many threads is written.
- Where no read of the contract shows a fact (which account is linked to whom,
  which claim names whom, a link's `:says` and the enters that it decided), the
  histories read the reference's state map, whose form `reference.clj` gives.
  A check of the module could read those facts only through results.
- Erasing, which the spine does not build.
