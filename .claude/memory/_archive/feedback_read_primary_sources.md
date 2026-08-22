---
name: Read primary sources before proposing
description: Always read actual linked articles/sources, not just local summary docs written by prior sessions. Don't go from vibes.
type: feedback
---

When external resources have links to primary sources (articles, blog posts, discussions), actually fetch and read them before forming opinions or proposing work. Don't rely on local summary docs (resource.md, codex-resource.md) written by prior Claude/Codex sessions — those may have biases, overclaims, or missing context.

**Why:** User caught Claude proposing slot-map work based on the synthesis summary without reading the floooh article, HN discussion, or other linked sources. The primary sources revealed the handle pattern is about API design philosophy (not just "add a generation counter"), and the HN discussion had critiques the summaries omitted. CLAUDE.md already says "Don't anchor on previous session artifacts" — this is the same principle applied to external resource docs.

**How to apply:** When an external-resources folder has links, use WebFetch to read the actual articles before proposing engineering work. Flag which sources you read vs couldn't access. Form your own opinion from primary sources, then compare against local summaries.
