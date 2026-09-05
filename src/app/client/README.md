# Client — rendering and spatial computation

The client turns text, images, paths and bounded 3D scenes into spatial answers and GPU work. Four content families share an engine for coordinates, color, GPU transport and render-target ownership. The browser harness constructs inputs, composes frames and collects evidence.

The diagrams are Mermaid source inside Markdown. IntelliJ displays them in Markdown preview with the [Mermaid plugin](https://plugins.jetbrains.com/plugin/20146-mermaid). Neovim in Kitty can display formatted text and diagrams directly in the buffer using [render-markdown.nvim](https://github.com/MeanderingProgrammer/render-markdown.nvim) and [Snacks image](https://github.com/folke/snacks.nvim/blob/main/docs/image.md), with Mermaid CLI installed. A browser preview is also available through [markdown-preview.nvim](https://github.com/iamcco/markdown-preview.nvim) with `:MarkdownPreview`. Edit the fenced text to update the diagram; generated images are disposable views. The prose and role tables remain available without a renderer.

```mermaid
flowchart TB
    A["Browser capabilities and font/image assets"] --> H["harness/<br/>inputs, execution, evidence"]
    H --> T["text/<br/>shape, lay out, query, draw"]
    H --> I["image/<br/>register, retain, draw"]
    H --> P["path/<br/>classify, tessellate, draw"]
    H --> R["region3d/<br/>maintain scene, pick, render region"]
    E["engine/<br/>coordinates, color, buffers, targets, leases"] --> T & I & P & R
    T & I & P & R --> O["Spatial results and GPU commands"]
    O --> H
    T -. "placed text calculations" .-> R
    P -. "placed ink geometry" .-> R
```

Arrows show inputs and consumed results. The caller determines execution order: family preparation produces retained resources or derived values, and drawing encodes commands into caller-owned passes. The engine provides shared infrastructure; content selection belongs to callers.

| Enter a scope | Responsibility | State owner |
|---|---|---|
| [engine/](engine/README.md) | Validation, group coordinates, scene color, GPU transport, target allocation and region leases. | Callers own pure registries; explicit pools, binding owners and compositors hold resources. |
| [text/](text/README.md) | Font loading, shaping, positioned layout, source/geometry queries and outline rendering. | Providers hold shaping resources; results hold layout; renderer systems hold GPU resources. |
| [image/](image/README.md) | Source verification, crop/placement rules, asynchronous texture residency and ordered draws. | Each renderer system holds its source registry, bytes and GPU residency. |
| [path/](path/README.md) | Ink/fill geometry, CPU classification, triangle derivation and rendering. | Components are caller values; renderer systems hold derived mesh caches and vertex buffers. |
| [region3d/](region3d/README.md) | Scene derivation/maintenance, spatial queries, placed content, offscreen rendering and composition. | Renderer systems retain scenes/buffers; engine compositors own physical texture leases. |
| [harness/](harness/README.md) | Browser acquisition, fixtures, frame driving, readback and bounded checks. | Drivers hold test resources and evidence; the entry publishes browser completion state. |

Component values describe input. Derived representations can be reconstructed from that input and may be retained for reuse. GPU ownership adds allocation, invalidation and teardown responsibilities. These three meanings stay distinct even when one system map holds all three.

The [browser build configuration](../../../shadow-cljs.edn) enters the render verifier through the harness. That entry exercises rendering with controlled fixtures; it does not establish an interactive product path. External asset formats, browser behavior and libraries remain dependencies of the corresponding family.

To zoom in, open a folder map, then a source file's namespace docstring, then a function's docstring. To zoom out, follow the containing folder's map. Folder maps describe immediate children; file and function explanations live with their code. [Agent upkeep instructions](AGENTS.md) describe how to keep those levels consistent during changes.
