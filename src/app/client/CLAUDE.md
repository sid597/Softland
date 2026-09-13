# Keep explanations with the client code

Read the containing folder's README and the relevant namespace/function docstrings when entering a scope. Use parent maps to understand its architectural role; descend into child files for implementation detail.

Each level has one home:

- `client/README.md`: client responsibility and relationships among immediate folders.
- Each folder's `README.md`: its responsibility, boundaries, state ownership and relationships among immediate files or subfolders.
- Namespace docstring: the file's computational role, inputs, outputs, owned/borrowed state and approach.
- Function docstring: input → output/effects and relevant preconditions. Keep non-obvious implementation reasoning and limitations with the function; put shader/representation notes beside their declarations.

Maintain these explanations in the same change as the code they describe:

- A function change updates its contract or reasoning when either changes. An internal algorithm change does not automatically change the file or folder's responsibility.
- A responsibility, dataflow or ownership change updates the affected scope and its immediate parent map. Continue upward only when the enclosing responsibility or relationship changes.
- A decision made at a higher level must be reflected in the affected child contracts and implementation. Check both directions before finishing: does the code fulfill the description, and does the description still explain the code?
- If intended behavior and actual behavior disagree, state that difference explicitly. Do not silently rewrite the intended contract to bless a bug, or present a proposed behavior as implemented. Documentation does not authorize unrelated code changes.
- Add, move or remove explanations with their code. Repair parent links and ownership descriptions. Rewrite obsolete wording rather than append correction history.
- Keep maps limited to their level. Link to child source; do not duplicate function catalogs, source listings or descendant contracts in READMEs or a parallel docs tree.

Prefer durable constraints and reasons over review grades, broad claims of optimality, snapshot counts, dated inventories or guessed runtime results. Small functions need small docstrings. Existing useful detail should be preserved or relocated when rewriting documentation.
