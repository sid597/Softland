# New Chat Bootstrap

Use this prompt to start a new chat.

```text
We are working in /mnt/data/projects/Softland.

Before answering, read:

1. docs/current-mental-model/README.md
2. docs/current-mental-model/conversation-trail.md
3. docs/current-mental-model/rama-world-kernel-text-instance.md
4. docs/current-mental-model/architecture/action-request-kernel-routing.md
5. docs/current-mental-model/architecture/rama-world-kernel-v0-pr-trail.md
6. docs/current-mental-model/architecture/rama-policy-throughput-post.md
7. docs/current-mental-model/architecture/rama-blog-patterns.md
8. docs/current-mental-model/implementation-review-prompt.md

Ignore older Softland lore unless I explicitly ask for it. Give highest weight to the April 28 current mental model.

The current center:

Projection shows materialized state
  -> user/agent acts on a projected target
  -> helper/API builds an ActionRequest
  -> ActionRequest enters a Rama depot
  -> Rama interpreter/policy derives accepted KernelEvent or rejected ActionDecision
  -> Rama ETL updates materialized state
  -> projection changes

The general kernel is:

event contract
action request contract
action decision contract
target contract
action contract
materialization contract
projection contract
policy contract
distillation contract

The event contract remains the accepted fact contract. The corrected lifecycle is:

ActionRequest asks.
ActionDecision records the answer.
KernelEvent happened.

ActionRequest is not a text/PDF/chat/code instance. It is a lifecycle envelope inside the general kernel.

KernelEvent =
  identity
  + type
  + actor
  + branch
  + context
  + target
  + action
  + payload
  + causality
  + ordering
  + policy
  + provenance

Text-specific, PDF-specific, chat-specific, and code-specific details plug into payload and target/address. They should not change the kernel envelope.

Text is not the ontology. Text is the first carrier/instance used to exercise the kernel.

Do not collapse planes into fake options. Distinguish:

Plane = system-level concern / contract
Instance = concrete carrier, e.g. text/chat/PDF/code
Module = implementation boundary
Walkthrough = one vertical path through all contracts

Current next development move:

Make the physical Rama shape more Ramanian:

ActionRequest gets :routing/key
  -> request depot hashes by routing key, not random/request-id
  -> policy state becomes real PStates
  -> accepted KernelEvent/rejected ActionDecision trail remains intact

Never add Rama I/O just because the code looks simpler. Follow the collaborative editor pattern: local buffer -> semantic edit object/batch -> document-keyed depot -> local transform.

V0 proof covered:

1. Ingest one text artifact.
2. Materialize artifact/text state.
3. Unitize into lines or paragraphs.
4. Accept/reject one unit in a branch.
5. Query canonical and discarded views separately.
6. Preserve provenance back to raw artifact/event.

When answering, do not give word-porn abstractions. Answer in context:

Here is the contract.
Here is the kernel data structure.
Here is how it wires to Rama.
Here is how text instantiates it.
Here is the next implementation boundary.
Here are the open choices and tradeoffs.
```
