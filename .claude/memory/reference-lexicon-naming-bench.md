---
name: lexicon-naming-bench
description: "The naming-bench artifact: URL, how Sid's picks persist (baked into the page source on self-publish), and the NEVER-CLOBBER rule for republishing it"
metadata: 
  node_type: memory
  type: reference
  originSessionId: 2771817b-3702-4933-8361-eee047adb2bd
  modified: 2026-08-18T19:06:22.509Z
---

The Softland naming bench lives at
https://claude.ai/code/artifact/4ddd8f05-6717-4799-902f-8290f13e56ba
(artifact "Softland Lexicon", favicon 📖). It holds the 2026-08-18 vocabulary
census (1,208 terms, 158 same-thing families across stance/span/older-road/code
dialects) as an interactive adjudication surface: checkboxes per candidate
name, Claude recommendations, write-ins, notes. Sid rules families across
sessions.

**How persistence works:** the page republishes ITSELF via the artifact
capability — Sid's picks are baked into `<script id="decisions">` in the
published page source. There is no external store.

**NEVER-CLOBBER RULE:** any session republishing this artifact from a local
file MUST first fetch the live published version (WebFetch the URL), extract
the current `<script id="decisions">` JSON, and re-inject it into the new
file — otherwise the republish silently wipes Sid's accumulated rulings.

**Converge path:** to cut the repo rosetta from his rulings, read the picks
from the live page source (same extraction); the page's converge view and
its rosetta.md download render the same data. Related:
[[feedback-corpus-terms-never-back-at-sid]].
