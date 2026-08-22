---
name: feedback_measure_before_assuming
description: Don't assume engineering constraints (like latency) before measuring. Try the simple/direct path first.
type: feedback
---

Do not lock in engineering workarounds (like batching, hybrid models, or local-until-save patterns) before testing the direct path and measuring the actual constraint.

**Why:** In the refactor planning session, Claude assumed editor keystroke-rate Rama round-trips would cause jank and pre-committed to a "local until Ctrl+S" hybrid model. The user pushed back: try direct committed editing through Rama first, measure the latency, and only fall back to batching if measurement proves it necessary. The premature assumption would have locked in complexity that might not be needed.

**How to apply:** When facing a design choice between a simple path and a complex workaround, always try the simple path first unless there's existing measurement proving it won't work. "I think this will be slow" is not the same as "I measured this and it's too slow."
