# The store's line, piece 2: what was built, and what was run

[The runtime's page](PROPOSAL.md), entry 6 and "The store's line, to start
from". [Piece 1](BUILD-store-piece-1.md) is beneath it. Its form and its
statements were written before the code: [store-piece-2/SPEC.md](store-piece-2/SPEC.md).

**21 November 2026.** Two Claude sessions built it. The first wrote the spec
and the code and ran the pure proofs (`34fdab0e`); the second ran the cluster
proofs, corrected the docstrings, made the fixture again and wrote this page
([the carry-over between them](store-piece-2/HANDOFF.md)). A session does not
judge its own build: this page says what was built and what a run printed,
for a session that reviews it from the diff.

## What Sid asked for, and what was built

His decisions, on the runtime's page, entry 6: "each act is the smallest
atomic unit right and we can have permission over that"; how much one yes
covers is a spectrum, "per run of some task/query/agent" (‹45c7127b›); and a
holder can hold more than one permission in a layer at once, since one agent
runs as many instances, "each instance can have different permisiion"
(‹95754ddf›).

Built in `src-store`, with their proofs in `test-store`:

- **A permission says what it covers.** A sixth slot of its id:
  `[:run session]` for one run, `[:act uuid digest]` for one act. Without the
  slot it covers the layer until it is ended, so every permission that
  existed keeps its meaning.
- **It sits on what it covers.** On its holder, on the run's session or on
  the act's uuid, each under a store key of its own. So a holder has, beside
  her permission for the layer, one for each of any number of runs and acts,
  and each stands or ends by itself.
- **A gate holds an act to it.** Under a run's permission an act is taken
  only from the permission's holder, in that run. Under an act's permission
  only the one act is taken: the uuid of its act-id and its digest are the
  ones that the permission names.
- **It stands as any permission does.** Piece 1's rule, unchanged: the end of
  a holder's permission for the layer ends every permission that she gave
  beneath it, for a layer, a run or an act.
- **A read can name its run.** A query carries `:session`, and an asker may
  see a layer that is open to its members under her permission for the layer
  or under the permission of the run that she asks in.

## A session's choices, each his to turn

Asked whether a yes for one act covers the act's id or the exact act, he said
that this is the product's question (‹4eaf571e›). So the forms were taken as
defaults and built, and each is marked in the code as a session's:

| Choice | Where it is made | The other way |
|---|---|---|
| What a permission covers is a sixth slot of its id | `act.clj`, the forms of a permission id; `covers` | A record of its own beside the permission |
| A yes for one act holds the act's content: its digest, without the time on it and without its `:permission` | `door/digest.clj`, `hex-for-its-permission` | The act-id alone: any act sent under that id is taken |
| A permission for an act says `:see-and-act`, never `:see` | `act.clj`, the shape `:permission-id` | None that allows anything: a permission for one act lets nobody see |
| Three store keys, one for each thing covered; the words `run-permission` and `act-permission` | `door/declaration.clj`, `store-keys` | One key: then a run whose session has a holder's id lands on that holder's place |
| A permission for a run or for an act gives nothing (R10) | `gate/decision.clj`, `permission-fact-fault` | A run that may give, as one more word in the sixth slot |
| A run ends no permission of its holder but its own; she ends any of hers under her permission for the layer (R5) | `gate/decision.clj`, `end-fault` | Any permission of hers ends any other of hers |
| A permission for one act lets nobody see (R8) | `read_path/layers.clj`, `either` | A yes for an act that also shows the layer |
| The read path takes the run from the caller, as it takes the asker | `read_path/query.clj`, `:session` | Nothing else until sign-in is built: the runtime will hold both against the caller |
| A holder still has one permission *for the layer* that stands at a time (S6) | `gate/decision.clj`, `give-over-fault` | "See" from one giver and "see and act" from another at once |
| The words of the six new refusals | `gate/decision.clj`, `standing`, `permission-fact-fault`; `door/declaration.clj`, the rules | Any words: they are data in an answer |

## What it costs, read from the code and not measured

- An act under a run's permission reads what an act under a given permission
  for the layer reads: the run's row where the holder's was.
- An act under a permission for one act costs one SHA-256 more over the
  thing. No other act pays for that hash.
- A read that names its run holds, in each layer that is open to its members,
  one row more and the rows above it: one push each when it is kept live, and
  no call more when it is asked once.

## What was run

All seven proofs of the store, on the final code, one after the other behind
the machine's cluster lock, on 21 November 2026 from 23:13 IST, after the
fixture was made again:

| Proof | Result |
|---|---|
| `act-proof` | 166 checks, none failed |
| `pure-proof` | 724, none failed |
| `module-proof`, in-process cluster | 475, none failed |
| `door-proof`, in-process cluster | 277, none failed |
| `read-path-pure-proof` | 223, none failed |
| `read-path-proof`, in-process cluster | 583, none failed |
| `run-again-proof`, in-process cluster | 48, none failed |

The four cluster proofs had never run on this piece's code. Their first run,
at 23:08 IST, passed with the same counts and needed no repair of the code or
of the proofs. The logs of both runs are kept outside the repository, under
`/mnt/data/projects/research/softland/store-line-2026-11-21/runs/`.

What they hold of this piece, each by its own sentences in a run:

- the gate's rules for a run and for an act on rows that the proof builds
  (`pure-proof`);
- a give for a run and for an act, the act in its run and in another, a give
  under a run's permission, an end by the giver and by the run itself, a
  second give, the permitted act and the same act-id with another value:
  through both gates on a cluster, in a streaming layer and in the base in
  one batch (`module-proof`, the groups `a-permission-for-a-run-and-for-an-act`
  and `a-permission-for-a-run-and-for-an-act-in-the-base`);
- a read asked once in a run, in no run, in another run and by another who in
  that run; and a live read that leaves the layer when its run's permission
  ends and takes it back when the run is given one again
  (`read-path-proof`, the group `a-read-asked-in-a-run`; every order of the
  callbacks in `read-path-pure-proof`);
- a line of ten things under permissions for a run and for an act, in each
  gate, comes out the same when the gates are run again over the depots, and
  from kept acts on 4 tasks and on 8 (`run-again-proof`).

**Another session's check, which this session ran**
([gate-histories/](gate-histories/README.md), as it stands uncommitted in the
working tree, with that session's statements R1 to R10): 20,000 generated
histories, seed 20261121, 865,275 decisions, through the gate's pure code.
The gate took a thing exactly when the statements did.

- That check's own adapter to the read path no longer fitted the read path,
  so its run turned the statement `see` off, and its two mutants of a read
  were not caught. That folder is another session's, so its files were not
  changed: [store-piece-2/see_check.clj](store-piece-2/see_check.clj) loads
  its code and puts a fitting adapter in place for the run. The statements
  stay the other session's own, which already took a run.
- With it: the same 20,000 histories hold `agree`, `stamps`, `kept`, `once`,
  `see` and `run-again`, with `see` asked for each asker in no run and in
  each run.
- Twelve mutants, twelve caught, each within 110 histories: that session's
  ten, and two that this session added, for R8 (a run's permission lets any
  asker in that run see) and R10 (a permission for a run gives).

**The fixture was made again**, on purpose, for the rules above. The store
with the new rules gave the fourth making's tables from the fourth making's
records, since those records held no permission for a run or for an act: the
run-again proof passed on the fourth making before this one replaced it. It
was made again so that the kept acts hold both. Its page says the rest
(`test-store/softland/store/run_again_fixture/README.md`, the fifth making).

## What no run reached

- A cluster of several machines, and a killed process: each cluster was
  Rama's in-process one.
- A permission for a run or for an act that was given before the module is
  started again, held after it; and one in a batch or an event that fails and
  runs again.
- What the digest for a permission of one act costs on a large act: the
  groups send acts of one small fact.
- On a cluster, a read asked in a run through the base, and through more
  than one layer: the group reads one streaming layer.
- In the generated histories: the base and the microbatch gate, a fact that
  names something to replace, and what a read gives beyond whether the layer
  is seen.

`test-store/softland/store/README.md` says the same, proof by proof.

## Beyond the ask

Listed from the diff of the two commits, not from memory.

- `softland.store.act` gained the leaf `:digest-hex` and `covers`.
- `softland.store.door.declaration` gained `permission-keys` and two rules,
  that a permission fact is under the key of its kind and sits on what it
  covers.
- `softland.store.read-path.layers` gained `run-permission-state` and
  `either`; `standing` and `meaning` now take whether she holds a permission
  that stands, where they took her row and the rows above it.
  `softland.store.read-path.sources` gained `run-permission-source` and
  `permission-state`.
- A query has one rule more: a `:session` only with an `:asker`.
- Docstrings that still said the old thing were corrected with the code:
  the walk's cost and its step 5, the gate's list of checks and check (d),
  the rows' map of keys and `ended?`, the read path's "What it follows" and
  both "What it costs", the declaration's `store-keys`. One was stale since
  piece 1: `read_path/layers.clj` still named who may write a setting as not
  decided.
- The store's pages were brought up to the code: `MODEL.md`, the maps of
  `softland/store/`, `gate/`, `door/`, `read_path/` and of the proofs, and
  entries 4 to 6 of "Decisions of 20 and 21 November" in
  `src-store/README.md`, on his word "3.yes" (‹95754ddf›).

## For Sid to turn, or to pass on

- The choices in the table above.
- `see_check.clj` belongs in `gate-histories/run.clj`. The session that owns
  that folder (`softland-1f` in the carry-over) could not be reached from
  this one, and its three files are still uncommitted in the working tree.
