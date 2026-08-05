# Component-policy model — design proposal (2026-07-12)

Design-session deliverable per `docs/sessions/policy-design-opening-prompt-2026-07-12.md`.
Plugs into block-write CONTRACT §6 (the seam: ONE server-side decision point;
policy inputs are data; client affordances never substitute for the check).
No implementation here; no contract amendment — additive only.

Sid's two questions, kept separate throughout:

1. **Component-local policy** — what can THIS component do; which fields/
   bindings are writable, by whom, in which mode. Authored in design, as
   data, living with the component.
2. **Vocabulary/granting** — what policies can a component even KNOW exist
   or bind; the term namespace; who grants which policies to which contexts.

## 0 · The one-mechanism observation

The land already contains exactly one policy mechanism, and it is the right
one: `authorized-request?` (`core.clj:343-350`) — tokens on the request
(`:action/capability`) checked against tokens on the actor
(`:actor/capabilities`), at the single decision step, producing a durable
accept/reject row. `edit-target-kinds` (`object_container.clj:547`) is
already a policy row living in code ("these kinds accept `:object/edit`").

Neither question needs a new mechanism. Both answer: **where does more data
for that same function come from, and who may write it.** Q1 adds
target-side data to the decision. Q2 defines where every token comes from
and who hands them out.

The asymmetry between them is the load-bearing wall:

> **LAW (proposed) — monotonic narrowing.** Q2 is the only source of
> authority (additive). Q1 can only narrow it (subtractive). Effective
> permission = grant-layer authority ∩ component-local policy. Intersection,
> never union. Authoring a component is therefore never privilege
> escalation — which is what makes "policy authored in design, as data,
> living with the component" safe for faces authored by Sid, an AI
> mid-conversation, or a visitor someday.

## 1 · Component-local policy (question 1)

### 1.1 Data shape

One block, row-shaped, all vocabularies closed:

```edn
{:policy/grammar :policy/v0
 :rows [{:aspect :block/text  :action :write :mode :*        :rule :allow}
        {:aspect :block/id    :action :write :mode :*        :rule :deny}
        {:aspect :created-at  :action :write :mode :*        :rule :deny}
        {:aspect :props/color :action :write :mode :product  :rule :allow}
        {:aspect :structure   :action :write :mode :product  :rule :deny}
        {:aspect :structure   :action :write :mode :design
         :rule {:require #{:cap/design}}}]}
```

- **:aspect** — which part: field keyword (Rama objects), prop path or
  `:structure` (assemblies), `:*` (whole component). The aspect namespace is
  the target-kind's existing field/prop space — data that already exists.
- **:action** — closed verb enum. v0: `:write` only. `:delete`,
  `:rearrange`, `:rebind` grow one reviewed line each (relation-kernel
  discipline). DEFAULT.
- **:mode** — `:product` | `:design` | `:*`. Closed enum of two. DEFAULT.
- **:rule** — closed verdict forms: `:allow` | `:deny` |
  `{:require #{cap ...}}`. Nothing else in v0. DEFAULT.

**Match semantics (LAW-grade inside the model — must be total, deterministic,
boring):** exact match beats `:*`; among remaining ties, most restrictive
wins (`:deny` > `:require` > `:allow`); **no matching row = no narrowing**
(fall through to the capability check alone). The no-row default makes every
existing object grandfather in unchanged.

> **LAW (proposed) — components speak capabilities, never actors.** A rule
> may say `{:require #{:cap/design}}`, never "only actor sid-597". WHO holds
> a capability is Q2's business; an actor-id inside a component fuses the
> two questions. "Only Sid" = mint a capability, grant it once.

### 1.2 Three homes, one shape — STANCE

| Component kind | Where the policy block lives |
|---|---|
| Face/assembly | `:assembly/policy` in the envelope — one-line growth of the closed key set `#{:assembly/name :assembly/grammar :assembly/belief :root}` (`face_assembly.cljc`) |
| Rama object (block, container) | a policy field on the object's materialized state |
| Code-lane primitive | declared at registry registration, as a data literal in code |

Inside an assembly, the existing V4 guard (no lists, no symbols, anywhere)
already mechanically enforces "policy is data, never a program" — no new
guard layer. Bind refs stay minimal (`{:bind [path]}` untouched): a
binding's writability is a policy row addressed BY the prop path, not a
flag scattered into the tree — one place to read a component's whole
policy, one place to diff it; V5 validation unchanged. STANCE.

### 1.3 The tower cut — LAW (proposed)

Sid's example ("id/timestamps read-only at product layer; the declaration
itself editable one layer up, in design mode") hides a trap: if the policy
block governed its own writability, a row could `:deny` policy edits in all
modes (bricked forever) or declare policy freely writable (tampering).

> **The tower cut: a component's policy rows govern its OTHER aspects. The
> policy block's own writability is defined one layer up — by mode + grant
> (Q2) — never by a row inside itself.**

The regress terminates in two levels by construction: level 0 = the
component's data; level 1 = its policy block (edited via design mode +
capability); "level 2" is not a new level — it is Q2. Kills both
self-escalation and self-bricking.

### 1.4 Decision-step consultation

Still one pure function of request + durable state; four reads instead of
two:

1. **Mode check** — the request carries a mode claim (`:request/mode
   :design`); the actor must hold that mode's capability. Modes are
   capability-gated claims (no spoofing) but distinct from capabilities:
   a mode is a hat, not a permission — Sid in product mode is offered
   read-only ids even though he could switch hats.
2. **Capability check** — existing `authorized-request?`, unchanged.
3. **Component check** — match rows for (aspect, action, mode); apply the
   winning rule per §1.1 semantics.
4. **Verdict** — accept → KernelEvent as today; refuse → durable rejection
   row that NAMES the refusing row and its home.

> **LAW (proposed) — refusals are doors.** Every policy refusal names the
> rule that refused and where that rule lives, so the client can render
> "read-only here — this rule is editable in design mode." The refusal is a
> pointer one layer up the tower: the map doesn't just not-lie, it teaches
> its own structure and points at the layer where the rule can change.

### 1.5 Affordances — same data, second projection

At wear time (`compile-assembly`), effective policy projects into the bind
context under a reserved key (`:policy/…` paths). Lock glyphs, disabled
affordances, "design-mode only" badges are just binds over the SAME rows
the server consults. One source, two projections: enforcement server-side,
affordance client-side. §6's "affordances never substitute for the check"
made structural — there is no second vocabulary to drift into.

### 1.6 Where authored

In design mode, with the component; the block travels with the component
definition through the normal write loop (editing policy IS a write, gated
per the tower cut). Near-term: authored in EDN by hand/AI exactly like
assemblies are today. The policy-editor face (visual design) is a later
design-harness brief — non-goal here.

## 2 · Vocabulary and granting (question 2 — genuinely open; option spaces)

### 2.a Where do terms come from?

| Option | Character |
|---|---|
| All code — closed enums | max review discipline, zero in-land growth |
| All data — registry in Rama | in-land growth, but verbs would carry semantics with no code review |
| **Hybrid** — semantics in code, tokens in data | **leaning (STANCE)** |

The hybrid split falls exactly on settled ground's "anything that wants to
be a program becomes real code": action verbs and verdict forms carry
enforcement SEMANTICS → code, one reviewed line each. Capability names and
mode names are pure TOKENS compared for set membership → a vocabulary
registry in Rama, created in-land, gated by `:vocab/define`. Aspects are
already data (each kind's field/prop space).

### 2.b Who grants what to whom?

Today caps ride the request, self-declared — honest and adequate for a
one-actor land. The granting layer's real content is the move from
**request-borne** to **state-borne** capabilities (decision step looks
grants up in materialized state instead of trusting the claim).

**Leaning: grants as typed RelationEdges** — grantor —`:grants`{capability,
scope}→ grantee, one reviewed line in the kind enum. `asserted-by`
provenance, retraction, and history come free: revocation is a retraction,
durable and visible; "who can do what and who said so" becomes a queryable
part of the map. Alternative (dedicated grant PState): cheaper read, but
rebuilds provenance/retraction the relation kernel already owns.

**Constraint either way (recorded so shapes don't design into an impossible
read):** the decision step is partition-local; actor→capabilities — and
component policy for instances vs definitions — must MATERIALIZE co-located
with where decisions run. A materialization obligation, not plumbing detail.

**OPEN:** migration timing (request-borne is not wrong today); whether
grants scope to contexts (land region, session) or only to actors.

### 2.c What can a component even see or bind?

Monotonic narrowing already removed the SECURITY reason for visibility
scoping: a component binding a term it "shouldn't know" can only narrow —
worst case it forbids something. What remains:

- **Authoring UX** — the design-mode palette offers only terms meaningful
  for this component kind (per-kind aspect data, which exists anyway).
- **Future visitors / multi-tenant** — visitor components perhaps shouldn't
  even render the admin vocabulary. Real, not today's land.

**DEFAULT: enforcement-only visibility now.** All terms nameable; an
unknown or malformed term in a policy block gets a durable refusal at write
time — never a silent strip (stripping would make the map lie about what a
component's policy is). The registry records per-term `:binds-to` (which
component kinds may carry it) from day one, so scoped visibility can grow
later without re-plumbing.

**Bootstrap root:** the tower stands on the land's constitution — Sid's
actor's initial capability set, in code, asserted at land creation. One
honest root rather than turtles all the way down.

## 3 · Failure modes attacked

- **Escalation by authoring a face** → monotonic narrowing (∩ never ∪).
- **Self-bricking / self-granting policy blocks** → the tower cut.
- **Client/server vocabulary drift** → single-source projection into the
  bind context (§1.5).
- **Turing creep** → closed verdict enum. Smell test: the moment a rule
  wants to read the TARGET'S DATA (not request/actor/mode), it stopped
  being policy. First real pressure: ownership ("writable by author only")
  — expressible later as a named relational verdict form
  (`{:same-actor-as [:created-by]}`), one reviewed code-lane line each,
  never predicates in data. OPEN growth path, deliberately not v0.
- **Mode spoofing** → modes are capability-gated claims.
- **Two rows disagree** → most-specific-then-most-restrictive; total,
  deterministic.
- **Replay divergence when policy changed between deliveries** → policy is
  durable state like any other; the existing decision-journal idempotency
  already makes replays no-ops against committed decisions.

## 4 · Commitments summary

| Mark | Commitment |
|---|---|
| LAW (proposed) | Monotonic narrowing: effective = grants ∩ component policy |
| LAW (proposed) | Components speak capabilities, never actors |
| LAW (proposed) | Tower cut: policy-block writability defined one layer up |
| LAW (proposed) | Refusals are doors: refusal names the rule and its home |
| LAW (proposed) | Unknown policy terms → durable refusal, never silent strip |
| STANCE | One row shape, three homes (assembly envelope / object state / primitive registration) |
| STANCE | Policy in the envelope block, addressed by path; bind refs stay minimal |
| STANCE | Hybrid vocabulary: semantics = code lane, tokens = registry data |
| STANCE (leaning) | Grants as typed RelationEdges (`:grants` kind) |
| DEFAULT | v0 verdicts: `:allow` / `:deny` / `{:require #{caps}}` only |
| DEFAULT | Modes: `:product`, `:design` only |
| DEFAULT | No matching row = no narrowing (grandfather-compatible) |
| DEFAULT | Visibility: enforcement-only now; registry records `:binds-to` for later scoping |
| OPEN | Request-borne → state-borne capability migration timing |
| OPEN | Grant scope: actors only vs contexts (region, session) |
| OPEN | Relational verdict forms (ownership) — first growth pressure |
| OPEN | Mode grain: per-request claim (v0) vs per-view-instance stance — UX question for the later design-harness brief |

## 5 · Non-goals (inherited from the opening prompt)

No implementation; no block-write contract amendment (additive follow-ons
only); no visual design of the policy UI (later design-harness brief).
