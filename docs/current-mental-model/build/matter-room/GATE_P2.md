# matter-room — P2 slim gate

2026-07-28 · fresh Fable gate over the final uncommitted P2 tree · base
`af527d9` / P1 code `7a3127c`.

## Verdict

**PASS — P2 is closed at the slim tier.**

G3, G4, G5, and G10-machine are green. The one fresh falsifier found a real
fail-open edit-sequence defect; the implementer corrected it, the falsifier
replayed its exact attacks, and the final post-falsifier suite is green.
P3 is not opened by this verdict. Staging and commit remain Sid's decision.

## Scope and fence

- HEAD reverified at close: `af527d9`.
- Final code/test diff: exactly the seven PLAN §P2 paths named in `P2.md`.
- The inherited off-plan `material_portal.clj` and
  `shared/material_portal.cljc` changes were read, then restored exactly.
- `ground.cljs` required no edit because entry rides the existing drill UUID
  lane.
- `cascade.clj`, `binding_material.cljc`, and the restored portal pair are
  zero-diff.
- Foreign multi-cascade, decisions, session, LOG, and block-anatomy paths were
  untouched.
- `env.clj` was never read.
- Empty index; no stage, commit, or push.

## Independent gate evidence

### Focused post-falsifier suite

Required runner form:

```sh
clojure -M:test -e "(require 'clojure.test ...)(clojure.test/run-tests ...)"
```

Namespaces:

```text
app.material-portal-test
app.server.episode-test
app.face-arsenal-test
app.face-projection-test
app.face-integration-test
app.provenance-material-test
app.reply-to-block-test
app.space-material-test
```

Result:

```text
82 tests · 1,201 assertions · 0 failures · 0 errors
```

This sum reconciles with the component receipts:

- material portal, including P2 physical/runtime tests: 23 / 441;
- episode default and machine parameterization: 18 / 110;
- compatibility/source-surface set: 41 / 650.

CLJS compile: 272 files, 0 compiled, 0 warnings, 1.21s.

### G3

PASS.

- IPC suite: repeat opens converge on stable ids and one physical native row
  per resident.
- Fresh falsifier: four simultaneous first opens yielded four intended
  residents, four physical rows, four distinct import keys, all physical units
  present, and a post-race reopen entirely `:unchanged`.
- Durable-cluster one-shot: accepted on the inherited verbatim pre-shutdown
  receipt—first open all `:birth/:accepted`, second all `:unchanged`. The
  shared conductor was unavailable during this gate and restarting it was
  prohibited.
- Independent current product-path reproduction: temporary `:8092` headless
  drive with isolated runtimes produced fresh birth then unchanged convergence.

The distinction is explicit: the durable one-shot was not re-run; every
post-package invariant was independently re-proved.

### G4

PASS.

The site-matched drill proves machine resident and Sid block behavior against
their corresponding ordinary ground block sites, including fold and
instance-tier behavior. Camera reservation remains on the space facet. No
binding enum or cascade edit exists.

### G5

PASS.

- Birth calls the parameterized existing episode import builder.
- No new adapter, import family, request builder, topology, PState, or
  `extract-object-key` branch.
- Tree-wide import-builder census remains the pre-existing eight owners.
- Physical served speaker is `softland:matter-room`, never `"sid"`;
  unit kind is the whole-block `:material-part`.
- Fresh changed-payload/same-import attack was rejected with
  `:idempotency/material-fingerprint-conflict`.

### G10-machine

PASS.

Isolated fresh-profile headless Chrome against temporary `:8092`, with the
room's endpoint and portal serve live:

```text
portal errors=[] · found=true · room residents=3 · relation roundtrips=1
echo n=43 · p50=14.7ms · p95=21.8ms · p99=53.2ms · max=53.2ms
bar: p95 < 52ms
```

The temp server was stopped; no listener remains on `:8080` or `:8092`. The
shared server/cluster was not started, stopped, or modified.

## Fresh falsifier disposition

Exactly one fresh falsifier was used.

Initial verdict: FAIL on one seam. A durable edit-order read exception was
collapsed to nil, resetting the proposed sequence to 1. With prior sequences
1 and 2, the old deterministic sequence-1 request replayed as accepted while
the new bytes did not land.

Correction: remove the catch-all and fail closed; retain only genuine
nil-row → sequence 1. Add the read-exception regression.

Required replay by the same falsifier:

- exact sequence counterexample: PASS, exception now propagates;
- four-concurrent-open physical attack: PASS;
- changed-fingerprint/same-import physical attack: PASS.

Final falsifier verdict: **PASS**.

## Full-diff and trap spot-checks

- Complete final code/test diff read; `git diff --check` clean.
- Diff stat: 7 files, 1,024 insertions, 33 deletions.
- Off-plan portal pair and forbidden source pair: exact zero diff.
- Import composer census: unchanged eight-file owner set.
- Episode zero-arity/default request behavior pinned green.
- Face projection remains read-only and passes its arsenal source scan.
- Experience widens through one `experience-around-many` call; resident
  membership comes from the existing episode projection API.
- Portal card set remains closed; room is one row inside identity.
- Resident trail ids/bytes remain append-only; head refresh preserves unit id.
- Root `.mr-p2-*.js` probes removed.
- `clj-kondo` unavailable on PATH; compilation and executable source-surface
  tests are green. Full repository suite not attempted under the slim-tier
  contract.

## Receipt coherence

`P2.md`, this gate, source/tests, live output, and the final diff agree on:

- seven phase code/test paths;
- legal actor type `:agent`, machine identity carried by the non-Sid actor id;
- three residents in the live registered-master room (head, bindings, one
  current trail resident) and four in the falsifier fixture;
- one batched relation roundtrip;
- G10 p95 21.8ms;
- final suite 82 tests / 1,201 assertions;
- fail-closed sequence correction and exact falsifier replay;
- no commit/push and no shared-cluster action.

No receipt inflates the headless zero-block visual census into a G5 proof;
classification is proven through physical rows and the served conversation.
No receipt claims the durable one-shot was re-run after the conductor became
unavailable.

## Close

P2 is proven at CONTRACT §8's slim tier and may be committed only by Sid's
ruling. P3 must begin in a fresh context from the starter in `P2.md`; this PASS
does not authorize it.
