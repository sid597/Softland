# machine-cut — pinned lane prompts (dispatch on Sid's word)

Both lanes: fresh Opus 4.8 subagents, one phase per fresh context, disjoint
fences, run in parallel. Each lane boots `/rama` + `/rama-pitfalls` BEFORE
writing any Rama-touching code, reads `build/machine-cut/CONTRACT.md` in
full, and cites MC-T trap numbers in code comments. Gates run to green
in-lane (first-run green is the upstream layers' receipt — if a gate does
not go green first run, say so in the lane artifact). Lane artifacts land in
`build/machine-cut/` (`LANE_A.md` / `LANE_B.md`): gate results RUN
in-context (never claimed), files touched, judgment calls, INT flags.
Neither lane commits; neither lane touches the board (NOW is the
orchestrator's).

---

## Lane A — the driver (`machine_cut.clj`)

You are implementing the machine-cut DRIVER per
`build/machine-cut/CONTRACT.md` §§3–5 (read the whole contract first;
binding). Fence: NEW files only — `src/app/server/rama/machine_cut.clj`,
`test/app/machine_cut_test.clj`, `test/app/fixtures/machine_cut/*`. You may
NOT touch face_projection, file_viewer, any kernel file, or any existing
test. The `:pairs-with` enum line is NOT yours (INT lands it
post-countersign): build against the kind keyword; your IPC gates may launch
the relation kernel only if the enum line is already in the tree — otherwise
pin those gates on a locally-seeded kind from the existing enum
(`:references`) behind ONE named test-only indirection constant, flagged for
INT re-pin. [If the enum line IS in the tree when you start, use
`:pairs-with` directly and delete this indirection.]

Deliverables: pure core (input build + input-hash per §5.1; prompt render +
strict output contract §5.2; validation totality §5.3; pair-plan diff §5.4)
separable from the IPC shell (`annotate-conversation!` §5.7; llm-module ride
§3 with synthetic deterministic ids; WAL-first §5.5; epoch bump §5.6 —
import `util-fns/!ingest-epoch-atom`). Fake adapter (`llm.clj:2179-2184`
`:lines`) for ALL gates; no live LLM anywhere in the suite. Gates: G1, G2,
G3, G5, G6, G7, G8, G9, G11 (+ your share of G16 discipline). Run
`test/app/missionary_claims_test.clj`? No — out of scope; run YOUR namespace
plus `relation_kernel` + `llm` suites you depend on, serially, one JVM.

Platform duties: microbatch append-ack does NOT imply PState visibility —
use materialized-read barriers (`rk/await-relation`,
`llm/await-decision`/`await-run`, verified in-tree). Every "X must NOT have
happened" assertion names a physical reader (validation-only PState reads
exist in both kernels). No raw control bytes in literals. Cite MC-T numbers.

## Lane B — the serve + the face (`face_projection.clj`)

You are implementing the pair-structure SERVE per
`build/machine-cut/CONTRACT.md` §§4.4, 6, 7 (read the whole contract first;
binding). Fence: EDIT `src/app/server/rama/face_projection.clj` (additive
only — `:turns` and every existing key byte-identical; MC-T8) + its test
namespace; NEW pure-core tests + fixtures; NEW pairs-aware face `.edn`
(`resources/public/faces/`, envelope `:assembly/based-on "boxes-face"`,
§7) + its golden under `test/app/fixtures/faces/`. You may NOT touch
machine_cut.clj (does not exist for you), file_viewer, kernels, or the
existing face `.edn` files.

Deliverables: the §4.4 read route (ONE conversation-key
`read-relations-for-targets` call; dedup by relation-id — MC-T14 pinned
with a both-copies fixture; filter to the configured machine-cut actor —
import the constant from... it lives in machine_cut.clj which is not yours:
define the v0 actor constant in face_projection with an INT flag to
re-home it into the shared location INT picks); §6 grouping laws (totality;
after-the-cut derivation MC-T11; conflicts deterministic + counted; foreign
asserters counted never merged MC-T10; `:structure` + pre-composed
`:structure-line`); serve totality (G12 — absent rk-rt / no edges / read
throw → honest `:none`, never a throw). Gates: G4, G10, G12 (seed rk edges
BY HAND via `rk/assert-request` in your IPC fixtures — no driver
dependency; same enum-line caveat as lane A). Face transcription per §7:
zero interpreter/grammar edits expected; if the pair nesting is genuinely
inexpressible, STOP-CLAUSE per §11 — never improvise a grammar change.
Regen of existing goldens is FORBIDDEN (additive keys must not disturb
them; if a golden breaks, your change was not additive — fix the change).

## INT checklist (orchestrating Fable session — after both lanes land)

1. Sid's countersign recorded → land the `:pairs-with` enum line (the ONE
   kernel edit); delete/re-pin both lanes' kind indirections.
2. `file_viewer.cljc` boot: attach rk-rt into face-ctx + WAL replay after
   attach; poisoned-but-total (MC-T12, Amendment C).
3. Shared-constant re-home (the machine-cut actor id): one def, two
   requires.
4. ONE serial suite (all face_* + machine_cut + relation_kernel + llm +
   clojure_adapter), one JVM.
5. G13 receipt (real corpus, real Claude, once) — asserted, not printed;
   mechanical-baseline comparison recorded with every disagreement
   classified.
6. G14 wearing — pair frames live, structure-line silver mark, salted
   re-annotation re-renders WITHOUT re-wear (epoch); op-count parity before
   reading any capture (Amendment D).
7. G15/G16 mechanical scans; falsification-by-class batch; gate review per
   CLAUDE.md protocol; NOW entry; commits on the standing rules (code/docs
   separate; docs auto-committed).
