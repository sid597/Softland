# scene-substrate — falsification records

## Wave 2 (P3a, commit a05d6ca) — 2026-07-12, one capped finder (82k tok)

**VERDICT: PASS-with-fixes.** Reactive core traced CLEAN: echo-fan-out
ordering (synchronous consumer edge, no reorder possible) · Missionary
laws (two single-source flows, one combine, effects at the reduce edge)
· T5 identity (unchanged store ⇒ no re-shape) · op-shape plumbing
(per-token container-idx survives to the shaders) · pick inverse
composition (pan then container, correctly reversed).

Findings → dispositions:
1. **HIGH — no clear lifecycle + unconditional cross-mode merge**: an
   orphaned slot (face un-worn / conversation switched) keeps compositing
   over other modes forever; reachable only via the dev spawn API.
   → ROUTED into P3b mid-flight (its rung 1 IS the despawn/clear
   lifecycle; instructed to prove orphans impossible by construction or
   add the mode gate).
2. **LOW — one-frame skew**: fan-out stamps addresses from a separately
   sampled `@!face-context`; blocks transiently unpickable, self-heals.
   → ROUTED to P3b (its per-vi rebuild replaces the path; single-sample
   instructed).
3. **NOTE — pick assumes world zoom 1.0** (shared with legacy, not a P3a
   regression). → STAGED: the gesture slice's gate adds camera-inverse
   (divide by zoom) to pick when zoom gestures land.
4. **NOTE — cid base 100 ⇒ ~1.6KB range writes** (cosmetic). → ROUTED to
   P3b (lower base or fix comment).

## Wave 1 (P1+P2) falsification record

**2026-07-12 · one fresh-context finder (Opus, ~188k tok) per the
machine-cut retro rule · VERDICT: PASS.** No HIGH/MED survived tracing.
The declared risk center — stride/offset alignment across the four
instance layouts and both buffer-pool packers — traced **exactly
consistent** end-to-end (shader struct ↔ vertex layout ↔ packer word
index ↔ stride constant, all four pipelines). Container-0 back-compat is
**byte-identical**: every old shader was `world*zoom+pan`; the new path
at `c=(0,0,1,0)` reduces to the same IEEE-exact expressions, and no
committed caller passes `:zoom` (defaults 1.0).

## Findings and dispositions (all LOW)

1. **Nested same-address write order** (`update-nodes-by-address`) — one
   address on a node AND its ancestor; shallow-first application could
   detach the descendant and hand `f` a nil node. **FIXED same session:**
   paths apply deepest-first; regression
   `nested-same-address-write-applies-deepest-first`.
2. **Probe step! unguarded** — ran outside the frame's try/catch inside
   the m/reduce; a probe exception (e.g. `ctProbe.set(2000,…)` → cid
   range throw) would tear down the whole render consumer. **FIXED:**
   step! wraps its body; on error it logs and self-deactivates.
3. **G4 receipt wording** — `write-containers!` uploads the whole
   `[1..max-cid]` range (256 B at 16 cids), not "16 B"; the load-bearing
   claim (instance buffers never re-upload after the start-time pack)
   HOLDS — traced. **FIXED in place:** CONTRACT G4 read-plan amended.
4. **`store-fns-free?` metadata hole** — a closure in node metadata
   passed both the walk and the EDN round-trip (printing drops meta).
   **FIXED:** the walk now inspects `(meta x)`; regression
   `store-fns-free?-sees-metadata-closures`.
5. **Unregistered `:container` on a slot** — silently unpickable AND
   invisible (GPU reads a zero-scale slot); consistent but silent.
   **CARRIED to P3** as a producer-contract item: the wiring phase
   asserts every slot's cid is registered at upsert-time (P3 gate).

Post-fix suite: **17t/59a green** (was 15t/54a).

## Clean categories (each traced, not assumed)

Stride/offset arithmetic (all four pipelines + both pool packers) ·
missed packing sites (grep-swept `src/app/client`; the four shared
pipelines' packers are the only ones; island-probe has its own, out of
scope) · WGSL validity (16KB uniform array under the 64KB floor; dynamic
uniform indexing by instance u32 legal; `select` arg order right; every
bind-group creation site passes the containers buffer, incl. the
font-swap path and recreate) · `write-containers!` ↔
`containers/effective` contract (cid-0 never clobbered; sparse cids
default identity) · P1 semantics vs CONTRACT §5 · probe integration (T4
clean — step! at the reduce edge; double getCurrentTexture same-frame
safe with load-op) · T5 identity/perf (with the probe driving, content
identical? checks still skip ALL text/pool uploads — instance geos
untouched frame-over-frame) · G5 back-compat (byte-identical, above).

**Known environment fact (not a finding):** headless Chrome on this box
cannot create a WebGPU device ("Failed to initialize vulkan surface") —
G4/G6 receipts come from Sid's headed browser (`localhost:8080/?ct-probe=16`,
[CT-PROBE] logs every 300 frames). Chrome-camera observation for P5:
chrome shares the content camera buffer, so a future world-zoom caller
zooms chrome too until P5's screen-container migration (the T7 item,
already deferred by contract).
