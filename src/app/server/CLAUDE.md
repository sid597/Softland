# Keep explanations with the server code

Enter through the [server map](README.md), then the containing folder's README
and the relevant namespace/function docstrings. Parent maps explain a scope's
role; child files explain its implementation. Read only the branches needed
for the question, following callers and dependencies when the relationship
crosses a folder boundary.

Each level has one home:

- `server/README.md`: server responsibility, entry points and relationships
  among immediate folders.
- Each folder's `README.md`: its responsibility, boundaries, state ownership
  and relationships among immediate files or subfolders.
- Namespace docstring: the file's computational role, inputs, outputs/effects,
  owned or borrowed state, and approach.
- Function docstring: inputs, results/effects, meaningful preconditions and
  limitations. Keep non-obvious local reasoning with the function.

Maintain these explanations in the same change as the code they describe:

- Update a function's contract or reasoning when either changes. An internal
  algorithm change does not automatically change the folder's responsibility.
- A responsibility, dataflow or ownership change updates the affected scope
  and its immediate parent map. Continue upward only when the enclosing
  responsibility or relationship changes.
- Reflect higher-level decisions in the affected child contracts and code.
  Check both directions: does the implementation fulfill its description,
  and does the description still explain the implementation?
- Keep intended behavior and actual behavior distinguishable when they
  disagree. Do not silently redefine the intended contract around a bug or
  present a proposal as implemented. Documentation does not authorize an
  unrelated code change.
- Add, move or remove explanations with their code. Repair parent links and
  ownership descriptions. Rewrite obsolete wording instead of appending a
  correction history.

Trace actual callers, registration and lifecycle before describing an active
path. Distinguish durable state from process-local coordination and borrowed
handles; requests from accepted outcomes; acquisition from release. State the
scope of retry, acknowledgement or transaction claims. Keep a source description,
an existing test, a fresh test result and a runtime-performance claim distinct.

Load the repository's Rama skill before reading or explaining Rama code, and
consult the Electric/Missionary guidance where those semantics matter. Never
read `env.clj` or `.env` files; reference imported environment symbols without
inspecting their values.

Keep maps limited to their level. Link to child source instead of duplicating
function catalogs, source listings or descendant contracts in a parallel docs
tree. Prefer durable constraints and reasons over dated inventories, snapshot
counts or guessed runtime results. Small functions need small docstrings.
Preserve useful existing detail, relocating it when its proper home changes.
