# Plan validation — stage 5a, "read exit" (PLAN-read-exit.md)

<!-- Phase 2 of the rama skill (phase-2-plan-validate.md, template
artifact-plan-validation.md), 26 September 2026, 02:13 to 03:00 IST, by a
fresh-context validator (Claude Opus 5.5, effort max) in worktree
/mnt/data/projects/Softland-rig-plan-reads, branch rig-plan-reads. Default
verdict FAIL. Plan validated: PLAN-read-exit.md at 59b7cf49.
Spec read: SPEC.md (phase 5 verbatim, "What Rama showed"), STARTER-next.md,
the MAIN rig worktree's RIG.md "Defaults taken overnight" 1 to 8
(/mnt/data/projects/Softland-rig-2026-09-25, at 5b29563f), PROGRESS.md "Now"
lines 42 to 210 on main, CONCLUSION.md R2 to R7 on main,
rama-check-2026-09-25/RESULTS.md lines 35 to 68 on main, model.clj's
`stood-on`, `chain-head`, `erasure`, `stamp-for`, `read-as-of`, `op-read`,
IMPLICIT_SPEC.md RD2 to RD6, E8, OP11, and the phase 1 code in the main rig
worktree (clock.clj at 0bc0cd7f, gate.clj `stamp-for` and `decide*`,
envelope.clj parts, bounds and `parse*`, module.clj whole). Interfaces read in
the parallel worktrees: PLAN-locks-and-forgetting.md at cbd2bb16
(rig-plan-locks) lines 94-100, 545-575, 598-606, 645-655, 1105-1115; the
micro plan's frontier id (rig-plan-micro, M23). Not opened:
src/app/server/env.clj, test/rig/store/gate_test.clj. One cluster probe
was run, for F2's page reads (see "Unverified Rama behaviours"). -->

## Verdict

**minor-fail.** Eleven failures, each fixed in the plan in place (F1 to F11,
marked `[F<n>]` in PLAN-read-exit.md). Two of them are substantive: the read
entry could be stamped below the moment it records (F1), and the rebuild as
one event cannot finish on a large layer inside Rama's stream timeout (F2).
Neither needs the architecture rethought: the PStates, the read path, the
exit's order and the entry's format stand; F1 adds one input to the gate's
stamp rule, F2 replaces one mechanism (the rebuild) by a paged one whose
every step is traced below. Why not major: every fix is a localized edit I
could write and trace here; nothing the other sections rest on moved.

| # | failure | fix, in the plan |
|---|---|---|
| F1 | An entry decided on another task can be stamped below its moment and below the matched facts' stamps; a read as of a T between them then shows, inside the entry, ids and stamps admitted after T | The entry stands on its moment: `gate/stamp-for` counts each `:read/*` fact's moment (`reads/entry-moments`, total, bounded like a carried stamp); FR14; T18 |
| F2 | Rebuild as one non-yielding event: 100,000 facts cost about 100,000 seeks (30 to 50 s), past the 5 s stream timeout; the event is retried while running, force-failed at 30 s and replayed, for ever, blocking the task | Paged rebuild: operator-driven put pages then sweep pages on `*index-ops`, each one event bounded well under 1 s; traced |
| F3 | `:resume` is the address of the first match not shown: it hands a person or model reader a fact id, stamp and (for `[:e]`) key that the entry's exact list does not carry | `:resume` removed; the limit + 1st entry decides the mark and is never returned |
| F4 | The read path answers `:no-such-layer` for a layer that does not exist and `:not-visible` for one that does: an existence oracle over private layers (ruling 9) | Both answer `:not-visible` on the read path |
| F5 | After a person forget (phase 2; no per-value purge planned there), a value-index entry keeps its plaintext address; a `[:kv k v]` read would match it and show it as erased, confirming a guess at a forgotten value (IMPLICIT_SPEC RD3) | A `[:kv]` candidate counts as matched only when open-value gives its value equal to the pattern's; phase 2's person forget still owes the purge (for builder A) |
| F6 | The index entry's fixed schema has `:v` only, so phase 2's row fields (`:sealed`, `:lock-id`, `:lock`, `:digest`) cannot ride in it and its `open-row>` could not open from an entry; the plan's `open-value` arity and returns differ from phase 2's plan (`open-value>`/`open-row>`, `{:value v :stamp s}`, `:after-moment`) | The entry is the log row's fields plus `:fid :stamp :erased-at`, derived from the row schema; the call is phase 2's `open-row>`, whose contract wins; the stub follows it |
| F7 | Every read entry's line (a 1,000-pair exact list is about 80 KB) is copied into two index entries: written on every read, read rarely; a cheaper design exists within latency | Hint `:no-copy` (seeded `#{:read/point :read/pattern}`): those entries carry no value copy; a pattern read showing one fetches its row |
| F8 | A pattern read as of an early moment over a large prefix scans every later entry, since skipped entries do not count toward the limit: unbounded work per query, never marked partial | A scan budget, 16 × (limit + 1) entries; past it the read is partial (RC10) |
| F9 | IMPLICIT_SPEC RD2 defines the point read by layer, entity and key (the chain's head), recorded as rows; the plan serves that read as the pattern `[:latest e k]`, recorded as a line, and defines point reads by fact id, without naming the divergence | Named in FR9 and "For Sid"; no mechanism change |
| F10 | Interfaces: the gate edits were not all named (F1 adds one), the person forget's reach into `:ix-kv` was left to "phase 2's pick" while phase 2's plan picks nothing for it, and the reader's kind is caller-supplied | Named in "Interfaces"; the person-forget obligation and phase 2's duplicate `read-as-of`/`:by-stamp` go to builder A; the kind comes from the actor in a kept store |
| F11 | The partitioning tables omit the point read, a category of input | Point-read row added, weights re-summed |

## Query topology: `read-point`

- Input examples present: yes (PLAN "read-point", "Input examples").
- Example 1, one fact id admitted before the moment: N = 1 settings + 1 clock
  + 1 record + 1 to 2 row = 4 to 5; M = 4 to 5 (visibility needs settings,
  the moment needs the clock, the record decides present or absent and gives
  the stamp, the row is shown). N == M: yes.
- Example 2, five fact ids, two absent: N = 2 + 5 records + 3 rows (3 to 6
  seeks); M the same (an absent record read is what decides "absent"; no row
  is read for it). N == M: yes.
- M differs across examples: yes; marked variable, `loop<-` over the ids,
  one record seek per id and a row seek only when present. Pass.

## Query topology: `read-pattern`

- Input examples present: yes (seven, PLAN lines 654-672).
- `[:latest e :note]`: N = 3 (settings, clock, one tail read), M = 3. Equal.
- `[:ek e :note]`, 4 matches: N = 3 seeks + 16 iterations, M = 3 seeks + 5
  iterations (4 matches and the entry that ends the range). The 11 extra
  iterations sit inside the one seek of the first page (a page of 16 is one
  iterator seek, then Next); they cost about 11 × 5 µs. Counted as seeks,
  N == M. Pass on seeks; the page-size choice (RC3) bounds iteration waste
  at 16 per small read.
- `[:k :note]`, limit 1,000: 2 + 6 page seeks, about 1,001 iterations, all
  meaningful (the 1,001st decides the mark). Equal.
- `[:e unknown]`: 3 seeks, 1 iteration; the range seek is what proves the
  read empty. Equal.
- Variable M: yes, marked, handled by the doubling page loop. With F8 the
  loop also stops at the scan budget. Pass after F8.

## PState schemas

- Groups by (key type, partitioner): the plan adds no PState; its four
  fields live in `$$layers`'s value, keyed by layer, hash by layer, beside
  phase 1's fields. The skill's merge rule is applied, not violated. Pass.
- Any Object type: no. Addresses String, ids keyword and vector, stamps
  Long. Pass.
- Uniform record-like values use fixed-keys-schema: the index entry, yes.
  **Fail before F6**: its fixed schema names `:v` only, while phase 2's
  validated-in-parallel row carries `:sealed`, `:lock-id`, `:lock`,
  `:digest` (PLAN-locks-and-forgetting.md lines 645-647, 1716-1718); a
  fixed-keys entry cannot carry fields it does not declare, so after phase 2
  an entry could not be opened without reading the row, and RC5 ("no
  per-fact seek") would silently fall. F6: the entry schema is the row's
  fields merged with `{:fid :stamp :erased-at}`, taken from the row schema
  in module.clj, so whatever phase 2 adds to a row an entry carries.
- Differing shapes at one position: none (an entry is an entry; a
  tombstone is an entry with `:erased-at` set and the value fields nil, a
  nullable field on one shape). Pass.
- Inner collections over 100: `:ix-ek`, `:ix-ke`, `:ix-kv`, `:ix-of` are
  subindexed. The `:ix-of` value set: enforced bound, one address per
  value-indexed field for the fact, and `index-writes` makes exactly one
  `:ix-kv` address per value-indexed fact (the code rule is the function:
  one set of at most one address per fid). Pass.

## Partitioning

- Every read: leading `(|hash *layer)` to the layer's home, the task that
  holds `$$layers[L]`. Every index write: inside the gate's event on that
  task, no partitioner. The entry: through `*offers`, `hash-by :layer` on
  the working layer. Keyspace: layers, many per task; a hot layer is phase
  1's named limit (ruling 2's re-class is the answer). Pass.
- Tables for N = 1, 16, 128: present. **Fail before F11**: no point-read row,
  though a point read is a category of input (and IMPLICIT_SPEC RD2 calls
  cell reads "the most frequent read"). Recomputed with F11's row (point
  read of one id: 4.5 seeks, 0 iterations, weight 0.10; small reads 0.50):
  0.25 × 3 + 0.50 × 3 + 0.10 × 4.5 + 0.10 × 8 + 0.05 × 3 = 3.65 seeks;
  0.25 × 1 + 0.50 × 16 + 0 + 0.10 × 1,001 + 0.05 × 1 = 108.4 iterations. The
  same at every N: every read dispatches to exactly one task; nothing fans.
  Weighted seeks do not grow with N. Pass after F11.
- Justifications draw only on the spec (one-owner layers on one task, P2);
  the exit's entry decision is phase 1's cost, cited not assumed. Pass.

## Topologies

- Microbatch unless justified: no new topology. The index writes ride the
  stream gate's event because "a person's write must be visible to their
  next read with no optimism": the entries must commit with the answer.
  Pass.
- Stream concerns: (1) index writes of an admitted act (must be visible
  when its answer is: stream); (2) the rebuild and test ops on `*index-ops`.
  Does (2) need stream? It needs to be in the topology that owns
  `$$layers` (F12 of phase 1: one owner), which is the stream gate; a
  microbatch topology cannot write `$$layers`. Owner constraint, not
  latency. Pass.
- Any choice made on test-synchronization grounds: no. Pass.

## Production readiness

- Concurrent clients: two readers' entries have separate names and are
  ordered by their working layers' gates; reads are read-only. Pass.
- Client restart: the exit holds no state across calls; a crash before the
  entry's `:yes` shows nothing; a restarted reader reads again under a new
  entry name. Pass.
- Worker restart mid-topology: index writes are set/delete at computed
  addresses (replay writes the same); queries fail and the exit shows
  nothing. **Fail before F2** for the rebuild (below). Pass after F2.
- Large scale: every unbounded map subindexed. **Fail before F2 and F8**:
  the rebuild of a large layer cannot complete (F2) and an old-moment pattern
  read over a large prefix has unbounded work (F8). Pass after.
- Non-idempotent writes in the stream topology: none. Every index write is a
  `termval` at an address computed from the act (name, index, stamp, entity,
  key, value text) or a `NONE>` at such an address; no append, no counter.
  After F2 the rebuild pages are the same: put pages `termval` implied
  entries, sweep pages delete non-implied ones. Resolution: made idempotent
  by computed addresses.
- Multi-partition writes: none. The exit's query and entry are two separate
  events on (possibly) two tasks, joined only by the client's order; a
  failure between them is traced under "The exit's order". Pass.

## Internal depot usage

- `*index-ops` is operator-appended, not `:disallow`; it carries records no
  client offer carries (maintenance, not acts), and putting them on
  `*offers` would put maintenance into the record (plan lines 441-443).
  Pass.

## Cross-topology correctness

- No internal depot flow. Pass.

## Stream topology correctness

- No `depot-partition-append!` inside a topology. (F2's pages are appended
  by the operator's client loop, not by the topology, precisely so that no
  in-topology append and no page-chain dedup is needed.) Pass.

## In-memory state efficiency

- No TaskGlobal. The fingerprint secret is a derived constant. Pass.

## Minimality — adversarial simplification

Simplest design I can construct: one index `:ix-ek` (entity, key, stamp)
serving everything, `[:k]` and `[:kv]` answered by a full scan; the entry as
an ordinary offer; no reverse map; no operator depot (rebuild as a test-only
pure function).

- **`:ix-ke`**. Delete it: `[:k k]` becomes a full scan of `:ix-ek`, O(layer)
  per read; phase 6's runner "finds tools by matching" by key (SPEC phase 6),
  so key reads are frequent. For a 10,000-fact layer: 10,000 iterations
  (≈ 50 ms plus page seeks) against 8 seeks + 1,001 iterations. Kept.
- **`:ix-kv`**. Delete it: `[:kv]` becomes a scan of `[:k k]` filtered by
  value, which the plan already offers as the reader's own `[:k]` read. The
  spec requires "only what pattern reads need" (STARTER); key-and-value is
  the runner's match (STARTER "a minimal runner finds tools by matching",
  plan line 953). For 4,000 `:note` facts: 4,000 iterations against 3 seeks.
  Kept.
- **`:ix-of`**. Delete it: a purge must rebuild the `:ix-kv` address from
  the value. For a value forget the gate has the plaintext in that event,
  but a person forget (phase 2) makes values unopenable without touching
  them, and the sweep of F2 must find `:ix-kv` entries of an erased value;
  without `:ix-of`, a purge by value id needs the value, which the spec's
  "purgeable by value id" excludes. Kept.
- **`*index-ops` and the second source**. Delete it: no way to run a rebuild
  or the test purge inside the gate's event (the only writer of
  `$$layers`). Merge into `*offers`: puts maintenance into the record (the
  plan's reason, first-record). Kept.
- **Doubling pages (RC3)**. Bypass with one range read of limit + 1: a
  3-fact entity iterates up to 1,001 entries. Kept.
- **The clamp (FR2)**. Bypass (record the asked moment): a recorded moment
  past the clock names a read whose answer can still grow. Kept, and F1
  depends on it (the moment is at most the clock, so a moment bounded by a
  real stamp is what the entry stands on).
- **The derived fingerprint secret (FR12)**. Bypass with phase 1's digest
  secret: the client holds a copy (F11 of phase 1), so any client could
  confirm what a person read. Kept.
- **The pure/dataflow split** and "one line in module.clj": required by the
  shared-file constraint (parallel builds); kept.

No mechanism fails deletion. Pass.

## Throughput — adversarial

- Pattern reads: 3 seeks + one iteration per entry; the cheapest design
  meeting latency (B, row seeks per fact, costs 43 to 63 seeks for 20 facts:
  constructed in the plan, rejected on numbers). Pass.
- Index writes per admitted act: 2 entries per fact (+2 per value-indexed
  fact), `termval`, no read. The gate's event gains no seek for an ordinary
  offer. This is the path the agent-rate number measures tonight. Pass.
- **Fail before F7**: every exit call writes a line whose value (up to about
  80 KB for a 1,000-pair exact list) is copied into `:ix-ek` and `:ix-ke`:
  about 160 KB of extra write bytes per large read, on the most frequent
  write the stage adds, for entries read rarely (the session close act,
  audits). Constructed alternative: entries of `:read/*` keys carry no value
  copy; a pattern read that shows one fetches its row, 1 to 2 seeks each.
  Side by side, per 1,000 reads of which 1 later pattern read shows 100
  entries: copy = 160 MB extra writes and 0 extra seeks; no copy = 0 extra
  writes and 100 to 200 seeks (≈ 50 to 100 ms once). The plan named this
  alternative and deferred it to after measurement, which the template does
  not allow. F7 adopts it as a hint, `:no-copy`, a rig choice (no record
  changes).
- Two round trips per read (query, then an acked append): the one-trip
  alternative was constructed (difficulty log 8) and does the same seeks and
  writes; it differs in latency only. Pass.

## Spec coverage — trace every operation and constraint

### C1. Point reads on the layer's home, as of a moment

- **Source**: SPEC phase 5, "Point reads and pattern reads"; STARTER "Point
  reads and pattern reads on a layer's home task"; IMPLICIT_SPEC RD2 "Point
  read: a layer, an entity, a key; now or as of a moment ... returns the
  chain's head as of the read's moment".
- **Trace**: Alice's `:alice` on task 1 (of 4). f1 `[n1 0]` admitted at
  s1 = 100, f2 `[n2 0]` replacing it at s2 = 200; clock 200. `read-point
  [:alice :alice [[n1 0] [n2 0]] 150]`: settings (visible), clock 200,
  m = min(150, 200) = 150; record n1 yes at 100 ≤ 150 → row, open →
  `{:value ..}`; record n2 yes at 200 > 150 → `{:fid [n2 0] :absent true}`.
  `:matched [[[n1 0] 100]]`.
- **Fault**: no in-memory state; a restart fails the query, the exit shows
  nothing and records nothing. Retry: read-only. Multi-partition: none.
- **Race**: an offer on task 1 during the loop's yield is stamped above
  200 ≥ 150: filtered. Two readers: independent.
- **Flaws found**: RD2's point read is by cell (entity and key); the plan's
  is by fact id and the cell read is `[:latest e k]`, a pattern line. That
  changes what the record carries (rows or a line) without being named (F9).
- **Verdict**: FAIL before F9; PASS after (the divergence is surfaced as a
  first-record pick for Sid).

### C2. Pattern reads, as of a moment; nothing admitted after it

- **Source**: PROGRESS sharpening "a read as of a moment shows nothing
  admitted after it, except an erasure, which shows only its date."
- **Trace**: `[:ek :e0 :note]` as of s1 = 100 in the history above: range
  `e0␀note␀` to `e0␀note␀hex(101)`: f1 only. `[:latest :e0 :note]` as of
  150: tail read below `e0␀note␀hex(151)` → f1 (f2's address has hex(200),
  above the bound). `[:e :e0]` as of 150: the prefix holds f1 and f2; the
  page-step drops f2 (stamp 200 > 150). `[:all]` likewise.
- **Fault**: as C1. **Race**: a write during a yield between pages is
  stamped above the clock read before the first page, so above m: dropped.
  A purge between pages: the entry read before shows the value, after shows
  the date; both are one task's linear order.
- **Flaws found**: (a) F1: the entry that records this read, decided on
  another task, can be stamped below m and below s1 (trace in C7); a read
  as of a T between the entry's stamp and s1 then shows the entry, which
  names `[[n1 0] 100]`, admitted after T. That breaks this constraint for
  the store taken as a whole. (b) F8: `[:all]` as of 100 on a layer of
  100,000 facts stamped later scans all 100,000 entries to find one: no
  bound, no partial mark.
- **Verdict**: FAIL; PASS after F1 and F8.

### C3. An erasure shows only its date; the value index purged by value id

- **Source**: sharpening (as C2); STARTER "each purgeable by value id";
  rig constraint "Every index over values is rebuildable from the log or
  purgeable by value id, so forget reaches it."
- **Trace**: f3 `[n3 0]` on `:note` (value-indexed), value "x", s3 = 300.
  Entries: `:ix-ek` `e0␀note␀hex(300)␀fid`, `:ix-ke` likewise,
  `:ix-kv` `note␀00000003␀"x"␀hex(300)␀fid` (vtext canonical `"x"`, length
  3), `:ix-of[[n3 0]] = #{that address}`. Forget at s4 = 400 (tonight the
  test `:purge`): RE4 reads `:ix-of` (1 seek); `purge-writes` → `:ix-ek` and
  `:ix-ke` tombstones `{... :v nil :erased-at 400}`, `:ix-kv` address and
  `:ix-of[fid]` deleted, all in the forget's event. Read as of 350:
  `[:e :e0]` shows f3 with `:erased-at 400` only (the erasure's date, after
  the moment: the one allowed exception); `[:kv :note "x"]` matches nothing;
  no address or entry holds "x".
- **Fault**: the purge's writes are sets/deletes in the forget's one event:
  replay writes the same. **Race**: a read yielding across the forget's
  event sees the value before or the date after.
- **Flaws found**: F5. A person forget (phase 2) closes every value wrapped
  under the person's lock without touching them; PLAN-locks-and-forgetting.md
  (cbd2bb16) line 749 (OP10) writes `$$persons[p] = {:lock nil :erased-at
  s}` on the home and on every task through `(|all)`, and line 649 "for a
  person act one termval into $$persons": no purge. The `:ix-kv` address of such a value keeps its plaintext; a
  `[:kv :note "x"]` read then matches it and shows `:erased-at`, which
  confirms the guess "x" at a forgotten value (IMPLICIT_SPEC RD3: "never a
  match on a value erased before the read"; RD6: a fingerprint "does not let
  anyone confirm a guess at a forgotten value"). The plan's text ("which one
  is phase 2's pick") leaves the obligation to a plan that does not take it.
- **Verdict**: FAIL; PASS after F5 for what reads show (a `[:kv]` candidate
  matches only when it opens to the pattern's value). The store still holds
  that plaintext in `:ix-kv` until a purge or rebuild reaches it: the
  obligation is written into the plan's interfaces for phase 2 (F10) and
  goes to builder A.

### C4. One exit: query, entry acknowledged, answer — a crash at each hook

- **Source**: STARTER "One exit for reads: query, append the read entry,
  then answer; nothing shown before its entry is acknowledged"; RIG.md
  default 4.
- **Trace**, one read `[:ek :e0 :note]` of `:alice` by Alice, working layer
  `:alice-hand`, entry name nm:
  - `:exit-after-query` armed: step 2 returned; the throw leaves nothing
    shown; nm never reached `*offers`, `client/lookup nm` gives no answer.
    Holds.
  - gate `:before-writes` on nm (task of `:alice-hand`): the worker dies,
    the event's writes are discarded (RQ 1), the client's append errors;
    `offer-until-answered!` resends the same built map under nm; the replay
    or the resend decides it once (phase 1's road); `:yes` → rows shown.
    One answer under nm, one line. Holds.
  - gate `:after-writes` on nm: the throw is inside the event, after the
    writes: the event fails and replays; the replay finds nm's record only
    if the writes committed — they did not (the event did not finish), so it
    decides afresh with the same answer; either way the resend is answered
    from the record. Holds (phase 1 tested both roads).
  - `:exit-after-entry` armed: the entry is `:yes` and recorded, nothing
    shown. Over-records (named in the plan for Sid). Holds for "nothing
    shown before acknowledged".
  - `:exit-shown` armed: as `:exit-after-entry`.
  - A refused entry (Alice's hand permission revoked): step 4 gets `:no
    :permission-revoked`; step 5 returns `{:refused .. :entry nm}`, no rows.
    Holds. A lost ack on a `:no`: the resend is answered `:no` from the
    record. Holds.
- **Race**: a forget of a shown value between the query and the entry's ack:
  the value was opened by the read (a read that happened, recorded by the
  entry) and is shown after the forget; IMPLICIT_SPEC RD3 ("either the read
  opened the value (a read that happened, recorded) or it did not") allows
  it, and it matches the promotion ruling's "the read-out is a read that
  happened". Not a flaw; worth saying at the exit (named for Sid).
- **Flaws found**: F3. The answer carries `:resume`, the address of the
  first matching entry not shown; the exit returns what the query answers,
  so a person or model reader gets that entry's fact id and stamp (and for
  `[:e e]` its key) while the entry's exact list names only the shown rows.
  Ruling 3: "a model's or a person's reads are the crossing's exact list,
  always". No input takes `:resume` back, so it buys nothing.
- **Verdict**: FAIL; PASS after F3.

### C5. The read entry's content (ruling 3)

- **Source**: ruling 3 "rows for point reads; one line per pattern read
  carrying the pattern, the moment, the role and a fingerprint of what
  matched; a complete-or-partial mark; empty pattern reads included ... a
  model's or a person's reads are the crossing's exact list, always ... a
  deterministic tool gets the short entry by default and may ask for exact
  rows in its signature ... Roles at seed: stood on, shown, matched, passed
  through."
- **Trace**: Alice `[:e :nobody]`: line `{:layer :alice :moment {:stamp m}
  :role :shown :pattern [:e :nobody] :mark :complete :count 0 :fingerprint
  fp(#{}) :fp-secret :read-fp/1 :exact []}`. A tool, `:rows? false`: the
  same without `:exact`. A point read of three ids: three `:read/point`
  facts. Roles: the four keywords.
- **Fault/race**: the entry is an ordinary act (phase 1's roads).
- **Flaws found**: the kind is supplied by the caller: a model could be
  called a `:tool` and leave a short line, against "a model's ... reads are
  the crossing's exact list, always". In the rig the exit's caller is the
  test; in a kept store the gateway must take the kind from the actor
  (session start fact), not from the call. Named (F10). A `[:kv k v]`
  line records v in its pattern, in the reader's working layer, which a
  forget of the matched value does not reach; the reader typed v, so this is
  the reader's own utterance, not a store copy, but it is a first-record
  question (for Sid). Not a failure of the plan against ruling 3.
- **Verdict**: PASS (with F10's note).

### C6. The fingerprint

- **Source**: RIG.md default 3 "The fingerprint is keyed, over the ids and
  stamps of what matched, never values"; rig constraint "Fingerprints over
  values are keyed"; STARTER "Build it as one swappable function."
- **Trace**: matched `#{[[n1 0] 100] [[n3 0] 300]}` → HMAC-SHA256 under
  `HMAC(root, "softland/read-fingerprint/1")` over
  `"softland.read-fp/1\n" + canonical(set)`; the set's canonical text is
  sorted, so the index that served it does not matter. `fingerprint` takes
  pairs only (T7). The secret never leaves the query topology.
- **Flaws found**: none, with reasoning: inputs are ids and stamps only; the
  secret is module-only (unlike phase 1's digest secret, F11 there); the id
  `:read-fp/1` lets a rotation keep old lines checkable. After F3 no unshown
  pair reaches the reader.
- **Verdict**: PASS.

### C7. The entry's stamp against the moment it records (ruling 4)

- **Source**: ruling 4 "Clock promises: yes. Never backward within a unit;
  never earlier than anything the fact stood on"; RIG.md default 2 "A
  task's next stamp is the largest of the wall clock's ms × 65536, its last
  stamp plus one, and the largest stood-on stamp plus one"; CONCLUSION R2
  "hybrid logical clock"; the sharpening (C2).
- **Trace**: 4 tasks; `:alice` on task 1, `:alice-hand` on task 3. Wall at
  ms 1,000 on both. Task 1 has given 500 stamps in that ms: clock
  = 1,000 × 65,536 + 500 = 65,536,500, the stamp of f1, just admitted in
  `:alice`. Task 3's clock is 65,500,000. Alice reads `[:ek :e0 :note]` as
  of now: m = 65,536,500, matched `{[n1 0] 65,536,500}`. The entry, stood-on
  `{}` (FR4), is decided on task 3: `next-stamp` = max(65,536,000,
  65,500,001) = 65,536,000 < m. A read of the store as of T = 65,536,200:
  `:alice` shows no f1 (100 < T < s1), `:alice-hand` shows the entry, whose
  line names `[[n1 0] 65,536,500]` and `:moment {:stamp 65,536,500}`, both
  after T. Across nodes with clock skew the gap is the skew, not one
  millisecond. The model has no read entries, so it does not decide this;
  its `stamp-for` puts every act after everything it stood on.
- **Decision**: the entry must stand on what it read, and the right carrier
  is its moment, not the matched pairs. The moment is at least every matched
  stamp (the query drops anything later), so one number orders the entry
  after everything it names; it constrains an empty read too, which matched
  pairs cannot; and it costs nothing, where R4's based-on (every matched
  pair in `:stood-on`) would store a person's exact list twice and turn a
  tool's short line into a long one. Phase 1's `:stood-on` is keyed by fact
  id, and a moment is not a fact, so the carrier is the moment the entry's
  facts already hold: `gate/stamp-for` counts each `:read/point` and
  `:read/pattern` fact's `[:moment :stamp]` as a stamp the act stood on
  (`reads/entry-moments`, total: only a long in [0, `env/max-carried-stamp`)
  counts, so a hand-written line pushes a task's clock no further than a
  carried stood-on stamp already can, R16). The entry's `:stood-on` stays
  `{}`: it records a reading and is not based on it (FR4 unchanged).
  First-record, as FR14, for Sid; the alternative is listed beside it.
- **Fault**: pure input to a pure stamp rule; replay computes the same.
  **Race**: none new.
- **Verdict**: FAIL; PASS after F1.

### C8. Agent session reads recorded there

- **Source**: RIG.md default 4 "Agent session reads are recorded there, and
  kept or dropped when the session closes; the close act says which."
- **Trace**: `:working :alice-agent`, `:reader-kind :model`: the line with
  its exact list lands in `:alice-agent` (T15). The close act is later
  (after phase 2's forget), found by `[:k :read/pattern]` and
  `[:k :read/point]`.
- **Flaws found**: none, with reasoning: the brief says "Eager in personal
  and hand layers; agent session layers untouched" while default 4 records
  agent reads; the plan follows default 4 and says so (plan line 737-739).
- **Verdict**: PASS.

### C9. The moment's form

- **Source**: default 3 "A one-owner layer's moment is its stamp, inline; a
  shared layer's is the id of its settled frontier."
- **Trace**: `{:stamp m}`; phase 3's frontier id (micro plan M23: the
  microbatch id of the last settled batch, a Long) fits `{:frontier id}`.
- **Verdict**: PASS.

### C10. Index writes of every admitted act

- **Source**: STARTER "Indexes: only what pattern reads need, each
  purgeable by value id and rebuildable from the log. Index kinds take hints
  as parameters." SPEC "What Rama showed" 2 and 3 (idempotent, never
  throws).
- **Trace**: an act of 3 facts, one on `:note` "y": `index-writes` gives 6
  `:index-put` (2 per fact) + 1 `:ix-kv` + 1 `:index-of`; applied after the
  heads writes, in the yes branch, in the event that writes the answer.
  Cost: 8 `termval`s, no read. A read entry act of one line: 2 entries
  (after F7 without the value copy).
- **Fault**: a crash before the event's end discards all (RQ 1); a replay
  writes the same computed entries. A bug in `index-writes` returns empty
  lists plus `:index-error`, which `decide*` turns into `:gate-error`
  (phase 1's road): no throw. **Race**: one task, one event.
- **Flaws found**: none beyond F6 (entry shape) and F7 (copy).
- **Verdict**: PASS after F6, F7.

### C11. Rebuildable from the log

- **Source**: STARTER "each purgeable by value id and rebuildable from the
  log"; SPEC "What Rama showed" and RESULTS.md line 45 "A slow event is
  retried while still running ... the event-tree timeout here is 5 s ...
  retried from the depot five more times while the first attempt sat on
  B ... force-failed after 30 s", and its consequence "Gate work per event
  must stay well under the stream timeout, or the event runs twice at once."
- **Trace (plan as written)**: an agent layer of 100,000 one-fact acts.
  One `:rebuild` event: `[(keypath L :answers) (subselect ALL)]`
  materialises 100,000 records; then 100,000 `(keypath L :log nm)` seeks
  (≈ 0.3 to 0.5 ms each: 30 to 50 s); then the four fields whole (about
  200,000 entries plus 0 to 100,000 `:ix-kv`, held in memory). Past 5 s the
  event is retried while the first attempt still runs; at 30 s the batch is
  force-failed and the record replays from `*index-ops`; every attempt
  restarts from nothing, none finishes, and the home task (every layer on
  it, including their offers) is blocked for the whole cycle. The plan says
  so ("the event blocks the home task for the whole layer") and defers the
  paged form to a kept store; SPEC: "build in the form it should be,
  because it may be kept", and the skill: "Never downgrade a correctness
  guarantee because 'tests won't hit that case.'"
- **Fix, F2, traced**: two operator ops, each one event, bounded:
  put page `{:layer L :op :rebuild-put :after nm-or-nil :acts n}`, n ≤ 256:
  up to n answer records after `nm` (`sorted-map-range-from nm {:max-amt n
  :inclusive? false}`, from the start when nil), stopping early once 4,096
  rows are gathered; per yes act its rows (1 seek) and `open-row>` per row;
  `termval` of every implied entry and `:ix-of` set; ack `{:next last-nm
  :done? bool}`. Cost ≤ 256 seeks + ≤ 4,096 opens and ≈ 10,000 writes:
  ≈ 0.1 to 0.3 s. Sweep page `{:layer L :op :rebuild-sweep :field f :after
  address-or-nil :entries n}`, n ≤ 512: up to n entries of field f after the
  cursor, and per entry its act's record (1 seek) and row (1 to 2 seeks);
  an entry the log does not imply (no yes act, other stamp, no such row,
  other entity or key, hints route it elsewhere, other address or other
  content) is deleted, and for `:ix-kv` removed from `:ix-of`. Cost ≤ 1,536
  seeks ≈ 0.5 to 0.8 s. The operator's loop (`read-exit/rebuild!`) runs put
  pages to the end, then sweep pages over the four fields, resending a page
  on error.
  - 100,000 facts: about 400 put pages and about 600 to 800 sweep pages;
    minutes in all, no event near 5 s.
  - Replay or resend of a page: put pages `termval` the same computed
    entries; sweep pages delete only entries the log does not imply, and a
    second run of the same page finds none left. Idempotent.
  - An offer admitted during the rebuild writes its own entries in its
    event; a put page never deletes; a sweep page reads the record and row
    in its own event, so an entry of an act admitted before it is implied
    and kept. A forget during the rebuild purges in its own event; a later
    put page sees `open-row>` erased and writes the same tombstone; a later
    sweep sees the tombstone implied. End state: exactly what the log
    implies when the last page runs.
  - Reads during a rebuild: every implied entry is present once the put
    pass has passed its act (put pages only add), stale entries until the
    sweep passes them; after a test `:drop` the dropped entries are missing
    until the put pass reaches them. Named in the plan.
- **Verdict**: FAIL; PASS after F2.

### C12. Hints as parameters; opaque keys

- **Source**: STARTER "Index kinds take hints as parameters"; ruling 6
  "at the cost of no matching, no shape check and no index on them".
- **Trace**: `seed-hints {:by-value #{:note} :opaque #{}}`; `[:kv :mention
  ..]` refused `:not-indexed`; an opaque key never gets an `:ix-kv` entry
  and `[:kv]` on it is refused `:opaque`. F7 adds `:no-copy`.
- **Verdict**: PASS.

### C13. Visibility (ruling 9)

- **Source**: ruling 9 "a person's own and session layers private to that
  person".
- **Trace**: Bob reads `:alice-hand` → `:not-visible`; Bob reads
  `:alice-q3` (never made) → `:no-such-layer`. Bob learns which of Alice's
  layer ids exist.
- **Flaws found**: F4, an existence oracle over private layers.
  IMPLICIT_SPEC O22 leaves open whether "cannot see" differs from empty; it
  does not license telling "exists" from "does not".
- **Verdict**: FAIL; PASS after F4.

### C14. A person hand-writing `:read/*` facts

- **Source**: default 3 "its keys as store-owned constants" (first-record).
- **Trace**: Alice offers `{:e :x :k :read/pattern :v "anything"}` into
  `:alice-hand` under her permission: phase 1's gate admits it; it is
  indexed; `[:k :read/pattern]` in `:alice-hand` returns it beside the
  exit's lines; the later close act would keep or drop it as an entry. After
  F1, `entry-moments` ignores a value that is not an entry line and counts a
  moment only below `max-carried-stamp`, so it cannot break the clock; it
  can forge an exposure record in a layer the writer may write. Telling
  exit-made lines from hand-made ones needs the exit on the server (R5) and
  a reserved-key rule at the gate, a first-record change.
- **Verdict**: PASS as planned (surfaced for Sid, not decided here), with
  F1's bound traced.

### C15. A gate never throws; every refusal is data

- **Source**: SPEC "What Rama showed" 3.
- **Trace**: `index-writes`, `purge-writes`, the page functions,
  `entry-moments`: pure, total, catching `Throwable` into
  `:index-error`/`:gate-error`; the `*index-ops` intake refuses anything
  unknown as data.
- **Verdict**: PASS.

### C16. open-value, the one function every shown value passes through

- **Source**: STARTER "Values stay unsealed; the open step passes them
  through. A rig choice."; ruling 6 "the gate by default".
- **Trace**: every row and entry shown goes through the call; tombstones
  show a date without opening.
- **Flaws found**: F6: the plan's contract `(locks/open-value *layer *fid
  *entry *moment)` → `{:value v}` differs from phase 2's plan (`open-value>
  [layer fid T]`, and its twin `open-row>` given a row and its act's stamp;
  `{:value v :stamp s}`, `{:erased-at date}`, `{:unreadable reason}` with
  `:no-such-fact`, `:after-moment`, `:does-not-open`), and the plan's rule
  "phase 2's `open-value` must keep this arity" contradicts a plan
  validated in parallel. Two stubs of one namespace would collide at merge.
- **Verdict**: FAIL; PASS after F6 (phase 2's contract wins; this stage
  calls `open-row>` with the entry as the row; the stub, if needed, has
  phase 2's names and arities).

### C17. Mind the costs: seeks, subindexing, yielding

- **Source**: STARTER/brief; the skill's cooperative multitasking rule.
- **Trace**: every query page `{:allow-yield? true}`; per-fact loops
  `yield-if-overtime`; unbounded maps subindexed; after F2 no event scans a
  whole layer; after F8 no query scans past its budget.
- **Verdict**: FAIL before F2 and F8; PASS after.

## Unverified Rama behaviours (the plan's [build checks])

Each has a fallback that keeps the design, so none was probed:

| behaviour | fallback | design kept? |
|---|---|---|
| a fixed-keys field chosen by a var in `keypath` | one block per field, constant keypath (phase 1's form) | yes |
| `keypath` with an index into a subindexed vector | `(nthpath *idx)` | yes |
| `<<query-topology` from a function called in the module body | the two forms inline in module.clj | yes |
| `declare-depot` from a function called in the module body | inline | yes |
| a second `source>` from a function, or a second `<<sources` call | the source inline in module.clj | yes |
| whether a query topology exception is fatal to the worker | none needed: every query step is total by construction | yes |

The behaviours the design rests on are probed (String address order and
prefix ranges, the tail read) or documented (`:max-amt` forms and
`:inclusive? false`, paths.md 467-475; yields). F2's page reads use the same
documented `sorted-map-range-from` with `{:max-amt n :inclusive? false}` on
the vector-keyed `:answers`; a page from an exact existing key needs no
prefix range, so the probe's finding about vector ranges does not bear on
it. Because a failure there would have changed F2's design (the put pass
would need another order to walk), it was probed tonight under the
machine-wide lock: [probed] `runs/probes/rig/probe/read_pages_probe.clj`,
output `runs/phase5-read-pages-probe.txt`: 23 names of the form `[layer
class scheme uuid]` in a subindexed map, walked by
`sorted-map-range-from-start 4` then `sorted-map-range-from last {:max-amt
4 :inclusive? false}`, came back in 6 pages of 4, 4, 4, 4, 4 and 3, every
name exactly once, in the map's own order (1 test, 3 assertions, 0
failures, 02:34 IST).

## Self-consistency

Every place above that names a gap is a FAIL with its fix (F1 to F11) and is
fixed in the plan. What remains open is surfaced, not certified: the
over-recording crash (entry recorded, answer not shown), hand-written
`:read/*` facts, a `[:kv]` line holding its value in the pattern, the
person forget's purge of `:ix-kv` (phase 2's obligation), and the reads that
see stale entries during a rebuild. None is a failure of this plan against
its spec; each is a question for Sid or builder A.

PHASE_VALIDATION:minor-fail
