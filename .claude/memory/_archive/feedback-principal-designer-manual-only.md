---
name: feedback-principal-designer-manual-only
description: principal-designer skill is manual-only; never auto-invoke for engineering/product-eng tasks — design canon must not anchor eng discussions
metadata: 
  node_type: memory
  type: feedback
  originSessionId: fc4bcd80-3b17-42c2-9311-e120d68ee1f6
---

The `principal-designer` skill is **manual-only**. Sid invokes it explicitly
(`/principal-designer` or "put on the designer lens"). Never auto-invoke it for
engineering or product-engineering tasks — ingestors, adapters, Rama modules,
kernel/contract work — even when Sid says "discuss this product wise."

**Why:** In the code-ingestor discussion (2026-06-10), "product wise" triggered
the skill, whose boot order force-loads the design-track canon (decision log,
taste, capstone). A proposed-not-ratified design artifact (the braid first
slice) became Fork 0 of an engineering discussion, costing two turns of
confusion/repair. Sid then compared three answers to the same prompt: Claude
4.8 (no canon, grounded in the actual `object-container-ingester-contract.md`)
ranked best; Codex second; the canon-loaded answer last. The canon contaminated
rather than helped. Skill description + body were edited same day; a ratified
entry was added to `design/claude/decision-log.md`.

**How to apply:** Eng/product-eng discussions run on CLAUDE.md behavioral rules
+ actual source code + MEMORY.md (theory of change) only. Ground product
questions in Sid's own framing (e.g. reasoning-trails-for-review), not
design-track proposals. Treat design-track docs the way Codex's track is
treated under [[independent-design-cut]]: a parallel perspective, read only
when Sid routes there. The right grounding artifact for ingestor product work
is `docs/current-mental-model/architecture/object-container-ingester-contract.md`
plus the adapters in `src/app/server/rama/object_container/`.
