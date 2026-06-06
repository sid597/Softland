# Object Container — Settled Spec

> **Essence:** The smallest identity-bearing thing in Softland is an **ObjectContainer**, not a text
> artifact. Text is one *kind* of content it holds. Documents, outlines, canvases, chats, and trails
> are **compositions of containers**, not container types themselves.
>
> Synthesis of the Claude + Codex design conversation (2026-06-06). This is the agreement, not a transcript.

---

## 1. What is settled (Claude + Codex agree)

1. **The atom is the container, not the text.** An empty Roam block is still a block — identity can't
   come from content, so the atom is a durable, addressable shell. Text is content it carries.
2. **`text_kernel` is a proof, not the final shape.** It demonstrated the Rama loop
   (ingest → revision → unit → status → projection) but is text-typed, its units are flat + derived,
   and it can't compose with other containers. The general container shape gets *extracted* from it.
3. **The container is thin.** It carries identity + origin + ownership + content-handle + policy +
   per-kind capabilities. Nothing else.
4. **Composition lives outside the container as first-class edges** (`CompositionEdge`, `RelationEdge`),
   never as `parent`/`children` fields. Reason: a container appears in many contexts and layers, and
   base/active overlays must be able to *diff* structure — you can't diff a field on a singleton object
   without forking the whole object; you can add/retract edges.
5. **One contract, many kinds, many interpreters.** All identity-bearing things satisfy one container
   contract and differ by `:kind` (`:text-block`, `:chat-turn-item`, `:canvas-region`, `:code-span`,
   `:slice`). Interpreters (outline / canvas / chat / agent / 3D) *render* containers. No interpreter is
   the canonical store.
6. **Two operations, kept separate:**
   - **Projection** = render existing data into a form. Invents *presentation* (layout), never relationships.
   - **Situate** = discover/compute relationships you didn't author. Invents *relationships*, never presentation.
7. **Independence = a visibility promotion.** A container is born *dependent* (`:visibility :private`, in
   its local context) and becomes *independent* (`:public-material`, in the global connectable pool) by a
   policy promotion — not merely by having an id.
8. **Decomposition lifecycle: derived until touched, durable after.**
   raw source (immutable) → distiller → `DerivedUnit` (re-runnable, not durable) → first meaningful touch
   (edit / quote / reply / select) **promotes** it to a durable `ObjectContainer` that keeps its
   source-anchor and gains its own revision history. Re-running a distiller may rebuild derived units but
   **must not overwrite graduated containers** — it may instead surface *drift* (the source changed after
   you forked).
9. **Ingest vs active decomposition — both, with a rule.**
   - *Ingest:* **always** create a `SourceArtifact` (raw + ref + hash). Seed a document-level container
     when the source should be addressable in Softland (most user imports). Seed finer child containers
     only where the source carries native ids (Roam blocks, canvas nodes, chat messages). Everything else
     is a `DerivedUnit` until touched (§1.12).
   - *Active:* slices, claims, candidate relations, alternate groupings — **candidates until accepted.**
   - Inferred structure is **never** accepted as truth at ingest time.
10. **Projection is read-only.** A gesture in any view (drag, indent, edit) emits an `ActionRequest`
    against the canonical model; the view re-derives. Structural edit → canonical event → all views change;
    view-native edit (canvas xy, collapse) → `ViewState`, one view only.
11. **`ViewState` is keyed by placement** `(view-instance, placement)`, not by `(view-kind, object)` —
    the same object can sit on two boards (transclusion), so position belongs to the placement.
12. **Source ≠ container — and import produces *levels*, not always a tree of containers.** A
    `SourceArtifact` is the immutable captured source *version* — format-general (md, Roam export, chat
    log, canvas JSON, PDF, code, image) — carrying a logical ref (`:source/ref`, e.g. the path) **and** a
    version hash (`:source/hash`). An `ObjectContainer` is id-addressed; content varies through
    `Revision`s. A SourceArtifact **never becomes** a container; it *seeds* them via `SourceAnchor`s.
    Every import:
    - **always** creates a `SourceArtifact` (raw, kept as-is);
    - **usually** seeds one document-level `ObjectContainer` *when the source should be
      addressable/placeable in Softland* (most user imports); purely internal/transient sources (raw
      stream chunks, tool output) may stay source-only until referenced;
    - seeds **child containers only where the source carries native ids** (Roam block uids, canvas node
      ids, chat message ids);
    - otherwise (markdown headings/paragraphs, plain text — no native ids) produces **`DerivedUnit`s**
      that graduate to containers on first touch (§1.8) or by an explicit import policy.

    So "the base layer for the importing user" has **two floors**: the immutable `SourceArtifact` (raw
    base) and the accepted Softland containers/edges derived from it (model base). The old `text_kernel`
    `textArtifact` collapsed `SourceArtifact` + `ObjectContainer` + `Revision` into one text-shaped
    object; the new model splits the three roles and retires the name `textArtifact`.

## 2. The contract (the facts)

```
ObjectContainer   identity-bearing atom; thin. ≈ generalize $$objects
Revision          versioned content of a container. ≈ $$text-revisions
SourceArtifact    immutable captured source *version*; format-general (md/Roam/chat/canvas/PDF/code/image).
                  carries :source/ref (logical, e.g. path) + :source/hash (version). text_kernel :text/content = one case
SourceAnchor      link from a unit/container back into a source span/block/item/hash
DerivedUnit       distiller-vN interpretation of a source; re-runnable; NOT durable. ≈ $$units-by-artifact + :derived-by
CompositionEdge   parent/child/order/contains/next-sibling. ≈ $$artifact-graph / $$artifact-graph-in (already first-class)
RelationEdge      supports/contradicts/references/derives-from/depends-on
ContextMembership where a container is situated: local world, page, board, thread, region (many at once)
LayerOverlay      base / active overlay semantics; proposed changes; promotions
ViewState         canvas xy, collapse, zoom, selection (keyed by placement)
Interpreter       outline / canvas / chat / agent / code / 3D rendering + gesture→ActionRequest rules
```

## 3. Storage rule (where each fact lives)

```
intrinsic + singleton + layer-invariant   -> on the container
layer-sensitive / multi-context / relational -> first-class edge or membership
view-specific                              -> ViewState
inferred                                   -> candidate until accepted
```

## 4. Grounded in existing code (this is NOT greenfield)

The space kernel already **separates** object rows (`$$objects`) from edges
(`$$artifact-graph` / `$$artifact-graph-in`) — i.e. it already does "composition is first-class, not on
the object." The text kernel is the one that flattened it. `core.clj:39` defines a `:distillation`
contract — *"versioned derivation … from raw artifacts"* — built for the derived-unit lifecycle, and the
distiller is already named/versioned (`line-distiller-id "text-line-v0"`). Visibility already runs
`:private → :public-material`. **So this spec is mostly "extract the general shape the space-kernel
half-implements, add graduate-on-edit, and stop treating any view as canonical."**

## 5. The one open decision (deferred — does NOT block starting)

**Promotion granularity.** When you accept working-layer changes into the stable version, can you accept
*some* changes and leave others as drafts, or is it all-or-nothing?

- **cherry-pick** → a "layer" is a *tag* on each change; there's no privileged `base-revision` on the
  container; an object's current revision is resolved per layer. (Thinnest container.)
- **all-or-nothing** → a "layer" is a *snapshot* (git-commit-like); a container can carry a
  `base-revision` field directly.

**Recommendation: cherry-pick** — it's the only option consistent with the preserved-plurality /
late-bound-synthesis worldview. **This only bites when the base/active promotion feature is built; it does
not block the first slice.**

## 6. Out of scope / do-not-reopen

- "Line vs paragraph" as the atomic *text* unit — dead. The atom is the container.
- Outliner or canvas as canonical storage — no; both are interpreters.
- Inferred/semantic structure accepted as truth at ingest — no; candidates only.

## 7. First buildable slice (pointer, not a commitment)

Generalize `text_kernel` into: `ObjectContainer` + `Revision` + `SourceAnchor` + one `CompositionEdge`
type (parent/child/order) + an outline projection + "editing a derived unit promotes it to a durable
container." Pressure-test against three sources later: **outliner, chat thread, canvas** (hierarchy,
conversational lineage, spatial affordance).
