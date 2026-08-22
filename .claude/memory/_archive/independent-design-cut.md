---
name: independent-design-cut
description: "For parallel Claude/Codex design-research tracks, do NOT read the other track or prior design docs — it contaminates Claude's independent cut"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 8fbaf2b6-ad12-4e5b-806a-0f6ff8422e6c
---

When running Claude's design-research track in parallel with Codex (or any other model), the user does NOT want Claude to read Codex's content or the existing `build/*/...DESIGN_RESEARCH.md` theses before producing Claude's own charter/dossiers. Reading them "spoils the context" and collapses Claude's independent perspective into a derived/aligned one. The user got visibly frustrated when an early charter draft kept referencing "the Codex cut" and "the existing thesis."

**Why:** The user's core value is preserved disagreement — each model's perspective preserved separately, Claude's is canonical. Independence only has value if the cuts are genuinely independent. A charter that reacts to someone else's framing is no longer clean-room.

**How to apply:** For `docs/current-mental-model/design/claude/`, write from the vision + real source code + first principles only. Do NOT read `design/codex/` or the `build/` design-research docs to "align." Derive substrate facts from the real object-container code, not from anyone's summary of it. Cross-track synthesis is a separate, later act the user runs. The charter at `design/claude/charter.md` encodes this as an "Independence" section. Related: [[core-reframes]] (preserved disagreement, late-bound consensus).

**STATUS UPDATE (2026-06-10, fabel-design session):** The quarantine was LIFTED by Sid ("now you have access to the whole repo you are free to read whatever whenever") — the independent cuts had been produced and delivered (settlement derivation + evidence review), so the cross-track synthesis act ran: all tracks read and merged into `design/claude/softland-at-scale-synthesis-2026-06-10.md`. The independence rule still applies as a PATTERN for any future parallel-cut exercise the user starts, but the 2026-06 design-research quarantine specifically is over.
