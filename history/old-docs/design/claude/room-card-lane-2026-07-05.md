# The Room, the Card, the Lane — Track C sitting 2 · 2026-07-05

**Standing:** design-track ruling doc (Fable, designer lens, Sid live). Non-binding
on build except where marked as demands in the closing handoff block; the
do-not-preclude ledger (write-gesture-sketch-2026-07-04) still binds phase 1.
Drivable companion: `trail-room-sketch-2026-07-05.html` (same directory).
Grounding: read against the AS-BUILT face — `workspace_actions.cljs` mode
derivation, `cards.cljc` / `lanes.cljc` / `wiring.cljs`, FIRST_LIGHT.md
F-L2/F-L3/F-L5, and the git-spine CONTRACT (threads + names arriving).

Diagnosis carried in from first light, located precisely in code: the trail
face already runs full-screen (`/trail timeline` is an explicit mode that wins;
`/trail off` clears) but under the editor's chrome (cmd panel 40px + status bar
24px, sidebar toggleable); and every card wears its full passport at every
altitude — four text lines (name · stamps · badges · raw EDN address) in a
filled box, ×167. Honesty was implemented as verbosity. These rulings separate
them: provenance must be **available at one gesture everywhere, worn only where
altitude affords it**.

---

## 1 · THE ROOM

**R1 — Two things earn pixels in daily read use: the ground and the rim.
[STANCE, strong]**
The ground is the trail face. The rim is the four closed slots — scope · delta
· address · palette (ledger 26). Nothing else is ambient: no sidebar, no
3-pane, no editor chrome. The file explorer, the editor, the chat pane are
ROOMS — entered through doors (a card's address, a palette command), never
standing furniture around the trail. The trail is the sense of where you are
(face-test LAW); chrome that isn't the rim is another app wearing Softland's
face.

Phase-1 piggyback (cheap, uses what exists): the status bar strip BECOMES the
rim — scope | delta | address | palette-hint in its 24px; the cmd panel IS the
palette (it already opens over the face — that is how `/trail` gets typed);
the face's floating address text moves into the rim's address slot; the
sidebar never auto-appears in trail mode.

*Forecloses:* the familiar file-tree while reading. *Reversal:* a palette
command summons the explorer as an overlay room, not chrome. If daily use
shows Sid constantly needing the tree while on the ground, that is form-break
evidence against R1 — the bet is that named cards (tonight) make the trail
itself the navigator.

**R2 — "Trail mode" is a misnomer: the trail is the ground floor, not a mode.
[STANCE]**
Retire the phrase in design language now, in code whenever cheap. Entering the
editor is entering a room; leaving a room lands on the ground. End-state entry
model: cold-open = the ground, resume-in-place — camera where you last stood,
"since you left" in the rim's delta slot. The editor is one door away, never
the container. (Naming of "ground / rim / room / band" as shipped words:
**OPEN** — founder fork on naming; these are working names.)

**R3 — HQ's interim full-screen-face toggle: CONFIRMED, with three
constraints. [DEFAULT]**
The `/trail timeline` ⇄ `/trail off` toggle is the right interim door: it
already avoids the one fatal shape (embedding the trail as a pane inside the
3-pane shell). Constraints so it precludes nothing:

- **C1** — the palette stays reachable over the face, always. The day the face
  eats focus and the palette can't open, the room is a trap.
- **C2** — the face address renders in constant rim chrome, one place, every
  face — not as floating card-level text (ledger 2 + 26).
- **C3** — the boot default (`:editor` when nothing set,
  `workspace_actions.cljs` `local-world-mode`) stays a one-line flippable. No
  feature may grow to require editor mode mounted before the trail can render.
  This guards the R2 future where cold-open lands on the ground.

*Noted for later, not phase 1:* the face-test law wants the trail present even
inside rooms eventually (a spine strip / rim-scope naming where you stand on
the trail). Full swap is acceptable interim; do not preclude a persistent
trail presence in rooms.

---

## 2 · CARD LANGUAGE (F-L5)

**R4 — Closed cards are typography; open cards are surfaces. [STANCE, strong]**
A closed card is a text line the lane spine touches — glyph + name (+ the
reading line at nearer bands). No box, no fill. The box (bg, radius, clip)
appears exactly when a card opens: the box IS the "local space" that the
face-test law says detail must cost. This is what turns 167 entries from a
wall of widgets into a map with labels. Hit target stays the full line region
(the rt-tree node keeps its bounds; only the paint changes). Dead-end ✗ and
the staleness marker ride the line as glyphs.

**R5 — The four bands, over the real corpus (ledger 13 applied). [STANCE,
strong; the band boundaries themselves DEFAULT]**

| Band | The card is | Real-corpus example (tonight's data) |
|---|---|---|
| 0 · mark | glyph + staleness dot on the lane; folds render as chips with mark/dead/?/new counts (ledger 16) | `↓●` |
| 1 · title | ONE line: `glyph name` | `↓ 01e3af9 · docs: H1 clock ruled…` / `↓ FIRST_LIGHT.md` |
| 2 · reading line | title + ONE second line: **when · who** — compressed two-clock stamp + asserter; written-by only when it differs ("sid, by fable's hand") | `claimed 07-04 · arrived 07-05 · import:git-spine` |
| 3 · open, in place | the surface: full clocks, badges, verdict lines (disagreements co-rendered), omissions block, mechanical material preview, and the element's own ADDRESS | the full passport — worn HERE only |

Rules riding the table:
- The band-2 second line is CONTRACT data. When the corpus later carries an
  authored decision/2-liner (ledger 13's middle form), THAT becomes the second
  line and when·who moves to band 3. It is never a truncation of body text
  (render-demand tripwire 4).
- The address is carried as data at every band (hover/copy affordance —
  ledger 1 holds) and **printed only at band 3**. The rim carries the FACE
  address at all times. The printed form is the addressable string itself,
  never a `pr-str` map dump.
- Names: band 1 requires display names — arriving tonight (git-spine E). Until
  they land, the hash id renders, honestly; the band grammar does not wait.

**R6 — Two materials: terrain and kraft. [STANCE, strong]**
Terrain rows (docs, commits, conversations — material) render in ground ink.
Mark rows (relation assertions, verdicts, `:relation-transition` feed entries)
render in the assertion material — kraft/amber, structurally distinct at every
band (ledger 10 taken to pixels). A relation is never a box-card:
- both endpoints on screen → a labeled connector (band ≥2:
  `⊢ based-on · import:git-spine`; band 1: bare `⊢` at the junction; band 0:
  absorbed into fold counts);
- an endpoint off screen → a standalone kraft LINE at its time position,
  naming the far end — the edge may never silently vanish (the map must not
  lie).

Tonight's `:based-on` / `:produced` edges arrive as marks, not as more cards.
**The H1 arming render, in this language:** a kraft `⊢ based-on` connector
between two commit title-lines, asserter badge visible. (H1 arms on the fact
of a typed relation rendering — the KILL text does not require this pixel
form; this is the form it should wear. [STANCE])

---

## 3 · LANE SEMANTICS (F-L2)

**R7 — A lane is an asserted thread; everything else is the band. [STANCE,
strong — ledger 18 applied]**

- **Lane-forming kinds** = the wall's lineage arrows: `based-on`, `produced`,
  `built-over`, `new-direction`. A thread is a connected component over those
  edges. Dead-end terminates the lane spine (already law in `lanes.cljc`).
- **Cross-link kinds** (`references`, `elaborates`) never merge lanes — they
  render as kraft connectors BETWEEN lanes.
- **Containment is not thread.** Doc+blocks, conversation+messages (today's
  `family-key` grouping) fold vertically INTO one card — the doc card at bands
  0–2 IS its blocks folded. Family-key-as-lane was an honest interim; it
  retires as lane semantics when edges land (it survives as the fold rule).
- **Unthreaded material renders as THE BAND**: one designated y-region at the
  bottom; x = time preserved (LAW: x is time everywhere); wrap-packing lives
  only inside the band; fog-tinged; self-declaring — `161 unthreaded · no
  asserted relations yet`. Never auto-filed into fake lanes, never hidden. The
  band is the frontier rendered honestly, and it is exactly the surface the
  /assert affordance works down. Its pressure is D-001 fuel: every relation
  Sid asserts drains it visibly.
- **Arrival choreography**: an edge lands, a doc leaves the band and joins its
  thread — a sayable move ("joined thread · produced by session …"), marked
  with a new-since chip, never a silent reshuffle. Nothing in a derived layout
  was hand-placed, so moves are lawful (taste #6 untouched) — but every move
  announces itself.
- **Ordering** [DEFAULT]: threads ordered by first appearance of their
  earliest member — deterministic from the log; the band always last.
  **Overflow** [DEFAULT]: more threads than viewport lanes → fold-chips with
  counts; mod-wrap never applies across threaded lanes (band-internal only).

*Forecloses:* if relations stay sparse, the face is mostly band. Accepted:
ugly-but-true beats confetti-false, and the band says the true thing —
structure has not been asserted yet. *Reversal:* none needed; the band
shrinks as edges land.

---

## Falsification pass (attacks attempted)

1. *Closed-card lines too small to hit* — hit region is the node's full
   bounds, unchanged; paint-only ruling. Held.
2. *Band-2 badge noise returns* — the second line is capped at one line:
   date(s) + one actor, written-by only on divergence. Held.
3. *Edges with off-screen endpoints vanish* — caught in drafting; standalone
   kraft line rule added to R6. Fixed pre-delivery.
4. *Band starves lanes forever (sparse relations)* — accepted and named in R7
   forecloses; the honest state is the designed state.
5. *Rim delta slot needs walk data that is phase-2* — interim: delta reads the
   arrival clock (new since last pull/session), labeled as such; attestation
   delta arrives with the write milestone (D-008 §5). Named in handoff.
6. *Does R1 reopen anything CLOSED?* — No: D-008 read-only untouched (render
   language only; /assert stays CLI/HTTP-side); D-002/D-005 face order
   untouched; the band adds no view writes.
7. *Full-screen swap vs "trail always on screen" when in the editor room* —
   real tension, deferred honestly: phase-1 full swap confirmed; persistent
   trail-presence-in-rooms recorded as do-not-preclude (R3 note).

---

## What the render contract must build next (handoff to HQ — falsifiable)

Each item is a demand with a check; HQ sequences. Nothing here requires
kernel/schema work; items 1–4 are the F-L5/F-L2 debt made precise.

1. **Band-aware card builders.** `cards.cljc` gains an altitude/band parameter;
   bands 0–2 emit line ops (no box node), band 3 emits the surface node
   (current card, plus printed address). *Check: at band 1 a feed entry
   renders exactly one text line and zero rect fills.*
2. **Address off the card face.** Closed cards stop printing `pr-str` EDN; the
   face address renders once in constant rim chrome (status-bar strip is the
   phase-1 rim); open cards print the element address as its addressable
   string. *Check: `/trail timeline` shows exactly ONE face address on screen,
   in the rim, at every scroll position.*
3. **Rim v0 in the status strip**: scope (land + source counts) · delta
   (new-since-last-pull, arrival-clock, labeled) · address (live) · palette
   hint. *Check: the four slots render on both trail faces; nothing else
   joins the strip.*
4. **Kraft mark rendering for relation rows**: `:relation-transition` entries
   and asserted edges render as labeled connectors (both endpoints on screen)
   or standalone kraft lines (endpoint off screen) — never box-cards; asserter
   badge at band ≥2. *Check: tonight's first `:based-on` edge renders as a
   connector with `⊢ based-on · import:git-spine` and H1's arming screenshot
   shows it.*
5. **Lanes from edges, band for the rest**: thread assignment consumes
   lineage-kind edges (connected components); entries with no lineage edge go
   to the band region (wrap inside band only), with the self-declaring count
   line. `family-key` demotes from lane-maker to fold rule. *Check: after
   git-spine sync, commit chains occupy single lanes; md docs without edges
   sit in one labeled band, and no unthreaded doc occupies its own lane.*
6. **Move announcement**: a card whose thread assignment changed since the
   last pull carries a new-since chip with the sayable reason. *Check: assert
   one `:produced` edge via /assert; the doc's card shows the chip on next
   pull, and nothing else moved.*
7. *(constraint, not work)* **Toggle constraints C1–C3** hold as stated in R3.
   *Check: palette opens over the face; boot-default flip stays one line.*

Sequencing recommendation (designer's, non-binding): 2+3 first (cheapest,
kills the worst of F-L5 on sight), then 4 (H1 wears the right form), then 1,
then 5+6 (needs tonight's edges in the feed), 7 is standing.
