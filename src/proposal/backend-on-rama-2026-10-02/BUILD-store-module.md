# Build: the store's module — depots, gates, the record

Cut by: the session "act implementation analysis" (Fable 5.1, effort max), 7 October 2026.
For: one build session. Status: Sid's decisions of 6 and 7 October 2026 are the contract.
Nothing here is a session's choice unless the line says so. The decisions are recorded in
`decisions/DECISIONS.md` (the store's ten) and `src-store/README.md` (the act's twelve).

## What this build is

The first time an act lands as a fact through the form Sid ruled. One Rama module,
`softland.store`, with: two depots, two gates, the record, and the answer by act-id. Its
proof plays the door by hand. When it passes, Softland has done the one transition the whole
medium depends on: an act is accepted and becomes a fact that can be read back.

```
MAKER ─act─► [the proof's hand: act-id · class · lock · seal · append] ─► DEPOT ─► GATE ─► RECORD ─► answer by act-id
```

## Read first, in this order

1. `src-store/CLAUDE.md` — the explanation rules; they bind every file of this build.
2. `src-store/README.md` — the tree, and the act's twelve decisions.
3. `src-store/softland/store/act.clj` — the one form. The only form. Read it whole.
4. `decisions/DECISIONS.md` — the ledger; the store's ten answers are rows F6, Q7, F2, F1, F4, F5, Q22, F7, F8.1, F9a–c.
5. The Rama skill (`/rama`), before any Rama code is read or written.
6. Last, and only as findings: the rig's notes under `src/proposal/rig-2026-09-25/` on what Rama forced —
   refusals as data (an exception in a topology kills the worker); a microbatch is visible only when the
   slowest task is done; plain milliseconds ran ahead under load; the depot's log keeps every appended value.
   No form, no name, no call shape and no default comes from the rig's code. Not "offer", not "by-layer".

## The decisions this build stands on

From the act (6 Oct): the act is the maker's form, ten parts, plain values (`act.clj`). The door adds the
act-id, the class and the seal; those are the door's declaration, which the gate checks on arrival. The
rig is an example. An id is a keyword or a UUID. A permission is `[who layer granted-in]`, ≤4 deep.
A fact is `{:e :k :v :replaces}`; no value = a retract. A stamp an act carries is an integer ≥ 0, < 2^62.

From the store (7 Oct):
- **F6 A** — two depots, one per gate.  **Q7 a** — called the *streaming depot* and the *microbatch depot*.
- **F2 A** — the record is in the gates' PStates. The acts depots are the replay log; the record is
  recomputable from them.
- **F1 A** — the members of a layer set its class, as a fact on the layer.
- **F4 C** — the base is a microbatch layer; every other layer starts streaming.
- **F5 A** — each layer holds a copy of each grammar it uses. (Grammar checks are not in this build; the
  record must leave room for them: a layer holds facts under its own entity.)
- **Q22 A** — the door reads a streaming act's answer from the record by act-id; the ack is a signal.
- **F7 A** — a rule for all code: a signal (an ack, a bell, a promotion's second act) is not the truth; the
  record is; read it by act-id before acting on a signal. A gate reads its own record by act-id before it
  decides, so a repeat is answered, not decided twice.
- **F8 C** — the gate refuses a setting key on an entity that is not the act's layer, as a recorded refusal.
- **F9** — the three limits are targets; the rig's measures are the starting point. Two nodes: later.

Words (Sid's): **act-id** `[layer class uuid]` — not "name". **class** — streaming or microbatch; the word
itself is still Sid's to give; use `class` in code until he does, and nowhere else invent a word for it.
"Key" is a fact's key; "lock" is an encryption key.

## What to build

**The module** `softland.store.module` (one Rama module), with these parts, each with its own namespace
and docstring, and a README per folder as the tree's rules say:

1. **Two depots.** The streaming depot partitions by the act's `:layer`. The microbatch depot partitions
   by one entity derived from the act's facts (decision 10A: derived by the depot). *Session's choice,
   provisional, say so in the README:* the entity of the first fact.
2. **The door's declaration** — a small shape, declared as data in the same style as `act.clj`'s `shapes`
   (reuse its engine by requiring `softland.store.act`): what arrives on a depot is the maker's act plus
   `:act-id`, `:class`, and each value fact's `:v` replaced by `:sealed` bytes and a `:lock-id`. Facts under
   a **store key** keep a plain value. The store keys are the module's, declared in one place with the
   reason for each; begin with only what this build needs: `:class`, `:owner`, `:permission`, `:lease`,
   and any others the build must have — list each, and no more. *Session's choice:* the exact set.
3. **The streaming gate** — a stream topology: one event per act, on the layer's task. Order of its checks,
   each a recorded refusal word, never an exception: (a) the record already holds an answer under this
   act-id → return it, decide nothing (F7); (b) the door's declaration (which contains `act.clj`'s check);
   (c) the layer's class is this gate's; (d) a permission for `:who` in `:layer` exists in the record;
   (e) each `:replaces` is the current fact for its entity and key; (f) no setting key on an entity other
   than the layer (F8); then accept: write the facts, their new versions, and the answer under the act-id,
   with a stamp.
4. **The microbatch gate** — a microbatch topology with the same checks, over a batch, across tasks.
   Decisions of one batch are visible together (the finding). Same record shape.
5. **The record** — PStates, per task: the layer's current facts (`entity → key → {value or sealed, the
   fact-id that wrote it, the stamp}`); answers by act-id (`act-id → {:accepted | :refused reason, stamp}`);
   the layer's settings as facts on its own entity (class, owner); permissions as facts; leased locks.
   Design the PState shapes with the Rama skill's guidance; keep each PState's reason in its docstring.
6. **The stamp** — an integer the gate assigns, strictly increasing on its task. *Session's choice,
   provisional, from a finding:* milliseconds × 2^16 + a counter, so that it never runs ahead of a later
   stamp under load. Below 2^62.
7. **Locks and the seal** — the module leases one lock per (session, layer) on request (a store act under
   `:lease`, or a query — *session's choice, say which and why*) and keeps it in the record so a later
   forget can destroy it. The proof's hand seals values with it. *Session's choice, provisional:*
   AES-GCM. The gate refuses a plain value under a non-store key: `:not-sealed`.
8. **The answer by act-id** — a query topology: act-id → the recorded answer, or nothing yet.
9. **The first layer** — how the base and a person's layer come to exist: an act whose facts set the
   layer's class and owner on the layer's own entity, made by the operator for the base (class microbatch)
   and by a person for their own (class streaming, F4 C). The permission that lets the operator act is the
   bootstrap; *state how it is seeded, as a session's provisional choice, in the README.*

**The proof** `softland.store.module-proof`, on Rama's in-process cluster, run as
`clojure -M:store -m softland.store.module-proof`. It plays the door by hand — makes the act-id, reads the
class, leases a lock, seals, appends, reads the answer by act-id — and must show, each on a SHOWN line:
one act landing through the streaming gate and read back by act-id; one through the microbatch gate; a
refusal recorded with its reason; a repeat (same act-id sent twice) answered from the record, not decided
twice; an unsealed value refused; a stale `replaces` refused; a setting key on another layer refused;
an act with a permission for another layer refused; the base's class microbatch and a new person's layer
streaming. It exits non-zero on a failure.

**Classpath.** `deps.edn`'s `:store` alias is pure Clojure today. Add Rama's dependencies to it (the rig's
`deps.edn` shows which artifacts and versions Rama needs — that is a finding about Rama, not a form) or
make a second alias `:store-module` that extends it; say which and why in `src-store/README.md`.

## Not in this build, by decision

Reads and `resolve` (group 6, unanswered); bells and the feed; promotion (?10); re-class (?9); forget's
destroy step (the lock table must make it possible; the step itself waits); persons and sign-in beyond the
bootstrap (group 5); grammar checks (F5 decided the place, not the check); the door proper — it comes after
this module, as the proof's hand made general. Do not build any of these. Where the record's shape must
leave room for one of them, say so in the docstring and leave the room.

## Explanations, as the tree's rules say

Each namespace and each function has a docstring; each folder a README with a parent link; no function
lists in READMEs; no history of corrections; a part that is not built stays out of the maps. The module's
README gets a section **"Decisions this module stands on"** that lists the ten store decisions by their
ledger row and the act's twelve by pointer, and a section **"Provisional, waiting for Sid"** that lists
every line marked *session's choice* above, with its reason. Nothing provisional is written as his.

## Commits and the receipt

Commit on `main`, grouped by concern (the module's declaration and depots; the gates and the record; the
proof and the maps), each message saying what and why. No push. No `Co-Authored-By`, no `Claude-Session`,
no attribution line of any kind. The receipt: the commit hashes; the proof's run output, in full; every
provisional choice made; every place this contract could not be met as written, and why; `git status --short`
(expect only `src/app/server/README.md` modified and `docs/.obsidian/`, `docs/Untitled.md` untracked —
leave them).

Never read `src/app/server/env.clj` or any `.env` file.
