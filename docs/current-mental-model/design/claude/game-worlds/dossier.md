# game-worlds — Dossier

Status: drafted, 2026-06-09
Seed links: Jonathan Blow (Notion blog). + level design, game feel, Souls-like
interconnection, diegetic UI, FromSoftware, Nintendo, Valve (added). Sources
named from knowledge, not freshly re-fetched.

## Map

Games are the only medium that routinely makes *huge, unfamiliar spaces*
inhabitable by strangers within minutes — and teaches mastery without manuals.
The relevant traditions:

- **Level design & world architecture** — how spaces guide movement without
  walls of text. **Dark Souls (FromSoftware)**: a single interconnected 3D world
  that folds back on itself (shortcuts that re-link distant areas), creating a
  *mental map* through topology, not a quest log. **Nintendo (Miyamoto)**:
  "kishōtenketsu" level structure — introduce, develop, twist, conclude a
  mechanic without words; the first screen of Super Mario Bros. as a wordless
  tutorial.
- **Game feel (Steve Swink, "Game Feel")** — real-time control + simulated space
  + polish; the *moment-to-moment* tactile loop that makes a control scheme feel
  alive. Juice / feedback: every action gets immediate, exaggerated sensory
  confirmation.
- **Jonathan Blow (Braid, The Witness)** — the game *is* the idea; mechanics as
  epistemology; the designer respects the player's intelligence and teaches by
  letting them discover rules, never by telling. The Witness: an entire island
  that teaches its own visual language with zero text.
- **Diegetic UI** — interface that lives *inside* the world (Dead Space's spine
  health bar; minimaps; in-world signage) vs. chrome HUD overlaid on top.
- **Wayfinding affordances** — lighting, color, sightlines, "weenies" (Disney's
  term, adopted by games: a distant landmark that pulls you toward it),
  breadcrumbing, gating, fog-of-war / progressive revelation.
- **Onboarding curves** — teaching through *play* in a safe early space; difficulty
  as a designed gradient; "press forward to learn."
- **Open-world legibility** — minimaps, fast-travel, points of interest, the map
  screen as a second representation of the same world.

## Extract — the principles they solved

1. **Teach by designed discovery, not instruction.** The best worlds make you
   *infer* the rules by arranging the space so the lesson is unavoidable (Mario
   screen 1, The Witness). Telling is the failure mode.
2. **Topology creates the mental map.** Dark Souls' interlocking shortcuts make
   you *understand* the world spatially; a quest-log list never does. Connection
   you discover > connection you're told.
3. **Landmarks pull ("weenies").** A distant, distinctive object creates
   intrinsic motivation to go there and orients you en route. Desire-line design.
4. **Immediate, exaggerated feedback (juice).** Game feel: the cause→effect loop
   is tightened and amplified until control feels physical. (Echoes Victor's
   immediate connection, made visceral.)
5. **Progressive revelation / fog-of-war.** Don't show the whole world at once;
   reveal as you explore. The unknown is motivating, not threatening, when paced.
6. **Diegetic > chrome where possible.** Information inside the world preserves
   immersion (inhabiting) better than overlaid panels (operating).
7. **Difficulty/complexity as a *gradient*.** Mastery is a curve the designer
   shapes; never a cliff.

## Transpose — what Softland derives (through the lens)

- **"Rediscovery over consumption" is the entire craft of game teaching.** The
  user's deepest value — *"I want to do the work, not know it"* — is exactly
  Blow/Nintendo's "teach by designed discovery." Games are the field that has
  *operationalized* rediscovery. Softland's "papers as logs you rebuild" should
  borrow their grammar: arrange the trail so the insight is *inferred*, gate
  reveals, never just tell.
- **Dark Souls topology = the model for "connection you discover."** The earlier
  import-view instinct ("show how silos connect") is weak as a told graph; games
  prove the strong version is *interconnection you traverse and map yourself*.
  Cross-silo connection should feel like finding a shortcut that re-links two
  areas you knew separately — the "oh, these connect!" moment, not a printed edge.
- **"Weenies" / landmark-pull is the motivational layer Lynch's landmarks lacked.**
  Spatial-wayfinding said landmarks *orient*; games add that they *pull* — they
  create the desire to traverse. Softland's landmarks (a frontier, an open
  problem) should generate information scent (tie to hci-infovis) — pull you
  toward the unexplored.
- **Fog-of-war = the honest rendering of "what's not yet known."** The InfoVis
  dossier asked "how does the overview show what's NOT known?" Games answer:
  *fog*. Unexplored knowledge is visible-as-unexplored, motivating rather than
  hidden or faked. This is a direct, buildable calibration primitive.
- **Diegetic UI ↔ "inhabit, not operate."** Softland's place-not-tool thesis maps
  onto diegetic-vs-chrome: push interface *into* the world (zoom 100 is the
  ultimate diegetic move — the controls ARE the terrain). Reserve chrome for what
  truly can't be diegetic.
- **Game feel = liveness with a body.** The malleable dossier wanted immediate
  connection; game feel says make it *tactile and juicy* — Softland's
  interactions should have weight and feedback, or "inhabiting" stays abstract.
- **Vocabulary:** designed discovery, kishōtenketsu, topology-as-map, weenies /
  desire lines, juice / game feel, fog-of-war, diegetic vs. chrome, difficulty
  gradient.

## Guard — what is dangerous to borrow

- **Gamification ≠ game design.** Points, badges, streaks are the *opposite* of
  what Blow/Nintendo do; they're extrinsic manipulation. Borrow the *teaching and
  spatial* craft, never the Skinner-box layer. (For a knowledge tool, gamified
  manipulation would be corrosive to trust.)
- **Games can afford authored, bounded worlds; Softland's content is open and
  real.** Designed-discovery assumes a designer placed the lesson. Real knowledge
  has no level designer — so Softland must *derive* gating/curricula from the
  material's own structure, which is much harder.
- **Immersion can fight calibration.** Diegetic, juicy, immersive surfaces can
  *overstate* (make a guess feel as solid as a fact). The map-must-not-lie
  criterion has to police immersion.
- **Fun is not the goal; understanding is.** Don't optimize engagement; optimize
  insight. The line is thin and easy to cross.

## Open — questions this forces

1. **Can rediscovery be authored over *real, un-authored* knowledge?** Games place
   lessons deliberately; can Softland auto-derive a designed-discovery path from a
   trail/paper's own structure? (The crux of "papers as logs.")
2. **What is Softland's fog-of-war?** A concrete rendering of the
   known/unknown/contested frontier — likely a top-3 buildable primitive.
3. **How much UI can go diegetic before legibility suffers?** Where's the line
   between immersive-place and unusable?
4. **Is there a difficulty/onboarding gradient for *entering a body of
   knowledge*** — the autoimmune-disease learner's curve, designed like a level?
