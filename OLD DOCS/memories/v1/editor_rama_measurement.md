---
name: Editor Rama latency measurement
description: Phase 4B measured direct committed editing through Rama — 7.5ms avg round-trip, well under 50ms threshold. Direct path is the chosen architecture.
type: project
---

Phase 4B measurement (2026-03-29): direct committed editing through Rama.

**Result**: avg 7.5ms, p95 ~13ms, max 20.7ms (network spike). Server (Rama write) 4-5ms. Network (localhost) 2-4ms.

**Why:** The consensus plan required measuring before assuming. Threshold was <50ms viable, >100ms needs hybrid. Result is 6x under threshold.

**How to apply:** The editor architecture is direct committed events through Rama. No hybrid model, no local-until-save, no batching. Every committed edit (char, backspace, delete, enter, paste, cut) goes to Rama fire-and-forget. Editor updates locally first for zero-latency feel. Event-reversal undo is the natural consequence — no snapshot-based undo/redo stacks needed.
