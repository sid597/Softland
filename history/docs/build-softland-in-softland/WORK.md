# Implementation notes

Owner: Codex primary. Authority: BUILD.md, MODEL.md and Sid's 2026-09-11 direct request.
Initial preservation commits: `5450ac0`, `2c1047d`, `a484881`; clean main afterward.

- [x] Preserve all pending work; update destination and Electric authorization. DONE WHEN clean baseline and accurate brief/decision record exist.
- [x] Build accepted facts, revisions, layers, admission and indexed demand. SOURCE: execution invariants. DONE WHEN rejection, duplicate requests, pins and promotion run through isolated Rama.
- [x] Build tracked Electric recipes, applicability and authored presentation/gestures. SOURCE: encounter and demonstrations 1–3. DONE WHEN the active instrument and its editor can be changed through accepted definitions, with narrow reads and owned disposal.
- [x] Build owned repeated recipes and external activities. SOURCE: activities and demonstrations 4–5. DONE WHEN authored traversal handles a cycle/budget/cancellation and a real bounded provider result is accepted with uncertain outcomes preserved.
- [x] Drive all five browser demonstrations, fix in-scope defects and capture three states. SOURCE: handoff. DONE WHEN runnable commands and actual receipts distinguish execution from remaining claims.
- [x] Freeze source, run the focused close bundle with one receipt runner, commit and write the final NOW/board receipt. SOURCE: repository bootstrap. DONE WHEN handoff points to committed source and running isolated product. Close receipt: 5 tests / 72 assertions and 7 browser/recovery checks passed; the final commit and normal launch are recorded in NOW.md.

Implementation choices: `src-inland/softland/inland` holds new machinery; existing
renderers get only required resource-disposal hooks. `.inland-runtime` owns the
isolated Rama storage and processes (2217 ZooKeeper, 1997 conductor, 8917 cluster
UI, 35970 supervisor, 8127 product; all checked available before launch).
Session cells are individually reactive, view-owned, disposable atoms, never a
mirror of accepted rows. Draft input is local. Accepted rows and retained
revisions are Rama-owned, keyed by workspace/layer/name; references resolve live
through ordered layers unless explicitly pinned. A tombstone hides a lower
value; deleting an override reveals it. Promotion uses base revision preconditions.
Requests have UUID identities and expected revisions; identity reuse with a
different envelope is rejected. The local server assigns actor identity, not
browser-supplied authority. This is a loopback single-user product, with independent
browser sessions, not an internet authentication product.

Read outcomes are value/absent/pending/failed/forbidden. Only complete absence
satisfies negation. Recipes are bounded named steps, explicit bindings, addressed
reads and named calls; no eval or whole-world evaluator. Repetition belongs to
an owner with state, basis, budget, yield and cancellation. Definitions stay
durable when no view demands them. Event execution snapshots its accepted basis
once and never replays on rule edits. Maintained results keep support identities.
External calls execute after accepted intent, under a single process owner and
durable claim. Interrupted running intents become unconfirmed; no automatic retry.

The requested single continuous implementation supersedes the Rama skill's
separate planning-session stops. Its partition, retry, ownership, admission and
product-path checks remain applied here. No new contract series is opened.

Pre-registered close bundle: `bin/inland check`, `bin/inland verify`; captures
initial/opened/reused instrument, with browser error and actual GPU adapter report.
The implementation pass includes the adversarial seam checks before source freeze.

Implementation seam receipt: all five browser demonstrations plus fresh browser/app
JVM recovery passed against isolated disk-backed Rama. The complete reply access
and preview repair passed through the same native record editor before source freeze. The focused suite
has 5 tests / 72 assertions; the latest Electric build has 0 warnings. No native
provider reply was substituted. Earlier unsuccessful real calls remain durable.

Close handoff requirements: preserve exactly three final rendered states plus
`browser.json` and the focused-suite receipt; report actual counts/statuses from
those files. Keep the authored preview's ellipsis and full-record access. Restart
the app without test controls after close verification, preserving the cluster.
Final mutations are the <=15-line NOW receipt and this package's board pointer.
No source/test/tool changes follow NOW. The broad ECS/host adoption questions remain
unsettled; use HANDOFF.md's bounded composition/floor findings, not new direction.

Candidate source freeze: `ed93ec5` (following lifetime commit `933dc73`). The full
pre-freeze browser pass succeeded, including full reply access and browser/app
restart recovery. A final diagnostic field records the activity's status when
the initiating view closes; it changes no product behavior. The fresh close
primary owns only the registered receipt, evidence preservation, factual handoff
completion and final NOW/board/doc commit. No further source review is requested.

## Documentation repair authorized by Sid

Owner: Codex primary. Source: Sid's request to make this build understandable and
questionable across sessions, using the hierarchy in `src/app/client/AGENTS.md`.

- [x] Add folder maps and repair entry links. Done when a reader can enter from the client or handoff, choose runtime/material/evidence, and descend by responsibility without loading every file.
- [x] Document each runtime namespace and named function, plus launcher/test helper contracts. Done when inputs, effects, ownership, preconditions and non-obvious limits are beside their implementation; existing useful explanations are preserved.
- [x] Check navigation, documentation coverage and executable equivalence to `8494613`. Done when Clojure/CLJS forms in both reader branches and Python/JS execution remain unchanged, links resolve, and selected causal paths agree with source.
- [x] Commit the documentation, finish the short close receipt and push main. Source: Sid's subsequent "do this then git push" authorizes the push over earlier no-push guidance. Local close documents and receipt are complete; the normal push is requested and is confirmed after this commit. Done when the board/handoff enter the maps, evidence and product are preserved, and origin/main contains the close commit. Never merge.

The original build close did not establish documentation completeness. This is
a documentation repair, not authorization to change application behavior. The
registered close check for this repair is static documentation coverage/link and
executable-form equivalence; no provider call, cluster mutation or browser restart
is required to establish those claims. Native code still has the same limits as
the demonstrated build; descriptions must not turn intended behavior into proof.

Documentation seam check: event snapshot → request slot → Rama decision, addressed
read → ProxyState cancellation, and view-owned Run versus process-owned execute!
were compared with their source windows. The maps identify single-slot admission,
non-awaited repeated admissions, answer-definition support ids, one scene slot,
and the override delete/recreate revision gap. These are static limits, not new
runtime failure demonstrations or approval to alter the intended model.

Documentation candidate close bundle (read-only):
`python3 test-inland/check-docs.py 8494613` and `git diff --check`.
The primary pass reports 19 runtime namespaces / 141 function docs, 6 JVM helpers,
15 launcher functions and 22 browser helpers; 21 Clojure/CLJS/EDN files have equal
forms in both reader branches, Python AST and JavaScript body are unchanged.
Seven folder maps and 75 local links passed. The installed Electric defn macro
explicitly carries doc metadata and strips the leading string before its body;
source checked through the resolved classpath resource, no dependency modified.

After candidate documentation freeze, the fresh close primary has only this
close scope: run the registered bundle through one fresh receipt_runner, preserve
a bounded documentation receipt, complete this checklist, update the <=15-line
NOW and only this package's board pointer last, commit those close documents,
and push main to origin/main as Sid explicitly requested. No product/source/test/
launcher changes, cluster actions or provider calls follow freeze. Read-only push
checks may continue; never merge or read the forbidden env file. A push failure
must be reported accurately rather than hidden by a completion claim.
