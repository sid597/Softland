# First record: applying the research. Handoff, 21 September 2026

Written by Claude Fable 5.1 at max effort at the end of Sid's session of 21 September 2026, so a
new session can continue on top. Every part carries its status. Nothing here is a ruling of Sid's
unless it says so. Sid's own text (the problem, the forced questions, one fact, handles 0 to 17) is
not copied here; he hands it to the session himself.

## Where the work stands

- **Sid's question, in his words:** "i am deciding what my store must fix before its first record.
  the store only appends and never rewrites, so whatever a fact does not carry when it is made is
  gone for every earlier fact. what i want first: the conventions that would paint me in a corner."
- **The material:** eight md files in `research/`, from seven research sessions in three rounds,
  merged by an orchestrator. Together 11,584 lines, 1.24 MB, about 310k tokens.
- **Done:** `clocks-ids-determinism.md` read whole and applied. The write-up is
  `research-trimmed/clocks-ids-determinism.applied.md`.
- **Said by Sid in this session:**
  - Reading all eight whole at max effort is not going to work: it uses everything and has a long
    time to value. The way forward has to be small.
  - The economy is out of scope as of now.
  - The scale he holds: hundreds to thousands of agents per person, writing at machine speed; tens
    of thousands of people and their agents on one problem.
- **Reading fence (Sid's):** only the eight md files, this folder's handoff files, and what is in
  his prompts. Never `research/loop/` or `research/sources/`. Never `src/app/server/env.clj`. The
  session that wrote this read no code, no `docs/`, no `vision/LOG.md`, no `decisions.md`.

## The picture as that session held it

Status: Sid moved on without correcting it. He did not confirm it in words.

- **Planet: the problem.** Where a line goes: which pieces are fixed, and what the fixed side must
  guarantee so everything live stays coherent. Six lines say what coherent means. The scale turns
  every "always" and "every" into a rate. Four things hold throughout: the map must not lie, no
  optimism, tools matched and never named in the store, same moves for people and agents.
- **Forest: what the problem forces.** One loop and what surrounds it. The loop: a fact lands, the
  tools it matches fire, they read, they give back a running answer or an offer, the offer meets the
  one gate, a fact lands. *Because of* links one turn to the next. *Based on* records the reads
  inside a turn. Around it: the record and the log; what keeps it honest; layers; what stays
  outside; the line and its check. The log ends up holding only what cannot be worked out again:
  what came in through the door, what the gate decided, what went out to a person, a model or the
  host.
- **Tree: one fact.** Nine parts, six things around it, eighteen handles, one test: gone for good,
  or fixable by a later fact. Each part is where a forest mechanism takes hold of the fact.
- **The two "to"s.** Neither is a deduction. The problem rules things out and Sid chose among what
  was left; choosing under forcing is architecture, and it can fail by itself. A level is what can
  be seen from one distance; a "to" is what must be true for two levels to be views of the same
  place. Planet to forest is tested by the rate of new built-in steps, and by cost curves. Forest to
  tree is hard because every part of the fact is paid on every fact for ever and is justified only
  by a guarantee that cannot be had later; (0) decides how hard it binds. Both run both ways: down
  says "this is forced", up says "this is enough, and affordable". The same "to" lives inside the
  place: summaries over papers, levels stored nowhere, stale and doubt climbing through based-on.
- **Derived, not checked:**
  - One operation ("a fact lands: which standing patterns does it fall in?") fires tools, redoes
    watched answers, and marks pattern reads stale. After file one, refined: eager matching only for
    tools and watched answers; a historical pattern read is checked lazily from its pattern and cut.
  - Tracing down: the sixth line, affordable, reaches no part of the fact and has no mechanism of
    its own. Tracing up: (9), (16) and half of (14) reach no line among the six; what stays a
    person's is unwritten at the planet. At no level: what stops a bad live change from locking the
    place against its own repair. Also distance: no optimism plus one store means every act waits on
    a trip.
  - Five clusters of handles: what points at what for ever (2, 3, 1, 6, 17); what current, as-of and
    replaced mean under per-part order (10, 11-when, 5, 6, 0, 13); nothing unlisted went into a fact
    (11, 4, 5, 12, 7); who speaks, sees, fires (8, 15, 16, 14, 7); kept against truly gone (9, 0,
    13, 1).

## The ledger from file one (the yardstick)

Status: one voice from one file. Not a lean of Sid's. Not ruled. Sid has not approved it as a
yardstick in words. The split into corners and early losses is the session's own and unchecked.
Reasons and line anchors are in `research-trimmed/clocks-ids-determinism.applied.md`.

- **L1 name.** The offerer makes a random 128-bit id for every entity, offer and fact, before the
  first attempt. Never from content, never the gate's number, no "where" or "when" inside. Facts
  point by it. A digest, tagged with its algorithm, may sit beside it.
- **L2 as-of.** An opaque, kind-tagged token minted by the store. A comparison may answer
  "incomparable".
- **L3 order.** The cell (entity, key, layer) sits in one ordered unit. What else shares that unit
  is open: entity or layer. Partition identity stays out of names and out of as-of.
- **L4 key.** A key is an id; its word and grammar are facts. The first key ids are published
  constants. The store id, incarnation and gate actor differ per store.
- **L5 erasure.** From the first erasable value: delete a key, or keep the value outside. Digests
  over ciphertext. The actor id stays; who it is can be erased.
- **L6.** The new fact names the fact it replaces.
- **L7 envelope.** Versioned and closed. No empty or magic value serves as a signal. The logical
  envelope is kept apart from the physical encoding.
- **L8.** By-whom is the immediate actor. Acts-for is separate, scoped and short-lived.
- **E, early losses.** The verdict kept under the offer id, atomic with the compare-and-set, naming
  grammar version, policy position and epoch. Crossings carry a digest, plus content for a model or
  the host. A claimed "when" beside the gate's. The grant invoked, named on every offer. Read marks
  with a third value: shown, dependence unknown. Reads live on the crossing as pattern plus cut,
  never as rows.
- **A, above the table.** Re-deriving holds only for a tool version on a build. A rebuild is a fact.
  Safety rests on the compare-and-set plus an epoch, not on there being one writer.

The session's six pushbacks on file one are in section 7 of the applied write-up.

## What the pilot showed, and the mechanics

- **The method:** the main session picks what to read whole from the headings and section weights.
  Codex reads the whole file in a read-only sandbox and returns line ranges with tags, never text.
  `research-trimmed/assemble.py` checks the plan (every candidate line exactly once; a deferral may
  only point at kept text) and copies by line number, so no model retypes anything.
- **Run used:** `codex exec -s read-only -C <repo> -c 'model_reasoning_effort="high"' --color never
  -o <plan.tsv> - < <prompt.md>`. Model was gpt-6-astra. About nine minutes for a 200 KB file, run
  in the background.
- **Result on file one:** kept by the session 70.7%, promoted by Codex 21.1%, deferred 8.2% (almost
  all of it the source list). The faithful trim saved 6%. All 18 of Codex's "already said elsewhere"
  calls were fair when checked against the text.
- **Lessons:** no size targets, a literal follower will cut good text to hit one. Ranges, not
  retyped text. Value in file one sat in about a quarter of it: round two (where the leans are
  pressed), the short version with its "what cannot be added later" list, and "what this camp would
  question above the table". The team-by-team evidence was 60% of the bytes.
- `clocks-ids-determinism.trim.md` is regenerable from the plan and the script.

## Layout of the other seven files

Measured from top-level headings only; no bodies read. Line, size, heading.

- `defaults-skeptics-bigtech.md`: L1 75.4 KB main; L368 12.9 KB "Round two: the leans, pressed from
  the skeptics…".
- `facts-datalog.md`: L1 105.7 KB main; L763 49.1 KB "Round two"; L1045 6.7 KB "Round three, line 1:
  what is the unit of total order, the entity or …".
- `frontiers-views.md`: L1 87.6 KB main; L505 13.2 KB "Round two: the leans, pressed from the
  frontiers camp".
- `log-as-truth.md`: one top-level heading, 138.7 KB. Inner layout not yet listed.
- `meaning-objects-substrates.md`: one top-level heading, 188.9 KB. Inner layout not yet listed.
- `rama-marz.md`: L29 33.1 KB "Part one — What Rama's own reference says"; L259 18.8 KB "Part two —
  Nathan Marz"; L359 10.5 KB "Part three — The dissent: Jay Kreps"; L425 9.1 KB "The group"; L481
  sources; L514 33.1 KB "Round two"; L736 6.7 KB "Round three, line 1".
- `sync-versioning-defaults.md`: one top-level heading, 220.8 KB. Inner layout not yet listed.

The two round-three sections are on the ledger's least settled line, L3.

## The small way, proposed for the next session

Status: proposed by the session after Sid said the large way will not work. Sid asked for a new
session to continue on top of it.

1. The ledger above is the yardstick.
2. Seven Codex runs in parallel, read-only, one per remaining file, same prompt. Each returns line
   ranges only, tagged: DISAGREES with a ledger line; NEW-REASON or deciding case the ledger lacks;
   NEW-CORNER, something to fix before the first record that the ledger lacks; and the file's own
   "cannot be added later" list. Passages that say the same as the ledger are counted, not
   extracted. No size target.
3. A script copies those ranges verbatim into one bundle per ledger line, all voices side by side,
   with file and line. This needs building: `assemble.py` works per file; the bundler takes rows of
   (file, start, end, ledger line, tag, note).
4. The session reads the bundles and both round-three sections, then writes one synthesis, about a
   page per corner.

Known risk: a voice that rejects the ledger's terms outright will not show up as a delta on any
line. Cover: the session reads the top of `defaults-skeptics-bigtech.md` and of
`meaning-objects-substrates.md` itself.

## Questions carried to the other files

- **`rama-marz.md`:** What is the log physically: the depot of offers with the gate a fold over it,
  or a second depot the gate writes? If a fold, the gate's "when" must be fixed at append, not read
  inside a retryable topology. Can an index state the depot positions it has applied? Can the append
  path refuse a stale writer? Can the partition count change, and what happens to positions? How
  does index layout behave with random ids? What shares a partition: entity, layer, or both?
- **`facts-datalog.md`:** Hickey's side of "sayings have ids and facts do not". Attributes as
  entities, for L4.
- **`sync-versioning-defaults.md`:** content addressing's best case, set against L1.
- **`log-as-truth.md`:** erasure in event-sourced systems; verifiable logs, for the strong form of
  (0).
