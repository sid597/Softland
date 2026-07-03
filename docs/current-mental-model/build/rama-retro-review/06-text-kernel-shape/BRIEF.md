# Brief - Text Kernel Shape

## Review Block

This block reviews the committed kernel-shape and text-kernel split work:

```text
shared kernel shape
  -> text kernel instance
  -> space rename/split
  -> identity headers across kernel modules
```

## Commit Anchors

```text
1c03e70 2026-05-13 kernel: add shape spec and KERNEL-SHAPE data form
1ef1cbd 2026-05-13 rama: split text kernel and rename space
c0dfafe 2026-05-13 docs: update rama space rename context
e751434 2026-05-19 rama: add identity headers to all 5 kernels
```

## Code Files

```text
src/app/server/rama/kernel.clj
src/app/server/rama/text_kernel.clj
src/app/server/rama/core.clj
src/app/server/rama/dogfood/compute.clj
src/app/server/rama/dogfood/space.clj
src/app/server/rama/dogfood/llm.clj
src/app/server/rama/dogfood/transcript.clj
```

## Test Files

```text
test/app/server/rama/text_kernel_test.clj
test/app/server/rama/dogfood_compute_test.clj
test/app/server/rama/dogfood_space_test.clj
test/app/server/rama/dogfood_llm_test.clj
test/app/server/rama/dogfood_transcript_test.clj
```

## Architecture Docs

```text
docs/current-mental-model/architecture/dogfood-runtime/README.md
docs/current-mental-model/architecture/rama-world-kernel-text-instance.md
docs/current-mental-model/architecture/action-request-kernel-routing.md
```

## Main Review Question

Did the split from generic/world text proof into text, space, compute, LLM, and
transcript kernel instances preserve one shared lifecycle contract, or did the
modules drift into parallel local conventions?

## Specific Questions

- Do identity headers describe real module contracts or only comments?
- Are module names, depot names, PState names, and tests aligned after the
  World-to-Space rename?
- Does `KERNEL-SHAPE` reflect the committed modules accurately?
- Are all kernels still request-first where they should be?
- Do text-kernel helpers preserve the ActionRequest/ActionDecision/KernelEvent
  distinction?
- Are transitional compatibility paths isolated enough to avoid becoming the
  real contract?
- Are PState names and read helpers consistent across modules where consistency
  matters?

## Out Of Scope

- Object-container Slice 1.
- In-process chat ingester work.
- New code generation or registry implementation.
