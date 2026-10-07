# Build: the store's module — depots, gates, the record

Cut by: the session "act implementation analysis" (Fable 5.1, effort max), 7 October 2026.
Amended by: the build session "softland-11" (Opus 5.5, effort max), 8 October 2026, to Sid's answers of
that day. Each amended line follows `src-store/README.md`, "Decisions of 8 October 2026".
For: one build, run by the Rama skill's stages (see "How the build runs"). Status: Sid's decisions of 6, 7
and 8 October 2026 are the contract.
The decisions, with Sid's reasons, are in `src-store/README.md`: the act's twelve (6 October), the store's
ten (7 October), and who acts and what the store reads (8 October). Nothing is left to ask before the
build. One row is open and is built to a working answer: H1, a lock for each value (item 7). Everything else
a builder chooses is engineering, written with its reason in a docstring, never as a decision. The rules that
the build session holds the build to are under "The builder's rules".

## What this build is

The first time an act lands as a fact through the form Sid ruled. One Rama module,
`softland.store`, with: two depots, two gates, the record, and the answer by act-id. Its
proof plays the door by hand. When it passes, Softland has done the one transition the whole
medium depends on: an act is accepted and becomes a fact in the record, and its answer is read back by
act-id. Reading a fact back out is the read side; it is not in this build.

```
MAKER ─act─► [the proof's hand: act-id · class · lock · seal · append] ─► DEPOT ─► GATE ─► RECORD ─► answer by act-id
```

## Read first, in this order

1. `src-store/CLAUDE.md` — the explanation rules; they bind every file of this build.
2. `src-store/README.md` — the tree, and the act's twelve decisions.
3. `src-store/softland/store/act.clj` — the one form. The only form. Read it whole.
4. `src-store/README.md`, "Decisions of 7 October 2026 — the store's module" and "Decisions of 8 October
   2026 — who acts, and what the store reads" — the store's decisions, with reasons.
   `decisions/OPEN-QUESTIONS.md` is the queue of what is not decided; answers never live there.
5. The Rama skill (`/rama`), before any Rama code is read or written.
6. Last, and only as findings: the rig's notes under `src/proposal/rig-2026-09-25/` on what Rama forced —
   refusals as data (an exception in a topology kills the worker); a microbatch is visible only when the
   slowest task is done; plain milliseconds ran ahead under load; the depot's log keeps every appended value.
   No form, no name, no call shape and no default comes from the rig's code. Not "offer", not "by-layer",
   not "lease".

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

From who acts, and what the store reads (8 Oct):
- **E1·H8 A** — the system stands at the top of the base's chain. In code the system is `:system` and the
  base is `:base`; both words are Sid's ("system", "the base").
- **K2** — a signed-in user has a personal layer and a permission in it without asking: the system makes
  both at the first sign-in. The person who creates a group makes the group's layer. Each layer is made by
  one act, its first (the builder's part).
- **E3·H7 B, E4 A, E2 A** — permission is a hierarchy that ties back to one person. The top of a chain is a
  person, or the system for the base, and never a session. Each actor acts under its own permission. Whoever
  holds a permission can give one beneath it.
- **H2 A** — a stamp holds the clock time and a counter.
- **K1 A** — the store's keys in this build are three: the class, a permission, the door's request for locks.
  No layer names an owner as a fact.
- **H1** — open. Working answer: a lock for each value.

Words (Sid's): **act-id** `[layer class uuid]` — not "name". **class** — streaming or microbatch; the word
itself is still Sid's to give; use `class` in code until he does, and nowhere else invent a word for it.
"Key" is a fact's key; "lock" is an encryption key. A word that the build has to make says in plain words
what the thing is, and is listed for Sid (builder's rule 8).

## What to build

**The module** `softland.store.module` (one Rama module), with these parts, each with its own namespace
and docstring, and a README per folder as the tree's rules say:

1. **Two depots.** The streaming depot partitions by the act's `:layer`. The microbatch depot partitions
   by one entity derived from the act's facts (decision 10A: derived by the depot). *Engineering choice, the
   builder's, reason in the docstring:* the entity of the first fact.
2. **The door's declaration** — a small shape, declared as data in the same style as `act.clj`'s `shapes`
   (reuse its engine by requiring `softland.store.act`): what arrives on a depot is the maker's act plus
   `:act-id`, `:class`, and each value fact's `:v` replaced by `:sealed` bytes and a `:lock-id`. Facts under
   a **store key** keep a plain value. The store keys are three (K1 A), declared in one place with the
   reason for each: `:class`, `:permission`, `:lock-request`. Not an owner. `:class` is a setting: its fact
   sits on the layer's own entity (F8). `:lock-request` is a session's plain word, Sid's to change.
3. **The streaming gate** — a stream topology: one event per act, on the layer's task. Order of its checks,
   each a recorded refusal word, never an exception: (a) the record already holds an answer under this
   act-id → return it, decide nothing (F7; builder's rule 1); (b) the door's declaration (which contains
   `act.clj`'s check); (c) the layer's class is this gate's; (d) the act's permission stands: the record
   holds it, each permission above it in its chain stands, and its holder is the act's `:who` (E4 A) — or
   the act is the layer's first act (item 9); (e) each `:replaces` is the current fact for its entity and
   key (builder's rules 2 and 3); (f) no setting key on an entity other than the layer (F8); (g) a fact
   under `:permission` writes a permission beneath the one that the act itself acts under, in the act's
   layer, and nothing else (E2 A); then accept: write the facts, their new versions, and the answer under
   the act-id, with a stamp. Not decided, so refused in this build with a word that says so: a retract of
   a permission (who may end one) and a change of a layer's class (re-class, ?9).
4. **The microbatch gate** — a microbatch topology with the same checks, over a batch, across tasks.
   Decisions of one batch are visible together (the finding). Same record shape. Builder's rule 6 holds it.
5. **The record** — PStates, per task: the layer's current facts (`entity → key → {value or sealed, the
   fact-id that wrote it, the stamp}`); answers by act-id (`act-id → {:accepted | :refused reason, stamp}`,
   with the digest of builder's rule 1); the layer's settings as facts on its own entity (the class);
   permissions as facts; the locks.
   Design the PState shapes with the Rama skill's guidance; keep each PState's reason in its docstring.
6. **The stamp** — an integer the gate assigns, strictly increasing on its task, later than each stamp that
   the act carries, below 2^62. It holds the clock time and a counter (H2 A). The finding that plain
   milliseconds ran ahead under load is why the two are in one integer; how they are packed is the
   builder's.
7. **Locks and the seal** — the working answer for the open row H1, from ruling 7 of the rulings file
   (R:91–100): every value gets its own small lock at write (a layer may later switch to one lock per act),
   lock rows co-located with their values in the record, so a later forget destroys a lock and that value
   is gone. The module makes locks on request. The request is an act with a fact under `:lock-request`: in
   Rama a query cannot write a PState, and both depots are for acts. A request may ask for several locks
   at once, ahead of need, so that sealing a value does not wait on the store; how a lock that was asked
   for ahead comes to sit beside its value is the plan's. The door reads a lock's bytes by a query. The
   proof's hand seals each value with its own lock. The gate refuses a plain value under a non-store key
   (`:not-sealed`), a sealed value whose lock the store did not make for this layer, and a second value
   under a lock that already seals one. Not in this build, room left: wrapping a lock under the locks of
   the people the value is about, and the per-act grain switch. *Engineering choice:* AES-GCM.
8. **The answer by act-id** — a query topology: act-id → the recorded answer, or nothing yet.
9. **The first act of a layer** — a layer is made by its first act (K2; the form is the builder's). A gate
   accepts an act into a layer for which the record holds nothing when it is a making act: it writes the
   layer's class and one permission, the layer's first, with nothing above it, and it names that permission
   as the one it acts under. The base, `:base`: class microbatch (F4 C); the first permission is the
   system's, `[:system :base :base]`; the act's `:who` is `:system` (E1·H8 A). Any other layer: class
   streaming (F4 C); the act's `:who` is the holder of the first permission (a group's layer, made by the
   person who creates the group) or `:system` (a personal layer, made for its person at the first
   sign-in). Sign-in is not built: the proof sends these acts by hand. Any other act into a layer that is
   not made is refused.

**The proof** `softland.store.module-proof`, on Rama's in-process cluster, run as
`clojure -M:store -m softland.store.module-proof` (or under the alias that the builder makes, see
Classpath). It plays the door by hand — makes the act-id, reads the class, asks for locks, seals, appends,
reads the answer by act-id — and must show, each on a SHOWN line:
the base made by the system, with the class microbatch; a person's layer made by the system for that
person, with the class streaming; a group's layer made by the person who creates it; an act into a layer
that is not made refused;
one act landing through the streaming gate and read back by act-id; one through the microbatch gate; a
refusal recorded with its reason; a repeat (same act-id sent twice) answered from the record, not decided
twice; a second, different act under a used act-id told so; an unsealed value refused; a stale `replaces`
refused; a setting key on another layer refused; an act with a permission for another layer refused; an act
under a permission that does not stand refused; a permission given beneath another by its holder, and an
act under it accepted; a permission given by one who does not hold the one above refused.
It exits non-zero on a failure.

**Classpath.** `deps.edn`'s `:store` alias is pure Clojure today. Add Rama's dependencies to it (the rig's
`deps.edn` shows which artifacts and versions Rama needs — that is a finding about Rama, not a form) or
make a second alias `:store-module` that extends it; say which and why in `src-store/README.md`.

## The builder's rules

These are the build session's, not Sid's. Each has its source. The plan decides how each is met.

1. A second, different act under a used act-id must not read back the first act's answer as its own. Each
   answer keeps a digest of its act; the gate tells a repeat from a different act by it. (The rulings file:
   "A reused name with different content is refused by a digest of the offer.")
2. A fact that names nothing to replace is never refused as stale; it becomes the current fact of its
   place. (Decision 4 of 6 October: `:replaces` carries the expected version, so an act is stale only when
   a fact that it replaces has moved.)
3. A retract is recorded as the current fact of its place, so that a later fact can replace it. (The
   rulings file: "Retract: a new fact".)
4. The acts depots are never trimmed. That is Rama's default; F2 A rests on it.
5. A lock's bytes are made inside the module and are never appended to a depot (the finding: the log keeps
   every appended value). Neither a retry nor a replay makes a lock again for a value that is already
   sealed.
6. In one batch of the microbatch gate, two acts that replace the same fact get the same winner on every
   attempt, and an act that touches several tasks is accepted whole or refused whole. Do not send all acts
   through one task unless the plan shows that it must (the Rama skill's first goal; open item ?15).
7. Either gate can tell which gate a layer belongs to. A microbatch act's answer is found from its act-id
   alone (Q22 A, F7 A).
8. Each word that the build makes — a reason that a gate records, a key, an id — says in plain words what
   the thing is. No word comes from the rig's code. The module's README lists them under "Words a session
   made, Sid's to change".
9. To find out, and to say in the plan: whether a rebuild from the acts depots picks the same winner when
   two microbatch acts on different entities collide. F2 A's reason holds for a layer on one task; this
   case is not yet shown.

## Not in this build, by decision

Reads and `resolve` (group 6, unanswered); bells and the feed; promotion (?10); re-class (?9); forget's
destroy step (the lock table must make it possible; the step itself waits); persons and sign-in (group 5):
the proof plays the sign-in's acts by hand, and a session's own permission beneath a person's waits for
sign-in; ending a permission (who may is not decided); grammar checks (F5 decided the place, not the
check); the door proper — it comes after this module, as the proof's hand made general. Do not build any of
these. Where the record's shape must leave room for one of them, say so in the docstring and leave the room.

## Explanations, as the tree's rules say

Each namespace and each function has a docstring; each folder a README with a parent link; no function
lists in READMEs; no history of corrections; a part that is not built stays out of the maps. The module's
README gets a section **"Decisions this module stands on"** that lists the store's decisions of 7 and
8 October by their row and the act's twelve by pointer, a section **"Engineering choices"** that lists
each choice the builder made (the partition entity, how locks are asked for and read, the cipher, the
stamp's packing, how each builder's rule is met) with its reason, and the section of builder's rule 8.
Sid's answers are in `src-store/README.md`'s decisions sections as his, never in this file as new text.

## How the build runs

By the Rama skill's stages: the implied spec, the split into parts, the plan and the check of the plan each
in a fresh session, then one build session, then one fresh review of the whole against this contract. The
build session "softland-11" judges each stage's paper. The papers go in
`src/proposal/backend-on-rama-2026-10-02/store-module/`, never under `src-store/`.

## Commits and the receipt

Commit on `main`, grouped by concern (the module's declaration and depots; the gates and the record; the
proof and the maps), each message saying what and why. No push. No `Co-Authored-By`, no `Claude-Session`,
no attribution line of any kind. The receipt: the commit hashes; the proof's run output, in full; every
provisional choice made; every place this contract could not be met as written, and why; `git status --short`
(expect only `src/app/server/README.md` modified and `docs/.obsidian/`, `docs/Untitled.md` untracked —
leave them).

Never read `src/app/server/env.clj` or any `.env` file.
