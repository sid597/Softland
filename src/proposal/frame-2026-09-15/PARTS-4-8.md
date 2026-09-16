# Parts four to eight (rendered 16 September 2026, not yet presented)

Rendered by the 16 September session in the same form as parts one to three in
[What-is-this-proposal-about.md](What-is-this-proposal-about.md): what it
proposes, with the words in dependency order and a diagram; what it settles;
what it leaves open, with the trace's positions marked as the session's; what
it asks. The proposal's own words are noted once per part. These carry no
ruling. Part three had not been responded to when these were written; if
Sid strikes "only the gate writes," part six changes.

The eight parts, for orientation:

1. What everything is made of. (closed)
2. What does things, and how it is found. (closed)
3. How change moves. (delivered, awaiting response)
4. How it stays honest.
5. How many people share it.
6. What stays outside, and how outside gets in.
7. What is fixed, and how to check the cut.
8. The current code against the frame.

---

## Part four. How it stays honest.

**What it proposes.** Words in the order they depend on each other.

Part one gave every fact a basis: the reads it stood on, each with the version
seen and whether the read was exact. This part is what the basis buys.

A fact or a running answer is **stale** when something in its basis has a newer
version that the reader's context can see. That is one rule with two
consequences. On a fact it is a mark: made against an older version. Nothing
re-runs, because a fact is a statement someone made, and it stays made. On a
running answer it is a re-trigger: the tool runs again, under the policy of
where it sits. The mark and the re-trigger come from the same check.

Stale is itself a running answer. Its reads are the current versions of the
things in someone's basis. Nothing in the fixed part knows what stale means.
It is a definition anyone inside could rewrite. The fixed part supplies only
versions.

A read is **exact** when the thing it resolved to is certainly the thing meant.
A read that matched by name alone is not. A fact or answer with an inexact read
in its own basis is **unreliable**, and the proposal asks that this be painted
as loudly as stale. Unreliable is local: it speaks of this one's own reads.
**Doubtful** follows the chain: this one read a fact that was itself
unreliable. Doubtful is a view that walks bases, not a property of a read, and
it is what "the map must not lie" means when the doubt is two steps back.

**Drift** is the same machine on tools. A tool's signature names the shapes it
expects, and a shape is a grammar with a version. The link between a tool and
the grammars it depends on is a running answer whose reads can move. When a
grammar changes, every tool that named it is stale exactly as a view is.
Detection is solved by this. What to do about it, migration, is not.

Every read returns a **status**, which part three named: value, absent,
pending, failed. The status is part of what was read. A read that has not come
back says pending, and a running answer built on it says pending too, rather
than showing an empty where the truth is "not yet." A read that returned absent
is recorded as a pattern read that found nothing, and it goes stale when
something matching appears.

A running answer made from several reads taken at different moments is
**torn**. The place does not promise a snapshot. It records each read's
version, so whether all reads came from one moment is derivable and can be
painted. "This page was composed from reads at different moments" is one more
thing the map may not hide.

**Time** is never read. A tool never asks what time it is. It reads the times
stamped on facts, and the gate stamps them. A tick that should cause work is an
offer from outside, a fact of an event kind, and the tools that want it match
it. Time enters twice, as a stamp at the gate and as an outside offer, and never
as a read. This is what keeps inside tools replayable, and replayable is what
makes a past running answer regenerable from its tool at a version plus its
reads at their versions.

No leaf may read a clock, a random number, or anything mutable outside.
Anything that needs those is an outside tool and gives only offers. This is a
fixed rule and a gauge: every proposed leaf is asked whether it is replayable.

Over time the whole place is a helix. Its shape is the same on every pass. Its
state never repeats, because nothing is overwritten.

```
   a running answer and its basis
     view of q1 ── built from [q1 members @13 · p1 text @18 · f1 source @2 (exact) · c1 text @5]
                                                              │
   f1 source @33 lands                                        │
     └─ who read f1 source @2?   an index over bases          ▼
          └─ the view: stale → re-runs, because watched
          └─ the relation f1→c1, which also read @2: stale → marked. Stays.

   exact · unreliable · doubtful
     ingest resolved "the thing named f" by name alone              that read: not exact
     the new source fact stood on that read                         unreliable
     the view stood on the new source fact                          doubtful, two steps back
     the painter follows the chain and says so, as loudly as stale

   time
     never:  a tool asking what time it is
     twice:  the gate stamping every fact · a tick arriving as an outside offer
```

**What it settles.** Fixed for the life of the place, from this part.

- Every read returns a status and leaves a basis entry with its version and its
  exactness.
- Stale, unreliable, doubtful, and drift are definitions over bases, not parts
  of the fixed thing. The fixed thing supplies versions and statuses.
- Time is stamped at the gate and arrives as outside offers. No tool reads it.
  No leaf is non-replayable.

**What it leaves open.** With the position the trace took where it took one.

- Exactness with two values or three. Part one's open item. The trace's
  position: two, with the method of a match kept in the value.
- Migration when a grammar or the envelope changes. Detection is drift.
  Migration is an owned outside job whose shape is settled by the first one.
- Whether a torn read is painted by default or only when asked. Not addressed.
- The trace's position that a read that found nothing is a pattern read as of a
  moment, and goes stale when something now matches. Not yet ruled.
- The trace's position that one stale rule both marks a fact and re-triggers a
  running answer. Not yet ruled.

**What it asks of you.**

- The line. Whether "every read leaves a basis entry" and "no tool reads time"
  belong on the fixed side. Both are costs paid on every pass to buy honesty on
  every change.
- Breaking. Name something the map could say that is not stale, unreliable,
  doubtful, torn, or pending, and is still a lie. If there is one, the machine
  is missing a case.
- Positions. Two-valued exactness. The found-nothing read. One rule, two
  consequences.
- Words. Stale, exact, unreliable, doubtful, drift, torn, status, replayable.

---

## Part five. How many people share it.

**What it proposes.** Words in the order they depend on each other.

A **layer** is a working copy. Part one put a layer on every fact; this part
says what layers are. The proposal names four: a **base**, which everyone
builds on; a **candidate**, work against the base kept inspectable before it
becomes the next base; a **session**, facts that live as long as someone's
sitting; and a **personal** layer, one person's durable overrides.

A **context** is what an asker brings to a read: who they are, and the ordered
list of layers they see, nearest first. Session over personal over candidate
over base, say. When a tool reads a thing's attribute, it gets the newest fact
in the nearest layer that has one. **Nearest active wins.** Two people reading
the same thing can get different answers, each right for its context, and
neither has to know the other exists.

**Resolve** is the one place reads leave the store, and it is fixed. No tool,
including one running in someone's browser, can read around it. What resolve
enforces is **visibility**: which layers this actor may see, and, where policy
says so, which things within them. Visibility is facts. Who may see what,
nested through group facts, is written by inhabitants and changed from inside.
What is fixed is only that every read carries an actor and passes through
resolve.

The rules bottom out in a seed: a root actor, and a base layer whose policy
governs promotion into it. That is the social fixed point, seeded like the
first grammars.

**Promotion** is how a candidate becomes base. It is an offer: this fact, from
that layer, into this one. The gate checks it like any offer, expected version
included. Two candidates that changed the same thing do not merge. The second
to promote is refused with what it lost to, and that refusal is a **miss**: a
fact with a subject and a time. Recorded work, not a blocked door, and nobody
resolves it silently.

**Branching** is only this. Two people on one thing at one time each write
their own layer and read their own first, and both are right in context. The
gate refuses only inside one layer, when an expectation is wrong because
someone else got there first. Across layers nothing is refused. **Comparison**
is free: run the same tool under two contexts and diff what it gives. That is
what "layers should be comparable and inform one another" costs.

The word "layer" gets used four ways, each answering a different question. The
**context** axis is this part: whose copy, who may see, which wins. The
**strata** axis is what a change costs: material changes next run; Softland
code changes by rebuild; the host is outside. The **grain** axis is thing
versus version. The **tier** axis is how real something is yet: a local signal
that never landed, a session fact, an accepted fact. Optimism is painting tier
one as tier three, and the rule is that each is painted as what it is. Only the
context axis is written on a fact. The other three are read off.

Agents are actors. An agent's tool reaches outside, so it gives only offers.
What it was shown is recorded as an offer, so its reply has a basis. Its
authority is policy. Its misses are facts. Nothing is added for agents. They
use what people use.

Another copy of the place is outside. Facts from it arrive as offers through
the outside door, with provenance made at the door. Federation is not a
feature. It falls out.

```
   alice's context: [session S · personal A · base]       bob's context: [session T · personal B · base]

   rel1 relation     base @25   (alice's, promoted earlier)
                     A    @37   (alice re-asserted)        alice reads @37
                     B    @36   (bob's own)                bob reads @36
   nothing refused. Different layers. Diff by re-deriving under each.

   bob promotes @36 into base:    expected @25, current @25  → base @36'
   alice promotes @37 into base:  expected @25, current @36' → refused · lost to @36' · a miss fact
   alice's move: read the miss, decide, offer again with expected @36'. Nothing merged. Nothing blocked.

   visibility
     every read ─▶ resolve ─▶ which layers may this actor see?   facts, nested through groups
                              nearest active wins among those
     seeded: a root actor · a base layer · the policy for promotion into it
```

**What it settles.** Fixed for the life of the place, from this part.

- Resolve is the one place reads leave the store. It is fixed, and it enforces
  visibility. Every read carries an actor.
- Who may see what, and who may write where, is facts, changeable from inside,
  seeded by a root actor and a base layer's promotion policy.
- Nearest active layer wins. Promotion is an offer. A conflict is a miss fact,
  never a merge and never a silent priority.

**What it leaves open.** With the position the trace took where it took one.

- Session lifetime: when session facts die and who clears them. Part two
  deferred it here. The proposal does not say. The trace: a rule tombstones a
  hold; the session's own end is a policy of the boundary, not decided.
- Erasure against sovereignty. Visibility handles who sees. It does not handle
  removal, and an append-only pile resists removal. The proposal's position: a
  tombstone above, a physical drop below as an outside job. Whether that
  satisfies a person's ownership of their data, or the log itself must be split
  per person, is settled by the first real second person.
- Tier overlapping context. The trace's position: tier adds only "not yet a
  fact"; its other two values are already context.
- Whether visibility is per layer only, or also per thing within a layer. The
  proposal says "down to the atomic packet," so per thing must be possible. How
  that is indexed is a store question.

**What it asks of you.**

- The line. Whether resolve as the one exit, and the seeded root, are fixed.
  This is where "authentication and authorization should come first because it
  solidifies" landed: the proposal says what solidifies is the exit, not the
  policy. The top-layer recognition flagged this as the sharpest possible
  divergence. This is the part to say whether it is one.
- Positions. Session lifetime. Erasure. Tier.
- Words. Layer, base, candidate, session, personal, context, resolve,
  visibility, promotion, miss, strata, grain, tier.

---

## Part six. What stays outside, and how outside gets in.

**What it proposes.** Words in the order they depend on each other.

Exactly one tool is not a fact: the **runtime**, the thing that holds the
store, runs the tools, and paints the screen. Its version is a fact. Its
behaviour cannot be produced from inside. Everything else, every tool that
edits tools, every rule about rules, is facts, and the depth of tool updating
tool is free until a pattern would have to match the runtime itself. That is
the mechanical meaning of "what must genuinely remain outside the land
forever."

A **boundary** is where two abstractions meet. The store has one inward
boundary, the gate. The place has outward boundaries: the **screen**, the
**host** it runs on, other **minds**, meaning people and models, and other
**instances** of the place. Every **crossing** is owned by the fixed part and
governed by facts: the fixed part performs the crossing; what it means is
material.

Every outward crossing that should come back does so through an **inward
return**, and the return is where provenance is manufactured, because the
outside carries none. A commit has no basis; the return gives it an anchor to
the commit. A pointer position has no thing; the return manufactures the
address from what was painted. A model's reply has no reads; the return gives
it the presentation it answered. Sometimes the return also manufactures a
request: a reply to a prompt continues the pass that asked; a bare arrival, a
commit or a tick, starts one.

**Ingest** is the return for files. When a commit lands, ingest mints ids for
the named forms in it, because reference by name is what it serves, and asserts
**continuity**: this blob is that function, by name or by hash. A name match
is not exact, and its basis says so. A person can override continuity in their
own layer, and that is a fact too.

An **enactment** is an ask to the host: rebuild, run the checks, physically
drop this. The place never evaluates a record. It asks the host and reads the
answer, which returns as offers: the check passed at this revision; the running
revision is now that. Changing Softland code is the same loop with one
enactment in the middle: the draft is material, admission yields a candidate,
an owned job asks the host to rebuild, and the outcomes return as offers. A
view over code shows three things side by side, candidate, running, and check,
each a fact with a basis.

The **strata** are what a change costs. Material: a new fact, effective next
run. Softland code: an enactment, a rebuild. The host: outside altogether. The
frame pushes as much as it can into material so that rebuilds are rare, and the
rate of rebuilds is the gauge of part seven.

The **leaf** vocabulary, the small fixed set of steps bodies are written in,
lives at the code stratum. Adding a leaf is a rebuild. That is why it is the
contested seam.

Erasure is an enactment. Above the host, a tombstone fact. Below, a physical
drop the host performs and reports. The loop stays honest about what it can and
cannot do itself.

A lying enactor, a host job that reports success falsely, is attributable, not
impossible: its outcome carries who and basis, so the check can run again and
the disagreement becomes a fact.

Many runtimes: a browser, a server, another person's instance. Each has its own
fixed point. Between them, everything is offers through outside doors, with
provenance made at the door.

```
   ╔══════════════ RUNTIME · not a fact · its version is ══════════════╗
   ║                                                                   ║
   ║  outside door ─┐                                                  ║
   ║                ├─▶ GATE ─▶ STORE ─▶ RESOLVE ─▶ MATCH ─▶ TOOLS ─┬─▶ running answers
   ║  inside door ──┘                                              │   ║
   ║       ▲                                                       │   ║
   ║       └──────────────── offers from inside ◀──────────────────┤   ║
   ║                                                               │   ║
   ║                     crossings out ◀───────────────────────────┘   ║
   ║                     screen · host · minds · instances             ║
   ╚═══════════════════════════════╪═══════════════════════════════════╝
                                   │
        inward return: identity, address, basis, sometimes request, made here
                                   └──▶ back in through the outside door

   a rebuild
     draft, material ─▶ admitted as a candidate ─▶ an owned job asks the host ─▶ host builds and checks
        ◀── offers return: check passed @rev · running revision is now X
     a view over code shows: candidate · running · check     each a fact with a basis
```

**What it settles.** Fixed for the life of the place, from this part.

- Exactly one tool is not a fact. Its version is. References resolving per run
  keep self-reference tame up to it.
- Inward crossings manufacture identity, address, and basis. A running answer
  shown across a boundary is recorded as an offer.
- The place never evaluates a record. Enactments go to the host and return as
  offers.

**What it leaves open.** With the position the trace took where it took one.

- Progress from a long enactment as offers along the way, or only an outcome.
  The first rebuild settles it.
- Migration: an enactment with an owner; shape settled by the first one.
- Erasure: tombstone plus drop, or a per-person log. The first real second
  person.
- The runtime's own runner as material. Research.
- Whether the request manufactured on return copies the asking pass's request.
  The trace's position: yes, when the presentation was made under a request
  still open; otherwise the arrival starts a pass.

**What it asks of you.**

- The line. "Exactly one tool is not a fact" and "the place never evaluates a
  record." The first is the frame's definition of what stays outside forever.
  Say whether it matches what you meant by that.
- Breaking. Name something that must be inside the place and cannot be a fact.
  Or a change to the place that cannot be a draft, a candidate, an enactment,
  and a returned outcome.
- Positions. The request rule on return.
- Words. Runtime, boundary, crossing, inward return, ingest, continuity,
  enactment, strata, leaf.

---

## Part seven. What is fixed, and how to check the cut.

**What it proposes.**

The nine things fixed for the life of the place. All were introduced across
parts one to six. Here they are together, in the proposal's order, with the
part that introduced each. Where Sid has already sharpened one, the sharpened
wording is used and marked, so what he rules on here is the current text.

1. A thing is an id. Nothing about it is built in. No "is a." (one)
2. Values have shapes. Grammars are facts with versions. The fixed part
   interprets them. (one)
3. A tool is a signature and a body. The signature is a pattern in, with
   context, and a kind out, with its nature. Signatures are facts. Tools are
   found by matching. A name lives only inside a body, never in the store:
   same lookup, and what differs is who holds the name. (two; the last
   sentence is Sid's sharpening, recorded in the ledger)
4. What a tool gives is a running answer or an offer. Running answers pass and
   recompute, and only replayable tools give them. Offers enter one gate, from
   inside or outside, with who and basis. (three)
5. The gate checks shape, expected version, and policy over actor, layer, and
   kind. Policy is facts. Only the gate writes. (three)
6. Resolve enforces visibility where reads leave the store. Every read returns
   a status and leaves a basis entry with exactness. What a context may see is
   facts. (four, five)
7. Placement is a rule over kind, and itself a tool. Rerun is a policy of the
   boundary. Budgets, cycle limits, and failure values are fixed. No leaf is
   non-replayable. (two, three)
8. Inward crossings manufacture identity, address, and basis. A running answer
   shown across a boundary is recorded as an offer. (six)
9. Exactly one tool is not a fact. Its version is. References resolving per
   run keep self-reference tame. (six)

The count is not the point. What the set excludes is: no routes, no list of
tools, no list of kinds, no tables, no stored levels.

**Who chooses shapes.** Three choosers. The fixed part chooses what a fact
carries and the leaf vocabulary. Inhabitants choose kinds, grammars,
signatures, patterns, policies, placement, and compositions. The leaf
vocabulary is the contested seam, because every leaf is a rebuild.

**The gauge.** The rate at which use demands new leaves. If the cut is right,
most of what people need is material and the leaf set grows slowly. If it is
wrong, every new capability needs a rebuild, and that shows up as a rate. The
proposal records that the last build's native-capability log classified nothing
it compiled as convenience, and that the rows added after the first loop, a
surface's lifetime, input delivery, step scheduling, were the floor growing
under use. Those rows are the needle.

**What any instance must demonstrate.** Nine things, without naming code.

1. A fact can be pointed at and its basis followed back through offers to
   reads.
2. A running answer shows its status, its staleness, and its exactness, and
   never presents an inexact read as exact.
3. An offer from inside and an offer from outside pass the same gate and leave
   decisions carrying the rule that answered and the basis.
4. A tool is added by asserting a signature and a body, with no change to the
   host.
5. A kind is added by asserting a grammar and a placement rule, with no change
   to the host.
6. Editing a definition changes the next pass and nothing else, and the pass
   boundary is visible.
7. A rebuild leaves a running-revision fact, and views over code show
   candidate, running, and check.
8. Two contexts can be diffed by re-deriving under each.
9. A miss is a fact with a subject and a time, not a fallback.

The trace in NOTATIONS.md exercised one, two, three, four, and six fully; seven
and eight in part; five and nine not at all. A second walk should be built to
hit five, seven, and nine.

**What stays open, each with what settles it.**

- Placement as an asserted rule or a maintained running answer. Settled by
  whether any index must outlive its rule.
- Migration when a grammar or the envelope changes. The first one.
- Exactness with two values or three. The proposal leans three; the trace two.
- Progress from long jobs as offers, or only an outcome. The first rebuild.
- A runner as material. Research with its own evidence bar.
- Erasure as an enactment, or a per-person log. The first real second person.

**What it settles.** This part is the list.

**What it asks of you.**

- The line, whole. Nine in, and the five exclusions. What is on it that should
  not be. What is missing.
- The check. Whether the leaf-rate gauge and the nine demonstrations are how
  you would know the cut is right. And whether five of nine from one walk is
  enough to start building on, or a second walk comes first.
- Leaving open. Whether the six open items are rightly deferred, or any is
  yours to settle now.

---

## Part eight. The current code against the frame.

Everything in this part is the proposal's own "checked" claims about existing
code. The sessions of 15 and 16 September read no code and verified none of
it. It is here so that you and the next session know what the proposal claims,
not so anyone treats it as verified.

**What it proposes.** Instances are siblings among possible siblings. None is
the abstraction.

| frame | what the proposal says exists | the proposal's note |
|---|---|---|
| the log | the Rama depot | a log instance |
| derived indexes | PStates | placement's output |
| a runner that reruns on change, and hosts the gate | a Rama topology | |
| a runner that reruns while watched, recording support | Electric with tracked reads | |
| a runner for outward crossings | the engine executor with capability tables | |
| presentation | the renderers | |
| the inward return for identity and continuity | the ingest lane | |
| a gate with actor sets and a resident policy | Inland's admission | |
| placement | Inland's index by event and demand kind | |
| maintain | Inland's proxy on a keypath | most portable; nearest the abstraction |
| the meaning of pointing, as material | Inland's pointing rule | |
| the second view kind | Inland's editor view | |
| placement and match | Canonical's verb and projection registries | written as code, not facts |
| offers | Canonical's matter-room routes | without basis |
| the gate's shape check | Worn's facet compile | |
| a gate per family | the May three-depot design | an upfront partition; regimes, not families |

**The first workpiece.** Against the nine demonstrations, the proposal places
the first workpiece at one through three over repo material, with six and nine
already partly present on the Inland side. The forced first step is repo
material as facts: the inward return manufacturing identity and continuity for
named forms and passages. The projection that exposes them must carry who and
layer, not only value, or demonstration three and visibility cannot hold.
Demonstration two requires exactness in the basis from the first read.

The trace added three things the first workpiece's facts must carry, all the
session's positions: basis entries of three shapes; no slot for which tool made
a fact, the tool being in the basis by reference; relations as things with
their own ids.

**The next representation up.** An executable model: the envelope as data, the
rules as functions, the seed and the walk as offers, and a run that folds them
through the gate and match. Roughly two hundred lines. It would remove the hand
from the trace and let demonstrations five, eight, and nine be tested directly.
Not built. Not the implementation.

**What it leaves open.** Whether the first workpiece is the repo-material one,
and whether the executable model comes before it.

**What it asks of you.** Go. Which next step, and in what order: the executable
model, the first workpiece, a second walk, or something else. And whether the
claims in the table should be verified against code before anything is built
on them, which would be a separate session with a code-reading brief.
