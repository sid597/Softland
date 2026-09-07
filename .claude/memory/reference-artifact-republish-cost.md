---
name: reference-artifact-republish-cost
description: "Republishing an existing artifact at its URL forces a full Read of the live version first (~80K tokens for a 160KB page), whatever the edit; publish once per session at the end, and let the definer read the committed files"
metadata:
  type: reference
---

Measured 2026-09-06 (path kind, session 11): the Artifact tool refuses a publish to an existing URL until the live version has been Read line by line with the Read tool, even when it is byte-identical to the committed file (it was, both times). The bench (161 KB) cost four Read calls of ~20K tokens; the page (128 KB) four more. A new URL skips the read and breaks every link. Sid's words that day: "there is too much token spend on just making the artifacts not sure if there is a faster way to do this".

**How to apply:** code and docs first; republish each artifact once per session, at the end, both together; make every edit to the file before that one publish. The definer's attacks cite commit hashes and file SHA-256s, so the live URLs are for Sid to feel, not for the other chair to read — a page whose picture did not change can wait a session. Kin: [[feedback-durable-work-lands-in-docs-not-tmp]] (the file is the durable thing), [[reference-webgl2-bench-headless-check]].
