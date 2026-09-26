# Test Validation — tools and grammars (rig phase 6)

Validator: the build session itself (Claude Opus 5.5, effort max), per
`references/phase-6-test-validate.md` and the template
`references/artifact-test-validation.md`. Default verdict FAIL. No test was
run for this step. Read for it: IMPLICIT_SPEC.md OP16 to OP18, E9 and E10
(lines 868-916, 1789-1860); the plan's tests (7.1, 7.2); the test files
`test/rig/store/shape_test.clj`, `grammar_test.clj`, `recipe_test.clj`,
`tools_test.clj`, `toy_grammars.clj`, and my edits to the earlier suites
(`stream_gate_test`, `forget_test`, `wave1_test`, `read_exit_test`,
`reads_test`, `read_model_test`).

## Minimize IPC launches

`rig.store.tools-test` is one `deftest`, `tools-and-grammars`, with one
`create-ipc` and one `launch-module!`; its cases are `testing` blocks on
disjoint layers (`:g1` to `:g7`, `:r5`, `:r6`) or ordered on purpose in
`:alice` (R1 to R4 build on each other). The three pure namespaces launch
nothing. My edits to the earlier suites add no launch. PASS.

## Implicit spec coverage

OP16 (write a key's grammar) and E9 (states G0 to G3):
- A key's grammar is facts about it, and adding one takes facts only:
  `tools-test` "the toy grammars, written as facts" (the row projected),
  every G case writes its grammar through the door.
- Subjects from the grammar (ruling 8): G2 (the record's `:subjects`, the
  wrap `[:alice :gus]` when marked), and after Gus's forget the value no
  longer opens; `grammar-test` `rows-and-what-they-give` (`named`).
- Opaque: no matching, no shape check, no index, shown as opaque: G3 (a
  string and a map admitted; `[:kv]` refused `:opaque`; no `:ix-kv` address
  under `blob`; `[:k :blob]` rows marked); admission refuses an opaque
  grammar with subjects or `:by-value` (G3, `grammar-test` `admission`).
- G0 × value write: G1's `:g1-bare` (admitted, no shape), G7 (wrap the owner
  alone). G0 × a grammar naming subjects: every layer's first grammar.
- G1 × value naming a person: G2. G1 × value failing the shape: G1 (three
  people, a string), recorded.
- G0 or G1 × make opaque: refused on a used key, `:grammar-change-needs-
  rebuild` (`grammar-test` "made opaque"; the cluster's G4 covers the
  by-value change on a used key and in the same act, V-F4). A key made
  opaque before any use: G3.
- G2 × value write: G3. G2 × value forget: G3 (the opaque value forgotten
  shows erased, the other still marked).
- G1 × change of the grammar: G6 (stricter shape: the act's own value under
  the grammar before, the next act's under the new one); G2 (subjects
  dropped; the earlier value keeps its wrap).
- "Values already written keep their wraps": G2's last assertion.
- A key with no grammar names no one beyond owner and tool: G1, G7.
- Competing grammar facts in one act: G5 (two for `:note`,
  `:malformed-control`); a grammar on a store key: G5, `grammar-test`.
- Who may write one (T-RC4): G5 (the tool's actor, `:control-not-allowed`);
  the owner: `grammar-test` "admitted: the operator's and the owner's".
- Index hints (a8): G4 (by value in `:g4`, not in `:alice-hand` after its
  rewrite, `[:kv :mention]` after a by-value grammar in `:alice-agent`).

OP17 (write a tool) and E10 (states T0 to T2):
- A tool is facts, what it matches, its signature, its read-entry
  preference: `recipe-test` `parse-tool`; R1 writes the test tool as a fact
  through the door, and nothing in `src/` names it.
- The short entry by default, exact rows when the signature asks: R3.
- A tool adds no envelope part: its outputs are ordinary offers (R4's
  records carry only the envelope's parts).
- T2, a tool changed: R6 (the new version runs over the existing match;
  the old one is not run; its output stays).
- A tool that is not deterministic: not in tonight's vocabulary (every
  step and formula is deterministic; the revision reader reads a commit).

OP18 (run tools):
- Finds tools by matching: R1 (the `[:k :tool]` read's report line).
- Outputs are offers through the gate, because of their trigger, named
  before the gate: R4 (`:because-of`, the permission, based-on with stamps,
  stamped after both); `recipe-test` `run-name`.
- A re-run must not double its outputs: R2 (a second pass recorded; one new
  run after a new match), R2's two runners at once (one output, no
  `:name-taken`, V-F5).
- A revoked tool: R4 (its read refused `:permission-revoked`, nothing run; a
  hand-built output refused and recorded).
- Loops (6.4, V-F7): R5 on the cluster (self-matching, cycle in stamp
  order, match-all, a pattern on `:read/pattern`, two passes with no output
  or entry by a refused tool); `recipe-test` `the-loop-check` for the graph.
- The revision reader as a capability and V-F13: `recipe-test`
  `the-revision-reader-as-a-capability` (units bound and read by a later
  step, a span, an error as data, an unknown repository id reading nothing).

The earlier suites: every place that relied on the constants now writes
the toy grammars first (D-P4): `stream_gate_test` (E1's Bob), `forget_test`
and `wave1_test` (subjects, `[:kv :note]`), `read_exit_test` (`[:kv :note]`
and its layers), `reads_test`'s `gate-integration` (the decision's hints
from rows), `read_model_test` (V-F12, the grammar facts left out of the
comparison like the door's own acts).

Out of this build's scope, by the orchestrator's decision (D-P1): the
micro gate's grammar, so G4's "two envelopes of one micro batch" and E9's
"a gate in the other store" are phase 6b's tests, named in the build
notes; nothing in tonight's scope is left without a test. PASS.

## Synchronization

The stream store answers through the ack: `offer-until-answered!` returns
after the gate's event has committed, and every read that follows reads the
layer's home task, where the event wrote (the read exit's queries, `record`,
`opens?`, the key rows by `foreign-select-one`). A person forget's ack
returns after its fan-out's event tree (stream.md), before the G2/G7 opens.
The runner's reads and offers go through the same acked path. No
microbatch is involved. PASS.

## Test namespaces compile

`rig.store.shape-test`, `rig.store.grammar-test`, `rig.store.recipe-test`
loaded and ran (their run is the step before: 12 tests, 217 checks);
`rig.store.tools-test` and the six edited suites were compiled with the
test classpath (a `require` of each, 26 September); `rig.store.toy-grammars`
is a test namespace with public functions only; no test requires a
private var. PASS.

## Verdict

`minor-fail` → fixed in place: before this pass the proof had no case for
a tool changed after it ran (E10 T2), none for two runners at once (OP18's
re-run, V-F5), none for an opaque value forgotten (E9 G2 × forget), and no
check that an earlier value keeps its wrap after its grammar changes (E9);
each was added to `tools-test` as a `testing` block or an assertion (R6,
R2's two runners, G3's forget, G2's last check), in the existing
namespace.

PHASE_VALIDATION:minor-fail
