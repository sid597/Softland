# Engine — shared rendering infrastructure

[Up: client](../README.md)

Input: declared data, group transforms, device handles, scene-color settings and target requests. Output: checked values, coordinate calculations, GPU transport, admitted render targets and presentation commands. The engine supplies shared computation and resource ownership for the content families.

```mermaid
flowchart TD
    S["schema.cljc<br/>validation mechanics"] --> T["transform.cljc<br/>group coordinates"]
    T --> D["device.cljs<br/>shared GPU transport"]
    C["color.cljc<br/>scene-color contract"] --> D
    C --> P["compositor.cljs<br/>physical targets and presentation"]
    L["limits.cljc<br/>limits and byte pricing"] --> P
    R["rungs.cljc<br/>resolution admission"] --> P
    B["leases.cljs<br/>logical region identities"] <--> P
    U["buffer_pool.cljs<br/>changed instance rows"] --> F["Family renderers"]
    X["expression.cljc<br/>arithmetic as data"] --> E["executor.cljc<br/>constructions over capabilities"]
    E --> F
    V["coverage.cljs<br/>the shared filler and its atlas"] --> F
    D --> F
    P --> F
```

| File | Role in this computation |
|---|---|
| [schema.cljc](schema.cljc) | Apply declared schemas and report malformed values with paths and named errors. Families supply the vocabulary. |
| [color.cljc](color.cljc) | Define transfer calculations and the legacy/linear-premultiplied scene-color modes. |
| [transform.cljc](transform.cljc) | Derive a shared affine coordinate system and compact GPU indexes from a caller-owned group registry. |
| [device.cljs](device.cljs) | Allocate/upload shared camera and group buffers and configure matching shader color behavior. |
| [buffer_pool.cljs](buffer_pool.cljs) | Retain an ordered instance vector and upload rows whose values changed. |
| [limits.cljc](limits.cljc) | Read selected device limits and calculate nominal texture storage cost. |
| [rungs.cljc](rungs.cljc) | Choose a resolution within a supplied physical budget using pure arithmetic. |
| [leases.cljs](leases.cljs) | Preserve logical region identities and composite indexes across physical resource replacement. |
| [compositor.cljs](compositor.cljs) | Allocate, reuse and retire physical targets; reconcile region leases; encode presentation. |
| [expression.cljc](expression.cljc) | Compile and evaluate a tool's arithmetic written as a string over named numbers, reporting the names it reads. |
| [executor.cljc](executor.cljc) | Run a construction written as data over a capability table, in order, reporting every value read and every operation missing. |
| [coverage.cljs](coverage.cljs) | The per-pixel coverage program text and paths share, the dynamic curve/band atlas paths pack into, and the region instance row. |

The caller owns group registries and pure results. Buffer pools retain instance storage; binding owners retain logical associations; compositors own physical textures and retirement; an atlas owns its two textures and their mirrors. The executor and the expression evaluator hold nothing: a construction's result depends on exactly what it read, and the caller keys its caches on that. Logical identity, admission arithmetic and physical allocation are separate responsibilities. The browser caller acquires the GPU device and supplies it to this infrastructure.
