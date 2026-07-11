# W2 G26 — end-gate artifact (faces-as-assemblies, Wave 2)

2026-07-11 · Fable gate, orchestrating session. Shape per the delivery-mode
ruling: ONE serial suite + ONE batched falsification-by-class (5 fresh Opus
subagents) + this Fable pass. All fixes applied at gate BY THE ORCHESTRATING
SESSION (no further subagent spend — Sid's mid-wave token flag honored);
suite re-run green after the fix set.

## Verdict: **PASS** — wave closes (G24/G25 live evidence in W2-INT.md).

Suite after all gate fixes: **46 tests / 804 assertions / 0 failures / 0
errors** (the W1+W2 serial suite + the new `face_gate_fixes_test` regression
namespace — one biting regression per fixed finding).

> **Count correction (2026-07-11 close recheck).** This stated **46/804** does
> not reproduce at committed HEAD (`1725f55`): the seven frozen `face_*` files
> carry 44 deftests (44/789), and the full 8-namespace serial suite
> (+ `clojure_adapter_test`) is **54/880**. The suite is GREEN (0f/0e, re-run
> twice, deterministic) — only the count was mis-stated here. Corrected in
> `RETRO.md` (Verdict + step-4 recheck addendum).

## Falsification-by-class — findings + disposition

Five classes, five reviewers (~503k subagent tokens total). 2 CONFIRMED HIGH,
1 CONFIRMED HIGH-design (contract-text error), 4 MEDIUM, assorted LOW.
Plus one finding the LIVE WEARING itself caught (the G15/G25 dynamic doing
its job twice in two waves).

| # | Class | Finding | Disposition |
|---|---|---|---|
| W2-F1 | live wearing | `face->projection-kind` knew only the W1 static names — `/face boxes-face` (and even keyword `:outline-face`) served `:unknown-projection` → empty scenes. | **FIXED**: `resolve-projection-kind` — a face registered in the arsenal roster routes to `:conversation` (the roster IS the face registry, T14); unknown names still error honestly; per-face `:assembly/projection` envelope field pre-named as the extension. Regression in gate-fixes suite. |
| W2-F2 | arsenal [HIGH] | `valid-face-event?` accepted non-numeric `worn-at-ms` → `(long …)` throws INSIDE the topology → `:retry-mode :all-after` wedges the face's partition / hangs boot replay. | **FIXED**: guard requires `number?`. Regression: 4 poison shapes rejected. |
| W2-F3 | adapter [HIGH] | Edge idempotency key embedded `import-key` (CONTRACT §17's own text) → every EDITED save minted a fresh key, defeating the rk journal; only a racy read-back prevented phantom re-assert transitions. | **FIXED**: stable key `(object-key, kind, target)`; import-key moved to the `:note`; §17 amended in place (dated). Regression: two import-keys → same idempotency key. |
| W2-F4 | adapter [HIGH] | Rename-in-place left a PERMANENT ghost roster entry (register never removes; `/face old-name` still served the orphaned object) — the map lying about wearability. | **FIXED**: `:face/unregistered` event + topology removal branch (`NONE>`) + watcher roster-reconcile (rows with this file's source-ref under an old name are unregistered on every accepted import). IPC regression: register → unregister → gone → idempotent. |
| W2-F5 | adapter [HIGH] | Two files claiming one `:assembly/name` silently merge into one identity (native-claim branch 3 compares neither source-ref nor import-key); pointer + revisions flap by import order. | **MITIGATED + RECORDED**: the watcher now detects the conflict at register time and warns loudly (last-import-wins, documented). The honest per-row `:name-conflict` FIELD needs a row-schema change — named residue with its falsifier (two files, one name → flap) for the fix-window/next contract. |
| W2-F6 | arsenal [MED] | Failed-append retry: live cluster holds the retry stamp, boot replay reconstructs the FIRST WAL line's stamp — G20's "stamps exactly" breached on that path. | **RESOLVED BY RULING**: WAL first-line = canonical (the first attempt IS the wear time; the retry is infra). G20 carve-out added (dated); record-wear! docstring states the semantics. No code change. |
| W2-F7 | server [MED] | The runtime Delay's throw site (`start-distiller-runtime!`) sat OUTSIDE every totality guard — a Delay caches its throw and re-throws on every deref, and W2's install-time roster pull forces it for EVERY client → one boot failure = permanent black screen for all clients (L13: no try upstream). | **FIXED**: boot wrapped; a failed boot yields a poisoned-but-TOTAL runtime map (`:oc-rt nil` + honest first-light-failed flag) → error data-contexts, never a throw. |
| W2-F8 | server [MED] | Arsenal launch + WAL replay + faces sweep ran SYNCHRONOUSLY in the delay — every connecting client's first pull stalled on the full boot window (the trail runtime's own precedent is async). | **FIXED**: module launch stays synchronous (the handle must be in the map); replay + sweep + watcher moved to a future — the roster fills via epoch pushes. |
| W2-F9 | client [MED] | The wear-ack watch cleared the outbox UNCONDITIONALLY — a late/stale ack could nil a NEWER wear before its write mounted (an undercount beyond the accepted depth-1 drop). | **FIXED**: the clear matches `:wear/id`. |
| W2-F10 | render [MED] | The sidebar scroll clamp under-counted the chrome rows (header/back/breadcrumb scroll INSIDE the same container) and subtracted a tab bar the tree does not render — the bottom ~36-112px (exactly where the FACES section lives) was unreachable on overflow. | **FIXED**: `compute-sidebar-content-height` counts the chrome; `visible-h` = full viewport. |
| W2-F11 | render [LOW] | `!sidebar-scene` (the hit-test cache) still gated by HASH compare while its two sibling flows were converted to value-compare (T13). | **FIXED**: value compare (`!last-sidebar-struct`, ::none sentinel). |
| W2-F12 | adapter [LOW] | Provenance targets didn't join across the two §17-sanctioned forms (name → doc-container; object-key → verbatim). | **FIXED**: `asm:`-prefixed values normalize to the same doc-container. Regression asserts the join. |
| W2-F13 | adapter [LOW] | `valid-assembly-name?` missed unicode whitespace (NBSP names → silent /face lookup misses; routing never at risk). | **FIXED**: `(?U)` flag. Regression with a real NBSP. |
| W2-F14 | server [LOW] | `resolve-request` substituted the default conversation address onto `:face-list` requests (dead but misleading; latent the day the projection reads `:address`). | **FIXED**: substitution only for requests that CARRY an `:address` key. |

**Recorded, no code change (accepted at gate):**
- No fsync on the WAL (matches the /assert precedent; dev in-memory scope —
  the honest durability boundary).
- Depth-1 wear outbox drops the MIDDLE wear under triple-rapid re-wear
  (designed, now loss-bounded by the F9 id-match; recorded).
- Half-written file during the boot SWEEP (no debounce there) can mint a
  transient stem-identity ghost; the F4 reconcile removes it on the next
  accepted import of the real name; window is boot-vs-editor-save
  concurrency (rare). Recorded.
- Stem-cleaning collisions (`a:b.edn`/`a b.edn` → one stem) on the
  malformed/nameless path only. Recorded.
- Stray non-face `.edn` inside the faces dir ingests as an invalid face
  (roster shows it honestly as ⚠). By-design-adjacent; recorded.
- `based-on` a never-imported face asserts a dangling edge (late-bound
  targets are the relation kernel's normal semantics); no honesty signal
  distinguishes "pending" from "typo" — recorded with the kinds round.
- Dev wearing boot carries NO relation-kernel runtime → envelope lineage
  edges skip (logged, honest T17-class gap; G19 proves them in-suite). The
  dev-boot rk attach is a pre-named extension.
- Pre-install `/face` command flips face-state without arming requests
  (unreachable: install precedes interactivity). Recorded asymmetry.
- Electric-supersede of an in-flight `RecordFaceWear` mid-write converges at
  boot replay (WAL-first). Verified semantics, recorded.
- The name-vs-stem lesson (W2-F1's sibling): `/face` takes the ENVELOPE name
  (`outline-face`), which the sidebar now shows — the W1 `/face outline`
  habit was filename-based and is gone with the HTTP fetch.

**Clean sweeps (verified by the reviewers, no finding):** m/latest arity
parity on every touched flow (7/7 + the 49-arg call site) · F4-class wear
races (name guard + wear-time sentinel reset jointly close every
interleaving) · equal-source-string re-wear · /face off mid-pull · scrub vs
assembly re-pull independence · reconnect/double-fire wear dedup (wear-id
journal) · WAL torn lines/double replay/concurrent wears · roster overwrite
atomicity · order-key overflow · lane E prims measure law (wrap-once,
`:text-layout` nowhere, ONE fallback-char-width, `:sliver` clamps) ·
`:child-w`/`:child-inset` composition + W1 bit-parity on the no-props path ·
`imp:asm:` two-segment routing + foreign reads · byte-identical re-import
convergence · T19 for non-faces roots · serve totality over nil/corrupt
arsenal.

## Fable pass (CLAUDE.md falsification protocol)

**Architecture.** The §16 placement held under attack: assemblies stayed OC
material; the arsenal held only events + pointers (G20's no-material assert);
the artery stayed read-only (T15 verified at the wear path and live — scrub
re-pulls append nothing); the roster became the face registry, which also
resolved W2-F1 in the §8-consistent direction (the assembly object is what
knows its data need; the static map shrinks instead of growing).

**Failure modes attempted.** Poison depot events (F2) · edited-save edge
storms (F3) · rename ghosts (F4) · two-files-one-name (F5) · boot poison +
boot stall (F7/F8) · stale-ack wear loss (F9) · clamp-vs-render mismatch
(F10) · every clean sweep above.

**Writers/readers/clearers (changed state only).** `$$faces-by-name`: writers
= register (overwrite) + unregister (NONE>); reader = projections via named
fns; cleared per-name by reconcile — no stuck state masks truth now.
`!face-wear-outbox`: writer = wear-face! + the id-matched ack clear; reader =
the Electric write loop. `!last-sidebar-struct`: value-compare cache, watched
by nothing. Delay map: written once, TOTAL by construction.

**Async ordering.** Replay-vs-live-wear (journal) · ack-vs-newer-wear (id
match) · sweep future vs first pull (roster fills via epoch push; honest
empty before) · reconcile unregister vs concurrent register for the same
name (single watcher thread serializes; cross-boot converges by sweep).

**Error-path cleanup.** Boot failure → total poisoned map + failed flag →
honest error contexts. Register/unregister/edge failures → warn + converge
on next event/sweep (T17). Wear write failure → outbox retains → client
re-fire → journal dedups.

**Open doubts (non-blocking, falsifiers named).** (1) W2-F5's warn-only
conflict handling — falsifier: two files, one name; fix is the row field,
queued. (2) The wear-log's `wearer` is hardcoded `human:local` (no auth
exists); becomes real at the A2/multi-actor milestone. (3) The G17 receipt
at committed HEAD re-ran via the arsenal IPC test post-commit — the FULL
receipt against a live dev JVM rides the next boot (fresh-JVM live check
done pre-commit this wave).

## SLOT numbers (final record)

- Apply cost over the committed plurality fixture (JVM, warm-20/median-50):
  outline 0.15 ms · boxes-face 0.32 ms · minimap-reader-face 0.27 ms
  (28/32/37 nodes) — far inside budget; reports all zeros; no activation.
- Live-wearing numbers: see W2-INT.md (real corpus, fresh JVM, post-fix).
