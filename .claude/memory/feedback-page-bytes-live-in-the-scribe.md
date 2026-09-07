---
name: feedback-page-bytes-live-in-the-scribe
description: Strong recommendation, near-always, for reading, editing or publishing an artifact page: the thinking session keeps page bytes out of its own context and works through a Sonnet page-scribe subagent (.claude/agents/page-scribe.md) that maps, shows, applies the parent's patch, checks, diffs live vs repo, publishes, and returns a receipt; the parent departs from the default when its thinking needs the text in front of it
metadata:
  type: feedback
---

Strongly recommended, and the right call nearly every time: the Fable session
thinks and authors, and does not read a whole page or call Artifact read
itself. Reach for the scribe first; reading a page inline is the exception
that needs a reason said out loud. It spawns one `page-scribe` (Sonnet) per
page and keeps it: `map` (a table of contents, about 2K tokens), `show` (a
window around an anchor), `apply` (new text in a scratchpad file plus an
anchor and an operation), `check`, `publish`. The scribe reads the page in a
context that is thrown away and returns a receipt. Hands, not head: it does
not decide what the page says. Sonnet by default; Opus per spawn when the ask
is a diagnosis inside a bench's script. Publish cadence per
[[reference-artifact-republish-cost]]: usually once per session, at the end.

Departures exist and are judgment, not breaches, but they are rare. A session folding a long review into
a page may need a whole section in front of it; a redesign of a figure may
need the SVG; a bench bug may need the parent to read the script itself. Read
it then, and know the cost: about 3 bytes per token, carried by every later
request in the session.

**Why:** Sid, 2026-09-06: artifact sessions reach 400 to 500k context.
Measured in three of that morning's transcripts: the read-before-republish
guard plus the line-by-line Read of the saved live copy cost about 150 KB per
page, two pages per session, and the live copy was byte-identical to a git
version every time. Sid's framing: "can't this type of read be done by sonnet
models as a subagent and modifying something in there can be passed from the
parent." And the register: "the judgement is like sometimes dynamic and
requires to do smth while in the process" — a default with a reason, not a
law.

**How to apply:** page bytes live in the scribe's context or in git nearly
always; Sid, 2026-09-06: "highly highly highly make it suggestive that we use
the sonnet for artifact one".
A bench's script is a page too. When the parent must patch code inside it,
the scribe shows only the function being patched (a window of the lines the
patch replaces), never the fixtures, the panel, the controls or the render
for orientation: those are described changes the scribe places itself. Fired
2026-09-06, session 12 of the path round: the parent read about half of a
187 KB bench (the executor windows it needed, plus 50 KB of fixtures, panel
and controls it did not) and Sid asked "are you reading the files yourself
why? should you not be using sonnet to read the artifact code".
The parent's page cost is the text it authors, plus whatever it chose to
read with a reason. Kin: [[feedback-waist-sort-hunters-code-only]] (the same
shape for code), [[reference-webgl2-bench-headless-check]] (the scribe's
check command), [[feedback-two-turn-boot-low-then-max]] (the load half).

**The check that closes Sid's worry about weaker hands (2026-09-06, "they are
shitty models comparatively how can you give them this role"):** the road is
bounded to placement and wiring; the parent hands functions verbatim, checked
in Node first, and lands a probe that runs against the *patched* file; every
earlier deep link is dumped before and after; and before anything is published
the parent reads the scribe's **diff** (not the file, ~15K tokens for a large
patch) and fixes what it finds. Same session: the diff read caught one false
readout wording; the scribe's own re-dump caught a crash in the parent's fix
snippet. Both directions catch things; neither replaces the other.
