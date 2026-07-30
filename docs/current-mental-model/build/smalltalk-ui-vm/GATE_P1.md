# smalltalk-ui-vm P1 — independent gate record

2026-07-30 · Fable (fresh gate contexts — no shared context with the
implementer) · tier: **FULL per CONTRACT §8 (cutover-class)**. The gate ran
across two fresh sessions: gate session A ran the fence re-hash, the full
diff read, the independent G1 re-run, and its OWN isolated live drive
(~90% of the gate), and was interrupted pre-verdict; this finisher session
spot-verified A's claims against its banked machine logs (never its prose),
re-ran the anatomy+assembly slice, re-read the three riskiest code
surfaces, adjudicated the four disclosed judgment calls, and wrote this
record. Inputs used as prior-pass records, never authority: `P1.md`,
`NOW.md`, session A's handoff + drive logs. The judged object is the exact
unstaged worktree at HEAD `c9a18efc3f5a26ccf92aa94795031b1467a50ac2`.

## VERDICT: PASS

Zero code defects found by either gate context; the two mid-drive alarms
were session A's own harness bugs (slot-wrapper walk · JSON-nested revision
regex), visibly present in drives 1–2 and corrected in drive 3. Gate PASS
does not close the phase: Sid rules the separate code and docs commits.
P2 stays closed until his ruling.

## Fence — re-verified independently (session A; finisher re-checked)

- HEAD `c9a18ef`; index empty; `git diff --check` clean (both sessions).
- Changed tracked set = exactly the §6-P1 allowlist: 7 source + 5 test
  paths + `build/smalltalk-ui-vm/NOW.md` (docs, free); new
  `anatomy_material.cljc` · `anatomy_test.clj` · `fixtures/anatomy/`
  (15 goldens + 1 counterexample, finisher-verified on disk). The three
  test re-cuts not named in §6 by file (`face_assembly_test` ·
  `provenance_material_test` · `space_material_test`) sit under §6's
  disturbed-namespace clause — the finisher read all four re-cut diffs:
  every moved pin carries its grounds in the same change (entity-mode SHA
  `7003fe90…`/23,104 bytes · wear-census vector + `:anatomy` · threaded
  v0 byte-stability asserted beside the grammar-1 floor · 6→7 non-space
  master count · runner pure-inventory entry). Foreign
  `PODCAST_SCRIPT.md` / `P1_IMPLEMENTER_NOTES.md` untouched, unclaimed.
- All six zero-diff pins recomputed byte-exact by BOTH sessions
  (`e1f1836f…` `episode.clj` · `ab283b47…` `relation_kernel.clj` · the
  other four status-clean); forbidden `cascade.clj` untouched; `env.clj`
  never read by any P1 context (the finisher's permission guard actively
  denied even an `ls` naming its path — the hard rule enforced below the
  session).
- `block-tree` grep-negative in `src/`; every temporary
  comparison/export hand gone.

## G1 — suite + compile, independently re-run

- Session A fast-lane re-run: **200 tests / 2,030 assertions, 0/0**
  (pure 159/1,613 + shared 41/417; 26 namespaces; the fail-closed
  inventory carries `app.anatomy-test` pure).
- Session A's grep-derived pinned-scan batch (material-portal ·
  material-truth · provenance-material · material-circulation · episode):
  **82 / 1,192, 0/0** — a batch the implementer never ran as one unit.
- Finisher re-ran the repaired slice: `app.anatomy-test` +
  `app.face-assembly-test` → **14 / 134, 0/0**.
- CLJS pinned compile: **273 files, 0 warnings** (warm content-hash
  cache, 0 recompiled — a same-bytes revalidation, noted honestly).

## G2 — byte identity + deletion

- The committed JVM post-invariant is green over all 15 legacy goldens
  (inside the fast lane above) — the ONE-SHOT law's committed-harness
  form: goldens cut from `block-tree` in one browser context BEFORE
  deletion.
- Implementer both-paths receipt banked in `P1.md`: fixtures 15/15 exact
  with assembly provenance complete on every node; the named live
  post-merge surface `window.__ground.blocks()` enumerated/compared 1/1.
- Session A independently confirmed the SEED BYTES: its own rig's
  freshly-seeded baseline revision id came out identical to the banked
  `rev:fm:anatomy:1d41b593…:479db276…c60093`.
- Deletion: grep-negative in `src/` (both sessions).

## G3 — the loop live (session A's own isolated rig, not the implementer's)

Rig: rsync of the exact uncommitted tree · blind `env.clj` symlink ·
`:8097` · `LAND_CLUSTER=0 LAND_PINNED=1` · masters seeded through the
existing `ensure-master!` / `ensure-active-source!` lanes. Environment
attested FIRST (vendor google, architecture swiftshader). Receipts in the
drive logs, finisher-read line by line: deviate mints candidate
`…aa5088a9149b` → preview wears the candidate with server bytes immobile
and focus/mode/`:idle` pointer intact, rail + indent visible on a real
settled block → `endPreview` restores exactly → activate renders both
pixels → full client restart re-reads the active pointer (rail persists)
→ rollback (activate of baseline) accepted, rail gone in 2s with no
nudge → second restart holds baseline. Bonus receipt: a malformed
rollback got a teaching refusal (`activation/grounds-invalid` with actual
value + type) before any append.

## G4 — echo

Attested-first swiftshader rig, n=50 real CDP keystrokes with the
candidate preview APPLIED during sampling: **p50 13.5ms · p95 17.5ms ·
max 18.2ms · zero over the 52ms bar**, focus and `:idle` pointer held
throughout (an earlier consistent run: p95 17.7 / max 25). Implementer's
own post-repair run: p95 18.2. All software-rasterizer — residue 2.

## G5 — falsifier standing + W8 live

- The fresh falsifier's first-FAIL (normal-priority machine+hover:
  legacy `[rail box]` vs static-anatomy `[box rail]`, while the tie
  golden requires the opposite order) and its smallest in-fence repair
  are visible in the diff; the counterexample is a committed fixture
  (`counterexamples/machine-hover-normal-priority.edn`) with the generic
  semantics covered in `face_assembly_test` (finisher-read: ranked
  siblings reorder only within ranked-occupied positions + the
  marker-never-leaks assertion).
- Session A replayed both attacks live through the client's own compiled
  interpreter: normal `[ground-mark ground-box]`, no conflict · tie
  `[ground-box ground-mark]`, conflict lint present ·
  `rankMarkerLeaked=false` in both.
- W8 live: a preview over baseline survived 20 keystrokes of real typing
  (the value-equal-serve membrane rebase working), then vanished cleanly
  at `endPreview`. No preview leak.

## Trap spot-checks (session A full-diff read; finisher re-read the three riskiest surfaces)

- **T1** — `block-tree` and its private helpers gone; the wrap truth
  moved whole into the primitive vocabulary, one implementation shared
  by render and machine-selection copy.
- **T2** — the block-root primitive branches on DATA (`machine?`), never
  component identity; `assembly-for` never dispatches on a component.
- **T3** — every vocabulary closed (presence 15 · view · props · stamp ·
  one-level defs); refusals teach with offending path, actual value, and
  legal set.
- **T4** — corpus enumerated with its verb named; the live post-merge
  surface driven by implementer AND session A independently.
- **T5** — preview is a client membrane; the finisher read the material
  watch: a substantive serve move ends the stale preview BEFORE render,
  so the activation a preview previewed always ends it; the value-equal
  rebase grafts only identity, never pixels.
- **T6** — substantive re-derive defers behind a pending/active pointer
  and flushes at terminal pointer-up; no derived state copied into a
  second atom; caret/focus continuity live-proven across
  preview/activate/rollback/restart.
- **T10** — every disturbed exact pin re-cut in the same change with
  grounds (all four re-cut diffs finisher-read).
- **T11** — the compile cache keys on `[master-id revision-id]`, an
  activation-minted slow identity; worn anatomy rides wears into
  render-sig.

## Adjudicated judgment calls — all four RATIFIED

1. **`:focused-or-hover` as the 15th presence key.** W1's own grounding
   ("the census is block-tree's own") governs over the literal 14-key
   list: the legacy attention box's actual predicate is
   `(or focused? hover?)` (finisher-verified at pre-change
   `ground.cljs:723`); two separate rows would double-draw when both
   hold. The vocabulary stays closed and the key carries its grounds in
   the docstring beside the grammar. F-class precision, pre-disclosed
   twice.
2. **Root multi-stamp map.** G2 exactness is the higher law: the legacy
   root char-grid mints multiple facet contributions; a single
   `{facet site role slot}` row cannot carry them without losing
   exactness. The widening stays closed — a keyword-keyed map whose
   values are each validated stamp rows — and compilation stays
   data-driven (`:stamps` bind), no dispatch.
3. **`fm:threaded` floor-form → the v1 form.** §6's actual constraint —
   "the existing keys byte-stable" — holds: v0's default bytes and
   grammar declaration are frozen verbatim (now separately test-pinned).
   The floor is W2's TOTAL fallback; with grammar 1 live it must supply
   the thread-edge keys (the resolved-wear law). Floor rail values are
   inert at the seed (no thread-edge parts there), so byte-identity is
   undisturbed.
4. **`:assembly/sibling-rank` — legal under §6's "additive, disclosed"
   clause.** G5 genuinely compelled the widening: no static part order
   satisfies both the normal-priority counterexample and the tie golden,
   because sibling order is `facet-material/compose`'s APPLY-time
   result. The convention is generic (no component identity), optional
   (nil-guarded), a no-op absent the marker, and leak-proof (private
   namespaced key stripped before primitives — proven in-suite and live
   twice). Existing assemblies' semantics are unchanged; disclosed in
   receipt and NOW; the counterexample is a committed fixture.

## Non-blocking residue — each with its cheap falsifier

1. **Committed goldens carry a precomputed `:anatomy-view`**, so the
   client-only view derivation (`rebuild-block!`'s view-model census) is
   outside the JVM harness. Falsifier: a committed view-model census
   test at the derivation boundary.
2. **Every echo number in P1 is software-rasterizer** (swiftshader,
   attested). Real-GPU echo is unmeasured by design; Sid's first headed
   wear covers it — the standing echo report is the instrument.
3. **The entity-mode byte pin** (`7003fe90…`, 23,104 bytes) legitimately
   re-cuts every time the master inventory grows — P2 (fm:invocation +
   fm:foldable) WILL move it. Expected under the pinned-scan law; named
   here so it arrives as duty, not surprise.
4. **(Informational, pre-existing lane)** The `activation/grounds-invalid`
   teaching card rendered a string's actual value exploded into
   characters (`["g","a","t","e"…]`, observed live). It taught; the
   shape is odd. Falsifier: one assertion on that card's actual-value
   rendering. Activation-lane machinery, untouched by this diff.
5. **Teardown residue.** Session A's rig dir (containing the blind env
   symlink) + browser profiles persist under its private session
   scratchpad (`/tmp/claude-1000/…73204110…/scratchpad/`); the
   finisher's deletion was permission-denied by the env-guard. The rig
   SERVER is dead (port 8097 free, finisher-verified); the directory is
   mode-700, owner sid, tmpfs-class. One line clears it:
   `rm -rf /tmp/claude-1000/-mnt-data-projects-Softland/73204110-cb8a-4f9d-af98-aa96ae2213e8/scratchpad`.

## Evaluation notes (D-006 style)

- The cutover-class two-phase cut held: one implementer context built P1
  whole; the in-phase falsifier found the one real design gap
  (composition-order vs static anatomy order), and the repair widened
  the ENGINE generically instead of restoring a block-specific composer
  — the second-wearer law working under pressure, exactly where T1
  would have been cheapest to violate.
- The gate itself fractured across two sessions (context exhaustion
  mid-gate) and the resume held at near-zero cost because session A
  banked MACHINE receipts (drive logs with per-step JSON lines), not
  prose conclusions. Worth keeping: gates that bank raw logs are
  resumable; gates that bank only conclusions are not.

## Next

1. Sid's commit ruling: code commit (the §6-P1 paths, exact-path
   staging, HEAD-guarded) and a SEPARATE docs commit
   (`build/smalltalk-ui-vm/` artifacts + board flip) — never mixed;
   docs branch never pushed; never Co-Authored-By.
2. After the code commit lands: re-run the HEAD-dynamic suites
   (`git_spine_gate_test` + `code_atoms_test`) at committed HEAD.
3. Then P2 opens: fresh implementer builds P2 whole per CONTRACT §6-P2;
   slim gate. P2 owns the episode SHA re-cut and will re-cut the
   entity-mode pin (residue 3).
