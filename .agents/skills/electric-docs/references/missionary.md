# Missionary reference notes

Read this when the question concerns actual flow composition, scheduling,
backpressure, or resource lifetime. Paths are relative to the repository root.
These notes refer to Missionary `b.46`, checked against its resolved
`missionary/core.cljc` and the relevant existing tests. Check the dependency
version before carrying a behaviour into different code.

## Combining state and reading at event time

For a calculation over the latest values of several watches, a direct
composition is:

```clojure
(m/latest (fn [a b] (derive a b))
          (m/watch !a)
          (m/watch !b))
```

`m/latest` calls its combining function with the latest input values on transfer;
each input must initially be ready. The whole combining function runs when the
combination updates. Put each calculation behind the dependencies it actually
needs. A plain `@atom` read inside the function adds no tracked input: a change
to that atom alone will not trigger recomputation.

That untracked read can be intentional for an event-time question:

```clojure
(m/eduction
  (filter (fn [_event] (= @!focus :editor)))
  events)
```

Here focus is consulted when an event arrives; changing focus alone should not
replay an event. If a focus change must itself produce an update, model that
dependency explicitly instead.

The existing `claim-01` and `claim-02` tests reproduce cancellation failure in
specific shapes with nested preemptive `m/?<` forks over watches. Read their
complete forms when diagnosing `Watch cancelled`; they do not justify a ban on
`m/ap`. Claims `08` and `09` cover coarse combination and untracked dereferences.

## Sampling, sharing, and consistency

- `m/sample` takes sampled flows before its final sampler flow. Sampled inputs
  must be initially ready. An `m/observe` source needs an initialization policy
  before use in a sampled slot; the operator itself does not supply an initial
  value. Claim `11` exercises this distinction.
- Sampling an unchanged `m/latest` result does not rerun its combining function
  on every sampler tick (claim `03`). A frame driver must own its scheduling;
  placing the next frame request inside a derivation does not create a reliable
  frame loop. Choose demand-driven or continuous presentation from the target's
  needs, rather than prescribing an unconditional RAF loop from an old recipe.
- A flow description can have multiple subscriptions. Sharing one underlying
  process is a separate choice: `m/signal` exposes the latest value, while
  `m/stream` distributes items and collects subscriber backpressure. They start
  upstream with the first subscriber and cancel it when the last leaves; account
  for their documented spontaneous-termination behaviour too. The tests named
  `seam-step1-signal-*` cover sharing, last-subscriber cancellation, and restart.
- The old raw-`m/latest` diamond probe observed inconsistent intermediate pairs.
  That observation does not establish a limit for every Missionary graph.
  `m/signal`/`m/stream` use the Propagator machinery; the source's `signal`
  example demonstrates consistent propagation through its shared diamond.
  Verify the actual graph and scheduler before making a wider consistency
  claim, especially across peers or asynchronous resource completion.

For values that must agree, establish their common input or coordinated
propagation. Sampling at a frame boundary alone is not proof of graph-wide
consistency. Keep derivations pure where practical; effects need an explicit
owner and a known subscription lifecycle.

## Event delivery and disposal

`m/observe` registers a subject callback and calls the returned cleanup thunk
on termination. While a previous transfer is pending, its callback blocks on a
platform that supports blocking and throws on one that does not. Browser event
adapters need an explicit overflow policy.

`m/relieve` drains its input and combines excess values; its default keeps only
the latest. That can suit a replaceable pointer position. Dropping intermediate
clicks, keystrokes, edits, or commands may change meaning, so do not wrap every
event source in it by habit. Pick buffering, aggregation, or delivery semantics
from the event's contract. Source anchors: `observe`, `relieve`, `buffer`,
`stream`, and `signal` in the resolved `missionary/core.cljc`.

Cancellation belongs to ownership. Give listeners, subscriptions, and native
resources a disposer; account for acquisition completing after its owner has
cancelled. `m/join` propagates a branch failure and cancels siblings (claim `12`),
so a shared failure does not prove every sibling caused it. Keep intended error
and cancellation handling distinct. A flow is a subscription function, not an
atom to dereference (claim `18`).

## Evidence and lookup

- Operator documentation: `reference/missionary/docs/collected-reference.txt`; verify
  sensitive details in the resolved `missionary/core.cljc`.
- Executable cases: `test/app/missionary_claims_test.clj`. Run a relevant subset
  when a proposed change depends on it; after a Missionary upgrade, run the
  suite. It contains no Electric runtime, DOM, or GPU verification.
- Earlier diagnoses and probe forms: `history/docs/electric-skill/VERDICTS.md`.
  Its proposed amendments and application descriptions are historical context,
  not extra project rules. The tested flow shapes and their evidence remain
  useful without carrying forward every old recommendation.
