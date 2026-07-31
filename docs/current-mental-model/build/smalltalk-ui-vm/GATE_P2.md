# smalltalk-ui-vm — GATE P2 (independent slim gate)

**VERDICT: PASS — zero code defects. One gate leg implementer-attested,
recorded as an open doubt with its falsifier (below), per the rule that
what the gate cannot independently verify is a doubt, never a pass.**

Gate session: Fable, fresh context, 2026-07-31. No implementer context
was shared; `P2.md` (the implementer receipt) was input, never
authority — every claim below was re-verified against disk, the diff,
or a re-run in this session. Tier: SLIM per CONTRACT §8 and the
2026-07-27 cadence ruling (P2 is additive mechanism + consult seams;
the cutover class was P1's FULL gate). Subject: the exact unstaged
state of `/mnt/data/projects/Softland-smalltalk-ui-vm-p2` at HEAD
`9250e1314124a252d43b9fafe0d1cb9c8fa4c54b`.

## Fence + pins — independently recomputed

- **Changed set == §6-P2 allowlist exactly**: 12 production files
  modified + 1 new (`invocation_material.cljc`) + 9 test re-cuts;
  every path allowlisted; zero drift files; `git diff --check` clean.
- **Protected byte-identity (12 files, all zero-diff to P1 HEAD)**: the
  receipt's eight (`verb_registry.cljc` · `binding_material.cljc` ·
  `events.cljs` · `runtime/mouse.cljs` · `relation_kernel.clj` ·
  `object_container.clj` · `matter_room.cljc` · `face_projection.clj`)
  plus four the gate added: `cascade.clj` (§6 FORBIDDEN),
  `face_primitives.cljc`, `face_assembly.cljc`, `facet_material.cljc`.
  Rooms stay master-generic; the interpreter core did not move.
- **SHA pins recomputed by this session**: `episode.clj`
  `7fce46b21976a981cb85f71bb51eb7285fc841ac318687f0341e4cf41096a5bc`
  (the P2 re-cut, matching the receipt and the re-cut test pin with its
  grounds in the docstring) · `relation_kernel.clj`
  `ab283b47ae273aa9a0a42b2e14a690a3804c054a7370ef3fb06ee910a2772ca2`
  (unchanged).
- **Test-pinned facts verified via the re-run suites**: entity-mode
  canonical projection 24,281 bytes / `ab468646…` (re-cut with grounds;
  GATE_P1 residue 3 executed exactly as banked — the pin's value moved
  under the pinned-scan law) · `block-wear-census` eight-vector with
  `:invocation` (source scan + vector pin) · master inventory 9 specs /
  8 non-space · four durable matter acts exact · 17-question floor
  untouched · portal no-write scan green · deterministic-time census
  unchanged (the foldable v3 migration rides `core/now-ms`).
- **Fences**: zero new HTTP routes (`server_jetty.clj` diff read whole
  — only the consult, argv threading, and the receipt emission); zero
  `register-verb!` changes; no module/depot/PState/topology/import
  composer; no anatomy branch in `matter_room.cljc`; the ONE new scene
  action (`:workshop/part`) rides `register-action!`, the halo-card
  pattern W5 names as the sanctioned lane.

## Independent re-runs (this session, this machine)

- **Fast lane**: `clj -X:test fast` → **205 tests / 2,153 assertions /
  0 failures / 0 errors** (26 namespaces, one shared IPC cluster) —
  receipt-exact.
- **Portal bank** (material-portal + material-truth +
  provenance-material): **56 tests / 1,034 assertions / 0 / 0** —
  receipt-exact.
- **V6 at gate time**: installed `claude` = 2.1.220 carrying `--model
  <model>` and `--effort <level>` (low, medium, high, xhigh, max) —
  the honest-subset claim re-verified against the CLI immediately
  before this verdict, not inherited from the wiring-time receipt.
- **T7 re-scan, independent detector**: the only `1200` in `src/` is
  `foldable_material.cljc:77` (the material declaration); `max-share`
  appears only as destructuring/validation; the full-diff read confirms
  `"sonnet"` exists only in `invocation_material.cljc`'s default form.
  `summon-argv` carries no model literal and the absence case (no
  flags when material absent) is test-pinned in `episode_test`.

## Diff read + traps

Every changed server/shared file was read whole; `ground.cljs` and
`face_wiring.cljs` were read whole in diff form.

- **T5**: `workshop-activate!` calls `endPreview` BEFORE `activate`
  (the trap cited in-code); `workshop-reverse!` re-opens the served
  portal before selecting the recovery offer, so a stale projection
  cannot supply the rollback target; preview rides the existing client
  membrane only — no fetch/write on the preview path.
- **T6**: the P1 pointer-phase deferral machinery is untouched; the
  material watch's one addition repaints the Workshop overlay only and
  never touches `!ground-edit` or block rebuild.
- **T8**: `compose-edit` / `edit-candidate` refuse with path + actual +
  complete legal vocabulary (`edit-ops` closed at seven); the
  invocation validators teach through the `teach` wrapper; both
  re-verified by the re-run suites (`p2-edit-to-candidate…`,
  `p2-invocation-master…`).
- **T9/T10**: clean per the fence section; every disturbed pin was
  re-cut in the same change with grounds.
- **Row↔pixel inverse**: the composition section's source paths
  (`[:root]`, `[:root :children i :template]`) match the path shape
  `face_assembly.cljc` itself documents and stamps
  (`:assembly/src-path`), so the inverse is exact by construction;
  `anatomy_test` pins the section side, the banked G7 picks exercised
  it live in both directions.

## Live receipts — slim tier: verified, not re-driven

Per the slim-gate mandate the gate verified the contract-critical live
receipt rather than re-driving the rig (independent re-drives are
FULL-tier; nothing in the falsifier, wearing, or stop-clauses flagged
this package).

- **The spawn receipt (contract-critical)**: the code prints the
  turn-start receipt at the actual spawn seam
  (`server_jetty.clj`, observable stdout); its key set matches the
  banked receipt exactly (`:invocation/precontext`,
  `:invocation/revision-id`, `:spawn/argv-options`); argv composition
  is independently pinned in `episode_test` for both presence and
  absence; the CLI flags were re-verified at gate time. The banked
  revision-id carries the instance marker (`~i~`) consistent with the
  per-block deviation story.
- **G7/G8/G9 receipts**: internally consistent and statically
  supported — 13 seed parts = 13 projection rows; 13 + 4 invocation
  parts = the 17 previewed parts; `fm:invocation` defaults
  (`sonnet/low/thread+2`) are exactly the master's default form; echo
  p95 35.4ms under the 52ms bar (environment attested swiftshader
  first, per the standing rule).

## Judgment calls — all four RATIFIED

1. **F6 widening**: `worn-five` wire keys retained, ruled meaning
   widened to the seven-facet worn block composition
   (`#{:anatomy :attention :foldable :invocation :positioned :threaded
   :text-body}`), `:provenance` stays the probe master. The contract
   ordered exactly this ruling in the section work; disclosed and
   test-asserted (the recipe suite's wearers now carry all seven).
2. **Foldable floor→v3** (the code floor moves to `paste-clamp-form`):
   same class as P1's ratified threaded floor→v1 — W2 totality; a v2
   floor could not supply the key the paste consult binds against.
   `binding_dispatch_test` asserts the binding rows are byte-equivalent
   to v2 with exactly one added key.
3. **The stdout spawn-receipt repair**: the dev classpath's logging
   provider was silent, making the promised live receipt unobservable;
   the smallest in-fence repair moved the already-constructed receipt
   to observable stdout at the allowlisted seam. Ratified; see residue
   3 for the honesty wrinkle it leaves.
4. **G10 detector narrowing**: the falsifier's first raw FAIL
   (`:t7-no-policy-literal-leak false`) was its own substring detector
   matching `1200` inside the pre-existing `120000` timeout. This
   session's independent token-boundary scan confirms: harness false
   positive, no product escape. Only the detector was narrowed; the
   replayed attack set passed.

## Open doubt + residue — non-blocking, falsifiers named

1. **OPEN DOUBT — the CLJS compile leg (275 files / 0 warnings) is
   implementer-attested, not gate-verified.** The gate's own compile
   re-run in the fresh worktree was blocked by the Electric compiler's
   interactive activation wall (the implementer's session carried
   "Electric token links" it removed at teardown; the gate could not
   reproduce that scaffold headlessly). Risk is small — every `.cljc`
   compiled and ran green JVM-side in both suite re-runs, and the full
   diff read screened the CLJS surface for the warning class. Falsifier
   is free: Sid's first `shadow-cljs` compile/watch on this branch
   surfaces any warning instantly; or one interactive login in the
   worktree unblocks `clj -M:dev -m shadow.cljs.devtools.cli compile
   dev`.
2. **Invocation view-model coupling**: `block-anatomy-view-model`
   special-cases the candidate by its part ids — four trailing rows
   assumed, and the heading string is duplicated in code for width
   measurement only. A retuned heading paints new text (material wins)
   but measures stale width; a fifth invocation part needs a code
   touch. Falsifier: workshop-retune the heading text and compare
   painted vs measured width.
3. **The spawn receipt reconstructs `:spawn/argv-options`** from the
   wear instead of slicing the actual argv; today they cannot diverge
   (wear is floor-or-valid, both flags always present), but a future
   guard change in `summon-argv` could make the receipt lie. Falsifier:
   one-line test asserting the receipt options equal the argv subvec.
4. **`paste-projection` projects only the FIRST paste envelope per
   block** (docstring-disclosed). Falsifier: two over-threshold pastes
   into one block — the second gets no fold header.
5. **Echo remains swiftshader-attested** (p95 35.4ms with the Workshop
   open vs P1's 17.5 without — growth expected, bar held). Sid's headed
   wear stays the real-GPU instrument (carried from GATE_P1).
- *Observation, not residue*: `__portal.openMaster` now appends
  `&master=` to `location.search` (the Workshop auto-open door); a
  behavior widening of an existing hand inside the allowlist, no new
  endpoint or verb. And the human edit hand is a `js/prompt` dialog in
  v0 — the contract's "block_edit machinery where warranted" was not
  exercised; taste, not fence.

## Custody

- The worktree was restored to the exact unstaged pre-gate state
  (status list = the allowlist set; `git diff --check` clean). Gate
  scaffolds removed: the blind `env.clj` symlink (created per the
  standing convention, never dereferenced) and the `node_modules`
  link. A `.shadow-cljs/` build cache remains in the worktree
  (gitignored; partly from the gate's stalled compile attempts).
- `env.clj` was never read. No commit, no push; code and docs both
  remain unstaged. `PODCAST_SCRIPT.md` untouched.
- **Sid's first unscaffolded Workshop open and his first real
  structural change routed in-land are NOT gates** — they are the
  C1/C2-class instruments, recorded here per CONTRACT §8's own order.
- Next act: **Sid's commit ruling** (code and docs ride separate
  commits; never pushed; after the code commit lands, re-run the
  HEAD-dynamic suites — `git_spine_gate_test`, `code_atoms_test` — at
  committed HEAD per §12). Package close after that = board flip;
  retro joins the stratum batch.
