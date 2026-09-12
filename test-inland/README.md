# Inland verification

[Product source entry](../src-inland/README.md) ·
[Preserved build receipt](../docs/builds/inland/README.md).

This tree owns checks and their temporary evidence, not accepted product behavior.
Choose the modality that can answer the question; a reference recipe interpreter
cannot establish Electric scheduling, and a browser screenshot cannot establish
Rama admission or disposal.

| Immediate child | Responsibility |
|---|---|
| [softland/](softland/README.md) | JVM tests against a fresh two-task Rama IPC, plus pure recipe/closure assertions. |
| [browser.mjs](browser.mjs) | Real canvas/native-keyboard demonstrations, accepted-state observations, narrow-work counters, lifetime and app/browser recovery. Writes captures and JSON under `target/inland/receipts`. |
| [check-docs.py](check-docs.py) | Static hierarchy links, namespace/function coverage and executable equivalence against the pre-documentation revision. Does not boot product code. |

The five focused scenarios and their evidence boundaries are:

| Scenario | JVM coverage | Browser coverage |
|---|---|---|
| Open and change the active instrument | Admission, invalid/duplicate/stale request outcomes | Canvas inspect/edit, pending/rejection, accepted targeting change |
| Keep, share and specialize | Current/version rows, candidate promotion and removal | Named variation, shared then replaced reference, layer/pin behavior |
| Propagate and withdraw narrowly | Addressed proxies, unrelated shape read, independent cancellation | Unchanged headline/3D preparations, 4 → 3 parent reads, 2 → 1 → 0 supports |
| Repeated owned step | Seed step via reference interpreter, finite closure and bounded outcomes | Authored walk, budget exhaustion, cancellation and view-owned work |
| Real ask and explicit uncertainty | Claim/observation ownership, terminal transitions, parser fixtures | Real Claude reply, labelled failure/uncertain controls, initiating-view close and another live view |

The browser driver also replaces the app JVM and browser against the same isolated
Rama disk storage, then retrieves instruments and reply. It does not restart the
whole cluster or prove disk-loss recovery. The focused runner does not call the
provider. Its reference interpreter covers only known seed value/call/read steps;
actual Electric execution is demonstrated in the browser.

Run the existing product checks from the repository root:

```sh
bin/inland check
bin/inland up --test-controls
bin/inland verify
```

`verify` requires the local graphical display, Chrome/WebGPU and normal Claude
CLI authentication. It **makes a new real provider request** and replaces the
owned app during recovery. Test HTTP endpoints only read isolated accepted state,
hold admission or inject labelled faults. The product's transport remains Electric.
After verification, `bin/inland stop-app` followed by `bin/inland serve` returns
to normal mode without discarding data. Launch details belong in the
[handoff](../docs/builds/inland/README.md).

For documentation-only work:

```sh
python3 test-inland/check-docs.py 8494613
```

This compares both Clojure reader branches after removing declaration docstrings,
preserves executable forms including tagged literals, compares Python ASTs without
docstrings, and permits only inserted JavaScript documentation comments. It checks
local Markdown file links and all 19 runtime namespace/141 named function docs,
the JVM helper contracts and launcher function docs. It needs local Clojure and
Python, with no browser, cluster or provider execution. Static checks establish
coverage and unchanged code forms, not the truth of every English explanation.
