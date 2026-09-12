---
name: Renderer law synthesis
description: Core architectural law for Softland's rendering pipeline — derived from 3-way synthesis (Claude + Codex + User) after reviewing 7 external resources against the completed Phase 0-7 substrate work.
type: project
---

## Softland's Core Renderer Law (Session 43, 2026-03-29)

Three-way convergence (Claude + Codex + User):

1. **Keep semantic truth separate from render caches** — `!effective-local-world` is semantic authority, keyed-diff buffer pool is render cache
2. **Cache expensive structure** — prepare once (text layout, SDF eval, tree resolve), sample cheaply many times
3. **Update only what changed** — keyed-diff by identity, not batch rebuild
4. **Sample cheaply from stable identities** — per-slot `writeBuffer`, not full-buffer re-upload
5. **Identity must survive across all operations** — semantic ID carries through to render cache slot (Phase 5 proved this when `:id` was added to `tree->rects`)

### The distinction

- **Phenomenologically**: one world (the user sees one continuous surface)
- **Architecturally**: distinct strata with lawful projection between them (semantic → spatial → GPU)

The goal is to make the semantic world and rendered world feel like the same structure at different altitudes. But the implementation preserves disciplined separation between semantic authority and render caches.

### Concrete next experiments (prioritized)

1. **Slot-map hardening** — add generation counters to buffer pool (from dynamic-sdf-engine). Prevents stale references from writing to re-allocated slots. 1-2 hours.
2. **GPU memory budget tracking** — query adapter.limits, track total bytes across all pools. Prevents silent VRAM demotion (from demote-tracker). 1-2 hours.
3. **MSDF quality-envelope test** — render current MSDF at extreme zoom levels, find where quality degrades. Determines whether continuous semantic zoom needs Slug (from slug resource). 30 min.
4. **Partial-clear shader** — fullscreen quad for clearing dirty regions (from webgpu-field-report). Phase 6E prerequisite. Build when 6E arrives.

### How to apply

Guide Phase 6 (differential pipeline) and all future rendering work by this law. Every new rendering surface should answer: what is the semantic identity? What is cached? What triggers an update? How does identity flow from semantic to render?
