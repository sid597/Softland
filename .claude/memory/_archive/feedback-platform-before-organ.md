---
name: feedback-platform-before-organ
description: "Never design a custom infrastructure organ (persistence, durability, backup, event log) without first loading /rama and naming the platform-native answer; a second patch on the same design = STOP and re-derive"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: fc7969c2-8891-4173-bb6b-9add62f5e0e0
---

2026-07-15, the journal fiasco: across four turns I designed a hand-rolled write-ahead journal beside Rama — an organ that re-implements Rama's core primitive (depots ARE durable, replayable event logs; "all storage is durable and replicated"). The real problem was that dev runs the in-memory TEST cluster (`create-ipc`) as the product; the platform answer was "deploy a real single-node cluster" the whole time (local ref: `docs/reference/rama/32-downloads-maven-local-dev.md:71`, `22-backups.md`). Each wall got a patch instead of a stop: fsync latency → group commit; semantic grain → burst coalescing; and the third wall was fatal — burst-grain durable truth under per-op rendered state re-created optimistic updates, contradicting the settled 07-12 no-optimistic-echo ruling. Sid caught it, furious: "do you even look at the past... wtf is journal??? we are talking about events and rama."

**Why:** two trigger gaps. (1) I mis-filed cluster/persistence/deployment design as "ops chat," so the rama-skill rule ("ALWAYS before any Rama code") never fired — but persistence design IS Rama design. (2) No existing-organ check fired — the base-layer commission's own closing rule ("check what already exists before minting new organs") applies doubly to platform capabilities. Also [[feedback-requests-are-approximate]]: the patch cascade was the wall-tell I ignored.

**How to apply:**
- Any design touching storage, durability, deployment, clusters, backups, replay, or event-log mechanics → load the /rama skill FIRST and name the platform-native answer before proposing anything custom. Custom organs need a stated reason the platform one fails.
- A second patch on the same design is a STOP signal: re-derive from the root, don't refine the patch.
- Every new proposal gets checked against the settled-ground list explicitly (the 07-12 no-optimistic-echo ruling was in my own context and I contradicted it).
