---
name: pr-video
description: Record a PR demo or code-walkthrough video (Loom replacement) using the storyboard-driven Playwright recorder and Kokoro narration. Use when the user wants a demo video, walkthrough video, or reviewer video for a PR/branch.
---

# PR videos: feature demos and code walkthroughs

Two video types, one engine. The engine lives in `/mnt/data/projects/dg-demo-videos`
and never changes per PR — if you find yourself editing engine code for a
new video, stop and reconsider.

**Engine (stable, do not modify per PR):**
- Recorder: `dg-demo-videos/recorder/recordDemo.ts` (flags in its header).
  Run it FROM `discourse-graph/apps/roam` (uses that repo's tsx/playwright):
  `npx tsx /mnt/data/projects/dg-demo-videos/recorder/recordDemo.ts --storyboard <file> [flags]`
  In a worktree-isolated session, run it from the WORKTREE's `apps/roam`
  instead — the isolation guard refuses commands whose cwd is the main
  checkout, and a foreground `cd` there wedges the whole shell (recover via
  EnterWorktree with the worktree path). tsx/playwright resolve from the
  worktree's node_modules; the recorder still imports roamSession from the
  main checkout, so the logged-in profile is found either way.
- Tour builder: `dg-demo-videos/tours/buildCodeTour.mjs`
- TTS: `--narrate` flag (Kokoro venv at `dg-demo-videos/tts/.venv`)
- Roam session driver: `discourse-graph/apps/roam/scripts/playwright/roamSession.ts`
  (repo code on `main`; imported by the recorder — any branch checkout works)
- Full setup/login details: `dg-demo-videos/README.md`

**Per-PR (data only, what you write each time):**
- Feature demo → a storyboard `dg-demo-videos/storyboards/eng-XXXX.ts`
- Code walkthrough → a spec `dg-demo-videos/tours/specs/eng-XXXX.mjs`

**App coverage:** feature demos drive Roam (`recordDemo.ts`), any plain web
page like Supabase Studio (`recordDemo.ts --no-roam` — see "Supabase Studio"
in the README), and Obsidian/Electron (`recorder/recordObsidian.ts`, attaches
over CDP — see below). Code walkthroughs are already app-agnostic —
`repoRoot` in a spec can point at any repo or worktree.

**PRs whose only visible output is a DB column** (contract/payload changes)
still make a user-facing demo: drive the real flow in Roam, then read the
row back in the same video with a `js` REST-readout overlay. Do NOT chase a
broken Studio container — see "Showing DB rows without Studio" in the
README, template `storyboards/eng-2153.ts`.

## Code walkthrough (reviewer-facing tour of the diff)

1. Read the PR's merge-base diff (`git diff main...HEAD` in the PR worktree).
2. Pick 6–10 stops, entrypoint outward: where the feature activates, the
   load-bearing hunks, anything a reviewer will question (answer the "why"
   in the narration), then teardown/proof. Note absolute line numbers in the
   worktree's CURRENT files (the builder reads the files, not the diff).
3. Write `tours/specs/eng-XXXX.mjs` — copy `tours/specs/eng-1373.mjs` as the
   template. Data only: id, repoRoot (the PR worktree), per-stop
   title/note/narration/excerpts. Keep excerpts ≤ ~30 lines so slides fit
   1080p; two short excerpts per stop are fine.
4. Build + record:
   ```bash
   cd /mnt/data/projects/dg-demo-videos
   node tours/buildCodeTour.mjs tours/specs/eng-XXXX.mjs
   cd /mnt/data/projects/discourse-graph/apps/roam
   npx tsx /mnt/data/projects/dg-demo-videos/recorder/recordDemo.ts \
     --storyboard /mnt/data/projects/dg-demo-videos/storyboards/eng-XXXX-walkthrough.ts \
     --no-roam --narrate --headless
   ```
5. After a rebase/new commits: line numbers may shift — re-check the spec's
   line ranges, re-run builder + recorder. Nothing else changes.
   `storyboards/*-walkthrough.ts` are generated; edit the spec, never them.
6. Pure-removal PRs: the builder reads current files, which no longer
   contain the deleted code. Point `repoRoot` at a detached checkout of the
   PR's PARENT commit (`git worktree add --detach <scratch> <sha>^`) so
   slides show the pre-removal code with the deleted lines as `hl`; say so
   in the intro. Leave the recreation command as a comment in the spec,
   `git worktree remove` the checkout after recording (the HTML bakes the
   excerpts in at build time), and add a "what deliberately survives" stop
   (shared components/settings that stay) plus the persisted-data answer
   (plain `z.object` strips unknown keys — stale stored settings are
   ignored, no migration). Template: `tours/specs/eng-2096.mjs`.

## Feature demo (driving the real flow in Roam)

1. Build the branch and copy dist to `/mnt/data/projects/dg-test-eng-XXXX/`
   (existing convention; never the shared folder).
   - Copy the WHOLE dist (README.md, CHANGELOG.md, package.json too) —
     recordDemo validates the extension folder and refuses a bare
     extension.js/extension.css pair.
   - Worktree builds silently produce a database-less extension: the env
     files are untracked, so a worktree has neither `apps/roam/.env` nor
     `packages/database/.env.local`. Copy both from the main checkout and
     build with `SUPABASE_USE_DB=local`. Symptoms of getting this wrong:
     build log prints "Not using the database"; the dialog shows "Missing
     required Supabase environment variables". Verify before recording:
     `grep -c 'http://127.0.0.1:54321' dist/extension.js` → 1.
2. Write `storyboards/eng-XXXX.ts` — copy an existing one as template:
   - `eng-1373.ts`: narrated captions, seeding via `js` action, drag actions
   - `eng-1373-multi-canvas.ts`: js assertion probes (storyboard-as-test)
   - Before scripting a UI affordance the feature relies on, verify it is
     actually in the PR branch's HISTORY (`git merge-base --is-ancestor`),
     not just in origin/main or the main checkout — features merged after
     the branch was cut are missing from the branch's build. Fallbacks and
     graph-data pitfalls (node formats, malformed relations, tldrawApps
     seeding) are in the README's "Canvas (tldraw) demos in Roam" section.
   - Probe graph data the demo depends on (node type formats, relation
     definitions) with a small headless roamSession script BEFORE the
     first recording — a title that doesn't match a node type's format
     fails silently (no relations, no node match).
   - UI gated behind a feature flag reads the flag at extension load, so
     the flag must already be persisted in the graph before the demo run.
     Persist it once with a one-scene util storyboard run with
     `--no-extension` (`util-enable-sync-flag.ts` and
     `util-enable-node-sharing-flag.ts` are templates — they mirror
     setFeatureFlag by writing props on the "Feature Flags" block of
     `roam/js/discourse-graph` and verify the read-back). Toggling the
     flag mid-storyboard does nothing without a reload.
   - A title-additions button (`handleTitleAdditions`) shares the
     `.discourse-graph-title-additions` container with other title
     buttons (e.g. Share appears when the title matches a node format) —
     anchor selectors with `:has-text("...")`, never bare `button`.
3. Seed test data idempotently inside the storyboard's first scene (`js`
   action with roamAlphaAPI); recreate pages that must be clean each run.
   - Cleaning up "pages my feature created" is not enough when a step can
     collide by title (import refuses a title an unrelated local page
     holds) — delete by the titles the demo will create too.
   - If a scene must move state the app only reads (e.g. a shared row in
     Supabase standing in for an edit made in another app), do it from the
     storyboard so takes stay repeatable: interpolate
     `process.env.<KEY>` into the `js` string and run the recorder after
     `set -a; . packages/database/.env.local; set +a`. Never commit keys
     into a storyboard. Details in the README.
4. Add `js` assertions after key scenes — storyboards double as regression
   tests, and a red run before the fix / green after is reviewer gold.
5. Record (from `discourse-graph/apps/roam`):
   `npx tsx /mnt/data/projects/dg-demo-videos/recorder/recordDemo.ts
   --storyboard <file> --narrate --headless` (drop `--headless` to watch;
   `--headed --allow-login` once if the profile session expired).

## Feature demo (Obsidian, or any Electron app, over CDP)

Playwright can't `recordVideo` a CDP-attached app, so `recordObsidian.ts`
captures a screenshot loop with real timestamps and assembles via ffmpeg
concat; captions/narration work like recordDemo. Same storyboard shape
(actions: click/type/fill/press/pause/wait_for/js/screenshot).

1. Launch the app with debugging (AppImages need `--no-sandbox` when the
   SUID sandbox errors): `~/Applications/Obsidian.AppImage --no-sandbox
   --remote-debugging-port=9223 &`, then poll `http://127.0.0.1:9223/json`.
   Obsidian opens the LAST-OPEN vault — set the target vault's
   `"open": true` in `~/.config/obsidian/obsidian.json` first, and restore
   it during cleanup. If an Obsidian instance is already running, stop and
   ask — never kill the user's instance.
2. Record (from `discourse-graph/apps/roam`):
   `npx tsx /mnt/data/projects/dg-demo-videos/recorder/recordObsidian.ts
   --storyboard <file> --narrate [--cdp http://127.0.0.1:9223]`
3. Obsidian command palette is `Control+p` → `.prompt input`; DG import
   modal is `.modal` with `text=Select nodes to import`. Escape resets a
   half-open modal between runs.
4. Never actually import/mutate in the demo unless asked — end with Cancel.

## Recording on a tiling WM (i3)

The desktop here is i3 — new windows get tiled to arbitrary sizes, and a
human can accidentally close/resize the headed window mid-run. Rules:

- HEADLESS IS FORBIDDEN — sid's standing rule: every recording must run
  headed and visible on screen (recordDemo.ts now ignores `--headless`
  with a warning; never try to re-enable it). Tell sid which workspace to
  watch, and that the window must not be focused/resized mid-run.
- Workspace 9 is the standing home for ALL recorder windows. The i3 config
  has `for_window` rules: `class="(?i)obsidian"` → move to ws9;
  `class="Chromium-browser"` (Playwright's Chromium — NOT `Chromium`,
  verify with a probe if unsure) → move to ws9 + **floating enable**.
  Never launch a headed window onto the user's active workspace; if a rule
  is missing for a new app class, add it + `i3-msg reload` BEFORE launching.
- Headed screencast captures the WINDOW scaled into the canvas, so the
  window's CSS size must EQUAL the viewport or the video shows a shrunken
  page in a gray frame. Floating (not tiled, not fullscreen) lets
  Playwright size the window exactly; `recordDemo.ts --dpr 2` (default)
  forces device-scale 2 for retina-sharp 720p-layout → 1080p output, and
  `--dpr 1` records big fixed-layout pages (1080p code tours) natively.
  `Emulation.setDeviceMetricsOverride` still pins CDP-attached Electron
  capture (recordObsidian) regardless of tile size.
- NEVER call CDP `Browser.setWindowBounds` under a tiling WM — i3 fights
  it and the call can hang the whole run (this froze a session once).

## Run recordings so they can't stall the session

Feature demos take ~1 min; anything longer means a hung selector, not a
slow run. Narrated code walkthroughs run near-realtime — scenes auto-extend
to the narration, so budget ~60s per stop plus assembly (a 10-stop tour is
~8–10 min; a 300s timeout will kill a healthy run during the final scene).
Launch recorder commands in the background with output to a log file,
poll the log for `Scene x/y` progress, and tell the user which scene is
running. `recordObsidian.ts` has a built-in watchdog (~90s/scene) that
kills a stuck run; for `recordDemo.ts` set a Bash timeout — ≤5 min for
feature demos, `60s × (stops + 2)` for narrated walkthroughs — so a dead
run surfaces as a visible failure instead of silence. Distinguish hung
from slow by whether `Scene x/y` lines keep advancing in the log.

## Verify before sharing (both types)

- `ffprobe`: duration sane, both `h264` video and `aac` audio streams exist.
- Extract 2–3 frames (`ffmpeg -ss <t> -i out.mp4 -frames:v 1 f.png` — ONE
  `-ss/-i` pair per invocation; multiple inputs in one call grab the wrong
  frames) and view them: right scene, captions burned, nothing clipped.
- One filmstrip beats guessing timestamps: `-vf
  "fps=1/4,scale=384:-1,tile=5x4"` puts the whole video in one image, so
  scene/caption alignment and any scene that flashes by too fast are
  visible at a glance. But `-ss` BEFORE `-i` seeks to a keyframe and can
  land seconds off, which reads exactly like a caption desync — put `-ss`
  AFTER `-i` before concluding anything is misaligned, and confirm the
  payoff frame at full resolution (thumbnails hide contrast bugs).
- Failed runs leave `<output>-failed.webm` next to the target for diagnosis.

## Capture what you learn (self-improving skill)

Chat-only learnings die with the session. If, while following this skill,
you discover anything a future run would need — a new app's navigation
tricks (URLs, selectors, auth), a recorder flag interaction, a timing
gotcha, a failure mode and its fix — persist it before ending the task,
generalized (drop ticket-specific details; keep the reusable rule):

- App/engine knowledge → a section in `dg-demo-videos/README.md`
  (e.g. "Supabase Studio": editor URLs are `/project/default/editor/<table OID>`,
  filters via `?filter=col%3Aop%3Aval`), committed to that repo.
- Procedure changes (new step, changed command, new pitfall) → update THIS
  file (`~/.codex/skills/pr-video/SKILL.md`) in the same session.
- Never leave a hard-won fix only in the conversation or a scratchpad probe.

## Narration/caption rules

- `caption` is both the burned subtitle and the TTS line: 1–3 plain
  sentences, no markdown, no unicode arrows. Scenes auto-extend to fit.
- Say what the reviewer needs to believe, not what's on screen: the "why"
  of each hunk, what happens to existing data/events, where the proof is.
