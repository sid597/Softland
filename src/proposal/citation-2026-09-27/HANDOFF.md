# The first tool: a citation that stays true (27 September 2026)

Two sessions, softland-de and softland-62, were given Sid's prompt of 27
September: check the idea of making Inland's pointer the first tool on the
store, map the three codebases (the rig, `src/app/server`, `src-inland`) onto
one system, walk the pointer through the protocol, then build. At Sid's word
("you communicate with the other session and decide then tell me") the two
converged and hand over to fresh sessions here. Nothing in this file is a
ruling; Sid rules in the walk.

## What the two sessions agreed

**The first tool is a citation that stays true.** A citation is a reference to
a function or a passage in this repo at a revision, kept as a fact that stands
on that unit. When the file changes, the citation shows where the same unit is
now, or that it is stale. It is the 13 September reference tool
(`docs/builds/inland/integration.md`, "The forced first step" and "What a tool
made inside must satisfy") that `formal-model-2026-09-24/STARTER-round-3.md`
was designed around. Its first material is this project's own `file:line`
citations.

**Inland's pointer comes second, as the citation's hand.** Its targeting rule
decides what a citation binds to: the lines pointed at, the passage, or the
containing function. Changing that rule while using it is Sid's "change a tool
while using it" claim, on real material instead of the orb and the ring.

Why the citation first (checked unless marked):

1. It is the frame's first promise at its smallest: "When something you built
   on changes, you find out. Always." Nothing makes that true yet. The rig
   keeps what each act stood on, by the act's name on the dependent's side
   (`src/rig/store/module.clj`, `:stood-on`), and every index runs forward
   (`reads.clj:218-229`); nothing finds what stood on a given fact. The pointer
   never needs that lookup, because it re-reads its rule on every click.
2. `integration.md`: "Nothing can be by-reference until repo material can be
   referenced as facts … That is the first implementation step." Its fifth
   requirement is "No invented components".
3. Its open questions freeze at the first kept record: what a unit is as a
   thing (open items 17, 60), what a stale citation shows (44), what a new
   tool owes citations written before it (85), and what the store writes when
   it finds a dependent. The pointer's are mostly read-side (derived).
4. The pain is here now: `PROGRESS.md:264-267` records a session bending "Now"
   so line ranges the rig's SPEC cites would not move;
   `src/app/server/ingest/code_import.clj:29` cites
   `relation_kernel.clj:441-448` for a rule now at `:489-497`.
5. It restores Sid's order of 25 September: the model's third round before
   rulings.

What the citation route loses, taken in as conditions (softland-62):

- **Edition one waits for the pointer's screen run too.** The read entry's
  form freezes at the first kept record (`RIG.md` For Sid 3), and only a tool
  held at a screen loads it at a person's rate.
- **The pointer's walk comes before the held-back count.** A contract drawn
  from the citation alone describes only tools woken by facts that act as
  themselves. If the held-back tool is held in the hand, the count would fail
  for a reason that is not the design's. So the one page has both fields: what
  wakes a tool (a fact, or a gesture or view) and whom it acts as (itself, or
  the person holding it). Both sessions walked the pointer on 27 September;
  the walk session folds those in, and Sid rules their choices there.

## The order

1. **The walk** (one fresh session, with Sid).
   a. The citation's thin slice, act by act (below). Sid rules each choice at
      its step, for the rig only until the model checks it.
   b. The pointer's walk folded in, so the one page covers both kinds of tool.
   Output: one page that is the draft contract for the held-back tool; what
   the rig needs, in build order; what the model needs to check it; the
   starters for step 2.
2. **Build and model, side by side** (two fresh sessions, after the walk
   settles). The builder cuts a new worktree off `rig-2026-09-25` named for
   the citation work, builds the thin slice with throwaway records, and counts
   every compiled step it needed. The model session extends the formal model
   to reads, tools, keys and the clock, and runs the walk as a fixed history.
3. **First use** over this project's citations, with a scribe noting misses.
4. **The held-back tool**, written by a fresh session from the one page; the
   count of what the page could not say. No session looks for it in recall or
   in chats.
5. **The pointer's build**, as the citation's hand, then its screen run.
6. **Edition one** waits for both runs.

Scenes two and three (a second person; forgetting someone), login, and the
merges on the map wait until a tool needs them.

## The thin slice

- Only function citations: a `path:line` in this project's documents that
  falls inside a named Clojure form.
- "The same function at the next revision" means the same name in the same
  file, the rule the server's unwired lineage code already uses
  (`src/app/server/ingest/code_import.clj:251-322`).
- Every other citation, most of them Markdown, is reported as "not bound yet",
  as a fact, never skipped: `integration.md`, "a miss is a fact with a subject
  and a time".
- The view is a report.
- The acts: a document is read in; a citation is found and bound to a
  function at a revision; the file changes; the store finds what stood on the
  old unit; the report shows each citation as true, moved, or stale.

## Choices the walk must put to Sid, beyond the eight defaults

- **Which part is the store's walk and which is the tool.** The rig's recipes
  cannot read the store; their only reads are the matched row and git files
  (`src/rig/store/recipe.clj:118-123`). A checker woken by a new revision must
  find the citations standing on that file's units: either a store-read step
  in the vocabulary, or a tool woken by "something you stood on changed". This
  is `STARTER-round-3.md` item 2.
- **Where the reverse lookup lives.** On the depended-on side, admission must
  write on another task, across stores, where Sid's rule says order between
  the two stores exists only through stood-on. On the dependent's side,
  finding dependents fans out over layers. Either way it holds ids, never
  values, is rebuildable from the log, and obeys visibility: a private
  citation must not be revealed to the owner of what it cites.
- **What the store writes when it finds a dependent**, and in whose layer.
- Open items 17 and 60 (what a unit is, and the same-unit rule), 44 (what a
  stale citation shows by default), 85 (what the tool owes older citations).
- Not needed in step 1: expected-versions. The rig's derived run name already
  makes a tool's rerun a retry; it returns with the pointer's keep.

## Findings the builder needs (checked in code)

- No lookup from a fact to what stood on it (above).
- The envelope has no "expected versions" part (`src/rig/store/envelope.clj:25-29`),
  though Sid's envelope ruling lists one; only a stale replace is refused
  (`gate.clj:252-254`).
- The gate checks grammar facts (`gate.clj:138`) but not tool facts: a
  malformed tool is admitted and refused only when the runner runs it. Only
  the operator or the layer's owner writes a grammar (`RIG.md` For Sid 63),
  so on the rig the tool as facts is written by the operator.
- The rig's reader cuts a file at a revision into passages and forms with
  content and position only: no identity by design, no containment
  (`src/rig/revision.clj`).
- The server's cutters know containment and form kinds and names, but a
  Markdown passage's id is its position in a hash of the whole file
  (`src/app/server/ingest/markdown_adapter.clj:20-23`, `:99-105`); any edit
  re-identifies every passage. Its lineage links only named Clojure forms,
  and nothing calls it except tests (`code_import.clj:14`).
- Visibility is a code constant in the rig (`reads.clj:578-590`, For Sid 73);
  ruling 9 says seeded policy facts.
- A group's members are fixed when the group is made (`micro.clj:882-885`).
- No codebase has login; the rig takes the writer and reader from the caller
  (For Sid 50). The thin slice needs none.
- Hazard, not in scope: the server listens on every interface
  (`src-dev/dev.cljc:8`) and `/api/episode/utterance` runs the Claude CLI in a
  directory the request names (`server_jetty.clj:475`, `:676`). Nothing in the
  repo calls its routes.

Paths under `src/rig/` are inside `src/proposal/rig-2026-09-25/` on branch
`rig-2026-09-25`.

## Read first

- `src/proposal/frame-2026-09-15/PROGRESS.md` "Now"; `PARTS-4-8.md` part five
  (resolve); `LEDGER.md:168` (a new version is a new fact on the same thing).
- The rig, on branch `rig-2026-09-25`: `README.md`; `RIG.md` "Defaults taken
  overnight, not ruled" and For Sid 3, 7, 8, 31, 33, 61 to 64; the docstrings
  of `recipe.clj`, `runner.clj` and `revision.clj`.
- `src/proposal/store-next-2026-09-25/CONCLUSION.md`;
  `src/proposal/formal-model-2026-09-24/STARTER-round-3.md`.
- `docs/builds/inland/integration.md`;
  `src/proposal/inland-integration-2026-09-14/FACTS.md` sections 3.2 and 4.8.
- The server: `ingest/code_import.clj:251-322`,
  `ingest/clojure_adapter.clj:209-297`.
- The map: https://claude.ai/artifact/QTaFy5M6soLYtJcWZRoJSp

## Practical

- Load the rama skill before any Rama code.
- One in-process cluster runs at a time. On the Linux machine, run tests under
  `flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock`. The full suite
  took about 17 minutes there: run it once per piece. The toolchain works in a
  worktree cut from `rig-2026-09-25` (softland-62 ran `rig.smoke-test` and
  `rig.store.envelope-test` there once; both passed).
- The code-hunter agent type reads client code only; gather with
  general-purpose agents.
- Never read `src/app/server/env.clj`. No recall search for the held-back
  tool.

## Machines

- **Linux** (`/mnt/data/projects/Softland`) holds the source of truth, the
  rig's worktree `/mnt/data/projects/Softland-rig-2026-09-25`, and, outside
  git, the inventories with `file:line` at
  `/mnt/data/projects/research/softland/three-codebases-2026-09-27/`, plus
  softland-62's uncommitted pointer work in
  `/mnt/data/projects/Softland-pointer-2026-09-27` (its `src/pointer/facts.clj`
  turns Inland's records into facts and waits for the pointer's build).
- **MacBook Air** (`/Users/sid/projects/Softland`) is a one-way mirror of the
  Linux checkout, `.git` included, refreshed at ten past every hour with
  `--delete`. Read there freely; write nothing inside it, or the next sync
  removes it. Work that writes needs a clone outside that folder, or a
  session on the Linux machine. Chats there are deleted after 30 days unless
  `cleanupPeriodDays` is set in the Mac's `~/.claude/settings.json`.

## Open, not decided

The eight defaults (ruled for the rig only in the walk); the rig's adoption;
login; scenes two and three; the merges on the map.
