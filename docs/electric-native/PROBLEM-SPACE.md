# Electric-Native — The Problem Space

Status: **LIVING** · banked 2026-08-13 out of the three-parallel-derivation
round and its adversarial court (three solo sessions on one question, merges
withheld, a fresh court auditing the convergences against the repo). This
file holds the PROBLEM held open: the loads, the tensions, the as-built
honesty, the forks with their deciders, the receipts owed. It is not a work
queue and it settles nothing.

Pointers: principles = `docs/ARCHITECTURE.md` · the road =
`DIRECTION.md` (beside this file) · receipts = `RECON.md` §1–10 + §11
(court receipts) · binding law = `docs/decisions.md`.

## The question, in Sid's words

"i think currently we have 2/3 ways for this whole sequence of
rama-electric-ui" · "the main thing … the data structures we store, how
they transfer between the boundaries why are they shaped like they are
shaped … from the product pov" · "the boundary of electric and rama is
very very very crucial .. is it one entry point a single atom deriving
everything .. is it hierarchical" · "how concurrent and throughput the
system is" · the thousand-editors example (each block's editor fast alone,
all live + global pan/zoom = "a dog slow system on load and then on
movement") · the collaboration sketch ("maybe have a buffer of 120 size on
server which can be consumed live but not saved"). Full verbatim: LOG
2026-08-12/13.

## The circulation as-built — honest

```
 ┌─ RAMA — 5 deployed modules (11 in source), keyed + act-linked ─────────┐
 │ depots per module (RECON §11) · ~25 OC PStates {object-key → rows} ·   │
 │ event-id on EVERY row defrecord · order is data (sorted submaps)       │
 └────────┬──────────────────────────────────────────▲────────────────────┘
          │ foreign-select (pull-only; proxy          │ foreign-append :ack
          │ subscription crashes Rama 1.6.0)          │ (depot → decision
 ┌────────▼──────────────────────────────────────────┴────────────────────┐
 │ JVM/ELECTRIC SERVER — face-projection joins pages per pull ·           │
 │ 12-entry projection registry · 9 quarantined mirror atoms ·            │
 │ TWO epoch counters (global ingest + local accept-gated), 13 bump      │
 │ sites, all strip identity ("a counter, not truth — resets on restart") │
 └────────┬──────────────────────────────────────────▲────────────────────┘
   whole  │ anonymous pages, ~1/sec on the bell       │ outbox atoms
 ══ WIRE ═╪═ 1 socket · e/diff-by live uses: ZERO ════╪═ caps 64 / 8 ══════
 ┌────────▼──────────────────────────────────────────┴────────────────────┐
 │ CLIENT — 11 pull loops + 6 watches → plain atoms → keyed !world ·      │
 │ scene store {(vi,address)→slot} incremental · !camera OUTSIDE the      │
 │ graph, per-RAF deref · optimistic overlay + refusal revert ·           │
 │ T2 editor: fixture-fed, NO server data path                            │
 └────────┬────────────────────────────────────────────────────────────────┘
          │ identity-gated uploads, keyed pools
 ┌─ WEBGPU: camera = uniforms · hit-test = paint tree reversed ───────────┐
 └─────────────────────────────────────────────────────────────────────────┘
```

One sentence: **keyed and act-linked at rest, anonymous in motion** — the
write side already knows every key and every causing act; the read side
pays every second to rediscover both.

Answering "2/3 ways": the count depends on the axis — 2 product boots ·
3+1 transports · 4 disciplines inside the Electric road · 6 client lanes ·
~15 architectures over time. The discriminating question is never the
count; it is which splits are **principled** (by rate/kind — the
camera-lane vs truth-lane split is receipted and stays) and which are
**era-debt** (Electric + REST peer writers into one depot with one global
integer as the only synchronizer).

## The loads — what the circulation must carry

```
 moment                     demands                       today
 ─────────────────────────  ────────────────────────────  ─────────────────
 typing in the focus        instant local echo · acked    overlay + ack ✓;
                            act · truth echo without a    echo = page stomp
                            stomp                         each second
 the agent pouring beside   keyed deltas to your lens ·   page re-pulls;
 you (THE GATE's load)      your keystroke preempting     no QoS
                            its bulk
 burst arrival (boot ≡      skeleton instantly, matter    2–3min cold loads
 teleport ≡ pulled-by-      proximity-out · progressive   recorded; no
 another-mind)              fidelity, honestly worn       skeleton tier
 the fling (1000 blocks)    pan = one uniform · zero      receipted ✓ (in-
                            per-block work                shader transforms)
 thousand editors           ONE live editor (T2 at        ruled + built —
                            focus) · blocks as inert      but T2 itself is
                            keyed rows                    fixture-fed
 the morning return         "what happened since I        depot replay only;
                            left", by actor, by place     no since-reader
 co-editing one block       turn-taking (warm blocks)     absent; CRDT
                            before/instead of CRDT        deliberately not
                                                          reached for
 collaboration motion       lossy live poses · durable    absent; the 120
                            only as punctuation           ring is unbuilt
```

## The tensions — problems the principles answer

Each is one of ARCHITECTURE.md's six conflicts, lived here: truth vs views
(§1) · being truth vs being felt (§2 — the 60fps/collaboration sketch) ·
identity at rest vs anonymity in motion (§3 — THE crucial boundary) ·
merge vs decision (§4 — the agent era) · existence vs attention (§5 — the
thousand-editors example) · one truth vs many eyes (§6 — humans, minds,
other-eyes).

The boundary question answered in principle: neither one-atom-deriving-
everything (O(N) at the root forever) nor per-component subscriptions
(N sockets, torn reads) — the third shape the client already invented in
the scene store: **demand up hierarchical (membership), delivery down flat
and keyed, applied atomically per accepted act.**

## Holes today — court receipts, decision-relevant

- **Geometry settles bump nothing.** A plain move/camera settle writes
  durable truth (event-linked hint rows) but bumps no epoch — only
  `:deleted?` bumps (`server_jetty.clj:2334-36`). Another lens never
  learns you moved something until an unrelated bump fires. Invalidation
  coverage is incomplete, not merely coarse.
- **T2 is an island.** The one real text editor boots from a hard-coded
  fixture (`editing_runtime.cljs:23`); its wiring injects font/camera/
  transform providers only — no server data source anywhere.
- **Identity is stripped at 13 sites.** All four sampled bump sites have
  the changed ids in scope and pass none of them (`RECON.md` §11) — at one
  site the identity travels in a sibling callback one line away.
- **The bell has restart amnesia by declaration** — the epoch counter is a
  quarantined mirror ("a counter, not truth … resets on restart", INV-14).
- **Cross-talk:** two minds in two conversations invalidate each other
  through the one global integer.
- **No since-reader over the revision log** — time-prefixed order-keys and
  cursor ranges exist; the "revisions since W" reader is one function
  away, unwritten.
- **Tombstones unstated** for any keyed-map delivery: deleted keys must
  linger as entries or removal is lost under conflation.
- **Recovery is the stomp.** The full pull's mid-session landing clears
  the overlay wholesale; its duties are named in code (committed-echo
  cross-check INV-19 · cap-overflow reconcile) but its contract — what
  happens to in-flight optimistic state — is unwritten.

## Principled vs debt — the fate table

```
 structure                shaped by                fate in the target
 ───────────────────────  ───────────────────────  ──────────────────────
 depot envelope + ack     acts are records         KEEPS SHAPE; grain
                                                   widens to verbs
 decision rows            truth is decided         KEEPS SHAPE; becomes
                                                   the morning-after
                                                   review surface
 graduation overlay       editing never erases     KEEPS SHAPE; bedrock
 RevisionRow + history    the log appreciates      KEEPS SHAPE; gains a
                                                   since-reader (the wire
                                                   changelog's spine)
 geometry-in-truth        the place remembers      KEEPS SHAPE; gains its
                                                   missing bump/notify
 scene store + camera     own your appliers · the  KEEPS SHAPE; the
 quarantine               hand is free             receiving organ is right
 data-context page        one-artery uniformity    CHANGES: dissolves into
                          era                      keyed planes; page-pull
                                                   retires to oracle WITH
                                                   duties
 9 mirror atoms           shipping past a broken   DIE as truth; reborn
                          proxy                    lawfully as declared-
                                                   ephemeral buffers —
                                                   contents face the
                                                   chrome-as-material FORK
 global epoch integer     simplest possible        DIES; scoped notify,
                          signal                   then the changelog
 in-flight queue + caps   events forced through a  caps dissolve when
                          conflating channel       correlation = act-id
 T2 fixture               editor built in          the fixture dies; T2
                          isolation first          wires to real blocks
```

## Forks — by decider

**Sid's alone** (options exist; only his word closes them): trails — are
movement traces material, and therefore what the presence ring retains ·
act grain (turn vs settlement vs verb) · chrome-as-material (do the 9
mirrors' contents — workspace, sidebar, settings — deserve durability) ·
zoom & briefings-as-zoom (semantic LOD as a named bet) · the collaboration
soul (decided-truth headline vs hedge; CRDT-in-workshop stays a composable
later refinement either way) · multi-mind timing · true deletion (revoked
sources — decide before an incident decides it) · residency-vs-gate
sequencing · the lease noun · the forest voice (memory-prosthesis vs
log-and-views vs place-with-physics — three compatible theses, three
different next-priorities).

**Argued leans** (position held; closes by adoption or better argument):
one-path recovery as the stability test · container-grain notification as
a lawful intermediate rung · the tombstone clause · the seen stamp ·
"the fold stays server-side" written as law (Rama is the view engine;
minds need server views regardless; audiences need projection, not
history) · machine-attention budgeted by actor class · the oracle
duties-transfer list.

**Probe-decided** (no one decides; receipts do): feed transport (Electric
keyed machinery vs owned Missionary lanes) · felt-lag locus · seen grain ·
per-key cardinality knee · propagation grouping · warming stampedes.

## Probes owed — deduped; what each closes

1. **The transfer bench** — four merge algebras × `e/diff-by`
   cross-wire losslessness × two-edits-one-tick. Closes: feed transport.
   (Zero live `diff-by` uses exist — the bet is untested in BOTH
   directions.)
2. **Road-1a profile + weigh one wire frame.** Closes: felt-lag locus and
   keyed-wire urgency. The only measured wall so far is client-side
   shaping (35.678s / 28.7s — contracted); the wire's felt contribution is
   still attribution-owed.
3. **Bumper audit** — 4/13 sites sampled (all know their keys); 11 remain;
   includes "does any UI-visible change originate with no bumper at all"
   (geometry already answers: yes).
4. **RevisionRow keystone — CLOSED** (fields banked, RECON §11). Residue:
   which non-text verbs should mint revisions vs ride event-linked
   projections; the since-reader.
5. **Per-key cardinality spike** (RECON §10) · **false-conflict rate** at
   real edit cadence (prices seen's grain) · **cross-conversation waste
   count** (prices the container-grain rung) · **ordering-under-retry** on
   the act path · **one swap of N units → client propagation frames**
   (the grouping law's pass/fail) · **warming stampede** shape (only if
   warm/cold proceeds) · **layout ms/block** and **briefing assembly
   cost**.

## What falsifies this frame

Blocks growing into documents (kills ops-up/states-down and resurrects
the patch-algebra monoid — note: block-smallness is currently enforced
nowhere) · usage turning into a write-dominated swarm (a read-optimized
lens is then the wrong center) · the instrumented gate showing making's
bottleneck is authoring-iteration, not echo (reorders the gate-critical
set — the honest unknown; watch it).
