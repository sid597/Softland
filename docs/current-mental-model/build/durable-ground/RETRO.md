# durable-ground — RETRO

2026-07-17 · Fable · written at close from the full trail (NOW + CONTRACT +
GATE + source + this session's live drills). Adversarial recheck of this
retro: NOT run — Sid's cost call, per standing practice.

## Shape of the package

One day, one orchestrating Fable session-chain (P0→close), zero cheaper-model
phases — Sid's "first light asap" priced the handoff ceremony out and the
work was ops-heavy (a class the phase pipeline wasn't built for: most
"phases" were drills against a live system, not code authoring). Roughly:
~500 lines of new code (cluster.clj + bin/land + ops + build task), 4 seam
edits, zero new Rama organs, and a day of drills.

## QC-layer scorecard

- **Contract (P0 inventory + traps ledger)**: earned its keep twice — the §3
  two-cluster unification finding (a REAL accidental split healed by design
  rather than discovered in production), and T8's replay-order rule keeping
  migration correct. T1's "devZookeeper persistence unverified" was answered
  by drills (survives fine). The ledger MISSED the whole shutdown-state
  machine (T11) — no static layer saw it.
- **Executable drills (the package's wearing)**: the star layer. Found BOTH
  real platform traps: the interrupted-shutdown resume (P5b) and
  daemons-exit-on-completed-shutdown (P6) — each by breaking live, neither
  visible in docs or code. Also produced the honest availability finding
  (Electric session death on in-flight foreign failure) that no suite
  could see.
- **Suites**: guarded the seams (flip-hazard sweep found zero test-tree
  couplings BEFORE the flip — that absence-of-coupling check is what made
  the flip safe to do at close). Caught nothing new themselves; that is
  the expected shape when no organ logic changed.
- **Fresh-context layers**: none used. Honest note: the P5a "same-unit
  text" claim was FIRST asserted from client-side snapshots that turned out
  to be a stale disconnected session's — the cluster-side read then gave the
  true (and stronger) receipt. An author-blind layer wasn't what caught it;
  distrust of a too-clean receipt was. The rule that generalizes: a client
  receipt across a failure drill is invalid until a server-side read
  confirms it (the session that renders the receipt may itself be a
  casualty of the drill).

## What the next contract should do differently (each from a concrete failure here)

1. **Ops scripts that gate on external state machines wait on the TERMINAL
   marker, never on a sleep or a CLI exit code** — and every wait-loop in a
   `set -e` script uses if-forms (a no-match `grep -q` aborted the first
   backup run silently). Both from P6's backup failure + P5b's resumed
   shutdown.
2. **Failure-drill receipts are server-read receipts.** Client-side
   snapshots may be frozen state from a session the drill killed (P5a).
3. **/tmp probe rigs are disposable; the RECIPE lives in the thread file.**
   A tmpfiles sweep deleted both sessions' scratchpads mid-close (scripts,
   raw EDN results, screenshots). Nothing evidentiary was lost ONLY because
   every number had been banked into the conversation/NOW as it landed.
   Bank receipts at capture time, always; and the reusable probe-rig facts
   (CDP flags, the panel-boots-open trap, the cljs-interop shape) belong in
   NOW, not in the scripts.
4. **Size guesses about durable state get measured at first contact** — the
   contract said backup "MBs at current scale"; reality is 1.6GB quiesced /
   3.0GB live. Harmless here, but a vault-rsync cadence decision would have
   been made on a 1000x-wrong number.

## Mechanisms that earned their keep

- The traps ledger cited by number in code and receipts (T4/T5/T6/T7/T9 all
  enforced themselves at least once this session).
- memo-total (total-with-retry) over delays — T6's both-halves drill is the
  proof the design choice was load-bearing, not style.
- bin/land as the ONE home for daemon mechanics — every trap fix landed in
  exactly one file.
- Drills as gates (this package's substitute for the wearing layer) — see
  scorecard; they found everything that mattered.

## Residue (carries forward; owners noted)

- **G5(c) machine-reboot drill — Sid's act**; the drill card is in the close
  handoff. Until it runs, "survives machine reboot" is derived (RocksDB +
  completed-shutdown semantics + G6), not observed.
- **Electric availability under worker failure** → first-light contract
  candidate: absorb in-flight foreign failures without killing the session;
  reconnect faster than the 30s pong timeout. (GATE doubt 1.)
- machine-cut live-annotate WAL ON (sanctioned; retires with that organ's
  own slice). util-fns text-kernel IPC delay remains action-driven outside
  §4 scope. Default-conversation literal duplicated in cluster.clj until the
  IPC branch retires. spine-run-id is a stable literal — delete the cursor
  file if the cluster is ever rebuilt empty.
- `data-restore-scratch/` (1.6GB) left on disk — the permission layer
  blocked `rm -rf`; Sid deletes in one line.
- Sid's vault leg: rsync `backups/` to the Mac when it's awake (recipe in
  bin/land backup output; cadence = his habit call).
- After Sid's CODE commit lands: re-run the git-HEAD-reading suites
  (git-spine pair) — the close rule from code-atom applies to whoever moves
  HEAD.
