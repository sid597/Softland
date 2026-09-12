---
name: electric-docs
description: Read and apply Electric v3 and Missionary source for Softland. Use for reactive UI/client composition, e/defn and e/diff semantics, flow and resource lifetimes, or investigating how Electric capabilities could serve Softland's renderer. Distinguishes language, DOM integration, and Softland target responsibilities without preselecting the implementation.
---

# Electric, Missionary, and the Softland target

Use this as a working reference for the question in front of you. Start with the
capability Softland needs, find the relevant library mechanism, and examine the
boundary between them. Existing implementation shows one attempt and its costs;
it does not determine the architecture we want.

Paths below are relative to the repository root unless written as links.

## Where Electric and Softland meet

Electric v3 supplies a reactive language and client/server execution machinery.
Its supplied UI integration targets the DOM: `electric-dom3` translates reactive
occurrences into browser nodes, properties, listeners, and removal. Softland's
presentation target is its own rendering system, with text, images, paths,
bounded 3D scenes, spatial queries, and physical resource ownership.

The meeting point is the UI/client work that turns changing values and live
interaction into a maintained presentation. Reactive functions, dependencies,
keyed occurrences, conditional lifetime, and client/server placement may be
useful there even when the final realization is Softland rendering. How much of
Electric to use, and where to connect it, are implementation decisions to
investigate. A different rendering target does not settle those decisions.

| Capability to examine | Electric reference | What Softland still has to determine |
|---|---|---|
| Compose computations over changing inputs | `e/defn`, `e/fn`, `e/watch`, `e/input` | Which inputs are tracked, what recomputes, and how authored material drives the computation. |
| Maintain individual occurrences across changes | `e/diff`, `e/diff-by`, `e/for` | The identity of a presentation occurrence, its relationship to an authored entity, and what updates or removes its resources. |
| Own work while a branch is demanded | Reactive branches, flow cancellation, `e/on-unmount` | The lifetimes of surfaces, nodes, subscriptions, fonts and GPU resources, including cancellation during acquisition. |
| Place computation across client and server | `e/client`, `e/server`, transfer machinery | What crosses the boundary, what remains local, and how accepted state reaches the client. This choice is separate from the drawing target. |
| Turn input into changing behaviour | DOM event and form implementations as examples | Softland picking, coordinates, gestures, focus, editing, accessibility, and the action-to-accepted-state path. Browser facilities can still participate where useful. |
| Realize changes physically | DOM mount/update/remove machinery as a comparison | Softland draw order, geometry, resource handles, GPU preparation, and frame scheduling. DOM operations do not specify these contracts. |

There are several possible places to connect. We can learn from Electric while
composing Missionary ourselves; run actual Electric for reactive UI computation
and feed values into Softland renderer calls; or develop a Softland-specific
target vocabulary whose occurrences and lifetimes are owned by Electric. These
are possibilities to examine, and can overlap. Neither a DOM-shaped clone nor a
compiler fork follows from wanting a Softland target.

`e/defn` and `e/diff` are forms in Electric's compiled language. Using them means
accounting for its compiler, runtime, and boot model; they are not standalone
Clojure helpers to copy into an otherwise unrelated execution system. Conversely,
using the language does not require expressing every visual as a DOM element.
Inspect the actual dependency surface before claiming either complete DOM
independence or an inseparable DOM requirement.

Keep the product aim in view: what humans and agents can build, understand, and
change from inside Softland. A compiled reactive host and the authored material
it interprets have different roles. Adopting a host does not by itself provide
editable behaviour, collaboration, durable truth, or self-hosting.

## Read the version that will run

The configuration checked on 2026-09-12 has Missionary `b.46` in the base
`deps.edn` dependencies and actual Electric `v3-alpha-20260519.115706-45` in the
`:inland` alias. `shadow-cljs.edn` contains the `:inland` browser build. Verify the
relevant alias when working; these are checkout facts, not permanent choices.

`reference/electric/source/` and `reference/` are searchable reference material. The
copies of `electric3.cljc`, `electric_dom3.cljc`, and `incseq.cljc` were **not byte
identical** to the resolved `-45` JAR in that check. Use them for orientation;
verify version-sensitive claims against the resolved dependency source. Obtain
the relevant classpath, for example with `clj -Spath -A:inland`, and read the
matching JAR entry. Electric source entries start at `hyperfiddle/`; Missionary's
public operators are in `missionary/core.cljc` in its own JAR.

For upstream changes, use the [Electric repository](https://github.com/hyperfiddle/electric)
and [Missionary repository](https://github.com/leonoel/missionary), recording the
revision relevant to the claim. Read current primary sources when maintenance,
version, or licensing affects a decision.

## Find the relevant source

| Question | Entry point and useful symbols |
|---|---|
| How do Electric functions, tables, placement and lifetime work? | `reference/electric/source/electric3.cljc`: `defn`, `fn`, `input`, `watch`, `diff`, `diff-by`, `for`, `for-by`, `as-vec`, `client`, `server`, `on-unmount`, `boot-client`, `boot-server`. |
| What does a sequence diff mean? | `reference/electric/source/incseq.cljc`: namespace docstring, `diff-by`, `patch-vec`, `items`. Follow the corresponding implementation in the resolved artifact when needed. |
| Which parts assume DOM semantics? | `reference/electric/source/electric_dom3.cljc`: `mount-items`, `attach!`, `Text`, `element`, `props`, `On`; also `electric_dom3_events.cljc` and `electric_dom3_props.cljc` in that folder. |
| How are forms, tokens and scrolling composed? | `reference/electric/source/electric_forms5.cljc`, `electric_tokens.cljc`, `electric_scroll0.cljc`; examples in `reference/electric/docs/tutorial.txt`. Read for behaviour and composition before adapting to Softland. |
| What are the Missionary operators and practical traps? | [Missionary reference notes](references/missionary.md), then `reference/missionary/docs/collected-reference.txt` or the resolved `missionary/core.cljc`. |
| How do the compiler and runtime fit together? | `reference/electric/source/codebase-snapshot.txt`, then the relevant `hyperfiddle/electric/impl/` entry in the resolved artifact. Internal implementation is evidence, not automatically a supported extension API. |

For present Softland usage, enter through `src/app/client/AGENTS.md`,
`src/app/client/README.md`, and `src-inland/README.md`. Follow their folder maps
and docstrings into the relevant code. A useful current connection to inspect is
`app/Views` → `paint/Items` → `nodes` → `render` under
`src-inland/softland/inland/`. `nodes` wraps renderer acquisition and updates in
Electric computations; `render` and `reactive` expose resource acquisition and
disposal. Trace input in the other direction when the question concerns UI
behaviour. This is a source example, not proof that every needed capability is
implemented or that this is the best boundary.

## Distinctions that matter in use

- On the checked `-45` source, `e/diff` uses the value itself as its key
  (`identity`); `e/diff-by` takes a key function and exposes items as an Electric
  table. It does not return a
  Softland `{entity-id -> change}` message. The underlying `incseq` representation
  uses sequence positions and permutations. Keep entity identity, occurrence
  identity, sequence position, draw order, and GPU handles distinguishable.
- On that version, `e/for` operates on tables, while `e/for-by` stabilizes
  collections and collects its body results into a vector. They are not
  interchangeable when a caller needs a zero-or-one occurrence or a scalar.
  `nodes/Await` is a current example where this matters.
- A keyed occurrence does not prove that the producer avoided rescanning a
  collection, that only one value crossed the network, or that only one GPU
  resource was touched. Check the part of that chain the claim depends on.
- DOM insertion can move an existing node. A GPU adapter that interprets every
  insertion as fresh allocation needs a different contract. Understand the
  producer and consumer operations before adapting a diff or mount interface.
- Reactive invalidation, resource lifetime, and frame presentation are related
  but distinct. An input change need not imply an immediate draw; a cancelled
  branch must release what it owns. Establish the intended relationship for
  Softland instead of inheriting the DOM adapter's assumptions.

For an implementation choice, identify the capability gained, the target
contract, and the evidence that would resolve the uncertainty. A small slice
can follow one input change through derivation, target update, and teardown;
identity-sensitive work also needs a reorder/removal case. Choose checks that
challenge the proposed behaviour, without turning every reference lookup into
a benchmark or architecture review.

Older investigations remain at `history/docs/electric-skill/VERDICTS.md`,
`history/docs/electric-skill/PROBE-EVIDENCE.md`, `history/docs/render-north/PROBE-10K.md`, and
`history/docs/electric-skill/SLACK-THREAD-efn-values.md`. Consult them for a matching
question, with their versions and measured scope. Raw incseq measurements do
not establish full Electric or Softland performance; old implementation recipes
and Slack reports do not settle current architecture. The Missionary regression
tests exercise Missionary only. Use existing evidence without promoting it into
universal laws.
